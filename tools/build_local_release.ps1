param(
    [string]$KeystorePath,
    [string]$KeyAlias,
    [string]$StorePassword,
    [string]$KeyPassword,
    [string]$ExpectedCertSha256 = "5306981ed4923615bf9b6ac6ca97d3f5bad2d993afa47cf451aef2baec95e89c"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$KeystorePath = if ($KeystorePath) {
    $KeystorePath
} elseif (Test-Path -LiteralPath "D:\ZindanSigning\zindan-update.keystore") {
    "D:\ZindanSigning\zindan-update.keystore"
} else {
    "$env:USERPROFILE\.android\debug.keystore"
}
$keystore = (Resolve-Path -LiteralPath $KeystorePath).Path
$credentialPath = Join-Path (Split-Path -Parent $keystore) "signing-credentials.xml"
if ((-not $StorePassword -or -not $KeyPassword -or -not $KeyAlias) -and
    (Test-Path -LiteralPath $credentialPath)) {
    $credential = Import-Clixml -LiteralPath $credentialPath
    $plainPassword = $credential.GetNetworkCredential().Password
    if (-not $KeyAlias) { $KeyAlias = $credential.UserName }
    if (-not $StorePassword) { $StorePassword = $plainPassword }
    if (-not $KeyPassword) { $KeyPassword = $plainPassword }
}
if (-not $KeyAlias) { $KeyAlias = "androiddebugkey" }
if (-not $StorePassword) { $StorePassword = "android" }
if (-not $KeyPassword) { $KeyPassword = "android" }
$javaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "C:\Program Files\Android\Android Studio\jbr" }
$androidSdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
$apksigner = Get-ChildItem -LiteralPath "$androidSdk\build-tools" -Directory |
    Sort-Object { [version]$_.Name } -Descending |
    ForEach-Object { Join-Path $_.FullName "apksigner.bat" } |
    Where-Object { Test-Path -LiteralPath $_ } |
    Select-Object -First 1

if (-not $apksigner) {
    throw "apksigner.bat was not found under $androidSdk\build-tools"
}

$oldEnvironment = @{
    JAVA_HOME = $env:JAVA_HOME
    ZINDAN_KEYSTORE_PATH = $env:ZINDAN_KEYSTORE_PATH
    ZINDAN_KEYSTORE_PASSWORD = $env:ZINDAN_KEYSTORE_PASSWORD
    ZINDAN_KEY_ALIAS = $env:ZINDAN_KEY_ALIAS
    ZINDAN_KEY_PASSWORD = $env:ZINDAN_KEY_PASSWORD
}

try {
    $env:JAVA_HOME = $javaHome
    $env:ZINDAN_KEYSTORE_PATH = $keystore
    $env:ZINDAN_KEYSTORE_PASSWORD = $StorePassword
    $env:ZINDAN_KEY_ALIAS = $KeyAlias
    $env:ZINDAN_KEY_PASSWORD = $KeyPassword

    & "$projectRoot\gradlew.bat" --gradle-user-home "$projectRoot\.gradle-user-home" `
        :app:assembleRelease --stacktrace --no-daemon
    if ($LASTEXITCODE -ne 0) { throw "Gradle release build failed with exit code $LASTEXITCODE" }

    $apk = Get-ChildItem -LiteralPath "$projectRoot\app\build\outputs\apk\release" -Filter "Zindan-*.apk" |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if (-not $apk) { throw "Release APK was not produced" }

    $certLine = & $apksigner verify --print-certs $apk.FullName 2>&1 |
        Select-String -Pattern "certificate SHA-256 digest:" |
        Select-Object -First 1
    if (-not $certLine) { throw "Unable to read APK signing certificate" }
    $actualCert = (($certLine.Line -split ": ")[-1] -replace ":", "").Trim().ToLowerInvariant()
    $expectedCert = ($ExpectedCertSha256 -replace ":", "").Trim().ToLowerInvariant()
    if ($actualCert -ne $expectedCert) {
        throw "Signing certificate mismatch. Expected $expectedCert, got $actualCert"
    }

    $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $apk.FullName).Hash.ToLowerInvariant()
    Write-Host "Release APK: $($apk.FullName)"
    Write-Host "Certificate SHA-256: $actualCert"
    Write-Host "APK SHA-256: $hash"
} finally {
    foreach ($name in $oldEnvironment.Keys) {
        Set-Item -Path "Env:$name" -Value $oldEnvironment[$name] -ErrorAction SilentlyContinue
        if ($null -eq $oldEnvironment[$name]) { Remove-Item -Path "Env:$name" -ErrorAction SilentlyContinue }
    }
}
