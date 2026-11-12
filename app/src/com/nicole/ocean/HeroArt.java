package com.nicole.ocean;

import android.graphics.*;

/** Code-native characters share the original soft, rounded game style. All heads face local +X. */
final class HeroArt {
    final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    final Path path=new Path();final RectF rect=new RectF();
    final android.util.SparseArray<Shader> shades=new android.util.SparseArray<>();
    static final int WHITE=0xfff3fff4,INK=0xff083c51,MINT=0xff9df1ce;
    int[] palette;int part=1,drawnParts;boolean detail;
    int tint(int col){if(palette==null||detail||col==INK)return col;int base=palette[part];float light=.6f+(Color.red(col)+Color.green(col)+Color.blue(col))/1530f;return Color.rgb(Math.min(255,(int)(Color.red(base)*light)),Math.min(255,(int)(Color.green(base)*light)),Math.min(255,(int)(Color.blue(base)*light)));}
    void fill(int col){
        if(!detail&&col!=INK)drawnParts|=1<<part;
        int color=tint(col);p.setColor(color);p.setStyle(Paint.Style.FILL);p.setShader(null);
        if(detail||col==INK)return;
        Shader shader=shades.get(color);
        if(shader==null){shader=new LinearGradient(0,-.6f,0,.65f,
            MainActivity.OceanView.blend(color,WHITE,.16f),MainActivity.OceanView.blend(color,INK,.25f),Shader.TileMode.CLAMP);shades.put(color,shader);}
        p.setShader(shader);
    }
    void oval(Canvas c,float x,float y,float rx,float ry,int col){part=x>.4f?0:Math.abs(y)>.4f?2:col==WHITE?4:1;fill(col);rect.set(x-rx,y-ry,x+rx,y+ry);c.drawOval(rect,p);}
    void poly(Canvas c,int col,float... xy){float xx=0,yy=0,max=-10;for(int n=0;n<xy.length;n+=2){xx+=xy[n];yy+=xy[n+1];max=Math.max(max,xy[n]);}xx/=xy.length/2;yy/=xy.length/2;part=max<-.5f?3:Math.abs(yy)>.28f?2:xx>.45f?0:1;if(col==0xffffdfa4||col==0xffffdc97||col==0xffffdc97)part=4;fill(col);path.reset();path.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)path.lineTo(xy[i],xy[i+1]);path.close();c.drawPath(path,p);}
    void line(Canvas c,float x,float y,float xx,float yy,float width,int col){part=Math.max(x,xx)<-.5f?3:x>.2f?0:4;fill(col);p.setStrokeWidth(width);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x,y,xx,yy,p);}
    void eye(Canvas c,float x,float y,float size){detail=true;oval(c,x,y,size,size,WHITE);oval(c,x+size*.34f,y,size*.53f,size*.6f,INK);oval(c,x+size*.5f,y-size*.4f,size*.17f,size*.17f,WHITE);detail=false;}
    void smile(Canvas c,float x,float y,float width){fill(INK);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.025f);path.reset();path.moveTo(x,y);path.quadTo(x+width*.5f,y+.09f,x+width,y-.02f);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);}
    void draw(Canvas canvas,int stage,int style,float radius,float time){
        drawnParts=0;part=1;canvas.save();canvas.scale(radius,radius);canvas.rotate((float)Math.sin(time*2)*2);
        int accent=style==1?0xffaa8de5:style==2?0xff78d4c7:0xff88cbdc;
        if(stage>=45){
            int variant=stage-45;int[] bases={44,16,40,9,26,20,22,23,32,38,8};
            expanded(canvas,bases[variant],time,accent);
            for(int n=0;n<=variant%4;n++){float xx=-.55f+n*.28f;poly(canvas,0xffffdda0,xx,-.3f,xx-.12f,-.8f-variant*.025f,xx+.12f,-.3f);}
            detail=true;oval(canvas,.55f,-.4f,.06f+variant*.004f,.06f,0xffbcffe4);detail=false;
        }else if(stage>=9)expanded(canvas,stage,time,accent);
        else if(stage==1)mermaid(canvas,time,style);
        else if(stage==4)manta(canvas,time);
        else if(stage==7)narwhal(canvas,time);
        else if(stage==8)dragon(canvas,time);
        else swimmer(canvas,stage,accent,time);
        if(stage>=9){
            if((drawnParts&1)==0)oval(canvas,.65f,.16f,.18f,.12f,accent);
            if((drawnParts&4)==0)oval(canvas,-.1f,.49f,.23f,.10f,accent);
            if((drawnParts&8)==0)poly(canvas,accent,-.7f,.18f,-1.15f,.36f,-.8f,.4f);
        }
        if(palette!=null){detail=true;for(int n=0;n<3;n++)oval(canvas,-.42f+n*.18f,-.08f+(n%2)*.12f,.042f,.038f,palette[4]);detail=false;}
        detail=true;
        if(style==1)for(int n=0;n<3;n++)line(canvas,-.45f+n*.16f,-.18f,-.55f+n*.16f,.12f,.027f,0x8899fff0);
        if(style==2){oval(canvas,-.32f,-.25f,.08f,.06f,WHITE);oval(canvas,-.55f,-.13f,.045f,.04f,WHITE);}
        detail=false;
        canvas.restore();
    }
    void expanded(Canvas c,int stage,float t,int col){
        float wave=(float)Math.sin(t*5)*.12f;
        int gold=0xffffd991,rose=0xffe8a2d6;
        // Each family has an anatomical silhouette, not merely a new tint.
        switch(stage){
        case 10: case 21: case 30: // seahorse, leafy dragon, unicorn
            oval(c,-.1f,.1f,.38f,.65f,col);oval(c,.35f,-.48f,.4f,.3f,col);
            line(c,.45f,-.4f,.95f,-.34f,.18f,col);
            for(int n=0;n<12;n++){float a=n*.4f;oval(c,-.22f+(float)Math.cos(a)*(.3f-n*.014f),.65f+(float)Math.sin(a)*(.3f-n*.014f),.09f,.09f,col);}
            for(int n=0;n<(stage==21?7:3);n++)poly(c,rose,-.35f,n*.19f-.65f,-.8f-wave,n*.19f-.9f,-.4f,n*.19f-.42f);
            if(stage==30)poly(c,gold,.3f,-.65f,.78f,-1.2f,.52f,-.6f);eye(c,.48f,-.5f,.105f);break;
        case 14: case 11: // puffer and sunfish
            if(stage==14)for(int n=0;n<14;n++){double a=n*Math.PI/7;float x=(float)Math.cos(a),y=(float)Math.sin(a);poly(c,gold,x*.6f-y*.08f,y*.6f+x*.08f,x*.95f,y*.95f,x*.6f+y*.08f,y*.6f-x*.08f);}
            oval(c,0,0,.8f,.72f,col);poly(c,rose,-.65f,0,-1.12f,-.38f+wave,-1.12f,.38f+wave);eye(c,.45f,-.17f,.14f);smile(c,.5f,.13f,.2f);break;
        case 15: case 19: case 33: case 34: // turtle, crab, mantis shrimp, lobster
            for(int side=-1;side<=1;side+=2)for(int n=0;n<(stage==15?2:4);n++)line(c,-.55f+n*.35f,side*.25f,-.8f+n*.45f,side*(.75f+wave),.12f,col);
            oval(c,0,0,stage==34?1f:.65f,.45f,col);
            if(stage==33){poly(c,gold,-.4f,-.25f,-1.5f,-.6f,-1.1f,0,-1.5f,.6f,-.4f,.25f);line(c,.5f,-.25f,1.1f,-1f,.06f,rose);line(c,.5f,.25f,1.1f,1f,.06f,rose);}
            if(stage==15){oval(c,-.05f,-.02f,.52f,.4f,rose);for(int n=0;n<3;n++)line(c,-.4f+n*.3f,-.27f,-.4f+n*.3f,.27f,.025f,gold);oval(c,.75f,0,.3f,.23f,col);eye(c,.85f,-.06f,.09f);}
            else{for(int side=-1;side<=1;side+=2){line(c,.4f,side*.2f,.8f,side*.65f,.12f,col);oval(c,.98f,side*.65f,.27f,.18f,rose);line(c,1f,side*.65f,1.3f,side*.72f,.035f,INK);eye(c,.5f,side*.2f,.1f);}}break;
        case 18: case 20: case 36: // squid, jellyfish, octopus
            for(int n=0;n<(stage==36?8:6);n++){float yy=(n-3)*.15f;line(c,-.1f,yy,-.7f,yy+wave,.12f,col);line(c,-.7f,yy+wave,-1.15f-(n%2)*.18f,yy+(float)Math.sin(t*4+n)*.25f,.08f,rose);}
            if(stage==18)poly(c,col,-.3f,-.45f,1.1f,0,-.3f,.45f);else oval(c,.3f,0,.65f,stage==20?.65f:.48f,col);
            eye(c,.6f,-.14f,.13f);smile(c,.62f,.1f,.18f);break;
        case 26: case 37: case 39: // three shell anatomies
            poly(c,col,-.5f,.2f,-1.2f,.5f+wave,-.6f,.55f);oval(c,0,0,.72f,.65f,col);detail=true;for(int n=0;n<25;n++){float a=n*.4f,r=.58f-n*.018f;oval(c,(float)Math.cos(a)*r,(float)Math.sin(a)*r,.045f,.045f,palette==null?gold:palette[4]);}detail=false;
            if(stage==39)for(int n=0;n<12;n++){double a=n*Math.PI/6;line(c,(float)Math.cos(a)*.5f,(float)Math.sin(a)*.5f,(float)Math.cos(a)*.76f,(float)Math.sin(a)*.76f,.04f,rose);}
            oval(c,.73f,.28f,.42f,.18f,col);eye(c,.9f,.16f,.1f);if(stage==37){line(c,.8f,.15f,.9f,-.3f,.05f,col);eye(c,.9f,-.3f,.09f);}break;
        case 25: case 40: // eel and storm serpent
            for(int n=12;n>=0;n--){float xx=.85f-n*.16f,yy=(float)Math.sin(t*4-n*.5f)*.2f;oval(c,xx,yy,.16f,.12f+n*.004f,col);if(stage==40&&n%2==0)poly(c,gold,xx,yy-.1f,xx-.16f,yy-.55f,xx-.25f,yy-.1f);}eye(c,.92f,-.05f,.09f);break;
        case 27: case 28: case 29: // penguin, seal and walrus
            oval(c,0,0,1,.48f,stage==27?0xff203e50:col);oval(c,.2f,.15f,.65f,.27f,WHITE);oval(c,.68f,-.08f,.4f,.4f,col);
            poly(c,col,-.25f,.2f,-.6f,.85f+wave,.25f,.4f);poly(c,col,-.8f,0,-1.3f,-.35f,-1.15f,.3f);eye(c,.85f,-.2f,.11f);
            if(stage==29){line(c,.8f,.12f,.7f,.72f,.09f,WHITE);line(c,1f,.12f,.95f,.65f,.08f,WHITE);}else if(stage==27)poly(c,gold,.95f,-.1f,1.3f,.02f,.95f,.13f);break;
        case 38: mermaid(c,t,1);for(int n=0;n<5;n++){double a=n*1.256;oval(c,.5f+(float)Math.cos(a)*.24f,-.5f+(float)Math.sin(a)*.2f,.14f,.10f,rose);}break;
        case 22: manta(c,t);for(int n=0;n<5;n++){float a=n*1.256f;poly(c,gold,0,0,(float)Math.cos(a)*.8f,(float)Math.sin(a)*.8f,(float)Math.cos(a+.5f)*.3f,(float)Math.sin(a+.5f)*.3f);}break;
        case 23: swimmer(c,6,col,t);for(int n=0;n<4;n++)poly(c,0xffa5e8ff,-.5f+n*.35f,-.3f,-.4f+n*.35f,-.9f,-.25f+n*.35f,-.3f);break;
        default:
            swimmer(c,stage==24?5:0,col,t);
            if(stage==9||stage==13||stage==31||stage==32||stage==43||stage==44){
                for(int side=-1;side<=1;side+=2)for(int n=0;n<4;n++)poly(c,n%2==0?rose:gold,-.35f,side*.1f,-1.1f+n*.3f,side*(.95f+(stage%7)*.075f-n*.12f+wave),.25f,side*.25f);
            }
            if(stage==12)poly(c,rose,-.65f,-.25f,-.7f,-1.3f,.5f,-.85f,.6f,-.3f);
            if(stage==16)for(int side=-1;side<=1;side+=2)for(int n=0;n<3;n++)line(c,.5f,side*.25f,.35f+n*.25f,side*(.75f+wave),.075f,rose);
            if(stage==17)for(int n=0;n<7;n++)line(c,-.7f+n*.23f,-.2f,-.85f+n*.25f,-1+(n%2)*.2f,.055f,gold);
            if(stage==24){poly(c,col,.4f,-.3f,.3f,-.95f,.75f,-.3f);poly(c,col,.7f,-.3f,.9f,-.9f,1,-.2f);}
            if(stage==35){line(c,.5f,-.35f,.6f,-.95f,.05f,col);line(c,.6f,-.95f,1.1f,-.8f,.05f,col);oval(c,1.1f,-.8f,.17f,.17f,gold);}
            if(stage==41)for(int n=0;n<5;n++)line(c,-.65f,(n-2)*.1f,-1.8f,(n-2)*.22f+wave,.05f,n%2==0?gold:rose);
            if(stage==42){poly(c,gold,.55f,-.3f,.9f,-1.2f,.8f,-.25f);for(int n=0;n<4;n++)oval(c,-.7f-n*.15f,.1f+wave,.15f,.12f,rose);}
            if(stage==44){poly(c,gold,.3f,-.4f,.3f,-.95f,.5f,-.7f,.7f,-1.1f,.9f,-.7f,1.1f,-.95f,1.1f,-.4f);}
        }
    }
    void mermaid(Canvas c,float time,int style){
        float wave=(float)Math.sin(time*6)*.08f;
        // Flowing fin, clothed torso and long hair make this silhouette distinct even at small sizes.
        poly(c,0xffb2a0ee,-.7f,0,-1.45f,-.48f+wave,-1.24f,0,-1.45f,.48f+wave);
        part=1;fill(0xff6ed7bf);path.reset();path.moveTo(.12f,-.24f);path.cubicTo(-.4f,-.43f,-.72f,-.18f,-1.2f,0);path.cubicTo(-.7f,.25f,-.25f,.3f,.12f,.25f);path.close();c.drawPath(path,p);
        poly(c,0xffb2a0ee,-.3f,-.2f,-.72f,-.8f,-.8f,-.15f);
        poly(c,0xffb2a0ee,-.3f,.2f,-.72f,.8f,-.8f,.15f);
        oval(c,.21f,.02f,.34f,.23f,0xffa98fe7);
        // Modest purple swim top; generic fairy-tale character, independent of Nicole's photos.
        detail=true;line(c,.25f,-.14f,.68f,-.4f,.11f,0xffffd3ad);line(c,.25f,.17f,.68f,.37f,.11f,0xffffd3ad);detail=false;
        oval(c,.61f,-.05f,.43f,.41f,style==2?0xff8e5a68:0xffc57e64);
        detail=true;oval(c,.75f,-.01f,.3f,.31f,0xffffd8b7);detail=false;
        poly(c,0xffc57e64,.75f,-.35f,.2f,-.34f,-.18f,-.04f,.43f,-.12f);
        eye(c,.88f,-.07f,.10f);smile(c,.88f,.12f,.13f);
        for(int i=0;i<4;i++)line(c,-.2f-i*.18f,-.06f,-.27f-i*.18f,.09f,.025f,0xffb8f5d7);
        oval(c,.48f,-.37f,.09f,.07f,0xfff9dc96);
    }
    void swimmer(Canvas c,int stage,int accent,float time){
        boolean hammer=stage==3,dolphin=stage==5,orca=stage==6;
        int body=orca?0xff173e59:dolphin?0xff74cbd8:stage==2?0xff8cb9d5:accent;
        float sway=(float)Math.sin(time*7)*.07f;
        poly(c,body,-.75f,0,-1.43f,-.52f+sway,-1.18f,0,-1.43f,.52f+sway);
        poly(c,body,-.24f,-.25f,.04f,-.96f,.37f,-.33f);
        oval(c,.03f,0,1.08f,.49f,body);oval(c,.65f,0,.45f,.43f,body);oval(c,.18f,.22f,.82f,.23f,orca?WHITE:0xffcbe9e9);
        poly(c,body,-.05f,.17f,-.53f,.78f,.4f,.38f);
        if(dolphin){oval(c,1.01f,.04f,.43f,.13f,body);eye(c,.65f,-.12f,.12f);smile(c,1.06f,.08f,.22f);}
        else if(hammer){
            // The broad transverse head is the defining hammerhead shape.
            oval(c,.87f,-.39f,.22f,.36f,body);oval(c,.87f,.39f,.22f,.36f,body);
            eye(c,.97f,-.6f,.10f);eye(c,.97f,.56f,.10f);smile(c,.9f,.09f,.15f);
        }else{
            if(orca){oval(c,.57f,-.25f,.22f,.13f,WHITE);oval(c,-.13f,-.35f,.25f,.09f,0xff67909d);}
            eye(c,.72f,-.09f,.12f);smile(c,.86f,.1f,.19f);
            if(!orca)for(int i=0;i<3;i++)line(c,.08f+i*.09f,-.05f,.04f+i*.09f,.17f,.025f,0xff487b96);
        }
    }
    void manta(Canvas c,float time){
        float wing=(float)Math.sin(time*4)*.1f;
        line(c,-.6f,0,-1.7f,.14f,.07f,0xff777ac6);
        part=2;fill(0xff8588d5);path.reset();path.moveTo(.8f,0);path.cubicTo(.32f,-.23f,-.3f,-1.45f+wing,-.82f,-1.12f);path.quadTo(-.54f,-.12f,-.68f,0);path.quadTo(-.55f,.16f,-.82f,1.12f-wing);path.cubicTo(-.3f,1.45f-wing,.32f,.23f,.8f,0);path.close();c.drawPath(path,p);
        oval(c,.15f,0,.71f,.33f,0xff9fa1ed);poly(c,0xffc6c9ff,.0f,.15f,-.49f,.72f,-.33f,.19f);
        poly(c,0xff9fa1ed,.56f,-.13f,.99f,-.37f,.91f,-.06f);poly(c,0xff9fa1ed,.56f,.13f,.99f,.37f,.91f,.06f);
        eye(c,.5f,-.16f,.10f);eye(c,.5f,.16f,.10f);smile(c,.68f,0,.12f);
    }
    void narwhal(Canvas c,float time){
        poly(c,0xff9cdfe1,-.75f,0,-1.35f,-.42f,-1.12f,0,-1.35f,.42f);
        oval(c,0,0,1.03f,.57f,0xffb3e8e3);oval(c,.6f,-.02f,.43f,.49f,0xffb3e8e3);oval(c,.2f,.28f,.65f,.23f,WHITE);
        poly(c,0xffffdfa4,.79f,-.08f,1.72f,-.22f,.91f,.08f);
        for(int i=0;i<4;i++)line(c,1+i*.13f,-.11f,1.04f+i*.13f,-.03f-i*.025f,.022f,0xffc69f71);
        poly(c,0xff8bc9d7,-.2f,.16f,-.45f,.75f,.2f,.42f);eye(c,.55f,-.16f,.13f);smile(c,.75f,.18f,.18f);
        for(int i=0;i<4;i++)oval(c,-.6f+i*.18f,-.15f-(i%2)*.12f,.045f,.035f,WHITE);
    }
    void dragon(Canvas c,float time){
        float sway=(float)Math.sin(time*4)*.12f;
        part=1;fill(0xff61cda9);path.reset();path.moveTo(.86f,-.18f);path.cubicTo(.05f,-.57f,-.63f,.44f,-1.5f,-.18f+sway);path.cubicTo(-.81f,.75f,-.1f,.22f,.84f,.29f);path.close();c.drawPath(path,p);
        for(int i=0;i<5;i++){float x=.3f-i*.28f;poly(c,0xffc1a0ef,x,-.16f,x-.23f,-.6f+(i%2)*.15f,x-.33f,-.05f);}
        poly(c,0xff93e3cd,.35f,.14f,-.13f,.76f,.58f,.38f);poly(c,0xffbea5ef,-.92f,.13f,-1.65f,.46f,-1.38f,-.01f);
        oval(c,.64f,-.03f,.5f,.38f,0xff80dfbb);oval(c,1.03f,.06f,.34f,.17f,0xffa6efd0);
        poly(c,0xffffdc97,.47f,-.3f,.53f,-.84f,.72f,-.35f);poly(c,0xffffdc97,.78f,-.32f,.95f,-.73f,1.0f,-.2f);
        eye(c,.82f,-.12f,.13f);smile(c,1.1f,.11f,.2f);oval(c,1.18f,0,.025f,.022f,INK);
        for(int i=0;i<3;i++)oval(c,-.2f-i*.3f,.1f,.04f,.04f,0xffffedb9);
    }
}
