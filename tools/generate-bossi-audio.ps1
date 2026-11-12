$ErrorActionPreference='Stop'
Set-Location -LiteralPath (Split-Path -Parent $PSScriptRoot)
New-Item -ItemType Directory -Force -Path 'app\assets\audio' | Out-Null
# Use an already installed Russian Windows voice; no account or online TTS required.
$voice=New-Object -ComObject SAPI.SpVoice
$token=New-Object -ComObject SAPI.SpObjectToken
$token.SetId('HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Speech_OneCore\Voices\Tokens\MSTTS_V110_ruRU_IrinaM')
$voice.Voice=$token
$voice.Rate=0
foreach($clip in @(
    @{Name='bossi_no';Text='Босси, нельзя!'},
    @{Name='bossi_away';Text='Босс, отойди от аквариума.'},
    @{Name='bossi_food';Text='Босс, пойдём кушать!'}
)) {
    $stream=New-Object -ComObject SAPI.SpFileStream
    try {
        $stream.Open((Join-Path $PWD "app\assets\audio\$($clip.Name).wav"),3,$false)
        $voice.AudioOutputStream=$stream
        $voice.Speak($clip.Text) | Out-Null
    } finally { $stream.Close() }
}
Write-Output 'Three offline Russian voice clips generated.'
