package com.nicole.ocean;

import java.util.Arrays;

/** Surprise timing, event variety, collision geometry and resumable event director. */
public final class EventTest {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static Game empty(int level){Game g=new Game();g.stage=g.highestStage=(level-1)/5;g.start();g.level=level;g.x=1100;g.y=1500;g.time=60;g.fish.clear();g.items.clear();g.bossi.wait=10000;return g;}
    public static void main(String[] args)throws Exception{
        float lastWarning=10,lastInterval=100;
        for(int level=1;level<=45;level++){
            check(Difficulty.bossWarning(level)<=lastWarning&&Difficulty.bossWarning(level)>=.69f,"Boss reaction interval decreases but remains dodgeable");
            check(Difficulty.eventInterval(level)<=lastInterval&&Difficulty.eventInterval(level)>=12,"Event cooldown decreases within bounds");
            lastWarning=Difficulty.bossWarning(level);lastInterval=Difficulty.eventInterval(level);
            Game g=empty(level);g.bossi.begin(g,Bossi.LEFT_PAW,420,850);
            check(g.bossi.stage==Bossi.ARRIVE&&g.bossi.audioSerial==1,"No lure or advance announcement before cat");
            float elapsed=0;
            while(g.bossi.stage!=Bossi.STRIKE){g.bossi.update(g,.01f,420,850);g.bossi.collide(g);elapsed+=.01f;check(g.hp==g.maxHp(),"Arrival and aim are harmless");}
            check(elapsed>=.32f+Difficulty.bossWarning(level)-.01f&&elapsed<.32f+Difficulty.bossWarning(level)+.04f,"First strike respects arrival plus reaction delay");
        }
        for(int type=1;type<=Hazard.TYPE_COUNT;type++)for(int level:new int[]{1,23,45})for(float height:new float[]{480,850,1100}){
            Game g=empty(level);g.hazard.begin(g,type,420,height);
            float x=g.hazard.originX,y=g.hazard.originY;
            g.hazard.update(g,.5f,420,height);check(g.hp==g.maxHp(),"Event warning is harmless "+type);
            Game copy=Game.restore(g.snapshot());
            for(int step=0;step<400;step++){
                g.hazard.update(g,.05f,420,height);copy.hazard.update(copy,.05f,420,height);
                check(Arrays.equals(g.snapshot(),copy.snapshot()),"Exact event continuation type "+type);
                check(Float.isFinite(g.x)&&Float.isFinite(g.y)&&g.x>=0&&g.x<=Game.WORLD_W&&g.y>=0&&g.y<=Game.WORLD_H,"Currents stay bounded");
                check(g.hazard.originX==x&&g.hazard.originY==y,"Event route never follows player");
                if(!g.hazard.visible())break;
            }
            check(!g.hazard.visible(),"Every event ends "+type);
            check(g.hp>=g.maxHp()-Difficulty.monsterDamage(level)-.01f,"At most one damage per event");
        }
        for(int type=1;type<=Hazard.TYPE_COUNT;type++){
            Game g=empty(1);Hazard h=g.hazard;h.begin(g,type,420,850);h.stage=2;h.timer=type==Hazard.GEYSER?1.2f:type==Hazard.MINE?4.8f:h.duration()*.5f;
            h.update(g,0,420,850);g.hp=g.maxHp();g.invincible=0;h.hit=false;
            g.x=type==Hazard.JELLY||type==Hazard.MIGRATION?h.elementX(1):h.x;
            g.y=type==Hazard.JELLY||type==Hazard.MIGRATION?h.elementY(1):h.y;
            if(type==Hazard.MINE)g.x+=h.blastRadius();
            h.update(g,0,420,850);check(g.hp<g.maxHp(),"Visible dangerous core damages for type "+type);
            float health=g.hp;g.invincible=0;h.update(g,0,420,850);check(g.hp==health,"No repeated damage type "+type);
            g.x=1100;g.y=380;g.hp=g.maxHp();g.invincible=0;h.hit=false;
            h.originX=h.x=1100;h.originY=h.y=380;h.update(g,.01f,420,850);
            check(g.hp==g.maxHp(),"Harbor protection for type "+type);
        }
        Game bag=empty(45);int previous=0;
        for(int round=0;round<3;round++){
            int bits=0;
            for(int n=0;n<Hazard.TYPE_COUNT;n++){
                bag.hazard.kind=bag.hazard.stage=0;bag.hazard.wait=0;bag.hazard.update(bag,.01f,420,850);
                int type=bag.hazard.kind;check(type!=previous,"No adjacent repeats across bag boundaries");previous=type;
                check((bits&(1<<(type-1)))==0,"Bag contains no duplicate event");bits|=1<<(type-1);
                bag=Game.restore(bag.snapshot());
            }
            check(bits==Hazard.ALL_TYPES,"Every shuffled bag includes all eleven types");
        }
        int[] counts=new int[2];
        for(int difficulty=0;difficulty<2;difficulty++){
            Game g=empty(difficulty==0?1:225);g.invincible=10000;boolean active=false;
            for(int step=0;step<12000;step++){
                g.hazard.update(g,.05f,420,850);
                if(g.hazard.visible()&&!active)counts[difficulty]++;
                active=g.hazard.visible();
            }
        }
        check(counts[1]>counts[0]*1.4f,"Late-game events are substantially more frequent: "+counts[0]+" / "+counts[1]);
        Game old=empty(10);old.hazard.remainingTypes=5;byte[] bytes=LegacyFixtures.v9(old);bytes[7]=8;
        java.util.zip.CRC32 crc=new java.util.zip.CRC32();crc.update(bytes,0,bytes.length-8);
        java.nio.ByteBuffer.wrap(bytes,bytes.length-8,8).putLong(crc.getValue());
        Game upgraded=Game.restore(bytes);
        check((upgraded.hazard.remainingTypes&15)==5&&(upgraded.hazard.remainingTypes&~15)==(Hazard.ALL_TYPES&~15),"Old shuffled bag gains new events without losing pending originals");
        System.out.println("PASS: "+checks+" surprise and event assertions; 11 types, 99 saved simulations, 3 shuffled bags, cadence "+counts[0]+" -> "+counts[1]+" per 10 minutes");
    }
}
