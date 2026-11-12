param([string]$JavaHome = 'C:\Program Files\Java\jdk-20', [switch]$SkipModelTests)
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
Set-Location -LiteralPath $root
$tools = Join-Path $root '.tools\build-tools\android-15'
$androidJar = Join-Path $root '.tools\platform\android-35\android.jar'
$apk = 'dist\NicoleOcean.apk'
if (!(Test-Path -LiteralPath $androidJar)) { throw 'Run setup-tools.ps1 first.' }
if (!(Test-Path -LiteralPath "$JavaHome\bin\javac.exe")) { throw 'Pass -JavaHome with a JDK 17 or newer.' }
$env:JAVA_HOME = $JavaHome
function Check-Exit([string]$step) { if ($LASTEXITCODE -ne 0) { throw "$step failed: $LASTEXITCODE" } }
New-Item -ItemType Directory -Force -Path 'build\classes','build\dex','build\tests','dist','.signing' | Out-Null
$sources = @(Get-ChildItem -LiteralPath 'app\src' -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
& "$JavaHome\bin\javac.exe" -encoding UTF-8 -source 8 -target 8 -Xlint:-options -classpath $androidJar -d 'build\classes' $sources
Check-Exit 'Java compilation'
if (!$SkipModelTests) {
$model = @('Game','Combat','Oceans','Bossi','Nicole','Evolution','Expedition','Hazard','Difficulty','Paints','FrameStepper','HomeReef') | ForEach-Object { "app/src/com/nicole/ocean/$_.java" }
& "$JavaHome\bin\javac.exe" -encoding UTF-8 -d 'build/tests' $model 'tests/OceanRulesTest.java' 'tests/ReviewTest.java' 'tests/CombatTest.java' 'tests/LethalHazardTest.java' 'tests/HomeReefTest.java'
Check-Exit 'Test compilation'
& "$JavaHome\bin\java.exe" -ea -cp 'build/tests' com.nicole.ocean.OceanRulesTest
Check-Exit 'Ocean rules tests'
& "$JavaHome\bin\java.exe" -ea -cp 'build/tests' com.nicole.ocean.CombatTest
Check-Exit 'Needle combat tests'
& "$JavaHome\bin\java.exe" -ea -cp 'build/tests' com.nicole.ocean.LethalHazardTest
Check-Exit 'Lethal hazard tests'
& "$JavaHome\bin\java.exe" -ea -cp 'build/tests' com.nicole.ocean.HomeReefTest
Check-Exit 'Home reef tests'
& "$JavaHome\bin\java.exe" -ea -cp 'build/tests' com.nicole.ocean.ReviewTest
Check-Exit 'Review regressions'
}
$classes = @(Get-ChildItem -LiteralPath 'build\classes' -Recurse -Filter '*.class' | ForEach-Object { $_.FullName })
& "$JavaHome\bin\java.exe" -cp "$tools\lib\d8.jar" com.android.tools.r8.D8 --lib $androidJar --min-api 26 --output 'build\dex' $classes
Check-Exit 'DEX compilation'
& "$tools\aapt2.exe" compile --dir 'app\res' -o 'build\resources.zip'
Check-Exit 'Resource compilation'
& "$tools\aapt2.exe" link -o 'build\unsigned.apk' -I $androidJar --manifest 'app\AndroidManifest.xml' 'build\resources.zip'
Check-Exit 'Resource linking'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::Open((Join-Path $root 'build\unsigned.apk'),[System.IO.Compression.ZipArchiveMode]::Update)
try {
    [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,(Join-Path $root 'build\dex\classes.dex'),'classes.dex') | Out-Null
    # Explicit portable ZIP names: the Windows aapt2 asset walker can retain backslashes.
    $assetRoot=Join-Path $root 'app\assets'
    foreach($asset in Get-ChildItem -LiteralPath $assetRoot -Recurse -File) {
        $entry='assets/'+[IO.Path]::GetRelativePath($assetRoot,$asset.FullName).Replace('\','/')
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,$asset.FullName,$entry,[IO.Compression.CompressionLevel]::NoCompression) | Out-Null
    }
} finally { $zip.Dispose() }
& "$tools\zipalign.exe" -f -P 16 4 'build\unsigned.apk' 'build\aligned.apk'
Check-Exit 'APK alignment'
$key = Join-Path $root '.signing\nicole-release.p12'
$secret = Join-Path $root '.signing\password.txt'
if (!(Test-Path -LiteralPath $key)) {
    $bytes = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($bytes); $rng.Dispose()
    $password = [Convert]::ToBase64String($bytes)
    [IO.File]::WriteAllText($secret,$password,[Text.UTF8Encoding]::new($false))
    & "$JavaHome\bin\keytool.exe" -genkeypair -keystore $key -storetype PKCS12 -storepass:file $secret -keypass:file $secret -alias nicole -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Nicole Ocean, OU=Personal Game, O=Nicole Ocean, C=CA'
    Check-Exit 'Signing key generation'
}
if (!(Test-Path -LiteralPath $secret)) { throw 'Signing password file is missing. Restore it before building an update.' }
& "$JavaHome\bin\java.exe" -jar "$tools\lib\apksigner.jar" sign --ks $key --ks-pass "file:$secret" --ks-key-alias nicole --v4-signing-enabled false --out $apk 'build\aligned.apk'
Check-Exit 'APK signing'
& "$JavaHome\bin\java.exe" -jar "$tools\lib\apksigner.jar" verify --verbose $apk
Check-Exit 'Signature verification'
& "$tools\aapt.exe" dump badging $apk | Select-Object -First 8
Check-Exit 'Package inspection'
Get-FileHash -LiteralPath $apk -Algorithm SHA256 | Format-List




