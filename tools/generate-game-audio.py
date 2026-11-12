"""Original offline game Foley; no downloaded or copyrighted samples."""
from pathlib import Path
import math, random, struct, wave
root=Path(__file__).resolve().parents[1]/'app/assets/audio'
root.mkdir(parents=True,exist_ok=True)
rate=44100
for name,duration in [('eat0',.32),('eat1',.36),('eat2',.30),('sonar',.85),('growth',.85),('hurt',.30),('dash',.32),('menu',.16)]:
    rng=random.Random(name);out=[];smooth=0
    for n in range(int(duration*rate)):
        t=n/rate;v=0
        if name.startswith('eat'):
            k=int(name[-1]);
            for start,freq,amp in [(0,170+k*27,.40),(.06,430+k*35,.30),(.145,720+k*46,.18)]:
                q=t-start
                if q>=0:
                    env=(1-math.exp(-q*650))*math.exp(-q*32)
                    v+=math.sin(2*math.pi*(freq*q+360*q*q))*env*amp
            smooth=smooth*.65+rng.uniform(-1,1)*.35
            v+=smooth*.24*math.exp(-t*25)*(1-math.exp(-t*900))
        elif name=='sonar':
            for start in [0,.36]:
                q=t-start
                if q>=0:v+=math.sin(2*math.pi*660*q)*.24*math.exp(-q*9)*(1-math.exp(-q*100))
        elif name=='growth':
            for j,f in enumerate([523.25,659.25,783.99,1046.5]):
                q=t-j*.105
                if q>=0:v+=(math.sin(2*math.pi*f*q)+.25*math.sin(2*math.pi*f*2*q))*.16*math.exp(-q*6)*(1-math.exp(-q*180))
        elif name=='menu':
            v=.3*(1-math.exp(-t*180))*math.exp(-t*34)*(math.sin(2*math.pi*740*t)+.2*math.sin(2*math.pi*1110*t))
        elif name=='hurt':
            v=math.sin(2*math.pi*(180*t-160*t*t))*.35*math.exp(-t*14)*(1-math.exp(-t*200))
        else:
            smooth=smooth*.83+rng.uniform(-1,1)*.17
            v=smooth*.8*math.sin(math.pi*t/duration)**2+math.sin(2*math.pi*(140*t+800*t*t))*.08*math.exp(-t*10)
        v*=min(1,(duration-t)/.012) # Click-free release on every short cue.
        out.append(struct.pack('<h',int(max(-.94,min(.94,v))*32767)))
    with wave.open(str(root/(name+'.wav')),'wb') as wav:
        wav.setnchannels(1);wav.setsampwidth(2);wav.setframerate(rate);wav.writeframes(b''.join(out))
print('Generated eight original PCM sound effects')
