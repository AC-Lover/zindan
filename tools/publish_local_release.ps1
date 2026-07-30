param(
    [string]$Tag,
    [string]$Repository = "GiorgioVik/zindan"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$versionProperties = @{}
Get-Content -LiteralPath "$projectRoot\version.properties" | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') { $versionProperties[$matches[1]] = $matches[2] }
}
$version = $versionProperties.VERSION_NAME
$code = $versionProperties.VERSION_CODE
if (-not $version -or -not $code) { throw "Invalid version.properties" }
$expectedTag = "v$version-$code"
if (-not $Tag) { $Tag = $expectedTag }
if ($Tag -ne $expectedTag) {
    throw "Tag $Tag does not match version.properties ($expectedTag)"
}

Push-Location $projectRoot
try {
    $status = git status --porcelain
    if ($LASTEXITCODE -ne 0) { throw "Unable to read Git status" }
    if ($status) { throw "Commit or remove all working-tree changes before publishing" }

    powershell.exe -NoProfile -ExecutionPolicy Bypass `
        -File "$PSScriptRoot\build_local_release.ps1"
    if ($LASTEXITCODE -ne 0) { throw "Signed release build failed" }

    $apk = Get-ChildItem -LiteralPath "$projectRoot\app\build\outputs\apk\release" `
        -Filter "Zindan-$version-($code)-release.apk" |
        Select-Object -First 1
    if (-not $apk) { throw "Expected release APK was not found" }
    $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $apk.FullName).Hash.ToLowerInvariant()
    $checksumPath = "$($apk.FullName).sha256"
    [IO.File]::WriteAllText($checksumPath, "$hash  $($apk.Name)`n")

    $notesOverride = Join-Path $projectRoot "RELEASE_NOTES_$Tag.md"
    $notesPath = Join-Path ([IO.Path]::GetTempPath()) "zindan-$Tag-release-notes.md"
    if (Test-Path -LiteralPath $notesOverride) {
        Copy-Item -LiteralPath $notesOverride -Destination $notesPath -Force
    } else {
        $lines = Get-Content -Encoding utf8 -LiteralPath "$projectRoot\CHANGELOG.md"
        $start = -1
        for ($i = 0; $i -lt $lines.Count; $i++) {
            if ($lines[$i] -eq "$version ($code)") { $start = $i + 1; break }
        }
        if ($start -lt 0) { throw "CHANGELOG.md has no section for $version ($code)" }
        $section = New-Object System.Collections.Generic.List[string]
        for ($i = $start; $i -lt $lines.Count; $i++) {
            if ($lines[$i] -match '^\d+\.\d+\.\d+ \(\d+\)$') { break }
            if ($lines[$i] -ne '===') { $section.Add($lines[$i]) }
        }
        $notes = @(
            "## Zindan $version ($code)",
            ""
        ) + $section + @(
            "",
            "### Install",
            "",
            "Check signing compatibility according to SIGNING.md before updating an existing install.",
            "Do not uninstall Zindan or remove the work profile for an update."
        )
        [IO.File]::WriteAllLines($notesPath, $notes, [Text.UTF8Encoding]::new($false))
    }

    git rev-parse --verify "refs/tags/$Tag" 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) {
        git tag -a $Tag -m "Zindan $version ($code)"
        if ($LASTEXITCODE -ne 0) { throw "Unable to create tag $Tag" }
    }
    git push origin $Tag
    if ($LASTEXITCODE -ne 0) { throw "Unable to push tag $Tag" }

    gh release view $Tag --repo $Repository *> $null
    if ($LASTEXITCODE -eq 0) {
        gh release upload $Tag $apk.FullName $checksumPath --repo $Repository --clobber
        gh release edit $Tag --repo $Repository --title "Zindan $version ($code)" --notes-file $notesPath
    } else {
        gh release create $Tag $apk.FullName $checksumPath --repo $Repository `
            --verify-tag --title "Zindan $version ($code)" --notes-file $notesPath
    }
    if ($LASTEXITCODE -ne 0) { throw "GitHub release publication failed" }
    Write-Host "Published https://github.com/$Repository/releases/tag/$Tag"
} finally {
    Pop-Location
}
