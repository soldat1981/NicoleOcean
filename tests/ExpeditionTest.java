package com.nicole.ocean;
import java.util.*;
public final class ExpeditionTest {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static Game game(int stage){Game g=new Game();g.stage=g.highestStage=stage;g.start();g.level=stage*5+2;g.x=1100;g.y=1500;g.time=30;g.fish.clear();g.items.clear();g.bossi.wait=g.hazard.wait=10000;return g;}
 public static void main(String[] args)throws Exception{
  check(Evolution.NAMES.length==45&&Evolution.MAX_LEVEL==225,"Fivefold content");check(new HashSet<>(Arrays.asList(Evolution.NAMES)).size()==45,"Unique form names");
  for(int stage=0;stage<45;stage++){
   Game g=game(stage);Expedition e=g.expedition;e.update(g,.01f);check(e.state==1&&e.targetLevel==g.level+2,"Guardian two levels stronger at second level");
   float x=e.x;e.update(g,.05f);check(x==e.x,"Guardian gives arrival grace");
   e.age=2;e.update(g,.05f);check(e.x<x,"Guardian pursues");
   g.hazard.wait=0;g.hazard.update(g,.05f,420,850);g.bossi.wait=0;g.updateVisitors(.05f,420,850);check(!g.hazard.visible()&&!g.bossi.visible()&&!g.nicole.visible,"Guardian excludes other dangers");
   e.dropTimer=0;e.update(g,.01f);check(!e.drops.isEmpty()&&e.drops.get(0).type==0,"Guardian drops edible growth resource");
   e.drops.clear();g.xp=0;for(int n=0;n<15;n++){e.drops.add(new Expedition.Drop(g.x,g.y,0));e.update(g,.001f);}check(g.level>e.targetLevel,"Collecting loot makes guardian edible");
   g.x=e.x;g.y=e.y;g.invincible=0;e.update(g,.01f);check(e.state==2&&e.celebrate>0,"Guardian consumed and victory celebrates");int shells=g.shells;e.update(g,.01f);check(g.shells==shells,"Reward once only");
   g.addXp(100000);check(g.active,"Celebration is not cut off by round end");for(int n=0;n<90;n++)e.update(g,.05f);check(!g.active&&g.roundWon,"Round can finish after celebration");
   g.start();check(g.expedition.state==0&&g.expedition.drops.isEmpty(),"New round resets encounter");
   g=game(stage);e=g.expedition;e.begin(g);e.age=3;for(int type=0;type<5;type++)e.grant(type);
   Game copy=Game.restore(g.snapshot());check(Arrays.equals(g.snapshot(),copy.snapshot()),"Exact guardian and powers save");
   for(int frame=0;frame<600;frame++){
    g.update(.016f,.3f,.1f,420,850);copy.update(.016f,.3f,.1f,420,850);
    check(Arrays.equals(g.snapshot(),copy.snapshot()),"Deterministic encounter resume");
    check(g.expedition.drops.size()<=24&&g.expedition.shots.size()<=16,"Bounded resource and projectile pools");
    check(!(g.expedition.busy()&&(g.hazard.visible()||g.bossi.visible()||g.nicole.visible)),"No overlapping encounter");
   }
  }
  for(int type=0;type<5;type++){
   Game g=game(0);g.expedition.state=2;g.expedition.drops.add(new Expedition.Drop(g.x,g.y,type+1));g.expedition.update(g,.01f);check(g.expedition.powers[type]>0,"Every bonus collected");
   for(int n=0;n<300;n++)g.expedition.update(g,.05f);check(g.expedition.powers[type]==0,"Every bonus expires");
  }
  Game g=game(0);g.expedition.grant(1);check(!g.hurt(20)&&g.hp==100&&g.expedition.powers[1]==0,"Shield absorbs exactly one hit");g.invincible=0;check(g.hurt(20)&&g.hp==80,"Next hit damages after shield");
  g=game(0);g.expedition.grant(2);for(int n=0;n<10;n++){g.invincible=0;check(!g.hurt(999),"Invulnerability blocks repeated hits");}check(g.expedition.powers[2]>0,"Immortality does not consume on contact");
  g=game(0);g.expedition.state=2;g.x=1100;g.y=380;g.expedition.drops.add(new Expedition.Drop(g.x,g.y,0));g.expedition.update(g,.01f);check(g.xp==0&&g.expedition.drops.size()==1,"No eating guardian loot in harbor");
  g=game(0);g.expedition.state=2;g.expedition.grant(3);Game.Fish f=new Game.Fish(g.x+150,g.y,9,0);g.fish.add(f);for(int n=0;n<60;n++)g.expedition.update(g,.016f);check(f.chase<0,"Automatic needles repel stronger fish");
  g=game(0);g.expedition.begin(g);g.expedition.age=3;g.expedition.stun=1;g.x=g.expedition.x;g.y=g.expedition.y;g.expedition.update(g,.01f);check(g.hp==100,"Stunned guardian cannot damage on contact");
  g=game(0);g.expedition.grant(4);g.expedition.drops.add(new Expedition.Drop(g.x+200,g.y,1));g.expedition.update(g,.05f);check(g.expedition.drops.get(0).x<g.x+200,"Magnet attracts bonus");
  for(int stage=0;stage<9;stage++){g=game(stage);g.choosePaint(stage,4);Game old=Game.restore(LegacyFixtures.v9(g));check(old.level==g.level&&old.stage==stage&&old.paintPreset[stage]==4&&old.paintParts.length==45,"v9 migration keeps old colors and expands slots");}
  g=game(44);g.level=225;check(Game.radius(g.level)<70,"Final fish remains phone-sized");check(Game.restore(g.snapshot()).level==225,"Final level save");
  System.out.println("ExpeditionTest: "+checks+" assertions passed; 45 guardians, five powers, migration and deterministic resumes");
 }
}
