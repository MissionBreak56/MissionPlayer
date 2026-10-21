package com.missionbreak56.musicplayer.data

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Works out how far a synced lyric is from the audio of the song it is playing with.
 *
 * It uses the loudness of the voice band over time (see [AudioEnvelope]). A line of a song usually
 * starts where the sound steps up: the voice coming in after the intro, after an instrumental break
 * or after a pause. The lyric is slid left and right, and for every shift the loudness in the 0.6 s
 * after each line start is compared with the 0.6 s before it. The shift where this step is biggest
 * is where the lyric matches the audio.
 *
 * The answer is only used when it is clearly better than leaving the lyric as it is and clearly
 * better than every other shift. When it is not sure it says nothing, so a lyric that was already
 * right is never moved by a guess. (On synthetic songs with noise, drums and loud sections unrelated
 * to the voice the thresholds below never moved a correct lyric and never gave a wrong shift.)
 */
object LyricAligner {

    const val HOP_MS = 50

    /** Window (each side) used to remove slow loudness changes: 160 hops = 8 s. */
    private const val NORM_HOPS = 160

    /** Length of the window compared before and after each line start: 12 hops = 0.6 s. */
    private const val EDGE_HOPS = 12

    private const val MIN_ENVELOPE_MS = 40_000L
    private const val MIN_LINES = 6

    // Decision thresholds, in "standard deviations of loudness".
    private const val MIN_SCORE = 0.45        // the lines must really start where the audio steps up
    private const val MIN_GAIN = 0.30         // ...and be clearly better than where it is now
    private const val MIN_PEAK_LEAD = 0.15    // ...and than every shift further than 1.5 s away
    private const val MIN_SHIFT_MS = 300L     // smaller shifts are not worth moving
    private const val PEAK_EXCLUDE_MS = 1_500L

    /**
     * @param shiftMs milliseconds to ADD to the lyric times so they match the audio (negative = the
     *   lyric is late and must come earlier).
     * @param score how well the lyric matches after the shift.
     * @param gain how much better that is than before the shift.
     */
    class Result(val shiftMs: Long, val score: Double, val gain: Double)

    /** Returns null when the lyric is already fine or when the answer would only be a guess. */
    fun estimate(envelope: FloatArray, lines: List<LyricLine>, maxShiftMs: Long = 8_000L): Result? {
        val n = envelope.size
        if (n * HOP_MS < MIN_ENVELOPE_MS) return null
        val sung = lines.filter { it.timeMs >= 0 && it.text.any { c -> c.isLetterOrDigit() } }
        if (sung.size < MIN_LINES) return null

        val z = normalise(envelope)
        val cum = DoubleArray(n + 1)
        for (k in 0 until n) cum[k + 1] = cum[k] + z[k]
        val total = cum[n]

        val starts = IntArray(sung.size) { (sung[it].timeMs / HOP_MS).toInt() }

        // How much louder the 0.6 s after each line start is than the 0.6 s before it, on average.
        // A line that begins where the audio steps up (the voice coming in after the intro, a break
        // or a pause) gives a big value; the right shift is the one where most lines do.
        val w = EDGE_HOPS
        fun score(shift: Int): Double? {
            var sum = 0.0
            var count = 0
            for (s0 in starts) {
                val at = s0 + shift
                if (at - w < 0 || at + w > n) continue
                sum += (cum[at + w] - cum[at]) / w - (cum[at] - cum[at - w]) / w
                count++
            }
            return if (count >= MIN_LINES) sum / count else null
        }

        val max = (maxShiftMs / HOP_MS).toInt()
        val scores = arrayOfNulls<Double>(2 * max + 1)
        for (s in -max..max) scores[s + max] = score(s)
        val atZero = scores[max] ?: return null

        var best = Int.MIN_VALUE
        var bestScore = Double.NEGATIVE_INFINITY
        for (s in -max..max) {
            val v = scores[s + max] ?: continue
            if (v > bestScore) {
                bestScore = v
                best = s
            }
        }
        if (best == Int.MIN_VALUE) return null

        val exclude = (PEAK_EXCLUDE_MS / HOP_MS).toInt()
        var runnerUp = Double.NEGATIVE_INFINITY
        for (s in -max..max) {
            if (abs(s - best) <= exclude) continue
            val v = scores[s + max] ?: continue
            if (v > runnerUp) runnerUp = v
        }

        val shiftMs = best.toLong() * HOP_MS
        val gain = bestScore - atZero
        val sure = bestScore >= MIN_SCORE &&
            gain >= MIN_GAIN &&
            bestScore - runnerUp >= MIN_PEAK_LEAD &&
            abs(shiftMs) >= MIN_SHIFT_MS
        return if (sure) Result(shiftMs, bestScore, gain) else null
    }

    /** Each value minus the local mean, divided by the local standard deviation. */
    private fun normalise(x: FloatArray): DoubleArray {
        val n = x.size
        val sum = DoubleArray(n + 1)
        val sq = DoubleArray(n + 1)
        for (k in 0 until n) {
            sum[k + 1] = sum[k] + x[k]
            sq[k + 1] = sq[k] + x[k].toDouble() * x[k]
        }
        val out = DoubleArray(n)
        for (k in 0 until n) {
            val a = maxOf(0, k - NORM_HOPS)
            val b = minOf(n, k + NORM_HOPS + 1)
            val len = (b - a).toDouble()
            val mean = (sum[b] - sum[a]) / len
            val variance = (sq[b] - sq[a]) / len - mean * mean
            out[k] = (x[k] - mean) / maxOf(sqrt(maxOf(variance, 0.0)), 1e-3)
        }
        return out
    }
}
