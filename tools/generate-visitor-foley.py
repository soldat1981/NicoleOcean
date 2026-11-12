"""Original three formant meows, liquid bloop, and small watery phrase transitions."""
import math,random,struct,wave
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'app/assets/audio'
rate=44100
def save(name,values,sr=rate):
    with wave.open(str(root/name),'wb') as f:
        f.setnchannels(1);f.setsampwidth(2);f.setframerate(sr);f.writeframes(b''.join(struct.pack('<h',int(max(-.96,min(.96,v))*32767)) for v in values))
# Keep the shared rebuild on the latest meow synthesis.
import runpy
runpy.run_path(str(Path(__file__).with_name("generate-meow.py")), run_name="__main__")

def bloop(duration=.7):
    values=[]
    for n in range(int(rate*duration)):
        t=n/rate;v=0
        for start,freq,amp in [(0,240,.42),(.10,410,.28),(.23,310,.24),(.39,650,.17)]:
            q=t-start
            if q>=0:v+=math.sin(2*math.pi*(freq*q+630*q*q))*math.exp(-q*24)*(1-math.exp(-q*700))*amp
        values.append(v)
    return values
save('nicole_bloop.wav',bloop())
# Blend a quiet bubble onset and a gentle bubble tail into each spoken phrase.
for name in ['nicole_fish','nicole_swim','nicole_dive','nicole_nyam','nicole_playground']:
    with wave.open(str(root/(name+'.wav')),'rb') as f:
        sr=f.getframerate();channels=f.getnchannels();assert f.getsampwidth()==2
        raw=struct.unpack('<'+'h'*(f.getnframes()*channels),f.readframes(f.getnframes()))
        mono=[sum(raw[j:j+channels])/channels/32768 for j in range(0,len(raw),channels)]
    # Resample to a uniform format without changing the voice's identity or pitch.
    voice=[]
    for n in range(int(len(mono)*rate/sr)):
        pos=n*sr/rate;j=int(pos);u=pos-j
        voice.append(mono[j]*(1-u)+mono[min(j+1,len(mono)-1)]*u)
    # Remove DC, tame sharp peaks and blend a quiet short room reflection.
    mean=sum(voice)/max(1,len(voice));voice=[math.tanh((v-mean)*1.1) for v in voice]
    delay=int(rate*.045);dry=voice[:]
    for n in range(delay,len(voice)):voice[n]+=.065*dry[n-delay]
    peak=max(.01,max(abs(v) for v in voice));voice=[v*.76/peak for v in voice]
    ramp=int(rate*.012)
    for n in range(min(ramp,len(voice))):voice[n]*=n/ramp;voice[-n-1]*=n/ramp
    head=bloop(.18);tail=bloop(.42);out=[v*.32 for v in head]+voice+[v*.22 for v in tail]
    save(name+'.wav',out)
print('Three meow variations, one liquid bloop and five newly finished voice clips saved.')
