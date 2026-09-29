package kr.local.voicememo

import org.junit.Assert.*
import org.junit.Test

class StopCommandTest {
    private val words = listOf(StopCommand.Word("녹음", 0.95), StopCommand.Word("끝", 0.92))

    @Test fun acceptsOnlyConfidentFinalCommand() {
        assertTrue(StopCommand.accepts("녹음 끝", words, true))
        assertTrue(StopCommand.accepts("녹음끝", words, true))
        assertFalse(StopCommand.accepts("녹음 끝", words, false))
        assertFalse(StopCommand.accepts("녹음 끝", listOf(words[0], words[1].copy(confidence = 0.4)), true))
    }

    @Test fun rejectsUnknownWordsAndSimilarPhrases() {
        for (text in listOf("[unk] 녹음 끝", "끝", "녹음 끄기", "녹음 끝까지 듣기", "녹음 끝 끝", "")) {
            assertFalse(text, StopCommand.accepts(text, words, true))
        }
        assertFalse(StopCommand.accepts("녹음 끝", emptyList(), true))
        assertFalse(StopCommand.accepts("녹음 끝", listOf(StopCommand.Word("[unk]", 1.0)), true))
        assertFalse(StopCommand.accepts("녹음 끝", listOf(words[0], words[1].copy(confidence = Double.NaN)), true))
    }
}
