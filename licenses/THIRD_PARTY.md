# 오프라인 음성 인식 및 의존성

* Vosk Android `com.alphacephei:vosk-android:0.3.75`: Apache License 2.0. https://github.com/alphacep/vosk-api
* 한국어 모델 `vosk-model-small-ko-0.22`: 공식 모델 목록에서 Apache License 2.0으로 표시. 압축 다운로드 약 82 MB. https://alphacephei.com/vosk/models
* 모델 원본: https://alphacephei.com/vosk/models/vosk-model-small-ko-0.22.zip
* JNA 5.18.1: Apache License 2.0 / LGPL 2.1 이중 라이선스. 본 프로젝트는 Apache 2.0 조건을 사용합니다. https://github.com/java-native-access/jna
* Whisper small (다국어 모델을 한국어 ko로 고정, INT8 ONNX): MIT License, Copyright (c) 2022 OpenAI. **OpenAI API를 호출하지 않으며 추론은 기기 내부에서만 실행합니다.** https://github.com/openai/whisper/blob/main/LICENSE
* sherpa-onnx 1.12.26: Apache License 2.0. https://github.com/k2-fsa/sherpa-onnx
* ONNX Runtime: MIT License. https://github.com/microsoft/onnxruntime/blob/main/LICENSE
* 변환된 Whisper 배포 파일: https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-whisper-small.tar.bz2

원본의 정수 양자화 encoder/decoder와 tokens 파일만 APK의 whisper-small.zip에 포함합니다. prepare_model.py는 Whisper 및 ONNX Runtime의 MIT 전문도 APK에 포함합니다. sherpa-onnx AAR는 GitHub release에서 제공하는 SHA-256과 대조합니다.
* AndroidX / Jetpack Compose / Room: Apache License 2.0. https://android.googlesource.com/platform/frameworks/support/
* Kotlin / kotlinx.coroutines: Apache License 2.0. https://github.com/Kotlin/kotlinx.coroutines

GitHub Actions의 scripts/prepare_model.py는 Apache 2.0 전문을 공식 Apache 사이트에서 내려받아 APK assets에 포함합니다. 모델 ZIP은 원본 구조와 포함된 저작권 고지를 유지한 채 APK에 포함합니다. 공식 배포 파일의 저작권 고지와 NOTICE는 재배포 시 유지해야 합니다. 다운로드된 모델의 SHA-256을 model-sha256.txt에 기록합니다(공식 공급자 체크섬 대조를 의미하지 않습니다).

Apache 2.0은 조건을 준수하는 사용·수정·재배포 및 상업적 사용을 허용하며, 라이선스 사본과 해당 저작권/NOTICE 고지를 보존해야 합니다. 모델 수정 시 변경 사실을 명시해야 합니다. 보증은 제공되지 않습니다.

모델 바이너리와 Maven 의존성은 Git에 보관하지 않으며 클라우드 빌드 중 공식 배포처에서 다운로드합니다. 스마트폰에서는 네트워크 다운로드가 발생하지 않습니다.

Gradle Wrapper 8.11.1 역시 Apache 2.0입니다. 원본: https://github.com/gradle/gradle/tree/v8.11.1 . 포함된 Wrapper JAR의 SHA-256은 `2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046`이며 https://gradle.org/release-checksums/ 의 공식 값과 대조했습니다.
