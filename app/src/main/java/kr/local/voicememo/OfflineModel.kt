package kr.local.voicememo

import android.content.Context
import org.vosk.Model
import java.io.File
import java.util.zip.ZipInputStream

object OfflineModel {
    // The APK contains the official model ZIP. Extraction is local and crash-safe.
    fun load(context: Context): Model {
        val root = File(context.noBackupFilesDir, "stt")
        val model = File(root, "vosk-model-small-ko-0.22")
        val ready = File(root, "ready-v1")
        if (!ready.exists()) {
            root.deleteRecursively(); root.mkdirs()
            ZipInputStream(context.assets.open("korean.zip")).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val out = File(root, entry.name)
                    require(out.canonicalPath.startsWith(root.canonicalPath + File.separator))
                    if (entry.isDirectory) out.mkdirs() else {
                        out.parentFile?.mkdirs(); out.outputStream().use { zip.copyTo(it) }
                    }
                    zip.closeEntry()
                }
            }
            check(File(model, "am/final.mdl").isFile)
            ready.writeText("1")
        }
        return try { Model(model.absolutePath) } catch (e: Exception) {
            ready.delete() // Retry extraction from the bundled original on the next recording.
            throw e
        }
    }
}
