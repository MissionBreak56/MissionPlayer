package com.missionbreak56.musicplayer.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.missionbreak56.musicplayer.BuildConfig
import com.missionbreak56.musicplayer.R
import com.missionbreak56.musicplayer.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle(R.string.about)

        binding.textAppName.text = getString(R.string.app_name)
        binding.textVersion.text = getString(R.string.version_format, BuildConfig.VERSION_NAME)
        binding.textDeveloper.text =
            getString(R.string.developer_format, getString(R.string.developer_name))
        binding.textAboutBody.text = getString(R.string.about_body)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
