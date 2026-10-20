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
    private const val KEY_LISTEN = "listen_ms"
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
    private const val KEY_PLAYER_THEME = "player_theme"
    private const val KEY_AUTO_LYRICS = "auto_lyrics"
    private const val KEY_ACCENT = "custom_accent"
    private const val KEY_FONT = "custom_font"
    private const val KEY_CORNERS = "custom_corners"
    private const val KEY_BG = "custom_background"
    private const val KEY_ICON = "custom_icon"
    private const val KEY_APP_NAME = "custom_app_name"
    private const val KEY_BG_IMAGE = "custom_bg_image"
    private const val KEY_BG_VISIBLE = "custom_bg_image_visible"

    /** Default look: custom deep blue accent (hue 220, "Escuro"), system font, pure background. */
    const val DEFAULT_ACCENT = 544
    const val DEFAULT_FONT = 9
    const val DEFAULT_BG = 1

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

    /** Look of the full player: 0 liquid glass (default), 1 AMOLED, 2 blurred cover, 3 aurora, 4 solid, 5 graphite. */
    var playerTheme: Int
        get() = (sp?.getInt(KEY_PLAYER_THEME, 0) ?: 0).coerceIn(0, 5)
        set(value) { store().edit().putInt(KEY_PLAYER_THEME, value).apply() }

    /**
     * The theme picker was removed: everyone uses the base theme with the personalization settings.
     * Someone who had picked another theme starts from the default look instead.
     */
    fun migrateThemeRemoval() {
        val st = store()
        if (st.getInt(KEY_THEME, 0) != 0) {
            st.edit().putInt(KEY_THEME, 0).putInt(KEY_ACCENT, DEFAULT_ACCENT)
                .putInt(KEY_FONT, DEFAULT_FONT).apply()
        }
    }

    var theme: Int
        get() = sp?.getInt(KEY_THEME, 0) ?: 0
        set(value) { store().edit().putInt(KEY_THEME, value).apply() }

    var nightMode: Int
        get() = sp?.getInt(KEY_NIGHT, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        set(value) { store().edit().putInt(KEY_NIGHT, value).apply() }

    /** Download missing lyrics automatically (needs internet only for the download). */
    var autoLyrics: Boolean
        get() = sp?.getBoolean(KEY_AUTO_LYRICS, true) ?: true
        set(value) { store().edit().putBoolean(KEY_AUTO_LYRICS, value).apply() }

    // ----- Advanced customization (0 / -1 = the theme's own look) -----

    /** Accent color override: -1 follows the theme, 0.. picks one of the palette colors. */
    var accent: Int
        get() = (sp?.getInt(KEY_ACCENT, DEFAULT_ACCENT) ?: DEFAULT_ACCENT).let { if (it < 0) DEFAULT_ACCENT else it }
        set(value) { store().edit().putInt(KEY_ACCENT, value).apply() }

    var fontStyle: Int
        get() = (sp?.getInt(KEY_FONT, DEFAULT_FONT) ?: DEFAULT_FONT).let { if (it == 0) DEFAULT_FONT else it }
        set(value) { store().edit().putInt(KEY_FONT, value).apply() }

    var cornerStyle: Int
        get() = sp?.getInt(KEY_CORNERS, 0) ?: 0
        set(value) { store().edit().putInt(KEY_CORNERS, value).apply() }

    /** 1 = pure (pure black in dark mode) is the default; the old "default" option (0) was removed. */
    var backgroundStyle: Int
        get() = (sp?.getInt(KEY_BG, DEFAULT_BG) ?: DEFAULT_BG).let { if (it == 0) DEFAULT_BG else it }
        set(value) { store().edit().putInt(KEY_BG, value).apply() }

    /** Which launcher icon is active (0 = original). */
    var iconVariant: Int
        get() = sp?.getInt(KEY_ICON, 0) ?: 0
        set(value) { store().edit().putInt(KEY_ICON, value).apply() }

    /** Name chosen in Customization. Empty = the app's own name. */
    var appName: String
        get() = sp?.getString(KEY_APP_NAME, "") ?: ""
        set(value) { store().edit().putString(KEY_APP_NAME, value.trim()).apply() }

    /** Time the gallery background image was saved (0 = none). Changes whenever a new image is set. */
    var bgImageStamp: Long
        get() = sp?.getLong(KEY_BG_IMAGE, 0L) ?: 0L
        set(value) { store().edit().putLong(KEY_BG_IMAGE, value).apply() }

    /** How much of the gallery image shows through the background color, 10..100 (%). */
    var bgImageVisible: Int
        get() = sp?.getInt(KEY_BG_VISIBLE, 55) ?: 55
        set(value) { store().edit().putInt(KEY_BG_VISIBLE, value.coerceIn(10, 100)).apply() }

    /** Changes whenever a look setting changes, so open screens know to redraw. */
    fun customSignature(): String = "$accent|$fontStyle|$cornerStyle|$backgroundStyle|$bgImageStamp|$bgImageVisible"

    fun resetCustomization() {
        store().edit().putInt(KEY_ACCENT, DEFAULT_ACCENT).putInt(KEY_FONT, DEFAULT_FONT).putInt(KEY_CORNERS, 0)
            .putInt(KEY_BG, DEFAULT_BG).putInt(KEY_THEME, 0).putInt(KEY_NIGHT, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM).putLong(KEY_BG_IMAGE, 0L).putInt(KEY_BG_VISIBLE, 55).putString(KEY_APP_NAME, "").apply()
    }

    /** Sort order chosen inside albums / folders / artists / playlists (-1 = their own order). */
    fun detailSort(kind: String): Int = sp?.getInt("detail_sort_$kind", -1) ?: -1

    fun setDetailSort(kind: String, order: Int) {
        store().edit().putInt("detail_sort_$kind", order).apply()
    }

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

    /** Adds the songs that are not there yet. Returns how many were added, or -1 for an empty name. */
    fun addAllToPlaylist(name: String, songIds: List<Long>): Int {
        val safe = safePlaylistName(name)
        if (safe.isEmpty()) return -1
        val map = playlists()
        val list = map.getOrPut(safe) { mutableListOf() }
        var added = 0
        songIds.forEach { id ->
            if (!list.contains(id)) {
                list.add(id)
                added++
            }
        }
        savePlaylists(map)
        return added
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

    /** Exact time heard per song, in milliseconds. */
    fun listenedMs(): Map<Long, Long> {
        val raw = sp?.getString(KEY_LISTEN, "") ?: ""
        if (raw.isBlank()) return emptyMap()
        val map = HashMap<Long, Long>()
        raw.split(",").forEach { entry ->
            val parts = entry.split(":")
            val id = parts.getOrNull(0)?.toLongOrNull()
            val ms = parts.getOrNull(1)?.toLongOrNull()
            if (id != null && ms != null) map[id] = ms
        }
        return map
    }

    fun addListened(delta: Map<Long, Long>) {
        if (delta.isEmpty()) return
        val map = listenedMs().toMutableMap()
        delta.forEach { (id, ms) -> map[id] = (map[id] ?: 0L) + ms }
        store().edit()
            .putString(KEY_LISTEN, map.entries.joinToString(",") { "${it.key}:${it.value}" })
            .apply()
    }

    /** Clears play counts and listening time (the "recently played" list is kept). */
    fun resetStats() {
        store().edit().remove(KEY_COUNTS).remove(KEY_LISTEN).apply()
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
