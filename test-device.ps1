param([string]$JavaHome='C:\Program Files\Java\jdk-20',[string]$Adb='C:\platform-tools\adb.exe',[string]$Serial='emulator-5554',[string]$Mode='')
$ErrorActionPreference='Stop'
function Check-Exit([string]$step){if($LASTEXITCODE -ne 0){throw "$step failed: $LASTEXITCODE"}}
if($Serial -notmatch '^emulator-'){throw 'DeviceTests replaces the game profile. This runner is restricted to a disposable emulator.'}
Set-Location -LiteralPath $PSScriptRoot
$tools=Join-Path $PSScriptRoot '.tools\build-tools\android-15'
$androidJar=Join-Path $PSScriptRoot '.tools\platform\android-35\android.jar'
$env:JAVA_HOME=$JavaHome
New-Item -ItemType Directory -Force -Path 'build\device-tests','build\device-dex' | Out-Null
& "$JavaHome\bin\javac.exe" -encoding UTF-8 -source 8 -target 8 -Xlint:-options -cp "$androidJar;build\classes" -d 'build\device-tests' 'tests\DeviceTests.java'
if($LASTEXITCODE -ne 0){throw 'Device test compilation failed'}
$classes=@(Get-ChildItem 'build\device-tests' -Recurse -Filter '*.class' | ForEach-Object {$_.FullName})
& "$JavaHome\bin\java.exe" -cp "$tools\lib\d8.jar" com.android.tools.r8.D8 --lib $androidJar --classpath 'build\classes' --min-api 26 --output 'build\device-dex' $classes
if($LASTEXITCODE -ne 0){throw 'Device test dex failed'}
& "$tools\aapt2.exe" link -o 'build\device-tests.apk' -I $androidJar --manifest 'tests\AndroidManifest.xml'
Check-Exit 'Android test setup'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip=[IO.Compression.ZipFile]::Open((Join-Path $PSScriptRoot 'build\device-tests.apk'),[IO.Compression.ZipArchiveMode]::Update)
try{[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,(Join-Path $PSScriptRoot 'build\device-dex\classes.dex'),'classes.dex') | Out-Null}finally{$zip.Dispose()}
& "$tools\zipalign.exe" -f 4 'build\device-tests.apk' 'build\device-tests-aligned.apk'
Check-Exit 'Android test setup'
& "$JavaHome\bin\java.exe" -jar "$tools\lib\apksigner.jar" sign --ks '.signing\nicole-release.p12' --ks-pass 'file:.signing/password.txt' --out 'build\device-tests-signed.apk' 'build\device-tests-aligned.apk'
Check-Exit 'Android test setup'
& $Adb -s $Serial install --no-incremental -r 'dist\NicoleOcean.apk'
Check-Exit 'Android test setup'
& $Adb -s $Serial install --no-incremental -r -t 'build\device-tests-signed.apk'
Check-Exit 'Android test setup'
& $Adb -s $Serial shell am force-stop com.nicole.ocean
Check-Exit 'Android test setup'
$result = & $Adb -s $Serial shell am instrument -w -e mode $Mode com.nicole.ocean.test/com.nicole.ocean.DeviceTests
$result | Write-Output
if (($result -join "`n") -notmatch 'INSTRUMENTATION_RESULT: result=PASS:') { throw 'Android instrumentation did not pass' }


