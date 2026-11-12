package com.nicole.ocean;

import java.io.*;

/** One telegraphed, saved world event. Routes lock before movement begins. */
public final class Hazard {
    public static final int SUBMARINE=1,ANCHOR=2,BOULDER=3,WHIRLPOOL=4,HURRICANE=5,SHIP=6,JELLY=7,NET=8,GEYSER=9,MIGRATION=10,MINE=11,TYPE_COUNT=11,ALL_TYPES=(1<<TYPE_COUNT)-1,SAVE_BYTES=53;
    public int kind,stage,lastKind,direction=1;
    public float wait=75,timer,x,y,originX,originY,width=420,height=850,length;
    public boolean hit;
    public float warningSeconds=3.4f;
    public int remainingTypes=ALL_TYPES;
    public boolean visible(){return kind!=0;}
    private static final String[] NAMES={"", "ПОДВОДНАЯ ЛОДКА", "ПАДАЕТ ЯКОРЬ", "ОБВАЛ СКАЛЫ", "МОРСКОЙ ВОДОВОРОТ", "ПОДВОДНЫЙ УРАГАН", "ТОНУЩИЙ КОРАБЛЬ", "ЭЛЕКТРИЧЕСКИЕ МЕДУЗЫ", "РЫБОЛОВНАЯ СЕТЬ", "ПОДВОДНЫЙ ГЕЙЗЕР", "СТАЯ СКАТОВ", "МОРСКАЯ МИНА"};
    public String name(){return NAMES[kind];}
    public float warning(){return warningSeconds*(kind==ANCHOR?.9f:1);}
    public float duration(){
        switch(kind){case SUBMARINE:return 13;case ANCHOR:return 3.4f;case BOULDER:return 4.8f;
            case HURRICANE:return 7;case SHIP:return 7;case JELLY:return 8;case NET:return 6;
            case GEYSER:return 6;case MIGRATION:return 6;case MINE:return 6;default:return 8;}
    }
    private float nextWait(Game g){return Difficulty.eventInterval(g.stage)*g.between(.8f,1.2f)*(g.calm?1.2f:1);}
    public void reset(Game g){kind=stage=lastKind=0;remainingTypes=ALL_TYPES;timer=0;wait=nextWait(g);hit=false;}
    public boolean radial(){return kind==WHIRLPOOL||kind==JELLY||kind==MINE;}
    public boolean horizontal(){return kind==SUBMARINE||kind==MIGRATION;}
    public float laneWidth(){return kind==BOULDER||kind==SHIP?200:kind==HURRICANE?145:kind==NET?150:kind==GEYSER?80:135;}
    public float elementX(int n){
        return kind==JELLY?originX+(float)Math.cos(timer*.65f+n*2.094395f)*90:x-direction*n*85;
    }
    public float elementY(int n){
        return kind==JELLY?originY+(float)Math.sin(timer*.65f+n*2.094395f)*75:y+(n-1)*55;
    }
    public boolean geyserOn(){return timer%2>=.7f;}
    public float blastRadius(){return timer<4.2f?0:Game.clamp((timer-4.2f)*160,0,210);}
    public void begin(Game g,int type,float vw,float vh){
        if(g.guardianOcean()||g.expedition.busy()||type<1||type>TYPE_COUNT||(g.bossi.visible()||g.bossi.pending())||g.nicole.visible)return;
        kind=type;lastKind=type;stage=1;timer=0;hit=false;width=vw;height=vh;length=vw*3;
        warningSeconds=Difficulty.eventWarning(g.stage)*(g.calm?1.25f:1);
        float cx=Game.clamp(g.x-vw*.5f,0,Game.WORLD_W-vw),cy=Game.clamp(g.y-vh*.46f,80,Game.WORLD_H-vh+30);
        direction=g.random()<.5f?-1:1;
        originX=Game.clamp(g.x+g.between(-45,45),cx+90,cx+vw-90);originY=g.y;
        if(horizontal()){originY=Game.clamp(g.y+g.between(-45,45),cy+220,cy+vh-220);originX=cx+vw*.5f;}
        else if(radial()){originX=Game.clamp(g.x+g.between(-80,80),cx+125,cx+vw-125);originY=Game.clamp(g.y+80,cy+260,cy+vh-220);}
        x=originX;y=originY;
    }
    public void update(Game g,float dt,float vw,float vh){
        if(!g.active)return;
        if(!visible()){
            wait-=dt;
            if(wait<=0&&g.time>12&&!g.guardianOcean()&&!g.expedition.busy()&&!(g.bossi.visible()||g.bossi.pending())&&!g.nicole.visible){
                if(remainingTypes==0)remainingTypes=ALL_TYPES;
                int choices=remainingTypes&~(lastKind==0?0:1<<(lastKind-1));if(choices==0)choices=remainingTypes;
                int pick=(int)(g.random()*Integer.bitCount(choices)),type=1;
                for(int n=0;n<TYPE_COUNT;n++)if((choices&(1<<n))!=0&&pick--==0){type=n+1;break;}
                remainingTypes&=~(1<<(type-1));begin(g,type,vw,vh);
            }
            return;
        }
        timer+=dt;
        if(stage==1){if(timer>=warning()){timer=0;stage=2;position();}return;}
        if(timer>=duration()){
            if(!hit){g.shells+=4;g.tell("Опасность позади! +4 ракушки");g.burst(g.x,g.y,0xffffd58b,22);}
            kind=stage=0;timer=0;wait=nextWait(g);return;
        }
        position();
        float r=Game.radius(g.level),dx=g.x-x,dy=g.y-y;
        boolean contact=false;
        if(kind==SUBMARINE)contact=Math.abs(dx)<length*.5f+r*.8f&&Math.abs(dy)<62+r*.75f;
        if(kind==ANCHOR)contact=Math.abs(dx)<72+r*.8f&&Math.abs(dy)<108+r*.8f;
        if(kind==BOULDER)contact=Game.dist(x,y,g.x,g.y)<83+r;
        if(kind==WHIRLPOOL){
            float d=Math.max(1,Game.dist(x,y,g.x,g.y));
            if(d<180&&d>5){float pull=85*(1-d/220);g.x-=dx/d*pull*dt;g.y-=dy/d*pull*dt;}
            contact=d<43+r*.65f;
        }
        if(kind==HURRICANE){
            contact=Math.abs(dx)<48+r*.6f&&Math.abs(dy)<height*.45f;
            if(Math.abs(dx)<145&&Math.abs(dy)<height*.45f){
                float flow=55*(1-Math.abs(dx)/145);g.x+=direction*flow*dt;g.y+=(float)Math.sin(timer*3)*flow*dt;
            }
        }
        if(kind==SHIP){
            float angle=shipAngle(),cs=(float)Math.cos(angle),sn=(float)Math.sin(angle);
            float lx=dx*cs+dy*sn,ly=-dx*sn+dy*cs;
            contact=Math.abs(lx)<145+r*.7f&&Math.abs(ly)<52+r*.7f;
        }
        if(kind==JELLY||kind==MIGRATION){
            for(int n=0;n<3;n++)if(Game.dist(g.x,g.y,elementX(n),elementY(n))<r+(kind==JELLY?29:38))contact=true;
        }
        if(kind==NET){
            contact=Math.abs(dx)<100+r*.7f&&Math.abs(dy)<65+r*.7f;
            if(contact&&g.hiddenTime<1){g.vx*=.8f;g.vy*=.8f;}
        }
        if(kind==GEYSER)contact=geyserOn()&&Math.abs(dx)<43+r*.6f&&dy<45+r&&dy>-height*.65f;
        if(kind==MINE){float d=(float)Math.hypot(dx,dy);contact=timer<4.2f?d<28+r:Math.abs(d-blastRadius())<24+r;}
        g.x=Game.clamp(g.x,r+20,Game.WORLD_W-r-20);g.y=Game.clamp(g.y,r+130,Game.WORLD_H-r-30);
        if(contact&&g.fatalHazard()){
            hit=true;g.burst(g.x,g.y,0xffffa4a1,28);
        }
    }
    public float shipAngle(){return direction*(.25f+timer/duration()*.65f);}
    private void position(){
        float u=Game.clamp(timer/duration(),0,1);
        if(kind==SUBMARINE){x=originX+direction*((u-.5f)*(length+width+200));y=originY;}
        else if(kind==ANCHOR){x=originX;y=originY-height*.65f-200+u*(height+600);}
        else if(kind==BOULDER){x=originX+direction*(u-.5f)*240;y=originY-height*.65f-180+u*(height+560);}
        else if(kind==SHIP){x=originX+direction*(u-.5f)*140;y=originY-height*.65f-180+u*(height+500);}
        else if(kind==NET){x=originX+(float)Math.sin(u*Math.PI)*45*direction;y=originY-height*.6f-140+u*(height+400);}
        else if(kind==MIGRATION){x=originX+direction*(u-.5f)*(width+650);y=originY+(u-.5f)*220;}
        else if(kind==HURRICANE){x=originX+direction*(u-.5f)*140;y=originY;}
        else if(kind==GEYSER){x=originX;y=originY+180;}
        else{x=originX;y=originY;}
    }
    public void write(DataOutput o)throws IOException{
        for(int n:new int[]{kind,stage,lastKind,direction})o.writeInt(n);
        for(float f:new float[]{wait,timer,x,y,originX,originY,width,height,length})o.writeFloat(f);o.writeBoolean(hit);
    }
    public void read(DataInputStream i)throws IOException{
        kind=Game.Codec.range(i.readInt(),0,TYPE_COUNT);stage=Game.Codec.range(i.readInt(),0,2);lastKind=Game.Codec.range(i.readInt(),0,TYPE_COUNT);direction=i.readInt();
        wait=Game.Codec.number(i);timer=Game.Codec.number(i);x=Game.Codec.number(i);y=Game.Codec.number(i);originX=Game.Codec.number(i);originY=Game.Codec.number(i);width=Game.Codec.number(i);height=Game.Codec.number(i);length=Game.Codec.number(i);hit=i.readBoolean();
        if(Math.abs(direction)!=1||width<300||width>3000||height<400||height>5000||timer<0||kind!=0&&stage==0)throw new IOException("Invalid hazard");
    }
}
