package com.nicole.ocean;
import java.util.*;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

public final class CombatTest {
 static int checks;static void ok(boolean value,String text){checks++;if(!value)throw new AssertionError(text);}
 static Game game(){Game g=new Game();g.stage=g.highestStage=3;g.start();g.x=1000;g.y=1500;g.time=30;g.fish.clear();g.items.clear();g.hazard.wait=g.bossi.wait=1000;return g;}
 static Expedition.Shot shot(int level,float x,float y,float vx,float vy){Expedition.Shot s=new Expedition.Shot(x,y,vx,vy);s.level=level;return s;}
 static void hit(Game g,Game.Fish f){Expedition.Shot s=shot(g.level,f.x,f.y,0,0);g.combat.friendly(g,s,f.x-80,f.y);}
 static byte[] old11(Game g)throws Exception{byte[] b=Arrays.copyOf(g.snapshot(),g.snapshot().length-g.combat.bytes(g)-g.home.bytes());b[7]=11;CRC32 crc=new CRC32();crc.update(b,0,b.length-8);ByteBuffer.wrap(b).putLong(b.length-8,crc.getValue());return b;}
 public static void main(String[] args)throws Exception{
  for(int shooter=1;shooter<=55;shooter++)for(int target=1;target<=56;target++){
   int hits=Combat.hits(shooter,target);ok(hits>=2,"Never one hit");ok(hits==Math.max(2,3+2*(target-shooter)),"Level-gap scaling");
   if(target==shooter)continue;
   Game g=game();g.level=shooter;Game.Fish f=new Game.Fish(g.x+180,g.y,target,0);g.fish.add(f);
   for(int n=1;n<hits;n++){hit(g,f);ok(g.fish.contains(f)&&g.eaten==0&&g.shells==0,"No early death or partial reward");}
   Game expected=Game.restore(g.snapshot());int reward=expected.fishReward(expected.fish.get(0));expected.addXp(reward);
   hit(g,f);ok(g.fish.isEmpty()&&g.eaten==expected.eaten&&g.shells==expected.shells&&g.bank==expected.bank&&g.level==expected.level&&g.xp==expected.xp,"Kill matches eating reward exactly");
  }
  Game automatic=game();automatic.fish.add(new Game.Fish(automatic.x+160,automatic.y,3,0));automatic.expedition.grant(3);
  for(int n=0;n<40;n++)automatic.expedition.update(automatic,.05f);
  ok(automatic.fish.isEmpty()&&automatic.eaten==1&&automatic.shells==1&&automatic.xp==9,"Automatic firing gives eating reward after multiple real flights");
  Game obscured=game();Game.Fish covered=new Game.Fish(1130,obscured.y-850*.46f+100,5,1);covered.shootWait=0;obscured.fish.add(covered);
  obscured.combat.update(obscured,.05f,420,850,obscured.x,obscured.y);ok(covered.shootWarning==0,"Fish behind HUD cannot start shooting");
  Game g=game();Game.Fish equal=new Game.Fish(1100,1500,g.level,1);g.fish.add(equal);for(int i=0;i<20;i++)hit(g,equal);ok(equal.needleDamage==0&&g.eaten==0,"Equal levels immune");
  g=game();Game.Fish near=new Game.Fish(1100,1500,3,0),far=new Game.Fish(1200,1500,3,0);g.fish.add(far);g.fish.add(near);
  Expedition.Shot s=shot(g.level,1250,1500,0,0);g.combat.friendly(g,s,1000,1500);ok(near.needleDamage>0&&far.needleDamage==0,"Nearest intersection wins regardless of list order");
  g=game();g.x=1100;g.y=380;Game.Fish outside=new Game.Fish(1400,380,3,0);g.fish.add(outside);hit(g,outside);ok(outside.needleDamage>0,"Shooting from home reef is allowed");
  g=game();Game.Fish protectedFish=new Game.Fish(1100,380,3,0);g.fish.add(protectedFish);hit(g,protectedFish);ok(protectedFish.needleDamage>0,"Shots damage prey inside home reef");
  for(int ocean=0;ocean<3;ocean++){g=game();g.stage=ocean;Game.Fish f=new Game.Fish(1130,1500,5,1);g.fish.add(f);for(int n=0;n<500;n++)g.combat.update(g,.05f,420,850,g.x,g.y);ok(g.combat.hostile.isEmpty()&&f.shootWarning==0,"First three oceans have no shooters");}
  g=game();Game.Fish shooter=new Game.Fish(1130,1500,5,1);shooter.shootWait=0;g.fish.add(shooter);
  g.combat.update(g,.01f,420,850,g.x,g.y);ok(shooter.shootWarning>1&&g.combat.hostile.isEmpty(),"Visible warning before firing");float locked=shooter.shootY;
  g.y+=50;for(int n=0;n<20;n++)g.combat.update(g,.05f,420,850,g.x,g.y);ok(g.combat.hostile.isEmpty()&&shooter.shootY==locked,"Warning lasts and aim is fixed");
  for(int n=0;n<12;n++)g.combat.update(g,.05f,420,850,g.x,g.y);ok(!g.combat.hostile.isEmpty()&&g.combat.hostile.get(0).level==5,"Warned fish fires with saved level");
  Game cp=Game.restore(g.snapshot());ok(Arrays.equals(g.snapshot(),cp.snapshot()),"Warnings and hostile projectiles save exactly");
  for(int n=0;n<30;n++){g.combat.update(g,.02f,420,850,g.x,g.y);cp.combat.update(cp,.02f,420,850,cp.x,cp.y);ok(Arrays.equals(g.snapshot(),cp.snapshot()),"Combat deterministic after load");}
  g=game();g.combat.hostile.add(shot(5,g.x-60,g.y,220,0));for(int n=0;n<6;n++)g.combat.update(g,.05f,420,850,g.x,g.y);ok(g.hp==50&&g.combat.hostile.isEmpty(),"Enemy needle hurts and is consumed");
  g=game();g.expedition.grant(1);g.combat.hostile.add(shot(5,g.x,g.y,0,0));g.combat.update(g,.01f,420,850,g.x,g.y);ok(g.hp==100&&g.expedition.powers[1]==0,"Shield absorbs needle");
  g=game();g.expedition.grant(2);g.combat.hostile.add(shot(5,g.x,g.y,0,0));g.combat.update(g,.01f,420,850,g.x,g.y);ok(g.hp==100,"Invincibility absorbs needle");
  g=game();g.combat.hostile.add(shot(g.level,g.x,g.y,0,0));g.combat.update(g,.01f,420,850,g.x,g.y);ok(g.hp==100,"Equal enemy projectile harmless");
  g=game();g.hiddenTime=2;g.combat.hostile.add(shot(5,g.x,g.y,0,0));g.combat.update(g,.01f,420,850,g.x,g.y);ok(g.hp==100,"Cover protects against needles");
  g=game();g.stage=5;g.start();g.x=1100;g.y=1500;g.expedition.begin(g);float ex=g.expedition.x,ey=g.expedition.y;
  for(int n=0;n<50;n++){s=shot(g.level,ex,ey,0,0);g.combat.friendly(g,s,ex-150,ey);}
  ok(g.expedition.state==1&&g.combat.guardianDamage==.9f,"Needles cannot bypass guardian growth");
  g.level=Oceans.goalLevel(g.stage);s=shot(g.level,ex,ey,0,0);g.combat.friendly(g,s,ex-150,ey);ok(g.expedition.state==2&&g.expedition.celebrate==4,"Eligible guardian can be finished by needle");
  int shells=g.shells;g.expedition.defeat(g);ok(g.shells==shells,"Guardian reward only once");
  g=game();Game.Fish damaged=new Game.Fish(1130,1500,5,1);damaged.needleDamage=.4f;damaged.shootWarning=.7f;damaged.shootX=-1;g.fish.add(damaged);g.expedition.shots.add(shot(4,1000,1500,360,0));
  cp=Game.restore(g.snapshot());ok(Arrays.equals(g.snapshot(),cp.snapshot()),"Damage, warning and friendly shot level persisted");
  Game legacy=Game.restore(old11(g));ok(legacy.stage==g.stage&&legacy.level==g.level&&legacy.fish.get(0).needleDamage==0&&legacy.expedition.shots.get(0).level==g.level,"v11 migration initializes combat without changing ocean");
  damaged.needleDamage=Float.NaN;try{Game.restore(g.snapshot());throw new AssertionError("Invalid damage accepted");}catch(java.io.IOException expected){checks++;}
  g=game();g.expedition.grant(1);g.expedition.grant(3);ok(g.expedition.powers[1]==15&&g.expedition.powers[3]==15,"Shield and needles last at most fifteen seconds");
  g.expedition.powers[1]=36;g.expedition.powers[3]=24;cp=Game.restore(g.snapshot());ok(cp.expedition.powers[1]==15&&cp.expedition.powers[3]==15,"Old long timers capped on load");
  cp.expedition.powers[1]=cp.expedition.powers[3]=.01f;cp.expedition.update(cp,.02f);ok(cp.expedition.powers[1]==0&&cp.expedition.powers[3]==0,"Short powers actually expire");
  System.out.println("CombatTest PASS: "+checks+" assertions; level gaps, rewards, telegraphs, protection, guardians and v11 migration");
 }
}
