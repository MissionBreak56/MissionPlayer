package com.missionbreak56.musicplayer.online

import org.json.JSONException
import org.json.JSONObject

/**
 * One picture offered by the Cover Art Archive. [fromOtherEdition] is true when it belongs to another
 * edition of the same album (same release group), not to the release the metadata came from: the UI
 * says so, because the two editions can have different covers.
 */
class CoverOption(
    val id: String,
    val thumbUrl: String,
    val fullUrl: String,
    val isFront: Boolean,
    val types: List<String>,
    val fromOtherEdition: Boolean
)

/** Client of the Cover Art Archive (https://musicbrainz.org/doc/Cover_Art_Archive/API). */
class CoverArtClient(private val http: HttpFetcher) {

    /** Pictures of one release. An empty list means the release has none (the archive answers 404). */
    suspend fun forRelease(releaseId: String): List<CoverOption> = list("release/$releaseId", false)

    /** Pictures any edition of the album has; used only when the chosen edition has none. */
    suspend fun forReleaseGroup(groupId: String): List<CoverOption> = list("release-group/$groupId", true)

    private suspend fun list(path: String, other: Boolean): List<CoverOption> {
        val body = try {
            http.get("https://coverartarchive.org/$path", maxBytes = 2 * 1024 * 1024)
        } catch (e: OnlineException) {
            if (e.error == OnlineError.NOT_FOUND) return emptyList()
            throw e
        }
        return parse(String(body, Charsets.UTF_8), other)
    }

    /** Downloads one picture (limited to 15 MB). */
    suspend fun download(url: String): ByteArray = http.get(url, accept = "image/*", maxBytes = 15 * 1024 * 1024)

    companion object {
        /** Archive JSON -> options, front covers first. Throws [OnlineException] when the JSON is not what the archive sends. */
        fun parse(json: String, fromOtherEdition: Boolean): List<CoverOption> {
            val root = try {
                JSONObject(json)
            } catch (e: JSONException) {
                throw OnlineException(OnlineError.INVALID_RESPONSE, "The answer is not valid JSON", cause = e)
            }
            val images = root.optJSONArray("images")
                ?: throw OnlineException(OnlineError.INVALID_RESPONSE, "The answer has no images list")
            val out = ArrayList<CoverOption>()
            for (i in 0 until images.length()) {
                val img = images.optJSONObject(i) ?: continue
                val full = img.optString("image", "")
                if (full.isBlank()) continue
                val thumbs = img.optJSONObject("thumbnails")
                fun thumb(vararg names: String): String? =
                    names.firstNotNullOfOrNull { n -> thumbs?.optString(n, "")?.takeIf { it.isNotBlank() } }
                val types = ArrayList<String>()
                img.optJSONArray("types")?.let { a -> for (k in 0 until a.length()) types += a.optString(k, "") }
                out += CoverOption(
                    id = img.optString("id", i.toString()),
                    thumbUrl = UrlConnectionFetcher.toHttps(thumb("500", "large", "250", "small") ?: full),
                    // 1200 px is plenty for an embedded cover; the original can be many megabytes.
                    fullUrl = UrlConnectionFetcher.toHttps(thumb("1200", "large", "500") ?: full),
                    isFront = img.optBoolean("front", false) || types.any { it.equals("Front", ignoreCase = true) },
                    types = types,
                    fromOtherEdition = fromOtherEdition
                )
            }
            return out.sortedWith(compareByDescending<CoverOption> { it.isFront }).take(16)
        }
    }
}
