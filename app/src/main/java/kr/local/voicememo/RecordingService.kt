package kr.local.voicememo

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.*
import android.os.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import org.vosk.Recognizer
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID

data class RecordingState(val active: Boolean = false, val saving: Boolean = false, val elapsed: Long = 0, val text: String = "", val message: String = "", val completed: Long = 0)
class RecordingService : Service() {
    companion object {
        val state = MutableStateFlow(RecordingState())
        const val START = "start"
        const val SAVE = "save"
        const val CANCEL = "cancel"
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var stopping = false
    @Volatile private var cancelled = false
    @Volatile private var captured = false
    @Volatile private var currentFile: File? = null
    private var job: Job? = null
    private var wake: PowerManager.WakeLock? = null
    private lateinit var audio: AudioManager
    private var focus: AudioFocusRequest? = null
    override fun onBind(intent: Intent?) = null
    override fun onCreate() {
        super.onCreate()
        audio = getSystemService(AudioManager::class.java)
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("record", "음성 녹음", NotificationManager.IMPORTANCE_LOW))
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            SAVE -> stopping = true
            CANCEL -> {
                cancelled = true; stopping = true
                currentFile?.let { file ->
                    runCatching { File(file.parentFile, file.nameWithoutExtension + ".deleted").writeText("") }
                }
            }
            START -> if (job == null) {
                state.value = RecordingState(active = true, message = "오프라인 음성 인식 준비 중… 녹음은 진행됩니다.")
                try {
                    val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
                    val save = PendingIntent.getService(this, 1, Intent(this, RecordingService::class.java).setAction(SAVE), PendingIntent.FLAG_IMMUTABLE)
                    val notification = Notification.Builder(this, "record").setSmallIcon(kr.local.voicememo.R.drawable.ic_mic)
                        .setContentTitle("듣고 있습니다...").setContentText("‘녹음 끝’이라고 말하면 저장됩니다.")
                        .setContentIntent(open).setOngoing(true).addAction(Notification.Action.Builder(null, "저장", save).build()).build()
                    if (Build.VERSION.SDK_INT >= 29) startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE) else startForeground(1, notification)
                    wake = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "voice:memo").apply { acquire(6 * 60 * 60 * 1000L) }
                    focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                        .setOnAudioFocusChangeListener({ if (it < 0) { state.update { s -> s.copy(message = "다른 오디오 작업으로 녹음을 종료합니다.") }; stopping = true } }, Handler(mainLooper)).build()
                    if (audio.requestAudioFocus(focus!!) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) stopping = true
                    job = scope.launch { record() }
                } catch (e: Exception) {
                    state.value = RecordingState(message = "녹음을 시작할 수 없습니다. 마이크 권한을 확인해 주세요.")
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }
    @Suppress("MissingPermission")
    private suspend fun record() {
        val repo = (application as MemoApp).repository
        val date = System.currentTimeMillis()
        val file = File(repo.directory, "${date}_${UUID.randomUUID()}.wav")
        currentFile = file
        val transcript = File(repo.directory, file.nameWithoutExtension + ".txt")
        var recorder: AudioRecord? = null
        var recognition: Job? = null
        var result = ""
        var problem = ""
        try {
            check(repo.directory.usableSpace > 10 * 1024 * 1024) { "저장 공간이 부족합니다." }
            RandomAccessFile(file, "rw").use { output ->
                output.setLength(44); Wav.header(output)
                recognition = scope.launch {
                    try {
                        OfflineModel.load(this@RecordingService).use { model ->
                            Recognizer(model, Wav.RATE.toFloat()).use { recognizer ->
                                state.update { it.copy(message = "") }
                                fun append(json: String, detect: Boolean) {
                                    val text = JSONObject(json).optString("text")
                                    if (text.isNotBlank()) {
                                        result = "$result $text".trim()
                                        transcript.writeText(MemoText.clean(result))
                                        state.update { it.copy(text = MemoText.clean(result)) }
                                        // Only endpoint-final results can stop capture. Never partial results.
                                        if (detect && MemoText.hasCommand(text)) stopping = true
                                    }
                                }
                                RandomAccessFile(file, "r").use { input ->
                                    input.seek(44)
                                    val buffer = ByteArray(8000)
                                    while (!cancelled) {
                                        val available = ((input.length() - input.filePointer).coerceAtLeast(0).toInt() / 2) * 2
                                        if (available > 0) {
                                            val n = input.read(buffer, 0, minOf(available, buffer.size))
                                            if (n > 0 && recognizer.acceptWaveForm(buffer, n)) append(recognizer.result, true)
                                        } else if (captured) break else delay(30)
                                    }
                                    if (!cancelled) append(recognizer.finalResult, false)
                                }
                            }
                        }
                    } catch (e: Throwable) {
                        if (e is CancellationException) throw e
                        if (e !is Exception && e !is LinkageError) throw e
                        state.update { it.copy(message = "음성 인식을 사용할 수 없습니다. 녹음은 보존됩니다. 저장 버튼을 눌러 주세요.") }
                    }
                }
                val minimum = AudioRecord.getMinBufferSize(Wav.RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                check(minimum > 0) { "마이크 형식을 지원하지 않습니다." }
                recorder = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, Wav.RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minimum * 2, 8000))
                check(recorder!!.state == AudioRecord.STATE_INITIALIZED) { "마이크 초기화 실패" }
                recorder!!.startRecording()
                check(recorder!!.recordingState == AudioRecord.RECORDSTATE_RECORDING)
                val buffer = ByteArray(3200)
                var lastSync = 0L
                while (!stopping) {
                    val n = recorder!!.read(buffer, 0, buffer.size)
                    check(n > 0) { "녹음이 중단되었습니다." }
                    output.write(buffer, 0, n)
                    val elapsed = (output.length() - 44) * 1000 / (Wav.RATE * 2)
                    state.update { it.copy(elapsed = elapsed) }
                    if (elapsed - lastSync >= 1000) {
                        Wav.header(output); output.fd.sync(); lastSync = elapsed
                        check(repo.directory.usableSpace > 2 * 1024 * 1024) { "저장 공간이 부족하여 녹음을 종료했습니다." }
                        if (Build.VERSION.SDK_INT >= 29 && audio.activeRecordingConfigurations.any { it.clientAudioSessionId == recorder!!.audioSessionId && it.isClientSilenced }) {
                            problem = "전화 또는 다른 앱이 마이크를 사용하여 녹음을 저장했습니다."; stopping = true
                        }
                        if (elapsed >= 6 * 60 * 60 * 1000L) { problem = "6시간 제한으로 녹음을 저장했습니다."; stopping = true }
                    }
                }
                Wav.header(output); output.fd.sync()
            }
        } catch (e: Exception) { problem = e.message ?: "녹음 오류가 발생했습니다." }
        finally {
            runCatching { recorder?.stop() }; recorder?.release(); captured = true
            state.update { it.copy(saving = true) }
            recognition?.join()
            try {
                if (cancelled) {
                    val marker = File(repo.directory, file.nameWithoutExtension + ".deleted").apply { writeText("") }
                    check(!file.exists() || file.delete()); transcript.delete(); marker.delete()
                } else if (file.exists()) repo.save(file, result, date)
            } catch (e: Exception) { problem = "저장을 완료하지 못했습니다. 다음 실행 때 녹음 복구를 다시 시도합니다." }
            state.update { it.copy(active = false, saving = false, message = problem.ifBlank { it.message }, completed = System.currentTimeMillis()) }
            withContext(Dispatchers.Main) { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
        }
    }
    override fun onDestroy() {
        stopping = true
        focus?.let { audio.abandonAudioFocusRequest(it) }
        if (wake?.isHeld == true) wake?.release()
        // Do not cancel the final file flush/DB transaction when Android destroys the service.
        job?.invokeOnCompletion { scope.cancel() } ?: scope.cancel()
        super.onDestroy()
    }
}
