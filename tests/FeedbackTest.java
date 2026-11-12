package com.nicole.ocean;

/** Player feedback regressions: harbor, depth, contact, checkpoints and visitor geometry. */
public final class FeedbackTest {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static Game empty(){Game g=new Game();g.start();g.fish.clear();g.items.clear();g.x=1100;g.y=1500;g.bossi.wait=g.hazard.wait=10000;return g;}
    public static void main(String[] args)throws Exception{
        Game opening=new Game();opening.start();
        for(int n=0;n<12;n++){Game.Item food=opening.items.get(n);check(!Game.inHarbor(food.x,food.y),"Opening food is outside harbor and can be eaten");}
        for(int type:new int[]{0,4,5,6,7}){
            Game g=empty();g.x=1100;g.y=380;g.level=3;
            Game.Item food=new Game.Item(g.x,g.y,type);g.items.add(food);
            Game.Fish prey=new Game.Fish(g.x,g.y,1,0);g.fish.add(prey);
            g.update(.01f,0,0,420,850);
            check(g.eaten==0&&g.xp==0&&g.items.contains(food)&&g.fish.contains(prey),"Harbor protects prey and food type "+type);
        }
        Game g=empty();g.y=592;g.level=3;Game.Fish sheltered=new Game.Fish(1100,580,1,0);g.fish.add(sheltered);
        g.update(.01f,0,0,420,850);check(g.eaten==0,"Cannot reach across harbor border to eat sheltered prey");
        for(float age:new float[]{1,25}){
            g=empty();g.time=age;g.fish.add(new Game.Fish(g.x+Game.radius(1)+Game.radius(4)-3,g.y,4,0));
            g.update(.01f,0,0,420,850);check(g.hp<100,"Visible grazing contact damages at time "+age);
        }
        g=empty();g.bossi.begin(g,Bossi.HEAD,420,850);g.bossi.stage=Bossi.CALL;
        g.fish.add(new Game.Fish(g.x,g.y,4,0));g.update(.01f,0,0,420,850);
        check(g.hp<100,"Cat visit does not disable fish contact damage");
        g=empty();Game.Fish equal=new Game.Fish(g.x,g.y,1,0);g.fish.add(equal);
        float before=g.x;g.update(.02f,0,0,420,850);
        check(g.hp==100&&g.eaten==0&&g.fish.contains(equal)&&g.x!=before,"Equal fish push without damage or eating");
        g=empty();Game.Fish a=new Game.Fish(700,1400,4,0),b=new Game.Fish(700,1400,4,1);a.turn=b.turn=100;g.fish.add(a);g.fish.add(b);
        for(int i=0;i<60;i++)g.update(1f/60,0,0,420,850);
        check(g.fish.contains(a)&&g.fish.contains(b)&&Game.dist(a.x,a.y,b.x,b.y)>40,"Overlapping NPC fish separate gently and survive");
        int top=0,bottom=0,topLevels=0,bottomLevels=0;
        for(int seed=1;seed<=100;seed++){
            g=new Game();g.seed=seed;g.stage=3;g.start();
            check(g.fish.size()<=Game.POPULATION,"Reduced global population cap");
            for(Game.Fish f:g.fish){if(f.y<900){top++;topLevels+=f.level;}if(f.y>2300){bottom++;bottomLevels+=f.level;}}
        }
        check(bottom>top,"Deep water has more fish than similarly sized shallow band");
        check(bottomLevels/(float)bottom>topLevels/(float)top+2,"Deep water fish are substantially stronger");
        check(Game.localPopulation(500)<Game.localPopulation(2500),"Local director increases population with depth");
        g=empty();g.stage=g.highestStage=2;g.stageLosses=2;
        byte[] legacy=LegacyFixtures.v9(g);legacy[7]=7;
        java.util.zip.CRC32 crc=new java.util.zip.CRC32();crc.update(legacy,0,legacy.length-8);
        java.nio.ByteBuffer.wrap(legacy,legacy.length-8,8).putLong(crc.getValue());
        g=Game.restore(legacy);g.start();g.hurt(1000);
        check(g.stage==2&&g.stageLosses==3,"Version 7 progress gets five-loss rule without resetting streak");
        g=empty();for(int n=0;n<Game.MAX_FISH;n++)g.fish.add(new Game.Fish(100+n,2500,1,0));
        for(int n=0;n<45;n++)g.update(.01f,0,0,420,850);
        check(g.fish.size()==Game.POPULATION,"Old dense save thins distant fish to new cap");
        for(int stage=1;stage<=8;stage++){
            g=new Game();g.stage=g.highestStage=stage;
            for(int loss=1;loss<=5;loss++){
                g.start();g.hurt(1000);g=Game.restore(g.snapshot());
                check(g.stage==(loss<5?stage:stage-1),"Exactly fifth loss demotes stage "+stage);
                check(g.stageLosses==(loss<5?loss:0),"Loss count survives save at "+loss);
            }
        }
        for(int kind=1;kind<=4;kind++){
            g=empty();g.bossi.begin(g,kind,420,850);g.bossi.stage=Bossi.AIM;
            float anchorX=g.bossi.x,anchorY=g.bossi.y;g.x+=300;g.y=2400;g.bossi.update(g,.01f,420,850);
            float camera=Game.clamp(g.y-850*.46f,80,Game.WORLD_H-850+30);
            check(g.bossi.x==anchorX&&g.bossi.y==anchorY,"Cat stays anchored in world when camera moves");
            g.bossi.stage=Bossi.STRIKE;g.bossi.timer=.48f*.45f;g.bossi.targetX=g.x;g.bossi.targetY=g.y;
            if(kind==Bossi.HEAD_PAWS){check(g.bossi.attacksLeft()&&g.bossi.attacksRight(),"Both paws attack together");}
            g.bossi.collide(g);check(g.hp<100,"Attacking paw causes damage for variant "+kind);
        }
        for(int kind=1;kind<=4;kind++){
            g=empty();g.bossi.begin(g,kind,420,850);
            g.bossi.update(g,.01f,420,850);
            check(g.bossi.stage==Bossi.ARRIVE,"Cat emerges immediately");
            float anchorX=g.bossi.x,anchorY=g.bossi.y;
            g.x=2000;g.y=2600;
            for(int step=0;step<250;step++){
                g.bossi.update(g,.02f,420,850);
                check(g.bossi.x==anchorX&&g.bossi.y==anchorY,"Cat never follows fleeing player");
                if(step%50==0)g=Game.restore(g.snapshot());
            }
        }
        g=empty();g.bossi.begin(g,Bossi.HEAD,420,850);Bossi cat=g.bossi;
        cat.stage=Bossi.STRIKE;cat.targetX=cat.x+80;cat.targetY=cat.y+420;
        cat.timer=0;float startX=cat.pawX(true),startY=cat.pawY(true);
        cat.timer=cat.duration()*.225f;
        check(cat.diagonal()&&cat.pawX(true)>startX+100&&cat.pawY(true)>startY+100,"Diagonal swipe travels across and down");
        check(Bossi.PAW_WIDTH==260&&Bossi.PAW_RADIUS==70,"Paw size and collision radius doubled");
        g.x=cat.pawX(true)+60;g.y=cat.pawY(true);cat.collide(g);
        check(g.hp<100,"Outer portion of enlarged paw hits");
        g=empty();g.bossi.begin(g,Bossi.HEAD,420,850);cat=g.bossi;cat.stage=Bossi.STRIKE;
        cat.targetX=cat.x;cat.targetY=cat.y+400;cat.timer=cat.duration()*.45f;
        g.x=cat.pawX(true)+Bossi.PAW_RADIUS+Game.radius(g.level)+5;g.y=cat.pawY(true);
        cat.collide(g);check(g.hp==100,"Outside enlarged paw remains safe");
        for(float h:new float[]{400,480,640,850,1100})for(float xx:new float[]{100,1100,2100})for(float yy:new float[]{200,1500,2800}){
            g=empty();g.x=xx;g.y=yy;g.nicole.begin(g,420,h);
            float cx=Game.clamp(xx-210,0,1780),cy=Game.clamp(yy-h*.46f,80,Game.WORLD_H-h+30);
            check(g.nicole.x-g.nicole.width/2>=cx&&g.nicole.x+g.nicole.width/2<=cx+420,"Nicole fits horizontally at world edge");
            check(g.nicole.y>cy+150&&g.nicole.y<cy+h-80,"Nicole appears in usable camera area");
            Game.restore(g.snapshot());
        }
        System.out.println("PASS: "+checks+" player-feedback regressions");
    }
}
