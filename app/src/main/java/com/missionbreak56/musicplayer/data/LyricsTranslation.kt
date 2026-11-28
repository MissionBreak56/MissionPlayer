package com.missionbreak56.musicplayer.data

import android.content.Context
import android.text.Html
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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
    suspend fun translateBatch(texts: List<String>, targetLanguage: String): List<String> =
        texts.map { translate(it, targetLanguage) }
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
            val language = URLEncoder.encode("autodetect|$targetLanguage", "UTF-8")
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
    private val provider: LyricsTranslationProvider = FallbackLyricsTranslationProvider()
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
            // Group verses to avoid one HTTP round-trip per lyric line.
            val uniqueTexts = lines.map { it.text.trim() }
                .filter { it.isNotBlank() && it !in setOf("♪", "♫", "♬") }
                .distinct()
            val batches = mutableListOf<List<String>>()
            var currentBatch = mutableListOf<String>()
            var currentLength = 0
            uniqueTexts.forEach { text ->
                val extraLength = text.length + 24
                if (currentBatch.isNotEmpty() &&
                    (currentLength + extraLength > 1_200 || currentBatch.size >= 8)
                ) {
                    batches += currentBatch
                    currentBatch = mutableListOf()
                    currentLength = 0
                }
                currentBatch += text
                currentLength += extraLength
            }
            if (currentBatch.isNotEmpty()) batches += currentBatch

            val semaphore = Semaphore(3)
            val translatedByText = coroutineScope {
                batches.map { batch ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            val translatedBatch = provider.translateBatch(batch, targetLanguage)
                            if (translatedBatch.size != batch.size || translatedBatch.any { it.isBlank() }) {
                                throw IOException("Incomplete translation batch")
                            }
                            batch.zip(translatedBatch)
                        }
                    }
                }.map { it.await() }.flatten().toMap()
            }
            val results = lines.map { line ->
                val text = line.text.trim()
                if (text.isBlank() || text in setOf("♪", "♫", "♬")) "" else translatedByText[text].orEmpty()
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


/**
 * Google Translate's web endpoint is a secondary provider. It is undocumented and can change
 * or throttle requests, so it must never be the only translation path.
 */
/**
 * Google web endpoint; supports batching several verses into a single HTTP request.
 * It is undocumented and may throttle requests, so independent fallbacks remain available.
 */
class GoogleWebTranslationProvider : LyricsTranslationProvider {
    override suspend fun translateBatch(texts: List<String>, targetLanguage: String): List<String> =
        withContext(Dispatchers.IO) {
            if (texts.isEmpty()) return@withContext emptyList()
            if (texts.size == 1) return@withContext listOf(translate(texts[0], targetLanguage))
            val marker = "__MP_LINE_SPLIT_7F3A__"
            val combined = texts.joinToString("\n$marker\n")
            val target = targetLanguage.substringBefore('-').ifBlank { "pt" }
            val query = URLEncoder.encode(combined, "UTF-8")
            val url = URL(
                "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$target&dt=t&q=$query"
            )
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 5_000
                connection.readTimeout = 12_000
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 MissionPlayer")
                val code = connection.responseCode
                if (code !in 200..299) throw IOException("Google translation HTTP $code")
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val segments = JSONArray(body).optJSONArray(0)
                    ?: throw IOException("Invalid Google translation response")
                val translated = buildString {
                    for (index in 0 until segments.length()) {
                        val segment = segments.optJSONArray(index) ?: continue
                        append(segment.optString(0))
                    }
                }.trim()
                val parts = translated.split(marker).map { it.trim() }
                if (parts.size != texts.size || parts.any { it.isBlank() }) {
                    throw IOException("Google translation changed the verse separators")
                }
                parts
            } finally {
                connection.disconnect()
            }
        }

    override suspend fun translate(text: String, targetLanguage: String): String =
        withContext(Dispatchers.IO) {
            if (text.isBlank()) return@withContext ""
            val target = targetLanguage.substringBefore('-').ifBlank { "pt" }
            val query = URLEncoder.encode(text, "UTF-8")
            val url = URL(
                "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$target&dt=t&q=$query"
            )
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 5_000
                connection.readTimeout = 12_000
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 MissionPlayer")
                val code = connection.responseCode
                if (code !in 200..299) throw IOException("Google translation HTTP $code")
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val segments = JSONArray(body).optJSONArray(0)
                    ?: throw IOException("Invalid Google translation response")
                val translated = buildString {
                    for (index in 0 until segments.length()) {
                        val segment = segments.optJSONArray(index) ?: continue
                        append(segment.optString(0))
                    }
                }.trim()
                if (translated.isBlank()) throw IOException("Google translation returned an empty result")
                translated
            } finally {
                connection.disconnect()
            }
        }
}

/**
 * Apertium is a rule-based fallback with a limited language-pair catalogue. Restrict it to
 * English-looking input because this public APY endpoint requires an explicit source language.
 */
class ApertiumEnglishPortugueseProvider : LyricsTranslationProvider {
    override suspend fun translate(text: String, targetLanguage: String): String =
        withContext(Dispatchers.IO) {
            if (text.isBlank()) return@withContext ""
            if (!looksLikeEnglish(text)) throw IOException("Apertium supports English source text only")
            val query = URLEncoder.encode(text, "UTF-8")
            val url = URL(
                "https://apertium.org/apy/translate?langpair=en%7Cpt&markUnknown=no&format=txt&q=$query"
            )
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 5_000
                connection.readTimeout = 10_000
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "MissionPlayer/2.7")
                val code = connection.responseCode
                if (code !in 200..299) throw IOException("Apertium translation HTTP $code")
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val json = JSONObject(body)
                if (json.optInt("responseStatus", 0) != 200) {
                    throw IOException(json.optString("responseDetails", "Apertium unavailable"))
                }
                val translated = json.optJSONObject("responseData")
                    ?.optString("translatedText").orEmpty().trim()
                if (translated.isBlank()) throw IOException("Apertium returned an empty result")
                translated
            } finally {
                connection.disconnect()
            }
        }

    private fun looksLikeEnglish(text: String): Boolean {
        val words = text.lowercase().split(Regex("[^a-z']+")).filter { it.length > 1 }
        if (words.size < 3) return false
        val common = setOf(
            "the", "and", "you", "that", "this", "with", "for", "are", "was", "have",
            "your", "not", "but", "all", "can", "will", "love", "know", "when", "what",
            "from", "they", "she", "him", "her", "our", "out", "one", "who", "how"
        )
        return words.count { it in common } >= 2
    }
}

/** Tries independent providers in order and propagates cancellation immediately. */
class FallbackLyricsTranslationProvider(
    private val providers: List<LyricsTranslationProvider> = listOf(
        GoogleWebTranslationProvider(),
        MyMemoryTranslationProvider(),
        ApertiumEnglishPortugueseProvider()
    )
) : LyricsTranslationProvider {
    override suspend fun translate(text: String, targetLanguage: String): String =
        translateBatch(listOf(text), targetLanguage).first()

    override suspend fun translateBatch(texts: List<String>, targetLanguage: String): List<String> {
        var lastError: Exception? = null
        for (provider in providers) {
            try {
                val result = provider.translateBatch(texts, targetLanguage).map { it.trim() }
                if (result.size == texts.size && result.all { it.isNotBlank() }) return result
                lastError = IOException("Translation provider returned an incomplete batch")
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                lastError = error
            }
        }
        throw IOException("All translation providers failed", lastError)
    }
}
