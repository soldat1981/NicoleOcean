package com.nicole.ocean;
import android.content.Context;
import android.graphics.*;

/** World-anchored buildings and a fitted, animated overview of the same home. */
final class HomeReefArt {
    final Bitmap buildings,decor;final Rect[] cells=new Rect[12],ornaments=new Rect[8];
    final Paint paint=new Paint(3);final RectF dst=new RectF();
    final Shader sand=new RadialGradient(HomeReef.X-50,HomeReef.Y+50,360,new int[]{0xffdfd4ad,0xffc3c09a,0xff529d96},null,Shader.TileMode.CLAMP);
    HomeReefArt(Context c){buildings=load(c,"home_reef_buildings");decor=load(c,"home_reef_decor");grid(buildings,cells,4,3);grid(decor,ornaments,4,2);}
    Bitmap load(Context c,String name){return BitmapFactory.decodeResource(c.getResources(),c.getResources().getIdentifier(name,"drawable",c.getPackageName()));}
    void grid(Bitmap image,Rect[] cells,int cols,int rows){for(int n=0;n<cells.length;n++){
        int left=n%cols*image.getWidth()/cols,top=n/cols*image.getHeight()/rows,right=(n%cols+1)*image.getWidth()/cols,bottom=(n/cols+1)*image.getHeight()/rows,w=right-left,h=bottom-top;
        int[] pixels=new int[w*h];image.getPixels(pixels,0,w,left,top,w,h);int minX=w,minY=h,maxX=0,maxY=0;
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)if((pixels[y*w+x]>>>24)>48){minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);}
        cells[n]=minX<maxX?new Rect(left+minX,top+minY,left+maxX+1,top+maxY+1):new Rect(left,top,right,bottom);
    }}
    void sprite(Canvas c,int id,float x,float bottom,float width,float alpha){draw(c,buildings,cells[id],x,bottom,width,alpha);}
    void draw(Canvas c,Bitmap b,Rect src,float x,float bottom,float width,float alpha){float height=width*src.height()/src.width();dst.set(x-width/2,bottom-height,x+width/2,bottom);paint.setColor(Color.WHITE);paint.setAlpha((int)(255*Game.clamp(alpha,0,1)));c.drawBitmap(b,src,dst,paint);}
    void scene(Canvas c,MainActivity.OceanView v,int tier,int decoration,float alpha){
        float x=HomeReef.X,y=HomeReef.Y;
        v.oval(c,x,y+70,335,260,0x345dc5b9);v.oval(c,x,y+100,320,230,0x667d9f8c);
        paint.setColor(Color.WHITE);paint.setAlpha(255);paint.setShader(sand);dst.set(x-308,y-120,x+308,y+315);c.drawOval(dst,paint);paint.setShader(null);
        for(int n=0;n<9;n++){v.fill(0x33fff2c7);v.p.setStyle(Paint.Style.STROKE);v.p.setStrokeWidth(1.4f);v.rect.set(x-290+n*10,y-105+n*35,x+280-n*7,y+120+n*22);c.drawArc(v.rect,18,145,false,v.p);v.p.setStyle(Paint.Style.FILL);}
        for(int n=0;n<32;n++){float a=n*2.39996f,r=100+(n*73)%200;v.circle(c,x+(float)Math.cos(a)*r,y+85+(float)Math.sin(a)*r*.65f,1+n%3,0x557c795b);}
        if(tier>=7)sprite(c,6,x,y-100,245,alpha);
        if(tier>=10){sprite(c,6,x-220,y+40,160,alpha);sprite(c,6,x+220,y+40,160,alpha);}
        if(tier>=2){sprite(c,2,x-175,y+75,150,alpha);sprite(c,2,x+175,y+75,150,alpha);}
        sprite(c,tier==0?0:tier<4?1:tier<10?4:7,x,y+110,tier==0?180:tier<4?220:tier<10?260:330,alpha);
        if(tier>=1&&tier<3)sprite(c,3,x,y-28,70,alpha);
        if(tier>=3)sprite(c,5,x-215,y+130,120,alpha);
        if(tier>=6)sprite(c,5,x+215,y+130,120,alpha);
        if(tier>=8)sprite(c,3,x,y-105,85,alpha);
        if(tier>=5)sprite(c,9,x-145,y+235,145,alpha);
        if(tier>=9)sprite(c,10,x+145,y+235,145,alpha);
        if(tier>=10)sprite(c,11,x,y+295,190,alpha);
        if(decoration>0)draw(c,decor,ornaments[decoration-1],x+(tier>=5?255:180),y+270,135,alpha);
        if(tier>=2)for(int side=-1;side<=1;side+=2)for(int n=0;n<4;n++){float yy=y-160+n*110;v.circle(c,x+side*285,yy,5,0xffe9dda6);v.circle(c,x+side*285,yy,11,0x22fff0b1);}
    }
    void world(Canvas c,MainActivity.OceanView v){
        if(v.g.guardianOcean())return;
        HomeReef r=v.g.home;
        if(Math.abs(v.cameraX+v.w/2-HomeReef.X)<v.w/2+400&&Math.abs(v.cameraY+v.h/2-HomeReef.Y)<v.h/2+400){
            scene(c,v,r.tier(),v.g.decor,.85f);
        }
        for(HomeReef.Food f:r.food){float yy=f.y+(float)Math.sin(v.clock*2+f.x)*3;
            if(f.type==0){for(int n=0;n<5;n++)v.circle(c,f.x+(n%3-1)*5,yy+(n/3)*5,3,0xffffdba2);}
            else if(f.type<3)v.snack(c,f.x,yy,f.type==1?7:4,v.clock);
            else{v.circle(c,f.x,yy,19,0x5576e5de);v.outline(c,f.x,yy,18,MainActivity.OceanView.GOLD,2);v.text(c,f.type==3?"Щ":"И",f.x,yy+5,15,MainActivity.OceanView.WHITE,true,true);v.text(c,f.type==3?"ЩИТ":"ИГЛЫ",f.x,yy+31,8,MainActivity.OceanView.MINT,true,true);}
        }
        for(Expedition.Shot s:r.shots){float d=Math.max(1,(float)Math.hypot(s.vx,s.vy));v.line(c,s.x-s.vx/d*16,s.y-s.vy/d*16,s.x,s.y,3,MainActivity.OceanView.GOLD);v.circle(c,s.x,s.y,3,MainActivity.OceanView.WHITE);}
        for(int n=0;n<r.robotCount();n++){HomeReef.Robot bot=r.robots[n];if(bot.hp<=0){v.outline(c,bot.x,bot.y,18,0x88a5c8c7,2);v.text(c,"Ремонт "+(int)Math.ceil(bot.respawn)+"с",bot.x,bot.y+30,9,MainActivity.OceanView.MUTED,true,true);continue;}
            sprite(c,8,bot.x,bot.y+24+(float)Math.sin(v.clock*3+n)*3,64,1);v.box(c,bot.x-24,bot.y+32,48,4,2,0xaa073c51);v.box(c,bot.x-24,bot.y+32,48*bot.hp/60,4,2,0xff9ee4bc);
        }
    }
    void overview(Canvas c,MainActivity.OceanView v){
        HomeReef r=v.g.home;float progress=r.pending>0?Game.clamp(r.buildTime/5,0,1):1;
        v.water(c,true);v.text(c,r.pending>0?"ТВОЙ РИФ РАСТЁТ":"ДОМАШНИЙ РИФ",210,68,25,MainActivity.OceanView.WHITE,true,true);
        v.text(c,HomeReef.BUILDINGS[r.tier()]+" · "+r.tier()+" / 10",210,98,15,MainActivity.OceanView.MINT,true,true);
        v.text(c,"◈ "+v.g.bank+" ракушек",210,125,12,MainActivity.OceanView.GOLD,true,true);
        c.save();c.translate(210,375);c.scale(.56f,.56f);c.translate(-HomeReef.X,-HomeReef.Y);
        if(progress<1)scene(c,v,Math.max(0,r.tier()-1),v.g.decor,1);
        scene(c,v,r.tier(),v.g.decor,progress);
        for(int n=0;n<r.robotCount();n++)sprite(c,8,HomeReef.X+(n-1)*100,HomeReef.Y+190,70,progress);
        if(r.pending>0&&progress<1)for(int n=0;n<28;n++){float a=n*2.39996f+v.clock*.3f,rad=50+(n*31)%250;v.star(c,HomeReef.X+(float)Math.cos(a)*rad,HomeReef.Y+(float)Math.sin(a)*rad,3+(float)Math.sin(v.clock*7+n)*2,MainActivity.OceanView.GOLD);}
        c.restore();
        v.text(c,r.pending>0&&progress<1?"Ракушки укладываются, загораются окна…":"Риф — дом, но не безопасная зона",210,580,12,MainActivity.OceanView.MINT,true,true);
        v.text(c,"Оборона: "+r.guns()+" огневых точек · роботов: "+r.robotCount(),210,606,12,MainActivity.OceanView.WHITE,true,true);
        if(r.pending>0){if(progress>=1)v.button(c,"Продолжить","Новый дом уже ждёт в океане",30,649,360,42,true);else v.text(c,"Строительство…",210,685,16,MainActivity.OceanView.GOLD,true,true);}
        else{v.button(c,v.g.decor<8?"Украсить риф":"Сад завершён",v.g.decor<8?HomeReef.DECOR[v.g.decor]+" · "+HomeReef.decorCost(v.g.decor)+" ракушек":"Все 8 вариантов открыты",30,639,360,41,true);v.button(c,"Назад","",30,718,360,42,false);}
    }
}
