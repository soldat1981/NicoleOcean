package com.nicole.ocean;

import java.io.*;

/** Passive background only. This shares a scheduler with Bossi and never changes combat. */
public final class Nicole {
    public static final int SAVE_BYTES=18;
    public boolean visible,nextNicole;
    public int face,gear;
    public float time,duration=13;
    public float x,y,width=630;public int audioSerial,audioKind;
    public void reset(Game g){visible=false;time=0;nextNicole=g.random()<.5f;}
    public void begin(Game g){begin(g,420,900);}
    public void begin(Game g,float vw,float vh){
        if(g.bossi.visible()||g.hazard.visible())return;
        visible=true;time=0;duration=g.between(22,30);face=(int)(g.random()*3);gear=(int)(g.random()*3);
        float cx=Game.clamp(g.x-vw*.5f,0,Game.WORLD_W-vw),cy=Game.clamp(g.y-vh*.46f,80,Game.WORLD_H-vh+30);
        x=cx+vw*.5f;y=cy+vh*.5f+40;width=Math.max(180,Math.min(vw*.86f,(vh-240)*.85f));audioKind=1+(int)(g.random()*6);audioSerial++;
    }
    public float opacity(){return visible?Game.clamp(Math.min(time/1.3f,(duration-time)/1.3f),0,1):0;}
    public void update(float dt){time+=dt;if(time>=duration)visible=false;}
    public void write(DataOutput out)throws IOException{
        out.writeBoolean(visible);out.writeBoolean(nextNicole);out.writeInt(face);out.writeInt(gear);out.writeFloat(time);out.writeFloat(duration);
    }
    public void read(DataInput in)throws IOException{
        visible=in.readBoolean();nextNicole=in.readBoolean();face=in.readInt();gear=in.readInt();time=in.readFloat();duration=in.readFloat();
        if(face<0||face>2||gear<0||gear>2||!Float.isFinite(time)||!Float.isFinite(duration)||time<0||time>60||duration<10||duration>32)throw new IOException("Invalid Nicole visit");
    }
}
