package kr.local.voicememo

import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer

/** A second decoder narrows the search without changing the memo transcript. */
class StopCommand(model: Model) : AutoCloseable {
    private val recognizer = Recognizer(model, Wav.RATE.toFloat(), GRAMMAR).apply { setWords(true) }

    fun accept(pcm: ByteArray, length: Int): Boolean {
        // Do not read partialResult or force finalResult while recording.
        if (!recognizer.acceptWaveForm(pcm, length)) return false
        val json = JSONObject(recognizer.result)
        val words = json.optJSONArray("result") ?: return false
        return accepts(json.optString("text"), (0 until words.length()).map {
            val word = words.getJSONObject(it)
            Word(word.optString("word"), word.optDouble("conf", 0.0))
        }, isFinal = true)
    }

    override fun close() = recognizer.close()

    data class Word(val text: String, val confidence: Double)
    companion object {
        const val GRAMMAR = "[\"녹음 끝\", \"[unk]\"]"
        private const val MIN_CONFIDENCE = 0.85

        fun accepts(text: String, words: List<Word>, isFinal: Boolean): Boolean {
            if (!isFinal || text.filterNot { it.isWhitespace() } != "녹음끝") return false
            if (words.map { it.text } !in listOf(listOf("녹음", "끝"), listOf("녹음끝"))) return false
            return words.all { it.confidence.isFinite() && it.confidence in MIN_CONFIDENCE..1.0 }
        }
    }
}
