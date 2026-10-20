package com.missionbreak56.musicplayer.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.missionbreak56.musicplayer.BuildConfig
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.databinding.ActivityAboutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AboutActivity : BaseActivity() {

    private lateinit var binding: ActivityAboutBinding
    private var latestReleaseUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }

        binding.textAppName.text = AppName.get(this)
        binding.textVersion.text = getString(R.string.version_format, BuildConfig.VERSION_NAME)
        binding.textDeveloper.text =
            getString(R.string.developer_format, getString(R.string.developer_name))
        binding.textAboutBody.text = getString(R.string.about_body)
        binding.textUpdateStatus.text = getString(
            R.string.update_current_version,
            BuildConfig.VERSION_NAME
        )
        binding.btnCheckUpdates.setOnClickListener { checkForUpdates() }
        binding.btnDownloadUpdate.setOnClickListener {
            latestReleaseUrl?.let { openUrl(it) }
        }
        binding.textRepo.setOnClickListener {
            openUrl(getString(R.string.repo_url))
        }
    }

    private fun checkForUpdates() {
        binding.btnCheckUpdates.isEnabled = false
        binding.btnDownloadUpdate.visibility = View.GONE
        binding.textUpdateStatus.text = getString(R.string.update_checking)

        lifecycleScope.launch {
            try {
                val release = withContext(Dispatchers.IO) { fetchLatestRelease() }
                val latestVersion = release.version
                latestReleaseUrl = release.downloadUrl
                if (compareVersions(latestVersion, BuildConfig.VERSION_NAME) > 0) {
                    binding.textUpdateStatus.text = getString(
                        R.string.update_available,
                        latestVersion,
                        BuildConfig.VERSION_NAME
                    )
                    binding.btnDownloadUpdate.visibility = View.VISIBLE
                } else {
                    latestReleaseUrl = null
                    binding.textUpdateStatus.text = getString(
                        R.string.update_up_to_date,
                        BuildConfig.VERSION_NAME
                    )
                }
            } catch (_: Exception) {
                binding.textUpdateStatus.text = getString(R.string.update_check_failed)
                Toast.makeText(
                    this@AboutActivity,
                    R.string.update_check_failed,
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.btnCheckUpdates.isEnabled = true
            }
        }
    }

    private fun fetchLatestRelease(): ReleaseInfo {
        val connection = (URL(
            "https://api.github.com/repos/MissionBreak56/MissionPlayer/releases/latest"
        ).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("GitHub API HTTP ${connection.responseCode}")
            }
            val json = connection.inputStream.bufferedReader().use { it.readText() }
            val release = JSONObject(json)
            val tag = release.optString("tag_name").removePrefix("v").removePrefix("V")
            require(tag.isNotBlank()) { "Release has no version tag" }
            val assets = release.optJSONArray("assets")
            var apkUrl: String? = null
            if (assets != null) {
                for (index in 0 until assets.length()) {
                    val asset = assets.optJSONObject(index) ?: continue
                    val name = asset.optString("name")
                    if (name.endsWith(".apk", ignoreCase = true) &&
                        name.contains("arm64-v8a", ignoreCase = true)
                    ) {
                        apkUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                        break
                    }
                }
            }
            val releasePage = release.optString("html_url")
            return ReleaseInfo(tag, apkUrl ?: releasePage)
        } finally {
            connection.disconnect()
        }
    }

    private fun compareVersions(first: String, second: String): Int {
        fun parts(value: String) = value.trim().removePrefix("v").removePrefix("V")
            .substringBefore('-').split('.')
            .map { it.toIntOrNull() ?: 0 }

        val a = parts(first)
        val b = parts(second)
        for (index in 0 until maxOf(a.size, b.size)) {
            val comparison = (a.getOrElse(index) { 0 }).compareTo(b.getOrElse(index) { 0 })
            if (comparison != 0) return comparison
        }
        return 0
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(this, R.string.update_open_link_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private data class ReleaseInfo(val version: String, val downloadUrl: String)
}
