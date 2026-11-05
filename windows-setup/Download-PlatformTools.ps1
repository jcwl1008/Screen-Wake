$ErrorActionPreference = 'Stop'
$zip = Join-Path $PSScriptRoot 'platform-tools-windows.zip'
$uri = 'https://dl.google.com/android/repository/platform-tools-latest-windows.zip'
Write-Host 'Downloading Android Platform Tools from Google...'
Invoke-WebRequest -Uri $uri -OutFile $zip -UseBasicParsing
Expand-Archive -LiteralPath $zip -DestinationPath $PSScriptRoot -Force
if (-not (Test-Path -LiteralPath (Join-Path $PSScriptRoot 'platform-tools\adb.exe'))) {
    throw 'The official download did not contain adb.exe. Setup has not been started.'
}
Remove-Item -LiteralPath $zip
Write-Host 'Ready. Open START_SETUP.cmd to begin USB setup.'
