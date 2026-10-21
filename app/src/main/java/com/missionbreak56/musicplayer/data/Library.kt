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

        // 1. Show the last known library at once, while MediaStore is queried below.
        var hadCache = false
        if (!loaded) {
            val cached = withContext(Dispatchers.IO) { SongCache.read(app) }
            if (cached != null && !loaded) {
                hadCache = true
                publish(cached)
                listeners.forEach { it() }
            }
        }

        // 2. The real scan (genres are no longer read: there is no Genres tab).
        val fresh = withContext(Dispatchers.IO) { MusicRepository.loadSongs(app) }
        val changed = publish(fresh)
        // Reloading on every return to the app must not rebuild the lists when nothing changed,
        // otherwise a tap right after coming back is swallowed by the refresh.
        if (changed) {
            listeners.forEach { it() }
            withContext(Dispatchers.IO) { SongCache.write(app, fresh) }
        } else if (!hadCache && !SongCache.exists(app)) {
            withContext(Dispatchers.IO) { SongCache.write(app, fresh) }
        }
    }

    /** Stores [songs] as the library. Returns true when it differs from what was shown before. */
    private fun publish(songs: List<Song>): Boolean {
        val changed = !loaded || songs != allSongs
        allSongs = songs
        allById = songs.associateBy { it.id }
        applyHidden(notify = false)
        loaded = true
        return changed
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
        derived = Derived(visible)
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

    private val accents = Regex("\\p{M}+")

    /** Lower-case text without accents, so "Álbum" sorts next to "Album". */
    fun sortKey(text: String): String {
        var plain = true
        for (ch in text) {
            if (ch.code >= 128) { plain = false; break }
        }
        if (plain) return text.lowercase()
        return java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
            .replace(accents, "")
            .lowercase()
    }

    /** Index-bar letter of a title: "A".."Z", or "#" for digits and symbols. */
    fun letterOf(text: String): String {
        val first = text.trim().firstOrNull() ?: return "#"
        val c = sortKey(first.toString()).firstOrNull() ?: return "#"
        return if (c in 'a'..'z') c.uppercaseChar().toString() else "#"
    }

    /** The "Personalizada" sort: the user drags songs into the order they want. */
    const val SORT_CUSTOM = 7

    /** Like [sorted], but also understands [SORT_CUSTOM] (which needs the list's [scope]). */
    fun sortedFor(list: List<Song>, order: Int, scope: String): List<Song> =
        if (order == SORT_CUSTOM) sortedCustom(list, scope) else sorted(list, order)

    /**
     * The saved custom order first; songs that are not in it yet (new ones) go to the end, A-Z.
     * Before the user ever dragged anything, the Songs tab starts A-Z and other lists keep their own order.
     */
    fun sortedCustom(list: List<Song>, scope: String): List<Song> {
        val saved = Prefs.customOrder(scope)
        if (saved.isEmpty()) return if (scope == "songs") list.sortedByText { it.displayTitle } else list
        val position = HashMap<Long, Int>(saved.size * 2)
        saved.forEachIndexed { i, id -> position.putIfAbsent(id, i) }
        val known = list.filter { it.id in position }.sortedBy { position[it.id] }
        val fresh = list.filter { it.id !in position }.sortedByText { it.displayTitle }
        return known + fresh
    }

    /**
     * Sort orders: 0 A-Z, 1 artist, 2 album, 3 most recent, 4 longest,
     * 5 most played, 6 Z-A. (7, custom, goes through [sortedFor].)
     */
    fun sorted(list: List<Song>, order: Int): List<Song> = when (order) {
        1 -> list.sortedByText { it.displayArtist }
        2 -> list.sortedByText { it.displayAlbum }
        3 -> list.sortedByDescending { it.dateAdded }
        4 -> list.sortedByDescending { it.durationMs }
        5 -> {
            val counts = Prefs.playCounts()
            list.map { Triple(counts[it.id] ?: 0, sortKey(it.displayTitle), it) }
                .sortedWith(compareByDescending<Triple<Int, String, Song>> { it.first }.thenBy { it.second })
                .map { it.third }
        }
        6 -> list.sortedByText { it.displayTitle }.reversed()
        else -> {
            val d = derived
            if (list === d.songs) d.byTitle else list.sortedByText { it.displayTitle }
        }
    }

    /** Collections derived from the visible songs; built once per library change, on first use. */
    private class Derived(val songs: List<Song>) {
        val byTitle: List<Song> by lazy { songs.sortedByText { it.displayTitle } }

        val albums: List<Group> by lazy {
            songs.groupBy { it.albumId }.map { (albumId, list) ->
                val first = list.first()
                val artists = list.map { it.displayArtist }.distinct()
                Group(
                    Kind.ALBUM, albumId.toString(), first.displayAlbum,
                    if (artists.size == 1) artists[0] else "",
                    list.sortedWith(compareBy({ it.track }, { it.displayTitle.lowercase() }))
                )
            }.sortedByText { it.title }
        }

        val artists: List<Group> by lazy {
            songs.groupBy { it.displayArtist }.map { (name, list) ->
                Group(Kind.ARTIST, name, name, "", list.sortedByText { it.displayTitle })
            }.sortedByText { it.title }
        }

        val folders: List<Group> by lazy {
            songs.groupBy { it.folderPath }.map { (path, list) ->
                Group(Kind.FOLDER, path, list.first().folderName, "", list.sortedByText { it.displayTitle })
            }.sortedByText { it.title }
        }
    }

    @Volatile
    private var derived = Derived(emptyList())

    fun albums(): List<Group> = derived.albums

    fun artists(): List<Group> = derived.artists

    fun folders(): List<Group> = derived.folders

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

/** Sorts by the accent-free text of [text], computing each key only once. */
private fun <T> List<T>.sortedByText(text: (T) -> String): List<T> =
    map { Library.sortKey(text(it)) to it }.sortedBy { it.first }.map { it.second }
