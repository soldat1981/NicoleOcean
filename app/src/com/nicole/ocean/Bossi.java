package com.nicole.ocean;

import java.io.*;

/** A rare, fully saved aquarium visitor. Geometry is in world coordinates, not tied to a finger. */
public final class Bossi {
    public static final int NONE=0,LEFT_PAW=1,RIGHT_PAW=2,HEAD=3,HEAD_PAWS=4,TAIL=5;
    public static final int ARRIVE=1,AIM=2,STRIKE=3,REST=4,CALL=5,LEAVE=6,HOLD_TAIL=7,LURE=8;
    public static final int SAVE_BYTES=4*13+1;
    public int kind,stage,attempts,maxAttempts,phrase,audioSerial,audioKind;
    public float wait=24,timer,x,y,targetX,targetY;
    public boolean strikeHit;
    public float warning=2.4f;
    public static final float PAW_RADIUS=70, PAW_WIDTH=260;
    private boolean previousStrike;
    private final float[] previousPaws=new float[4];
    public void reset(Game g){kind=stage=attempts=audioSerial=audioKind=0;timer=0;wait=g.between(18,26);}
    public boolean hunting(){return kind!=NONE&&kind!=TAIL&&stage!=LURE;}
    public boolean pending(){return kind!=NONE&&stage==LURE;}
    public boolean visible(){return kind!=NONE&&stage!=LURE;}
    public String phraseText(){return phrase==0?"Босси, нельзя!":phrase==1?"Босс, отойди от аквариума!":"Босс, пойдём кушать!";}
    public void begin(Game g,int variant,float viewW,float viewH){
        g.nicole.visible=false;
        kind=variant;stage=LURE;timer=0;attempts=0;maxAttempts=3+(int)(g.random()*2);strikeHit=false;
        warning=Difficulty.bossWarning(g.stage)*(g.calm?1.25f:1);
        float cameraX=Game.clamp(g.x-viewW*.5f,0,Game.WORLD_W-viewW);
        float cameraY=Game.clamp(g.y-viewH*.46f,80,Game.WORLD_H-viewH+30);
        x=Game.clamp(g.x+g.between(-70,70),170,Game.WORLD_W-170);y=g.random()<.08f?cameraY+20:Game.clamp(g.y-260,100,Game.WORLD_H-550);
        targetX=x;targetY=y+200;phrase=(int)(g.random()*3);
    }
    public float duration(){
        switch(stage){case LURE:return 1;case ARRIVE:return .32f;case AIM:return warning;case STRIKE:return .48f;
            case REST:return .85f;case CALL:return 3.8f;case LEAVE:return 1.1f;case HOLD_TAIL:return 9;default:return 1;}
    }
    public float progress(){return Game.clamp(timer/duration(),0,1);}
    public float presence(){if(stage==ARRIVE){float u=progress();return 1-(1-u)*(1-u)*(1-u);}if(stage==LEAVE)return 1-progress();return 1;}
    public float reach(){
        float u=progress();
        if(stage==STRIKE)return u<.45f?u/.45f:1-(u-.45f)/.55f;
        return 0;
    }
    private int attackIndex(){return stage==REST?Math.max(0,attempts-1):attempts;}
    public boolean attacksLeft(){return kind==LEFT_PAW||kind==HEAD_PAWS||kind==HEAD&&attackIndex()%2==0;}
    public boolean attacksRight(){return kind==RIGHT_PAW||kind==HEAD_PAWS||kind==HEAD&&attackIndex()%2!=0;}
    public float shoulderX(boolean left){return x+(left?-115:115);}
    public boolean diagonal(){
        int attempt=attackIndex();
        return kind!=HEAD_PAWS&&(kind==HEAD||attempt%3==1);
    }
    public float strikeStartX(boolean left){return shoulderX(left)+(diagonal()?(left?-100:100):0);}
    public float strikeStartY(){return y+(diagonal()?170:265);}
    public float strikeTargetX(boolean left){return targetX+(kind==HEAD_PAWS?(left?-65:65):0);}
    private float windup(){return stage==AIM?progress():stage==REST?1-progress():0;}
    public float pawX(boolean left){
        float base=shoulderX(left);if(!(left?attacksLeft():attacksRight()))return base;
        if(stage==STRIKE)return strikeStartX(left)+(strikeTargetX(left)-strikeStartX(left))*reach();
        return base+(strikeStartX(left)-base)*windup();
    }
    public float pawY(boolean left){
        float base=y+265;if(!(left?attacksLeft():attacksRight()))return base;
        if(stage==STRIKE)return strikeStartY()+(targetY-strikeStartY())*reach();
        return base+(strikeStartY()-base)*windup();
    }
    public float pawBaseX(){return shoulderX(attacksLeft());}
    public float tipX(){return pawX(attacksLeft());}
    public float tipY(){return pawY(attacksLeft());}
    private void aim(Game g){
        stage=AIM;timer=0;
        targetX=Game.clamp(g.x,x-155,x+155);targetY=Game.clamp(g.y,y+230,y+510);
        strikeHit=false;
    }
    public void update(Game g,float dt,float viewW,float viewH){
        if(kind==NONE)return; // Game owns the shared, mutually exclusive visitor schedule.
        // Anchor once when emerging. Camera movement must never drag the cat after the hero.
        previousStrike=stage==STRIKE;
        previousPaws[0]=pawX(true);previousPaws[1]=pawY(true);
        previousPaws[2]=pawX(false);previousPaws[3]=pawY(false);
        timer+=dt;if(timer<duration())return;
        if(stage==LURE){
            stage=ARRIVE;timer=0;audioKind=1;audioSerial++;g.tell("Мяу! Босси у аквариума!");
        }
        else if(stage==ARRIVE){if(kind==TAIL){stage=HOLD_TAIL;timer=0;}else aim(g);}
        else if(stage==AIM){stage=STRIKE;timer=0;strikeHit=false;}
        else if(stage==STRIKE){attempts++;stage=REST;timer=0;}
        else if(stage==REST){if(attempts<maxAttempts)aim(g);else{stage=CALL;timer=0;audioKind=2+phrase;audioSerial++;g.tell(phraseText());}}
        else if(stage==CALL||stage==HOLD_TAIL){stage=LEAVE;timer=0;}
        else if(stage==LEAVE){kind=NONE;stage=0;timer=0;wait=g.between(18,28);}
    }
    public void collide(Game g){
        if(kind==TAIL||stage!=STRIKE||strikeHit||!g.active)return;
        if(attacksLeft()&&touches(g,true)||attacksRight()&&touches(g,false)){
            strikeHit=true;
            if(g.hiddenTime<1&&g.hurt(Difficulty.catDamage(g.stage)*(g.calm?.5f:1))){
                float d=Math.max(1,Game.dist(g.x,g.y,x,y));
                g.x=Game.clamp(g.x+(g.x-x)/d*35,65,Game.WORLD_W-65);
                g.y=Game.clamp(g.y+(g.y-y)/d*40,180,Game.WORLD_H-65);
            }
        }
    }
    private boolean touches(Game g,boolean left){
        int i=left?0:2;float px=pawX(left),py=pawY(left);
        float oldX=previousStrike?previousPaws[i]:px,oldY=previousStrike?previousPaws[i+1]:py;
        return Game.sweptDistance(g.x-oldX,g.y-oldY,g.x-px,g.y-py)<Game.radius(g.level)+PAW_RADIUS;
    }
    public void write(DataOutput out)throws IOException{
        for(int n:new int[]{kind,stage,attempts,maxAttempts,phrase,audioSerial,audioKind})out.writeInt(n);
        for(float f:new float[]{wait,timer,x,y,targetX,targetY})out.writeFloat(f);
        out.writeBoolean(strikeHit);
    }
    public void read(DataInput in)throws IOException{
        kind=in.readInt();stage=in.readInt();attempts=in.readInt();maxAttempts=in.readInt();phrase=in.readInt();audioSerial=in.readInt();audioKind=in.readInt();
        wait=in.readFloat();timer=in.readFloat();x=in.readFloat();y=in.readFloat();targetX=in.readFloat();targetY=in.readFloat();strikeHit=in.readBoolean();
        if(kind<0||kind>5||stage<0||stage>8||attempts<0||attempts>4||maxAttempts<0||maxAttempts>4||phrase<0||phrase>2||audioKind<0||audioKind>4)throw new IOException("Invalid Bossi state");
        for(float f:new float[]{wait,timer,x,y,targetX,targetY})if(!Float.isFinite(f)||Math.abs(f)>100000)throw new IOException("Invalid Bossi geometry");
    }
}
