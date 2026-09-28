package kr.local.voicememo

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile

class MemoTest {
    @Test fun commandAndCleanup() {
        assertTrue(MemoText.hasCommand("내일 우유 구매 녹음 끝"))
        assertTrue(MemoText.hasCommand("녹음끝"))
        assertFalse(MemoText.hasCommand("녹음 끄기"))
        assertEquals("내일 우유 구매", MemoText.clean("내일 우유 구매 녹음 끝"))
        assertEquals(20, MemoText.title("가".repeat(30), 0).length)
        assertTrue(MemoText.title("녹음 끝", 0).startsWith("음성 메모 "))
    }
    @Test fun shortTitleKeepsWholeText() {
        assertEquals("내일 회의", MemoText.title("내일 회의", 0))
    }
    @Test fun removesPunctuatedCommandWithoutGuessingUserWords() {
        assertEquals("", MemoText.clean("녹음 끝."))
        assertEquals("유리컵", MemoText.clean("유리컵 녹음, 끝"))
        assertEquals("가수 분사 후 선풍기 유리 터", MemoText.clean("가수 분사 후 선풍기 유리 터"))
    }
    @Test fun emptyTranscriptUsesRecordingDate() {
        assertEquals("음성 메모 ${formatDate(123456789L)}", MemoText.title("   ", 123456789L))
    }
    @Test fun recoversWavHeaderAfterInterruptedWrite() {
        val file = File.createTempFile("memo", ".wav")
        try {
            RandomAccessFile(file, "rw").use {
                it.setLength(44 + 32001); Wav.header(it)
                assertEquals(32044L, it.length())
                it.seek(0); assertEquals(0x52494646, it.readInt())
                it.seek(40); assertEquals(32000, Integer.reverseBytes(it.readInt()))
            }
        } finally { file.delete() }
    }
}
