package com.nicole.ocean;

import java.util.Arrays;

/** Regression cases found during the 1.7 audit, plus reproducible long-running property checks. */
public final class ReviewTest {
    private static int checks;
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    private static Game empty(){Game g=new Game();g.start();g.fish.clear();g.items.clear();g.x=1000;g.y=1500;g.bossi.wait=g.hazard.wait=1000;return g;}
    public static void main(String[] args)throws Exception{
        for(float dt:new float[]{0,-1,Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY}){
            Game g=empty();g.items.add(new Game.Item(g.x,g.y,0));byte[] before=g.snapshot();
            g.update(dt,1,1,420,850);check(Arrays.equals(before,g.snapshot()),"Invalid/zero time must be a true no-op");
        }
        for(float value:new float[]{-10,Float.NaN,Float.POSITIVE_INFINITY}){
            Game g=empty();byte[] before=g.snapshot();g.addXp(value);g.hurt(value);
            check(Arrays.equals(before,g.snapshot()),"Invalid rewards/damage cannot poison state");
        }
        Game invalid=empty();invalid.update(.01f,Float.NaN,Float.POSITIVE_INFINITY,Float.NaN,Float.NaN);
        check(Float.isFinite(invalid.x)&&Float.isFinite(invalid.y),"Nonfinite controls sanitized");Game.restore(invalid.snapshot());
        for(int mutation=0;mutation<8;mutation++){
            Game bad=empty();
            switch(mutation){
                case 0:bad.seed=0;break;case 1:bad.sequence=-1;break;case 2:bad.invincible=-1;break;
                case 3:bad.cooldown=-1;break;case 4:bad.dash=-1;break;case 5:bad.hp=110;break;
                case 6:bad.fish.add(new Game.Fish(-10,500,1,0));break;
                default:bad.items.add(new Game.Item(500,Game.WORLD_H+1,0));
            }
            try{Game.restore(bad.snapshot());throw new AssertionError("Semantically invalid save accepted: "+mutation);}catch(java.io.IOException expected){checks++;}
        }
        try{Game.restore(null);throw new AssertionError("Null save accepted");}catch(java.io.IOException expected){checks++;}
        Game winner=empty();winner.level=3;winner.eaten=8;winner.pearls=2;winner.shells=1;
        winner.addXp(Game.threshold(3));check(winner.bank==25&&winner.mission==4,"All four earned missions deposited on victory");
        winner.finish();winner.addXp(100);check(winner.bank==25,"Finishing repeatedly cannot duplicate mission rewards");
        for(int hz:new int[]{24,30,60,90,120,144,240}){
            FrameStepper clock=new FrameStepper();int ticks=0;
            for(int i=0;i<hz*30;i++)ticks+=clock.advance(1.0/hz);
            check(ticks==1800,"30 seconds has 1800 simulation steps at "+hz+" Hz, got "+ticks);
            clock.reset();check(clock.advance(0)==0&&clock.advance(Double.NaN)==0,"Clock rejects invalid time");
            check(clock.advance(60)==15,"Long stalls have bounded recovery");clock.reset();check(clock.advance(1.0/120)==0,"Pause clears fractional time");
        }
        byte[] baseline=null;
        for(int hz:new int[]{30,60,90,120,144}){
            Game g=empty();FrameStepper clock=new FrameStepper();
            for(int frame=0;frame<hz*10;frame++)for(int tick=clock.advance(1.0/hz);tick>0;tick--)g.update(FrameStepper.STEP,.3f,.2f,420,850);
            byte[] snapshot=g.snapshot();if(baseline==null)baseline=snapshot;else check(Arrays.equals(baseline,snapshot),"Display refresh cannot change the world");
        }
        // End-point contact misses this diagonal dash crossing; swept pickup must collect it.
        Game g=empty();g.vx=4000;g.vy=0;g.items.add(new Game.Item(g.x+55,g.y,2));
        g.update(.05f,1,0,420,850);check(g.shells==2,"A fast crossing collects a shell between endpoints");
        for(int stage=0;stage<60;stage++)for(int seed=1;seed<=4;seed++)for(int height:new int[]{480,850,1100}){
            g=new Game();g.seed=seed*971L;g.stage=g.highestStage=stage;g.calm=(seed&1)==0;g.start();
            Game copy=Game.restore(g.snapshot());
            for(int frame=0;frame<3600;frame++){
                float ix=(float)Math.sin(frame*.019+seed),iy=(float)Math.cos(frame*.013+stage);
                if(frame%180==0){g.useDash();copy.useDash();}
                g.update(FrameStepper.STEP,ix,iy,420,height);copy.update(FrameStepper.STEP,ix,iy,420,height);
                check(Float.isFinite(g.x)&&Float.isFinite(g.hp)&&g.hp>=0&&g.hp<=g.maxHp(),"Finite bounded hero");
                check(g.fish.size()<=Game.MAX_FISH&&g.items.size()<=Game.MAX_ITEMS&&g.particles.size()<=240,"Bounded collections");
                check(!(g.hazard.visible()&&(g.bossi.visible()||g.nicole.visible))&&!(g.bossi.visible()&&g.nicole.visible),"Exclusive visitors and hazards");
                if(frame%120==0){check(Arrays.equals(g.snapshot(),copy.snapshot()),"Deterministic continuation across periodic restore");copy=Game.restore(copy.snapshot());}
                if(!g.active){g.start();copy.start();}
            }
        }
        // Every byte of a valid slot is protected, including its checksum and version.
        byte[] good=empty().snapshot();
        for(int i=0;i<good.length;i++){
            byte[] broken=good.clone();broken[i]^=1;
            try{Game.restore(broken);throw new AssertionError("Corruption accepted at "+i);}catch(java.io.IOException expected){checks++;}
        }
        System.out.println("PASS: "+checks+" audit assertions; 720 seeded one-minute runs, refresh-rate equivalence, save corruption, invalid inputs and swept pickups.");
    }
}
