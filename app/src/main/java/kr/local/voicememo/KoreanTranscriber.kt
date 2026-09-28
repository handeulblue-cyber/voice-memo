package kr.local.voicememo

import android.content.Context
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import java.io.File
import java.io.RandomAccessFile
import java.util.zip.ZipInputStream
import kotlin.math.abs

/** Offline second pass. Language is explicitly Korean; no translation or network API. */
class KoreanTranscriber(private val context: Context) {
    private fun modelDirectory(): File {
        val root = File(context.noBackupFilesDir, "whisper-small-int8-v1")
        val ready = File(root, "ready")
        if (!ready.exists()) {
            root.deleteRecursively(); check(root.mkdirs())
            check(root.usableSpace > 650L * 1024 * 1024) { "한국어 모델을 준비할 저장 공간이 부족합니다." }
            ZipInputStream(context.assets.open("whisper-small.zip")).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val target = File(root, entry.name)
                    require(target.canonicalPath.startsWith(root.canonicalPath + File.separator))
                    if (entry.isDirectory) target.mkdirs() else {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { zip.copyTo(it) }
                    }
                    zip.closeEntry()
                }
            }
            for (name in listOf("small-encoder.int8.onnx", "small-decoder.int8.onnx", "small-tokens.txt")) {
                check(File(root, name).length() > 0) { "한국어 모델 파일이 불완전합니다." }
            }
            ready.writeText("1")
        }
        return root
    }

    fun transcribe(wav: File, progress: (Int) -> Unit): String {
        check(android.os.Process.is64Bit()) { "정밀 한국어 인식에는 64비트 기기가 필요합니다." }
        val root = modelDirectory()
        val recognizer = OfflineRecognizer(config = OfflineRecognizerConfig(
            modelConfig = OfflineModelConfig(
                whisper = OfflineWhisperModelConfig(
                    encoder = File(root, "small-encoder.int8.onnx").absolutePath,
                    decoder = File(root, "small-decoder.int8.onnx").absolutePath,
                    language = "ko", task = "transcribe", tailPaddings = 1000,
                ),
                tokens = File(root, "small-tokens.txt").absolutePath,
                numThreads = 2, debug = false, provider = "cpu",
            ),
        ))
        try {
            val parts = mutableListOf<String>()
            RandomAccessFile(wav, "r").use { input ->
                val total = ((input.length() - 44).coerceAtLeast(0) / 2)
                var offset = 0L
                while (offset < total) {
                    input.seek(44 + offset * 2)
                    val n = minOf(total - offset, (28 * Wav.RATE).toLong()).toInt()
                    val bytes = ByteArray(n * 2); input.readFully(bytes)
                    val samples = PcmSegments.decode(bytes)
                    val take = if (offset + n < total) PcmSegments.splitPoint(samples) else n
                    val segment = samples.copyOf(take)
                    // Near-digital silence should not produce invented sentences.
                    if (PcmSegments.hasSignal(segment)) {
                        val stream = recognizer.createStream()
                        try {
                            stream.acceptWaveform(segment, Wav.RATE)
                            recognizer.decode(stream)
                            val text = recognizer.getResult(stream).text.trim()
                            if (text.isNotBlank()) parts.add(text)
                        } finally { stream.release() }
                    }
                    offset += take
                    progress((offset * 100 / total.coerceAtLeast(1)).toInt())
                }
            }
            return parts.joinToString(" ")
        } finally { recognizer.release() }
    }
}

/** Continuous, non-overlapping chunks preserve every sample even in long recordings. */
object PcmSegments {
    fun decode(bytes: ByteArray): FloatArray = FloatArray(bytes.size / 2) { i ->
        val sample = ((bytes[i * 2].toInt() and 255) or (bytes[i * 2 + 1].toInt() shl 8)).toShort()
        sample.toFloat() / 32768f
    }
    fun hasSignal(samples: FloatArray) = samples.any { abs(it) >= 0.001f }
    fun splitPoint(samples: FloatArray): Int {
        val window = Wav.RATE / 10
        if (samples.size <= 20 * Wav.RATE) return samples.size
        var best = samples.size
        var minimum = Double.POSITIVE_INFINITY
        var start = 20 * Wav.RATE
        while (start + window <= samples.size) {
            var energy = 0.0
            for (i in start until start + window) energy += samples[i].toDouble() * samples[i]
            if (energy < minimum) { minimum = energy; best = start + window / 2 }
            start += window
        }
        return best
    }
}
