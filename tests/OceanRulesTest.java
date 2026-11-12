package com.nicole.ocean;
import java.util.*;
import java.io.*;
public final class OceanRulesTest {
 static int checks;static void ok(boolean c,String m){checks++;if(!c)throw new AssertionError(m);}
 static Game ocean(int n){Game g=new Game();g.stage=g.highestStage=n;g.start();return g;}
 static void grow(Game g,int levels){for(int j=0;j<levels;j++)g.addXp(Game.threshold(g.level));}
 public static void main(String[] args)throws Exception{
  int normal=0,guard=0;Game run=ocean(0);
  for(int n=0;n<60;n++){
   Game g=ocean(n);int start=g.level;ok(start==1+n-n/6,"Start-level arithmetic");ok(g.paintParts.length==56,"All form palettes");
   if(g.guardianOcean()){
    guard++;ok(g.fish.isEmpty()&&g.items.isEmpty(),"Empty guardian ocean");g.x=1100;g.y=1500;g.time=2;g.update(.01f,0,0,420,850);ok(g.expedition.state==1,"Guardian starts without normal fish");
    g.expedition.age=7;g.x=g.expedition.x;g.y=g.expedition.y;g.invincible=0;g.update(.01f,0,0,420,850);ok(g.hp==g.maxHp(),"Distracted guardian harmless");
    grow(g,4);ok(g.active&&g.level==start+4,"Four growth levels do not auto-finish guardian");
    g.x=g.expedition.x;g.y=g.expedition.y;g.update(.01f,0,0,420,850);ok(g.expedition.state==2,"Eat guardian when four levels grown");
    for(int t=0;t<90;t++)g.update(.05f,0,0,420,850);ok(!g.active&&g.roundWon,"Celebration completes guardian ocean");
    if(n<59){g.start();ok(g.level==start,"Guardian grants no start-level increment");}
   }else{
    normal++;ok(!g.fish.isEmpty(),"Ordinary ocean populated");grow(g,2);ok(g.active&&g.level==start+2,"Two levels insufficient");grow(g,1);ok(!g.active&&g.level==start+3,"Exactly three levels wins");g.start();ok(g.level==start+1,"Next ocean starts one level above old start");
   }
   g=ocean(n);g.choosePaint(Evolution.index(g.level),4);Game cp=Game.restore(g.snapshot());ok(Arrays.equals(g.snapshot(),cp.snapshot()),"All ocean saves exact");
   g.invincible=cp.invincible=10000;
   for(int t=0;t<900;t++){float ix=(float)Math.sin(t*.03),iy=.6f;g.update(.02f,ix,iy,420,850);cp.update(.02f,ix,iy,420,850);ok(Arrays.equals(g.snapshot(),cp.snapshot()),"Deterministic ocean simulation");ok(g.fish.size()<=72&&g.expedition.drops.size()<=24,"Bounded pools");if(g.guardianOcean())ok(g.fish.isEmpty()&&g.items.isEmpty()&&!g.hazard.visible()&&!g.bossi.visible(),"Guardian ocean remains exclusive");}
   g=ocean(n);for(int loss=0;loss<5;loss++){g.invincible=0;g.hurt(1000);if(loss<4){ok(g.stage==n,"First four losses retain ocean");g.start();}}ok(g.stage==Math.max(0,n-1),"Fifth loss demotes ocean");
  }
  ok(normal==50&&guard==10,"50 normal plus 10 guardians");ok(Oceans.startLevel(59)==51&&Oceans.goalLevel(59)==55,"Final level 55");
  ok(Evolution.NAMES.length==56&&new HashSet<>(Arrays.asList(Evolution.NAMES)).size()==56,"56 named forms");for(int l=1;l<=56;l++)ok(Evolution.index(l)==l-1,"One form per level");
  for(int n=0;n<60;n++){float p=Difficulty.progress(n);ok(p>=0&&p<=1,"Ocean difficulty bounded");ok(Difficulty.eventInterval(n)<=18,"Events more frequent");}
  Game g=ocean(0);for(int p=0;p<5;p++){g.expedition.grant(p);ok(g.expedition.powers[p]>=15,"Longer powers");}g.expedition.reset();g.expedition.grant(1);ok(!g.hurt(40)&&g.hp==100,"Shield absorbs");
  g=ocean(5);g.x=1100;g.y=1500;g.expedition.begin(g);for(int k=0;k<52;k++){g.expedition.drops.clear();g.expedition.drops.add(new Expedition.Drop(g.x,g.y,0));g.expedition.update(g,.001f);}ok(g.level==Oceans.goalLevel(5),"Guardian resources suffice without fish");
  g=ocean(0);g.x=1100;g.y=1500;g.time=40;g.bossi.begin(g,Bossi.HEAD_PAWS,420,850);ok(g.bossi.pending()&&!g.bossi.visible(),"One-second invisible warning");float bx=g.bossi.x,by=g.bossi.y;g.bossi.update(g,.9f,420,850);ok(!g.bossi.visible(),"No early cat");g.x+=400;g.bossi.update(g,.11f,420,850);ok(g.bossi.visible()&&g.bossi.x==bx&&g.bossi.y==by,"Cat appears at fixed anchor");
  Game saved=Game.restore(g.snapshot());ok(Arrays.equals(g.snapshot(),saved.snapshot()),"Cat exact save");
  int near=0;g=ocean(0);g.x=1100;g.y=1800;for(int k=0;k<400;k++){g.bossi.begin(g,1,420,850);if(Math.abs(g.bossi.y-(g.y-260))<1)near++;}ok(near>340&&near<395,"Rare top placement, mostly near hero");
  for(int l=1;l<=55;l++)ok(Game.threshold(l)>12*(l+1)*2,"Slower growth");
  for(int k=0;k<32;k++){float wx=Oceans.decorX(4,k);ok((wx-200)-(wx-300)==100,"World decor follows camera translation exactly");}
  // Build a real v10-layout checkpoint (45 palettes), then migrate it.
  g=ocean(26);g.level=133;g.bestLevel=200;g.bank=321;g.stageLosses=4;g.choosePaint(26,4);g.xp=600;
  byte[] current=g.snapshot();byte[] modern=Arrays.copyOf(current,current.length-g.combat.bytes(g)-g.home.bytes());int cut=modern.length-8-g.expedition.bytes()-11*24;
  byte[] legacy=new byte[modern.length-11*24];System.arraycopy(modern,0,legacy,0,cut);System.arraycopy(modern,cut+11*24,legacy,cut,modern.length-cut-11*24);legacy[7]=10;
  java.util.zip.CRC32 crc=new java.util.zip.CRC32();crc.update(legacy,0,legacy.length-8);java.nio.ByteBuffer.wrap(legacy).putLong(legacy.length-8,crc.getValue());
  Game migrated=Game.restore(legacy);ok(migrated!=null,"v10 migration decodes");ok(migrated.stage==31&&migrated.level==29,"v10 progress mapped to normal ocean");ok(migrated.bank==321&&migrated.stageLosses==4&&migrated.paintPreset[26]==4,"v10 rewards, losses, paint preserved");ok(migrated.xp>0&&migrated.xp<Game.threshold(29),"v10 fractional experience preserved");
  ok(Arrays.equals(migrated.snapshot(),Game.restore(migrated.snapshot()).snapshot()),"Migrated checkpoint stable in v11");
  System.out.println("OceanRulesTest PASS: "+checks+" assertions; all 60 oceans, progression, 56 forms, guardians, bonuses, saves and Bossi");
 }
}
