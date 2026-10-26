package com.missionbreak56.musicplayer.tags

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.kyant.taglib.AudioPropertiesReadStyle
import com.kyant.taglib.Metadata
import com.kyant.taglib.Picture
import com.kyant.taglib.TagLib
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.math.abs

enum class TagError { UNSUPPORTED_FORMAT, UNREADABLE, NO_WRITE_ACCESS, NO_SPACE, WRITE_FAILED, VERIFY_FAILED }

/** Why reading or writing the tags of a file failed. [message] is for logs; the UI shows its own text. */
class TagException(val error: TagError, message: String, cause: Throwable? = null) : Exception(message, cause)

class AudioInfo(val lengthMs: Int, val bitrateKbps: Int, val sampleRate: Int, val channels: Int)

/** Everything read from a file before editing: the raw properties (kept to write back safely) and the editor values. */
class TagSnapshot(
    val format: AudioTagFormat,
    val fileName: String,
    val properties: HashMap<String, Array<String>>,
    val values: Map<TagField, String>,
    val audio: AudioInfo?
)

/**
 * Reads and writes the tags of audio files with TagLib.
 *
 * Writing never touches the original until the new tags were written to a private copy and checked:
 * original -> backup copy + working copy -> TagLib edits the working copy -> it is read back and compared
 * -> the working copy replaces the original content -> the original is read back and compared again.
 * If anything fails after the original was touched, the backup is written back. The audio stream is
 * never decoded or re-encoded: TagLib only rewrites the tag area.
 */
object TagStore {

    private const val TMP_DIR = "tagedit"
    private const val COPY_BUFFER = 256 * 1024
    private const val SPACE_MARGIN = 16L * 1024 * 1024

    fun displayName(context: Context, uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (_: Exception) {
        null
    }

    private fun extensionFor(name: String?, format: AudioTagFormat): String {
        val fromName = name.orEmpty().substringAfterLast('.', "").lowercase()
        if (fromName.isNotEmpty() && fromName.length <= 5) return fromName
        return when (format) {
            AudioTagFormat.MP3 -> "mp3"
            AudioTagFormat.FLAC -> "flac"
            AudioTagFormat.OGG -> "ogg"
            AudioTagFormat.M4A -> "m4a"
            AudioTagFormat.OTHER -> "bin"
        }
    }

    private fun tmpDir(context: Context): File = File(context.cacheDir, TMP_DIR).apply { mkdirs() }

    /** Removes working files left behind by a process that died while saving (never the backups of a failed restore). */
    fun cleanLeftovers(context: Context) {
        try {
            File(context.cacheDir, TMP_DIR).listFiles()?.forEach { if (it.name.startsWith("work") || it.name.startsWith("read")) it.delete() }
        } catch (_: Exception) {
        }
    }

    // ----- Reading -----

    /** Reads the tags of [uri] (blocking: call from a background thread). */
    fun read(context: Context, uri: Uri, fileNameHint: String? = null): TagSnapshot {
        val resolver = context.contentResolver
        val name = fileNameHint ?: displayName(context, uri) ?: ""
        val format = AudioTagFormat.detect(name, resolver.getType(uri))

        var metadata: Metadata? = null
        var audio: AudioInfo? = null
        try {
            resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val pair = readFrom(pfd)
                metadata = pair.first
                audio = pair.second
            }
        } catch (e: SecurityException) {
            throw TagException(TagError.UNREADABLE, "No read access", e)
        } catch (e: IOException) {
            throw TagException(TagError.UNREADABLE, "Cannot open the file", e)
        }
        if (metadata == null) {
            // Some descriptors do not expose a usable name for TagLib: retry on a private copy that keeps the extension.
            val copy = File(tmpDir(context), "read.${extensionFor(name, format)}")
            try {
                copyUriToFile(context, uri, copy)
                ParcelFileDescriptor.open(copy, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    val pair = readFrom(pfd)
                    metadata = pair.first
                    audio = pair.second
                }
            } catch (e: IOException) {
                throw TagException(TagError.UNREADABLE, "Cannot read a copy of the file", e)
            } finally {
                copy.delete()
            }
        }
        val meta = metadata ?: throw TagException(TagError.UNREADABLE, "TagLib could not read this file")
        val props = HashMap<String, Array<String>>()
        for ((k, v) in meta.propertyMap) props[k] = v
        return TagSnapshot(format, name, props, TagCodec.read(format, props), audio)
    }

    private fun readFrom(pfd: ParcelFileDescriptor): Pair<Metadata?, AudioInfo?> {
        val meta = TagLib.getMetadata(pfd.dup().detachFd(), false)
        val props = TagLib.getAudioProperties(pfd.dup().detachFd(), AudioPropertiesReadStyle.Average)
        val info = props?.let { AudioInfo(it.length, it.bitrate, it.sampleRate, it.channels) }
        return meta to info
    }

    // ----- Writing -----

    /**
     * Applies [edited] (and [cover]) to the file behind [uri]. Returns normally only when the file was
     * written and read back with the expected content. Blocking: call from a background thread.
     * The caller must already hold write access to [uri] (see the editor's permission flow).
     */
    @Synchronized
    fun save(
        context: Context,
        uri: Uri,
        snapshot: TagSnapshot,
        edited: Map<TagField, String>,
        cover: CoverChange
    ) {
        val format = snapshot.format
        if (!format.writable) throw TagException(TagError.UNSUPPORTED_FORMAT, "Writing ${format.label} is not supported")

        val newProps = TagCodec.apply(format, snapshot.properties, snapshot.values, edited)
        val propsChanged = !TagCodec.sameProperties(newProps, snapshot.properties)
        if (!propsChanged && cover is CoverChange.Keep) return

        val resolver = context.contentResolver
        val ext = extensionFor(snapshot.fileName, format)
        val dir = tmpDir(context)
        val work = File(dir, "work.$ext")
        val backup = File(dir, "backup.$ext")
        var keepBackup = false
        try {
            // 1. Space for two private copies.
            val size = try {
                resolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
            } catch (e: SecurityException) {
                throw TagException(TagError.NO_WRITE_ACCESS, "No access to the file", e)
            }
            if (size > 0 && dir.usableSpace < size * 2 + SPACE_MARGIN) {
                throw TagException(TagError.NO_SPACE, "Not enough free space for the safety copy")
            }

            // 2. Backup + working copy.
            copyUriToFile(context, uri, backup)
            backup.copyTo(work, overwrite = true)

            // 3. Edit the working copy only.
            ParcelFileDescriptor.open(work, ParcelFileDescriptor.MODE_READ_WRITE).use { pfd ->
                if (propsChanged) {
                    if (!TagLib.savePropertyMap(pfd.dup().detachFd(), newProps)) {
                        throw TagException(TagError.WRITE_FAILED, "TagLib refused to save the properties")
                    }
                }
                if (cover !is CoverChange.Keep) {
                    val pictures = TagLib.getPictures(pfd.dup().detachFd()).toMutableList()
                    val at = coverIndex(pictures)
                    when (cover) {
                        is CoverChange.Remove -> if (at >= 0) pictures.removeAt(at)
                        is CoverChange.Replace -> {
                            val pic = Picture(cover.bytes, "", FRONT_COVER, cover.mime)
                            if (at >= 0) pictures[at] = pic else pictures.add(0, pic)
                        }
                        else -> {}
                    }
                    if (!TagLib.savePictures(pfd.dup().detachFd(), pictures.toTypedArray())) {
                        throw TagException(TagError.WRITE_FAILED, "TagLib refused to save the pictures")
                    }
                }
            }

            // 4. The working copy must read back exactly as intended, with the same audio.
            checkFile(work, snapshot, edited, cover)

            // 5. Replace the original content. Nothing was written to it before this point.
            try {
                overwrite(context, uri, work)
            } catch (e: SecurityException) {
                throw TagException(TagError.NO_WRITE_ACCESS, "No write access", e)
            } catch (e: FileNotFoundException) {
                val denied = e.message.orEmpty().let { it.contains("EACCES") || it.contains("EPERM") || it.contains("Permission", true) }
                throw TagException(if (denied) TagError.NO_WRITE_ACCESS else TagError.WRITE_FAILED, "Cannot open the file for writing", e)
            } catch (e: IOException) {
                restore(context, uri, backup)?.let { keepBackup = true; throw it }
                throw TagException(TagError.WRITE_FAILED, "Writing failed; the original was restored", e)
            }

            // 6. Read the real file back; if it is wrong, put the original back.
            try {
                val tmp = File(dir, "work_check.$ext")
                try {
                    copyUriToFile(context, uri, tmp)
                    checkFile(tmp, snapshot, edited, cover)
                } finally {
                    tmp.delete()
                }
            } catch (e: Exception) {
                restore(context, uri, backup)?.let { keepBackup = true; throw it }
                throw TagException(TagError.VERIFY_FAILED, "The saved file did not read back correctly; the original was restored", e)
            }
        } finally {
            work.delete()
            if (!keepBackup) backup.delete()
        }
    }

    private const val FRONT_COVER = "Front Cover"

    private fun coverIndex(pictures: List<Picture>): Int {
        val front = pictures.indexOfFirst { it.pictureType.equals(FRONT_COVER, ignoreCase = true) }
        return if (front >= 0) front else if (pictures.isNotEmpty()) 0 else -1
    }

    private fun norm(s: String): String = s.replace("\r\n", "\n").trim()

    /** Reads [file] with TagLib and compares it with what [edited]/[cover] should have produced. */
    private fun checkFile(file: File, snapshot: TagSnapshot, edited: Map<TagField, String>, cover: CoverChange) {
        val format = snapshot.format
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            val meta = TagLib.getMetadata(pfd.dup().detachFd(), false)
                ?: throw TagException(TagError.VERIFY_FAILED, "The file can no longer be read")
            val now = TagCodec.read(format, meta.propertyMap)
            for (f in TagField.values()) {
                if (!format.supports(f)) continue
                val wanted = norm(edited[f].orEmpty())
                val before = norm(snapshot.values[f].orEmpty())
                val changed = wanted != before
                val core = f == TagField.TITLE || f == TagField.ARTIST || f == TagField.ALBUM
                if (!changed && !core) continue
                if (norm(now[f].orEmpty()) != (if (changed) wanted else before)) {
                    throw TagException(TagError.VERIFY_FAILED, "Field ${f.key} did not read back as expected")
                }
            }
            val audio = TagLib.getAudioProperties(pfd.dup().detachFd(), AudioPropertiesReadStyle.Average)
            val was = snapshot.audio
            if (audio != null && was != null) {
                if (audio.sampleRate != was.sampleRate || audio.channels != was.channels || abs(audio.length - was.lengthMs) > 1500) {
                    throw TagException(TagError.VERIFY_FAILED, "Audio properties changed")
                }
            }
            if (cover !is CoverChange.Keep) {
                val pictures = TagLib.getPictures(pfd.dup().detachFd()).toList()
                val front = pictures.getOrNull(coverIndex(pictures))
                when (cover) {
                    is CoverChange.Remove -> if (front != null && front.pictureType.equals(FRONT_COVER, true)) {
                        throw TagException(TagError.VERIFY_FAILED, "The cover is still there")
                    }
                    is CoverChange.Replace -> if (front == null || !front.data.contentEquals(cover.bytes)) {
                        throw TagException(TagError.VERIFY_FAILED, "The new cover did not read back")
                    }
                    else -> {}
                }
            }
        }
    }

    private fun copyUriToFile(context: Context, uri: Uri, target: File) {
        val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")
        input.use { src -> target.outputStream().use { out -> src.copyTo(out, COPY_BUFFER) } }
    }

    /** Replaces the whole content behind [uri] with [source], flushing it to storage. */
    private fun overwrite(context: Context, uri: Uri, source: File) {
        val pfd = context.contentResolver.openFileDescriptor(uri, "rwt") ?: throw IOException("Cannot open $uri for writing")
        ParcelFileDescriptor.AutoCloseOutputStream(pfd).use { out ->
            FileInputStream(source).use { it.copyTo(out, COPY_BUFFER) }
            out.flush()
            try {
                out.fd.sync()
            } catch (_: Exception) {
            }
        }
    }

    /** Puts the backup back. Returns null when it worked, otherwise the exception to report. */
    private fun restore(context: Context, uri: Uri, backup: File): TagException? = try {
        overwrite(context, uri, backup)
        null
    } catch (e: Exception) {
        TagException(TagError.WRITE_FAILED, "Writing failed AND the original could not be restored (backup: ${backup.path})", e)
    }
}
