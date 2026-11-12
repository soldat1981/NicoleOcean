$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
New-Item -ItemType Directory -Force -Path '.tools' | Out-Null
$base = 'https://dl.google.com/android/repository/'
Invoke-WebRequest -Uri ($base+'repository2-1.xml') -OutFile '.tools\repository.xml'
[xml]$repo = Get-Content -LiteralPath '.tools\repository.xml'
$requests = @(
    @{ Package='build-tools;35.0.0'; Url='build-tools_r35_windows.zip'; Zip='build-tools.zip'; Destination='build-tools' },
    @{ Package='platforms;android-35'; Url='platform-35_r02.zip'; Zip='platform.zip'; Destination='platform' }
)
foreach ($request in $requests) {
    $archive = $repo.SelectNodes("//*[local-name()='remotePackage']") | Where-Object { $_.path -eq $request.Package } | ForEach-Object { $_.archives.archive } | Where-Object { $_.complete.url -eq $request.Url } | Select-Object -First 1
    if (!$archive) { throw "Official package not found: $($request.Package)" }
    $zip = Join-Path '.tools' $request.Zip
    if (!(Test-Path -LiteralPath $zip)) { Invoke-WebRequest -Uri ($base+$request.Url) -OutFile $zip }
    $hash = (Get-FileHash -LiteralPath $zip -Algorithm SHA1).Hash.ToLowerInvariant()
    if ($hash -ne ([string]$archive.complete.checksum).ToLowerInvariant()) { throw "Checksum mismatch: $zip" }
    Expand-Archive -LiteralPath $zip -DestinationPath (Join-Path '.tools' $request.Destination) -Force
}
Write-Output 'Official Android build tools are ready.'

