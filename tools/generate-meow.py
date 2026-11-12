"""Original cat calls: smooth glottal harmonics, moving vowels and breath. No samples."""
import math
import random
import struct
import wave
from pathlib import Path

RATE = 44100
ROOT = Path(__file__).resolve().parents[1] / 'app/assets/audio'

def curve(u, points):
    for (a, va), (b, vb) in zip(points, points[1:]):
        if u <= b:
            t = max(0, min(1, (u-a)/(b-a)))
            t = t*t*(3-2*t)
            return va+(vb-va)*t
    return points[-1][1]

def meow(variant):
    rng = random.Random(1900+variant)
    duration = (1.12, 1.32, .98)[variant]
    phase = 0.0
    breath = jitter = 0.0
    values = []
    pitch_scale = (1.0, .90, 1.12)[variant]
    for n in range(int(RATE*duration)):
        t = n/RATE
        u = t/duration
        jitter = .997*jitter + .003*rng.uniform(-1, 1)
        pitch = curve(u, [(0,440),(.12,610),(.35,690),(.63,560),(1,330)])*pitch_scale
        pitch *= 1+.004*math.sin(2*math.pi*5.1*t)+.025*jitter
        phase += 2*math.pi*pitch/RATE
        opening = curve(u, [(0,0),(.16,.35),(.32,1),(.65,.85),(1,.08)])
        f1 = 400+730*opening
        f2 = curve(u, [(0,2400),(.25,2050),(.55,1600),(1,850)])
        voiced = 0.0
        for h in range(1, 25):
            freq = pitch*h
            if freq>11000: break
            # Spectral roll-off prevents the old saturated buzzy upper harmonics.
            resonance = .16 + 1.1*math.exp(-.5*((freq-f1)/260)**2) + .55*math.exp(-.5*((freq-f2)/420)**2)
            voiced += resonance*math.sin(phase*h+.12*h)/(h**1.65)
        breath = .88*breath+.12*rng.uniform(-1,1)
        envelope = curve(u, [(0,0),(.065,.38),(.18,.9),(.38,1),(.68,.70),(.86,.40),(1,0)])
        nasal = .08*(1-opening)*math.sin(phase*.5)
        values.append((voiced+nasal+breath*.045)*envelope)
    # Tiny diffuse reflection, no audible repeating echo or abrupt ending.
    dry=values[:]
    for delay, gain in ((.019,.035),(.037,.02)):
        offset=int(delay*RATE)
        for i in range(offset,len(values)):
            values[i]+=dry[i-offset]*gain
    dc=sum(values)/len(values)
    values=[v-dc for v in values]
    peak=max(abs(v) for v in values)
    values=[v*.64/peak for v in values]
    ramp=int(RATE*.012)
    for i in range(ramp):
        values[i]*=i/ramp
        values[-i-1]*=i/ramp
    name='bossi_meow.wav' if variant==0 else f'bossi_meow{variant+1}.wav'
    with wave.open(str(ROOT/name),'wb') as out:
        out.setnchannels(1);out.setsampwidth(2);out.setframerate(RATE)
        out.writeframes(struct.pack('<'+'h'*len(values),*(round(v*32767) for v in values)))
    print(f'{name}: {duration:.2f}s, peak 0.64, smooth endpoints')

if __name__ == '__main__':
    for variant in range(3):
        meow(variant)
