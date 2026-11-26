package com.missionbreak56.musicplayer.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.missionbreak56.musicplayer.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/** The newest release published on GitHub. [apkUrl] is null when the release carries no arm64 APK. */
class ReleaseInfo(
    val version: String,
    val notes: String,
    val apkUrl: String?,
    val apkSize: Long,
    val sha256: String?,
    val pageUrl: String
)

/**
 * In-app updates: asks GitHub for the latest release, downloads its APK into the app's private cache
 * (checking size and checksum) and hands it to the system installer through a PackageInstaller session.
 * Android still shows its own confirmation, and refuses an APK signed with another key.
 */
object AppUpdater {
    private const val LATEST = "https://api.github.com/repos/MissionBreak56/MissionPlayer/releases/latest"
    const val ACTION_RESULT = "com.missionbreak56.musicplayer.UPDATE_RESULT"

    /** The automatic check runs once per app start. */
    @Volatile
    var autoChecked = false

    // ----- Pure rules (unit tested) -----

    fun compareVersions(first: String, second: String): Int {
        fun parts(v: String) = v.trim().removePrefix("v").removePrefix("V").substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val a = parts(first)
        val b = parts(second)
        for (i in 0 until maxOf(a.size, b.size)) {
            val c = a.getOrElse(i) { 0 }.compareTo(b.getOrElse(i) { 0 })
            if (c != 0) return c
        }
        return 0
    }

    /** GitHub's "latest release" answer -> [ReleaseInfo]. Throws IOException when it is not a usable release. */
    fun parse(json: String): ReleaseInfo {
        val o = try {
            JSONObject(json)
        } catch (e: JSONException) {
            throw IOException("The answer is not valid JSON", e)
        }
        val tag = o.optString("tag_name").removePrefix("v").removePrefix("V")
        if (tag.isBlank()) throw IOException("Release has no version tag")
        var url: String? = null
        var size = -1L
        var sha: String? = null
        val assets = o.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val a = assets.optJSONObject(i) ?: continue
                val name = a.optString("name")
                if (name.endsWith(".apk", ignoreCase = true) && name.contains("arm64-v8a", ignoreCase = true)) {
                    url = a.optString("browser_download_url").takeIf { it.isNotBlank() }
                    size = a.optLong("size", -1L)
                    sha = a.optString("digest").takeIf { it.startsWith("sha256:") }?.removePrefix("sha256:")?.lowercase()
                    break
                }
            }
        }
        return ReleaseInfo(tag, o.optString("body"), url, size, sha, o.optString("html_url"))
    }

    // ----- Network -----

    /** Blocking: asks GitHub for the latest release. */
    fun fetchLatest(): ReleaseInfo {
        val c = URL(LATEST).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 10_000
        c.setRequestProperty("Accept", "application/vnd.github+json")
        c.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        try {
            if (c.responseCode != HttpURLConnection.HTTP_OK) throw IOException("GitHub API HTTP ${c.responseCode}")
            return parse(c.inputStream.bufferedReader().use { it.readText() })
        } finally {
            c.disconnect()
        }
    }

    /** Blocking: the latest release when it is newer than the installed version, otherwise null. */
    fun latestIfNewer(): ReleaseInfo? {
        val r = fetchLatest()
        return if (compareVersions(r.version, BuildConfig.VERSION_NAME) > 0) r else null
    }

    /**
     * Downloads the APK of [info] to the private cache. [onProgress] gets (bytes so far, total or -1) on a
     * background thread. Verified before returning: size, SHA-256 when GitHub gives one, and that it is this
     * app in a newer version. Cancelling the coroutine stops the download.
     */
    suspend fun download(context: Context, info: ReleaseInfo, onProgress: (Long, Long) -> Unit): File =
        withContext(Dispatchers.IO) {
            val url = info.apkUrl ?: throw IOException("No APK in this release")
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val part = File(dir, "MissionPlayer-${info.version}.apk.part")
            val target = File(dir, "MissionPlayer-${info.version}.apk")
            val c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 15_000
            c.readTimeout = 30_000
            try {
                if (c.responseCode != HttpURLConnection.HTTP_OK) throw IOException("Download HTTP ${c.responseCode}")
                val total = if (c.contentLengthLong > 0) c.contentLengthLong else info.apkSize
                val sha = MessageDigest.getInstance("SHA-256")
                var done = 0L
                c.inputStream.use { input ->
                    part.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            coroutineContext.ensureActive()
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            sha.update(buf, 0, n)
                            done += n
                            onProgress(done, total)
                        }
                    }
                }
                if (info.apkSize > 0 && done != info.apkSize) throw IOException("Incomplete download")
                val hex = sha.digest().joinToString("") { "%02x".format(it) }
                if (info.sha256 != null && info.sha256 != hex) throw IOException("Checksum mismatch")
                if (!part.renameTo(target)) throw IOException("Cannot store the download")
                checkArchive(context, target)
                target
            } catch (e: Exception) {
                part.delete()
                target.delete()
                throw e
            } finally {
                c.disconnect()
            }
        }

    private fun checkArchive(context: Context, apk: File) {
        val pm = context.packageManager
        val archive = pm.getPackageArchiveInfo(apk.path, 0) ?: throw IOException("The file is not a valid APK")
        if (archive.packageName != context.packageName) throw IOException("The APK belongs to another app")
        val installed = PackageInfoCompat.getLongVersionCode(pm.getPackageInfo(context.packageName, 0))
        if (PackageInfoCompat.getLongVersionCode(archive) <= installed) throw IOException("The APK is not newer")
    }

    /** Hands [apk] to the system installer. The result comes to [UpdateInstallReceiver]. */
    fun install(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(context.packageName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            apk.inputStream().use { input ->
                session.openWrite("update.apk", 0, apk.length()).use { out ->
                    input.copyTo(out, 64 * 1024)
                    session.fsync(out)
                }
            }
            val intent = Intent(context, UpdateInstallReceiver::class.java).setAction(ACTION_RESULT)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            session.commit(PendingIntent.getBroadcast(context, id, intent, flags).intentSender)
        }
    }
}
