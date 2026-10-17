package com.missionbreak56.musicplayer.data

import android.content.Context
import com.missionbreak56.musicplayer.R
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class Kind { ALBUM, ARTIST, FOLDER, GENRE, PLAYLIST, AUTO }

/** A browsable collection of songs (album, artist, folder, genre or playlist). */
data class Group(
    val kind: Kind,
    val key: String,
    val title: String,
    val extra: String,
    val songs: List<Song>
)

/** In-memory snapshot of the device library plus the derived collections. */
object Library {

    /** Every song found on the device, including hidden ones. */
    @Volatile
    var allSongs: List<Song> = emptyList()
        private set

    /** Songs the user can see (hidden songs and folders removed). */
    @Volatile
    var songs: List<Song> = emptyList()
        private set

    @Volatile
    private var byId: Map<Long, Song> = emptyMap()

    @Volatile
    private var allById: Map<Long, Song> = emptyMap()

    @Volatile
    private var genreMap: Map<String, List<Long>> = emptyMap()

    @Volatile
    var loaded: Boolean = false
        private set

    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    fun addListener(listener: () -> Unit) {
        listeners.addIfAbsent(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    fun song(id: Long): Song? = byId[id] ?: allById[id]

    suspend fun load(context: Context, force: Boolean = true) {
        if (loaded && !force) return
        val app = context.applicationContext
        val result = withContext(Dispatchers.IO) {
            MusicRepository.loadSongs(app) to MusicRepository.loadGenres(app)
        }
        val changed = !loaded || result.first != allSongs || result.second != genreMap
        allSongs = result.first
        allById = result.first.associateBy { it.id }
        genreMap = result.second
        applyHidden(notify = false)
        loaded = true
        // Reloading on every return to the app must not rebuild the lists when nothing changed,
        // otherwise a tap right after coming back is swallowed by the refresh.
        if (changed) listeners.forEach { it() }
    }

    fun isFolderHidden(path: String, hidden: Set<String>): Boolean =
        hidden.contains(path)

    /** Recomputes the visible songs from the hidden song / folder preferences. */
    fun applyHidden(notify: Boolean = true) {
        val hiddenSongs = Prefs.hiddenSongs()
        val hiddenFolders = Prefs.hiddenFolders()
        val visible = if (hiddenSongs.isEmpty() && hiddenFolders.isEmpty()) {
            allSongs
        } else {
            allSongs.filter {
                !hiddenSongs.contains(it.id.toString()) && !isFolderHidden(it.folderPath, hiddenFolders)
            }
        }
        songs = visible
        byId = visible.associateBy { it.id }
        if (notify) listeners.forEach { it() }
    }

    fun hideSong(id: Long) {
        val set = Prefs.hiddenSongs()
        set.add(id.toString())
        Prefs.setHiddenSongs(set)
        applyHidden()
    }

    fun hideFolder(path: String) {
        val set = Prefs.hiddenFolders()
        set.add(path)
        Prefs.setHiddenFolders(set)
        applyHidden()
    }

    /** Lower-case text without accents, so "Álbum" sorts next to "Album". */
    fun sortKey(text: String): String =
        java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()

    /** Index-bar letter of a title: "A".."Z", or "#" for digits and symbols. */
    fun letterOf(text: String): String {
        val c = sortKey(text.trim()).firstOrNull() ?: return "#"
        return if (c in 'a'..'z') c.uppercaseChar().toString() else "#"
    }

    /**
     * Sort orders: 0 A-Z, 1 artist, 2 album, 3 most recent, 4 longest,
     * 5 most played, 6 Z-A.
     */
    fun sorted(list: List<Song>, order: Int): List<Song> = when (order) {
        1 -> list.sortedBy { sortKey(it.displayArtist) }
        2 -> list.sortedBy { sortKey(it.displayAlbum) }
        3 -> list.sortedByDescending { it.dateAdded }
        4 -> list.sortedByDescending { it.durationMs }
        5 -> {
            val counts = Prefs.playCounts()
            list.sortedWith(
                compareByDescending<Song> { counts[it.id] ?: 0 }.thenBy { sortKey(it.displayTitle) }
            )
        }
        6 -> list.sortedByDescending { sortKey(it.displayTitle) }
        else -> list.sortedBy { sortKey(it.displayTitle) }
    }

    fun albums(): List<Group> = songs.groupBy { it.albumId }.map { (albumId, list) ->
        val first = list.first()
        val artists = list.map { it.displayArtist }.distinct()
        Group(
            Kind.ALBUM, albumId.toString(), first.displayAlbum,
            if (artists.size == 1) artists[0] else "",
            list.sortedWith(compareBy({ it.track }, { it.displayTitle.lowercase() }))
        )
    }.sortedBy { sortKey(it.title) }

    fun artists(): List<Group> = songs.groupBy { it.displayArtist }.map { (name, list) ->
        Group(Kind.ARTIST, name, name, "", list.sortedBy { it.displayTitle.lowercase() })
    }.sortedBy { sortKey(it.title) }

    fun folders(): List<Group> = songs.groupBy { it.folderPath }.map { (path, list) ->
        Group(Kind.FOLDER, path, list.first().folderName, "", list.sortedBy { it.displayTitle.lowercase() })
    }.sortedBy { sortKey(it.title) }

    fun genres(): List<Group> = genreMap.mapNotNull { (name, ids) ->
        val list = ids.mapNotNull { byId[it] }.distinctBy { it.id }
        if (list.isEmpty()) null
        else Group(Kind.GENRE, name, name, "", list.sortedBy { it.displayTitle.lowercase() })
    }.sortedBy { sortKey(it.title) }

    fun playlists(context: Context): List<Group> {
        val favIds = Prefs.favorites()
        val counts = Prefs.playCounts()
        val fav = songs.filter { favIds.contains(it.id.toString()) }
        val added = songs.sortedByDescending { it.dateAdded }.take(100)
        val most = songs.filter { (counts[it.id] ?: 0) > 0 }
            .sortedByDescending { counts[it.id] ?: 0 }
            .take(100)
        val recent = Prefs.recent().mapNotNull { byId[it] }

        val out = ArrayList<Group>()
        out += Group(Kind.AUTO, "fav", context.getString(R.string.pl_favorites), "", fav)
        out += Group(Kind.AUTO, "added", context.getString(R.string.pl_recently_added), "", added)
        out += Group(Kind.AUTO, "most", context.getString(R.string.pl_most_played), "", most)
        out += Group(Kind.AUTO, "recent", context.getString(R.string.pl_recently_played), "", recent)
        for ((name, ids) in Prefs.playlists()) {
            out += Group(Kind.PLAYLIST, name, name, "", ids.mapNotNull { byId[it] })
        }
        return out
    }

    fun group(context: Context, kind: Kind, key: String): Group? = when (kind) {
        Kind.ALBUM -> albums().firstOrNull { it.key == key }
        Kind.ARTIST -> artists().firstOrNull { it.key == key }
        Kind.FOLDER -> folders().firstOrNull { it.key == key }
        Kind.GENRE -> genres().firstOrNull { it.key == key }
        Kind.PLAYLIST, Kind.AUTO -> playlists(context).firstOrNull { it.kind == kind && it.key == key }
    }
}
