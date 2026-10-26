package com.missionbreak56.musicplayer.online

import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder

/** Album covers from the iTunes Search service (no account needed): a second source next to the Cover Art Archive. */
class ItunesCoverClient(private val http: HttpFetcher) {

    suspend fun search(term: String): List<CoverOption> {
        val q = URLEncoder.encode(term, "UTF-8")
        val body = http.get("https://itunes.apple.com/search?term=$q&media=music&entity=album&limit=12", maxBytes = 2 * 1024 * 1024)
        return parse(String(body, Charsets.UTF_8))
    }

    companion object {
        fun parse(json: String): List<CoverOption> {
            val root = try {
                JSONObject(json)
            } catch (e: JSONException) {
                throw OnlineException(OnlineError.INVALID_RESPONSE, "The answer is not valid JSON", cause = e)
            }
            val results = root.optJSONArray("results") ?: return emptyList()
            val seen = HashSet<String>()
            val out = ArrayList<CoverOption>()
            for (i in 0 until results.length()) {
                val o = results.optJSONObject(i) ?: continue
                val small = o.optString("artworkUrl100", "")
                if (small.isBlank() || !seen.add(small)) continue
                out += CoverOption(
                    id = "itunes-" + o.optString("collectionId", i.toString()),
                    thumbUrl = UrlConnectionFetcher.toHttps(small.replace("100x100bb", "300x300bb")),
                    fullUrl = UrlConnectionFetcher.toHttps(small.replace("100x100bb", "1200x1200bb")),
                    isFront = true,
                    types = listOf("Front"),
                    fromOtherEdition = false
                )
            }
            return out
        }
    }
}
