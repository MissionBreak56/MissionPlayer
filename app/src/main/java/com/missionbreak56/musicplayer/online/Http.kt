package com.missionbreak56.musicplayer.online

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class OnlineError { NETWORK, HTTP, RATE_LIMITED, NOT_FOUND, INVALID_RESPONSE }

/** A failed online lookup. The UI turns [error] into a clear message; [message] is for logs. */
class OnlineException(
    val error: OnlineError,
    message: String,
    val httpCode: Int = 0,
    cause: Throwable? = null
) : Exception(message, cause)

/** The only thing the online clients need from the network, so tests can replace it. */
interface HttpFetcher {
    /** Downloads [url] (GET). Throws [OnlineException]. Cancelling the coroutine aborts the connection. */
    suspend fun get(url: String, accept: String = "application/json", maxBytes: Int = 4 * 1024 * 1024): ByteArray
}

/** [HttpFetcher] over HttpURLConnection: https only, follows redirects by hand, size limited, cancellable. */
class UrlConnectionFetcher(private val userAgent: String) : HttpFetcher {

    override suspend fun get(url: String, accept: String, maxBytes: Int): ByteArray =
        suspendCancellableCoroutine { cont ->
            val current = AtomicReference<HttpURLConnection?>(null)
            val job = CoroutineScope(Dispatchers.IO).launch {
                try {
                    val bytes = fetch(url, accept, maxBytes, current)
                    if (cont.isActive) cont.resume(bytes)
                } catch (e: Throwable) {
                    if (cont.isActive) cont.resumeWithException(e)
                }
            }
            cont.invokeOnCancellation {
                try {
                    current.get()?.disconnect()
                } catch (_: Throwable) {
                }
                job.cancel()
            }
        }

    private fun fetch(startUrl: String, accept: String, maxBytes: Int, holder: AtomicReference<HttpURLConnection?>): ByteArray {
        var target = toHttps(startUrl)
        var hops = 0
        while (true) {
            val base = URL(target)
            val conn = try {
                base.openConnection() as HttpURLConnection
            } catch (e: IOException) {
                throw OnlineException(OnlineError.NETWORK, "Cannot connect", cause = e)
            }
            holder.set(conn)
            try {
                conn.connectTimeout = 12_000
                conn.readTimeout = 20_000
                conn.instanceFollowRedirects = false
                conn.setRequestProperty("User-Agent", userAgent)
                conn.setRequestProperty("Accept", accept)
                val code = try {
                    conn.responseCode
                } catch (e: IOException) {
                    throw OnlineException(OnlineError.NETWORK, "No connection", cause = e)
                }
                when (code) {
                    in 200..299 -> {
                        return try {
                            readLimited(conn, maxBytes)
                        } catch (e: IOException) {
                            throw OnlineException(OnlineError.NETWORK, "Connection lost", cause = e)
                        }
                    }
                    301, 302, 303, 307, 308 -> {
                        val location = conn.getHeaderField("Location")
                            ?: throw OnlineException(OnlineError.INVALID_RESPONSE, "Redirect without a destination", code)
                        if (++hops > 6) throw OnlineException(OnlineError.INVALID_RESPONSE, "Too many redirects", code)
                        target = toHttps(URL(base, location).toString())
                    }
                    404 -> throw OnlineException(OnlineError.NOT_FOUND, "Not found", code)
                    429, 503 -> throw OnlineException(OnlineError.RATE_LIMITED, "The service asked to slow down", code)
                    else -> throw OnlineException(OnlineError.HTTP, "HTTP $code", code)
                }
            } finally {
                try {
                    conn.disconnect()
                } catch (_: Throwable) {
                }
            }
        }
    }

    private fun readLimited(conn: HttpURLConnection, maxBytes: Int): ByteArray {
        val declared = conn.contentLengthLong
        if (declared > maxBytes) throw OnlineException(OnlineError.INVALID_RESPONSE, "Response too large")
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(32 * 1024)
        conn.inputStream.use { input ->
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                if (out.size() + n > maxBytes) throw OnlineException(OnlineError.INVALID_RESPONSE, "Response too large")
                out.write(buffer, 0, n)
            }
        }
        return out.toByteArray()
    }

    companion object {
        /** The services used here all speak https; links in their answers sometimes still say http. */
        fun toHttps(url: String): String =
            if (url.startsWith("http://", ignoreCase = true)) "https://" + url.substring(7) else url
    }
}
