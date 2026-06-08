# build-versions.ps1
# Builds CraftStats Fabric for every version in versions/fabric-*.properties
# Output jars are collected into dist/

param(
    [string[]]$Versions = @()  # Optional: limit to specific versions, e.g. "1.21.4","1.21.6"
)

$root = $PSScriptRoot
$distDir = "$root\dist"
$gradleProps = "$root\gradle.properties"
$backupProps = "$root\gradle.properties.bak"
$versionsDir = "$root\versions"

# Gradle system settings preserved across all version builds
$systemSettings = @(
    "org.gradle.jvmargs=-Xmx4096m",
    "org.gradle.parallel=true",
    "org.gradle.caching=true"
)

# Collect version files
$versionFiles = Get-ChildItem "$versionsDir\fabric-*.properties" | Sort-Object Name

if ($Versions.Count -gt 0) {
    $versionFiles = $versionFiles | Where-Object {
        $mc = $_.BaseName -replace '^fabric-', ''
        $Versions -contains $mc
    }
}

if ($versionFiles.Count -eq 0) {
    Write-Host "No matching version files found in $versionsDir" -ForegroundColor Red
    exit 1
}

Write-Host "Building $($versionFiles.Count) version(s): $($versionFiles.BaseName -join ', ')" -ForegroundColor Cyan

# Back up original gradle.properties
Copy-Item $gradleProps $backupProps -Force

$results = @()

try {
    New-Item -ItemType Directory -Force -Path $distDir | Out-Null

    foreach ($vf in $versionFiles) {
        $mcVersion = $vf.BaseName -replace '^fabric-', ''
        Write-Host "`n=== Building MC $mcVersion ===" -ForegroundColor Yellow

        # Build new gradle.properties: system lines + version-specific lines
        $versionLines = Get-Content $vf.FullName
        $merged = ($systemSettings + @("") + $versionLines) -join "`n"
        Set-Content -Path $gradleProps -Value $merged -Encoding utf8

        # Run fabric build
        $startTime = Get-Date
        & "$root\gradlew.bat" :fabric:build --rerun-tasks 2>&1 | Tee-Object -Variable buildOutput

        $exitCode = $LASTEXITCODE
        $elapsed = [math]::Round(((Get-Date) - $startTime).TotalSeconds, 1)

        if ($exitCode -eq 0) {
            # Find the remapped jar (no classifier = final jar)
            $jarPattern = "$root\fabric\build\libs\craftstats-*-$mcVersion.jar"
            $jar = Get-ChildItem "$root\fabric\build\libs\craftstats-*.jar" |
                   Where-Object { $_.Name -notmatch '-(dev|sources|dev-shadow)' } |
                   Select-Object -First 1

            if ($null -eq $jar) {
                # Fallback: grab any remapped jar
                $jar = Get-ChildItem "$root\fabric\build\libs\craftstats-*.jar" |
                       Where-Object { $_.Name -notmatch '-(dev|sources|dev-shadow)' } |
                       Select-Object -Last 1
            }

            if ($null -ne $jar) {
                $destName = "craftstats-fabric-$mcVersion.jar"
                Copy-Item $jar.FullName "$distDir\$destName" -Force
                Write-Host "  Collected: $destName" -ForegroundColor Green
                $results += [PSCustomObject]@{ Version=$mcVersion; Status="OK"; Jar=$destName; Time="${elapsed}s" }
            } else {
                Write-Host "  Build succeeded but no jar found in fabric/build/libs" -ForegroundColor DarkYellow
                $results += [PSCustomObject]@{ Version=$mcVersion; Status="NO_JAR"; Jar=""; Time="${elapsed}s" }
            }
        } else {
            Write-Host "  BUILD FAILED (exit $exitCode)" -ForegroundColor Red
            $results += [PSCustomObject]@{ Version=$mcVersion; Status="FAILED"; Jar=""; Time="${elapsed}s" }
        }
    }
} finally {
    # Always restore original gradle.properties
    Copy-Item $backupProps $gradleProps -Force
    Remove-Item $backupProps -Force -ErrorAction SilentlyContinue
    Write-Host "`nRestored original gradle.properties" -ForegroundColor Gray
}

# Summary table
Write-Host "`n=== Build Summary ===" -ForegroundColor Cyan
$results | Format-Table -AutoSize

$failed = $results | Where-Object { $_.Status -ne "OK" }
if ($failed.Count -gt 0) {
    Write-Host "Failed versions: $($failed.Version -join ', ')" -ForegroundColor Red
    exit 1
} else {
    Write-Host "All $($results.Count) version(s) built successfully. Jars in: $distDir" -ForegroundColor Green
}
