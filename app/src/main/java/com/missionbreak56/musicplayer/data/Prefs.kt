package com.missionbreak56.musicplayer.data

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

/**
 * Tiny SharedPreferences wrapper: favorites, playlists, play statistics,
 * appearance and equalizer settings. Everything is stored on-device.
 */
object Prefs {

    private const val FILE = "mission_player_prefs"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_PLAYLISTS = "playlists"
    private const val KEY_COUNTS = "play_counts"
    private const val KEY_RECENT = "recent"
    private const val KEY_THEME = "theme"
    private const val KEY_NIGHT = "night_mode"
    private const val KEY_SORT = "sort_order"
    private const val KEY_TAB_ORDER = "tab_order"
    private const val KEY_HIDDEN_SONGS = "hidden_songs"
    private const val KEY_HIDDEN_FOLDERS = "hidden_folders"
    private const val KEY_FX_ENABLED = "fx_enabled"
    private const val KEY_FX_PRESET = "fx_preset"
    private const val KEY_FX_LEVELS = "fx_levels"
    private const val KEY_FX_BASS = "fx_bass"
    private const val KEY_FX_VIRT = "fx_virtualizer"
    private const val KEY_FX_GAIN = "fx_gain"
    private const val KEY_SHUFFLE = "play_shuffle"
    private const val KEY_REPEAT = "play_repeat"
    private const val KEY_LAST_QUEUE = "last_queue"
    private const val KEY_LAST_INDEX = "last_index"
    private const val KEY_LAST_POS = "last_position"

    private const val SEP_ENTRY = "\n"
    private const val SEP_FIELD = "|"
    private const val SEP_ID = ","

    @Volatile
    private var sp: SharedPreferences? = null

    fun init(context: Context) {
        if (sp == null) {
            sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        }
    }

    private fun store(): SharedPreferences = sp ?: error("Prefs.init() was not called")

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        store().registerOnSharedPreferenceChangeListener(listener)
    }

    // ----- Appearance / list options -----

    var theme: Int
        get() = sp?.getInt(KEY_THEME, 0) ?: 0
        set(value) { store().edit().putInt(KEY_THEME, value).apply() }

    var nightMode: Int
        get() = sp?.getInt(KEY_NIGHT, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        set(value) { store().edit().putInt(KEY_NIGHT, value).apply() }

    var sortOrder: Int
        get() = sp?.getInt(KEY_SORT, 0) ?: 0
        set(value) { store().edit().putInt(KEY_SORT, value).apply() }

    // ----- Tab order -----

    /** Ids of the library tabs in display order: 0 songs, 1 albums, 2 artists, 3 folders. */
    fun tabOrder(): List<Int> {
        val default = listOf(0, 1, 2, 3)
        val saved = (sp?.getString(KEY_TAB_ORDER, "") ?: "").split(",").mapNotNull { it.toIntOrNull() }
        return if (saved.size == default.size && saved.toSet() == default.toSet()) saved else default
    }

    fun setTabOrder(order: List<Int>) {
        store().edit().putString(KEY_TAB_ORDER, order.joinToString(",")).apply()
    }

    // ----- Hidden songs / folders -----

    fun hiddenSongs(): MutableSet<String> {
        val s = sp ?: return mutableSetOf()
        return (s.getStringSet(KEY_HIDDEN_SONGS, emptySet()) ?: emptySet()).toMutableSet()
    }

    fun hiddenFolders(): MutableSet<String> {
        val s = sp ?: return mutableSetOf()
        return (s.getStringSet(KEY_HIDDEN_FOLDERS, emptySet()) ?: emptySet()).toMutableSet()
    }

    fun setHiddenSongs(set: Set<String>) {
        store().edit().putStringSet(KEY_HIDDEN_SONGS, HashSet(set)).apply()
    }

    fun setHiddenFolders(set: Set<String>) {
        store().edit().putStringSet(KEY_HIDDEN_FOLDERS, HashSet(set)).apply()
    }

    // ----- Favorites -----

    fun favorites(): MutableSet<String> {
        val s = sp ?: return mutableSetOf()
        return (s.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()).toMutableSet()
    }

    fun isFavorite(songId: Long): Boolean = favorites().contains(songId.toString())

    /** Returns the new favorite state. */
    fun toggleFavorite(songId: Long): Boolean {
        val set = favorites()
        val key = songId.toString()
        val nowFavorite = if (set.contains(key)) {
            set.remove(key); false
        } else {
            set.add(key); true
        }
        store().edit().putStringSet(KEY_FAVORITES, set).apply()
        return nowFavorite
    }

    // ----- Playlists -----

    fun playlists(): MutableMap<String, MutableList<Long>> {
        val raw = sp?.getString(KEY_PLAYLISTS, "") ?: ""
        val map = linkedMapOf<String, MutableList<Long>>()
        if (raw.isBlank()) return map
        raw.split(SEP_ENTRY).forEach { line ->
            if (line.isBlank()) return@forEach
            val parts = line.split(SEP_FIELD)
            val name = parts.getOrNull(0).orEmpty()
            if (name.isBlank()) return@forEach
            val ids = parts.getOrNull(1)
                ?.split(SEP_ID)
                ?.mapNotNull { it.toLongOrNull() }
                ?: emptyList()
            map[name] = ids.toMutableList()
        }
        return map
    }

    fun savePlaylists(map: Map<String, List<Long>>) {
        val raw = map.entries.joinToString(SEP_ENTRY) { (name, ids) ->
            val safeName = name.replace(SEP_ENTRY, " ").replace(SEP_FIELD, "/")
            safeName + SEP_FIELD + ids.joinToString(SEP_ID)
        }
        store().edit().putString(KEY_PLAYLISTS, raw).apply()
    }

    private fun safePlaylistName(name: String): String =
        name.replace(SEP_ENTRY, " ").replace(SEP_FIELD, "/").trim()

    fun createPlaylist(name: String) {
        val safe = safePlaylistName(name)
        if (safe.isEmpty()) return
        val map = playlists()
        map.getOrPut(safe) { mutableListOf() }
        savePlaylists(map)
    }

    fun addToPlaylist(name: String, songId: Long) {
        val safe = safePlaylistName(name)
        if (safe.isEmpty()) return
        val map = playlists()
        val list = map.getOrPut(safe) { mutableListOf() }
        if (!list.contains(songId)) list.add(songId)
        savePlaylists(map)
    }

    fun removeFromPlaylist(name: String, songId: Long) {
        val map = playlists()
        map[name]?.remove(songId)
        savePlaylists(map)
    }

    fun deletePlaylist(name: String) {
        val map = playlists()
        map.remove(name)
        savePlaylists(map)
    }

    /** Returns the final name used, or null when the new name is empty. */
    fun renamePlaylist(old: String, new: String): String? {
        val safe = safePlaylistName(new)
        if (safe.isEmpty()) return null
        val map = playlists()
        val ids = map.remove(old) ?: return null
        val target = map.getOrPut(safe) { mutableListOf() }
        ids.forEach { if (!target.contains(it)) target.add(it) }
        savePlaylists(map)
        return safe
    }

    // ----- Play statistics -----

    fun playCounts(): Map<Long, Int> {
        val raw = sp?.getString(KEY_COUNTS, "") ?: ""
        if (raw.isBlank()) return emptyMap()
        val map = HashMap<Long, Int>()
        raw.split(",").forEach { entry ->
            val parts = entry.split(":")
            val id = parts.getOrNull(0)?.toLongOrNull()
            val count = parts.getOrNull(1)?.toIntOrNull()
            if (id != null && count != null) map[id] = count
        }
        return map
    }

    fun recent(): List<Long> {
        val raw = sp?.getString(KEY_RECENT, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(",").mapNotNull { it.toLongOrNull() }
    }

    fun recordPlay(songId: Long) {
        val counts = playCounts().toMutableMap()
        counts[songId] = (counts[songId] ?: 0) + 1
        val rawCounts = counts.entries.joinToString(",") { "${it.key}:${it.value}" }
        val newRecent = (listOf(songId) + recent().filter { it != songId }).take(100)
        store().edit()
            .putString(KEY_COUNTS, rawCounts)
            .putString(KEY_RECENT, newRecent.joinToString(","))
            .apply()
    }

    // ----- Playback state (shuffle / repeat / last queue) -----

    /** Last chosen shuffle mode. */
    var shuffleMode: Boolean
        get() = sp?.getBoolean(KEY_SHUFFLE, false) ?: false
        set(value) { store().edit().putBoolean(KEY_SHUFFLE, value).apply() }

    /** Last chosen repeat mode (Player.REPEAT_MODE_OFF / ONE / ALL = 0 / 1 / 2). */
    var repeatMode: Int
        get() = sp?.getInt(KEY_REPEAT, 0) ?: 0
        set(value) { store().edit().putInt(KEY_REPEAT, value).apply() }

    /** Song ids of the last queue, comma separated, in queue order. */
    var lastQueue: String
        get() = sp?.getString(KEY_LAST_QUEUE, "") ?: ""
        set(value) { store().edit().putString(KEY_LAST_QUEUE, value).apply() }

    var lastIndex: Int
        get() = sp?.getInt(KEY_LAST_INDEX, 0) ?: 0
        set(value) { store().edit().putInt(KEY_LAST_INDEX, value).apply() }

    var lastPosition: Long
        get() = sp?.getLong(KEY_LAST_POS, 0L) ?: 0L
        set(value) { store().edit().putLong(KEY_LAST_POS, value).apply() }

    fun lastQueueIds(): List<Long> =
        lastQueue.split(SEP_ID).mapNotNull { it.trim().toLongOrNull() }

    // ----- Equalizer / audio effects -----

    var fxEnabled: Boolean
        get() = sp?.getBoolean(KEY_FX_ENABLED, false) ?: false
        set(value) { store().edit().putBoolean(KEY_FX_ENABLED, value).apply() }

    /** Index of the selected preset, or -1 for custom. */
    var fxPreset: Int
        get() = sp?.getInt(KEY_FX_PRESET, -1) ?: -1
        set(value) { store().edit().putInt(KEY_FX_PRESET, value).apply() }

    var fxBass: Int
        get() = sp?.getInt(KEY_FX_BASS, 0) ?: 0
        set(value) { store().edit().putInt(KEY_FX_BASS, value).apply() }

    var fxVirtualizer: Int
        get() = sp?.getInt(KEY_FX_VIRT, 0) ?: 0
        set(value) { store().edit().putInt(KEY_FX_VIRT, value).apply() }

    /** Volume boost above the normal maximum, in millibels (0 = off). */
    var fxGain: Int
        get() = sp?.getInt(KEY_FX_GAIN, 0) ?: 0
        set(value) { store().edit().putInt(KEY_FX_GAIN, value).apply() }

    fun fxLevels(): IntArray {
        val raw = sp?.getString(KEY_FX_LEVELS, "") ?: ""
        if (raw.isBlank()) return IntArray(0)
        return raw.split(",").mapNotNull { it.toIntOrNull() }.toIntArray()
    }

    fun setFxLevels(levels: IntArray) {
        store().edit().putString(KEY_FX_LEVELS, levels.joinToString(",")).apply()
    }
}
