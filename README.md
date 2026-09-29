# 음성 메모 한국어 개선판 (1.1)

Kotlin · Jetpack Compose · MVVM · Room 기반 Android 로컬 음성 메모 프로젝트입니다.

## GitHub에서 APK 받기 — PC에 개발 도구 설치 불필요

저장소: https://github.com/handeulblue-cyber/voice-memo

**한국어·종료 명령 개선판 다운로드:** [새 테스트 APK ZIP 다운로드](https://github.com/handeulblue-cyber/voice-memo/actions/runs/36509650747/artifacts/11008677965) · [빌드 및 검사 결과](https://github.com/handeulblue-cyber/voice-memo/actions/runs/36509650747). ZIP 약 521 MB. 압축을 풀어 app-debug.apk를 설치하세요. APK에 모델을 포함하므로 앱 첫 실행부터 오프라인으로 동작합니다.

**기존 1.0 버전 다운로드(개선판 아님):** [테스트 APK ZIP 다운로드](https://github.com/handeulblue-cyber/voice-memo/actions/runs/36388522351/artifacts/10955417231) · [빌드 및 검사 결과](https://github.com/handeulblue-cyber/voice-memo/actions/runs/36388522351). ZIP 약 114 MB. 압축을 풀어 app-debug.apk를 설치하세요. GitHub 로그인이 필요할 수 있습니다.

이번 개선판은 Vosk 일반 받아쓰기와 별도의 종료 명령 전용 인식기를 함께 사용하고 녹음 후 **Whisper small INT8를 한국어(ko)로 고정하여 기기 안에서 원음을 다시 인식**합니다. 외부 API를 사용하지 않으며 단어를 임의 치환하지 않습니다. 개인 발음을 학습한 모델은 아닙니다.

앱 이름은 **음성 메모 한국어 개선**이며 기존 앱과 함께 설치됩니다. 기존 앱을 삭제하지 않아도 됩니다. 두 앱의 메모 목록은 별개이고 기존 메모는 자동 복사하지 않습니다. 정밀 인식은 64비트 기기가 필요하며, 실패하면 원본 WAV와 기본 인식 결과를 보존합니다.

모델 추가로 APK 용량·설치 공간·메모리 사용량·저장 후 처리 시간이 늘어납니다. 저장 공간 2 GB 이상 여유를 권장합니다. 실제 속도는 기기별로 다릅니다. '녹음 끝' 후 한국어 정밀 인식 안내가 사라지면 목록에서 최종 결과를 확인하세요. 진행률은 처리한 오디오 길이 기준입니다.

**재시험:** '아름다운 선풍기 유리컵'을 말하고 잠시 쉰 다음 '녹음 끝'이라고 말한 뒤 2초 정도 기다리세요. 같은 문장을 조용한 환경에서 3회 말해 기존 앱과 비교하세요. '아름다운 유리컵', '선풍기를 켜 주세요' 등 다른 문장도 확인하세요. 실제 사용자 녹음은 제공받지 않아 개인 발음에서의 개선은 미검증입니다.

프로젝트 수정 → main 반영 → GitHub Actions 자동 빌드 → APK 다운로드 → 스마트폰 테스트 흐름입니다. Windows PC에 Android Studio, Android SDK 또는 JDK를 설치할 필요가 없습니다. **클라우드 빌드 결과는 Actions에서 확인할 수 있습니다. 실기기 음성 인식 테스트는 별도로 필요합니다.**

1. GitHub에 로그인하고 위 저장소에 접속합니다.
2. 상단 **Actions** 메뉴를 누릅니다.
3. 왼쪽 **Android Build**를 선택합니다.
4. 가장 최근 **초록색 체크 표시가 있는 성공한 실행**을 선택합니다. 커밋이 원하는 수정본인지도 확인합니다.
5. 실행 결과 아래 **Artifacts** 영역을 찾습니다.
6. **voice-memo-debug-apk**를 눌러 ZIP을 다운로드합니다.
7. ZIP 압축을 풀면 `app-debug.apk`가 있습니다.
8. USB로 APK를 스마트폰에 복사합니다.
9. 스마트폰 파일 앱에서 APK를 누르고, 필요한 경우 해당 파일 앱의 ‘알 수 없는 앱 설치’를 허용합니다.
10. 설치 후 앱을 열고 마이크 권한을 허용합니다. 첫 실행부터 자동 녹음합니다.

APK Artifact는 30일 보관합니다. 만료되면 Actions → Android Build → **Run workflow** → main → Run workflow로 다시 빌드하세요. GitHub가 저장소의 Actions 활성화를 요청하면 허용하세요. Private 저장소에서는 다운로드 계정에 저장소 접근 권한이 필요합니다. GitHub 계정의 Actions 실행 한도도 적용됩니다.

Debug APK의 기본 서명 키는 새 클라우드 실행 환경마다 달라질 수 있습니다. 이전 설치에 덮어쓰기가 서명 오류로 실패하면 기존 앱을 삭제하고 설치해야 하며, **삭제 시 메모도 없어집니다**. 지속적인 업데이트용 고정 서명은 별도 키/Secrets 설정이 필요합니다. 현재 APK는 초기 테스트 용도입니다.

## GitHub Actions 구성

`.github/workflows/android-build.yml`은 push, main 대상 PR 및 수동 실행을 지원합니다. Ubuntu 24.04의 격리된 빌드 환경에서 JDK 17, SDK 35, Build Tools 35.0.0을 준비하고 다음을 실행합니다.

```sh
python3 scripts/prepare_model.py
chmod +x gradlew
./gradlew --no-daemon --console=plain --stacktrace testDebugUnitTest lintDebug assembleDebug
```

Gradle 8.11.1 Wrapper(스크립트/JAR/설정)가 프로젝트에 포함되어 있습니다. AGP 8.9.1, Kotlin 2.1.10, Compose, Room 버전을 명시했습니다. `local.properties` 또는 Windows 절대 경로에 의존하지 않습니다. 모델은 공식 URL에서 빌드 중 받으며 ZIP 무결성과 파일 구조를 검사하고 APK assets에 포함합니다. 모델이 빠지면 preBuild가 실패합니다. APK 생성 후 INTERNET 권한이 없는지, 한국어 모델과 지정 ABI의 Vosk와 sherpa-onnx 라이브러리가 포함됐는지 검사합니다.

Room 저장/삭제/복구는 Robolectric 단위 테스트로 실행하므로 에뮬레이터가 필요하지 않습니다. 테스트·lint 실패 시 성공 APK Artifact를 업로드하지 않습니다. 진단 보고서는 `android-build-reports`에 14일간 보관합니다.

### 빌드 실패 확인

Actions의 빨간색 실행 → **build** 작업 → 빨간색 단계의 로그를 엽니다. `--stacktrace`가 오류 원인을 표시합니다.

* Prepare bundled offline Korean model 실패: 공식 모델 배포처 통신/ZIP 오류. 일시적이면 Re-run failed jobs를 사용합니다.
* SDK 설치 실패: Android 배포처 연결이나 GitHub runner 상태를 확인합니다.
* Build, unit tests and lint 실패: 첫 `e:` 또는 `FAILURE` 문구와 파일/줄을 확인합니다. `android-build-reports`의 테스트 XML/HTML, lint 보고서도 확인합니다.
* Verify APK 실패: 모델/ABI 누락 또는 INTERNET 권한 추가 여부를 확인합니다.
* Actions 실행 자체가 안 됨: 저장소 Settings → Actions 허용 여부, 기본 브랜치 및 GitHub 실행 한도를 확인합니다.

PC 개발 도구 설치로 해결할 필요가 없습니다. GitHub에서 수정·커밋하면 클라우드에서 다시 검사합니다.

## 기능 및 설계

* 실행 시 마이크 권한 요청 후 자동 녹음. 저장/취소 후 목록으로 복귀합니다. 화면 회전으로 재시작하지 않습니다.
* AudioRecord 하나에서 16 kHz, 16-bit, mono PCM을 읽어 내부 WAV 파일에 기록합니다. WAV는 일반 재생 가능한 무손실 오디오 형식이며, m4a 대신 강제 종료 복구 용이성을 위해 선택했습니다. 분당 약 1.92 MB입니다.
* 녹음과 Vosk 처리를 별도 IO 작업으로 실행합니다. 모델 준비 중에도 녹음하며, 모델 로드 후 파일의 앞부분부터 인식합니다.
* 종료 명령 전용 인식기는 `녹음 끝`과 `<UNK>`를 포함한 제한 문법을 사용합니다. 발화 종료가 확정되고 명령만 인식했으며 각 단어의 신뢰도가 0.85 이상일 때만 종료합니다. 한국어 모델은 영문 예제의 `[unk]` 대신 `<UNK>`를 사용하며 빌드에서 사전 존재 여부와 비명령 음성의 오종료 여부를 검사합니다. 신뢰도는 실제 정확도 확률을 보장하지 않습니다. 일반 받아쓰기 내용은 전용 인식기로 대체하지 않습니다. 전용 인식기 오류 시 일반 받아쓰기와 수동 저장을 유지합니다.
* Vosk 일반 인식에서도 `acceptWaveForm()`이 발화 종료를 확정한 `result`에 `녹음\s*끝`이 있을 때만 종료합니다. 부분 결과로 종료하지 않습니다. 명령 후 잠시 침묵해야 확정됩니다. 띄어쓰기 없는 `녹음끝`도 허용하며 `녹음 끝까지 듣기`처럼 단어가 이어지는 문장은 제외합니다.
* 원본 WAV와 기본 확정 결과를 먼저 보존한 뒤 정밀 인식 결과로 본문과 제목을 갱신합니다. 28초를 넘는 녹음은 20~28초 구간의 저에너지 지점에서 나누며 모든 샘플을 처리합니다. 경계의 단어는 여전히 오인식될 수 있습니다. 제목/본문에서 종료 명령을 제거하지만 **원본 오디오에서는 제거하지 않습니다**.
* 제목은 정리된 텍스트 앞 20자, 빈 텍스트는 날짜·시간 제목입니다.
* Room에는 ID, 제목, 본문, WAV 절대 경로, 녹음 시작 일시, 실제 PCM 길이가 저장됩니다. 최신순 목록, 상세 재생/일시정지/탐색/삭제를 제공합니다.
* 전경 마이크 서비스와 wake lock으로 백그라운드·화면 꺼짐 중 계속 녹음합니다. 6시간에 자동 저장합니다. 전화 등 오디오 포커스 상실 또는 OS 마이크 차단 감지 시 보존 후 종료합니다.
* 인식/모델 로드 실패 시 녹음은 계속하며 수동 저장할 수 있습니다. 자동 음성 종료는 이 경우 사용할 수 없습니다.
* WAV 헤더를 매초 동기화하고 확정 텍스트를 임시 보존합니다. 다음 실행에서 미등록 WAV를 복구합니다. 0샘플 녹음은 저장하지 않으며 짧은 녹음은 파일이 있으면 보존합니다.
* 삭제·취소에는 삭제 표시 파일을 사용하여 중단 후 재실행 시 처리를 마칩니다. Android 강제 중지 자체를 막을 수는 없으며, 복구되는 텍스트는 마지막 확정 저장분입니다.

## 최소 버전과 기기

Android 8.0(API 26) 이상. compileSdk/targetSdk 35. arm64-v8a, armeabi-v7a, x86_64 ABI를 대상으로 합니다. 기기별 성능/마이크 정책/제조사 절전 정책 및 최신 OS 호환성은 실기기 확인이 필요합니다. STT 모델은 기기 음성 서비스 설치 여부와 무관합니다.

Vosk 모델은 약 82 MB이고 Whisper 정밀 인식 모델은 수백 MB가 추가됩니다. Vosk 모델은 앱 첫 실행 때 내부 noBackupFilesDir로 풉니다. 최초 설치 후 인터넷은 필요하지 않습니다. 설치/압축 해제/녹음 여유를 위해 2 GB 이상 여유 공간을 권장합니다. 인식 정확도는 한국어 발음·소음·마이크·기기 성능에 따라 달라지고 아직 실측하지 않았습니다.

## 선택 사항: 개발자가 별도 환경에서 직접 빌드

일반 사용자는 이 절차를 실행하지 않아도 됩니다. GitHub Actions만 사용하세요. 별도 로컬 개발을 원하는 개발자에게만 해당하며 프로젝트 폴더의 PowerShell에서 실행합니다. **PC 빌드 준비 단계에는 인터넷이 필요**하며, 이것은 앱 실행 시 네트워크 요구와 별개입니다. JDK/Gradle/SDK는 프로젝트 `.tools`에 보관합니다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\prepare.ps1
python scripts/prepare_model.py
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1
```

prepare는 JDK 17, Gradle 8.11.1, Android SDK 35, 모델, Apache 라이선스를 공식 배포처에서 다운로드합니다. Android SDK 라이선스는 화면에서 직접 확인하고 동의합니다. 이미 읽고 동의한 경우에만 `-AcceptAndroidLicenses`를 전달합니다. Gradle wrapper도 생성됩니다. AGP 8.9.1, Kotlin 2.1.10, Compose, Room, Vosk 등의 Maven 의존성을 최초 빌드 시 내려받습니다.

빌드 스크립트는 단위 테스트, lint, debug APK 조립을 실행한 뒤 다음 위치에 복사합니다.

* `dist/VoiceMemo-debug.apk`
* 원본: `app/build/outputs/apk/debug/app-debug.apk`

Android Studio에서는 준비 스크립트 실행 후 루트 폴더를 엽니다. Gradle JDK를 17로 설정하고 Sync 후 Build APK를 실행합니다. 이미 모든 도구와 의존성이 캐시되었으면 `gradlew.bat --offline assembleDebug`로 빌드할 수 있습니다. 테스트용 debug 서명이며 스토어 배포용 서명은 별도로 설정해야 합니다.

## 스마트폰 설치

1. 빌드된 APK를 USB로 스마트폰에 복사하고 파일 앱에서 엽니다.
2. 해당 파일 앱의 ‘알 수 없는 앱 설치’를 허용하여 설치합니다.
3. 또는 개발자 옵션/USB 디버깅을 켜고 PC에서 실행합니다.

```powershell
.\.tools\android-sdk\platform-tools\adb.exe devices
.\.tools\android-sdk\platform-tools\adb.exe install -r .\dist\VoiceMemo-debug.apk
```

4. 앱을 열어 마이크를 허용합니다. 바로 녹음이 시작되며, 첫 모델 압축 해제 중에도 음성은 저장됩니다.
5. ‘내일 오전 열 시에 회의 준비. 녹음 끝’이라고 말한 뒤 잠시 침묵합니다. 목록 복귀 및 본문/오디오를 확인합니다.

Android 13 이상 알림 권한을 자동 요청하지 않아 최초 녹음 흐름을 방해하지 않습니다. 권한이 없어도 전경 서비스는 실행되지만 알림 서랍의 저장 액션이 보이지 않을 수 있습니다. 필요하면 시스템 앱 설정에서 알림을 허용합니다.

## 비행기 모드 전체 테스트

APK를 설치한 뒤 **앱 최초 실행 전에** 비행기 모드를 켜고 Wi-Fi/모바일 데이터도 꺼 주세요. 모델은 APK에 포함되므로 첫 실행부터 가능해야 합니다.

| 시험 | 기대 결과 |
|---|---|
| 최초 실행, 권한 허용 | 자동 녹음·모델 로드·시간 표시 |
| 한국어 메모 + ‘녹음 끝’ + 침묵 | 확정 결과 후 자동 저장, 목록 복귀 |
| 발화 중 부분 결과 | 확정 전에는 종료하지 않음 |
| 잠시 쉰 뒤 ‘녹음끝’ 발화 + 약 2초 침묵 | 별도 명령 인식 경로에서도 자동 저장 |
| ‘녹음 끝까지 듣기’, ‘선풍기를 켜 주세요’, 무음 | 이 문장이나 무음만으로 자동 저장되지 않음 |
| ‘아름다운 선풍기 유리컵’ 3회 비교 | 정밀 인식 완료 후 본문을 원본 재생과 대조 |
| 목록/상세 확인 | 최신순, 날짜·길이, 전체 본문, 명령 제거 |
| 재생/일시정지/위치 탐색 | 원본 녹음 정상 재생 |
| 삭제 후 재실행 | DB 행과 오디오 모두 없어짐 |
| 취소 후 재실행 | 취소 녹음이 복구 목록에 나타나지 않음 |
| 권한 거부/설정에서 허용 | 충돌 없이 목록 표시, 허용 후 새 녹음 가능 |
| 1초 미만 수동 저장 | PCM이 있으면 메모 보존, 없으면 생성하지 않음 |
| 화면 회전/홈 버튼/화면 끄기 | 중복 녹음 없음, 서비스 녹음 계속 |
| 전화 수신 | OS가 포커스/마이크를 넘기면 기존 녹음 보존 후 종료 |
| 시스템 설정에서 강제 중지 후 재실행 | 미등록 WAV 복구, 새 녹음 시작 |
| 모델 누락/손상 테스트 빌드 | 안내와 수동 저장 가능, 자동 종료 불가 |
| 공간 부족 | 오류 안내, 기록 가능한 구간 보존·재실행 복구 시도 |

마지막 세 가지 장애 시험은 에뮬레이터 또는 별도 테스트 기기에서 수행하세요. 음성 명령 품질은 조용한 곳·이동 중·소음 환경에서 여러 한국어 문장으로 실제 평가해야 합니다. 모델 테스트 빌드는 preBuild 검사를 의도적으로 변경한 경우에만 만들 수 있습니다.

## 파일 구조

* `Data.kt`: Room Entity/DAO/DB, Repository, 제목 및 명령 처리, WAV 헤더/복구.
* `MemoViewModel.kt`: 목록 StateFlow, 복구 및 UI 명령 연결.
* `RecordingService.kt`: 전경 녹음, AudioRecord, WAV 저장, 오프라인 STT, 음성 종료, 예외 보존.
* `StopCommand.kt`: 별도 제한 문법과 확정 결과·단어 신뢰도 검사로 종료 명령 보강.
* `KoreanTranscriber.kt`: 한국어 고정 Whisper 정밀 인식, PCM 변환 및 긴 녹음 분할.
* `scripts/compare_stt.py`: 공개 한국어 음성 4개로 기존/정밀 인식 비교. 사용자 음성은 사용하지 않습니다.
* `OfflineModel.kt`: APK asset ZIP의 안전한 로컬 압축 해제 및 Vosk 로드.
* `MainActivity.kt`: Compose 권한 흐름, 녹음/목록/상세, MediaPlayer 재생.
* `app/src/test/.../MemoTest.kt`: 명령·제목·WAV 복구 단위 테스트.
* `app/src/test/.../MemoRepositoryTest.kt`: Robolectric Room 저장, 실제 파일 동반 삭제, 고아 녹음 복구, 취소 복구 방지 테스트.
* `.github/workflows/android-build.yml`: 클라우드 빌드, 테스트, APK 검사, Artifact 업로드.
* `gradle/wrapper`, `gradlew`, `gradlew.bat`: 공식 Gradle Wrapper.
* `scripts/prepare_model.py`: 클라우드에서 한국어 모델과 라이선스 준비.
* `scripts/*.ps1`: 선택적인 로컬 개발용(일반 사용자는 불필요).
* `licenses/THIRD_PARTY.md`: STT·모델·의존성 라이선스 및 출처.

## 개인정보 보호

INTERNET 권한은 선언하지 않으며 라이브러리에서 추가되더라도 manifest merger에서 제거합니다. 서버/계정/광고/분석/추적 SDK를 사용하지 않습니다. 음성 및 본문은 앱 전용 내부 저장소와 Room DB에만 저장하고 백업·기기간 전송을 비활성화했습니다. 앱 삭제 시 데이터도 삭제됩니다. OS의 앱 격리/기기 암호화에 의존하며 별도 앱 비밀번호나 DB 자체 암호화는 구현하지 않았습니다.

## 검증 기록

2026-09-29 실행 36509650747, 코드 커밋 f04f967d3d5dace3e48ff8652a9380656c4bee87에서 종료 명령 개선판의 컴파일·JUnit/Robolectric·lint·APK 조립·권한/모델/ABI 검사를 통과했습니다. 종료 명령 어휘와 `<UNK>`가 모델 사전에 존재하고, 공개 한국어 비명령 음성 4개와 무음에서 전용 인식기가 오종료하지 않는지 실제 모델로 확인했습니다. 실제 ‘녹음 끝’ 발음의 성공률·거리·소음별 평가는 아직 실기기 미검증입니다.

한국어 정밀 인식 추가 빌드 36426821794에서 컴파일·단위 테스트·lint·APK 권한/모델 검사를 통과했습니다. 공개 한국어 음성 4개의 공백·문장부호 제외 문자 오류율은 기존 Vosk 약 46.1%, Whisper 약 15.8%였습니다. 작은 공개 표본이며 일부 문장에서는 Whisper가 더 나빴습니다. 사용자 발음의 결과로 일반화할 수 없습니다.

2026-09-28 GitHub Actions 실행 36388522351에서 Gradle 컴파일, JUnit/Robolectric 단위 테스트, lint, Debug APK 조립이 모두 성공했습니다. 최종 APK의 INTERNET 권한 없음, 한국어 모델 포함, arm64-v8a/armeabi-v7a/x86_64의 Vosk 네이티브 라이브러리 포함 검사도 통과했습니다. Wrapper JAR는 공식 SHA-256과 대조했습니다.

실제 마이크·전화·화면 꺼짐·비행기 모드 한국어 인식률 및 16 KB page-size 기기 동작은 실기기 시험이 필요합니다. 자동 빌드 통과가 이 시험의 완료를 의미하지는 않습니다.

개선판 빌드는 공개 한국어 WAV 4개로 실제 모델 실행을 검사하고 stt-comparison.json을 보고서에 기록합니다. 이 작은 비교는 개인 발음·마이크·소음 환경의 인식률 보장이 아닙니다. Whisper는 무음이나 소음에서 잘못된 문장을 생성할 수 있으므로 원음과 대조해 주세요.
