package com.missionbreak56.musicplayer

import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kyant.taglib.TagLib
import com.missionbreak56.musicplayer.tags.CoverChange
import com.missionbreak56.musicplayer.tags.TagField
import com.missionbreak56.musicplayer.tags.TagStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File

/** Reads and writes real audio files with the real TagLib library, on a device (arm64). */
@RunWith(AndroidJUnit4::class)
class TagStoreInstrumentedTest {
    private val target = InstrumentationRegistry.getInstrumentation().targetContext
    private val tests = InstrumentationRegistry.getInstrumentation().context

    private fun asset(name: String): File {
        val f = File(target.cacheDir, "it_$name")
        tests.assets.open(name).use { input -> f.outputStream().use { input.copyTo(it) } }
        return f
    }

    private val formats = listOf("t.mp3", "t.flac", "t.ogg", "t.m4a")

    @Test
    fun readsTheTagsOfEveryFormat() {
        for (name in formats) {
            val snap = TagStore.read(target, Uri.fromFile(asset(name)), name)
            assertTrue("$name should be writable", snap.format.writable)
            assertEquals(name, "Orig Title", snap.values[TagField.TITLE])
            assertEquals(name, "Orig Artist", snap.values[TagField.ARTIST])
        }
    }

    @Test
    fun savesNewTagsAndKeepsTheAudio() {
        for (name in formats) {
            val uri = Uri.fromFile(asset(name))
            val before = TagStore.read(target, uri, name)
            val edited = LinkedHashMap(before.values)
            edited[TagField.TITLE] = "Novo Título ção"
            edited[TagField.GENRE] = "Pop"
            edited[TagField.LYRICS] = "linha 1\nlinha 2"
            TagStore.save(target, uri, before, edited, CoverChange.Keep)

            val after = TagStore.read(target, uri, name)
            assertEquals(name, "Novo Título ção", after.values[TagField.TITLE])
            assertEquals(name, "Pop", after.values[TagField.GENRE])
            assertEquals(name, "linha 1\nlinha 2", after.values[TagField.LYRICS])
            assertEquals(name, "Orig Artist", after.values[TagField.ARTIST])
            assertEquals(name, before.audio?.sampleRate, after.audio?.sampleRate)
            assertEquals(name, before.audio?.channels, after.audio?.channels)
        }
    }

    @Test
    fun replacesAndRemovesTheCover() {
        val jpeg = ByteArrayOutputStream().also {
            Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, it)
        }.toByteArray()
        for (name in listOf("t.mp3", "t.flac", "t.m4a")) {
            val file = asset(name)
            val uri = Uri.fromFile(file)
            val snap = TagStore.read(target, uri, name)
            TagStore.save(target, uri, snap, snap.values, CoverChange.Replace(jpeg, "image/jpeg"))
            assertTrue("$name should have a picture", pictureCount(file) >= 1)

            val again = TagStore.read(target, uri, name)
            TagStore.save(target, uri, again, again.values, CoverChange.Remove)
            assertEquals("$name should have no picture", 0, pictureCount(file))
        }
    }

    private fun pictureCount(file: File): Int =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            assertNotNull(pfd)
            TagLib.getPictures(pfd.dup().detachFd()).size
        }
}
