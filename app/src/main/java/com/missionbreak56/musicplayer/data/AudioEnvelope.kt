package com.missionbreak56.musicplayer.data

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Decodes the beginning of a song and measures how loud its voice band (300 - 3400 Hz) is, one value
 * per [LyricAligner.HOP_MS]. The values are logarithmic. Nothing is saved or sent anywhere.
 */
object AudioEnvelope {

    private const val TIMEOUT_US = 10_000L
    private const val GIVE_UP_MS = 25_000L

    /** Never throws: returns null when the file cannot be decoded. Blocking, call from a background thread. */
    fun compute(context: Context, uri: Uri, maxMs: Long): FloatArray? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            var track = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    track = i
                    format = f
                    break
                }
            }
            if (track < 0 || format == null) return null
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            extractor.selectTrack(track)
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            var sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else {
                44_100
            }
            var channels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else {
                2
            }
            var floatPcm = false

            val maxFrames = (maxMs / LyricAligner.HOP_MS).toInt().coerceAtLeast(1)
            val out = FloatArray(maxFrames)
            var written = 0

            // band-pass: one-pole high-pass at 300 Hz followed by one-pole low-pass at 3400 Hz
            var hopSamples = 1
            var hpA = 0.0
            var lpAlpha = 0.0
            fun setup() {
                val dt = 1.0 / sampleRate
                val rcHigh = 1.0 / (2 * PI * 300.0)
                val rcLow = 1.0 / (2 * PI * 3400.0)
                hpA = rcHigh / (rcHigh + dt)
                lpAlpha = dt / (rcLow + dt)
                hopSamples = (sampleRate.toLong() * LyricAligner.HOP_MS / 1000).toInt().coerceAtLeast(1)
            }
            setup()
            var hpX = 0.0
            var hpY = 0.0
            var lpY = 0.0
            var acc = 0.0
            var count = 0

            fun feed(x: Double) {
                val hp = hpA * (hpY + x - hpX)
                hpX = x
                hpY = hp
                lpY += lpAlpha * (hp - lpY)
                acc += lpY * lpY
                count++
                if (count >= hopSamples) {
                    if (written < maxFrames) out[written++] = ln(sqrt(acc / count) + 1e-5).toFloat()
                    acc = 0.0
                    count = 0
                }
            }

            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            val maxUs = maxMs * 1000
            val deadline = SystemClock.elapsedRealtime() + GIVE_UP_MS
            while (!outputDone && written < maxFrames) {
                if (SystemClock.elapsedRealtime() > deadline) return null
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inIndex >= 0) {
                        val input = codec.getInputBuffer(inIndex)
                        val size = if (input != null) extractor.readSampleData(input, 0) else -1
                        if (size < 0 || extractor.sampleTime > maxUs) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                when {
                    outIndex >= 0 -> {
                        val buffer = codec.getOutputBuffer(outIndex)
                        if (buffer != null && info.size > 0) {
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            buffer.order(ByteOrder.nativeOrder())
                            val ch = channels.coerceAtLeast(1)
                            if (floatPcm) {
                                val fb = buffer.asFloatBuffer()
                                while (fb.remaining() >= ch) {
                                    var sum = 0.0
                                    for (c in 0 until ch) sum += fb.get()
                                    feed(sum / ch)
                                }
                            } else {
                                val sb = buffer.asShortBuffer()
                                while (sb.remaining() >= ch) {
                                    var sum = 0.0
                                    for (c in 0 until ch) sum += sb.get()
                                    feed(sum / ch / 32768.0)
                                }
                            }
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    }
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val f = codec.outputFormat
                        if (f.containsKey(MediaFormat.KEY_SAMPLE_RATE)) sampleRate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        if (f.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        if (Build.VERSION.SDK_INT >= 24 && f.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                            floatPcm = f.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                        }
                        setup()
                    }
                }
            }
            return if (written < 400) null else out.copyOf(written)
        } catch (_: Exception) {
            return null
        } finally {
            try {
                codec?.stop()
            } catch (_: Exception) {
            }
            try {
                codec?.release()
            } catch (_: Exception) {
            }
            extractor.release()
        }
    }
}
