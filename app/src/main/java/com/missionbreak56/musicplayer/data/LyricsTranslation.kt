package com.missionbreak56.musicplayer.data

import android.content.Context
import android.text.Html
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

data class TranslatedLyricLine(
    val timeMs: Long,
    val originalText: String,
    val translatedText: String
)

/** Translation is only started by an explicit user action in the player. */
interface LyricsTranslationProvider {
    suspend fun translate(text: String, targetLanguage: String): String
}

/**
 * Uses MyMemory's public endpoint. It does not require an API key, but it has public-service
 * quotas and availability limits. Lyrics are sent to this third-party service when requested.
 */
class MyMemoryTranslationProvider : LyricsTranslationProvider {
    override suspend fun translate(text: String, targetLanguage: String): String =
        withContext(Dispatchers.IO) {
            if (text.isBlank()) return@withContext ""
            if (text.toByteArray(Charsets.UTF_8).size > 500) {
                throw IOException("Translation segment exceeds the service's 500-byte limit")
            }
            val query = URLEncoder.encode(text, "UTF-8")
            val language = URLEncoder.encode("auto|$targetLanguage", "UTF-8")
            val connection = (URL(
                "https://api.mymemory.translated.net/get?q=$query&langpair=$language"
            ).openConnection() as HttpURLConnection)
            try {
                connection.connectTimeout = 5_000
                connection.readTimeout = 10_000
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "MissionPlayer/2.7")
                val code = connection.responseCode
                if (code !in 200..299) throw IOException("Translation service HTTP $code")
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val json = JSONObject(body)
                if (json.optInt("responseStatus", 0) != 200) {
                    throw IOException(json.optString("responseDetails", "Translation unavailable"))
                }
                val translated = json.optJSONObject("responseData")
                    ?.optString("translatedText").orEmpty()
                if (translated.isBlank()) throw IOException("Translation service returned an empty result")
                if (Build.VERSION.SDK_INT >= 24) {
                    Html.fromHtml(translated, Html.FROM_HTML_MODE_LEGACY).toString()
                } else {
                    @Suppress("DEPRECATION")
                    Html.fromHtml(translated).toString()
                }
            } finally {
                connection.disconnect()
            }
        }
}

/**
 * Persistent translation cache. A changed source lyric produces a different hash and cannot
 * accidentally reuse an old translation. This store never modifies LyricsStore or LRC files.
 */
object LyricsTranslationStore {
    private fun directory(context: Context) =
        java.io.File(context.filesDir, "lyrics_translations").apply { mkdirs() }

    fun sourceHash(source: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(source.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun file(context: Context, songId: Long, hash: String, language: String) =
        java.io.File(directory(context), "${songId}_${hash}_${language.replace(Regex("[^A-Za-z0-9-]"), "")}.json")

    fun load(
        context: Context,
        songId: Long,
        source: String,
        targetLanguage: String
    ): List<String>? {
        val f = file(context, songId, sourceHash(source), targetLanguage)
        if (!f.isFile) return null
        return try {
            val json = JSONObject(f.readText())
            if (json.optString("sourceHash") != sourceHash(source) ||
                json.optString("language") != targetLanguage
            ) return null
            val array = json.optJSONArray("lines") ?: return null
            List(array.length()) { index -> array.optString(index) }
        } catch (_: Exception) {
            null
        }
    }

    fun save(
        context: Context,
        songId: Long,
        source: String,
        targetLanguage: String,
        translations: List<String>
    ) {
        val hash = sourceHash(source)
        val json = JSONObject()
            .put("sourceHash", hash)
            .put("language", targetLanguage)
            .put("lines", JSONArray(translations))
        val target = file(context, songId, hash, targetLanguage)
        val temporary = java.io.File(target.parentFile, target.name + ".tmp")
        temporary.writeText(json.toString())
        if (!temporary.renameTo(target)) {
            temporary.copyTo(target, overwrite = true)
            temporary.delete()
        }
    }

    fun clear(context: Context, songId: Long) {
        directory(context).listFiles()
            ?.filter { it.name.startsWith("${songId}_") }
            ?.forEach { it.delete() }
    }
}

/**
 * Translates parsed lines in their existing order. Timestamps and original text are copied
 * unchanged; blank and instrumental placeholder lines do not generate network requests.
 */
class LyricsTranslationManager(
    private val provider: LyricsTranslationProvider = MyMemoryTranslationProvider()
) {
    suspend fun translate(
        context: Context,
        songId: Long,
        sourceRaw: String,
        lines: List<LyricLine>,
        targetLanguage: String = "pt-BR"
    ): List<TranslatedLyricLine> {
        require(songId > 0L)
        require(targetLanguage.isNotBlank())
        val cached = LyricsTranslationStore.load(context, songId, sourceRaw, targetLanguage)
        val translated = if (cached != null && cached.size == lines.size) {
            cached
        } else {
            val results = ArrayList<String>(lines.size)
            for (line in lines) {
                val text = line.text.trim()
                results += if (text.isBlank() || text in setOf("♪", "♫", "♬")) {
                    ""
                } else {
                    provider.translate(text, targetLanguage).trim()
                }
            }
            if (results.size != lines.size || results.anyIndexed { index, value ->
                    lines[index].text.isNotBlank() && lines[index].text !in setOf("♪", "♫", "♬") && value.isBlank()
                }
            ) throw IOException("Incomplete translation response")
            LyricsTranslationStore.save(context, songId, sourceRaw, targetLanguage, results)
            results
        }
        return lines.mapIndexed { index, line ->
            TranslatedLyricLine(line.timeMs, line.text, translated[index])
        }
    }

    private inline fun <T> List<T>.anyIndexed(predicate: (Int, T) -> Boolean): Boolean {
        forEachIndexed { index, item -> if (predicate(index, item)) return true }
        return false
    }
}
