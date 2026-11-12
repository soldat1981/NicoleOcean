package com.nicole.ocean;

import java.io.*;
import java.util.ArrayList;

/** Persistent home progression; bounded food, slow local defence and mortal escorts. */
public final class HomeReef {
    public static final float X=1100,Y=600,RADIUS=340;
    public static final String[] BUILDINGS={"Песчаное дно","Ракушечный дом","Коралловый сад","Сторожевая башня","Жемчужный особняк","Первый придворный","Две батареи","Второй придворный","Три батареи","Третий придворный","Морской замок"};
    public static final String[] DECOR={"Россыпь ракушек","Клумба в раковине","Коралловые фонарики","Жемчужный светильник","Пузырьковый фонтан","Цветущая арка","Садовый павильон","Королевский сад"};
    public int victories,pending;public float buildTime,foodWait=4,powerWait=12,fireWait=5;
    public final ArrayList<Food> food=new ArrayList<>();
    public final ArrayList<Expedition.Shot> shots=new ArrayList<>();
    public final Robot[] robots={new Robot(),new Robot(),new Robot()};
    public static final class Food {public float x,y,life=90;public int type;public Food(float x,float y,int t){this.x=x;this.y=y;type=t;}}
    public static final class Robot {public float x=X,y=Y,hp=60,respawn,attack,hitWait;}
    public int tier(){return Integer.bitCount(victories);}
    public int robotCount(){return tier()<5?0:tier()<7?1:tier()<9?2:3;}
    public int guns(){return tier()==0?0:tier()<6?1:tier()<8?2:3;}
    public float gunX(int n){return X+(tier()<3||n==2?0:n==0?-215:215);}
    public float gunY(int n){return Y+(tier()<3?-65:n==2?-125:30);}
    public static boolean contains(float x,float y){return Game.dist(x,y,X,Y)<RADIUS;}
    public static int decorCost(int owned){return 12+12*owned+6*owned*owned;}
    public void won(int stage){if(!Oceans.guardian(stage))return;int bit=1<<(stage/6);if((victories&bit)!=0)return;victories|=bit;pending=tier();buildTime=0;}
    public void resetRun(){food.clear();shots.clear();foodWait=4;powerWait=12;fireWait=5;for(int n=0;n<3;n++){Robot r=robots[n];r.x=X+(n-1)*100;r.y=Y+180;r.hp=60;r.respawn=r.attack=r.hitWait=0;}}
    public void migrate(Game g){int n=Math.min(10,g.highestStage/6);if(g.roundWon&&g.lastStage==59)n=10;victories=(1<<n)-1;}
    public void update(Game g,float dt){
        // Guardian oceans keep their dedicated collection puzzle, with no home farming or defence.
        if(g.guardianOcean())return;
        foodWait=Math.max(0,foodWait-dt);powerWait=Math.max(0,powerWait-dt);fireWait=Math.max(0,fireWait-dt);
        int snacks=0,bonuses=0;for(Food f:food)if(f.type<3)snacks++;else bonuses++;
        if(foodWait==0){foodWait=g.between(10,16);if(snacks<8){float a=g.between(0,6.2832f),r=g.between(55,270);food.add(new Food(X+(float)Math.cos(a)*r,Y+(float)Math.sin(a)*r,(int)(g.random()*3)));}}
        if(powerWait==0){powerWait=g.between(55,75);if(bonuses==0){float a=g.between(0,6.2832f);Food f=new Food(X+(float)Math.cos(a)*210,Y+(float)Math.sin(a)*210,g.random()<.5f?3:4);f.life=22;food.add(f);}}
        for(int n=food.size()-1;n>=0;n--){Food f=food.get(n);f.life-=dt;
            if(Game.dist(g.x,g.y,f.x,f.y)<Game.radius(g.level)+12){food.remove(n);g.snackSerial++;g.burst(f.x,f.y,0xffa9f8d6,8);if(f.type<3)g.addXp(2+g.level/6);else{int power=f.type==3?1:3;g.expedition.powers[power]=Math.max(g.expedition.powers[power],10);g.tell((power==1?"Риф подарил щит":"Риф подарил иглы")+" · 10 секунд");}if(!g.active)return;}
            else if(f.life<=0)food.remove(n);
        }
        boolean nearby=g.reefDistance()<RADIUS+120;
        if(!nearby){shots.clear();}
        if(nearby&&guns()>0&&fireWait==0&&shots.size()<4){
            int gun=(int)(g.time/4)%guns();float sx=gunX(gun),sy=gunY(gun);Game.Fish target=null;float best=Float.MAX_VALUE;
            for(Game.Fish f:g.fish){float d=Game.dist(sx,sy,f.x,f.y);if(f.level==g.level||d>470||Game.dist(f.x,f.y,X,Y)>(tier()<3?RADIUS:RADIUS+130))continue;
                float priority=d+(f.level>g.level?-160:0);if(priority<best){best=priority;target=f;}}
            if(target!=null){float d=Math.max(1,Game.dist(sx,sy,target.x,target.y));Expedition.Shot s=new Expedition.Shot(sx,sy,(target.x-sx)/d*260,(target.y-sy)/d*260);s.life=2;s.level=Oceans.startLevel(g.stage);shots.add(s);fireWait=tier()<3?5.5f:tier()<8?4.5f:3.5f;}
        }
        for(int n=shots.size()-1;n>=0;n--){Expedition.Shot s=shots.get(n);float ox=s.x,oy=s.y;s.x+=s.vx*dt;s.y+=s.vy*dt;s.life-=dt;Game.Fish target=null;float first=Float.POSITIVE_INFINITY;
            for(Game.Fish f:g.fish){if(f.level==g.level||Game.dist(f.x,f.y,X,Y)>(tier()<3?RADIUS:RADIUS+130))continue;float t=Combat.intersection(ox,oy,s.x,s.y,f.x,f.y,Game.radius(f.level)+3);if(t<first){first=t;target=f;}}
            if(target!=null){strike(g,target,.6f*Combat.damage(s.level,target.level));s.life=0;}
            if(s.life<=0||!nearby)shots.remove(n);if(!g.active)return;
        }
        for(int n=0;n<robotCount();n++){
            Robot r=robots[n];r.attack=Math.max(0,r.attack-dt);r.hitWait=Math.max(0,r.hitWait-dt);
            if(r.hp<=0){r.respawn=Math.max(0,r.respawn-dt);if(r.respawn==0){r.hp=60;r.x=X+(n-1)*100;r.y=Y+180;g.burst(r.x,r.y,0xff9deedc,12);}continue;}
            Game.Fish target=null;float best=260;
            if(nearby)for(Game.Fish f:g.fish)if(f.level>g.level&&Game.dist(f.x,f.y,g.x,g.y)<230&&Game.dist(f.x,f.y,X,Y)<RADIUS+140){float d=Game.dist(r.x,r.y,f.x,f.y);if(d<best){best=d;target=f;}}
            float tx=target==null?X+(n-1)*100:target.x,ty=target==null?Y+180:target.y,d=Math.max(1,Game.dist(r.x,r.y,tx,ty));float step=Math.min(d,dt*(target==null?65:125));r.x+=(tx-r.x)/d*step;r.y+=(ty-r.y)/d*step;
            if(target!=null&&d<Game.radius(target.level)+27){
                if(r.hitWait==0){r.hp=Math.max(0,r.hp-(14+Math.max(0,target.level-Oceans.startLevel(g.stage))*5));r.hitWait=1.2f;if(r.hp==0){r.respawn=45;g.burst(r.x,r.y,0xffffbb8e,14);}}
                if(r.hp>0&&r.attack==0){r.attack=1.8f;strike(g,target,.5f*Combat.damage(Oceans.startLevel(g.stage),target.level));if(!g.active)return;}
            }
        }
    }
    private void strike(Game g,Game.Fish fish,float damage){fish.needleDamage=Math.min(1,fish.needleDamage+damage);g.burst(fish.x,fish.y,0xffffd58b,5);if(fish.needleDamage>=.99999f){g.fish.remove(fish);g.addXp(g.fishReward(fish));}}
    public int bytes(){return 104+food.size()*16+shots.size()*24;}
    public void write(DataOutputStream o)throws IOException{
        o.writeInt(victories);o.writeInt(pending);for(float f:new float[]{buildTime,foodWait,powerWait,fireWait})o.writeFloat(f);
        o.writeInt(food.size());for(Food f:food){o.writeFloat(f.x);o.writeFloat(f.y);o.writeFloat(f.life);o.writeInt(f.type);}
        o.writeInt(shots.size());for(Expedition.Shot s:shots){for(float f:new float[]{s.x,s.y,s.vx,s.vy,s.life})o.writeFloat(f);o.writeInt(s.level);}
        for(Robot r:robots)for(float f:new float[]{r.x,r.y,r.hp,r.respawn,r.attack,r.hitWait})o.writeFloat(f);
    }
    private static float read(DataInputStream i,float min,float max)throws IOException{float f=Game.Codec.number(i);if(f<min||f>max)throw new IOException("Invalid home reef state");return f;}
    public void read(DataInputStream i)throws IOException{
        victories=Game.Codec.range(i.readInt(),0,1023);pending=Game.Codec.range(i.readInt(),0,10);buildTime=read(i,0,6);foodWait=read(i,0,16);powerWait=read(i,0,75);fireWait=read(i,0,6);
        if(pending>tier())throw new IOException("Invalid construction milestone");
        int count=Game.Codec.range(i.readInt(),0,9);for(int n=0;n<count;n++){Food f=new Food(read(i,0,Game.WORLD_W),read(i,0,Game.WORLD_H),0);f.life=read(i,0,90);f.type=Game.Codec.range(i.readInt(),0,4);food.add(f);}
        count=Game.Codec.range(i.readInt(),0,4);for(int n=0;n<count;n++){Expedition.Shot s=new Expedition.Shot(read(i,0,Game.WORLD_W),read(i,0,Game.WORLD_H),read(i,-260,260),read(i,-260,260));s.life=read(i,0,2);s.level=Game.Codec.range(i.readInt(),1,55);shots.add(s);}
        for(Robot r:robots){r.x=read(i,0,Game.WORLD_W);r.y=read(i,0,Game.WORLD_H);r.hp=read(i,0,60);r.respawn=read(i,0,45);r.attack=read(i,0,1.8f);r.hitWait=read(i,0,1.2f);}
    }
}
