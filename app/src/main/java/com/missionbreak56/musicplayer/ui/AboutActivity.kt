package com.missionbreak56.musicplayer.ui

import android.os.Bundle
import com.missionbreak56.musicplayer.BuildConfig
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.databinding.ActivityAboutBinding

class AboutActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }

        binding.textAppName.text = getString(R.string.app_name)
        binding.textVersion.text = getString(R.string.version_format, BuildConfig.VERSION_NAME)
        binding.textDeveloper.text =
            getString(R.string.developer_format, getString(R.string.developer_name))
        binding.textAboutBody.text = getString(R.string.about_body)
    }
}
