param([string]$JavaHome = 'C:\Program Files\Java\jdk-20', [switch]$SkipPackage, [switch]$SkipModelTests)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
function Check-Exit([string]$step) { if ($LASTEXITCODE -ne 0) { throw "$step failed: $LASTEXITCODE" } }
if (!(Test-Path -LiteralPath "$JavaHome\bin\jpackage.exe")) { throw 'A Windows x64 JDK with jpackage is required.' }
# Only generated outputs inside this workspace may be replaced.
foreach ($relative in @('build\pc','dist\pc\NicoleOceanPC')) {
    $target = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot $relative))
    if (!$target.StartsWith($PSScriptRoot + [IO.Path]::DirectorySeparatorChar)) { throw 'Unsafe output path' }
    if (Test-Path -LiteralPath $target) { Remove-Item -LiteralPath $target -Recurse -Force }
}
New-Item -ItemType Directory -Force -Path build/pc/classes,build/pc/input,dist/pc,dist/qa-pc | Out-Null
python pc/tools/prepare.py
Check-Exit 'Preparing shared sources'
$sources = @(Get-ChildItem pc/src,build/pc/generated -Recurse -Filter *.java | ForEach-Object FullName)
& "$JavaHome\bin\javac.exe" -encoding UTF-8 -d build/pc/classes $sources
Check-Exit 'Desktop compilation'
Copy-Item -LiteralPath app/res/drawable-nodpi -Destination build/pc/classes/drawable -Recurse
Copy-Item -LiteralPath app/assets/audio -Destination build/pc/classes/audio -Recurse
& "$JavaHome\bin\java.exe" '-Djava.awt.headless=true' -cp build/pc/classes com.nicole.ocean.DesktopLauncher --self-test dist/qa-pc
Check-Exit 'Desktop tests'
if (!$SkipModelTests) {
$tests = @('tests/OceanRulesTest.java','tests/CombatTest.java','tests/LethalHazardTest.java','tests/HomeReefTest.java')
& "$JavaHome\bin\javac.exe" -encoding UTF-8 -cp build/pc/classes -d build/pc/tests $tests
Check-Exit 'Shared test compilation'
foreach($test in @('OceanRulesTest','CombatTest','LethalHazardTest','HomeReefTest')) {
    & "$JavaHome\bin\java.exe" -ea -cp 'build/pc/classes;build/pc/tests' "com.nicole.ocean.$test"
    Check-Exit $test
}
}
& "$JavaHome\bin\jar.exe" --create --file build/pc/input/NicoleOcean.jar --main-class com.nicole.ocean.DesktopLauncher -C build/pc/classes .
Check-Exit 'JAR creation'
if ($SkipPackage) { return }
& "$JavaHome\bin\jlink.exe" --add-modules java.desktop,java.logging,java.prefs --strip-debug --no-header-files --no-man-pages --compress=2 --output build/pc/runtime
Check-Exit 'Bundling Java runtime'
& "$JavaHome\bin\jpackage.exe" --type app-image --name NicoleOceanPC --input build/pc/input --main-jar NicoleOcean.jar --main-class com.nicole.ocean.DesktopLauncher --app-version 1.16.0 --vendor 'Nicole Ocean' --description 'Океан Николь для Windows' --dest dist/pc --runtime-image build/pc/runtime --java-options '-Xmx512m' --icon pc/NicoleOcean.ico
Check-Exit 'Windows packaging'
Copy-Item -LiteralPath pc/README-PC.txt -Destination dist/pc/NicoleOceanPC/README-PC.txt
& 'dist/pc/NicoleOceanPC/runtime/bin/java.exe' '-Djava.awt.headless=true' -cp dist/pc/NicoleOceanPC/app/NicoleOcean.jar com.nicole.ocean.DesktopLauncher --self-test dist/qa-pc/bundled-runtime
Check-Exit 'Bundled runtime tests'
Compress-Archive -Path dist/pc/NicoleOceanPC -DestinationPath dist/NicoleOcean-PC-Windows-x64.zip -Force
Get-FileHash -Algorithm SHA256 -LiteralPath dist/NicoleOcean-PC-Windows-x64.zip
