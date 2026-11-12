package com.nicole.ocean;
import android.content.Context;
import android.graphics.*;
/** One cached ornament atlas plus animated game-native encounter effects. */
final class ExpeditionArt {
 final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 final Rect src=new Rect();final RectF dst=new RectF();final Bitmap ornaments;
 static final int[] GUARD_FORMS={36,19,20,15,18,34,22,10,26,55};
 static final int[] COLORS={0xffa7f4d4,0xffa8caff,0xffffdfa0,0xffddb0fa,0xffffb4b8};
 ExpeditionArt(Context c){ornaments=BitmapFactory.decodeResource(c.getResources(),c.getResources().getIdentifier("ocean_ornaments","drawable",c.getPackageName()));}
 void scenery(Canvas c,MainActivity.OceanView v){
  int level=v.g.stage+1;float t=v.clock;
  // Each level has a deterministic hue, wave cadence, species and placement signature.
  int tint=Color.HSVToColor(17,new float[]{155+(level*17)%100,.65f,.7f});v.fill(tint);c.drawRect(0,0,v.w,v.h,v.p);
  for(int n=0;n<32;n++){float xx=Oceans.decorX(v.g.stage,n)-v.cameraX,yy=Oceans.decorY(v.g.stage,n)-v.cameraY;
   if(xx< -80||xx>v.w+80||yy< -80||yy>v.h+80)continue;
   int cell=(level+n)%6;if(ornaments!=null){int cw=ornaments.getWidth()/3,ch=ornaments.getHeight()/2;src.set(cell%3*cw,cell/3*ch,(cell%3+1)*cw,(cell/3+1)*ch);dst.set(xx-68,yy-65,xx+68,yy+65);p.setAlpha(170);c.drawBitmap(ornaments,src,dst,p);p.setAlpha(255);}
  }
  for(int row=0;row<4;row++)for(int n=0;n<16;n++){float xx=n*30,yy=30+row*190+(float)Math.sin(t*(.45f+level%5*.08f)+n*.5f+level)*8;v.line(c,xx,yy,xx+20,yy+2,1,0x177ef9ed);}
  for(int n=0;n<18;n++){float xx=(n*83+level*19)%420,yy=(n*137+level*43-t*(4+level%7))%(v.h+40);v.circle(c,xx,yy,1+n%2,0x449af3e6);}
 }
 void world(Canvas c,MainActivity.OceanView v){
  Expedition e=v.g.expedition;
  for(Expedition.Drop d:e.drops){float yy=d.y+(float)Math.sin(v.clock*3+d.x)*4;int col=d.type==0?0xffffe4a6:COLORS[d.type-1];v.circle(c,d.x,yy,19,0x2046ffe0);v.outline(c,d.x,yy,15,col,1.5f);
   if(d.type==0){switch(e.kind){
    case 0: for(int n=0;n<3;n++)v.circle(c,d.x+(n-1)*5,yy+(n%2)*4,4,col);break;
    case 1:v.circle(c,d.x,yy,8,0xfff3eaff);v.circle(c,d.x-2,yy-3,3,0xffffffff);break;
    case 2:v.oval(c,d.x,yy,5,9,0xffc7e8ff);break;
    case 3:v.oval(c,d.x-3,yy,4,8,0xff9ff7bb);v.oval(c,d.x+3,yy,4,8,0xff9ff7bb);break;
    case 4:v.star(c,d.x,yy,10,col);break;
    case 5:v.line(c,d.x,yy+8,d.x,yy-8,3,0xffffafca);v.line(c,d.x,yy,d.x-7,yy-6,3,0xffffafca);v.line(c,d.x,yy,d.x+7,yy-6,3,0xffffafca);break;
    case 6:v.line(c,d.x+4,yy-9,d.x-4,yy,3,col);v.line(c,d.x-4,yy,d.x+4,yy,3,col);v.line(c,d.x+4,yy,d.x-4,yy+9,3,col);break;
    case 7:v.outline(c,d.x,yy,8,0xffa8ecff,2);v.circle(c,d.x-3,yy-3,2,0xffffffff);break;
    case 9:v.star(c,d.x,yy,11,0xffb6ffef);v.circle(c,d.x,yy,4,0xffffffff);break;
    default:v.shell(c,d.x,yy,9,col);
   }}else{String[] marks={">>","Щ","∞","↑","М"};v.text(c,marks[d.type-1],d.x,yy+5,14,col,true,true);}
  }
  for(Expedition.Shot s:e.shots){float d=Math.max(1,(float)Math.hypot(s.vx,s.vy));v.line(c,s.x-s.vx/d*16,s.y-s.vy/d*16,s.x,s.y,3,0xffffefa9);v.circle(c,s.x,s.y,3,0xffffffff);}
  for(Expedition.Shot s:v.g.combat.hostile){float d=Math.max(1,(float)Math.hypot(s.vx,s.vy));v.line(c,s.x-s.vx/d*22,s.y-s.vy/d*22,s.x,s.y,4,0xffff846a);v.circle(c,s.x,s.y,5,0xffffe0bb);}
  if(e.state==1){
   float pulse=1+(float)Math.sin(e.age*3)*.035f;v.outline(c,e.x,e.y,73, e.age<1.8f?0x88ffda99:0x337fffff,2);
   c.save();c.translate(e.x,e.y);c.scale(v.g.x<e.x?-1:1,1);int[] palette=v.heroArt.palette;v.heroArt.palette=null;v.heroArt.draw(c,GUARD_FORMS[e.kind],e.kind%3,100*pulse,v.clock);v.heroArt.palette=palette;c.restore();
   float labelTop=e.y-145;if(labelTop<v.cameraY+242)labelTop=e.y+125;if(Math.abs(labelTop+22-v.g.y)<Game.radius(v.g.level)+45)labelTop=v.g.y+Game.radius(v.g.level)+35;float labelX=Game.clamp(e.x,v.cameraX+110,v.cameraX+v.w-110);
   v.box(c,labelX-108,labelTop,216,45,14,0xe007354d);v.text(c,Expedition.NAMES[e.kind]+" · "+e.targetLevel,labelX,labelTop+21,13,0xffffdfa0,true,true);
   v.text(c,v.g.level>e.targetLevel?"Теперь можно съесть!":e.distracted()?"Отвлёкся — собирай добычу!":"Собирай "+Expedition.LOOT[e.kind],labelX,labelTop+38,10,0xffb3f7df,true,true);
   if(v.g.combat.guardianDamage>0){v.box(c,labelX-70,labelTop+48,140,5,2,0xff532f43);v.box(c,labelX-70,labelTop+48,140*Math.max(0,1-v.g.combat.guardianDamage),5,2,0xffffd58b);}
   if(e.stun>0)for(int n=0;n<3;n++)v.star(c,e.x-25+n*25,e.y-48+(float)Math.sin(v.clock*5+n)*4,5,0xffffdfa0);
  }
  for(int n=0;n<5;n++)if(e.powers[n]>0){v.outline(c,v.g.x,v.g.y,Game.radius(v.g.level)+17+n*3,COLORS[n],n==2?3:1.5f);}
 }
 void overlay(Canvas c,MainActivity.OceanView v){
  Expedition e=v.g.expedition;int slot=0;
  for(int n=0;n<5;n++)if(e.powers[n]>0){float xx=22+slot++*78;v.box(c,xx,198,74,32,10,0xdd093b50);v.text(c,Expedition.POWERS[n],xx+37,210,8,COLORS[n],true,true);v.text(c,(int)Math.ceil(e.powers[n])+" с",xx+37,224,10,0xffffffff,true,true);}
  if(e.state==1){float dx=e.x-v.g.x,dy=e.y-v.g.y,d=Math.max(1,(float)Math.hypot(dx,dy));if(e.x<v.cameraX||e.x>v.cameraX+v.w||e.y<v.cameraY||e.y>v.cameraY+v.h){v.text(c,"Хранитель "+(Math.abs(dx)>Math.abs(dy)?(dx<0?"←":"→"):(dy<0?"↑":"↓")),210,248,12,0xffffdfa0,true,true);}}
  if(e.celebrate<=0)return;
  float progress=(4-e.celebrate)/4,cy=Math.min(v.h*.43f,330);int act=(v.g.stage/6)%5;
  v.box(c,70,cy-93,280,188,25,0xdf0b435b);v.text(c,"ХРАНИТЕЛЬ ПОБЕЖДЁН!",210,cy+73,15,0xffffdfa0,true,true);
  c.save();c.translate(210,cy-12);if(act==2)c.rotate(progress*720);else c.rotate((float)Math.sin(progress*12)*7);
  float bounce=(float)Math.sin(progress*Math.PI)*12;c.translate(0,-bounce);
  if((v.g.stage/6)%2==0){Rect r=v.nicoleArt.faces[e.kind%3];float hh=115f*r.height()/r.width();dst.set(-57,-hh/2,57,hh/2);p.setAlpha(255);c.drawBitmap(v.nicoleArt.atlas,r,dst,p);}else v.bossiArt.part(c,0,0,55,115);
  if(act==0){v.line(c,8,-8,31,-8,4,0xff103c51);v.star(c,65,-30,8,0xffffdfa0);} // playful wink overlay
  if(act==1){v.box(c,52,8,28,29,8,0xffffd8b2);v.line(c,80,22,82,-12,10,0xffffd8b2);for(int n=0;n<3;n++)v.line(c,56,15+n*7,69,15+n*7,1,0xffce956f);}
  if(act==3){v.circle(c,65,0,10,0xffffa9c7);v.circle(c,79,0,10,0xffffa9c7);v.line(c,60,7,72,23,12,0xffffa9c7);v.line(c,84,7,72,23,12,0xffffa9c7);}
  if(act==4){for(int n=0;n<5;n++)v.star(c,-40+n*20,-65+(n%2)*8,8,0xffffdfa0);}
  c.restore();
  for(int n=0;n<18;n++){double a=n*2.399;float r=45+progress*135;v.star(c,210+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r,3+n%3,COLORS[(n+e.kind)%5]);}
 }
}
