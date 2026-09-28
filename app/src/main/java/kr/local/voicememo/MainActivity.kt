package kr.local.voicememo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.provider.Settings
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF2563EB), background = Color.White, surface = Color.White)) {
                Surface(modifier = Modifier.fillMaxSize()) { MemoScreen() }
            }
        }
    }
}
@Composable private fun MemoScreen(vm: MemoViewModel = viewModel()) {
    val context = LocalContext.current
    val ready by vm.ready.collectAsStateWithLifecycle()
    val rec by vm.recording.collectAsStateWithLifecycle()
    val memos by vm.memos.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    val microphone = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        if (granted) vm.start()
    }
    fun start() {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) vm.start()
        else microphone.launch(Manifest.permission.RECORD_AUDIO)
    }
    LaunchedEffect(ready) {
        if (ready && !vm.autoStartHandled) { vm.autoStartHandled = true; if (!rec.active) start() }
    }
    BackHandler(rec.active || selectedId != null) {
        if (rec.active) vm.stop(true) else selectedId = null
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp)) {
        if (error.isNotBlank()) {
            Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 12.dp))
            TextButton(onClick = { vm.error.value = "" }) { Text("닫기") }
        }
        when {
            !ready -> { Spacer(Modifier.height(80.dp)); CircularProgressIndicator(); Text("이전 녹음 확인 중…") }
            rec.active -> {
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Mic, contentDescription = "녹음 중", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(128.dp))
                    Spacer(Modifier.height(28.dp))
                    Text(if (rec.saving) "저장하고 있습니다…" else "듣고 있습니다...", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp)); Text("'녹음 끝'이라고 말하면 저장됩니다.")
                    Spacer(Modifier.height(24.dp)); Text(duration(rec.elapsed), fontSize = 42.sp, color = MaterialTheme.colorScheme.primary)
                    if (rec.message.isNotBlank()) Text(rec.message, modifier = Modifier.padding(vertical = 20.dp))
                    if (rec.text.isNotBlank()) Text(rec.text.takeLast(180), modifier = Modifier.padding(vertical = 16.dp))
                }
                OutlinedButton(onClick = { vm.stop(false) }, enabled = !rec.saving, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("저장") }
                TextButton(onClick = { vm.stop(true) }, enabled = !rec.saving, modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp)) { Text("취소") }
            }
            selectedId != null -> {
                val selected = memos.find { it.id == selectedId }
                if (selected != null) Detail(selected, { selectedId = null }, { vm.delete(selected) { selectedId = null } })
                else TextButton(onClick = { selectedId = null }) { Text("목록으로") }
            }
            else -> {
                Spacer(Modifier.height(24.dp)); Text("음성 메모", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("내 기기에만 안전하게", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                if (rec.message.isNotBlank()) Text(rec.message, modifier = Modifier.padding(vertical = 8.dp))
                if (permissionDenied) {
                    Text("녹음하려면 마이크 권한이 필요합니다. 저장된 메모는 권한 없이 확인할 수 있습니다.")
                    TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }) { Text("앱 권한 설정 열기") }
                }
                LazyColumn(Modifier.weight(1f)) {
                    if (memos.isEmpty()) item { Text("아직 저장된 메모가 없습니다.", modifier = Modifier.padding(vertical = 48.dp)) }
                    items(memos, key = { it.id }) { memo ->
                        Column(Modifier.fillMaxWidth().clickable { selectedId = memo.id }.padding(vertical = 20.dp)) {
                            Text(memo.title, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                            Text("${formatDate(memo.createdAt)}  ·  ${duration(memo.durationMs)}", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                        }
                        HorizontalDivider(color = Color(0xFFEEF2F6))
                    }
                }
                Button(onClick = { start() }, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).heightIn(min = 60.dp)) { Icon(Icons.Default.Mic, null); Spacer(Modifier.width(8.dp)); Text("새 음성 메모", fontSize = 18.sp) }
            }
        }
    }
}
@Composable private fun Detail(memo: Memo, back: () -> Unit, delete: () -> Unit) {
    var player by remember(memo.id) { mutableStateOf<MediaPlayer?>(null) }
    var prepared by remember(memo.id) { mutableStateOf(false) }
    var playing by remember(memo.id) { mutableStateOf(false) }
    var position by remember(memo.id) { mutableStateOf(0) }
    var length by remember(memo.id) { mutableStateOf(memo.durationMs.toInt()) }
    var failure by remember(memo.id) { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    DisposableEffect(memo.id) {
        val p = MediaPlayer(); player = p
        try {
            p.setDataSource(memo.audioPath)
            p.setOnPreparedListener { prepared = true; length = it.duration }
            p.setOnCompletionListener { playing = false; position = length }
            p.setOnErrorListener { _, _, _ -> failure = "녹음 파일을 재생할 수 없습니다."; playing = false; prepared = false; true }
            p.prepareAsync()
        } catch (e: Exception) { failure = "녹음 파일을 열 수 없습니다." }
        onDispose { p.release(); player = null }
    }
    LaunchedEffect(playing) { while (playing) { position = runCatching { player?.currentPosition ?: 0 }.getOrDefault(0); delay(200) } }
    Column(Modifier.fillMaxSize()) {
        TextButton(onClick = back) { Text("‹ 목록") }
        Text(memo.title, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("${formatDate(memo.createdAt)} · ${duration(memo.durationMs)}", color = Color.Gray, modifier = Modifier.padding(vertical = 12.dp))
        Text(memo.text.ifBlank { "변환된 텍스트가 없습니다. 아래에서 녹음을 재생할 수 있습니다." }, fontSize = 19.sp, lineHeight = 30.sp, modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 16.dp))
        if (failure.isNotBlank()) Text(failure, color = MaterialTheme.colorScheme.error)
        Slider(value = position.toFloat().coerceIn(0f, length.coerceAtLeast(1).toFloat()), onValueChange = { position = it.toInt(); player?.seekTo(position) }, valueRange = 0f..length.coerceAtLeast(1).toFloat(), enabled = prepared)
        Text("${duration(position.toLong())} / ${duration(length.toLong())}")
        Button(onClick = {
            runCatching { if (playing) player?.pause() else { if (position >= length) player?.seekTo(0); player?.start() }; playing = !playing }
                .onFailure { failure = "재생에 실패했습니다."; playing = false }
        }, enabled = prepared, modifier = Modifier.fillMaxWidth().padding(top = 16.dp).heightIn(min = 56.dp)) { Text(if (playing) "일시정지" else "재생") }
        TextButton(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("메모 삭제", color = MaterialTheme.colorScheme.error) }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("메모를 삭제할까요?") }, text = { Text("텍스트와 녹음 파일이 함께 삭제됩니다.") }, confirmButton = { TextButton(onClick = { player?.pause(); playing = false; confirm = false; delete() }) { Text("삭제") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("취소") } })
}
