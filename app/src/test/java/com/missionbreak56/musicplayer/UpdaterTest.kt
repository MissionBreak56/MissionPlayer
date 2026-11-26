package com.missionbreak56.musicplayer

import com.missionbreak56.musicplayer.update.AppUpdater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class UpdaterTest {
    @Test fun comparesVersionsNumerically() {
        assertTrue(AppUpdater.compareVersions("2.23.0", "2.22.1") > 0)
        assertTrue(AppUpdater.compareVersions("v2.10.0", "2.9.9") > 0)
        assertEquals(0, AppUpdater.compareVersions("2.23", "2.23.0"))
        assertTrue(AppUpdater.compareVersions("2.22.1", "2.23.0") < 0)
    }

    @Test fun parsesTheArm64AssetWithSizeAndChecksum() {
        val json = """{"tag_name":"v2.23.0","body":"## Novidades\n- x","html_url":"https://github.com/x/y/releases/tag/v2.23.0",
          "assets":[{"name":"notes.txt","size":5,"browser_download_url":"https://x/notes.txt"},
                    {"name":"MissionPlayer-v2.23.0-arm64-v8a.apk","size":123,"browser_download_url":"https://x/a.apk","digest":"sha256:ABCDEF"}]}"""
        val r = AppUpdater.parse(json)
        assertEquals("2.23.0", r.version); assertEquals("https://x/a.apk", r.apkUrl)
        assertEquals(123L, r.apkSize); assertEquals("abcdef", r.sha256)
    }

    @Test fun releaseWithoutApkFallsBackToThePage() {
        val r = AppUpdater.parse("""{"tag_name":"v1.0.0","html_url":"https://p","assets":[]}""")
        assertNull(r.apkUrl); assertEquals("https://p", r.pageUrl)
    }

    @Test fun rejectsAnswersThatAreNotReleases() {
        try { AppUpdater.parse("<html>"); fail() } catch (e: IOException) { }
        try { AppUpdater.parse("""{"message":"Not Found"}"""); fail() } catch (e: IOException) { }
    }
}
