package com.nicole.ocean;

import android.graphics.*;

/** Resolution-independent illustrated hazards, with geometry matching the simulation. */
final class HazardArt {
    final Paint p=new Paint(3);final Path path=new Path();final RectF rect=new RectF();
    void color(int c){p.setShader(null);p.setColor(c);p.setStyle(Paint.Style.FILL);}
    void line(Canvas c,float x,float y,float xx,float yy,float sw,int col){color(col);p.setStrokeWidth(sw);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x,y,xx,yy,p);}
    void oval(Canvas c,float x,float y,float rx,float ry,int col){color(col);rect.set(x-rx,y-ry,x+rx,y+ry);c.drawOval(rect,p);}
    void ring(Canvas c,float x,float y,float r,int col,float stroke){color(col);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(stroke);c.drawCircle(x,y,r,p);p.setStyle(Paint.Style.FILL);}
    void draw(Canvas c,Hazard a,float clock,float cameraY){
        if(!a.visible())return;
        if(a.stage==1){
            int alpha=60+(int)(45*(1+Math.sin(a.timer*9)));int col=(alpha<<24)|0xffb677;
            if(a.horizontal()){color(col);c.drawRect(a.originX-a.width,a.originY-200,a.originX+a.width,a.originY+200,p);
                for(int j=-4;j<5;j++)line(c,a.originX+j*65,a.originY-15,a.originX+j*65+a.direction*22,a.originY,4,0xaaffdda0);
            }else if(a.radial()){ring(c,a.originX,a.originY,a.kind==Hazard.MINE?210:155,col,10);}
            else{float lane=a.laneWidth();color(col);c.drawRect(a.originX-lane,cameraY,a.originX+lane,cameraY+a.height,p);
                for(int j=0;j<8;j++){float yy=cameraY+180+j*80;line(c,a.originX-12,yy,a.originX,yy+16,3,0xaaffdda0);line(c,a.originX,yy+16,a.originX+12,yy,3,0xaaffdda0);}}
            return;
        }
        if(a.kind==Hazard.SUBMARINE)submarine(c,a,clock);
        else if(a.kind==Hazard.ANCHOR)anchor(c,a,cameraY);
        else if(a.kind==Hazard.BOULDER)boulder(c,a,clock);
        else if(a.kind==Hazard.WHIRLPOOL)whirlpool(c,a,clock);
        else if(a.kind==Hazard.HURRICANE)hurricane(c,a);
        else if(a.kind==Hazard.SHIP)ship(c,a);
        else if(a.kind==Hazard.JELLY)jellies(c,a);
        else if(a.kind==Hazard.NET)net(c,a);
        else if(a.kind==Hazard.GEYSER)geyser(c,a);
        else if(a.kind==Hazard.MIGRATION)migration(c,a);
        else if(a.kind==Hazard.MINE)mine(c,a);
    }
    void submarine(Canvas c,Hazard a,float t){
        c.save();c.translate(a.x,a.y);c.scale(a.direction,1);float half=a.length/2;
        oval(c,0,25,half+12,78,0x33021930);
        rect.set(-half,-65,half,65);color(Color.WHITE);p.setShader(new LinearGradient(0,-65,0,65,new int[]{0xff7babb6,0xff315568,0xff0f2b42},null,Shader.TileMode.CLAMP));c.drawRoundRect(rect,65,65,p);p.setShader(null);
        rect.set(-half+40,-48,half-48,-28);color(0x336dfff4);c.drawRoundRect(rect,12,12,p);
        rect.set(-160,-110,5,-40);color(0xff456c7e);c.drawRoundRect(rect,20,20,p);
        line(c,-70,-108,-70,-157,13,0xff557a88);line(c,-70,-157,-34,-157,13,0xff557a88);
        for(float xx=-half+110;xx<half-90;xx+=110){
            ring(c,xx,-2,20,0xff182e41,8);oval(c,xx,-2,15,15,0xff52adba);oval(c,xx-4,-7,6,4,0xffa4e8df);
            for(int j=-1;j<=1;j++)oval(c,xx+j*27,44,2,2,0xff92b0b2);
        }
        line(c,-half+15,0,-half-25,0,13,0xff577b86);
        for(int j=0;j<3;j++){float q=t*12+j*2.094f;line(c,-half-25,0,-half-25+(float)Math.cos(q)*25,(float)Math.sin(q)*58,7,0xff85acaf);}
        for(int j=0;j<12;j++){float xx=-half-40-(j*29+t*70)%160;ring(c,xx,(float)Math.sin(j*3+t)*38,3+j%5,0x558fe9e4,1.4f);}
        path.reset();path.moveTo(-half+70,-35);path.lineTo(-half+12,-100);path.lineTo(-half+150,-47);path.close();color(0xff426b7b);c.drawPath(path,p);
        line(c,half-60,-20,half-60,20,3,0xffa3bac0);c.restore();
    }
    void anchor(Canvas c,Hazard a,float cy){
        // Chain connects to the top even when the anchor has already fallen past the screen.
        for(float yy=cy-35;yy<a.y-100;yy+=31){rect.set(a.x-8,yy,a.x+8,yy+25);color(0xff526f7f);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);c.drawOval(rect,p);p.setStyle(Paint.Style.FILL);}
        c.save();c.translate(a.x,a.y);oval(c,12,105,85,20,0x29001e35);
        ring(c,0,-83,20,0xffa5bec5,12);line(c,0,-60,0,78,19,0xff7696a4);line(c,-49,-24,49,-24,16,0xffa4bec5);
        color(0xff7898a7);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(20);path.reset();path.moveTo(-65,28);path.cubicTo(-63,77,-18,75,0,92);path.cubicTo(18,75,63,77,65,28);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
        for(int d:new int[]{-1,1}){path.reset();path.moveTo(d*47,39);path.lineTo(d*83,39);path.lineTo(d*65,8);path.close();color(0xffa2bac2);c.drawPath(path,p);}
        line(c,-3,-58,-3,65,3,0xffd0e1dc);c.restore();
    }
    void boulder(Canvas c,Hazard a,float t){
        c.save();c.translate(a.x,a.y);c.rotate(a.timer*28*a.direction);
        path.reset();for(int j=0;j<11;j++){float an=j*6.283185f/11,rr=78+(j%3)*5;float xx=(float)Math.cos(an)*rr,yy=(float)Math.sin(an)*rr;if(j==0)path.moveTo(xx,yy);else path.lineTo(xx,yy);}path.close();
        color(Color.WHITE);p.setShader(new LinearGradient(-60,-70,60,80,0xffb7aaa1,0xff40596c,Shader.TileMode.CLAMP));c.drawPath(path,p);p.setShader(null);
        line(c,-45,-35,0,-10,4,0xff667581);line(c,0,-10,17,40,4,0xff667581);line(c,0,-10,44,-30,4,0xff667581);
        for(int j=0;j<8;j++)oval(c,(j*47)%115-55,(j*33)%109-52,6+j%3*3,4,0x3340576b);c.restore();
        for(int j=0;j<8;j++)ring(c,a.x+(j*37)%110-55,a.y-105-j*21,3+j%4,0x557fc9d4,1.5f);
    }
    void whirlpool(Canvas c,Hazard a,float t){
        oval(c,a.x,a.y,131,131,0x2210a9ba);oval(c,a.x,a.y,65,65,0x6609224c);
        for(int j=0;j<5;j++){
            path.reset();for(int n=0;n<80;n++){float u=n/79f,an=t*1.8f+j*1.256637f+u*6.8f,r=10+u*115;
                float xx=a.x+(float)Math.cos(an)*r,yy=a.y+(float)Math.sin(an)*r;if(n==0)path.moveTo(xx,yy);else path.lineTo(xx,yy);}
            color(j%2==0?0xaa78e4e8:0x664dc4da);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3+j%2);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
        }
        oval(c,a.x,a.y,23,23,0xaa072a47);
    }
    void hurricane(Canvas c,Hazard a){
        for(int j=0;j<26;j++){
            float yy=a.y-a.height*.45f+j*a.height*.9f/25;
            float width=50+45*(1-j/26f),phase=a.timer*5+j*.7f;
            oval(c,a.x+(float)Math.sin(phase)*18,yy,width,12,0x226cdef0);
            rect.set(a.x-width,yy-14,a.x+width,yy+14);color(0xaa9fe8ee);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);
            c.drawArc(rect,phase*57,240,false,p);p.setStyle(Paint.Style.FILL);
        }
        for(int j=0;j<18;j++){float q=a.timer*3+j*2.4f,rr=75+j%3*16;
            oval(c,a.x+(float)Math.cos(q)*rr,a.y-a.height*.4f+(j*77+a.timer*110)%(a.height*.8f),5,2,0xaab5f7ed);}
    }
    void ship(Canvas c,Hazard a){
        c.save();c.translate(a.x,a.y);c.rotate((float)Math.toDegrees(a.shipAngle()));
        oval(c,0,22,165,67,0x33051f32);
        path.reset();path.moveTo(-155,-35);path.lineTo(155,-35);path.lineTo(118,54);path.lineTo(-115,54);path.close();
        color(Color.WHITE);p.setShader(new LinearGradient(0,-35,0,54,0xffa46e54,0xff344451,Shader.TileMode.CLAMP));c.drawPath(path,p);p.setShader(null);
        line(c,-145,-34,145,-34,8,0xffd8b58b);
        for(int j=-2;j<=2;j++){ring(c,j*43,3,12,0xff233a48,5);oval(c,j*43,3,7,7,0xff5eb9c3);}
        color(0xffb7c5bb);rect.set(-60,-91,70,-39);c.drawRoundRect(rect,6,6,p);
        for(int j=0;j<3;j++){color(0xff244b60);c.drawRect(-47+j*36,-78,-22+j*36,-59,p);}
        line(c,-88,-37,-88,-145,6,0xff8e8069);line(c,-88,-133,-30,-116,3,0xffe2c3a0);
        path.reset();path.moveTo(-83,-126);path.lineTo(-28,-111);path.lineTo(-80,-71);path.close();color(0x99d2cdb5);c.drawPath(path,p);
        line(c,64,-38,98,52,5,0xff142f40);line(c,79,-8,110,4,3,0xff142f40);c.restore();
        for(int j=0;j<12;j++)ring(c,a.x-80+(j*43)%160,a.y-115-(j*29+a.timer*65)%170,3+j%5,0x889bdce4,2);
    }
    void jellies(Canvas c,Hazard a){
        for(int j=0;j<3;j++){
            float x=a.elementX(j),y=a.elementY(j),pulse=(float)Math.sin(a.timer*5+j);
            oval(c,x,y,45,45,0x225fd5ff);oval(c,x,y-5,29,22+pulse*3,0xaacba8ff);
            oval(c,x-6,y-12,12,6,0x88edeeff);
            for(int n=-2;n<=2;n++){
                path.reset();path.moveTo(x+n*9,y+8);path.cubicTo(x+n*9+14*pulse,y+22,x+n*9-13*pulse,y+32,x+n*10,y+43);
                color(0xaab7a0ff);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
            }
            ring(c,x,y,32+3*pulse,0x777ff4ff,1.5f);
            if(pulse>.6f){line(c,x-36,y-9,x-44,y,2,0xffc4ffff);line(c,x-44,y,x-35,y+7,2,0xffc4ffff);}
        }
    }
    void net(Canvas c,Hazard a){
        c.save();c.translate(a.x,a.y);
        color(0x336db1b5);c.drawRect(-100,-65,100,65,p);
        for(int j=-4;j<=4;j++){float x=j*24;line(c,x,-65,x,65,2,0xaa91bab3);}
        for(int j=-2;j<=2;j++)line(c,-100,j*24,100,j*24,2,0xaa91bab3);
        line(c,-100,-65,100,-65,6,0xffd6c28d);line(c,-100,65,100,65,5,0xff8b989c);
        line(c,-100,-65,-100,65,4,0xffb6bf9d);line(c,100,-65,100,65,4,0xffb6bf9d);
        for(int j=-2;j<=2;j++){oval(c,j*42,-65,9,6,0xffeabf76);oval(c,j*42,65,6,8,0xff5f7e8a);}
        c.restore();
    }
    void geyser(Canvas c,Hazard a){
        oval(c,a.x,a.y+12,73,28,0xff344c5c);oval(c,a.x,a.y,49,15,0xff1a344a);
        boolean active=a.geyserOn();oval(c,a.x,a.y,35,9,active?0xffffc68a:0xff8b645e);
        if(!active){ring(c,a.x,a.y-8,22+a.timer%2*25,0x66ffbe86,3);return;}
        float top=a.y-a.height*.65f;
        color(Color.WHITE);p.setShader(new LinearGradient(0,a.y,0,top,new int[]{0xaaffce94,0x8859e8df,0x009affea},null,Shader.TileMode.CLAMP));
        c.drawRect(a.x-43,top,a.x+43,a.y,p);p.setShader(null);
        for(int j=0;j<20;j++){float yy=a.y-(j*31+a.timer*260)%(a.height*.65f),xx=a.x+(float)Math.sin(j*3+a.timer*4)*33;
            ring(c,xx,yy,3+j%5,0x99b4fff0,2);}
    }
    void migration(Canvas c,Hazard a){
        for(int j=0;j<3;j++){
            float x=a.elementX(j),y=a.elementY(j),flap=(float)Math.sin(a.timer*8+j)*12;
            c.save();c.translate(x,y);c.scale(a.direction,1);
            line(c,-25,0,-88,-10,3,0xff669fba);
            path.reset();path.moveTo(38,0);path.quadTo(6,-16,-18,-45-flap);path.quadTo(-38,-8,-26,0);path.quadTo(-38,8,-18,45+flap);path.quadTo(6,16,38,0);path.close();
            color(0xff71a8c0);c.drawPath(path,p);oval(c,1,0,26,13,0xffadcbd2);oval(c,24,-6,3,3,0xff152e44);
            for(int n=0;n<3;n++)line(c,-65-n*16,25,-45-n*16,25,2,0x66a8e9ee);c.restore();
        }
    }
    void mine(Canvas c,Hazard a){
        if(a.timer>=4.2f){float r=a.blastRadius();ring(c,a.x,a.y,r,0xaaffbc86,25);ring(c,a.x,a.y,r*.82f,0x88b9f7ea,4);return;}
        for(int j=0;j<8;j++){float an=j*.785398f;line(c,a.x+(float)Math.cos(an)*22,a.y+(float)Math.sin(an)*22,a.x+(float)Math.cos(an)*36,a.y+(float)Math.sin(an)*36,7,0xff75919f);}
        oval(c,a.x,a.y,28,28,0xff36546a);oval(c,a.x-7,a.y-9,12,8,0xff7597a3);
        oval(c,a.x+5,a.y-2,5,5,Math.sin(a.timer*12)>0?0xffffaa7d:0xff8d5b56);
        ring(c,a.x,a.y,40+a.timer*4,0x77ffb98e,2);
        line(c,a.x,a.y+30,a.x-10,a.y+95,2,0xff537d8e);
    }

}
