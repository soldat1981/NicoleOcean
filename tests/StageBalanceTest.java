package com.nicole.ocean;
import java.util.Arrays;
/** Behavioral regressions for the nine stage boundaries, not elapsed-time scaling. */
public final class StageBalanceTest {
 static int checks;
 static void check(boolean ok,String s){checks++;if(!ok)throw new AssertionError(s);}
 static float[] profile(int l){return new float[]{Difficulty.progress(l),Difficulty.dashCooldown(l),Difficulty.bossWarning(l),Difficulty.eventWarning(l),Difficulty.eventInterval(l),Difficulty.extraDamage(l),Difficulty.heal(l),Difficulty.monsterDamage(l),Difficulty.catDamage(l),Difficulty.fishWarning(l),Difficulty.chaseRange(l),Difficulty.chaseSpeed(l),Difficulty.chaseDuration(l),Difficulty.chaseRest(l),Difficulty.chaseLimit(l),Difficulty.fleeRange(l),Difficulty.fleeSpeed(l),Difficulty.dashLimit(l),Difficulty.fishDashes(l)?1:0};}
 static Game game(int l){Game g=new Game();g.stage=g.highestStage=Difficulty.stage(l);g.start();g.level=l;g.fish.clear();g.items.clear();g.x=1100;g.y=1500;g.time=60;g.bossi.wait=g.hazard.wait=10000;g.expedition.state=2;return g;}
 static Game.Fish fish(Game g,int level,float dx){Game.Fish f=new Game.Fish(g.x+dx,g.y,level,0);f.turn=100;f.dashWait=20;f.vx=0;f.vy=1;g.fish.add(f);return f;}
 public static void main(String[] args)throws Exception{
  for(int stage=0;stage<Evolution.STAGES;stage++){
   int first=stage*5+1;
   for(int l=first;l<first+5;l++){
    check(Arrays.equals(profile(first),profile(l)),"Every balance value constant within stage "+stage);
    for(float time:new float[]{60,600,3600,86400}){
     Game g=game(l);g.time=time;Game.Fish f=fish(g,l+1,160);g.update(.01f,0,0,420,850);
     check(f.vx<-.99f&&f.chase>0,"Stronger fish pursues at any elapsed time");
     check(Math.abs((g.x+160-f.x)-Difficulty.chaseSpeed(l)*.01f)<.001f,"Pursuit speed follows stage");
     g=game(l);g.time=time;g.y=380;f=fish(g,Math.max(1,l-1),90);g.update(.01f,0,0,420,850);
     if(l>1){check(f.vx>.99f,"Weaker fish flees");check(Math.abs((f.x-g.x-90)-Difficulty.fleeSpeed(l)*.01f)<.001f,"Flee speed follows stage");}
    }
    Game g=game(l);Game.Fish f=fish(g,l+1,160);f.chase=Difficulty.chaseDuration(l)-.005f;g.update(.01f,0,0,420,850);
    check(f.chase<0&&f.vx>0,"Exhausted predator turns away");
    Game copy=Game.restore(g.snapshot());check(Arrays.equals(g.snapshot(),copy.snapshot()),"Resting predator saved exactly");
    for(int n=0;n<40;n++){g.update(.01f,0,0,420,850);copy.update(.01f,0,0,420,850);check(Arrays.equals(g.snapshot(),copy.snapshot()),"Resting AI resumes deterministically");}
   }
   if(stage>0){check(Difficulty.chaseRange(first)>Difficulty.chaseRange(first-1),"Chase range grows at stage boundary");check(Difficulty.fleeSpeed(first)>Difficulty.fleeSpeed(first-1),"Flee speed grows at stage boundary");check(Difficulty.bossWarning(first)<Difficulty.bossWarning(first-1),"Boss reaction shrinks at stage boundary");check(Difficulty.eventInterval(first)<Difficulty.eventInterval(first-1),"Events more frequent at stage boundary");}
   for(boolean calm:new boolean[]{false,true}){
    Game g=game(first);g.calm=calm;
    for(int n=0;n<4;n++){Game.Fish f=fish(g,first+1,0);f.x=g.x+(float)Math.cos(n*Math.PI/2)*160;f.y=g.y+(float)Math.sin(n*Math.PI/2)*160;}
    g.update(.01f,0,0,420,850);int chasing=0;
    for(Game.Fish f:g.fish)if(f.chase>0)chasing++;
    check(chasing==Difficulty.chaseLimit(first),"Concurrent chase cap in both modes");
    g=game(first);g.calm=calm;g.bossi.begin(g,Bossi.LEFT_PAW,420,850);
    check(Math.abs(g.bossi.warning-Difficulty.bossWarning(first)*(calm?1.25f:1))<.0001f,"Actual boss reaction uses stage and calm mode");
    for(int l=first;l<first+5;l++){
     Game other=game(l);other.calm=calm;other.bossi.begin(other,Bossi.LEFT_PAW,420,850);
     check(other.bossi.warning==g.bossi.warning,"Actual boss warning stable through stage");
    }
   }
   Game g=game(first);for(int n=0;n<5;n++){g.invincible=0;g.hurt(1000);if(n<4)g.start();}check(g.stage==Math.max(0,stage-1),"Fifth loss demotes");g.start();check(Arrays.equals(profile(g.level),profile(g.stage*5+1)),"Demotion restores corresponding balance");
  }
  check(Arrays.equals(profile(1),profile(0)),"Low bound");check(Arrays.equals(profile(225),profile(1000)),"High bound");
  System.out.println("StageBalanceTest: "+checks+" assertions passed");
 }
}

