package com.nicole.ocean;

import java.io.*;
import java.util.ArrayList;

/** Needle combat. Damage is a saved fraction of health, based on levels at impact. */
public final class Combat {
    public final ArrayList<Expedition.Shot> hostile=new ArrayList<>();
    public float guardianDamage;
    public static int hits(int shooter,int target){return Math.max(2,3+2*(target-shooter));}
    public static float damage(int shooter,int target){return 1f/hits(shooter,target);}
    public void reset(){hostile.clear();guardianDamage=0;}
    public static boolean shooter(Game g,Game.Fish f){return g.stage>=3&&f.level>=4&&f.kind==1&&f.level!=g.level;}
    public static float warning(Game g){return (1.5f-.4f*Difficulty.progress(g.stage))*(g.calm?1.25f:1);}

    // First circle along a segment, not whichever fish happens to be first in the list.
    static float intersection(float ax,float ay,float bx,float by,float cx,float cy,float radius){
        float dx=bx-ax,dy=by-ay,ox=ax-cx,oy=ay-cy,a=dx*dx+dy*dy,c=ox*ox+oy*oy-radius*radius;
        if(c<=0)return 0;if(a<.000001f)return Float.POSITIVE_INFINITY;
        float b=ox*dx+oy*dy,disc=b*b-a*c;if(disc<0)return Float.POSITIVE_INFINITY;
        float t=(-b-(float)Math.sqrt(disc))/a;return t>=0&&t<=1?t:Float.POSITIVE_INFINITY;
    }
    public void friendly(Game g,Expedition.Shot s,float ox,float oy){
        float first=Float.POSITIVE_INFINITY;
        Game.Fish victim=null;boolean guardian=false;
        for(Game.Fish f:g.fish){
            if(f.level==s.level)continue;
            float t=intersection(ox,oy,s.x,s.y,f.x,f.y,Game.radius(f.level)+3);
            if(t<first){first=t;victim=f;}
        }
        Expedition e=g.expedition;
        if(e.state==1){float t=intersection(ox,oy,s.x,s.y,e.x,e.y,85);if(t<first){first=t;victim=null;guardian=true;}}
        if(first==Float.POSITIVE_INFINITY)return;s.life=0;
        if(victim!=null){
            victim.needleDamage+=damage(s.level,victim.level);g.burst(victim.x,victim.y,0xffffe4a6,7);
            if(victim.needleDamage>=1-.00001f){g.fish.remove(victim);g.addXp(g.fishReward(victim));}
        }else if(guardian){
            e.stun=.35f;guardianDamage+=damage(s.level,e.targetLevel);g.burst(e.x,e.y,0xffffe4a6,8);
            // The four-level growth objective still applies in guardian oceans.
            if(g.level<=e.targetLevel)guardianDamage=Math.min(.9f,guardianDamage);
            else if(guardianDamage>=1-.00001f)e.defeat(g);
        }
    }
    public void update(Game g,float dt,float vw,float vh,float oldX,float oldY){
        boolean blocked=g.guardianOcean()||g.bossi.visible()||g.bossi.pending()||g.hazard.visible()||g.nicole.visible||g.time<12||g.hiddenTime>=1;
        float cameraX=Game.clamp(g.x-vw*.5f,0,Game.WORLD_W-vw),cameraY=Game.clamp(g.y-vh*.46f,80,Game.WORLD_H-vh+30);
        int busy=hostile.size();for(Game.Fish f:g.fish)if(f.shootWarning>0)busy++;
        int limit=1+(g.stage>=30?1:0);
        for(Game.Fish f:g.fish){
            boolean can=shooter(g,f)&&!blocked&&f.x>cameraX+45&&f.x<cameraX+vw-45&&f.y>cameraY+240&&f.y<cameraY+vh-140&&Game.dist(f.x,f.y,g.x,g.y)>90;
            if(!can){if(f.shootWarning>0){f.shootWarning=0;f.shootWait=4;}continue;}
            if(f.shootWarning>0){
                f.shootWarning=Math.max(0,f.shootWarning-dt);
                if(f.shootWarning==0){
                    Expedition.Shot s=new Expedition.Shot(f.x,f.y,f.shootX*220,f.shootY*220);s.level=f.level;hostile.add(s);
                    f.shootWait=g.between(12,17)-4*Difficulty.progress(g.stage);
                }
            }else{
                f.shootWait=Math.max(0,f.shootWait-dt);
                if(f.shootWait==0&&busy<limit&&f.dashTime==0&&f.warnTime==0){
                    float d=Math.max(1,Game.dist(f.x,f.y,g.x,g.y));f.shootX=(g.x-f.x)/d;f.shootY=(g.y-f.y)/d;
                    f.shootWarning=warning(g);busy++;
                }
            }
        }
        for(int n=hostile.size()-1;n>=0;n--){
            Expedition.Shot s=hostile.get(n);float ox=s.x,oy=s.y;s.x+=s.vx*dt;s.y+=s.vy*dt;s.life-=dt;
            float harbor=Float.POSITIVE_INFINITY;
            float contact=intersection(ox-oldX,oy-oldY,s.x-g.x,s.y-g.y,0,0,Game.radius(g.level)+3);
            if(harbor!=Float.POSITIVE_INFINITY)s.life=0;
            if(contact<harbor&&g.hiddenTime<1&&s.level!=g.level){g.hurt(100*damage(s.level,g.level)*(g.calm?.5f:1));s.life=0;}
            if(s.life<=0||s.x<0||s.x>Game.WORLD_W||s.y<0||s.y>Game.WORLD_H)hostile.remove(n);
            if(!g.active)return;
        }
    }
    public int bytes(Game g){return 8+g.fish.size()*20+g.expedition.shots.size()*4+hostile.size()*24;}
    public void write(DataOutputStream o,Game g)throws IOException{
        o.writeFloat(guardianDamage);
        for(Game.Fish f:g.fish)for(float v:new float[]{f.needleDamage,f.shootWait,f.shootWarning,f.shootX,f.shootY})o.writeFloat(v);
        for(Expedition.Shot s:g.expedition.shots)o.writeInt(s.level);
        o.writeInt(hostile.size());for(Expedition.Shot s:hostile){for(float v:new float[]{s.x,s.y,s.vx,s.vy,s.life})o.writeFloat(v);o.writeInt(s.level);}
    }
    private static float bounded(DataInputStream i,float min,float max)throws IOException{float v=Game.Codec.number(i);if(v<min||v>max)throw new IOException("Invalid combat state");return v;}
    public void read(DataInputStream i,Game g)throws IOException{
        guardianDamage=bounded(i,0,2);
        for(Game.Fish f:g.fish){f.needleDamage=bounded(i,0,1);f.shootWait=bounded(i,0,20);f.shootWarning=bounded(i,0,2);f.shootX=bounded(i,-1,1);f.shootY=bounded(i,-1,1);}
        for(Expedition.Shot s:g.expedition.shots)s.level=Game.Codec.range(i.readInt(),1,56);
        int count=Game.Codec.range(i.readInt(),0,2);
        for(int n=0;n<count;n++){Expedition.Shot s=new Expedition.Shot(bounded(i,0,Game.WORLD_W),bounded(i,0,Game.WORLD_H),bounded(i,-220,220),bounded(i,-220,220));s.life=bounded(i,0,1.2f);s.level=Game.Codec.range(i.readInt(),1,56);hostile.add(s);}
    }
}
