package com.nicole.ocean;
import java.io.*;
import java.util.*;
/** Bounded, deterministic guardian encounters, loot and temporary powers. */
public final class Expedition {
 public static final String[] NAMES={"Осьминог","Жемчужный краб","Лунная медуза","Черепаха-садовник","Кальмар-звездочёт","Коралловый омар","Электрический скат","Морской конёк","Рак-отшельник","Древний левиафан"};
 public static final String[] LOOT={"икру","жемчужины","капли света","семена","звёзды","кораллы","искры","пузырьки","ракушки","кристаллы"};
 public static final String[] POWERS={"Ускорение","Щит","Бессмертие","Иглы","Магнит"};
 public int state,kind,targetLevel,collected; // 0 waiting, 1 active, 2 defeated (once per round)
 public float x,y,age,dropTimer,celebrate,powerWait=16,shotWait,stun;
 public final float[] powers=new float[5];
 public final ArrayList<Drop> drops=new ArrayList<>();
 public final ArrayList<Shot> shots=new ArrayList<>();
 public static final class Drop {public float x,y,life=24;public int type;Drop(float x,float y,int type){this.x=x;this.y=y;this.type=type;}}
 public static final class Shot {public int level=1;public float x,y,vx,vy,life=1.2f;Shot(float x,float y,float vx,float vy){this.x=x;this.y=y;this.vx=vx;this.vy=vy;}}
 public void reset(){state=kind=targetLevel=collected=0;x=y=age=dropTimer=celebrate=shotWait=stun=0;powerWait=16;Arrays.fill(powers,0);drops.clear();shots.clear();}
 public boolean distracted(){return state==1&&age%11>=6;}
 public boolean busy(){return state==1||celebrate>0;}
 public void begin(Game g){g.combat.guardianDamage=0;state=1;kind=g.stage/6;targetLevel=Oceans.goalLevel(g.stage)-1;age=0;dropTimer=.4f;x=Game.clamp(g.x+240,100,Game.WORLD_W-100);y=Game.clamp(g.y+130,700,Game.WORLD_H-100);g.tell("Хранитель: "+NAMES[kind]+"! Собирай "+LOOT[kind]);}
 public void defeat(Game g){if(state!=1)return;state=2;celebrate=4;g.invincible=Math.max(g.invincible,4);g.shells+=15+g.stage;g.burst(x,y,0xffffd58b,60);g.tell("Хранитель побеждён! +"+(15+g.stage)+" ракушек");}
 public void grant(int type){powers[type]=type==1||type==2||type==3?15:24;}
 public boolean block(){if(powers[2]>0)return true;if(powers[1]>0){powers[1]=0;return true;}return false;}
 public void update(Game g,float dt){
  for(int n=0;n<5;n++)powers[n]=Math.max(0,powers[n]-dt);
  celebrate=Math.max(0,celebrate-dt);stun=Math.max(0,stun-dt);
  if(state==0&&g.guardianOcean()&&g.time>1&&!g.bossi.visible()&&!g.nicole.visible&&!g.hazard.visible())begin(g);
  powerWait-=dt;
  if(powerWait<=0&&drops.size()<24){powerWait=g.between(18,28);float a=g.between(0,6.2832f);drops.add(new Drop(Game.clamp(g.x+(float)Math.cos(a)*180,70,Game.WORLD_W-70),Game.clamp(g.y+(float)Math.sin(a)*180,650,Game.WORLD_H-70),1+(int)(g.random()*5)));}
  if(state==1){
   age+=dt;dropTimer-=dt;float dx=g.x-x,dy=g.y-y,d=Math.max(1,Game.dist(x,y,g.x,g.y));
   boolean chasing=g.level<=targetLevel&&g.hiddenTime<1;
   float speed=stun>0?0:age<1.8f?0:distracted()?28:chasing?95+kind*3:45;
   if(distracted()){dx=(float)Math.cos(age*.7f);dy=(float)Math.sin(age*.7f);d=1;}
   float turn=kind==2||kind==4?(float)Math.sin(age*1.4f)*.65f:0;
   x=Game.clamp(x+(dx/d-turn*dy/d)*speed*dt,90,Game.WORLD_W-90);y=Game.clamp(y+(dy/d+turn*dx/d)*speed*dt,190,Game.WORLD_H-90);
   if(dropTimer<=0&&drops.size()<24){dropTimer=distracted()?.65f:1.8f+kind*.08f;float a=age*2.4f;drops.add(new Drop(Game.clamp(x+(float)Math.cos(a)*110,60,Game.WORLD_W-60),Game.clamp(y+(float)Math.sin(a)*110,650,Game.WORLD_H-60),0));}
   if(age>1.8f&&Game.dist(g.x,g.y,x,y)<Game.radius(g.level)+82){
    if(g.level>targetLevel)defeat(g);
    else if(g.hiddenTime<1&&stun<=0&&!distracted())g.hurt((18+Difficulty.extraDamage(g.stage))*(g.calm?.5f:1));
   }
  }
  if(!g.active)return;
  for(int n=drops.size()-1;n>=0;n--){Drop d=drops.get(n);d.life-=dt;float distance=Game.dist(g.x,g.y,d.x,d.y);
   if(powers[4]>0&&distance<240){float pull=Math.min(distance,180*dt);d.x+=(g.x-d.x)/Math.max(1,distance)*pull;d.y+=(g.y-d.y)/Math.max(1,distance)*pull;}
   if(Game.dist(g.x,g.y,d.x,d.y)<Game.radius(g.level)+16){
    drops.remove(n);g.snackSerial++;g.burst(d.x,d.y,0xffa8ffe3,12);
    if(d.type==0){collected++;g.addXp(Game.threshold(g.level)*.08f);}else{grant(d.type-1);g.tell(POWERS[d.type-1]+" — ненадолго!");}
   }else if(d.life<=0)drops.remove(n);
  }
  if(state==2&&celebrate==0&&g.guardianOcean()){g.completeRound();return;}
  shotWait=Math.max(0,shotWait-dt);
  if(powers[3]>0&&shotWait==0&&shots.size()<16){
   float tx=0,ty=0,best=420;
   for(Game.Fish f:g.fish)if(f.level!=g.level){float d=Game.dist(g.x,g.y,f.x,f.y);if(d<best){best=d;tx=f.x;ty=f.y;}}
   if(state==1&&Game.dist(g.x,g.y,x,y)<best){best=Game.dist(g.x,g.y,x,y);tx=x;ty=y;}
   if(best<420){Shot shot=new Shot(g.x,g.y,(tx-g.x)/Math.max(1,best)*360,(ty-g.y)/Math.max(1,best)*360);shot.level=g.level;shots.add(shot);shotWait=.45f;}
  }
  for(int n=shots.size()-1;n>=0;n--){Shot s=shots.get(n);float ox=s.x,oy=s.y;s.x+=s.vx*dt;s.y+=s.vy*dt;s.life-=dt;
   g.combat.friendly(g,s,ox,oy);
   if(s.life<=0)shots.remove(n);
   if(!g.active)return;
  }
 }
 public int bytes(){return 16+9*4+5*4+4+drops.size()*16+4+shots.size()*20;}
 public void write(DataOutputStream o)throws IOException{for(int v:new int[]{state,kind,targetLevel,collected})o.writeInt(v);for(float v:new float[]{x,y,age,dropTimer,celebrate,powerWait,shotWait,stun,0})o.writeFloat(v);for(float v:powers)o.writeFloat(v);o.writeInt(drops.size());for(Drop d:drops){o.writeFloat(d.x);o.writeFloat(d.y);o.writeFloat(d.life);o.writeInt(d.type);}o.writeInt(shots.size());for(Shot s:shots)for(float v:new float[]{s.x,s.y,s.vx,s.vy,s.life})o.writeFloat(v);}
 public void read(DataInputStream i)throws IOException{
  state=Game.Codec.range(i.readInt(),0,2);kind=Game.Codec.range(i.readInt(),0,9);targetLevel=Game.Codec.range(i.readInt(),0,230);collected=Game.Codec.range(i.readInt(),0,1000000);
  x=Game.Codec.number(i);y=Game.Codec.number(i);age=timer(i,1e8f);dropTimer=Game.Codec.number(i);celebrate=timer(i,4);powerWait=Game.Codec.number(i);shotWait=timer(i,1);stun=timer(i,2);Game.Codec.number(i);
  if(x<0||x>Game.WORLD_W||y<0||y>Game.WORLD_H)throw new IOException("Guardian position");
  for(int n=0;n<5;n++)powers[n]=timer(i,36);
  powers[1]=Math.min(powers[1],15);powers[3]=Math.min(powers[3],15);
  int count=Game.Codec.range(i.readInt(),0,24);for(int n=0;n<count;n++){Drop d=new Drop(Game.Codec.number(i),Game.Codec.number(i),0);d.life=timer(i,24);d.type=Game.Codec.range(i.readInt(),0,5);if(d.x<0||d.x>Game.WORLD_W||d.y<0||d.y>Game.WORLD_H)throw new IOException("Drop position");drops.add(d);}
  count=Game.Codec.range(i.readInt(),0,16);for(int n=0;n<count;n++){Shot s=new Shot(Game.Codec.number(i),Game.Codec.number(i),Game.Codec.number(i),Game.Codec.number(i));s.life=timer(i,1.2f);shots.add(s);}
 }
 private static float timer(DataInputStream i,float max)throws IOException{float v=Game.Codec.number(i);if(v<0||v>max)throw new IOException("Power timer");return v;}
}
