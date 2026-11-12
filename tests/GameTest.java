package com.nicole.ocean;
import java.util.*;
import java.io.*;
public final class GameTest {
    static int assertions;
    static void check(boolean b,String s){assertions++;if(!b)throw new AssertionError(s);}
    static void near(float a,float b,String s){check(Math.abs(a-b)<.001f,s+": "+a+" != "+b);}
    static Game empty(){Game g=new Game();g.start();g.fish.clear();g.items.clear();g.time=25;g.x=1100;g.y=1200;return g;}
    public static void main(String[] args)throws Exception{
        roundTests();
        progressionTests();
        polishTests();
        check(Game.damage(3,5)==25,"damage formula");check(Game.damage(3,3)==0,"equal levels safe");check(Game.damage(1,10)==35,"damage cap");
        Game g=empty();g.level=3;g.stage=Evolution.index(g.level);g.highestStage=g.stage;check(g.hit(5),"first hit");near(g.hp,75,"25 damage");check(!g.hit(8),"global protection");near(g.hp,75,"no stacked damage");
        g=empty();g.calm=true;g.level=3;g.stage=Evolution.index(g.level);g.highestStage=g.stage;g.hit(5);near(g.hp,87.5f,"calm half damage");
        g=empty();g.hp=10;g.hit(10);check(!g.active,"death ends run");near(g.hp,0,"health clamped");int runs=g.totalRuns;g.finish();check(g.totalRuns==runs,"finish idempotent");
        g=empty();g.addXp(30);check(g.level==2,"level up");g.stage=Evolution.index(g.level);g.highestStage=g.stage;near(g.xp,6,"XP carries");allRounds(g);check(g.level==Evolution.MAX_LEVEL,"max level");g.stage=Evolution.index(g.level);g.highestStage=g.stage;near(g.xp,0,"max XP cap");
        g=empty();g.hp=40;g.addXp(60);check(g.level==3&&g.pendingUpgrade==0,"growth never requires a choice");g.stage=Evolution.index(g.level);g.highestStage=g.stage;check(g.speedBonus==1&&g.healBonus==5,"both bonuses automatic");near(g.hp,80,"level healing plus auto bonus");g.addXp(0);check(g.speedBonus==1,"no duplicated auto bonus");
        g=empty();allRounds(g);check(g.speedBonus==3&&g.healBonus==15&&g.pendingUpgrade==0,"all milestone bonuses granted in multi-level gain");
        g=empty();g.update(1f/60,1,0,420,850);check(g.vx>45,"faster initial response");near(g.heading,0,"face right");
        g.update(.05f,0,-1,420,850);check(g.heading<-.8f,"turn head toward upward motion");
        g=empty();for(int i=0;i<20;i++)g.update(1f/60,0,1,420,850);near(g.heading,(float)Math.PI/2,"face down");
        for(int i=0;i<20;i++)g.update(1f/60,-1,-1,420,850);check(g.heading<-2&&g.heading>-3,"face upper left");
        // v1 -> v2 migration also resolves a pending manual bonus from the old release.
        g=empty();g.level=3;g.stage=Evolution.index(g.level);g.highestStage=g.stage;g.pendingUpgrade=3;g.upgradeMask=1<<3;g.face=-1;g.vx=g.vy=0;
        byte[] old=legacySave(g);Game migrated=Game.restore(old);
        check(migrated.pendingUpgrade==0&&migrated.speedBonus==1&&migrated.healBonus==5,"old pending bonus granted automatically");near(migrated.heading,(float)Math.PI,"old facing migrated");
        Game reloaded=Game.restore(migrated.snapshot());check(reloaded.speedBonus==1,"migrated bonus not repeated");
        g=empty();g.level=3;g.stage=Evolution.index(g.level);g.highestStage=g.stage;g.fish.add(new Game.Fish(g.x,g.y,2,0));g.update(.01f,0,0,420,850);check(g.eaten==1&&g.shells==1,"eat smaller once");near(g.xp,7,"prey reward");
        g=empty();g.level=3;g.stage=Evolution.index(g.level);g.highestStage=g.stage;g.fish.add(new Game.Fish(g.x,g.y,3,0));g.update(.01f,0,0,420,850);check(g.eaten==0,"cannot eat equal");near(g.hp,100,"equal no damage");
        g=empty();g.level=3;g.stage=Evolution.index(g.level);g.highestStage=g.stage;g.fish.add(new Game.Fish(g.x,g.y,5,0));g.update(.01f,0,0,420,850);near(g.hp,75,"large collision");
        g=empty();g.level=3;g.stage=Evolution.index(g.level);g.highestStage=g.stage;g.fish.add(new Game.Fish(g.x,g.y,5,0));g.fish.add(new Game.Fish(g.x,g.y,6,1));g.update(.01f,0,0,420,850);near(g.hp,75,"two enemies one hit");
        g=empty();g.time=1;g.fish.add(new Game.Fish(g.x,g.y,8,0));g.update(.01f,0,0,420,850);check(g.hp<100,"contact hurts even during opening seconds");
        g=empty();g.x=1100;g.y=380;g.fish.add(new Game.Fish(g.x,g.y,8,0));g.update(.01f,0,0,420,850);near(g.hp,100,"safe lagoon");
        g=empty();g.hp=95;g.items.add(new Game.Item(g.x,g.y,1));g.update(.01f,0,0,420,850);near(g.hp,100,"healing capped");
        g=empty();g.shells=15;g.pearls=2;g.finish();check(g.bank==15&&g.unlocked==3,"bank and unlock");g.finish();check(g.bank==15,"no duplicate reward");
        g=empty();g.bank=50;check(g.buyDecor(),"decoration purchase");check(g.bank==38&&g.decor==1,"purchase exact cost");
        g=empty();check(g.useDash(),"dash available");check(!g.useDash(),"dash cooldown");
        // Bossi is rare, telegraphs each attempt, then leaves after a spoken call.
        for(int variant=Bossi.LEFT_PAW;variant<=Bossi.HEAD_PAWS;variant++){
            g=empty();g.bossi.begin(g,variant,420,850);check(g.bossi.stage==Bossi.ARRIVE,"cat arrives without advance lure");
            int calls=0,serial=g.bossi.audioSerial;float elapsed=0;
            while(g.bossi.visible()&&elapsed<60){
                g.bossi.update(g,.05f,420,850);elapsed+=.05f;
                if(g.bossi.audioSerial!=serial){serial=g.bossi.audioSerial;if(g.bossi.audioKind>1){calls++;check(g.bossi.stage==Bossi.CALL,"voice follows all attempts");}else check(g.bossi.stage==Bossi.ARRIVE,"meow on attacking cat appearance");}
            }
            check(!g.bossi.visible()&&calls==1,"cat called away exactly once");
            check(g.bossi.attempts>=3&&g.bossi.attempts<=4,"three to four attempts");
            check(g.bossi.wait>=18&&g.bossi.wait<=28,"shorter shared rest between visits");
        }
        g=empty();check(g.bossi.wait>=18&&g.bossi.wait<=26,"first visit sooner");
        g.bossi.begin(g,Bossi.TAIL,420,850);check(g.bossi.audioKind==1&&g.bossi.audioSerial==1,"tail has a single meow");
        for(int j=0;j<30;j++)g.bossi.update(g,.05f,420,850);
        check(g.bossi.stage==Bossi.HOLD_TAIL&&!g.bossi.hunting(),"tail is passive");float tailX=g.bossi.x,tailY=g.bossi.y;
        for(int j=0;j<100;j++)g.bossi.update(g,.05f,420,850);
        near(g.bossi.x,tailX,"tail does not move horizontally");near(g.bossi.y,tailY,"tail does not move vertically");
        g.bossi.collide(g);near(g.hp,100,"tail causes no damage");check(g.bossi.audioSerial==1,"no repeated meow");
        g=empty();g.bossi.begin(g,Bossi.LEFT_PAW,420,850);g.bossi.stage=Bossi.STRIKE;g.bossi.timer=.48f*.45f;g.bossi.targetX=g.x;g.bossi.targetY=g.y;
        g.bossi.collide(g);near(g.hp,86,"paw damage");g.bossi.collide(g);near(g.hp,86,"one damage per attempt");
        g=empty();g.bossi.begin(g,Bossi.HEAD_PAWS,420,850);g.bossi.stage=Bossi.AIM;g.bossi.targetX=g.x;g.bossi.targetY=g.y;
        Game.Fish left=new Game.Fish(g.x-60,g.y,8,0),right=new Game.Fish(g.x+60,g.y,8,1);g.fish.add(left);g.fish.add(right);
        g.update(.01f,0,0,420,850);check(left.vx<0&&right.vx>0,"all fish flee in different directions");
        // Save every stage/variant and continue deterministically; an interrupted visit is not rerolled.
        for(int variant=1;variant<=5;variant++){
            g=empty();g.bossi.begin(g,variant,420,850);g.bossi.timer=.6f;
            Game catCopy=Game.restore(g.snapshot());check(Arrays.equals(g.snapshot(),catCopy.snapshot()),"cat snapshot roundtrip");
            g.update(.016f,.2f,.1f,420,850);catCopy.update(.016f,.2f,.1f,420,850);check(Arrays.equals(g.snapshot(),catCopy.snapshot()),"cat deterministic continuation");
        }
        g=empty();Game v2=Game.restore(legacyV2Save(g));check(v2.level==g.level&&v2.bossi.kind==0,"v2 save loads without resetting hero");
        for(int tier=0;tier<Evolution.NAMES.length;tier++){
            check(Evolution.index(tier*5+1)==tier,"new character every five levels");
            check(Evolution.index(tier*5+5)==tier,"character lasts five levels");
        }
        check(Game.radius(45)<70,"high levels stay playable on phone");
        g=empty();g.level=45;g.stage=Evolution.index(g.level);g.highestStage=g.stage;g.bestLevel=45;check(Game.restore(g.snapshot()).level==45,"highest evolution survives restart");
        g=empty();g.bossi.wait=190;Game v3=Game.restore(legacyV3Save(g));
        check(v3.level==g.level&&v3.bossi.wait<=26,"v3 save retained and old long wait shortened");
        g=empty();g.nicole.begin(g);g.nicole.face=2;g.nicole.gear=2;g.nicole.time=4.25f;
        Game nicoleCopy=Game.restore(g.snapshot());check(Arrays.equals(g.snapshot(),nicoleCopy.snapshot()),"face variant, equipment and fade saved exactly");
        g.update(.016f,0,0,420,850);nicoleCopy.update(.016f,0,0,420,850);
        check(Arrays.equals(g.snapshot(),nicoleCopy.snapshot()),"passive visitor continues deterministically");
        g.bossi.begin(g,Bossi.HEAD_PAWS,420,850);check(!g.nicole.visible,"cat excludes Nicole immediately");
        g.nicole.begin(g);check(!g.nicole.visible,"Nicole cannot start during cat visit");
        // 50 minutes of the shared schedule: equal frequency, varied faces/gear, no overlap or damage.
        g=empty();int catVisits=0,nicoleVisits=0,faceBits=0,gearBits=0;boolean wasCat=false,wasNicole=false;
        for(int i=0;i<60000;i++){
            g.time+=.05f;g.updateVisitors(.05f,420,850);
            check(!(g.bossi.visible()&&g.nicole.visible),"visitors mutually exclusive");
            if(g.bossi.visible()&&!wasCat)catVisits++;
            if(g.nicole.visible&&!wasNicole){nicoleVisits++;faceBits|=1<<g.nicole.face;gearBits|=1<<g.nicole.gear;}
            wasCat=g.bossi.visible();wasNicole=g.nicole.visible;
        }
        check(catVisits>20&&nicoleVisits>20,"both visitors are frequent enough");
        check(Math.abs(catVisits-nicoleVisits)<=1,"both visitors have the same frequency");
        check(faceBits==7&&gearBits==7,"all three faces and all three gear types appear");near(g.hp,100,"background schedule does not damage hero");
        g=empty();g.level=38;g.stage=Evolution.index(g.level);g.highestStage=g.stage;g.bossi.wait=1000;
        for(int j=0;j<10;j++){g.fish.clear();g.update(.01f,0,0,420,850);check(!g.fish.isEmpty()&&g.fish.get(0).level>25,"new opponents support late evolution levels");}
        // Full snapshot: all fish, item locations, random state and cooldowns survive byte-for-byte.
        g=new Game();g.start();g.x=735.5f;g.y=1000.25f;g.level=6;g.stage=Evolution.index(g.level);g.highestStage=g.stage;g.hp=43;g.xp=13;g.shells=19;g.cooldown=2.1f;g.sequence=178;g.mission=2;
        byte[] saved=g.snapshot();Game copy=Game.restore(saved);check(Arrays.equals(saved,copy.snapshot()),"exact snapshot roundtrip");
        g.update(.016f,.4f,.6f,420,850);copy.update(.016f,.4f,.6f,420,850);check(Arrays.equals(g.snapshot(),copy.snapshot()),"deterministic continuation incl RNG");
        byte[] damaged=saved.clone();damaged[85]^=4;boolean rejected=false;try{Game.restore(damaged);}catch(IOException e){rejected=true;}check(rejected,"CRC rejects corruption");
        rejected=false;try{Game.restore(Arrays.copyOf(saved,saved.length-8));}catch(IOException e){rejected=true;}check(rejected,"truncated file rejected");
        // Long simulation exercises spawning, boundaries and save/load at arbitrary points.
        g=new Game();g.start();g.calm=true;
        for(int i=0;i<18000;i++){
            if(!g.active)g.start();
            g.update(1f/60,(float)Math.sin(i*.008),(float)Math.cos(i*.011),420,850);
            if(i%61==0){byte[] b=g.snapshot();g=Game.restore(b);check(Arrays.equals(b,g.snapshot()),"long-run save");}
            check(g.level>=1&&g.level<=Evolution.MAX_LEVEL&&g.hp>=0&&g.hp<=g.maxHp(),"valid health/level");
            check(g.fish.size()<=Game.MAX_FISH&&g.items.size()<=Game.MAX_ITEMS,"spawn limits");
        }
        System.out.println("PASS: "+assertions+" assertions; contact, density, combat, events, growth, exact resume; 20 minutes game simulation + 50 minutes visitor schedule.");
    }
    static byte[] legacySave(Game g)throws IOException{
        byte[] modern=g.snapshot();int headingOffset=149;
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        bytes.write(modern,0,headingOffset);bytes.write(modern,headingOffset+4,modern.length-headingOffset-4-8-Bossi.SAVE_BYTES-Nicole.SAVE_BYTES-Hazard.SAVE_BYTES-12-Game.Codec.extensionBytes(g));
        byte[] payload=bytes.toByteArray();payload[7]=1;
        java.util.zip.CRC32 crc=new java.util.zip.CRC32();crc.update(payload);
        ByteArrayOutputStream result=new ByteArrayOutputStream();result.write(payload);new DataOutputStream(result).writeLong(crc.getValue());return result.toByteArray();
    }
    static byte[] legacyV2Save(Game g)throws IOException{
        byte[] modern=g.snapshot(),payload=Arrays.copyOf(modern,modern.length-8-Bossi.SAVE_BYTES-Nicole.SAVE_BYTES-Hazard.SAVE_BYTES-12-Game.Codec.extensionBytes(g));payload[7]=2;
        java.util.zip.CRC32 crc=new java.util.zip.CRC32();crc.update(payload);
        ByteArrayOutputStream result=new ByteArrayOutputStream();result.write(payload);new DataOutputStream(result).writeLong(crc.getValue());return result.toByteArray();
    }
    static byte[] legacyV3Save(Game g)throws IOException{
        byte[] modern=g.snapshot(),payload=Arrays.copyOf(modern,modern.length-8-Nicole.SAVE_BYTES-Hazard.SAVE_BYTES-12-Game.Codec.extensionBytes(g));payload[7]=3;
        java.util.zip.CRC32 crc=new java.util.zip.CRC32();crc.update(payload);
        ByteArrayOutputStream result=new ByteArrayOutputStream();result.write(payload);new DataOutputStream(result).writeLong(crc.getValue());return result.toByteArray();
    }
    static void polishTests()throws Exception{
        // Eating works on either flank, above, below and behind every evolved character.
        for(int tier=0;tier<9;tier++)for(int side=0;side<8;side++){
            Game g=empty();g.level=3+tier*5;g.stage=Evolution.index(g.level);g.highestStage=g.stage;g.heading=1.1f;
            float angle=side*(float)Math.PI/4,r=Game.radius(g.level)+Game.radius(g.level-1)-1;
            g.fish.add(new Game.Fish(g.x+(float)Math.cos(angle)*r,g.y+(float)Math.sin(angle)*r,g.level-1,side%4));
            g.update(.0001f,0,0,420,850);check(g.eaten==1,"any-side contact eats prey, tier "+tier+" side "+side);
            g.update(.0001f,0,0,420,850);check(g.eaten==1,"prey cannot be eaten twice");
        }
        near(Game.sweptDistance(-150,0,150,0),0,"swept crossing is detected");
        near(Game.sweptDistance(-150,90,150,90),90,"swept near miss remains a miss");
        Game g=empty();g.level=5;g.stage=Evolution.index(g.level);g.highestStage=g.stage;
        for(int n=0;n<3;n++){g.fish.clear();g.fish.add(new Game.Fish(g.x,g.y,1,0));g.update(.01f,0,0,420,850);}
        check(g.combo==3&&g.comboTime>3.9f,"successive prey build combo");
        g.hurt(1);check(g.combo==0,"damage breaks combo");
        g=new Game();g.start();int local=0;for(Game.Fish f:g.fish)if(Math.abs(f.x-g.x)<315&&Math.abs(f.y-g.y)<552)local++;
        check(local>=4&&local<=12,"opening ocean has a sparse readable population: "+local);
        g=empty();g.level=10;g.stage=Evolution.index(g.level);g.highestStage=g.stage;g.bossi.wait=10000;g.hazard.wait=10000;g.invincible=10000;
        for(int j=0;j<2000;j++)g.update(.016f,0,0,420,850);
        local=0;int prey=0;for(Game.Fish f:g.fish)if(Math.abs(f.x-g.x)<315&&Math.abs(f.y-g.y)<552){local++;if(f.level<g.level)prey++;}
        check(local>=4&&prey>=2,"director replenishes sparse nearby prey: "+local+" / "+prey);
        // Warning cannot hurt; all events have exact deterministic continuation and finite lives.
        for(int type=1;type<=Hazard.TYPE_COUNT;type++){
            g=empty();g.hazard.begin(g,type,420,850);g.bossi.wait=0;
            float lockedX=g.hazard.originX,lockedY=g.hazard.originY;
            g.hazard.update(g,.5f,420,850);near(g.hp,100,"warning harmless");
            g.updateVisitors(.05f,420,850);check(!g.bossi.visible()&&!g.nicole.visible,"hazard suppresses visitors");
            g.x+=50;g.hazard.update(g,.5f,420,850);near(g.hazard.originX,lockedX,"route does not retarget");near(g.hazard.originY,lockedY,"depth does not retarget");
            Game copy=Game.restore(g.snapshot());check(Arrays.equals(g.snapshot(),copy.snapshot()),"warning exact snapshot");
            for(int n=0;n<420;n++){
                g.hazard.update(g,.05f,420,850);copy.hazard.update(copy,.05f,420,850);
                check(Arrays.equals(g.snapshot(),copy.snapshot()),"event deterministic after load");
            }
            check(!g.hazard.visible(),"event finishes");check(g.hp>=76,"at most one 24 HP event impact");
            check(g.hazard.wait>0,"event cooldown prevents repeats");
        }
        g=empty();g.hazard.begin(g,1,420,850);near(g.hazard.length,1260,"submarine is three screens long");
        g.hazard.stage=2;g.hazard.timer=6.5f;g.x=g.hazard.originX;g.y=g.hazard.originY;g.hazard.update(g,0,420,850);
        near(g.hp,88,"submarine hitbox aligned to visible hull");g.invincible=0;g.hazard.update(g,0,420,850);near(g.hp,88,"one hit per event");
        g=empty();g.hazard.begin(g,4,420,850);g.hazard.stage=2;g.hazard.timer=1;g.x=g.hazard.originX+150;g.y=g.hazard.originY;
        float d=Game.dist(g.x,g.y,g.hazard.originX,g.hazard.originY);g.hazard.update(g,.05f,420,850);
        check(Game.dist(g.x,g.y,g.hazard.originX,g.hazard.originY)<d,"whirlpool current pulls inward");near(g.hp,100,"outer swirl does not hurt");
        // Genuine v4 layout migrates without resetting a running game.
        g=empty();g.level=19;g.stage=Evolution.index(g.level);g.highestStage=g.stage;g.shells=51;byte[] modern=g.snapshot(),payload=Arrays.copyOf(modern,modern.length-8-Hazard.SAVE_BYTES-12-Game.Codec.extensionBytes(g));payload[7]=4;
        java.util.zip.CRC32 crc=new java.util.zip.CRC32();crc.update(payload);ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(payload);new DataOutputStream(out).writeLong(crc.getValue());
        Game old=Game.restore(out.toByteArray());check(old.level==19&&old.shells==51&&!old.hazard.visible(),"v4 migration preserves progress");
        // Fifteen minutes of the full director, including fish panic and exact reloads.
        g=empty();g.level=45;g.stage=Evolution.index(g.level);g.highestStage=g.stage;g.invincible=10000;g.expedition.state=2;int events=0,bits=0,last=0;
        for(int n=0;n<36000;n++){
            g.xp=0;g.update(.025f,(float)Math.sin(n*.001)*.25f,(float)Math.cos(n*.0013)*.25f,420,850);
            int type=g.hazard.kind;if(type!=0&&last==0){events++;bits|=1<<(type-1);}last=type;
            check(!g.hazard.visible()||!g.bossi.visible()&&!g.nicole.visible,"full director never stacks a visitor with a hazard");
            check(g.particles.size()<=240&&g.fish.size()<=112,"bounded graphics and fish workload");
            if(n%997==0){byte[] state=g.snapshot();g=Game.restore(state);check(Arrays.equals(state,g.snapshot()),"full director exact reload");}
        }
        check(events>=11&&bits==Hazard.ALL_TYPES,"all eleven events occur in long play");
    }
    static void allRounds(Game g){for(int n=0;n<Evolution.STAGES;n++){g.stage=n;g.start();g.addXp(100000);}}
    static void roundTests()throws Exception{
        Game g=new Game();g.start();g.shells=7;g.addXp(100000);
        check(!g.active&&g.roundWon&&g.level==5&&g.stage==1&&g.highestStage==1,"first round ends at five and unlocks mermaid");
        check(g.bank>=7&&g.lastLevel==5,"victory deposits rewards");
        for(int preset=0;preset<5;preset++){
            g.choosePaint(1,preset);check(Arrays.equals(g.paintParts[1],Paints.PRESETS[preset]),"complete composed palette");
        }
        for(int part=0;part<5;part++){g.colorPart(part,Paints.SWATCHES[part]);check(g.paintParts[1][part]==Paints.SWATCHES[part],"independent body-part paint");}
        check(g.paintPreset[1]==5,"custom look marked as custom");
        Game copy=Game.restore(g.snapshot());check(Arrays.equals(g.snapshot(),copy.snapshot()),"victory, checkpoint and custom colors saved exactly");
        g.start();check(g.level==6&&g.stage==1&&g.hp==g.maxHp()&&g.speedBonus==2,"next round starts at next form with milestone bonuses");
        int[] retained=g.paintParts[1].clone();
        for(int loss=1;loss<=4;loss++){
            g.hp=1;g.invincible=0;g.hurt(100);check(!g.active&&g.stage==1&&g.stageLosses==loss&&!g.demoted,"first four failures keep stage");
            g=Game.restore(g.snapshot());g.start();check(g.level==6&&Arrays.equals(g.paintParts[1],retained),"retry retains checkpoint colors");
        }
        g.hp=1;g.invincible=0;g.hurt(100);
        check(g.stage==0&&g.highestStage==0&&g.demoted&&g.stageLosses==0,"fifth failure drops exactly one stage");
        check(g.paintPreset[1]==0&&Arrays.equals(g.paintParts[1],Paints.PRESETS[0]),"lost form paint removed");
        g.start();g.addXp(100000);check(g.stage==1&&g.roundWon&&g.paintPreset[1]==0,"lost form can be earned again without old paint");
        g.start();g.hp=1;g.hurt(100);g.start();g.addXp(100000);check(g.stage==2&&g.stageLosses==0,"success resets failure streak");
        g=new Game();for(int n=0;n<3;n++){g.start();g.hp=1;g.invincible=0;g.hurt(100);}check(g.stage==0&&g.highestStage==0,"first stage never drops below zero");
        g=new Game();allRounds(g);check(g.level==225&&g.stage==44&&g.roundWon&&!g.active,"final round completes without level forty-six");
        int bank=g.bank;g.resetJourney();g.start();check(g.stage==0&&g.level==1&&g.bank==bank,"journey reset retains bank");
        // Version 6 contains NPC dashes and world visitors, but no round or paint extension.
        g=new Game();g.stage=3;g.start();g.xp=17;g.choosePaint(3,4);
        byte[] modern=g.snapshot(),payload=Arrays.copyOf(modern,modern.length-8-234-Game.Codec.extraBytes(g));payload[7]=6;
        java.util.zip.CRC32 crc=new java.util.zip.CRC32();crc.update(payload);ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(payload);new DataOutputStream(out).writeLong(crc.getValue());
        copy=Game.restore(out.toByteArray());check(copy.stage==3&&copy.level==16&&copy.xp==17&&copy.active,"v6 running hero becomes matching round without XP change");
        // A mission completed on the finishing frame must reach the bank before the run closes.
        g=new Game();g.start();g.level=5;g.eaten=8;g.mission=0;g.shells=1;g.addXp(100000);check(g.bank==13,"all eligible finishing-frame missions banked exactly once");
    }
    static void progressionTests()throws Exception{
        near(Game.threshold(1),24,"double length opening level");near(Game.threshold(10),132,"double length later level");
        float lastDanger=0,lastCooldown=0,lastBoss=10,lastEvent=10,lastHeal=100;
        for(int level=1;level<=45;level++){
            check(Difficulty.predators(level)>=lastDanger,"predators grow smoothly");lastDanger=Difficulty.predators(level);
            check(Difficulty.dashCooldown(level)>=lastCooldown,"dash recovery grows smoothly");lastCooldown=Difficulty.dashCooldown(level);
            check(Difficulty.bossWarning(level)<=lastBoss,"boss warning becomes shorter");lastBoss=Difficulty.bossWarning(level);
            check(Difficulty.eventWarning(level)<=lastEvent,"event warning becomes shorter");lastEvent=Difficulty.eventWarning(level);
            check(Difficulty.heal(level)<=lastHeal,"healing decreases smoothly");lastHeal=Difficulty.heal(level);
        }
        near(Difficulty.predators(1),.12f,"easy predator share");near(Difficulty.predators(225),.46f,"hard predator share");
        near(Difficulty.dashCooldown(225),7,"late dash cooldown");check(Difficulty.eventWarning(225)>=1.8f,"late events remain dodgeable");
        Game g=empty();g.level=1;g.stage=Evolution.index(g.level);g.highestStage=g.stage;g.useDash();near(g.cooldown,4,"easy boost");g.cooldown=0;g.level=225;g.stage=Evolution.index(g.level);g.highestStage=g.stage;g.useDash();near(g.cooldown,7,"hard boost");
        g=empty();g.level=5;g.stage=Evolution.index(g.level);g.highestStage=g.stage;g.bossi.wait=g.hazard.wait=1000;Game.Fish f=new Game.Fish(g.x+190,g.y,8,0);f.dashWait=0;g.fish.add(f);
        for(int n=0;n<10;n++)g.update(.02f,0,0,420,850);near(f.warnTime,0,"no enemy dashes on first stage");
        g=empty();g.level=6;g.stage=Evolution.index(g.level);g.highestStage=g.stage;g.bossi.wait=g.hazard.wait=1000;f=new Game.Fish(g.x+190,g.y,9,0);f.dashWait=0;g.fish.add(f);
        g.update(.02f,0,0,420,850);check(f.warnTime>1&&f.dashTime==0,"fish telegraphs before boost");
        float ax=f.aimX,ay=f.aimY;Game copy=Game.restore(g.snapshot());check(Arrays.equals(g.snapshot(),copy.snapshot()),"fish warning saved exactly");
        g.x-=40;for(int n=0;n<70;n++)g.update(.02f,0,0,420,850);
        near(f.aimX,ax,"dash direction does not retarget");near(f.aimY,ay,"dash vertical direction locked");
        for(int type=4;type<=7;type++){
            g=empty();g.items.add(new Game.Item(g.x,g.y,type));g.update(.01f,0,0,420,850);
            check(g.snackSerial==1&&g.xp>0,"each new target is edible");
        }
        g=empty();Game.Item crab=new Game.Item(g.x+100,g.y+100,7);g.items.add(crab);float cx=crab.x,cy=crab.y;
        g.update(.05f,0,0,420,850);float moved=Game.dist(cx,cy,crab.x,crab.y);check(moved>0&&moved<.2f,"crab moves very slowly");
        g=empty();g.nicole.begin(g,420,850);float nx=g.nicole.x,ny=g.nicole.y;check(g.nicole.width<=420,"Nicole fits the visible screen");
        g.x+=600;g.y+=400;g.nicole.update(.3f);near(g.nicole.x,nx,"Nicole independent of hero X");near(g.nicole.y,ny,"Nicole independent of hero Y");
        copy=Game.restore(g.snapshot());check(Arrays.equals(g.snapshot(),copy.snapshot()),"world face and selected audio saved");
        int sounds=0;for(int n=0;n<80;n++){g.nicole.begin(g);sounds|=1<<(g.nicole.audioKind-1);}check(sounds==63,"six visitor sounds available");
        g=empty();g.level=2;g.stage=Evolution.index(g.level);g.highestStage=g.stage;g.bossi.begin(g,Bossi.HEAD_PAWS,420,850);
        check(g.bossi.stage==Bossi.ARRIVE&&g.bossi.audioSerial==1,"surprise cat meows immediately");
        float anchoredY=g.bossi.y;g.y=500;g.bossi.update(g,.05f,420,850);
        check(g.bossi.y==anchoredY,"surprise cat stays anchored after appearing");
        g=empty();g.bank=43;g.decor=2;g.level=18;g.stage=Evolution.index(g.level);g.highestStage=g.stage;g.shells=29;g.resetJourney();g.start();check(g.level==1&&g.shells==0&&g.bank==43&&g.decor==2,"new run retains collection but resets current run");g.stage=Evolution.index(g.level);g.highestStage=g.stage;
        g=empty();long rng=g.seed;g.burst(g.x,g.y,0xff00ffff,240);check(g.seed==rng,"cosmetic RNG cannot change world RNG");
        copy=Game.restore(g.snapshot());g.level=copy.level=3;g.stage=Evolution.index(g.level);g.highestStage=g.stage;g.fish.add(new Game.Fish(g.x,g.y,1,0));copy.fish.add(new Game.Fish(copy.x,copy.y,1,0));
        g.update(.01f,0,0,420,850);copy.update(.01f,0,0,420,850);check(Arrays.equals(g.snapshot(),copy.snapshot()),"full particle pool cannot break deterministic resume");
        g=empty();g.level=6;g.stage=Evolution.index(g.level);g.highestStage=g.stage;g.xp=7;byte[] modern=g.snapshot(),payload=Arrays.copyOf(modern,modern.length-8-Game.Codec.extensionBytes(g));payload[7]=5;
        java.util.zip.CRC32 crc=new java.util.zip.CRC32();crc.update(payload);ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(payload);new DataOutputStream(out).writeLong(crc.getValue());
        copy=Game.restore(out.toByteArray());near(copy.xp,14,"v5 migration retains level completion fraction");check(copy.level==6,"migration retains level");
    }
}
