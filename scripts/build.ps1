$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$toolRoot = Join-Path $projectRoot '.tools'
$jdk = Get-ChildItem (Join-Path $toolRoot 'jdk') -Directory | Select-Object -First 1
if (!$jdk) { throw 'Run scripts/prepare.ps1 first' }
$env:JAVA_HOME = $jdk.FullName
$env:PATH = "$($jdk.FullName)\bin;$env:PATH"
$env:GRADLE_USER_HOME = Join-Path $toolRoot 'gradle-home'
$env:ANDROID_HOME = Join-Path $toolRoot 'android-sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
Push-Location $projectRoot
try {
    & (Join-Path $toolRoot 'gradle-8.11.1/bin/gradle.bat') --no-daemon testDebugUnitTest lintDebug assembleDebug
    if ($LASTEXITCODE -ne 0) { throw 'Build/check failed. APK has not been delivered.' }
    New-Item -ItemType Directory -Force (Join-Path $projectRoot 'dist') | Out-Null
    Copy-Item -LiteralPath (Join-Path $projectRoot 'app/build/outputs/apk/debug/app-debug.apk') -Destination (Join-Path $projectRoot 'dist/VoiceMemo-debug.apk') -Force
    Get-FileHash (Join-Path $projectRoot 'dist/VoiceMemo-debug.apk') -Algorithm SHA256 | Format-List
} finally { Pop-Location }
