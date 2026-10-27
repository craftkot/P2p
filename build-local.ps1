# One physical JAR with embedded official e4mc 6.2.3. Requires JDK 17 and PowerShell 5+.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$gradleVersion = "8.1.1"
$toolsDir = Join-Path $root ".gradle-dist"
$gradleDir = Join-Path $toolsDir "gradle-$gradleVersion"
$gradleExe = Join-Path $gradleDir "bin\gradle.bat"
if (!(Test-Path $gradleExe)) {
    New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
    $zipPath = Join-Path $toolsDir "gradle-$gradleVersion-bin.zip"
    Write-Host "Downloading official Gradle $gradleVersion..."
    Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip" -OutFile $zipPath
    Expand-Archive -LiteralPath $zipPath -DestinationPath $toolsDir -Force
    Remove-Item $zipPath
}
& java -version
if ($LASTEXITCODE -ne 0) { throw "Java 17 JDK is required." }

$vendorDir = Join-Path $root ".e4mc-vendor\link\e4mc\e4mc-forge\6.2.3"
New-Item -ItemType Directory -Force -Path $vendorDir | Out-Null
$jar = Join-Path $vendorDir "e4mc-forge-6.2.3.jar"
if (!(Test-Path $jar)) {
    $primary = "https://cursemaven.com/curse/maven/e4mc-849519/9030240/e4mc-849519-9030240.jar"
    $fallback = "https://api.modrinth.com/maven/maven/modrinth/qANg5Jrr/VYizGDdc/qANg5Jrr-VYizGDdc.jar"
    try { Invoke-WebRequest -Uri $primary -OutFile $jar }
    catch { Invoke-WebRequest -Uri $fallback -OutFile $jar }
}
$digest = (Get-FileHash -Path $jar -Algorithm SHA256).Hash.ToLowerInvariant()
if ($digest -ne "3313a4087661a2678dcf0c26f45bb5a44aed45ad7a36b86878f60bbe1defa6b3") {
    throw "The upstream e4mc binary does not match pinned SHA256."
}
@'
<project xmlns="http://maven.apache.org/POM/4.0.0">
 <modelVersion>4.0.0</modelVersion>
 <groupId>link.e4mc</groupId>
 <artifactId>e4mc-forge</artifactId>
 <version>6.2.3</version>
 <packaging>jar</packaging>
</project>
'@ | Set-Content -Encoding UTF8 -Path (Join-Path $vendorDir "e4mc-forge-6.2.3.pom")
@'
<metadata>
 <groupId>link.e4mc</groupId>
 <artifactId>e4mc-forge</artifactId>
 <versioning><latest>6.2.3</latest><release>6.2.3</release>
  <versions><version>6.2.3</version></versions><lastUpdated>20261001000000</lastUpdated>
 </versioning>
</metadata>
'@ | Set-Content -Encoding UTF8 -Path (Join-Path (Split-Path $vendorDir -Parent) "maven-metadata.xml")

Push-Location $root
try {
    & $gradleExe --no-daemon clean jarJar --stacktrace
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed." }
    $dist = Get-ChildItem -Path (Join-Path $root "build\libs") -Filter "*-all.jar"
    if (!$dist) { throw "OneJar artifact was not generated." }
    Write-Host "Install ONLY this one-file OpenTogether JAR:"
    $dist | ForEach-Object { Write-Host $_.FullName }
} finally {
    Pop-Location
}
