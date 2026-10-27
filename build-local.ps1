# Run in PowerShell: powershell -NoProfile -ExecutionPolicy Bypass -File .\build-local.ps1
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$gradleVersion = "8.8"
$toolsDir = Join-Path $root ".gradle-dist"
$gradleDir = Join-Path $toolsDir "gradle-$gradleVersion"
$gradleExe = Join-Path $gradleDir "bin\gradle.bat"
if (!(Test-Path $gradleExe)) {
    New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
    $zipPath = Join-Path $toolsDir "gradle-$gradleVersion-bin.zip"
    Write-Host "Downloading official Gradle $gradleVersion distribution..."
    Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip" -OutFile $zipPath
    Expand-Archive -LiteralPath $zipPath -DestinationPath $toolsDir -Force
    Remove-Item $zipPath
}
Write-Host "Java version:"
& java -version
if ($LASTEXITCODE -ne 0) { throw "Java 17 JDK is required." }
Push-Location $root
try {
    & $gradleExe --no-daemon clean build --stacktrace
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed. Send the terminal output or build log." }
    Write-Host "Build completed. JAR files:"
    Get-ChildItem -Path (Join-Path $root "build\libs") -Filter "*.jar" | Select-Object -ExpandProperty FullName
} finally {
    Pop-Location
}
