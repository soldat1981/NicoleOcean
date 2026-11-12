package com.nicole.ocean;

import java.io.*;
import java.util.*;

/** Platform-independent simulation. All mutable run state is saved by Codec. */
public final class Game {
    public static final float WORLD_W=2200, WORLD_H=3000;
    public static final int MAX_FISH=112, POPULATION=72, MAX_ITEMS=95, LOSSES_TO_DEMOTE=5;
    public long seed=0x71cafebabeL, sequence;
    public boolean active, calm, sound=true, leftHand, tutorialSeen;
    public int bank, bestLevel=1, totalRuns, decor, form, unlocked=1;
    public float x=1100,y=600,hp=100,xp,time,invincible,dash,cooldown,face=1;
    public float vx,vy, heading,hiddenTime, saveTimer;
    public int level=1,eaten,pearls,shells,mission,pendingUpgrade,upgradeMask,healBonus,speedBonus;
    public int lastLevel=1,lastShells,lastEaten;
    public float messageTime;
    public String message="";
    public final ArrayList<Fish> fish=new ArrayList<>();
    public final ArrayList<Item> items=new ArrayList<>();
    public final ArrayList<Spark> sparks=new ArrayList<>();
    public final Bossi bossi=new Bossi();
    public final Nicole nicole=new Nicole();
    public final Hazard hazard=new Hazard();
    public final Expedition expedition=new Expedition();
    public final Combat combat=new Combat();
    public final HomeReef home=new HomeReef();
    public int combo,snackSerial;
    public int stage,highestStage,stageLosses,lastStage;
    public boolean roundWon,demoted;
    public final int[][] paintParts=new int[Evolution.STAGES][5];public final int[] paintPreset=new int[Evolution.STAGES];
    public Game(){for(int n=0;n<Evolution.STAGES;n++)choosePaint(n,0);}
    public void choosePaint(int tier,int preset){paintPreset[tier]=preset;System.arraycopy(Paints.PRESETS[preset],0,paintParts[tier],0,5);}
    public int paintIndex(){return Evolution.index(active?level:Oceans.startLevel(stage));}
    public boolean guardianOcean(){return Oceans.guardian(stage);}
    public void colorPart(int part,int color){int tier=paintIndex();paintPreset[tier]=5;paintParts[tier][part]=color;}
    public void resetJourney(){stage=highestStage=stageLosses=0;for(int n=1;n<Evolution.STAGES;n++)choosePaint(n,0);}
    public void completeRound(){
        if(!active)return;lastStage=stage;home.won(stage);checkMission();finish();roundWon=true;demoted=false;stageLosses=0;
        stage=Math.min(Oceans.COUNT-1,stage+1);highestStage=Math.max(highestStage,stage);xp=0;
    }
    private void loseRound(){
        if(!active)return;lastStage=stage;finish();roundWon=false;demoted=false;stageLosses++;
        if(stageLosses>=LOSSES_TO_DEMOTE){stageLosses=0;if(stage>0){int removed=stage--;highestStage=stage;choosePaint(Evolution.index(Oceans.startLevel(removed)),0);demoted=true;}}
    }
    public float comboTime,densityTimer,biteGlow,levelGlow;
    public final ArrayList<Particle> particles=new ArrayList<>();
    public static final class Particle {
        public float x,y,vx,vy,life,total,size;public int color;
        Particle(float x,float y,float vx,float vy,float life,float size,int color){this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.life=this.total=life;this.size=size;this.color=color;}
    }
    public void burst(float xx,float yy,int color,int count){
        Random visual=new Random(Float.floatToIntBits(xx)^Float.floatToIntBits(yy)^Float.floatToIntBits(time)^count);
        for(int n=0;n<count;n++){
            float a=visual.nextFloat()*6.283185f,sp=25+visual.nextFloat()*95;
            if(particles.size()<240)particles.add(new Particle(xx,yy,(float)Math.cos(a)*sp,(float)Math.sin(a)*sp,.35f+visual.nextFloat()*.5f,2+visual.nextFloat()*3,color));
        }
    }
    static float sweptDistance(float ax,float ay,float bx,float by){
        float dx=bx-ax,dy=by-ay,den=dx*dx+dy*dy;
        float t=den<.0001f?0:clamp(-(ax*dx+ay*dy)/den,0,1);
        return (float)Math.hypot(ax+dx*t,ay+dy*t);
    }
    private void populateNearby(float vw,float vh,boolean initial){
        int near=0;for(Fish f:fish)if(Math.abs(f.x-x)<vw*.75f&&Math.abs(f.y-y)<vh*.65f)near++;
        int target=localPopulation(y);
        if(near>=target)return;
        // Recycle only distant fish. Visible animals never teleport or appear on the player.
        int recycle=-1;
        if(fish.size()>=POPULATION){
            int far=-1;float best=0;for(int j=0;j<fish.size();j++){Fish f=fish.get(j);
                if(Math.abs(f.x-x)<vw*.8f&&Math.abs(f.y-y)<vh*.8f)continue;
                float d=dist(x,y,f.x,f.y);if(d>best){best=d;far=j;}}
            if(far<0)return;recycle=far;
        }
        float xx,yy;
        if(initial){xx=x+between(-vw*.48f,vw*.48f);yy=y+between(-vh*.37f,vh*.48f);}
        else if(random()<.55f){xx=x+(random()<.5f?-1:1)*(vw*.5f+85);yy=y+between(-vh*.48f,vh*.48f);}
        else{xx=x+between(-vw*.48f,vw*.48f);yy=y+(random()<.5f?-1:1)*(vh*.54f+85);}
        xx=clamp(xx,60,WORLD_W-60);yy=clamp(yy,210,WORLD_H-80);
        if(dist(xx,yy,x,y)<110||!initial&&Math.abs(xx-x)<vw*.5f+65&&Math.abs(yy-y)<vh*.54f+65)return;
        for(Fish other:fish)if(dist(xx,yy,other.x,other.y)<100)return;
        if(recycle>=0)fish.remove(recycle);
        int l=populationLevel(yy);
        Fish f=new Fish(xx,yy,l,(int)(random()*4));f.vx=xx>x?-1:1;f.vy=between(-.35f,.35f);f.turn=between(2,5);fish.add(f);
    }
    public static final float[][] GRASS={{640,840},{1480,1110},{960,1750},{1730,2220},{450,2350}};
    public static final class Fish {
        public float x,y,vx,vy,turn,chase,previousX,previousY;
        public int level,kind;
        public float dashWait=7,dashTime,warnTime,aimX,aimY;
        public float needleDamage,shootWait=8,shootWarning,shootX,shootY;
        public Fish(float x,float y,int l,int k){this.x=x;this.y=y;level=l;kind=k;}
    }
    public static final class Item {
        public float x,y; public int type;
        public Item(float x,float y,int t){this.x=x;this.y=y;type=t;}
    }
    public static final class Spark {
        public float x,y,life=1; public String text; public int color;
        Spark(float x,float y,String t,int c){this.x=x;this.y=y;text=t;color=c;}
    }
    public float random(){seed^=seed<<13;seed^=seed>>>7;seed^=seed<<17;return (seed>>>40)/(float)(1L<<24);}
    public float between(float a,float b){return a+random()*(b-a);}
    public static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
    public static float dist(float x,float y,float xx,float yy){return (float)Math.hypot(x-xx,y-yy);}
    public static int threshold(int l){return 4*(18+8*(l-1));}
    public static int damage(int hero,int enemy){return enemy>hero?Math.min(35,15+5*(enemy-hero)):0;}
    public static float radius(int l){return 21*(1+.12f*Math.min(l-1,9)+.015f*Math.min(35,Math.max(0,l-10)));}
    public float maxHp(){return 100+(form==2?10:0);}
    public static boolean inHarbor(float xx,float yy){return HomeReef.contains(xx,yy);}
    public static int localPopulation(float depth){return 6+Math.round(6*clamp((depth-400)/2300,0,1));}
    public float reefDistance(){return dist(x,y,HomeReef.X,HomeReef.Y);}
    public boolean hidden(){for(float[] p:GRASS)if(dist(x,y,p[0],p[1])<95)return true;return false;}
    public void tell(String s){message=s;messageTime=3.5f;}
    public void start(){
        active=true;x=1100;y=600;hp=maxHp();xp=time=invincible=dash=cooldown=0;vx=vy=0;
        roundWon=demoted=false;face=1;heading=0;level=Oceans.startLevel(stage);eaten=pearls=shells=mission=pendingUpgrade=upgradeMask=healBonus=speedBonus=0;
        for(int milestone:new int[]{3,6,9})if(milestone<=level){speedBonus++;healBonus+=5;upgradeMask|=1<<milestone;}
        hiddenTime=comboTime=densityTimer=biteGlow=levelGlow=0;combo=0;fish.clear();items.clear();sparks.clear();particles.clear();
        // First growth is achievable nearby without chasing fish of the same level.
        if(!guardianOcean()){
        for(int i=0;i<12;i++){double a=i*Math.PI*2/12;items.add(new Item(x+(float)Math.cos(a)*95,y+110+(float)Math.sin(a)*95,0));}
        for(int i=0;i<MAX_ITEMS-12;i++)spawnItem(false,420,900);
        for(int i=0;i<POPULATION;i++)spawnFish(false,420,900);
        for(int i=0;i<26;i++)populateNearby(420,900,true);
        }
        bossi.reset(this);nicole.reset(this);hazard.reset(this);expedition.reset();combat.reset();home.resetRun();
        tell("Океан "+(stage+1)+": вырасти с "+level+" до "+Oceans.goalLevel(stage));
    }
    public void finish(){
        if(!active)return;
        bank+=shells;bestLevel=Math.max(bestLevel,level);totalRuns++;
        lastLevel=level;lastShells=shells;lastEaten=eaten;
        if(totalRuns>=1)unlocked=Math.max(unlocked,2);
        if(pearls>=2)unlocked=3;
        active=false;vx=vy=0;pendingUpgrade=0;
    }
    public boolean buyDecor(){int cost=HomeReef.decorCost(decor);if(decor>=8||bank<cost)return false;bank-=cost;decor++;return true;}
    public void applyAutomaticUpgrade(){
        if(pendingUpgrade==0)return;
        speedBonus++;healBonus+=5;hp=Math.min(maxHp(),hp+20);
        pendingUpgrade=0;tell("Новый бонус: сильнее плавники и лечение!");
    }
    public boolean useDash(){
        if(!active||cooldown>0)return false;
        dash=.35f;cooldown=Difficulty.dashCooldown(stage)-(form==1?.5f:0);return true;
    }
    public void addXp(float amount){
        if(!active||!Float.isFinite(amount)||amount<0)return;
        if(level>=Oceans.goalLevel(stage))return;
        xp+=amount;
        while(active&&xp>=threshold(level)&&level<Oceans.goalLevel(stage)){
            xp-=threshold(level);level++;levelGlow=1.2f;burst(x,y,0xffffd58b,32);
            hp=Math.min(maxHp(),hp+Math.max(4,10-Difficulty.extraDamage(stage)/2));
            tell("Уровень "+level+": "+Evolution.name(level));
            if((level==3||level==6||level==9)&&(upgradeMask&(1<<level))==0){pendingUpgrade=level;upgradeMask|=1<<level;applyAutomaticUpgrade();}
            if(level==Oceans.goalLevel(stage)){xp=0;if(!guardianOcean())completeRound();return;}
        }
    }

    public int fishReward(Fish f){
                combo=Math.min(99,combo+1);comboTime=4;biteGlow=.28f;burst(f.x,f.y,0xff9dffe0,14);
                int reward=3+2*f.level+(combo>=3?Math.min(3,combo/3):0);
                eaten++;shells++;sparks.add(new Spark(f.x,f.y,"+"+reward,0xff92f5cf));return reward;
    }

    public boolean hit(int enemy){
        if(!active||invincible>0||enemy<=level)return false;
        return hurt((damage(level,enemy)+Difficulty.extraDamage(stage))*(calm?.5f:1));
    }
    public boolean fatalHazard(){
        if(!active)return false;
        hp=0;combo=0;comboTime=0;burst(x,y,0xffff8b9e,36);loseRound();return true;
    }
    public boolean hurt(float amount){
        if(!active||invincible>0||!Float.isFinite(amount)||amount<=0)return false;
        if(expedition.block()){invincible=.3f;return false;}
        hp=Math.max(0,hp-amount);invincible=1.2f;combo=0;comboTime=0;burst(x,y,0xffff8b9e,20);
        sparks.add(new Spark(x,y-40,"−"+(int)amount,0xffff8b9e));
        if(hp<=0)loseRound();
        return true;
    }
    private void spawnItem(boolean offscreen,float viewW,float viewH){
        float xx=0,yy=0;int attempts=0;
        do{xx=between(80,WORLD_W-80);yy=between(160,WORLD_H-140);attempts++;}
        while(offscreen&&attempts<80&&Math.abs(xx-x)<viewW*.65f&&Math.abs(yy-y)<viewH*.65f);
        if(offscreen&&Math.abs(xx-x)<viewW*.65f&&Math.abs(yy-y)<viewH*.65f)return;
        float r=random();items.add(new Item(xx,yy,r<.30?0:r<.43?1:r<.51?2:r<.55?3:4+(int)(random()*4)));
    }
    private void spawnFish(boolean offscreen,float viewW,float viewH){
        float xx=0,yy=0;int attempts=0;
        do{xx=between(80,WORLD_W-80);yy=230+(WORLD_H-330)*(float)Math.sqrt(random());attempts++;}
        while(attempts<80&&((offscreen&&Math.abs(xx-x)<viewW*.65f&&Math.abs(yy-y)<viewH*.65f)||dist(xx,yy,x,y)<120));
        if(offscreen&&Math.abs(xx-x)<viewW*.65f&&Math.abs(yy-y)<viewH*.65f)return;
        // Existing fish never level up with the player. The deeper ocean has larger species.
        int l=populationLevel(yy);
        Fish f=new Fish(xx,yy,l,(int)(random()*4));f.turn=between(1,4);
        double angle=random()*Math.PI*2;f.vx=(float)Math.cos(angle);f.vy=(float)Math.sin(angle);
        fish.add(f);
    }
    private int populationLevel(float depth){
        float d=clamp((depth-400)/2300,0,1);
        float roll=random(),danger=(.04f+.32f*d)* (calm?.75f:1);
        int base=Math.max(1,Oceans.startLevel(stage)+Math.round(d*5)-2);
        if(roll<danger)return Math.min(Evolution.MAX_ENEMY_LEVEL,base+2+(int)(random()*3));
        return Math.max(1,base-(int)(random()*3));
    }
    /** Soft bounded separation prevents schools from becoming unreadable piles. */
    private void separateFish(float dt){
        for(int i=0;i<fish.size();i++)for(int j=i+1;j<fish.size();j++){
            Fish a=fish.get(i),b=fish.get(j);float dx=b.x-a.x,dy=b.y-a.y;
            float gap=radius(a.level)+radius(b.level)+30;
            if(Math.abs(dx)>=gap||Math.abs(dy)>=gap)continue;
            float d=(float)Math.hypot(dx,dy);
            if(d>=gap)continue;
            if(d<.001f){dx=((i+j)&1)==0?1:-1;dy=0;d=1;}
            float push=Math.min((gap-d)*.5f,32*dt);
            a.x=clamp(a.x-dx/d*push,55,WORLD_W-55);a.y=clamp(a.y-dy/d*push,190,WORLD_H-65);
            b.x=clamp(b.x+dx/d*push,55,WORLD_W-55);b.y=clamp(b.y+dy/d*push,190,WORLD_H-65);
        }
        for(Fish f:fish)if(f.level==level){
            float dx=f.x-x,dy=f.y-y,d=(float)Math.hypot(dx,dy),gap=radius(level)*2;
            if(d>=gap)continue;
            if(d<.001f){dx=1;dy=0;d=1;}
            float push=Math.min((gap-d)*.5f,25*dt);
            f.x=clamp(f.x+dx/d*push,55,WORLD_W-55);f.y=clamp(f.y+dy/d*push,190,WORLD_H-65);
            x=clamp(x-dx/d*push,radius(level)+20,WORLD_W-radius(level)-20);
            y=clamp(y-dy/d*push,radius(level)+130,WORLD_H-radius(level)-30);
        }
    }
    public String missionText(){
        switch(mission){case 0:return "Съешь маленьких рыб: "+Math.min(eaten,8)+" / 8";
            case 1:int middle=Oceans.startLevel(stage)+2;return "Дорасти до уровня "+middle+": "+Math.min(level,middle)+" / "+middle;
            case 2:return "Найди жемчужины: "+Math.min(pearls,2)+" / 2";
            case 3:int end=Oceans.goalLevel(stage);return "Дорасти до уровня "+end+": "+Math.min(level,end)+" / "+end;
            default:return "Цель океана: уровень "+Oceans.goalLevel(stage);}
    }
    private void checkMission(){
        int rewards=0;
        while(mission<4){
            boolean done=mission==0&&eaten>=8||mission==1&&level>=Oceans.startLevel(stage)+2||mission==2&&pearls>=2||mission==3&&level>=Oceans.goalLevel(stage);
            if(!done)break;
            mission++;shells+=6;rewards+=6;
        }
        if(rewards>0)tell("Задание выполнено! +"+rewards+" ракушек");
    }
    public void updateVisitors(float dt,float viewW,float viewH){
        if(guardianOcean()||expedition.busy())return;
        if(hazard.visible())return;
        if(bossi.visible()||bossi.pending()){bossi.update(this,dt,viewW,viewH);return;}
        if(nicole.visible){nicole.update(dt);if(!nicole.visible)bossi.wait=between(18,28);return;}
        bossi.wait-=dt;if(bossi.wait>0)return;
        if(time<20){bossi.wait=5;return;}
        // Alternate the two visitor types: equal frequency without random runs of cats or faces.
        if(nicole.nextNicole){nicole.begin(this,viewW,viewH);nicole.nextNicole=false;}
        else{bossi.begin(this,random()<.25f?Bossi.TAIL:1+(int)(random()*4),viewW,viewH);nicole.nextNicole=true;}
    }
    public void update(float dt,float inputX,float inputY,float viewW,float viewH){
        if(!active||!Float.isFinite(dt)||dt<=0)return;
        if(guardianOcean()){fish.clear();items.clear();}
        if(!Float.isFinite(inputX))inputX=0;
        if(!Float.isFinite(inputY))inputY=0;
        viewW=Float.isFinite(viewW)?clamp(viewW,300,WORLD_W):420;
        viewH=Float.isFinite(viewH)?clamp(viewH,400,WORLD_H-100):850;
        applyAutomaticUpgrade();
        dt=clamp(dt,0,.05f);time+=dt;messageTime=Math.max(0,messageTime-dt);
        invincible=Math.max(0,invincible-dt);cooldown=Math.max(0,cooldown-dt);dash=Math.max(0,dash-dt);
        float len=(float)Math.hypot(inputX,inputY);if(len>1){inputX/=len;inputY/=len;}
        float speed=(150+speedBonus*12)*(dash>0?2.8f:1)*(expedition.powers[0]>0?1.45f:1);
        float response=1-(float)Math.exp(-24*dt);
        vx+=(inputX*speed-vx)*response;vy+=(inputY*speed-vy)*response;
        if(Math.hypot(vx,vy)>5)heading=(float)Math.atan2(vy,vx);
        if(Math.abs(vx)>8)face=vx>0?1:-1;
        float oldX=x,oldY=y;
        float pr=radius(level);x=clamp(x+vx*dt,pr+20,WORLD_W-pr-20);y=clamp(y+vy*dt,pr+130,WORLD_H-pr-30);
        hiddenTime=hidden()?hiddenTime+dt:0;
        expedition.update(this,dt);
        if(!active)return;
        hazard.update(this,dt,viewW,viewH);
        updateVisitors(dt,viewW,viewH);bossi.collide(this);
        if(!active)return;
        int pursuers=0,fishBoosts=0;
        for(Fish f:fish)if((f.warnTime>0||f.dashTime>0)&&dist(x,y,f.x,f.y)<500)fishBoosts++;
        final boolean safe=time<20||hiddenTime>1;
        for(Fish f:fish){
            f.previousX=f.x;f.previousY=f.y;
            float distance=dist(x,y,f.x,f.y);boolean predator=f.level>level;
            f.turn-=dt;
            if(f.turn<=0){double a=random()*Math.PI*2;f.vx=(float)Math.cos(a);f.vy=(float)Math.sin(a)*.65f;f.turn=between(1,3);}
            float fs=predator?Difficulty.chaseSpeed(stage):65;
            if(f.chase<0)f.chase=Math.min(0,f.chase+dt);
            if(predator&&distance<Difficulty.chaseRange(stage)&&!safe){
                if(f.chase>=0&&pursuers<Difficulty.chaseLimit(stage)){
                    f.chase+=dt;
                    if(f.chase<Difficulty.chaseDuration(stage)*(calm?.65f:1)){
                        f.vx=(x-f.x)/Math.max(distance,1);f.vy=(y-f.y)/Math.max(distance,1);pursuers++;
                    }else f.chase=-Difficulty.chaseRest(stage);
                }else if(f.chase>0)f.chase=-Difficulty.chaseRest(stage);
                // Resting and excess predators turn away instead of retaining the chase vector.
                if(f.chase<0||pursuers>=Difficulty.chaseLimit(stage)&&f.chase==0){
                    f.vx=(f.x-x)/Math.max(distance,1);f.vy=(f.y-y)/Math.max(distance,1);fs=65;
                }
            }else if(f.chase>0)f.chase=-Difficulty.chaseRest(stage);
            if(f.level<level&&distance<Difficulty.fleeRange(stage)){
                f.vx=(f.x-x)/Math.max(distance,1);f.vy=(f.y-y)/Math.max(distance,1);fs=Difficulty.fleeSpeed(stage);
            }
            if(Difficulty.fishDashes(stage)&&f.shootWarning==0&&!bossi.visible()&&!bossi.pending()&&!hazard.visible()){
                if(f.warnTime>0){
                    f.warnTime=Math.max(0,f.warnTime-dt);fs=18;
                    if(f.warnTime==0)f.dashTime=.45f;
                }else if(f.dashTime>0){f.dashTime=Math.max(0,f.dashTime-dt);f.vx=f.aimX;f.vy=f.aimY;fs=240+Difficulty.progress(stage)*60;}
                else{f.dashWait=Math.max(0,f.dashWait-dt);if(f.dashWait<=0&&distance>100&&distance<480&&!(predator&&safe)&&fishBoosts<Difficulty.dashLimit(stage)){
                    fishBoosts++;
                    f.warnTime=Difficulty.fishWarning(stage)*(calm?1.3f:1);f.dashWait=between(7,12)-Difficulty.progress(stage)*3;
                    float ax=predator?x-f.x:f.vx,ay=predator?y-f.y:f.vy,d=Math.max(.001f,(float)Math.hypot(ax,ay));f.aimX=ax/d;f.aimY=ay/d;
                }}
            }else{f.warnTime=f.dashTime=0;}
            
            if(bossi.hunting()||bossi.pending()){
                float fx=f.x-bossi.x,fy=f.y-(bossi.y+230),fd=Math.max(1,(float)Math.hypot(fx,fy));
                if(fd<2){fx=f.x<bossi.x?-1:1;fy=1;fd=1.4142f;}
                f.vx=fx/fd;f.vy=fy/fd;f.chase=0;fs=230;
            }
            if(hazard.visible()){
                float fx=f.x-hazard.originX,fy=f.y-hazard.originY;
                boolean flee=hazard.horizontal()?Math.abs(fy)<200:hazard.radial()?Math.hypot(fx,fy)<220:Math.abs(fx)<hazard.laneWidth()+50;
                if(flee){
                    if(hazard.horizontal()){f.vx*=.3f;f.vy=fy<0?-1:1;}
                    else if(hazard.radial()){float d=Math.max(1,(float)Math.hypot(fx,fy));f.vx=fx/d;f.vy=fy/d;}
                    else{f.vx=fx<0?-1:1;f.vy*=.3f;}
                    f.chase=0;fs=170;
                }
            }
            if(f.shootWarning>0)fs*=.25f;
            f.x+=f.vx*fs*dt;f.y+=f.vy*fs*dt;
            if(f.x<55||f.x>WORLD_W-55){f.x=clamp(f.x,55,WORLD_W-55);f.vx*=-1;}
            if(f.y<190||f.y>WORLD_H-65){f.y=clamp(f.y,190,WORLD_H-65);f.vy*=-1;}
        }
        combat.update(this,dt,viewW,viewH,oldX,oldY);if(!active)return;
        // Resolve hostile bites first. One global invulnerability gate prevents damage stacking.
        for(Fish f:fish){
            if(f.level<=level||hiddenTime>1)continue;
            float fr=radius(f.level);
            if(sweptDistance(oldX-f.previousX,oldY-f.previousY,x-f.x,y-f.y)<(pr+fr)&&hit(f.level)){
                float dx=x-f.x,dy=y-f.y,d=Math.max(1,dist(x,y,f.x,f.y));
                x=clamp(x+dx/d*55,pr+20,WORLD_W-pr-20);y=clamp(y+dy/d*55,pr+130,WORLD_H-pr-30);
                break;
            }
        }
        if(!active)return;
        // XP is awarded after all collisions to avoid order-dependent growth mid-loop.
        float gained=0;
        comboTime=Math.max(0,comboTime-dt);if(comboTime==0)combo=0;
        biteGlow=Math.max(0,biteGlow-dt);levelGlow=Math.max(0,levelGlow-dt);
        for(int i=fish.size()-1;i>=0;i--){Fish f=fish.get(i);
            // Whole-body contact, including tail-side contact and relative movement during a dash.
            if(f.level<level&&sweptDistance(oldX-f.previousX,oldY-f.previousY,x-f.x,y-f.y)<=pr*1.2f+radius(f.level)*1.1f){
                gained+=fishReward(f);fish.remove(i);
            }
        }
        for(int i=items.size()-1;i>=0;i--){Item it=items.get(i);
            if(it.type==7){it.x=clamp(it.x+(float)Math.sin(time*.45f+it.y*.01f)*3*dt,35,WORLD_W-35);it.y=clamp(it.y+(float)Math.cos(time*.33f+it.x*.01f)*1.5f*dt,180,WORLD_H-40);}
            if(sweptDistance(oldX-it.x,oldY-it.y,x-it.x,y-it.y)<pr+13){
                if(it.type==0||it.type>=4){gained+=(it.type==0?2:it.type-1)+level/5;snackSerial++;biteGlow=.22f;burst(it.x,it.y,0xffffd58b,8);}
                else if(it.type==1){float heal=Difficulty.heal(stage)+healBonus;float restored=Math.min(heal,maxHp()-hp);hp+=restored;sparks.add(new Spark(it.x,it.y,"+"+(int)restored+" HP",0xff92f5cf));}
                else if(it.type==2){shells+=2;sparks.add(new Spark(it.x,it.y,"+2",0xffffd585));}
                else{pearls++;shells+=4;sparks.add(new Spark(it.x,it.y,"Жемчужина!",0xffe2c5ff));}
                items.remove(i);
            }
        }
        separateFish(dt);
        addXp(gained);if(!active)return;home.update(this,dt);if(!active)return;checkMission();
        // Old saves may contain 112 fish. Thin only distant animals, never visible ones.
        if(fish.size()>POPULATION)for(int i=fish.size()-1;i>=0;i--){
            Fish f=fish.get(i);if(Math.abs(f.x-x)>viewW*.8f||Math.abs(f.y-y)>viewH*.8f){fish.remove(i);break;}
        }
        // Top up gently, outside the visible camera rectangle only.
        if(!guardianOcean()&&fish.size()<POPULATION)spawnFish(true,viewW,viewH);
        densityTimer-=dt;if(densityTimer<=0){densityTimer=.3f;if(!guardianOcean())populateNearby(viewW,viewH,false);}
        for(int j=particles.size()-1;j>=0;j--){Particle p=particles.get(j);p.life-=dt;if(p.life<=0){particles.remove(j);continue;}p.x+=p.vx*dt;p.y+=p.vy*dt;p.vx*=1-dt*2;p.vy-=18*dt;}
        if(!guardianOcean()&&items.size()<MAX_ITEMS)spawnItem(true,viewW,viewH);
        for(int i=sparks.size()-1;i>=0;i--){Spark s=sparks.get(i);s.life-=dt;s.y-=22*dt;if(s.life<=0)sparks.remove(i);}
    }

    public byte[] snapshot() throws IOException {return Codec.encode(this);}
    public static Game restore(byte[] data) throws IOException {return Codec.decode(data);}
    /** Versioned binary format plus CRC: truncated or corrupt slots cannot become a new game. */
    static final class Codec {
        static final int MAGIC=0x4e49434f,VERSION=13;
        static int extensionBytes(Game g){return 266+g.fish.size()*20+extraBytes(g);}
        static int extraBytes(Game g){return (Evolution.STAGES-9)*24+g.expedition.bytes()+g.combat.bytes(g)+g.home.bytes();}
        static byte[] encode(Game g)throws IOException{
            ByteArrayOutputStream payload=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(payload);
            o.writeInt(MAGIC);o.writeInt(VERSION);o.writeLong(g.sequence);o.writeLong(g.seed);
            o.writeBoolean(g.active);o.writeBoolean(g.calm);o.writeBoolean(g.sound);o.writeBoolean(g.leftHand);o.writeBoolean(g.tutorialSeen);
            for(int v:new int[]{g.bank,g.bestLevel,g.totalRuns,g.decor,g.form,g.unlocked,g.level,g.eaten,g.pearls,g.shells,g.mission,g.pendingUpgrade,g.upgradeMask,g.healBonus,g.speedBonus,g.lastLevel,g.lastShells,g.lastEaten})o.writeInt(v);
            for(float v:new float[]{g.x,g.y,g.hp,g.xp,g.time,g.invincible,g.dash,g.cooldown,g.face,g.vx,g.vy,g.hiddenTime})o.writeFloat(v);
            o.writeFloat(g.heading);
            o.writeInt(g.fish.size());for(Fish f:g.fish){o.writeInt(f.level);o.writeInt(f.kind);for(float v:new float[]{f.x,f.y,f.vx,f.vy,f.turn,f.chase})o.writeFloat(v);}
            o.writeInt(g.items.size());for(Item it:g.items){o.writeFloat(it.x);o.writeFloat(it.y);o.writeInt(it.type);}
            g.bossi.write(o);
            g.nicole.write(o);g.hazard.write(o);o.writeInt(g.combo);o.writeFloat(g.comboTime);o.writeFloat(g.densityTimer);
            o.writeFloat(g.nicole.x);o.writeFloat(g.nicole.y);o.writeFloat(g.nicole.width);o.writeInt(g.nicole.audioSerial);o.writeInt(g.nicole.audioKind);
            o.writeFloat(g.bossi.warning);o.writeFloat(g.hazard.warningSeconds);
            o.writeInt(g.hazard.remainingTypes);
            for(Fish f:g.fish)for(float v:new float[]{f.dashWait,f.dashTime,f.warnTime,f.aimX,f.aimY})o.writeFloat(v);
            for(int v:new int[]{g.stage,g.highestStage,g.stageLosses,g.lastStage})o.writeInt(v);o.writeBoolean(g.roundWon);o.writeBoolean(g.demoted);
            for(int n=0;n<9;n++){o.writeInt(g.paintPreset[n]);for(int color:g.paintParts[n])o.writeInt(color);}
            for(int n=9;n<Evolution.STAGES;n++){o.writeInt(g.paintPreset[n]);for(int color:g.paintParts[n])o.writeInt(color);}
            g.expedition.write(o);g.combat.write(o,g);g.home.write(o);
            o.flush();byte[] bytes=payload.toByteArray();java.util.zip.CRC32 crc=new java.util.zip.CRC32();crc.update(bytes);
            o.writeLong(crc.getValue());o.flush();return payload.toByteArray();
        }
        static float number(DataInputStream i)throws IOException{float v=i.readFloat();if(!Float.isFinite(v)||Math.abs(v)>1e8)throw new IOException("Invalid number");return v;}
        static int range(int n,int lo,int hi)throws IOException{if(n<lo||n>hi)throw new IOException("Invalid range");return n;}
        static Game decode(byte[] bytes)throws IOException{
            if(bytes==null||bytes.length<120||bytes.length>1000000)throw new IOException("Invalid size");
            java.util.zip.CRC32 crc=new java.util.zip.CRC32();crc.update(bytes,0,bytes.length-8);
            DataInputStream tail=new DataInputStream(new ByteArrayInputStream(bytes,bytes.length-8,8));if(tail.readLong()!=crc.getValue())throw new IOException("Checksum mismatch");
            DataInputStream i=new DataInputStream(new ByteArrayInputStream(bytes,0,bytes.length-8));
            if(i.readInt()!=MAGIC)throw new IOException("Unsupported save");
            int version=i.readInt();if(version<1||version>VERSION)throw new IOException("Unsupported save");
            Game g=new Game();g.sequence=i.readLong();g.seed=i.readLong();g.active=i.readBoolean();g.calm=i.readBoolean();g.sound=i.readBoolean();g.leftHand=i.readBoolean();g.tutorialSeen=i.readBoolean();
            if(g.sequence<0||g.seed==0)throw new IOException("Invalid sequence or random state");
            g.bank=range(i.readInt(),0,100000000);g.bestLevel=range(i.readInt(),1,version<11?225:Evolution.MAX_LEVEL);g.totalRuns=range(i.readInt(),0,10000000);
            g.decor=range(i.readInt(),0,version>=13?8:3);g.form=range(i.readInt(),0,2);g.unlocked=range(i.readInt(),1,3);g.level=range(i.readInt(),1,version<11?225:Evolution.MAX_LEVEL);
            g.eaten=range(i.readInt(),0,10000000);g.pearls=range(i.readInt(),0,10000000);g.shells=range(i.readInt(),0,100000000);g.mission=range(i.readInt(),0,4);
            g.pendingUpgrade=range(i.readInt(),0,10);g.upgradeMask=i.readInt();g.healBonus=range(i.readInt(),0,15);g.speedBonus=range(i.readInt(),0,3);
            g.lastLevel=range(i.readInt(),1,version<11?225:Evolution.MAX_LEVEL);g.lastShells=range(i.readInt(),0,100000000);g.lastEaten=range(i.readInt(),0,10000000);
            g.x=number(i);g.y=number(i);g.hp=number(i);g.xp=number(i);g.time=number(i);g.invincible=number(i);g.dash=number(i);g.cooldown=number(i);g.face=number(i);g.vx=number(i);g.vy=number(i);g.hiddenTime=number(i);
            g.heading=version>=2?number(i):Math.hypot(g.vx,g.vy)>5?(float)Math.atan2(g.vy,g.vx):g.face<0?(float)Math.PI:0;
            if(g.x<0||g.x>WORLD_W||g.y<0||g.y>WORLD_H||g.hp<0||g.hp>110||g.xp<0||g.time<0||g.active&&g.hp<=0)throw new IOException("Invalid state");
            int n=range(i.readInt(),0,MAX_FISH);for(int j=0;j<n;j++){int l=range(i.readInt(),1,version<11?230:Evolution.MAX_ENEMY_LEVEL),k=range(i.readInt(),0,3);Fish f=new Fish(number(i),number(i),l,k);f.vx=number(i);f.vy=number(i);f.turn=number(i);f.chase=number(i);g.fish.add(f);}
            n=range(i.readInt(),0,MAX_ITEMS);for(int j=0;j<n;j++){float x=number(i),y=number(i);g.items.add(new Item(x,y,range(i.readInt(),0,7)));}
            if(version>=3)g.bossi.read(i);
            if(version>=4)g.nicole.read(i);else g.bossi.wait=Math.min(g.bossi.wait,26);
            if(version>=5){g.hazard.read(i);g.combo=range(i.readInt(),0,99);g.comboTime=number(i);g.densityTimer=number(i);}
            if(version>=6){
                g.nicole.x=number(i);g.nicole.y=number(i);g.nicole.width=number(i);g.nicole.audioSerial=range(i.readInt(),0,100000000);g.nicole.audioKind=range(i.readInt(),0,6);
                g.bossi.warning=number(i);g.hazard.warningSeconds=number(i);
                g.hazard.remainingTypes=range(i.readInt(),0,Hazard.ALL_TYPES);
                if(version<9){g.hazard.remainingTypes|=Hazard.ALL_TYPES&~15;g.hazard.wait=Math.min(g.hazard.wait,Difficulty.eventInterval(g.stage)*1.2f*(g.calm?1.2f:1));}
                if(g.nicole.width<150||g.nicole.width>5000||g.bossi.warning<.5f||g.bossi.warning>4||g.hazard.warningSeconds<1||g.hazard.warningSeconds>5)throw new IOException("Invalid progression geometry");
                for(Fish f:g.fish){f.dashWait=number(i);f.dashTime=number(i);f.warnTime=number(i);f.aimX=number(i);f.aimY=number(i);
                    if(f.dashWait<0||f.dashWait>20||f.dashTime<0||f.dashTime>1||f.warnTime<0||f.warnTime>2||Math.abs(f.aimX)>1.01f||Math.abs(f.aimY)>1.01f)throw new IOException("Invalid fish dash");}
            }else{g.xp*=2;g.nicole.x=g.x;g.nicole.y=g.y-108;g.bossi.warning=1.15f;g.hazard.warningSeconds=g.hazard.kind==Hazard.ANCHOR?2.8f/.9f:3.2f;}
            if(version>=7){
                g.stage=range(i.readInt(),0,version>=11?59:version>=10?44:8);g.highestStage=range(i.readInt(),g.stage,version>=11?59:version>=10?44:8);g.stageLosses=range(i.readInt(),0,version>=8?4:2);g.lastStage=range(i.readInt(),0,version>=11?59:version>=10?44:8);g.roundWon=i.readBoolean();g.demoted=i.readBoolean();
                for(int tier=0;tier<9;tier++){g.paintPreset[tier]=range(i.readInt(),0,5);for(int part=0;part<5;part++){int color=i.readInt();if((color>>>24)!=255)throw new IOException("Invalid paint color");g.paintParts[tier][part]=color;}}
            }else{g.stage=Math.max(0,((g.active?g.level:Math.max(g.bestLevel,g.lastLevel))-1)/5);g.highestStage=g.stage;g.lastStage=g.stage;}
            if(version>=10){for(int tier=9;tier<(version>=11?Evolution.STAGES:45);tier++){g.paintPreset[tier]=range(i.readInt(),0,5);for(int part=0;part<5;part++){int color=i.readInt();if((color>>>24)!=255)throw new IOException("Invalid paint color");g.paintParts[tier][part]=color;}}g.expedition.read(i);}
            if(version>=12)g.combat.read(i,g);else for(Expedition.Shot shot:g.expedition.shots)shot.level=g.level;
            if(version>=13)g.home.read(i);
            if(g.expedition.busy()&&(g.hazard.visible()||g.bossi.visible()||g.nicole.visible))throw new IOException("Guardian overlaps visitor");
            if(g.hazard.visible()&&(g.bossi.visible()||g.nicole.visible))throw new IOException("Hazard overlaps visitor");
            if(g.bossi.visible()&&g.nicole.visible)throw new IOException("Visitors cannot overlap");
            if((g.active&&g.hp>g.maxHp())||g.invincible<0||g.cooldown<0||g.dash<0||g.hiddenTime<0||g.comboTime<0)throw new IOException("Invalid health or timer");
            for(Fish f:g.fish)if(f.x<0||f.x>WORLD_W||f.y<0||f.y>WORLD_H)throw new IOException("Fish outside world");
            for(Item it:g.items)if(it.x<0||it.x>WORLD_W||it.y<0||it.y>WORLD_H)throw new IOException("Item outside world");
            if(i.available()!=0)throw new IOException("Trailing data");
            if(version<11){
                int oldLevel=g.level;float fraction=Math.min(.99f,g.xp/(12f*(oldLevel+1)));
                int oldStage=version>=7?g.stage:Math.max(0,(oldLevel-1)/5);
                g.stage=Oceans.fromNormal(Math.min(49,oldStage));g.highestStage=Math.max(g.stage,Oceans.fromNormal(Math.min(49,g.highestStage)));g.lastStage=g.stage;
                g.level=Oceans.startLevel(g.stage)+Math.min(2,Math.max(0,(oldLevel-1)%5));g.xp=fraction*threshold(g.level);
                g.bestLevel=Math.min(55,Math.max(g.level,(g.bestLevel-1)/5+1));g.lastLevel=Math.min(55,(g.lastLevel-1)/5+1);
                for(Fish f:g.fish)f.level=Math.max(1,Math.min(56,g.level+f.level-oldLevel));
                g.expedition.reset();g.bossi.reset(g);g.nicole.reset(g);g.hazard.reset(g);
            }
            if(version<13)g.home.migrate(g);
            g.applyAutomaticUpgrade();return g;
        }
    }
}
