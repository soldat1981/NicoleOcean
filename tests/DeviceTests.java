package com.nicole.ocean;

import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import java.io.*;
import java.util.*;

/** Runs only in the throwaway emulator; never packaged into the delivered game. */
public final class DeviceTests extends Instrumentation {
    String checkpointMode;
    boolean verify,seedHazard,verifyHazard,seedPaint,verifyPaint;
    @Override public void callActivityOnResume(android.app.Activity activity){
        super.callActivityOnResume(activity);
        // startActivitySync itself waits for an idle UI; freeze animation only in instrumentation.
        if(activity instanceof MainActivity){
            android.view.ViewGroup content=activity.findViewById(android.R.id.content);
            if(content.getChildCount()>0){MainActivity.OceanView view=(MainActivity.OceanView)content.getChildAt(0);view.running=false;}
        }
    }
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);String mode=arguments==null?"":arguments.getString("mode");checkpointMode=mode;verify="verify".equals(mode);seedHazard="seed-hazard".equals(mode);verifyHazard="verify-hazard".equals(mode);seedPaint="seed-paint".equals(mode);verifyPaint="verify-paint".equals(mode);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            progress("started");
            if("reef".equals(checkpointMode)||"verify-reef".equals(checkpointMode)){
                SaveStore saved=new SaveStore(getTargetContext());Game state=saved.load();
                if("verify-reef".equals(checkpointMode)){
                    require(state.home.tier()==10&&state.home.pending==10&&state.home.buildTime==2,"Construction restored after process death");
                    require(state.decor==8&&state.home.robots[0].hp==0&&state.home.robots[0].respawn==21,"Decor and robot repair restored");saved.close();result.putString("result","PASS: v13 home construction, eight decorations and robot repair survive force-stop.");finish(-1,result);return;
                }
                long reefSequence=state.sequence;state=new Game();state.sequence=reefSequence;state.home.victories=1;state.home.pending=1;state.bank=1000;saved.save(state,true);saved.close();
                MainActivity activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                MainActivity.OceanView view=(MainActivity.OceanView)((android.view.ViewGroup)activity.findViewById(android.R.id.content)).getChildAt(0);
                runOnMainSync(()->{require(view.screen==MainActivity.OceanView.REEF,"Cold load reopens pending construction");view.running=false;require(view.homeArt.buildings.hasAlpha()&&view.homeArt.decor.hasAlpha(),"Reef atlases preserve alpha");});
                for(int tier:new int[]{0,1,3,5,7,10}){final int t=tier;runOnMainSync(()->{view.g.home.victories=(1<<t)-1;view.g.home.pending=0;view.g.decor=t==10?8:0;});capture(activity,view,MainActivity.OceanView.REEF,"reef-android-"+tier+".png");}
                runOnMainSync(()->{view.g.home.pending=10;view.g.home.buildTime=2;view.doAction(42);require(view.screen==MainActivity.OceanView.REEF,"Build cannot be skipped early");});
                capture(activity,view,MainActivity.OceanView.REEF,"reef-android-construction.png");
                runOnMainSync(()->{view.g.home.buildTime=5;view.doAction(42);require(view.screen==MainActivity.OceanView.RESULT&&view.g.home.pending==0,"Finished construction opens result");view.screen=MainActivity.OceanView.REEF;view.g.decor=0;view.doAction(41);require(view.g.decor==1&&view.g.bank==988,"Decoration purchase exact");view.g.start();view.g.decor=8;view.g.x=HomeReef.X;view.g.y=HomeReef.Y+120;view.running=false;});
                capture(activity,view,MainActivity.OceanView.PLAY,"reef-android-world.png");
                runOnMainSync(()->{view.g.finish();view.g.home.pending=10;view.g.home.buildTime=2;view.g.home.robots[0].hp=0;view.g.home.robots[0].respawn=21;view.screen=MainActivity.OceanView.REEF;view.persist(true);});
                result.putString("result","PASS: Android reef graphics, progression views, construction gate, purchase and v13 checkpoint.");finish(-1,result);return;
            }
            android.graphics.drawable.Drawable icon=getTargetContext().getPackageManager().getApplicationIcon(getTargetContext().getPackageName());
            require(icon instanceof android.graphics.drawable.AdaptiveIconDrawable,"Nicole face is an adaptive launcher icon");
            android.graphics.Bitmap iconImage=android.graphics.Bitmap.createBitmap(512,512,android.graphics.Bitmap.Config.ARGB_8888);
            icon.setBounds(0,0,512,512);icon.draw(new android.graphics.Canvas(iconImage));
            try(FileOutputStream imageOut=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),"ocean-launcher-icon.png"))){iconImage.compress(android.graphics.Bitmap.CompressFormat.PNG,100,imageOut);}iconImage.recycle();
            if("verify-lethal".equals(checkpointMode)){
                for(int type=1;type<=Hazard.TYPE_COUNT;type++){
                    Game state=new Game();state.stage=state.highestStage=3;state.start();state.x=1100;state.y=1500;
                    Hazard h=state.hazard;h.begin(state,type,420,850);h.stage=2;h.timer=type==Hazard.GEYSER?1.2f:type==Hazard.MINE?4.8f:h.duration()*.5f;
                    state.x=100;state.y=2700;h.update(state,0,420,850);
                    state.x=type==Hazard.JELLY||type==Hazard.MIGRATION?h.elementX(1):h.x;
                    state.y=type==Hazard.JELLY||type==Hazard.MIGRATION?h.elementY(1):h.y;if(type==Hazard.MINE)state.x+=h.blastRadius();
                    state.invincible=100;state.hiddenTime=2;state.calm=true;state.expedition.grant(1);state.expedition.grant(2);
                    h.hit=type%2==0;h.update(state,0,420,850);require(!state.active&&state.hp==0&&state.stageLosses==1,"Instant event death through all bonuses: "+type);
                    SaveStore store=new SaveStore(getTargetContext());state.sequence=store.load().sequence;store.save(state,true);require(!store.failed,"Lethal event saved");store.close();
                    SaveStore check=new SaveStore(getTargetContext());Game loaded=check.load();require(!loaded.active&&loaded.hp==0&&loaded.stageLosses==1,"Death checkpoint restored: "+type);check.close();
                }
                result.putString("result","PASS: All 11 hazards kill instantly and their defeat checkpoints restore on Android.");finish(-1,result);return;
            }
            if("seed-combat".equals(checkpointMode)||"verify-combat".equals(checkpointMode)){
                SaveStore store=new SaveStore(getTargetContext());Game state=store.load();
                if("seed-combat".equals(checkpointMode)){
                    state.stage=state.highestStage=3;state.start();state.x=1000;state.y=1500;state.fish.clear();state.items.clear();
                    Game.Fish enemy=new Game.Fish(1130,1500,5,1);enemy.needleDamage=.4f;enemy.shootWarning=.7f;enemy.shootX=-1;state.fish.add(enemy);
                    Expedition.Shot friendly=new Expedition.Shot(1020,1500,360,0);friendly.level=4;state.expedition.shots.add(friendly);
                    Expedition.Shot hostile=new Expedition.Shot(1100,1500,-220,0);hostile.level=5;state.combat.hostile.add(hostile);
                    store.save(state,true);require(!store.failed,"Combat checkpoint saved");result.putString("result","PASS: Needle damage, telegraph and both projectile teams seeded.");
                }else{
                    require(state.stage==3&&state.level==4&&state.fish.size()==1,"Combat world restored");Game.Fish enemy=state.fish.get(0);
                    require(enemy.needleDamage==.4f&&enemy.shootWarning==.7f&&enemy.shootX==-1,"Damage and locked warning restored");
                    require(state.expedition.shots.size()==1&&state.expedition.shots.get(0).level==4&&state.combat.hostile.size()==1&&state.combat.hostile.get(0).level==5,"Projectile ownership and levels restored");
                    result.putString("result","PASS: Needle damage, telegraph and both projectile teams survived force-stop.");
                }
                store.close();finish(-1,result);return;
            }
            if("seed-expedition".equals(checkpointMode)||"verify-expedition".equals(checkpointMode)){
                SaveStore store=new SaveStore(getTargetContext());Game state=store.load();
                if("seed-expedition".equals(checkpointMode)){
                    state.stage=state.highestStage=59;state.start();state.level=53;state.x=1100;state.y=1500;state.hp=73;
                    state.expedition.begin(state);state.expedition.age=3;state.expedition.grant(3);state.expedition.grant(1);state.choosePaint(52,4);
                    state.expedition.drops.add(new Expedition.Drop(1200,1600,0));state.expedition.shots.add(new Expedition.Shot(1150,1500,360,0));
                    store.save(state,true);require(!store.failed,"Expedition checkpoint saved");
                    result.putString("result","PASS: Final-stage guardian, loot, needle, powers and paint seeded.");
                }else{
                    require(state.stage==59&&state.level==53&&state.hp==73&&state.paintPreset[52]==4,"Final-stage hero and paint survived");
                    require(state.expedition.state==1&&state.expedition.kind==9&&state.expedition.targetLevel==54&&state.expedition.age==3,"Guardian survived exactly");
                    require(state.expedition.powers[3]==15&&state.expedition.powers[1]==15&&state.expedition.drops.size()==1&&state.expedition.shots.size()==1,"Powers, loot and shot survived exactly");
                    result.putString("result","PASS: Final-stage guardian, powers, loot, needle and custom paint survived force-stop.");
                }
                store.close();finish(-1,result);return;
            }
            if(seedPaint||verifyPaint){
                SaveStore storage=new SaveStore(getTargetContext());Game state=storage.load();
                if(seedPaint){state.stage=state.highestStage=2;state.stageLosses=2;state.start();state.choosePaint(2,4);state.colorPart(0,Paints.SWATCHES[3]);storage.save(state,true);require(!storage.failed,"Paint checkpoint saved");result.putString("result","PASS: Painted checkpoint seeded.");}
                else{require(state.active&&state.stage==2&&state.highestStage==2&&state.level==3&&state.stageLosses==2,"Round checkpoint and two failures survive process death");require(state.paintPreset[2]==5&&state.paintParts[2][0]==Paints.SWATCHES[3]&&state.paintParts[2][1]==Paints.PRESETS[4][1],"Custom head and remaining composed colors restored");result.putString("result","PASS: Round, failure streak and custom multi-part paint survived force-stop.");}
                finish(-1,result);return;
            }
            if(seedHazard||verifyHazard){
                SaveStore storage=new SaveStore(getTargetContext());Game state=storage.load();
                if(seedHazard){
                    state.bossi.kind=state.bossi.stage=0;state.nicole.visible=false;
                    state.hazard.begin(state,Hazard.ANCHOR,420,850);state.hazard.stage=2;state.hazard.timer=1.2f;
                    state.hazard.update(state,0,420,850);storage.save(state,true);
                    require(!storage.failed,"Active anchor saved");result.putString("result","PASS: Active anchor seeded for process-death check.");
                }else{
                    require(state.active&&state.level==6&&state.hp==67&&state.shells==17,"Hero preserved during anchor");
                    require(state.hazard.kind==Hazard.ANCHOR&&state.hazard.stage==2&&state.hazard.timer==1.2f,"Falling anchor resumes exact stage and time");
                    require(!state.bossi.visible()&&!state.nicole.visible,"No visitors restored over anchor");
                    result.putString("result","PASS: After force-stop falling anchor and hero restored exactly.");
                }
                finish(-1,result);return;
            }
            if(verify){
                Game saved=new SaveStore(getTargetContext()).load();
                require(saved.active&&saved.level==6&&saved.hp==67&&saved.x==1177.25f&&saved.y==1234.5f&&saved.shells==17,"Process death preserved exact hero and rewards");
                require(saved.bossi.kind==Bossi.HEAD_PAWS&&saved.bossi.stage==Bossi.AIM&&saved.bossi.timer==.4f&&saved.bossi.attempts==1,"Interrupted cat visit restored exactly");
                result.putString("result","PASS: After force-stop exact hero, rewards and interrupted Bossi visit survived.");finish(-1,result);return;
            }
            SaveStore store=new SaveStore(getTargetContext());Game g=new Game();g.sequence=store.load().sequence;g.start();
            g.x=1177.25f;g.y=1234.5f;g.level=6;g.stage=g.highestStage=1;g.hp=67;g.xp=23;g.bank=42;g.shells=17;g.time=99;g.mission=2;
            g.tutorialSeen=true;g.pendingUpgrade=0;g.form=0;g.decor=2;g.unlocked=3;g.bestLevel=6;
            g.fish.clear();g.fish.add(new Game.Fish(1040,1100,3,0));g.fish.add(new Game.Fish(1305,1080,8,1));g.fish.add(new Game.Fish(1250,1370,4,2));g.fish.add(new Game.Fish(1015,1390,6,3));
            for(int j=0;j<12;j++){Game.Fish f=new Game.Fish(g.x-175+(j%4)*111,g.y-310+(j/4)*260,2+j%4,j%4);f.vx=j%2==0?1:-1;g.fish.add(f);}
            for(int j=0;j<4;j++)g.items.set(j,new Game.Item(g.x-110+j*70,g.y+135,4+j));
            store.save(g,true);byte[] before=g.snapshot();Game loaded=new SaveStore(getTargetContext()).load();
            require(Arrays.equals(before,loaded.snapshot()),"Exact Android disk roundtrip");
            store.save(g,true);long latest=g.sequence;
            File slot=new File(getTargetContext().getFilesDir(),(latest&1)==0?"ocean-a.save":"ocean-b.save");
            try(RandomAccessFile file=new RandomAccessFile(slot,"rw")){file.seek(70);file.writeByte(0xff);}
            SaveStore fallback=new SaveStore(getTargetContext());loaded=fallback.load();
            require(fallback.recovered&&loaded.sequence==latest-1,"Corrupt slot falls back to previous snapshot");
            store.save(g,true);store.save(g,true);require(!store.failed,"Atomic write succeeds");
            for(int n=0;n<100;n++){g.shells=n;store.save(g,false);}
            g.shells=17;store.save(g,true);
            require(Arrays.equals(g.snapshot(),new SaveStore(getTargetContext()).load().snapshot()),"Coalesced writes retain the newest exact snapshot");
            store.close();
            progress("disk checks and 100 queued saves passed; launching activity");
            MainActivity activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            progress("activity launched");
            runOnMainSync(()->{
                MainActivity.OceanView view=(MainActivity.OceanView)((android.view.ViewGroup)activity.findViewById(android.R.id.content)).getChildAt(0);
                require(view.screen==MainActivity.OceanView.PAUSE,"Cold load starts paused");
                require(view.g.level==6&&view.g.x==1177.25f&&view.g.hp==67,"Activity restored exact hero");
                view.screen=MainActivity.OceanView.PAUSE;view.g.pendingUpgrade=0;view.running=false;view.persist(true);view.invalidate();
            });
            MainActivity.OceanView view=(MainActivity.OceanView)((android.view.ViewGroup)activity.findViewById(android.R.id.content)).getChildAt(0);
            runOnMainSync(()->{
                StringBuilder issues=new StringBuilder();java.util.HashSet<Integer> unique=new java.util.HashSet<>();
                for(int tier=0;tier<Evolution.STAGES;tier++){
                    int[] colors=Paints.PRESETS[0].clone();int base=artHash(view,tier,colors);
                    if(!unique.add(base))issues.append("duplicate ").append(tier).append("; ");
                    for(int part=0;part<5;part++){int old=colors[part];colors[part]=Paints.SWATCHES[11];if(artHash(view,tier,colors)==base)issues.append("invisible ").append(tier).append("/").append(part).append("; ");colors[part]=old;}
                }
                require(issues.length()==0,"Form preflight: "+issues);
            });
            capture(activity,view,MainActivity.OceanView.HOME,"ocean-home.png");
            capture(activity,view,MainActivity.OceanView.PAUSE,"ocean-pause.png");
            capture(activity,view,MainActivity.OceanView.TUTORIAL,"ocean-tutorial.png");
            runOnMainSync(()->{view.g.y=2600;view.g.hp=18;});
            capture(activity,view,MainActivity.OceanView.PLAY,"ocean-deep-low-health.png");
            runOnMainSync(()->{view.g.y=1234.5f;view.g.hp=67;});
            // Draw an active world without advancing its simulation during visual QA.
            runOnMainSync(()->{view.running=false;});
            capture(activity,view,MainActivity.OceanView.PLAY,"ocean-gameplay.png");
            runOnMainSync(()->{view.g.heading=-(float)Math.PI/4;});
            capture(activity,view,MainActivity.OceanView.PLAY,"ocean-turning.png");
            runOnMainSync(()->{view.g.heading=0;});
            runOnMainSync(()->{
                float sc=view.getWidth()/420f,yy=(view.h-82)*sc+view.padTop;
                long now=android.os.SystemClock.uptimeMillis();
                android.view.MotionEvent down=android.view.MotionEvent.obtain(now,now,0,90*sc,yy,0);
                android.view.MotionEvent move=android.view.MotionEvent.obtain(now,now+16,2,130*sc,yy,0);
                android.view.MotionEvent up=android.view.MotionEvent.obtain(now,now+32,1,130*sc,yy,0);
                view.onTouchEvent(down);view.onTouchEvent(move);require(view.inputX>.9f,"Android joystick moves right");view.onTouchEvent(up);require(view.inputX==0&&view.stickId==-1,"Joystick releases");
                down.recycle();move.recycle();up.recycle();
                view.moveStick(view.stickX+70,view.stickY);require(!view.boostRequested,"Inside boost threshold does not accelerate");
                view.moveStick(view.stickX+74,view.stickY);require(view.boostRequested,"Outside joystick circle requests boost");
                view.moveStick(view.stickX+66,view.stickY);require(view.boostRequested,"Hysteresis prevents edge flicker");
                view.moveStick(view.stickX+60,view.stickY);require(!view.boostRequested,"Return into circle cancels repeated boost");view.clearInput();
            });
            capture(activity,view,MainActivity.OceanView.SETTINGS,"ocean-settings.png");
            progress("base screenshots captured; checking audio");
            runOnMainSync(()->{
                OceanMusic music=view.music;music.remaining=0;music.last=-1;
                int seen=0,prior=-1;
                for(int n=0;n<100;n++){int chosen=music.choose();require(chosen>=0&&chosen<10&&chosen!=prior,"Music shuffle does not repeat adjacent tracks");if(n<10){require((seen&(1<<chosen))==0,"First shuffle plays each track once");seen|=1<<chosen;}prior=chosen;}
                require(seen==1023,"All ten themes selected");
                for(int n=1;n<=10;n++){
                    try(android.content.res.AssetFileDescriptor fd=getTargetContext().getAssets().openFd(String.format(java.util.Locale.ROOT,"audio/music/ocean_%02d.wav",n))){
                        android.media.MediaPlayer decoder=new android.media.MediaPlayer();
                        try{decoder.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());decoder.prepare();require(decoder.getDuration()>60000&&decoder.getDuration()<85000,"Music PCM decodes with expected length");}finally{decoder.release();}
                    }catch(Exception ex){throw new AssertionError("Music asset "+n,ex);}
                }
                music.enabled=true;music.userResume();music.next();music.update(true,false);
                });
            android.os.SystemClock.sleep(700);
            runOnMainSync(()->{
                OceanMusic music=view.music;
                require(music.player!=null&&!music.preparing&&music.player.isPlaying(),"Music starts with gameplay");
                view.g.sound=true;view.doAction(13);require(!view.g.sound&&music.player.isPlaying(),"Effects switch leaves music playing independently");view.g.sound=true;
                for(int n=0;n<100;n++)music.update(true,false);float normal=music.volume;
                for(int n=0;n<20;n++)music.update(true,true);
                require(music.volume<normal*.4f,"Music ducks during visitor dialogue");
                music.update(false,false);require(!music.player.isPlaying()&&!music.hasFocus,"Pause stops music and releases audio focus");
                music.toggle();require(!music.enabled,"Separate music setting can disable soundtrack");music.update(true,false);require(!music.player.isPlaying(),"Disabled music stays silent");music.toggle();
                for(int i=1;i<=10;i++){require(view.bossiAudio.play(i),"Bundled visitor audio decodes: "+i);view.bossiAudio.stop();}
                for(int i=0;i<3;i++){require(view.bossiAudio.play(1),"Meow variation decodes");view.bossiAudio.stop();}
                require(view.bossiArt.atlas.hasAlpha(),"Cat atlas supports transparency");
                require(view.gameAudio.loaded==8,"All eight Foley clips decoded by Android SoundPool");
                for(int n=0;n<8;n++)view.gameAudio.play(n,1);
                view.screen=MainActivity.OceanView.PLAY;
                music.focusChanged(android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT);
                require(view.screen==MainActivity.OceanView.PAUSE&&view.bossiAudio.player==null&&view.inputX==0,"Audio interruption pauses game and dialogue");
                music.userResume();view.screen=MainActivity.OceanView.PLAY;
            });
            progress("audio decoded; capturing cat variants");
            for(int variant=1;variant<=5;variant++){
                final int kind=variant;
                runOnMainSync(()->{
                    view.g.bossi.begin(view.g,kind,view.w,view.h);
                    view.g.bossi.stage=kind==Bossi.TAIL?Bossi.HOLD_TAIL:Bossi.STRIKE;
                    view.g.bossi.timer=kind==Bossi.TAIL?3:.48f*.45f;
                    view.g.bossi.targetX=view.g.x+60;view.g.bossi.targetY=view.g.y-40;
                });
                capture(activity,view,MainActivity.OceanView.PLAY,"ocean-bossi-"+variant+".png");
            }
            final float visitorHeroX=view.g.x,visitorHeroY=view.g.y;
            runOnMainSync(()->{
                view.g.bossi.begin(view.g,Bossi.HEAD,view.w,view.h);view.g.bossi.stage=Bossi.AIM;
                view.g.bossi.timer=view.g.bossi.warning*.65f;view.g.bossi.targetX=view.g.x+60;view.g.bossi.targetY=view.g.y;
            });
            capture(activity,view,MainActivity.OceanView.PLAY,"ocean-bossi-diagonal-warning.png");
            runOnMainSync(()->{view.g.bossi.stage=Bossi.STRIKE;view.g.bossi.timer=.48f*.225f;});
            capture(activity,view,MainActivity.OceanView.PLAY,"ocean-bossi-diagonal-mid.png");
            final float anchoredX=view.g.bossi.x,anchoredY=view.g.bossi.y;
            runOnMainSync(()->{view.g.x+=650;view.g.y+=650;view.g.bossi.update(view.g,.01f,view.w,view.h);
                require(view.g.bossi.x==anchoredX&&view.g.bossi.y==anchoredY,"Swimming away leaves cat at fixed world anchor");});
            capture(activity,view,MainActivity.OceanView.PLAY,"ocean-bossi-left-behind.png");
            runOnMainSync(()->{view.g.x=visitorHeroX;view.g.y=visitorHeroY;});
            runOnMainSync(()->{view.g.bossi.begin(view.g,Bossi.HEAD,view.w,view.h);view.g.bossi.stage=Bossi.CALL;view.g.bossi.timer=.5f;});
            capture(activity,view,MainActivity.OceanView.PLAY,"ocean-bossi-peek.png");
            runOnMainSync(()->{view.g.bossi.begin(view.g,Bossi.HEAD_PAWS,view.w,view.h);view.g.bossi.timer=2;});
            capture(activity,view,MainActivity.OceanView.PLAY,"ocean-bossi-before-appearance.png");
            runOnMainSync(()->{
                view.g.bossi.kind=Bossi.NONE;view.g.bossi.stage=0;view.g.bossi.wait=95;view.g.messageTime=0;
            });
            progress("capturing Nicole backgrounds and evolved characters");
            for(int variant=0;variant<3;variant++){
                final int selected=variant;
                runOnMainSync(()->{
                    view.g.nicole.begin(view.g);view.g.nicole.face=selected;view.g.nicole.gear=selected;view.g.nicole.time=4;
                    require(!view.g.bossi.visible(),"cat absent during Nicole background");
                });
                capture(activity,view,MainActivity.OceanView.PLAY,"ocean-nicole-"+variant+".png");
            }
            final float fixedFaceX=view.g.nicole.x;
            runOnMainSync(()->{view.g.x-=800;require(view.g.nicole.x==fixedFaceX,"Swimming away does not drag Nicole");});
            capture(activity,view,MainActivity.OceanView.PLAY,"ocean-nicole-left-behind.png");
            runOnMainSync(()->{view.g.x=1177.25f;});
            runOnMainSync(()->{view.g.nicole.visible=false;});
            for(int tier=0;tier<Evolution.NAMES.length;tier++){
                final int selected=tier;
                runOnMainSync(()->{view.g.level=selected+1;});
                capture(activity,view,MainActivity.OceanView.PLAY,"ocean-hero-"+tier+".png");
            }
            runOnMainSync(()->{
                android.graphics.Bitmap sheet=android.graphics.Bitmap.createBitmap(1680,1200,android.graphics.Bitmap.Config.ARGB_8888);
                android.graphics.Canvas cc=new android.graphics.Canvas(sheet);cc.drawColor(0xff103b51);
                java.util.HashSet<Integer> hashes=new java.util.HashSet<>();
                for(int tier=0;tier<Evolution.STAGES;tier++){
                    require(hashes.add(artHash(view,tier,Paints.PRESETS[0])),"Every form has distinct pixels "+tier);
                    float xx=120+(tier%7)*240,yy=65+(tier/7)*150;
                    view.previewHero(cc,xx,yy,44,tier,Paints.PRESETS[tier%5]);view.text(cc,(tier+1)+". "+Evolution.NAMES[tier],xx,yy+65,13,0xffddfff6,true,true);
                }
                try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),"ocean-all-56-forms.png"))){sheet.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}catch(Exception ex){throw new RuntimeException(ex);}sheet.recycle();
            });
            runOnMainSync(()->{view.g.level=6;});
            final Game beforePaint=Game.restore(view.g.snapshot());
            runOnMainSync(()->{
                // Render the actual game art and verify that every editable part changes pixels.
                for(int tier=0;tier<Evolution.STAGES;tier++){
                    int[] colors=Paints.PRESETS[0].clone();int baseline=artHash(view,tier,colors);
                    for(int part=0;part<5;part++){int old=colors[part];colors[part]=Paints.SWATCHES[11];require(artHash(view,tier,colors)!=baseline,"Editable part visible: tier "+tier+" part "+part);colors[part]=old;}
                }
                view.g.stage=1;view.g.level=4;view.g.xp=0;view.g.addXp(Game.threshold(4));require(view.g.roundWon&&view.g.stage==2,"Android round victory unlocks next stage");
            });
            capture(activity,view,MainActivity.OceanView.RESULT,"ocean-round-victory.png");
            for(int preset=0;preset<5;preset++){
                final int selected=preset;runOnMainSync(()->{view.screen=MainActivity.OceanView.PAINT;view.doAction(100+selected);});
                capture(activity,view,MainActivity.OceanView.PAINT,"ocean-paint-"+preset+".png");
            }
            runOnMainSync(()->{
                for(int part=0;part<5;part++){view.doAction(200+part);view.doAction(300+part);}
                require(view.g.paintPreset[2]==5,"Android custom paint selected");
            });
            capture(activity,view,MainActivity.OceanView.PAINT,"ocean-paint-custom.png");
            runOnMainSync(()->{
                view.doAction(31);require(view.g.active&&view.g.level==3&&view.g.stage==2,"Next painted round starts at three");
                beforePaint.sequence=view.g.sequence;view.g=beforePaint;view.heardCatSerial=view.g.bossi.audioSerial;view.heardNicoleSerial=view.g.nicole.audioSerial;view.persist(true);
            });
            runOnMainSync(()->{view.g.level=12;Game.Fish f=view.g.fish.get(0);f.warnTime=.8f;f.aimX=.8f;f.aimY=.6f;});
            capture(activity,view,MainActivity.OceanView.PLAY,"ocean-fish-warning.png");
            runOnMainSync(()->{Game.Fish f=view.g.fish.get(0);f.warnTime=0;f.dashTime=.3f;});
            capture(activity,view,MainActivity.OceanView.PLAY,"ocean-fish-dash.png");
            runOnMainSync(()->{view.g.level=6;view.g.fish.get(0).dashTime=0;});
            for(int type=1;type<=Hazard.TYPE_COUNT;type++){
                final int selected=type;
                runOnMainSync(()->{view.g.bossi.kind=0;view.g.nicole.visible=false;view.g.hazard.begin(view.g,selected,view.w,view.h);view.g.messageTime=0;});
                capture(activity,view,MainActivity.OceanView.PLAY,"ocean-hazard-warning-"+type+".png");
                runOnMainSync(()->{
                    Hazard a=view.g.hazard;a.stage=2;a.timer=a.duration()*.5f;float px=view.g.x,py=view.g.y;view.g.x=100;view.g.y=2700;a.update(view.g,0,view.w,view.h);view.g.x=px;view.g.y=py;
                });
                capture(activity,view,MainActivity.OceanView.PLAY,"ocean-hazard-"+type+".png");
                runOnMainSync(()->{view.g.hazard.kind=view.g.hazard.stage=0;view.g.hp=67;view.g.x=1177.25f;view.g.y=1234.5f;view.g.invincible=0;});
            }
            runOnMainSync(()->{view.g.hazard.begin(view.g,Hazard.MINE,view.w,view.h);view.g.hazard.stage=2;view.g.hazard.timer=5;float px=view.g.x,py=view.g.y;view.g.x=100;view.g.y=2700;view.g.hazard.update(view.g,0,view.w,view.h);view.g.x=px;view.g.y=py;});
            capture(activity,view,MainActivity.OceanView.PLAY,"ocean-mine-explosion.png");
            runOnMainSync(()->{view.g.hazard.kind=view.g.hazard.stage=0;view.g.hp=67;view.g.invincible=0;});
            runOnMainSync(()->{view.g.sparks.clear();view.g.particles.clear();view.g.combo=5;view.g.comboTime=3;view.g.biteGlow=.15f;view.g.levelGlow=.65f;view.g.burst(view.g.x,view.g.y,0xff92ffdc,30);
                for(Game.Particle pt:view.g.particles){pt.x+=pt.vx*.3f;pt.y+=pt.vy*.3f;pt.life*=.7f;}
            });
            capture(activity,view,MainActivity.OceanView.PLAY,"ocean-effects.png");
            runOnMainSync(()->{view.g.combo=0;view.g.comboTime=0;view.g.biteGlow=view.g.levelGlow=0;view.g.particles.clear();});
            final Game beforeGuardians=Game.restore(view.g.snapshot());
            for(int guardian=0;guardian<10;guardian++){
                final int selected=guardian;
                runOnMainSync(()->{view.g=new Game();view.g.stage=view.g.highestStage=selected*6+5;view.g.start();view.g.x=1100;view.g.y=1500;view.g.time=60;view.g.fish.clear();view.g.items.clear();view.g.expedition.begin(view.g);view.g.expedition.x=view.g.x+80;view.g.expedition.y=view.g.y-180;view.g.expedition.age=3;for(int n=0;n<5;n++)view.g.expedition.grant(n);});
                capture(activity,view,MainActivity.OceanView.PLAY,"ocean-guardian-"+guardian+".png");
                runOnMainSync(()->{view.g.expedition.state=2;view.g.expedition.celebrate=2.5f;});
                capture(activity,view,MainActivity.OceanView.PLAY,"ocean-celebration-"+guardian+".png");
            }
            runOnMainSync(()->{view.g=beforeGuardians;view.g.expedition.reset();});
            final Game beforeCombat=Game.restore(view.g.snapshot());
            runOnMainSync(()->{
                view.g=new Game();view.g.stage=view.g.highestStage=3;view.g.start();view.g.x=1000;view.g.y=1500;view.g.time=30;view.g.fish.clear();view.g.items.clear();view.g.messageTime=0;
                Game.Fish enemy=new Game.Fish(1130,1500,5,1);enemy.needleDamage=.4f;enemy.shootWait=0;view.g.fish.add(enemy);
                view.g.combat.update(view.g,.01f,view.w,view.h,view.g.x,view.g.y);require(enemy.shootWarning>1,"Android shooter warns before firing");
            });
            capture(activity,view,MainActivity.OceanView.PLAY,"ocean-needle-warning.png");
            runOnMainSync(()->{
                view.g.y+=80;for(int n=0;n<36;n++)view.g.combat.update(view.g,.05f,view.w,view.h,view.g.x,view.g.y);
                require(view.g.combat.hostile.size()==1,"Android warned projectile in flight");
                Expedition.Shot shot=new Expedition.Shot(1040,1540,300,-120);shot.level=4;view.g.expedition.shots.add(shot);
            });
            capture(activity,view,MainActivity.OceanView.PLAY,"ocean-needle-projectiles.png");
            runOnMainSync(()->{view.g=beforeCombat;});
            layoutAndInputChecks(view);
            final Game beforeDensity=Game.restore(view.g.snapshot());
            runOnMainSync(()->{view.g=new Game();view.g.start();view.g.messageTime=0;});
            capture(activity,view,MainActivity.OceanView.PLAY,"ocean-natural-opening.png");
            runOnMainSync(()->{view.g=beforeDensity;});
            long initialFrames=view.renderedFrames;float initialTime=view.g.time;
            runOnMainSync(()->{view.screen=MainActivity.OceanView.PLAY;view.g.hazard.wait=100;view.g.bossi.wait=100;view.g.expedition.state=2;view.g.invincible=100;
                view.g.cooldown=0;view.stickX=90;view.stickY=view.h-96;view.moveStick(164,view.stickY);view.running=true;view.previous=0;view.invalidate();});
            android.os.SystemClock.sleep(3000);
            runOnMainSync(()->{view.running=false;require(view.screen==MainActivity.OceanView.PLAY,"Continuous run stays in gameplay");require(view.g.time-initialTime>2.3f,"Simulation follows elapsed time despite slow rendering");require(view.g.cooldown>0,"Joystick boost activates in real frame loop");view.clearInput();});
            require(view.renderedFrames-initialFrames>20,"Continuous Android frame loop makes progress without crash");
            progress("continuous render passed: "+(view.renderedFrames-initialFrames)+" frames in 3 seconds");
            runOnMainSync(()->{view.g.x=1177.25f;view.g.y=1234.5f;view.g.level=6;view.g.xp=23;view.g.hp=67;view.g.shells=17;view.g.invincible=0;});
            runOnMainSync(()->{
                float sc=view.getWidth()/420f,yy=view.h*.45f*sc+view.padTop;view.screen=MainActivity.OceanView.PLAY;
                long now=android.os.SystemClock.uptimeMillis();
                android.view.MotionEvent down=android.view.MotionEvent.obtain(now,now,0,90*sc,yy,0);
                view.onTouchEvent(down);require(view.stickId>=0,"Joystick starts in enlarged upper area");view.clearInput();down.recycle();
            });
            runOnMainSync(()->{
                view.g.bossi.begin(view.g,Bossi.HEAD_PAWS,view.w,view.h);view.g.bossi.stage=Bossi.AIM;view.g.bossi.timer=.4f;view.g.bossi.attempts=1;
                view.screen=MainActivity.OceanView.PAUSE;view.persist(true);
            });
            result.putString("result","PASS: Android atomic disk save, corrupt-slot recovery, cold-load pause and exact hero restoration.");finish(-1,result);
        }catch(Throwable error){result.putString("error",error.toString());finish(0,result);}
    }
    void layoutAndInputChecks(MainActivity.OceanView view){
        runOnMainSync(()->{
            int oldW=view.getWidth(),oldH=view.getHeight();float top=view.padTop,bottom=view.padBottom;
            view.running=false;view.padTop=view.padBottom=0;
            for(int height:new int[]{480,640,900,1100}){
                view.layout(0,0,420,height);
                android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(420,height,android.graphics.Bitmap.Config.ARGB_8888);
                for(int screen=0;screen<=6;screen++){
                    view.screen=screen;view.onDraw(new android.graphics.Canvas(bitmap));
                    for(MainActivity.OceanView.Button button:view.buttons){
                        require(button.r.left>=0&&button.r.right<=420&&button.r.top>=0&&button.r.bottom<=view.h,"Every control remains inside scrollable content: screen "+screen+" height "+height);
                        require(button.r.width()>=44&&button.r.height()>=44,"Touch controls are at least 44 logical pixels");
                    }
                    for(int i=0;i<view.buttons.size();i++)for(int j=i+1;j<view.buttons.size();j++)
                        require(!android.graphics.RectF.intersects(view.buttons.get(i).r,view.buttons.get(j).r),"Interactive controls never overlap");
                }
                bitmap.recycle();
            }
            view.layout(0,0,420,480);view.screen=MainActivity.OceanView.SETTINGS;
            android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(420,480,android.graphics.Bitmap.Config.ARGB_8888);
            view.onDraw(new android.graphics.Canvas(image));boolean calm=view.g.calm;
            touch(view,0,100,420);touch(view,2,100,110);touch(view,1,100,110);
            require(view.menuScroll>200&&view.g.calm==calm,"Swipe scrolls a short menu without activating settings");
            view.screen=MainActivity.OceanView.HOME;view.onDraw(new android.graphics.Canvas(image));
            touch(view,0,180,455);require(view.screen==MainActivity.OceanView.HOME,"Press does not trigger navigation early");
            touch(view,3,180,455);require(view.screen==MainActivity.OceanView.HOME,"Cancelled touch cannot activate a button");
            touch(view,0,180,455);touch(view,1,180,455);require(view.screen==MainActivity.OceanView.PAUSE,"Release activates the original button");
            view.screen=MainActivity.OceanView.PLAY;view.onDraw(new android.graphics.Canvas(image));
            touch(view,0,90,380);touch(view,2,135,380);require(view.inputX>0,"Joystick moves before cancellation");
            touch(view,3,135,380);require(view.inputX==0&&!view.boostRequested&&view.stickId<0,"Cancellation clears every movement state");
            image.recycle();view.layout(0,0,oldW,oldH);view.padTop=top;view.padBottom=bottom;view.screen=MainActivity.OceanView.PLAY;view.layoutScreen=-1;view.menuScroll=0;view.invalidate();
        });
        progress("28 screen/size layouts, touch targets, scrolling, release and cancellation passed");
    }
    static void touch(MainActivity.OceanView view,int action,float x,float y){
        long now=android.os.SystemClock.uptimeMillis();android.view.MotionEvent event=android.view.MotionEvent.obtain(now,now,action,x*view.scale,y*view.scale+view.padTop,0);
        view.onTouchEvent(event);event.recycle();
    }
    static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static int artHash(MainActivity.OceanView view,int tier,int[] colors){
        android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(220,180,android.graphics.Bitmap.Config.ARGB_8888);
        view.previewHero(new android.graphics.Canvas(image),110,90,45,tier,colors);int[] pixels=new int[220*180];image.getPixels(pixels,0,220,0,0,220,180);image.recycle();return Arrays.hashCode(pixels);
    }
    void progress(String text){android.util.Log.i("OceanTest",text);Bundle state=new Bundle();state.putString("step",text);sendStatus(1,state);}
    void capture(MainActivity activity,MainActivity.OceanView view,int screen,String name)throws Exception{
        final long[] drawn={0};runOnMainSync(()->{drawn[0]=view.renderedFrames;view.screen=screen;view.invalidate();});
        for(int n=0;n<30;n++){android.os.SystemClock.sleep(100);final boolean[] ready={false};runOnMainSync(()->ready[0]=view.renderedFrames>drawn[0]);if(ready[0])break;}
        waitForIdleSync();android.os.SystemClock.sleep(700);
        android.graphics.Bitmap bitmap=getUiAutomation().takeScreenshot();require(bitmap!=null,"Screenshot available");
        File file=new File(getTargetContext().getExternalFilesDir(null),name);
        try(FileOutputStream out=new FileOutputStream(file)){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
    }
}
