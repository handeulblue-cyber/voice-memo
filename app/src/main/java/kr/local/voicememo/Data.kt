package kr.local.voicememo

import android.app.Application
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = "memos")
data class Memo(@PrimaryKey val id: String, val title: String, val text: String, val audioPath: String, val createdAt: Long, val durationMs: Long)
@Dao interface MemoDao {
    @Query("SELECT * FROM memos ORDER BY createdAt DESC") fun observe(): Flow<List<Memo>>
    @Query("SELECT * FROM memos") suspend fun all(): List<Memo>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(memo: Memo)
    @Delete suspend fun delete(memo: Memo)
}
@Database(entities = [Memo::class], version = 1, exportSchema = true)
abstract class MemoDb : RoomDatabase() { abstract fun memos(): MemoDao }
class MemoApp : Application() {
    val db by lazy { Room.databaseBuilder(this, MemoDb::class.java, "memos.db").build() }
    val repository by lazy { MemoRepository(this, db.memos()) }
}
object MemoText {
    private val command = Regex("녹음\\s*끝")
    fun hasCommand(finalText: String) = command.containsMatchIn(finalText)
    fun clean(text: String) = command.replace(text, " ").replace(Regex("\\s+"), " ").trim()
    fun title(text: String, date: Long) = clean(text).take(20).ifBlank { "음성 메모 ${formatDate(date)}" }
}
fun formatDate(date: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).format(Date(date))
fun duration(ms: Long): String = "%02d:%02d".format(ms / 60000, ms / 1000 % 60)
object Wav {
    const val RATE = 16000
    fun header(file: RandomAccessFile) {
        val length = (file.length() - 44).coerceAtLeast(0).let { it - it % 2 }
        file.setLength(length + 44)
        fun int(v: Int) = file.writeInt(Integer.reverseBytes(v))
        fun short(v: Int) = file.writeShort(java.lang.Short.reverseBytes(v.toShort()).toInt())
        file.seek(0); file.writeBytes("RIFF"); int((length + 36).toInt()); file.writeBytes("WAVEfmt ")
        int(16); short(1); short(1); int(RATE); int(RATE * 2); short(2); short(16)
        file.writeBytes("data"); int(length.toInt()); file.seek(length + 44)
    }
}
class MemoRepository(private val app: Application, val dao: MemoDao) {
    val directory get() = File(app.filesDir, "recordings").apply { mkdirs() }
    suspend fun save(file: File, text: String, date: Long) {
        if (file.length() <= 44) { file.delete(); return }
        RandomAccessFile(file, "rw").use { Wav.header(it); it.fd.sync() }
        dao.insert(Memo(file.nameWithoutExtension, MemoText.title(text, date), MemoText.clean(text), file.absolutePath, date, (file.length()-44)*1000/(Wav.RATE*2)))
        File(directory, file.nameWithoutExtension + ".txt").delete()
    }
    suspend fun recover() {
        // A tombstone makes interrupted deletion idempotent; unfinished WAVs are recoverable.
        val rows = dao.all()
        directory.listFiles()?.filter { it.extension == "deleted" }?.forEach { marker ->
            val id = marker.nameWithoutExtension
            File(directory, "$id.wav").let { check(!it.exists() || it.delete()) }
            File(directory, "$id.txt").delete()
            rows.find { it.id == id }?.let { dao.delete(it) }
            marker.delete()
        }
        val ids = dao.all().map { it.id }.toSet()
        directory.listFiles()?.filter { it.extension == "wav" && it.nameWithoutExtension !in ids }?.forEach {
            val transcript = File(directory, it.nameWithoutExtension + ".txt")
            save(it, if (transcript.exists()) transcript.readText() else "", it.nameWithoutExtension.substringBefore('_').toLongOrNull() ?: it.lastModified())
        }
    }
    suspend fun delete(memo: Memo) {
        val marker = File(directory, "${memo.id}.deleted").apply { writeText("") }
        val audio = File(memo.audioPath)
        check(!audio.exists() || audio.delete()) { "녹음 파일을 삭제하지 못했습니다." }
        File(directory, "${memo.id}.txt").delete(); dao.delete(memo); marker.delete()
    }
}
