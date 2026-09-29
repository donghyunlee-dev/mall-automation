Param(
    [switch]$Online
)

$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot

Write-Host "[start-backend] Using project-local Gradle cache: .\.gradle"
$env:GRADLE_USER_HOME = (Resolve-Path .\.gradle).Path

$argsList = @('bootRun','--no-daemon')
if (-not $Online) {
  $argsList += '--offline'
}

Write-Host "[start-backend] > .\\gradlew.bat $($argsList -join ' ')" -ForegroundColor Cyan
try {
  & .\gradlew.bat @argsList 2>&1 | Tee-Object -FilePath server.log
  $exit = $LASTEXITCODE
} catch {
  $exit = 1
}

if ($exit -ne 0) {
  Write-Host "[start-backend] Gradle wrapper failed (exit $exit)." -ForegroundColor Yellow
  Write-Host "[start-backend] If this is due to missing wrapper distribution or dependencies:" -ForegroundColor Yellow
  Write-Host "  - Temporarily allow network OR run with -Online to permit downloads." -ForegroundColor Yellow
  Write-Host "  - All downloads stay inside: $env:GRADLE_USER_HOME" -ForegroundColor Yellow
}

Pop-Location
exit $exit

