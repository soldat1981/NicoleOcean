package com.nicole.ocean;

import android.app.*;
import android.os.*;
import android.view.*;
import android.graphics.*;
import android.media.*;
import android.content.*;
import java.util.*;

public final class MainActivity extends Activity {
    private OceanView ocean;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.rgb(7,42,66));getWindow().setNavigationBarColor(Color.rgb(7,42,66));
        if(Build.VERSION.SDK_INT>=28)getWindow().getAttributes().layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
        immersive();ocean=new OceanView(this);setContentView(ocean);
    }
    private void immersive(){getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);}
    @Override protected void onPause(){super.onPause();if(ocean!=null)ocean.suspend();}
    @Override protected void onResume(){super.onResume();immersive();if(ocean!=null)ocean.resumeFrames();}
    @Override protected void onDestroy(){if(ocean!=null){ocean.store.close();ocean.music.release();ocean.bossiAudio.stop();ocean.gameAudio.release();}super.onDestroy();}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(!focus&&ocean!=null)ocean.pauseGame();if(focus)immersive();}
    @Override public void onBackPressed(){if(ocean!=null)ocean.back();}

    static final class OceanView extends View {
        static final int HOME=0,PLAY=1,PAUSE=2,RESULT=3,TUTORIAL=4,SETTINGS=5,PAINT=6,REEF=7;
        static final int INK=0xff083c51,WHITE=0xfff1fffa,MINT=0xff8ff3cf,GOLD=0xffffd58b,MUTED=0xffa8cfd5;
        final MainActivity activity;final SaveStore store;Game g;
        final HomeReefArt homeArt;final ExpeditionArt expeditionArt;final OceanBackdrop backdrop=new OceanBackdrop();final FrameStepper stepper=new FrameStepper();
        final android.util.SparseArray<Shader> fishShaders=new android.util.SparseArray<>();final Matrix fishMatrix=new Matrix();
        static final int[] ENEMY_COLORS={0xff7ddbc4,0xffecaaa1,0xffbca6e6,0xff83bfdf};
        final Paint p=new Paint(3);final Path path=new Path();final RectF rect=new RectF();
        final Typeface regular=Typeface.create("sans-serif",Typeface.NORMAL),bold=Typeface.create("sans-serif",Typeface.BOLD);
        final ArrayList<Button> buttons=new ArrayList<>();
        final BossiArt bossiArt;final BossiAudio bossiAudio;int heardCatSerial,heardNicoleSerial;
        final GameAudio gameAudio;final OceanMusic music;final HazardArt hazardArt=new HazardArt();
        final NicoleArt nicoleArt;final HeroArt heroArt=new HeroArt();
        int screen,settingsFrom,stickId=-1,paintPart,previewStage=-1;int[] previewPaint;float w=420,h=900,scale=1,padTop,padBottom,clock,saveClock;
        float inputX,inputY,stickX,stickY,knobX,knobY,cameraX,cameraY;
        float viewportH=900,menuScroll,touchStartY,scrollStart,renderHeading;int layoutScreen=-1,pressedAction=-1,pressedPointer=-1;boolean dragging;
        long previous,renderedFrames;boolean running=true,saveWarningShown,boostRequested;
        static final class Button{RectF r;int action;Button(float x,float y,float ww,float hh,int a){r=new RectF(x,y,x+ww,y+hh);action=a;}}
        OceanView(MainActivity a){
            super(a);activity=a;store=new SaveStore(a);g=store.load();screen=g.home.pending>0?REEF:g.active?PAUSE:HOME;homeArt=new HomeReefArt(a);
            expeditionArt=new ExpeditionArt(a);bossiArt=new BossiArt(a);bossiAudio=new BossiAudio(a);heardCatSerial=g.bossi.audioSerial;heardNicoleSerial=g.nicole.audioSerial;
            nicoleArt=new NicoleArt(a);gameAudio=new GameAudio(a);music=new OceanMusic(a);
            music.onFocusLost=this::pauseGame;
            setBackgroundColor(INK);
            setFocusable(true);setContentDescription("Океан Николь. Плыви джойстиком; выведи палец чуть за круг для ускорения. Меню и настройки доступны касанием.");
            setOnApplyWindowInsetsListener((v,insets)->{padTop=insets.getSystemWindowInsetTop();padBottom=insets.getSystemWindowInsetBottom();return insets;});
            if(store.recovered)g.tell("Сохранение восстановлено из резервной копии");
            if(store.damaged)post(()->new AlertDialog.Builder(a).setTitle("Не удалось прочитать сохранения")
                .setMessage("Оба файла повреждены. Можно начать заново; старые файлы останутся до первого нового сохранения.")
                .setPositiveButton("Новый океан",(d,b)->{store.damaged=false;store.save(g,true);})
                .setNegativeButton("Закрыть",(d,b)->a.finish()).setCancelable(false).show());
        }
        void beep(){if(g.sound)gameAudio.play(7,1);}
        void clearInput(){pressedAction=pressedPointer=-1;stepper.reset();stickId=-1;inputX=inputY=knobX=knobY=0;boostRequested=false;}
        void persist(boolean wait){store.save(g,wait);music.save(wait);saveClock=0;}
        void pauseGame(){music.pause(true);music.save(true);bossiAudio.stop();gameAudio.stop();if(screen==PLAY){screen=PAUSE;clearInput();persist(true);}}
        void suspend(){pauseGame();persist(true);running=false;previous=0;}
        void resumeFrames(){running=true;previous=0;postInvalidateOnAnimation();}
        void back(){if(screen==REEF){if(g.home.pending==0||g.home.buildTime>=5)doAction(42);return;}if(screen==PLAY)pauseGame();else if(screen==SETTINGS)screen=settingsFrom;else if(screen==PAINT)screen=RESULT;else if(screen==PAUSE){screen=HOME;persist(true);}else if(screen==HOME)activity.finish();else screen=HOME;invalidate();}
        float worldW(){return w;}
        float worldH(){return h;}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);if(getWidth()<=0||getHeight()<=0)return;renderedFrames++;scale=getWidth()/420f;w=420;
            viewportH=Math.max(1,(getHeight()-padTop-padBottom)/scale);
            if(layoutScreen!=screen){menuScroll=0;pressedAction=pressedPointer=-1;layoutScreen=screen;}
            h=screen==PLAY?viewportH:Math.max(800,viewportH);
            long now=System.nanoTime();double elapsed=previous==0?0:(now-previous)/1e9;float dt=(float)Math.min(.25,elapsed);previous=now;
            if(running){clock+=dt;
                if(screen==REEF&&g.home.pending>0){g.home.buildTime=Math.min(6,g.home.buildTime+dt);saveClock+=dt;if(saveClock>=1)persist(false);}
                if(screen==PLAY){
                    int beforeSnack=g.snackSerial;int before=g.eaten,beforeLevel=g.level,beforeHazard=g.hazard.kind;float hp=g.hp;
                    int ticks=stepper.advance(elapsed);
                    for(int tick=0;tick<ticks&&g.active;tick++){
                        if(boostRequested&&g.useDash()&&g.sound)gameAudio.play(6,1);
                        g.update(FrameStepper.STEP,inputX,inputY,worldW(),worldH());
                    }
                    float angle=(float)Math.atan2(Math.sin(g.heading-renderHeading),Math.cos(g.heading-renderHeading));
                    renderHeading+=angle*(1-(float)Math.exp(-18*dt));
                    if(g.sound){
                        if(g.hp<hp)gameAudio.play(5,1);else if(g.eaten>before||g.snackSerial!=beforeSnack)gameAudio.eat(g.combo);
                        if(g.level>beforeLevel)gameAudio.play(4,1);
                        if(g.hazard.kind!=0&&beforeHazard==0)gameAudio.play(3,1);
                    }
                    if(g.bossi.audioSerial!=heardCatSerial){heardCatSerial=g.bossi.audioSerial;if(g.sound)bossiAudio.play(g.bossi.audioKind);}
                    if(g.nicole.audioSerial!=heardNicoleSerial){heardNicoleSerial=g.nicole.audioSerial;if(g.sound)bossiAudio.play(4+g.nicole.audioKind);}
                    saveClock+=dt;if(saveClock>=1)persist(false);
                    if(!g.active){screen=g.home.pending>0?REEF:RESULT;clearInput();bossiAudio.stop();persist(true);}
                }
            }
            if(!running||screen!=PLAY)renderHeading=g.heading;
            music.effectsEnabled=g.sound;music.update(running&&screen==PLAY,bossiAudio.player!=null,dt);
            c.save();c.translate(0,padTop);c.scale(scale,scale);c.clipRect(0,0,w,viewportH);c.translate(0,-menuScroll);buttons.clear();
            if(screen==HOME||screen==SETTINGS||screen==TUTORIAL||screen==RESULT||screen==PAINT)drawHome(c);else drawOcean(c);
            if(screen==HOME)drawHomeUi(c);
            else if(screen==PLAY){drawHud(c);expeditionArt.overlay(c,this);}
            else if(screen==PAUSE)drawPause(c);
            else if(screen==TUTORIAL)drawTutorial(c);
            else if(screen==SETTINGS)drawSettings(c);
            else if(screen==RESULT)drawResult(c);else if(screen==PAINT)drawPainter(c);else if(screen==REEF)homeArt.overview(c,this);
            if(store.failed){box(c,18,h-40,384,30,12,0xff804354);text(c,"Не удалось сохранить. Освободи место на телефоне",210,h-20,10,WHITE,true,true);}
            if(h>viewportH){box(c,w-6,menuScroll+12+(viewportH-48)*menuScroll/Math.max(1,h-viewportH),3,24,2,0x889ce6dc);}
            c.restore();if(running){if(screen==PLAY)postInvalidateOnAnimation();else postInvalidateDelayed(33);}
        }
        void fill(int color){p.setColor(color);p.setStyle(Paint.Style.FILL);p.setShader(null);p.setStrokeWidth(1);}
        void circle(Canvas c,float x,float y,float r,int color){fill(color);c.drawCircle(x,y,r,p);}
        void oval(Canvas c,float x,float y,float rx,float ry,int color){fill(color);rect.set(x-rx,y-ry,x+rx,y+ry);c.drawOval(rect,p);}
        void box(Canvas c,float x,float y,float ww,float hh,float rad,int color){fill(color);rect.set(x,y,x+ww,y+hh);c.drawRoundRect(rect,rad,rad,p);}
        void line(Canvas c,float x,float y,float xx,float yy,float sw,int col){fill(col);p.setStrokeWidth(sw);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x,y,xx,yy,p);}
        void text(Canvas c,String s,float x,float y,float size,int color,boolean heavy,boolean center){
            fill(color);p.setTypeface(heavy?bold:regular);p.setTextSize(size);p.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);c.drawText(s,x,y,p);
        }
        void outline(Canvas c,float x,float y,float r,int color,float sw){fill(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(sw);c.drawCircle(x,y,r,p);p.setStyle(Paint.Style.FILL);}
        void pill(Canvas c,String s,float x,float y,float ww,int color){box(c,x,y,ww,27,14,0x244fffff);text(c,s,x+ww/2,y+18,10,color,true,true);}
        void button(Canvas c,String title,String sub,float x,float y,float ww,int action,boolean primary){
            float hh=sub.isEmpty()?54:66;box(c,x,y+4,ww,hh,20,0x30031f33);box(c,x,y,ww,hh,20,primary?MINT:0xff174e65);
            box(c,x+1,y+1,ww-2,hh-2,19,pressedAction==action?0x307fffff:0x087fffff);
            line(c,x+22,y+1,x+ww-22,y+1,1,primary?0x88ffffff:0x336ce2d2);
            buttonIcon(c,x+27,y+hh/2,action,primary?INK:MINT);
            text(c,title,x+ww/2+10,y+(sub.isEmpty()?33:28),ww<200?14:16,primary?INK:WHITE,true,true);
            if(!sub.isEmpty())text(c,sub,x+ww/2,y+47,10,primary?0xff34685f:MUTED,false,true);
            buttons.add(new Button(x,y,ww,hh,action));
        }
        void shade(Canvas c){fill(0xbb032c43);c.drawRect(0,0,w,h,p);}
        void water(Canvas c,boolean menu){backdrop.draw(c,w,h,clock,cameraX,cameraY,menu);}
        void buttonIcon(Canvas c,float x,float y,int action,int col){
            if(action==1||action==7||action==11||action==31){fill(col);path.reset();path.moveTo(x-6,y-10);path.lineTo(x+10,y);path.lineTo(x-6,y+10);path.close();c.drawPath(path,p);}
            else if(action==17||action==16){outline(c,x,y,9,col,2);line(c,x-10,y-8,x-10,y-1,2,col);line(c,x-10,y-1,x-3,y-3,2,col);}
            else if(action==2){shell(c,x,y,10,col);}
            else if(action==3){outline(c,x,y,7,col,2);circle(c,x,y,2,col);for(int j=0;j<8;j++){float a=j*.7854f;line(c,x+(float)Math.cos(a)*8,y+(float)Math.sin(a)*8,x+(float)Math.cos(a)*12,y+(float)Math.sin(a)*12,2,col);}}
            else if(action==4){box(c,x-9,y-10,18,21,3,col);text(c,"?",x,y+5,15,INK,true,true);}
            else if(action==8||action==6){line(c,x-11,y-1,x,y-10,2,col);line(c,x,y-10,x+11,y-1,2,col);box(c,x-7,y-1,14,12,2,col);}
            else if(action==18){line(c,x+6,y-10,x+6,y+7,2,col);line(c,x+6,y-10,x-5,y-7,3,col);line(c,x-5,y-7,x-5,y+10,2,col);oval(c,x-9,y+10,4,3,col);oval(c,x+2,y+7,4,3,col);}
            else if(action==13){box(c,x-9,y-4,7,8,1,col);line(c,x-2,y-4,x+4,y-10,2,col);line(c,x+4,y-10,x+4,y+10,2,col);line(c,x+4,y+10,x-2,y+4,2,col);outline(c,x+3,y,12,col,1.5f);}
            else if(action==15){line(c,x-8,y,x-2,y+6,3,col);line(c,x-2,y+6,x+9,y-7,3,col);}
            else if(action==14){line(c,x-10,y-3,x+10,y-3,2,col);line(c,x-10,y-3,x-5,y-8,2,col);line(c,x+10,y+4,x-10,y+4,2,col);line(c,x+10,y+4,x+5,y+9,2,col);}
            else{star(c,x,y,11,col);}
        }
        void snack(Canvas c,float x,float y,int type,float t){
            circle(c,x,y,19,0x187dffcb);
            if(type==4){for(int j=-1;j<=1;j++){line(c,x+j*5,y+10,x+j*6+(float)Math.sin(t+j)*2,y-8,3,0xff9fe4a8);}oval(c,x,y+8,10,4,0xfff0c394);}
            else if(type==5){shell(c,x,y,12,0xffeab6d3);circle(c,x,y-1,4,0xffffead9);}
            else if(type==6){c.save();c.translate(x,y);c.rotate(18);star(c,0,0,14,0xffffcd80);circle(c,0,0,3,0xffffefa8);c.restore();}
            else{float wiggle=(float)Math.sin(t*3+x)*2;for(int side=-1;side<=1;side+=2){for(int j=0;j<3;j++)line(c,x+side*7,y+j*3-2,x+side*(14+j%2*2),y+j*5-3+wiggle,2,0xffe79887);line(c,x+side*6,y-1,x+side*12,y-10,2,0xffecac86);circle(c,x+side*12,y-12,4,0xfff8c497);circle(c,x+side*4,y-8,3,WHITE);circle(c,x+side*4,y-8,1.3f,INK);}oval(c,x,y,10,7,0xffeea18c);}
        }
        void drawHome(Canvas c){
            water(c,true);
            for(int i=0;i<8;i++)grass(c,i*64-30,h+15,65+i%3*25,0xff106b76,clock+i);
            coral(c,50,h-12,.9f,0xffb386c4);coral(c,350,h+18,1.15f,0xffe6a58e);

        }
        void drawHomeUi(Canvas c){
            float top=35,heroY=255;
            pill(c,"ТВОЙ МАЛЕНЬКИЙ ОКЕАН",27,top,213,MINT);
            pill(c,"◈ "+g.bank,288,top,104,GOLD);
            text(c,"Океан Николь",210,126,38,WHITE,true,true);
            text(c,"Плыви навстречу чудесам",210,155,13,MUTED,false,true);
            for(int i=3;i>0;i--)circle(c,210,heroY,64+i*17,0x0797ffe4);
            outline(c,210,heroY,92,0x3684e7d9,1);
            for(int i=0;i<9;i++){
                float angle=(float)(i*Math.PI*2/9-clock*.07);
                float xx=210+(float)Math.cos(angle)*92,yy=heroY+(float)Math.sin(angle)*92;
                circle(c,xx,yy,i==g.stage?4:2.5f,i<=g.highestStage?GOLD:0x6688c9d3);
            }
            previewHero(c,210,heroY+(float)Math.sin(clock*1.8)*5,58,g.paintIndex(),g.paintParts[g.paintIndex()]);
            text(c,Evolution.name(Oceans.startLevel(g.stage)),210,378,17,WHITE,true,true);
            text(c,"ОКЕАН "+(g.stage+1)+" ИЗ 60  ·  УРОВНИ "+(Oceans.startLevel(g.stage))+"–"+(Oceans.goalLevel(g.stage)),210,400,10,MINT,true,true);
            float by=428;
            button(c,g.active?"Продолжить плавание":g.roundWon?"Раскрасить новую форму":"Отправиться в океан",g.active?"Уровень "+g.level+" · твоё место сохранено":"Океан "+(g.stage+1)+" · новые открытия ждут",28,by,364,1,true);
            button(c,"Домашний риф","Постройки "+g.home.tier()+" / 10 · украшения "+g.decor+" / 8",28,by+80,364,2,false);
            button(c,"Настройки","",28,by+160,175,3,false);
            button(c,"Как играть","",217,by+160,175,4,false);
            // Restart is deliberately separated from the primary play action.
            button(c,"Начать с начала","",28,by+228,364,17,false);
            box(c,28,733,364,37,14,0xd5083c51);
            text(c,"Лучший уровень "+g.bestLevel+"   ·   Плаваний "+g.totalRuns,210,755,11,MUTED,false,true);
            text(c,"Сделано для Николь  ♡",210,h-19,10,WHITE,true,true);
        }
        void drawOcean(Canvas c){
            cameraX=Game.clamp(g.x-w*.5f,0,Game.WORLD_W-w);cameraY=Game.clamp(g.y-h*.46f,80,Game.WORLD_H-h+30);
            water(c,false);expeditionArt.scenery(c,this);
            c.save();c.translate(-cameraX,-cameraY);
            if(!g.bossi.visible())nicoleArt.draw(c,g.nicole,w,h);
            homeArt.world(c,this);
            for(int j=0;j<22;j++){
                float xx=60+(j*367)%2100,yy=650+(j*523)%2250;
                if(!g.guardianOcean()&&HomeReef.contains(xx,yy))continue;
                if(yy-cameraY>h+150||yy-cameraY<-150||xx-cameraX<-150||xx-cameraX>w+150)continue;
                oval(c,xx,yy+8,49+j%3*17,18+j%2*11,0x35011831);
                oval(c,xx,yy,40+j%3*17,18+j%2*11,0xff1a5268);
                coral(c,xx+20,yy,.4f+(j%4)*.14f,j%3==0?0xffd38baf:j%3==1?0xff71b8b2:0xff827db9);
                for(int k=0;k<5;k++){float fx=xx-31+k*12,fy=yy-7+(float)Math.sin(clock+k)*2;
                    line(c,fx,yy,fx,fy-11-k%2*8,2,0xff659ea4);circle(c,fx,fy-13-k%2*8,4,0xffb2d9cf);circle(c,fx-1,fy-15-k%2*8,1.5f,WHITE);}
            }
            for(float[] gr:Game.GRASS){
                if(gr[0]<cameraX-150||gr[0]>cameraX+w+150||gr[1]<cameraY-150||gr[1]>cameraY+h+150)continue;
                circle(c,gr[0],gr[1],93,0x092fffc3);
                for(int j=0;j<6;j++)grass(c,gr[0]-65+j*26,gr[1]+60,100+(j%3)*14,Color.HSVToColor(170,new float[]{130+(g.stage*13)%110,.65f,.72f}),clock+j+g.stage*.2f);
            }
            for(Game.Item it:g.items){if(it.x-cameraX<-40||it.x-cameraX>w+40||it.y-cameraY<-40||it.y-cameraY>h+40)continue;
                float yy=it.y+(float)Math.sin(clock*2+it.x)*3;
                if(it.type==0){circle(c,it.x,yy,10,0x1678ffba);circle(c,it.x,yy,4,0xffb8f38d);circle(c,it.x-1,yy-1,1.3f,WHITE);}
                else if(it.type>=4)snack(c,it.x,it.y,it.type,clock);
                else if(it.type==1){circle(c,it.x,yy,16,0x2263f7c9);shell(c,it.x,yy,11,MINT);text(c,"+",it.x,yy+3,9,INK,true,true);}
                else if(it.type==2)shell(c,it.x,yy,10,GOLD);
                else {circle(c,it.x,yy,17,0x228f9bff);circle(c,it.x,yy,8,0xffeadcff);circle(c,it.x-2,yy-3,3,WHITE);star(c,it.x+12,yy-12,3,WHITE);}
            }
            for(Game.Fish f:g.fish){if(f.x-cameraX<-100||f.x-cameraX>w+100||f.y-cameraY<-100||f.y-cameraY>h+100)continue;
                float r=Game.radius(f.level);
                if(f.warnTime>0){float ax=f.aimX,ay=f.aimY;line(c,f.x,f.y,f.x+ax*100,f.y+ay*100,2.5f,0xccffd58b);
                    float ex=f.x+ax*100,ey=f.y+ay*100;line(c,ex,ey,ex-ax*15-ay*9,ey-ay*15+ax*9,3,GOLD);line(c,ex,ey,ex-ax*15+ay*9,ey-ay*15-ax*9,3,GOLD);
                    outline(c,f.x,f.y,r+8,0x99ffd58b,2);
                }
                if(f.dashTime>0)for(int j=1;j<=3;j++)oval(c,f.x-f.aimX*j*17,f.y-f.aimY*j*17,r*.8f,r*.4f,0x247ff9e1);

                if(f.shootWarning>0){
                    float ax=f.shootX,ay=f.shootY,ex=f.x+ax*260,ey=f.y+ay*260;
                    line(c,f.x,f.y,ex,ey,2,0x99ffaf81);
                    line(c,ex,ey,ex-ax*15-ay*8,ey-ay*15+ax*8,3,0xffffa078);
                    line(c,ex,ey,ex-ax*15+ay*8,ey-ay*15-ax*8,3,0xffffa078);
                    outline(c,f.x,f.y,r+9+(float)Math.sin(clock*15)*3,0xffffa078,3);
                    text(c,"ВЫСТРЕЛ",f.x,f.y+r+22,10,0xffffc09b,true,true);
                }
                previewStage=Evolution.index(f.level);swimmingFish(c,f.x,f.y,r,(float)Math.atan2(f.vy,f.vx),0,true,clock+f.x);previewStage=-1;
                if(f.needleDamage>0){box(c,f.x-25,f.y+r+8,50,5,2,0xff532f43);box(c,f.x-25,f.y+r+8,50*(1-f.needleDamage),5,2,0xffffd58b);}
                int color=f.level>g.level?0xffffa8a7:f.level<g.level?MINT:0xffccdae5;
                box(c,f.x-35,f.y-r*.7f-35,70,28,12,0xbb073b50);
                text(c,(f.level>g.level?"!  ":f.level<g.level?"•  ":"=  ")+f.level,f.x,f.y-r*.7f-14,17,color,true,true);
            }
            float pr=Game.radius(g.level);
            hazardArt.draw(c,g.hazard,clock,cameraY);
            if(g.hazard.visible())outline(c,g.x,g.y,pr+5,0x7797f9de,1.5f);
            if(g.dash>0)for(int i=1;i<4;i++)oval(c,g.x-(float)Math.cos(g.heading)*i*16,g.y-(float)Math.sin(g.heading)*i*16,pr*.7f,pr*.45f,0x196affd3);
            float swim=(float)Math.hypot(g.vx,g.vy);
            if(swim>20)for(int n=0;n<7;n++){float age=(clock*1.7f+n/7f)%1;float d=pr+age*(g.dash>0?115:48);float side=(float)Math.sin(n*2.4f+clock)*9;
                float bx=g.x-(float)Math.cos(renderHeading)*d-(float)Math.sin(renderHeading)*side,by=g.y-(float)Math.sin(renderHeading)*d+(float)Math.cos(renderHeading)*side;
                outline(c,bx,by,1+age*3,((int)((1-age)*95)<<24)|0xaaffed,1);}
            float squash=g.biteGlow>0?(float)Math.sin(g.biteGlow/.28f*Math.PI)*.12f:0;
            c.save();c.translate(g.x,g.y);c.scale(1+squash,1-squash*.6f);
            swimmingFish(c,0,0,pr,renderHeading,g.form,true,clock);c.restore();
            if(g.biteGlow>0){float u=1-g.biteGlow/.28f;outline(c,g.x,g.y,pr+6+u*33,((int)((1-u)*150)<<24)|0x93ffe4,2);}
            if(g.levelGlow>0){float u=1-g.levelGlow/1.2f;outline(c,g.x,g.y,pr+u*100,((int)((1-u)*160)<<24)|0xffdc8e,3);}
            for(Game.Particle pt:g.particles){float a=pt.life/pt.total;int color=((int)(a*220)<<24)|(pt.color&0xffffff);if(pt.size>3.5f)star(c,pt.x,pt.y,pt.size*a,color);else circle(c,pt.x,pt.y,pt.size*a,color);}
            if(g.combo>=2)text(c,"КОМБО ×"+g.combo,g.x,g.y-pr-22,12,GOLD,true,true);
            if(g.invincible>0)outline(c,g.x,g.y,pr+12,0xaa9ef8f4,2);
            if(g.hiddenTime>=1)text(c,"В УКРЫТИИ",g.x,g.y+pr+22,9,MINT,true,true);
            for(Game.Spark s:g.sparks)text(c,s.text,s.x,s.y,13,s.color,true,true);
            // Foreground grasses partially cover the player to make shelter tangible.
            for(float[] gr:Game.GRASS)if(gr[0]>=cameraX-150&&gr[0]<=cameraX+w+150&&gr[1]>=cameraY-150&&gr[1]<=cameraY+h+150)for(int j=0;j<3;j++)grass(c,gr[0]-40+j*40,gr[1]+65,70,0x994acfa0,clock+j);
            expeditionArt.world(c,this);
            bossiArt.draw(c,g.bossi,cameraY);
            c.restore();
        }
        void drawHud(Canvas c){
            box(c,14,18,392,83,22,0xeb083c51);
            text(c,"Океан "+(g.stage+1)+" · "+Evolution.name(g.level),30,41,10,MUTED,true,false);text(c,"Уровень "+g.level,30,69,28,WHITE,true,false);
            text(c,"◈ "+g.shells,314,43,13,GOLD,true,true);
            box(c,346,27,52,52,15,0x336affde);line(c,364,42,364,64,6,WHITE);line(c,381,42,381,64,6,WHITE);buttons.add(new Button(344,23,56,60,5));
            box(c,30,78,252,6,3,0xff27596b);box(c,30,78,252*Game.clamp(g.xp/Game.threshold(g.level),0,1),6,3,MINT);
            text(c,"РОСТ",294,84,8,MINT,true,false);
            box(c,30,90,230,5,3,0xff324e60);box(c,30,90,230*g.hp/g.maxHp(),5,3,g.hp<=30?0xffffa1a2:0xffa3efd4);
            text(c,(int)g.hp+" HP",278,95,8,g.hp<=30?0xffffa1a2:WHITE,true,false);
            box(c,22,113,376,40,15,0xb50a435b);text(c,g.guardianOcean()?(g.level>=Oceans.goalLevel(g.stage)?"Теперь съешь хранителя!":"Собирай добычу · цель: уровень "+Oceans.goalLevel(g.stage)):g.hp<=30?"Нужно лечение — найди зелёную ракушку":"Цель океана: уровень "+Oceans.goalLevel(g.stage),210,138,g.hp<=30?11:12,g.hp<=30?GOLD:WHITE,true,true);
            String zone=g.reefDistance()<HomeReef.RADIUS&&!g.guardianOcean()?"ДОМАШНИЙ РИФ · БУДЬ НАЧЕКУ":g.y<1350?"КОРАЛЛОВЫЙ РИФ":"СВЕТЯЩИЕСЯ ГЛУБИНЫ";
            text(c,zone,24,178,9,MUTED,true,false);text(c,"Поражений на океане: "+g.stageLosses+" / 5",24,194,9,g.stageLosses==4?GOLD:MUTED,false,false);
            drawCompass(c);
            if(g.hazard.visible()){
                boolean warn=g.hazard.stage==1;
                box(c,23,h-322,374,38,13,warn?0xee714951:0xe0083c51);
                text(c,(warn?"⚠  ":"")+g.hazard.name(),210,h-306,11,GOLD,true,true);
                text(c,warn?"СМЕРТЕЛЬНО! Покинь подсвеченную область":"КАСАНИЕ УБИВАЕТ · держись подальше",210,h-292,9,WHITE,false,true);
                if(warn){box(c,44,h-288,332,2,1,0x55ffffff);box(c,44,h-288,332*(1-g.hazard.timer/g.hazard.warning()),2,1,GOLD);}
            }
            if(g.bossi.visible()){
                String note=g.bossi.kind==Bossi.TAIL?"Босси отдыхает · хвост не опасен":g.bossi.stage==Bossi.CALL?g.bossi.phraseText():g.bossi.stage==Bossi.LEAVE?"Босси уходит…":Game.dist(g.x,g.y,g.bossi.x,g.bossi.y+350)>650?"Босси остался позади":g.bossi.diagonal()?"Косой удар! Уйди с подсвеченной полосы":"Босси охотится! Отплывай от кружка";
                box(c,23,h-278,374,32,13,0xdf083c51);text(c,note,210,h-257,10,GOLD,true,true);
            }
            if(g.messageTime>0){box(c,24,h-230,372,39,15,0xe00c455b);text(c,g.message,210,h-205,11,WHITE,true,true);}
            if(!g.guardianOcean()&&g.reefDistance()<HomeReef.RADIUS){button(c,"Завершить","",g.leftHand?25:245,h-110,150,6,false);}
            drawControls(c);
        }
        void drawCompass(Canvas c){
            float dx=HomeReef.X-g.x,dy=HomeReef.Y-g.y,dd=(float)Math.hypot(dx,dy);float cx=373,cy=185;
            circle(c,cx,cy,23,0x7f083c51);
            if(dd>HomeReef.RADIUS){float a=(float)Math.atan2(dy,dx);c.save();c.rotate((float)Math.toDegrees(a),cx,cy);path.reset();path.moveTo(cx+11,cy);path.lineTo(cx-6,cy-6);path.lineTo(cx-2,cy);path.lineTo(cx-6,cy+6);path.close();fill(MINT);c.drawPath(path,p);c.restore();}
            else shell(c,cx,cy,10,MINT);
            text(c,"Дом",cx,cy+37,9,MUTED,false,true);
        }
        void drawControls(Canvas c){
            float sx=g.leftHand?330:90,sy=h-96;
            if(stickId>=0){sx=stickX;sy=stickY;}
            circle(c,sx,sy,64,0x2623b9b9);outline(c,sx,sy,64,0x5575e0d5,1.5f);
            outline(c,sx,sy,73,boostRequested?GOLD:g.cooldown<=0?0x668ff3cf:0x334fffff,boostRequested?3:1);
            circle(c,sx+knobX*43,sy+knobY*43,28,boostRequested?0xbaffd58b:0x807be4d0);outline(c,sx+knobX*43,sy+knobY*43,27,0x88c7fff1,1);
            text(c,g.cooldown<=0?"За круг — ускорение":"Ускорение через "+(Math.ceil(g.cooldown*10)/10)+" с",sx,sy-87,10,g.cooldown<=0?MINT:MUTED,true,true);
            text(c,"ПЛЫТЬ",sx,h-17,8,MUTED,true,true);
        }
        void drawPause(Canvas c){
            shade(c);float top=h*.23f;
            pill(c,store.failed?"НЕ УДАЛОСЬ СОХРАНИТЬ":"ПЛАВАНИЕ НА ПАУЗЕ",89,top-34,242,store.failed?GOLD:MINT);
            text(c,"Океан подождёт",210,top+28,28,WHITE,true,true);
            text(c,"Уровень "+g.level+" · "+(int)g.hp+" HP · "+g.shells+" ракушек",210,top+59,12,MUTED,false,true);
            button(c,"Продолжить","С той же точки плавания",36,top+93,348,7,true);
            button(c,"Настройки","",36,top+173,348,3,false);
            button(c,"В главное меню","Плавание останется сохранённым",36,top+241,348,8,false);
            text(c,"Сохранение каждую секунду и при выходе",210,top+342,11,MUTED,false,true);
        }
        void drawTutorial(Canvas c){
            shade(c);float top=h*.17f;text(c,"Привет, Николь!",210,top,31,WHITE,true,true);
            String[] titles={"Плыви за пальцем","Ешь тех, кто меньше","Береги свою рыбку","Вырастай на три уровня"};
            String[] subs={"Выведи палец чуть за круг — ускоришься.","Каждый уровень — новая форма.","Пять поражений — на океан ниже.","У хранителя: +4 уровня, затем съешь его."};
            for(int i=0;i<4;i++){float yy=top+38+i*89;circle(c,52,yy+19,18,0xff286a7b);text(c,""+(i+1),52,yy+25,15,MINT,true,true);text(c,titles[i],84,yy+14,15,WHITE,true,false);text(c,subs[i],84,yy+38,10,MUTED,false,false);}
            button(c,"Я готова!","",36,top+422,348,11,true);
            button(c,"Назад","",36,top+489,348,8,false);
        }
        void drawSettings(Canvas c){
            shade(c);float top=h*.14f;text(c,"Твой океан",210,top+10,30,WHITE,true,true);
            button(c,g.calm?"Спокойный режим":"Обычный режим",g.calm?"Урон вдвое меньше, короткие погони":"Крупные рыбы опаснее — есть время спастись",30,top+48,360,12,true);
            button(c,g.sound?"Эффекты и голоса: вкл.":"Эффекты и голоса: выкл.","",30,top+128,360,13,false);
            button(c,music.enabled?"Музыка: включена":"Музыка: выключена","",30,top+196,360,18,false);
            top+=68;
            button(c,g.leftHand?"Джойстик справа":"Джойстик слева","Можно поменять руки",30,top+196,360,14,false);
            String[] names={"Искорка","Бабочка","Жемчужинка"};
            text(c,"ТВОЙ СТИЛЬ",210,top+302,10,MUTED,true,true);
            for(int i=0;i<3;i++){
                float xx=85+i*125;circle(c,xx,top+351,40,g.form==i?0x447af5d1:0x173fffff);fish(c,xx,top+350,24,1,i,true,clock);
                text(c,i<g.unlocked?names[i]:"Закрыто",xx,top+413,11,i<g.unlocked?WHITE:MUTED,true,true);
                buttons.add(new Button(xx-53,top+308,106,120,20+i));
            }
            text(c,"Бабочка — за первое плавание",210,top+449,10,MUTED,false,true);
            text(c,"Жемчужинка — найди 2 жемчужины за плавание",210,top+468,10,MUTED,false,true);
            button(c,"Готово","",36,top+499,348,15,true);
        }
        void previewHero(Canvas c,float x,float y,float r,int tier,int[] colors){
            previewStage=tier;previewPaint=colors;fish(c,x,y,r,1,g.form,true,clock);previewStage=-1;previewPaint=null;
        }
        void drawPainter(Canvas c){
            shade(c);float top=h*.08f;
            text(c,"ТВОЯ НОВАЯ ФОРМА",210,top,11,MUTED,true,true);
            text(c,Evolution.name(Oceans.startLevel(g.stage)),210,top+38,29,WHITE,true,true);
            text(c,"Океан "+(g.stage+1)+" · уровни "+(Oceans.startLevel(g.stage))+"–"+(Oceans.goalLevel(g.stage)),210,top+64,12,MUTED,false,true);
            circle(c,210,top+153,82,0x1665e6d1);previewHero(c,210,top+153,57,g.paintIndex(),g.paintParts[g.paintIndex()]);
            text(c,"ПЯТЬ ГОТОВЫХ РАСКРАСОК",210,top+259,10,MUTED,true,true);
            for(int n=0;n<5;n++){
                float xx=68+n*70;box(c,xx-28,top+275,60,83,13,g.paintPreset[g.paintIndex()]==n?0x7754c5ae:0xff174e65);
                previewHero(c,xx+2,top+305,17,g.paintIndex(),Paints.PRESETS[n]);text(c,Paints.NAMES[n],xx+2,top+344,8,WHITE,true,true);
                buttons.add(new Button(xx-28,top+275,60,83,100+n));
            }
            text(c,"ИЛИ РАСКРАСЬ ПО ЧАСТЯМ",210,top+389,10,MUTED,true,true);
            for(int n=0;n<5;n++){
                float xx=68+n*70;box(c,xx-28,top+406,60,48,11,paintPart==n?0xff388c8d:0xff174e65);
                circle(c,xx+2,top+420,5,g.paintParts[g.paintIndex()][n]);text(c,Paints.PARTS[n],xx+2,top+444,8,WHITE,true,true);
                buttons.add(new Button(xx-28,top+406,60,48,200+n));
            }
            for(int n=0;n<Paints.SWATCHES.length;n++){
                float xx=60+n%6*60,yy=top+487+n/6*44;
                circle(c,xx,yy,16,Paints.SWATCHES[n]);if(g.paintParts[g.paintIndex()][paintPart]==Paints.SWATCHES[n])outline(c,xx,yy,20,WHITE,2);
                buttons.add(new Button(xx-22,yy-22,44,44,300+n));
            }
            button(c,"Начать океан "+(g.stage+1),"",36,h-100,348,31,true);
        }
        void drawResult(Canvas c){
            shade(c);float top=h*.23f;fish(c,210,top-20,49,1,g.form,true,clock);
            text(c,g.roundWon?(g.lastStage==Oceans.COUNT-1?"Океан покорён!":"Океан пройден!"):g.hp<=0?"Попробуем ещё?":"С возвращением!",210,top+70,29,WHITE,true,true);
            text(c,g.demoted?"Пять поражений: возврат на океан "+(g.stage+1):g.roundWon?(g.lastStage==Oceans.COUNT-1?"Все океаны пройдены!":"Открыта форма: "+Evolution.name(Oceans.startLevel(g.stage))):"Океан "+(g.stage+1)+" · поражений "+g.stageLosses+" / 5",210,top+98,12,MUTED,false,true);
            box(c,36,top+128,348,90,22,0xff164e64);
            text(c,"УРОВЕНЬ",102,top+155,9,MUTED,true,true);text(c,""+g.lastLevel,102,top+193,30,WHITE,true,true);
            text(c,"РАКУШКИ",210,top+155,9,MUTED,true,true);text(c,"+"+g.lastShells,210,top+193,30,GOLD,true,true);
            text(c,"РЫБКИ",318,top+155,9,MUTED,true,true);text(c,""+g.lastEaten,318,top+193,30,MINT,true,true);
            button(c,g.roundWon?"Выбрать раскраску":"Повторить океан "+(g.stage+1),"",36,top+248,348,16,true);
            button(c,"В домашний риф","",36,top+316,348,8,false);
            if(g.totalRuns==1)text(c,"Открыта новая рыбка: Бабочка!",210,top+410,12,GOLD,true,true);
        }
        void swimmingFish(Canvas c,float x,float y,float r,float angle,int type,boolean hero,float t){
            c.save();c.translate(x,y);c.rotate((float)Math.toDegrees(angle));
            // Flip across the swimming axis on leftward travel so the fish stays upright.
            if(Math.cos(angle)<0)c.scale(1,-1);
            fish(c,0,0,r,1,type,hero,t);c.restore();
        }
        void fish(Canvas c,float x,float y,float r,float face,int type,boolean hero,float t){
            int stage=previewStage>=0?previewStage:Evolution.index(g.level);int[] colors=previewPaint!=null?previewPaint:g.paintParts[Math.min(Evolution.STAGES-1,stage)];
            heroArt.palette=colors;
            if(hero&&stage>0){c.save();c.translate(x,y);c.scale(face,1);heroArt.draw(c,stage,0,r,t);c.restore();return;}
            c.save();c.translate(x,y);c.scale(face,1);c.rotate((float)Math.sin(t*2)*3);
            if(!hero){if(type==1)c.scale(.92f,1.12f);else if(type==2)c.scale(1.07f,.84f);else if(type==3)c.scale(.91f,1.08f);}
            int col=hero?colors[1]:ENEMY_COLORS[type%4];
            float tail=(float)Math.sin(t*8)*r*.11f;
            path.reset();path.moveTo(-r*.67f,0);path.lineTo(-r*1.3f,-r*.55f+tail);path.quadTo(-r*1.42f,tail,-r*1.3f,r*.55f+tail);path.close();fill(hero?colors[3]:col);c.drawPath(path,p);
            path.reset();path.moveTo(-r*.35f,-r*.4f);path.quadTo(-r*.2f,-r*.95f,r*.35f,-r*.53f);path.close();fill(hero?colors[2]:col);c.drawPath(path,p);
            // Rounded volume, fine scales and a luminous upper rim keep fish readable in schools.
            oval(c,0,r*.45f,r*.85f,r*.3f,0x20021c35);
            fill(col);Shader shader=fishShaders.get(col);if(shader==null){shader=new LinearGradient(0,-.62f,0,.62f,col,blend(col,0xff163d64,.28f),Shader.TileMode.CLAMP);fishShaders.put(col,shader);}
            fishMatrix.setScale(r,r);shader.setLocalMatrix(fishMatrix);p.setShader(shader);
            rect.set(-r,-r*.62f,r,r*.62f);c.drawOval(rect,p);p.setShader(null);
            if(hero)oval(c,r*.48f,0,r*.52f,r*.55f,colors[0]);
            oval(c,-r*.05f,-r*.31f,r*.66f,r*.14f,0x338fffff);
            if(!hero&&type%2==0)for(int j=0;j<3;j++)oval(c,-r*.57f+j*r*.3f,-r*.05f,r*.075f,r*.39f,0x33705496);
            if(!hero&&type%2==1)for(int j=0;j<6;j++)circle(c,-r*.65f+(j%3)*r*.3f,-r*.19f+(j/3)*r*.29f,r*.042f,0x5579fff0);
            oval(c,r*.12f,r*.23f,r*.71f,r*.30f,hero?blend(colors[1],WHITE,.4f):0x338fffff);
            oval(c,-r*.32f,r*.12f,r*.19f,r*.23f,hero?colors[2]:0x4480567e);
            for(int ray=-1;ray<=1;ray++)line(c,-r*.78f,0,-r*1.26f,ray*r*.35f+tail,Math.max(.6f,r*.018f),0x5596e5ef);
            circle(c,r*.48f,-r*.17f,r*.25f,0xfffcfff6);circle(c,r*.57f,-r*.17f,r*.125f,INK);circle(c,r*.61f,-r*.23f,r*.044f,WHITE);
            oval(c,r*.67f,r*.18f,r*.13f,r*.08f,hero?colors[4]:0x33844265);
            fill(INK);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,r*.027f));path.reset();path.moveTo(r*.79f,r*.14f);path.quadTo(r*.95f,r*.22f,r*.98f,r*.03f);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);

            c.restore();
        }
        static int blend(int a,int b,float t){return Color.rgb((int)(Color.red(a)*(1-t)+Color.red(b)*t),(int)(Color.green(a)*(1-t)+Color.green(b)*t),(int)(Color.blue(a)*(1-t)+Color.blue(b)*t));}
        void grass(Canvas c,float x,float y,float length,int color,float t){
            fill(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(7);p.setStrokeCap(Paint.Cap.ROUND);
            path.reset();path.moveTo(x,y);path.cubicTo(x-20,y-length*.4f,x+20+(float)Math.sin(t)*9,y-length*.6f,x+(float)Math.sin(t)*14,y-length);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
        }
        void coral(Canvas c,float x,float y,float sz,int color){
            c.save();c.translate(x,y);c.scale(sz,sz);
            line(c,0,0,2,-66,9,color);line(c,1,-26,-23,-45,7,color);line(c,-23,-45,-27,-70,6,color);line(c,2,-40,28,-62,7,color);line(c,28,-62,30,-90,6,color);line(c,1,-55,-11,-84,6,color);
            for(int j=0;j<7;j++){float xx=j%2==0?-25:29,yy=-45-j*6;circle(c,xx,yy,3,0x55fff0ce);}
            circle(c,2,-66,5,0x55ffeedb);circle(c,-11,-84,4,0x55ffeedb);c.restore();
        }
        void shell(Canvas c,float x,float y,float r,int color){
            fill(color);path.reset();path.moveTo(x,y+r*.65f);path.cubicTo(x-r*1.5f,y-r*.25f,x-r*.8f,y-r,x,y-r*.7f);path.cubicTo(x+r*.8f,y-r,x+r*1.5f,y-r*.25f,x,y+r*.65f);c.drawPath(path,p);
            for(int i=-1;i<=1;i++)line(c,x,y+r*.4f,x+i*r*.55f,y-r*.4f,1,0x55684c58);
        }
        void star(Canvas c,float x,float y,float r,int color){
            fill(color);path.reset();for(int i=0;i<8;i++){double a=-Math.PI/2+i*Math.PI/4;float rr=i%2==0?r:r*.35f;float xx=x+(float)Math.cos(a)*rr,yy=y+(float)Math.sin(a)*rr;if(i==0)path.moveTo(xx,yy);else path.lineTo(xx,yy);}path.close();c.drawPath(path,p);
        }
        void doAction(int action){
            int oldScreen=screen;
            clearInput();
            beep();
            if(action>=100&&action<105){g.choosePaint(g.paintIndex(),action-100);persist(true);invalidate();return;}
            if(action>=200&&action<205){paintPart=action-200;invalidate();return;}
            if(action>=300&&action<312){g.colorPart(paintPart,Paints.SWATCHES[action-300]);persist(true);invalidate();return;}
            switch(action){
                case 1: if(g.active){screen=PAUSE;}else if(g.roundWon){screen=PAINT;}else if(!g.tutorialSeen)screen=TUTORIAL;else newRun();break;
                case 2:screen=REEF;break;
                case 41:if(g.home.pending>0)break;if(g.decor>=8)popup("Королевский сад","Все восемь украшений уже открыты.");else if(g.buyDecor()){persist(true);g.tell("Новое украшение заменило прежнее");}else popup("Украшение для рифа","Нужно "+HomeReef.decorCost(g.decor)+" ракушек. Сейчас в банке: "+g.bank);break;
                case 42:if(g.home.pending>0){if(g.home.buildTime<5)break;g.home.pending=0;g.home.buildTime=0;screen=RESULT;persist(true);}else screen=HOME;break;
                case 3:settingsFrom=screen;screen=SETTINGS;break;
                case 4:screen=TUTORIAL;break;
                case 5:pauseGame();break;
                case 6:g.finish();screen=RESULT;clearInput();bossiAudio.stop();persist(true);break;
                case 7:screen=PLAY;renderHeading=g.heading;music.userResume();clearInput();previous=0;break;
                case 8:screen=HOME;clearInput();persist(true);break;
                case 11:g.tutorialSeen=true;if(g.active)screen=HOME;else newRun();persist(true);break;
                case 12:g.calm=!g.calm;persist(true);break;
                case 13:g.sound=!g.sound;if(!g.sound){bossiAudio.stop();gameAudio.stop();}persist(true);break;
                case 18:music.toggle();break;
                case 14:g.leftHand=!g.leftHand;clearInput();persist(true);break;
                case 15:screen=settingsFrom;persist(true);break;
                case 16:if(g.roundWon)screen=PAINT;else newRun();break;
                case 31:newRun();break;
                case 17:if(!g.active&&g.stage==0){g.resetJourney();newRun();}else new AlertDialog.Builder(activity).setTitle("Начать с начала?")
                    .setMessage("Прохождение океанов начнётся с первого океана. Ракушки в банке и домашний риф останутся, текущая добыча не переносится.")
                    .setPositiveButton("Начать",(dialog,which)->{g.resetJourney();newRun();}).setNegativeButton("Продолжить прежнее",null).show();break;
                default: if(action>=20&&action<23){int f=action-20;if(f<g.unlocked){if(g.active)popup("После возвращения","Выбрать другую рыбку можно между плаваниями.");else{g.form=f;persist(true);}}else popup("Новая рыбка",f==1?"Заверши первое плавание.":"Найди две жемчужины за одно плавание.");}
            }
            if(screen!=oldScreen)buttons.clear();invalidate();
        }
        void popup(String title,String message){new AlertDialog.Builder(activity).setTitle(title).setMessage(message).setPositiveButton("Хорошо",null).show();}
        void newRun(){bossiAudio.stop();music.next();music.userResume();g.start();renderHeading=g.heading;heardCatSerial=g.bossi.audioSerial;heardNicoleSerial=g.nicole.audioSerial;screen=PLAY;clearInput();previous=0;persist(true);}
        @Override public boolean performClick(){super.performClick();return true;}
        @Override public boolean onTouchEvent(MotionEvent e){
            int action=e.getActionMasked(),index=e.getActionIndex();
            float tx=e.getX(index)/scale,rawY=(e.getY(index)-padTop)/scale,ty=rawY+menuScroll;
            if(action==MotionEvent.ACTION_CANCEL){clearInput();dragging=false;invalidate();return true;}
            if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){
                if(pressedPointer<0){
                    for(int j=buttons.size()-1;j>=0;j--){Button b=buttons.get(j);if(b.r.contains(tx,ty)){
                        pressedPointer=e.getPointerId(index);pressedAction=b.action;touchStartY=rawY;scrollStart=menuScroll;dragging=false;invalidate();return true;
                    }}
                    if(screen!=PLAY&&h>viewportH){pressedPointer=e.getPointerId(index);touchStartY=rawY;scrollStart=menuScroll;dragging=false;}
                }
                if(screen==PLAY){boolean side=g.leftHand?tx>w/2:tx<w/2;
                    if(side&&ty>h*.40f&&stickId<0){stickId=e.getPointerId(index);stickX=Game.clamp(tx,65,w-65);stickY=Game.clamp(ty,h*.40f,h-72);moveStick(tx,ty);}
                }
            }else if(action==MotionEvent.ACTION_MOVE){
                if(stickId>=0){int k=e.findPointerIndex(stickId);if(k>=0)moveStick(e.getX(k)/scale,(e.getY(k)-padTop)/scale);}
                if(pressedPointer>=0){int k=e.findPointerIndex(pressedPointer);if(k>=0){
                    float yy=(e.getY(k)-padTop)/scale;
                    if(Math.abs(yy-touchStartY)>10)dragging=true;
                    if(dragging){pressedAction=-1;if(screen!=PLAY)menuScroll=Game.clamp(scrollStart+touchStartY-yy,0,Math.max(0,h-viewportH));}
                    invalidate();
                }}
            }else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP){
                int id=e.getPointerId(index);
                if(id==stickId){stickId=-1;inputX=inputY=knobX=knobY=0;boostRequested=false;}
                if(id==pressedPointer){
                    int selected=pressedAction;pressedPointer=pressedAction=-1;
                    if(!dragging&&selected>=0)for(Button b:buttons)if(b.action==selected&&b.r.contains(tx,ty)){performClick();doAction(selected);break;}
                    dragging=false;invalidate();
                }
            }
            return true;
        }
        void moveStick(float x,float y){float distance=Game.dist(x,y,stickX,stickY);if(distance>=72)boostRequested=true;else if(distance<=62)boostRequested=false;float xx=(x-stickX)/40,yy=(y-stickY)/40;float len=(float)Math.hypot(xx,yy);if(len>1){xx/=len;yy/=len;}knobX=xx;knobY=yy;inputX=Math.abs(xx)<.03?0:xx;inputY=Math.abs(yy)<.03?0:yy;}
    }
}
