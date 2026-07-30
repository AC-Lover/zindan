param(
    [Parameter(Mandatory = $true)]
    [string]$ApkPath,
    [string]$PackageName = "net.typeblog.shelter",
    [string]$Serial
)

$ErrorActionPreference = "Stop"
$apk = (Resolve-Path -LiteralPath $ApkPath).Path
$androidSdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
$adb = Join-Path $androidSdk "platform-tools\adb.exe"
$apksigner = Get-ChildItem -LiteralPath "$androidSdk\build-tools" -Directory |
    Sort-Object { [version]$_.Name } -Descending |
    ForEach-Object { Join-Path $_.FullName "apksigner.bat" } |
    Where-Object { Test-Path -LiteralPath $_ } |
    Select-Object -First 1

if (-not (Test-Path -LiteralPath $adb)) { throw "adb.exe was not found: $adb" }
if (-not $apksigner) { throw "apksigner.bat was not found under $androidSdk\build-tools" }
$adbArgs = @()
if ($Serial) { $adbArgs += @("-s", $Serial) }

$state = (& $adb @adbArgs get-state 2>&1).Trim()
if ($state -ne "device") { throw "ADB device is not connected or authorized: $state" }

$packagePathLine = & $adb @adbArgs shell pm path --user 0 $PackageName 2>&1 |
    Select-String -Pattern "^package:" |
    Select-Object -First 1
if (-not $packagePathLine) { throw "$PackageName is not installed for Android user 0" }
$remoteApk = $packagePathLine.Line.Substring("package:".Length).Trim()
$tempDir = Join-Path ([IO.Path]::GetTempPath()) ("zindan-signature-check-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $tempDir | Out-Null
$installedApk = Join-Path $tempDir "installed.apk"

try {
    & $adb @adbArgs pull $remoteApk $installedApk | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Unable to pull the installed APK" }

    function Get-CertDigest([string]$Path) {
        $line = & $apksigner verify --print-certs $Path 2>&1 |
            Select-String -Pattern "certificate SHA-256 digest:" |
            Select-Object -First 1
        if (-not $line) { throw "Unable to read certificate from $Path" }
        return (($line.Line -split ": ")[-1] -replace ":", "").Trim().ToLowerInvariant()
    }

    $installedCert = Get-CertDigest $installedApk
    $candidateCert = Get-CertDigest $apk
    Write-Host "Installed certificate: $installedCert"
    Write-Host "Candidate certificate: $candidateCert"
    if ($installedCert -ne $candidateCert) {
        throw "APK is NOT update-compatible. Do not uninstall Zindan or install this APK."
    }
    Write-Host "Compatible: Android can install this APK as an update without removing the work profile."
} finally {
    Remove-Item -LiteralPath $tempDir -Recurse -Force -ErrorAction SilentlyContinue
}
