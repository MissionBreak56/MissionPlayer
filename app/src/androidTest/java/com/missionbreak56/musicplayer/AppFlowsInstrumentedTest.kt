package com.missionbreak56.musicplayer

import android.Manifest
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.missionbreak56.musicplayer.data.MusicRepository
import com.missionbreak56.musicplayer.data.Prefs
import com.missionbreak56.musicplayer.ui.Backup
import com.missionbreak56.musicplayer.ui.MainActivity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

/** Critical flows on a device: permission-granted start-up, the MediaStore read, favorites and backup. */
@RunWith(AndroidJUnit4::class)
class AppFlowsInstrumentedTest {
    private val target = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        *if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    )

    @Test
    fun mainScreenOpensWithPermissionGranted() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.pager)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun mediaStoreReadDoesNotFail() {
        val songs = MusicRepository.loadSongs(target)
        assertFalse("reading MediaStore with permission must not report a failure", MusicRepository.lastLoadFailed)
        assertTrue(songs.all { it.durationMs >= 0 })
    }

    @Test
    fun favoriteToggleSurvivesTheCache() {
        Prefs.init(target)
        val id = -4242L
        if (Prefs.isFavorite(id)) Prefs.toggleFavorite(id)
        assertTrue(Prefs.toggleFavorite(id))
        assertTrue(Prefs.isFavorite(id))
        assertFalse(Prefs.toggleFavorite(id))
        assertFalse(Prefs.isFavorite(id))
    }

    @Test
    fun backupCanBeCreatedAndRestoredAndRejectsGarbage() = runBlocking {
        Prefs.init(target)
        val out = ByteArrayOutputStream()
        Backup.create(target, out, "test")
        assertTrue("the backup file must not be empty", out.size() > 0)
        val result = Backup.restore(target, ByteArrayInputStream(out.toByteArray()))
        assertTrue(result.matched >= 0 && result.missing >= 0)
        try {
            Backup.restore(target, ByteArrayInputStream(ByteArray(64) { it.toByte() }))
            fail("a file that is not a backup must be rejected")
        } catch (_: IOException) {
            // expected
        }
    }
}
