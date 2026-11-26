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
import com.missionbreak56.musicplayer.update.AppUpdater
import com.missionbreak56.musicplayer.update.ReleaseInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AboutActivity : BaseActivity() {

    private lateinit var binding: ActivityAboutBinding
    private var latest: ReleaseInfo? = null

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
            latest?.let { UpdateFlow.start(this, it) }
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
                val release = withContext(Dispatchers.IO) { AppUpdater.fetchLatest() }
                val latestVersion = release.version
                latest = release
                if (AppUpdater.compareVersions(latestVersion, BuildConfig.VERSION_NAME) > 0) {
                    binding.textUpdateStatus.text = getString(
                        R.string.update_available,
                        latestVersion,
                        BuildConfig.VERSION_NAME
                    )
                    binding.btnDownloadUpdate.visibility = View.VISIBLE
                } else {
                    latest = null
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

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(this, R.string.update_open_link_failed, Toast.LENGTH_SHORT).show()
        }
    }

}
