package kr.local.voicememo

import org.junit.Assert.*
import org.junit.Test

class PcmSegmentsTest {
    @Test fun signedLittleEndianConversion() {
        val samples = PcmSegments.decode(byteArrayOf(0, 0, 0, -128, -1, 127))
        assertEquals(0f, samples[0], 0f)
        assertEquals(-1f, samples[1], 0f)
        assertEquals(32767f / 32768f, samples[2], 0f)
    }
    @Test fun digitalSilenceIsSkipped() {
        assertFalse(PcmSegments.hasSignal(FloatArray(16000)))
        assertTrue(PcmSegments.hasSignal(floatArrayOf(0.2f)))
    }
    @Test fun longRecordingSplitsInsideQuietWindow() {
        val samples = FloatArray(28 * 16000) { 0.2f }
        for (i in 25 * 16000 until 26 * 16000) samples[i] = 0f
        val split = PcmSegments.splitPoint(samples)
        assertTrue(split in 25 * 16000 until 26 * 16000)
        assertEquals(samples.size, samples.take(split).size + samples.drop(split).size)
        assertEquals(16000, PcmSegments.splitPoint(FloatArray(16000)))
    }
}
