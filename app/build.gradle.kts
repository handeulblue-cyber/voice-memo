plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}
android {
    namespace = "kr.local.voicememo"
    compileSdk = 35
    defaultConfig {
        applicationId = "kr.local.voicememo.korean"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1-korean"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
    }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    packaging { jniLibs.useLegacyPackaging = true; resources.excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1") }
    androidResources { noCompress += "zip" }
    testOptions { unitTests.isIncludeAndroidResources = true }
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("com.alphacephei:vosk-android:0.3.75@aar")
    implementation("net.java.dev.jna:jna:5.18.1@aar")
    implementation(files("libs/sherpa-onnx-1.12.26.aar"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
}

tasks.register("verifyOfflineModel") {
    doLast {
        val model = file("src/main/assets/korean.zip")
        check(model.exists() && model.length() > 50_000_000) {
            "Bundled Korean model is missing. Run python scripts/prepare_model.py before building."
        }
        check(file("src/main/assets/whisper-small.zip").length() > 100_000_000) { "Whisper model is missing" }
        check(file("libs/sherpa-onnx-1.12.26.aar").exists()) { "Local sherpa-onnx runtime is missing" }
    }
}
tasks.named("preBuild").configure { dependsOn("verifyOfflineModel") }
