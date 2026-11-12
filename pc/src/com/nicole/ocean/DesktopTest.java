package com.nicole.ocean;
import java.awt.event.*;import java.awt.image.BufferedImage;import java.nio.file.*;import java.util.*;import javax.imageio.ImageIO;import javax.swing.*;
/** Executed by both the JDK build and the packaged runtime. Never touches the player's profile. */
public final class DesktopTest {
    static int checks;static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static void render(MainActivity.OceanView v,Path file)throws Exception{android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(v.getWidth(),v.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);try(android.graphics.Canvas c=new android.graphics.Canvas(bitmap)){v.onDraw(c);}ImageIO.write(bitmap.image,"png",file.toFile());check(Files.size(file)>10000,"Rendered "+file);}
    public static void run(String output)throws Exception{
        Path out=Path.of(output).toAbsolutePath();Files.createDirectories(out);System.setProperty("ocean.mute","true");System.setProperty("ocean.saveDir",Files.createTempDirectory("ocean-desktop-test-").toString());
        final Throwable[] error={null};SwingUtilities.invokeAndWait(()->{MainActivity.OceanView v=null;try{
            v=new MainActivity.OceanView(new MainActivity());v.setSize(504,960);v.running=false;render(v,out.resolve("home.png"));v.newRun();v.running=false;render(v,out.resolve("game.png"));
            DesktopLauncher.Controls c=new DesktopLauncher.Controls(v,null);c.keys.add(KeyEvent.VK_D);c.keys.add(KeyEvent.VK_W);c.apply();check(Math.abs(v.inputX-.7071068f)<.0001&&v.inputY<0,"Normalized diagonal keyboard input");c.keys.clear();c.apply();check(v.inputX==0&&v.inputY==0,"Release stops movement");
            c.keys.add(KeyEvent.VK_SPACE);c.apply();check(v.boostRequested,"Space boost");c.clear();check(!v.boostRequested&&v.inputX==0,"Focus loss clears input");
            // Exercise the actual View mouse bridge at non-unit scale, including the upper/right field.
            int mx=(int)(330*v.scale),my=(int)(220*v.scale);
            v.dispatchEvent(new MouseEvent(v,MouseEvent.MOUSE_PRESSED,0,MouseEvent.BUTTON1_DOWN_MASK,mx,my,1,false,MouseEvent.BUTTON1));
            check(c.mouseHeld&&v.inputX>0&&v.inputY<0,"Mouse steers toward upper-right cursor");
            float beforeX=v.g.x,beforeY=v.g.y;v.g.update(FrameStepper.STEP,v.inputX,v.inputY,v.w,v.h);check(v.g.x>beforeX&&v.g.y<beforeY,"Mouse input moves hero");
            c.keys.add(KeyEvent.VK_SPACE);c.apply();check(v.boostRequested&&v.inputX>0,"Boost preserves mouse direction");c.keys.clear();
            c.keys.add(KeyEvent.VK_A);c.apply();check(v.inputX==-1&&v.inputY==0,"Keyboard overrides held mouse");c.keys.clear();c.apply();check(v.inputX>0,"Mouse resumes after keyboard release");
            v.dispatchEvent(new MouseEvent(v,MouseEvent.MOUSE_DRAGGED,0,MouseEvent.BUTTON1_DOWN_MASK,(int)(80*v.scale),(int)(500*v.scale),0,false,MouseEvent.NOBUTTON));check(v.inputX<0&&v.inputY>0,"Dragging updates direction");
            v.dispatchEvent(new MouseEvent(v,MouseEvent.MOUSE_RELEASED,0,0,-10,-10,1,false,MouseEvent.BUTTON1));check(!c.mouseHeld&&v.inputX==0&&v.inputY==0,"Release outside field stops mouse movement");
            v.dispatchEvent(new MouseEvent(v,MouseEvent.MOUSE_PRESSED,0,MouseEvent.BUTTON3_DOWN_MASK,mx,my,1,false,MouseEvent.BUTTON3));check(!c.mouseHeld,"Right button does not steer");
            c.pointer(new android.view.MotionEvent(0,(v.g.x-v.cameraX)*v.scale,(v.g.y-v.cameraY)*v.scale));c.apply();check(v.inputX==0&&v.inputY==0,"Cursor near hero has stable dead zone");
            c.pointer(new android.view.MotionEvent(0,mx,my));v.pauseGame();check(!c.mouseHeld&&v.inputX==0,"Pause cancels held mouse");v.doAction(7);c.apply();check(v.inputX==0&&v.inputY==0,"Resume does not revive stale mouse input");
            render(v,out.resolve("mouse-controls.png"));MainActivity.OceanView.Button pause=null;for(MainActivity.OceanView.Button b:v.buttons)if(b.action==5)pause=b;
            check(pause!=null,"Pause button exists");float px=(pause.r.left+pause.r.right)/2*v.scale,py=(pause.r.top+pause.r.bottom)/2*v.scale;
            v.onTouchEvent(new android.view.MotionEvent(0,px,py));check(!c.mouseHeld,"HUD press is not movement");v.onTouchEvent(new android.view.MotionEvent(1,px,py));check(v.screen==MainActivity.OceanView.PAUSE,"Mouse pause button still works");v.doAction(7);
            c.dispatchKeyEvent(new KeyEvent(v,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_ESCAPE,(char)27));check(v.screen==MainActivity.OceanView.PAUSE,"Escape pauses");render(v,out.resolve("pause.png"));
            c.dispatchKeyEvent(new KeyEvent(v,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_ESCAPE,(char)27));check(v.screen==MainActivity.OceanView.PAUSE,"Held Escape does not close menus repeatedly");
            c.dispatchKeyEvent(new KeyEvent(v,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_ENTER,'\n'));check(v.screen==MainActivity.OceanView.PLAY,"Enter resumes");
            for(int s:new int[]{0,2,3,4,5,6}){v.screen=s;render(v,out.resolve("screen-"+s+".png"));}
            v.screen=1;for(int level=1;level<=55;level++){v.g.level=level;render(v,out.resolve(String.format(Locale.ROOT,"form-%02d.png",level)));}
            v.g.level=7;v.persist(true);Game loaded=new SaveStoreTest(v.activity).read();check(loaded.level==7&&loaded.active,"Save/load restores active game");
            v.g.x=1000;v.g.y=1500;
            for(int kind=1;kind<=5;kind++){v.g.bossi.begin(v.g,kind,420,800);v.g.bossi.stage=kind==5?Bossi.HOLD_TAIL:Bossi.STRIKE;v.g.bossi.timer=.2f;render(v,out.resolve("bossi-"+kind+".png"));}v.g.bossi.kind=0;
            for(int face=0;face<3;face++){v.g.nicole.begin(v.g,420,800);v.g.nicole.face=face;v.g.nicole.time=3;render(v,out.resolve("nicole-"+face+".png"));}v.g.nicole.visible=false;
            for(int kind=1;kind<=11;kind++){v.g.hazard.begin(v.g,kind,420,800);v.g.hazard.stage=2;v.g.hazard.timer=1;render(v,out.resolve("hazard-"+kind+".png"));}v.g.hazard.kind=0;
            for(int guardian=0;guardian<10;guardian++){v.g.stage=guardian*6+5;v.g.start();v.g.expedition.age=3;render(v,out.resolve("guardian-"+guardian+".png"));}
            v.g.stage=0;v.g.start();v.running=false;v.g.finish();v.screen=MainActivity.OceanView.REEF;
            for(int tier=0;tier<=10;tier++){v.g.home.victories=(1<<tier)-1;v.g.decor=tier==10?8:0;v.g.home.pending=0;render(v,out.resolve(String.format(Locale.ROOT,"reef-tier-%02d.png",tier)));}
            for(int decoration=1;decoration<=8;decoration++){v.g.decor=decoration;render(v,out.resolve("reef-decor-"+decoration+".png"));}
            v.g.home.pending=10;v.g.home.buildTime=2;render(v,out.resolve("reef-building.png"));v.doAction(42);check(v.screen==MainActivity.OceanView.REEF,"Cannot skip unfinished construction");v.g.home.buildTime=5;v.doAction(42);check(v.screen==MainActivity.OceanView.RESULT&&v.g.home.pending==0,"Construction acknowledges and opens result");
            v.screen=MainActivity.OceanView.REEF;v.g.decor=0;v.g.bank=1000;v.doAction(41);check(v.g.decor==1&&v.g.bank==988,"Decoration purchase from reef menu");
            v.screen=0;v.setSize(360,500);render(v,out.resolve("small-window.png"));check(v.h>v.viewportH,"Small window has scrollable menu");
            ArrayList<String> sounds=new ArrayList<>(Arrays.asList("bossi_meow","bossi_meow2","bossi_meow3","bossi_no","bossi_away","bossi_food","eat0","eat1","eat2","sonar","growth","hurt","dash","menu","nicole_bloop","nicole_fish","nicole_swim","nicole_dive","nicole_nyam","nicole_playground"));for(int i=1;i<=10;i++)sounds.add(String.format(Locale.ROOT,"music/ocean_%02d",i));
            for(String name:sounds)try(var stream=javax.sound.sampled.AudioSystem.getAudioInputStream(new java.io.BufferedInputStream(DesktopTest.class.getResourceAsStream("/audio/"+name+".wav")))){check(stream.getFrameLength()>0,"Decode "+name);}
        }catch(Throwable t){error[0]=t;}finally{if(v!=null){v.store.close();v.music.release();v.bossiAudio.stop();v.gameAudio.release();}}});
        if(error[0]!=null)throw new AssertionError("Desktop tests",error[0]);String result="PASS: "+checks+" desktop checks; shared renderer, 55 hero forms, menus, keyboard, audio decoding, save/load\n";Files.writeString(out.resolve("desktop-tests.txt"),result);System.out.print(result);
    }
    static final class SaveStoreTest {final android.content.Context context;SaveStoreTest(android.content.Context c){context=c;}Game read(){SaveStore s=new SaveStore(context);try{return s.load();}finally{s.close();}}}
}
