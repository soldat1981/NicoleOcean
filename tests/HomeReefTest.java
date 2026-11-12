package com.nicole.ocean;
import java.util.*;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
public final class HomeReefTest {
    static int checks;static void ok(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
    static Game game(int tier){Game g=new Game();g.stage=g.highestStage=3;g.start();g.x=HomeReef.X;g.y=HomeReef.Y;g.fish.clear();g.items.clear();g.bossi.wait=g.hazard.wait=1000;g.time=30;g.home.victories=(1<<tier)-1;return g;}
    static void advance(Game g,float seconds){for(int n=0;n<(int)(seconds*20)&&g.active;n++)g.home.update(g,.05f);}
    public static void main(String[] args)throws Exception{
        Game g=game(0);g.fish.add(new Game.Fish(g.x,g.y,g.level+2,0));g.update(.01f,0,0,420,850);ok(g.hp<100,"Home has no contact immunity");
        g=game(0);g.fish.add(new Game.Fish(g.x,g.y,1,0));g.items.add(new Game.Item(g.x,g.y,7));g.update(.01f,0,0,420,850);ok(g.eaten==1&&g.xp>0,"Eat prey and crabs at home");
        g=game(0);g.hazard.begin(g,Hazard.WHIRLPOOL,420,850);g.hazard.stage=2;g.hazard.originX=g.hazard.x=g.x;g.hazard.originY=g.hazard.y=g.y;g.hazard.timer=1;g.hazard.update(g,0,420,850);ok(!g.active,"Hazards remain lethal at home");
        g=game(0);g.combat.hostile.add(CombatTest.shot(g.level+1,g.x,g.y,0,0));g.combat.update(g,.01f,420,850,g.x,g.y);ok(g.hp<100,"Enemy needles enter reef");
        g=game(0);g.home.food.add(new HomeReef.Food(g.x,g.y,0));g.home.update(g,.01f);ok(g.xp>0&&g.home.food.isEmpty(),"Home food collected");
        for(int type=3;type<=4;type++){g=game(0);g.home.food.add(new HomeReef.Food(g.x,g.y,type));g.home.update(g,.01f);ok(g.expedition.powers[type==3?1:3]==10,"Home bonus ten seconds");}
        g=game(0);g.stage=0;g.level=1;g.x=100;g.y=2500;advance(g,13);int gifts=0;for(HomeReef.Food f:g.home.food)if(f.type>=3)gifts++;ok(gifts==1,"First-ocean bonus appears after twelve seconds");
        for(int n=0;n<20000;n++){g.home.update(g,.05f);ok(g.home.food.size()<=9,"Food and gift pool bounded");}ok(g.xp==0,"Uncollected resources never award XP");
        for(int i=0;i<10;i++){HomeReef r=new HomeReef();for(int j=0;j<=i;j++)r.won(j*6+5);ok(r.tier()==i+1&&r.pending==i+1,"All ten unique milestones");r.won(i*6+5);ok(r.tier()==i+1,"Replay cannot duplicate construction");}
        g=game(0);g.stage=5;g.completeRound();ok(g.home.tier()==1&&g.home.pending==1&&g.roundWon,"Guardian victory schedules construction");g.home.pending=0;g.home.won(0);ok(g.home.pending==0,"Ordinary ocean does not build");
        g=game(10);g.stage=5;g.home.foodWait=g.home.powerWait=0;advance(g,30);ok(g.home.food.isEmpty()&&g.home.shots.isEmpty(),"Guardian puzzle gets no home food or defence");
        g=game(1);g.home.fireWait=0;Game.Fish prey=new Game.Fish(g.x+90,g.y-35,1,0);g.fish.add(prey);g.home.update(g,.01f);ok(g.home.shots.size()==1&&g.home.fireWait>5,"First house fires slowly");
        int count=g.fish.size();advance(g,1);ok(g.fish.size()==count&&prey.needleDamage>0&&prey.needleDamage<1,"Defence cannot one-shot");advance(g,23);ok(g.eaten==1&&g.shells==1&&g.xp==5,"Defence kill awards exact eating reward");
        g=game(1);g.home.fireWait=0;g.fish.add(new Game.Fish(g.x+HomeReef.RADIUS+50,g.y,1,0));advance(g,1);ok(g.home.shots.isEmpty(),"First house only targets inside");
        g=game(3);g.home.fireWait=0;g.fish.add(new Game.Fish(g.x-HomeReef.RADIUS-50,g.y,1,0));g.home.update(g,.01f);ok(!g.home.shots.isEmpty(),"Watchtower can shoot beyond reef");
        g=game(10);g.x=100;g.y=2500;g.fish.add(new Game.Fish(HomeReef.X,HomeReef.Y,1,0));advance(g,100);ok(g.fish.size()==1&&g.eaten==0&&g.home.shots.isEmpty(),"No remote defence farming");
        g=game(10);g.fish.add(new Game.Fish(g.x+50,g.y,g.level,0));advance(g,20);ok(g.fish.get(0).needleDamage==0,"Equal-level fish remain immune to defence");
        for(int tier=0;tier<=10;tier++){g=game(tier);ok(g.home.guns()==(tier==0?0:tier<6?1:tier<8?2:3),"Gun progression");ok(g.home.robotCount()==(tier<5?0:tier<7?1:tier<9?2:3),"Robot progression");}
        g=game(5);HomeReef.Robot robot=g.home.robots[0];robot.x=g.x+60;robot.y=g.y;g.fish.add(new Game.Fish(robot.x,robot.y,12,0));advance(g,3);ok(robot.hp==0&&robot.respawn>40,"Predator breaks robot");ok(g.fish.get(0).needleDamage>0,"Robot damages attacker too");g.fish.clear();advance(g,46);ok(robot.hp==60&&robot.respawn==0,"Robot respawns after repair");
        g=game(5);g.x=100;g.y=2500;g.fish.add(new Game.Fish(HomeReef.X,HomeReef.Y,12,0));advance(g,10);ok(g.home.robots[0].hp==60&&g.fish.get(0).needleDamage==0,"Idle robot does not aggress without hero");
        g=game(10);g.home.pending=10;g.home.buildTime=2.7f;g.home.robots[0].hp=0;g.home.robots[0].respawn=21;g.home.food.add(new HomeReef.Food(1100,700,3));g.home.shots.add(new Expedition.Shot(1100,600,150,0));
        Game copy=Game.restore(g.snapshot());ok(Arrays.equals(g.snapshot(),copy.snapshot()),"Construction, robot repair, food and shots save exactly");
        for(int n=0;n<1200;n++){g.home.update(g,.02f);copy.home.update(copy,.02f);ok(Arrays.equals(g.snapshot(),copy.snapshot()),"Reef deterministic after restore");}
        g=game(0);g.stage=g.highestStage=30;g.decor=3;byte[] current=g.snapshot(),old=Arrays.copyOf(current,current.length-g.home.bytes());old[7]=12;CRC32 crc=new CRC32();crc.update(old,0,old.length-8);ByteBuffer.wrap(old).putLong(old.length-8,crc.getValue());copy=Game.restore(old);ok(copy.home.tier()==5&&copy.home.pending==0&&copy.decor==3,"v12 migration restores earned construction and decorations");
        g=game(0);g.bank=10000;for(int n=0;n<8;n++){int before=g.bank,cost=HomeReef.decorCost(n);ok(g.buyDecor()&&g.decor==n+1&&g.bank==before-cost,"Eight exact decoration purchases");ok(g.home.tier()==0&&g.home.guns()==0,"Decorations are cosmetic");}int bank=g.bank;ok(!g.buyDecor()&&g.bank==bank,"No ninth purchase");
        for(int n=0;n<8;n++){g=game(0);switch(n){case 0:g.home.victories=1024;break;case 1:g.home.pending=1;break;case 2:g.home.foodWait=Float.NaN;break;case 3:g.home.robots[0].hp=61;break;case 4:g.home.robots[0].respawn=46;break;case 5:g.home.powerWait=76;break;case 6:g.home.buildTime=7;break;default:g.home.food.add(new HomeReef.Food(-1,500,0));}try{Game.restore(g.snapshot());throw new AssertionError("Invalid reef state accepted");}catch(java.io.IOException expected){checks++;}}
        for(int seed=1;seed<=12;seed++){g=new Game();g.seed=seed*781;g.start();for(int n=0;n<2400&&g.active;n++)g.update(.05f,0,0,420,850);ok(!g.roundWon,"Idle first-ocean home does not automatically win");}
        System.out.println("HomeReefTest PASS: "+checks+" assertions; vulnerability, ten milestones, eight decorations, bounded resources, defence, robots, migration and balance");
    }
}
