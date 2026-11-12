package com.nicole.ocean;
import java.util.Arrays;
public final class LethalHazardTest {
 static int checks;static void ok(boolean v,String msg){checks++;if(!v)throw new AssertionError(msg);}
 static Game game(){Game g=new Game();g.stage=g.highestStage=3;g.start();g.x=1100;g.y=1500;g.fish.clear();g.items.clear();return g;}
 static void contact(Game g,int type){
  Hazard h=g.hazard;h.begin(g,type,420,850);h.stage=2;h.timer=type==Hazard.GEYSER?1.2f:type==Hazard.MINE?4.8f:h.duration()*.5f;
  g.x=100;g.y=2700;h.update(g,0,420,850);
  g.x=type==Hazard.JELLY||type==Hazard.MIGRATION?h.elementX(1):h.x;
  g.y=type==Hazard.JELLY||type==Hazard.MIGRATION?h.elementY(1):h.y;
  if(type==Hazard.MINE)g.x+=h.blastRadius();
 }
 public static void main(String[] args)throws Exception{
  for(int type=1;type<=Hazard.TYPE_COUNT;type++)for(int mode=0;mode<7;mode++){
   Game g=game();contact(g,type);g.calm=mode==1;g.invincible=mode==2?100:0;g.hiddenTime=mode==3?2:0;
   if(mode==6)g.hazard.hit=true;
   if(mode==4)g.expedition.grant(1);if(mode==5)g.expedition.grant(2);
   Game copy=Game.restore(g.snapshot());g.hazard.update(g,0,420,850);copy.hazard.update(copy,0,420,850);
   ok(!g.active&&g.hp==0&&g.stageLosses==1&&g.hazard.hit,"All 11 hazards kill through each protection mode");
   ok(Arrays.equals(g.snapshot(),copy.snapshot()),"Lethal contact deterministic after load");
   byte[] after=g.snapshot();g.hazard.update(g,100,420,850);ok(Arrays.equals(after,g.snapshot()),"No post-death progression or rewards");
  }
  for(int type=1;type<=Hazard.TYPE_COUNT;type++){
   Game g=game();g.hazard.begin(g,type,420,850);g.x=g.hazard.originX;g.y=g.hazard.originY;
   for(int n=0;n<10;n++)g.hazard.update(g,g.hazard.warning()/11,420,850);
   ok(g.active&&g.hp==100,"Warning harmless for every hazard");
   g=game();contact(g,type);g.x=1100;g.y=380;g.hazard.update(g,0,420,850);ok(g.active&&g.hp==100,"Distant non-contact point remains safe");
   g=game();contact(g,type);g.x=100;g.y=2700;int shells=g.shells;
   while(g.hazard.visible())g.hazard.update(g,.05f,420,850);
   ok(g.active&&g.hp==100&&g.shells==shells+4,"Evading hazard survives and rewards once");
   g=game();g.stageLosses=4;contact(g,type);g.hazard.update(g,0,420,850);ok(g.stage==2&&g.stageLosses==0&&g.demoted,"Fifth lethal event demotes one ocean");
  }
  Game g=game();contact(g,Hazard.GEYSER);g.hazard.timer=.1f;g.hazard.update(g,0,420,850);ok(g.active,"Inactive geyser phase safe");
  g=game();contact(g,Hazard.WHIRLPOOL);g.x=g.hazard.x+140;g.hazard.update(g,.01f,420,850);ok(g.active&&g.x<g.hazard.x+140,"Whirlpool outer flow pulls without killing");
  System.out.println("LethalHazardTest PASS: "+checks+" assertions; 11 hazards, warnings, dodges, protection modes, saves and demotion");
 }
}
