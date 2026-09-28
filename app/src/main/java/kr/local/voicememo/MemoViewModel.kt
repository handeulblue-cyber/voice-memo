package kr.local.voicememo

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MemoViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MemoApp
    val memos = app.repository.dao.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recording = RecordingService.state
    val ready = MutableStateFlow(false)
    val error = MutableStateFlow("")
    var autoStartHandled = false
    init {
        viewModelScope.launch(Dispatchers.IO) {
            try { if (!recording.value.active) app.repository.recover() }
            catch (e: Exception) { error.value = "이전 녹음 복구에 실패했습니다. 저장 공간을 확인한 뒤 앱을 다시 실행해 주세요." }
            finally { ready.value = true }
        }
    }
    fun start() {
        if (!ready.value || recording.value.active) return
        runCatching { app.startForegroundService(Intent(app, RecordingService::class.java).setAction(RecordingService.START)) }
            .onFailure { error.value = "녹음을 시작하지 못했습니다. 앱을 화면에 열고 마이크 권한을 확인해 주세요." }
    }
    fun stop(cancel: Boolean) {
        if (recording.value.active) app.startService(Intent(app, RecordingService::class.java).setAction(if (cancel) RecordingService.CANCEL else RecordingService.SAVE))
    }
    fun delete(memo: Memo, done: () -> Unit) = viewModelScope.launch {
        try { withContext(Dispatchers.IO) { app.repository.delete(memo) }; done() }
        catch (e: Exception) { error.value = "삭제하지 못했습니다. 다시 시도해 주세요." }
    }
}
