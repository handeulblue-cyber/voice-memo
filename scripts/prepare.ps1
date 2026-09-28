param([switch]$AcceptAndroidLicenses)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$toolRoot = Join-Path $projectRoot '.tools'
New-Item -ItemType Directory -Force $toolRoot | Out-Null

function Download-Official([string]$Url, [string]$Destination) {
    if (Test-Path -LiteralPath $Destination) { return }
    Write-Host "Downloading $Url"
    $partial = "$Destination.partial"
    Invoke-WebRequest -Uri $Url -OutFile $partial -UseBasicParsing
    Move-Item -LiteralPath $partial -Destination $Destination -Force
}

Download-Official 'https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse' (Join-Path $toolRoot 'jdk.zip')
if (!(Test-Path (Join-Path $toolRoot 'jdk'))) {
    Expand-Archive -LiteralPath (Join-Path $toolRoot 'jdk.zip') -DestinationPath (Join-Path $toolRoot 'jdk')
}
$jdk = Get-ChildItem (Join-Path $toolRoot 'jdk') -Directory | Select-Object -First 1
$env:JAVA_HOME = $jdk.FullName
$env:PATH = "$($jdk.FullName)\bin;$env:PATH"
$env:GRADLE_USER_HOME = Join-Path $toolRoot 'gradle-home'

Download-Official 'https://services.gradle.org/distributions/gradle-8.11.1-bin.zip' (Join-Path $toolRoot 'gradle.zip')
Download-Official 'https://services.gradle.org/distributions/gradle-8.11.1-bin.zip.sha256' (Join-Path $toolRoot 'gradle.sha256')
$expected = (Get-Content (Join-Path $toolRoot 'gradle.sha256') -Raw).Trim()
if ((Get-FileHash (Join-Path $toolRoot 'gradle.zip') -Algorithm SHA256).Hash -ne $expected) { throw 'Gradle checksum mismatch' }
if (!(Test-Path (Join-Path $toolRoot 'gradle-8.11.1'))) { Expand-Archive -LiteralPath (Join-Path $toolRoot 'gradle.zip') -DestinationPath $toolRoot }

Download-Official 'https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip' (Join-Path $toolRoot 'android-tools.zip')
$sdk = Join-Path $toolRoot 'android-sdk'
$latest = Join-Path $sdk 'cmdline-tools/latest'
if (!(Test-Path (Join-Path $latest 'bin/sdkmanager.bat'))) {
    $unpack = Join-Path $toolRoot 'sdk-unpack'
    Expand-Archive -LiteralPath (Join-Path $toolRoot 'android-tools.zip') -DestinationPath $unpack -Force
    New-Item -ItemType Directory -Force (Split-Path -Parent $latest) | Out-Null
    Move-Item -LiteralPath (Join-Path $unpack 'cmdline-tools') -Destination $latest
}
$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk
$manager = Join-Path $latest 'bin/sdkmanager.bat'
if ($AcceptAndroidLicenses) {
    (1..30 | ForEach-Object { 'y' }) | & $manager "--sdk_root=$sdk" --licenses
} else {
    & $manager "--sdk_root=$sdk" --licenses
}
if ($LASTEXITCODE -ne 0) { throw 'Android SDK licenses were not accepted' }
& $manager "--sdk_root=$sdk" 'platform-tools' 'platforms;android-35' 'build-tools;35.0.0'
if ($LASTEXITCODE -ne 0) { throw 'Android SDK install failed' }

$model = Join-Path $projectRoot 'app/src/main/assets/korean.zip'
New-Item -ItemType Directory -Force (Split-Path -Parent $model) | Out-Null
Download-Official 'https://alphacephei.com/vosk/models/vosk-model-small-ko-0.22.zip' $model
# Verify ZIP structure before allowing a build.
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($model)
try { if (!$zip.GetEntry('vosk-model-small-ko-0.22/am/final.mdl')) { throw 'Unexpected model archive' } } finally { $zip.Dispose() }
Download-Official 'https://www.apache.org/licenses/LICENSE-2.0.txt' (Join-Path $projectRoot 'licenses/Apache-2.0.txt')
Copy-Item -LiteralPath (Join-Path $projectRoot 'licenses/Apache-2.0.txt') -Destination (Join-Path $projectRoot 'app/src/main/assets/Apache-2.0.txt') -Force
Copy-Item -LiteralPath (Join-Path $projectRoot 'licenses/THIRD_PARTY.md') -Destination (Join-Path $projectRoot 'app/src/main/assets/THIRD_PARTY.md') -Force
Get-FileHash -LiteralPath $model -Algorithm SHA256 | Format-List | Out-File (Join-Path $projectRoot 'licenses/model-sha256.txt') -Encoding utf8

# Java properties require ASCII escapes for Korean Windows paths.
$sdkForward = $sdk.Replace('\', '/')
$escaped = -join ($sdkForward.ToCharArray() | ForEach-Object { if ([int]$_ -gt 127) { '\u{0:x4}' -f [int]$_ } else { [string]$_ } })
[System.IO.File]::WriteAllText((Join-Path $projectRoot 'local.properties'), "sdk.dir=$escaped`n", [System.Text.Encoding]::ASCII)
Push-Location $projectRoot
try {
    & (Join-Path $toolRoot 'gradle-8.11.1/bin/gradle.bat') wrapper --gradle-version 8.11.1 --distribution-type bin
    if ($LASTEXITCODE -ne 0) { throw 'Gradle wrapper generation failed' }
} finally { Pop-Location }
Write-Host 'Preparation complete. Run scripts/build.ps1'
