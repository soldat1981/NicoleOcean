"""Ten original, deterministic ambient miniatures; no recordings or borrowed melodies.
Run with Python + NumPy. Outputs standard PCM WAV playable offline by Android.
"""
from pathlib import Path
import json
import wave
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'app/assets/audio/music'
OUT.mkdir(parents=True, exist_ok=True)
SR = 22050
THEMES = [
    ('Тихая лагуна', 60, 48, [0,5,3,4], [0,2,4,6,4,2,1,4]),
    ('Жемчужный свет', 57, 50, [0,3,5,4], [4,6,8,6,4,3,1,2]),
    ('Сон кораллов', 54, 53, [0,4,5,3], [2,4,6,8,6,4,2,0]),
    ('Голубые качели', 62, 55, [0,5,1,4], [0,4,2,6,4,8,6,4]),
    ('Лунная вода', 56, 45, [5,3,0,4], [6,4,2,1,4,2,0,2]),
    ('Дельфиний сон', 64, 48, [3,0,5,4], [4,2,0,2,6,4,2,4]),
    ('Сад медуз', 52, 52, [0,1,5,4], [2,6,4,2,0,4,6,8]),
    ('Письмо океана', 59, 46, [0,3,1,4], [8,6,4,2,4,6,4,0]),
    ('Тёплое течение', 61, 51, [5,4,0,3], [0,2,6,4,2,4,8,6]),
    ('Звёзды под водой', 55, 49, [0,5,4,0], [4,8,6,4,2,0,2,4]),
]
SCALE = [0,2,4,5,7,9,11]
def note(degree, root):
    return root + SCALE[degree % 7] + 12*(degree//7)

manifest = []
for index, (name, bpm, root, progression, melody) in enumerate(THEMES):
    rng = np.random.default_rng(8171+index)
    beat = 60/bpm
    length = 32*beat*2 + 5
    mix = np.zeros(int(length*SR), dtype=np.float64)
    def add(start, midi, duration, amplitude, timbre):
        count = int(duration*SR)
        t = np.arange(count)/SR
        freq = 440*2**((midi-69)/12)
        if timbre == 'pad':
            signal = (np.sin(2*np.pi*freq*t)+.28*np.sin(2*np.pi*freq*1.0018*t)
                      +.12*np.sin(4*np.pi*freq*t))
            env = np.minimum(t/1.3,1)*np.minimum((duration-t)/1.8,1)
        else:
            # Rounded plucked keys, muted overtones; no sharp click or percussion.
            signal = np.sin(2*np.pi*freq*t)+.19*np.sin(4*np.pi*freq*t)*np.exp(-2*t)
            signal += (.07 if timbre=='harp' else .035)*np.sin(6*np.pi*freq*t)*np.exp(-3*t)
            env = (1-np.exp(-t/0.024))*np.exp(-t/(1.9 if timbre=='harp' else 2.8))
            env *= np.minimum((duration-t)/.22,1)
        signal *= env*amplitude
        offset = int(start*SR)
        n = min(count, len(mix)-offset)
        mix[offset:offset+n] += signal[:n]
    for bar in range(16):
        degree = progression[bar % 4]
        start = bar*4*beat
        for step in [0,2,4,6]:
            add(start, note(degree+step,root), 4*beat+2, .045 if step!=6 else .025, 'pad')
        add(start, note(degree,root)-12, 5*beat, .065, 'key')
        for step in range(4):
            arp = [0,4,2,6][(step+index)%4]
            add(start+(step+.12)*beat, note(degree+arp,root)+12,
                3.5*beat, .055, 'harp' if index%2 else 'key')
        # Each theme has its own eight-note phrase and a quiet answering phrase.
        if bar%2==0:
            phrase = melody[(bar//2)%8]
            add(start+.5*beat, note(phrase,root)+24, 4*beat, .075, 'key')
            add(start+2.5*beat, note(melody[(bar//2+1)%8],root)+24, 4*beat, .055, 'key')
        elif bar%4==3:
            add(start+2*beat, note(degree+4,root)+12, 4*beat, .06, 'harp')
    dry = mix.copy()
    # Gentle reverberant reflections rather than a rhythmic echo.
    for delay, gain in [(.163,.14),(.293,.10),(.431,.07),(.719,.045)]:
        d = int(delay*SR)
        mix[d:] += dry[:-d]*gain
    fade = int(3*SR)
    mix[:fade] *= np.linspace(0,1,fade)
    mix[-fade:] *= np.linspace(1,0,fade)
    mix *= .69/max(np.max(np.abs(mix)), 1e-9)
    assert np.isfinite(mix).all() and np.max(np.abs(mix)) < .7
    rms = float(np.sqrt(np.mean(mix**2)))
    assert .08 < rms < .3, (name,rms)
    pcm = (mix*32767).astype('<i2')
    filename = f'ocean_{index+1:02}.wav'
    with wave.open(str(OUT/filename),'wb') as out:
        out.setnchannels(1); out.setsampwidth(2); out.setframerate(SR); out.writeframes(pcm.tobytes())
    manifest.append(dict(file=filename,title=name,bpm=bpm,seconds=round(length,2),rms=round(rms,4),peak=.69))
    print(filename,round(length,1),'seconds')
(ROOT/'docs/music-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
