package kr.local.voicememo

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.RandomAccessFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MemoRepositoryTest {
    private lateinit var db: MemoDb
    private lateinit var repo: MemoRepository

    @Before fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(app, MemoDb::class.java).allowMainThreadQueries().build()
        repo = MemoRepository(app, db.memos())
        repo.directory.deleteRecursively()
    }
    @After fun tearDown() { db.close(); repo.directory.deleteRecursively() }
    private fun audio(id: String): File = File(repo.directory, "$id.wav").also { file ->
        RandomAccessFile(file, "rw").use { it.setLength(32044); Wav.header(it) }
    }
    @Test fun savesFieldsThenDeletesDatabaseAndAudio() = runBlocking {
        val file = audio("1234_test")
        repo.save(file, "내일 회의 준비 녹음 끝", 1234)
        val saved = db.memos().all().single()
        assertEquals("1234_test", saved.id)
        assertEquals("내일 회의 준비", saved.text)
        assertEquals(saved.text, saved.title)
        assertEquals(1000L, saved.durationMs)
        assertEquals(1234L, saved.createdAt)
        assertEquals(file.absolutePath, saved.audioPath)
        repo.delete(saved)
        assertTrue(db.memos().all().isEmpty())
        assertFalse(file.exists())
    }
    @Test fun recoversOrphanExactlyOnce() = runBlocking {
        audio("1234_orphan")
        File(repo.directory, "1234_orphan.txt").writeText("복구된 내용")
        repo.recover(); repo.recover()
        val saved = db.memos().all().single()
        assertEquals("복구된 내용", saved.text)
        assertEquals(1234L, saved.createdAt)
    }
    @Test fun interruptedCancellationNeverReappears() = runBlocking {
        val file = audio("1234_cancelled")
        File(repo.directory, "1234_cancelled.deleted").writeText("")
        repo.recover()
        assertTrue(db.memos().all().isEmpty())
        assertFalse(file.exists())
    }
    @Test fun listIsNewestFirst() = runBlocking {
        repo.save(audio("1000_old"), "이전 메모", 1000)
        repo.save(audio("2000_new"), "최신 메모", 2000)
        assertEquals(listOf("2000_new", "1000_old"), db.memos().observe().first().map { it.id })
    }
    @Test fun secondPassUpdatesTextWithoutLosingOriginalAudio() = runBlocking {
        val file = audio("1000_refine")
        repo.save(file, "기본 인식", 1000)
        repo.refine(file, "아름다운 선풍기 유리컵", 1000)
        assertEquals("아름다운 선풍기 유리컵", db.memos().all().single().text)
        assertEquals(32044L, file.length())
        repo.refine(file, "", 1000)
        assertEquals("아름다운 선풍기 유리컵", db.memos().all().single().text)
    }
}
