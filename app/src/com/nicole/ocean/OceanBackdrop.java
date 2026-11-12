package com.nicole.ocean;

import android.graphics.*;

/** Decorative ocean layers. No gameplay RNG, bitmaps, or per-frame shader allocations. */
final class OceanBackdrop {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF oval = new RectF();
    private final Matrix matrix = new Matrix();
    private Bitmap base;
    private float baseW,baseH;
    private int baseDepth=-1;
    private final Shader shallow = new LinearGradient(0,0,0,1,
        new int[]{0xff268f9c,0xff10677e,0xff07354f},null,Shader.TileMode.CLAMP);
    private final Shader deep = new LinearGradient(0,0,0,1,
        new int[]{0xff244774,0xff152e58,0xff101d3b},null,Shader.TileMode.CLAMP);
    private final Shader light = new RadialGradient(0,0,1,
        new int[]{0x487cffe4,0x1274e5d7,0x0074e5d7},null,Shader.TileMode.CLAMP);
    private final Shader vignette = new RadialGradient(0,0,1,
        new int[]{0x0002152f,0x0002152f,0x7002152f},new float[]{0,.55f,1},Shader.TileMode.CLAMP);

    private void color(int color) { p.setShader(null); p.setColor(color); p.setStyle(Paint.Style.FILL); }
    private void ellipse(Canvas c,float x,float y,float rx,float ry,int color) {
        color(color);oval.set(x-rx,y-ry,x+rx,y+ry);c.drawOval(oval,p);
    }
    private void gradient(Canvas c,Shader shader,float w,float h,int alpha) {
        matrix.setScale(w,h);shader.setLocalMatrix(matrix);p.setColor(Color.WHITE);
        p.setAlpha(alpha);p.setShader(shader);c.drawRect(0,0,w,h,p);p.setShader(null);p.setAlpha(255);
    }
    void draw(Canvas c,float w,float h,float time,float cameraX,float cameraY,boolean menu) {
        float depth=menu?0:Game.clamp((cameraY-450)/1750,0,1);
        drawBase(c,w,h,depth);
        for(int i=0;i<6;i++) {
            float x=i*112-150+(float)Math.sin(time*.14f+i)*22-cameraX*.025f;
            color(((int)(12-depth*6)<<24)|0xb7ffea);path.reset();path.moveTo(x,-5);
            path.lineTo(x+18,-5);path.lineTo(x+210,h);path.lineTo(x+80,h);path.close();c.drawPath(path,p);
        }
        // Parallax reef shelves and sea fans at the margins, leaving the hero unobscured.
        for(int layer=0;layer<2;layer++) {
            float parallax=.10f+layer*.09f;
            for(int i=-1;i<7;i++) {
                float y=i*260-(cameraY*parallax)%260;
                float side=(i&1)==0?-28:w+28;
                float radius=80+(i+7)%3*22;
                ellipse(c,side,y,radius,55,layer==0?0x1617bca8:0x2229939d);
                fan(c,side+(side<0?45:-45),y-12,54+(i+7)%3*16,time+i,depth);
            }
        }
        // Tiny distant schools are visibly silhouettes, never interactive targets.
        for(int school=0;school<3;school++)for(int i=0;i<6;i++) {
            float x=wrap(school*179+i*16+time*(school%2==0?5:-4)-cameraX*.13f,w+80)-40;
            float y=wrap(school*253+90+(i%3)*9-cameraY*.12f,h+80)-40;
            ellipse(c,x,y,5,2,0x24042c48);
            color(0x24042c48);path.reset();path.moveTo(x-4,y);path.lineTo(x-9,y-3);path.lineTo(x-9,y+3);path.close();c.drawPath(path,p);
        }
        for(int i=0;i<7;i++) {
            float x=wrap(i*137+35-cameraX*.07f+(float)Math.sin(time*.15f+i)*13,w+100)-50;
            float y=wrap(i*191-time*4-cameraY*.09f,h+140)-70;
            jelly(c,x,y,10+i%3*4,time+i,depth);
        }
        for(int i=0;i<52;i++) {
            float x=wrap(i*73.37f-cameraX*.2f+(float)Math.sin(time*.2f+i)*5,w);
            float y=wrap(i*137.9f-time*(2+i%4)-cameraY*.15f,h);
            float r=.6f+i%3*.45f;
            ellipse(c,x,y,r,r,i%5==0?0x58bbffe5:0x2491d5df);
            if(i%9==0){color(0x328ce6e4);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.8f);c.drawCircle(x,y,3+i%4,p);p.setStyle(Paint.Style.FILL);}
        }
    }
    private void drawBase(Canvas c,float w,float h,float depth){
        int bucket=Math.round(depth*32);
        if(base==null||baseW!=w||baseH!=h||baseDepth!=bucket){
            int height=Math.max(1,Math.min(1600,(int)Math.ceil(h)));
            if(base==null||base.getHeight()!=height)base=Bitmap.createBitmap(420,height,Bitmap.Config.ARGB_8888);
            baseW=w;baseH=h;baseDepth=bucket;
            Canvas cached=new Canvas(base);cached.scale(420/w,height/h);
            gradient(cached,shallow,w,h,255);if(bucket>0)gradient(cached,deep,w,h,bucket*255/32);
            matrix.setScale(w*.95f,h*.8f);matrix.postTranslate(w*.35f,-h*.08f);
            light.setLocalMatrix(matrix);p.setShader(light);cached.drawRect(0,0,w,h,p);
            matrix.setScale(w*.85f,h*.8f);matrix.postTranslate(w/2,h*.43f);vignette.setLocalMatrix(matrix);
            p.setShader(vignette);cached.drawRect(0,0,w,h,p);p.setShader(null);
        }
        color(Color.WHITE);p.setFilterBitmap(true);oval.set(0,0,w,h);c.drawBitmap(base,null,oval,p);
    }
    private static float wrap(float x,float period){return (x%period+period)%period;}
    private void fan(Canvas c,float x,float y,float size,float time,float depth) {
        color(depth>.5f?0x335b91bd:0x3359c2b5);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(1.4f);
        path.reset();
        for(int i=-3;i<=3;i++) {
            float tip=x+i*size*.15f+(float)Math.sin(time*.5f)*4;
            path.moveTo(x,y);path.cubicTo(x,y-size*.4f,tip,y-size*.6f,tip,y-size+Math.abs(i)*5);
            for(int j=1;j<=3;j++){float yy=y-size*j*.22f,xx=x+(tip-x)*j*.23f;path.moveTo(xx,yy);path.lineTo(xx+i*3,yy-9);}
        }c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
    }
    private void jelly(Canvas c,float x,float y,float r,float t,float depth) {
        int alpha=(int)(20+depth*32);color((alpha<<24)|0xc4bbff);
        path.reset();path.moveTo(x-r,y);path.cubicTo(x-r,y-r*1.5f,x+r,y-r*1.5f,x+r,y);
        path.quadTo(x,y+r*.3f,x-r,y);c.drawPath(path,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);
        for(int i=-1;i<=1;i++){path.reset();path.moveTo(x+i*r*.5f,y);path.cubicTo(x+i*r*.5f+4,y+r,x+i*r*.5f-5,y+r*1.5f,x+i*r*.5f+(float)Math.sin(t)*5,y+r*2);c.drawPath(path,p);}
        p.setStyle(Paint.Style.FILL);
    }
}
