$ErrorActionPreference='Stop'
Set-Location -LiteralPath (Split-Path -Parent $PSScriptRoot)
$voice=New-Object -ComObject SAPI.SpVoice
$token=New-Object -ComObject SAPI.SpObjectToken
$token.SetId('HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Speech_OneCore\Voices\Tokens\MSTTS_V110_ruRU_IrinaM')
$voice.Voice=$token
$voice.Rate=1
foreach($clip in @(
    @{Name='nicole_fish';Text='<pitch middle="2"><emph>Смотри</emph><silence msec="100"/> рыбки!</pitch>'},
    @{Name='nicole_swim';Text='<pitch middle="1">А я тут <emph>плаваю</emph>!</pitch>'},
    @{Name='nicole_dive';Text='<pitch middle="2">Смотри<silence msec="100"/> как я <emph>ныряю</emph>!</pitch>'},
    @{Name='nicole_nyam';Text='<pitch middle="2"><rate speed="-1">Ням<silence msec="110"/>ням<silence msec="110"/>ням<silence msec="110"/>ням!</rate></pitch>'},
    @{Name='nicole_playground';Text='<pitch middle="1"><emph>Хочу</emph><silence msec="80"/> на плэйграунд!</pitch>'}
)) {
    $stream=New-Object -ComObject SAPI.SpFileStream
    try {
        $stream.Format.Type=22
        $stream.Open((Join-Path $PWD "app/assets/audio/$($clip.Name).wav"),3,$false)
        $voice.AudioOutputStream=$stream
        $voice.Speak($clip.Text,8) | Out-Null
    } finally { $stream.Close() }
}
Write-Output 'Five new offline Nicole visitor phrases generated with prosody.'
