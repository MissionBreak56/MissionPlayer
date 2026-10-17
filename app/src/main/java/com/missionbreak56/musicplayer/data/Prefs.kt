package com.missionbreak56.musicplayer.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Tiny SharedPreferences wrapper: premium flag, favorites and simple playlists.
 * Everything is stored on-device; nothing is synced or verified remotely.
 */
object Prefs {

    private const val FILE = "mission_player_prefs"
    private const val KEY_PREMIUM = "premium"
    private const val KEY_PREMIUM_INIT = "premium_initialised"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_PLAYLISTS = "playlists"

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

    /** Always true unless explicitly changed. There is no server to ask. */
    fun isPremium(): Boolean = sp?.getBoolean(KEY_PREMIUM, true) ?: true

    var premium: Boolean
        get() = isPremium()
        set(value) { store().edit().putBoolean(KEY_PREMIUM, value).apply() }

    val premiumInitialised: Boolean
        get() = sp?.getBoolean(KEY_PREMIUM_INIT, false) ?: false

    fun markPremiumInitialised() {
        store().edit().putBoolean(KEY_PREMIUM_INIT, true).apply()
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
}
