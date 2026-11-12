package com.nicole.ocean;

import android.content.Context;
import android.graphics.*;

/** One generated atlas, kept intact; source rectangles select the head, mittens and tail at runtime. */
final class BossiArt {
    final Bitmap atlas,lookingDown,body;final Rect downSource,bodySource;
    final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    final Rect[] source=new Rect[4];
    final RectF dest=new RectF();
    final Rect limbCrop=new Rect();
    BossiArt(Context context){
        int id=context.getResources().getIdentifier("bossi_atlas","drawable",context.getPackageName());
        atlas=BitmapFactory.decodeResource(context.getResources(),id);
        if(atlas==null)throw new IllegalStateException("Bossi atlas missing");
        int w=atlas.getWidth(),h=atlas.getHeight();
        source[0]=new Rect(0,0,(int)(w*.55f),(int)(h*.49f));
        source[1]=new Rect((int)(w*.65f),0,(int)(w*.91f),(int)(h*.49f));
        source[2]=new Rect((int)(w*.12f),(int)(h*.49f),(int)(w*.39f),h);
        source[3]=new Rect((int)(w*.62f),(int)(h*.49f),(int)(w*.94f),h);
        for(int i=0;i<source.length;i++)source[i]=NicoleArt.trim(atlas,source[i]);
        int downId=context.getResources().getIdentifier("bossi_looking_down","drawable",context.getPackageName());
        lookingDown=BitmapFactory.decodeResource(context.getResources(),downId);
        if(lookingDown==null)throw new IllegalStateException("Bossi downward pose missing");
        downSource=NicoleArt.trim(lookingDown,new Rect(0,0,lookingDown.getWidth(),lookingDown.getHeight()));
        body=BitmapFactory.decodeResource(context.getResources(),context.getResources().getIdentifier("bossi_torso","drawable",context.getPackageName()));
        if(body==null)throw new IllegalStateException("Bossi torso missing");
        bodySource=NicoleArt.trim(body,new Rect(0,0,body.getWidth(),body.getHeight()));
    }
    float naturalHeight(int index,float width){return width*source[index].height()/source[index].width();}
    void part(Canvas canvas,int index,float cx,float bottom,float width){
        float height=naturalHeight(index,width);
        dest.set(cx-width/2,bottom-height,cx+width/2,bottom);paint.setColor(Color.WHITE);paint.setStyle(Paint.Style.FILL);paint.setAlpha(255);
        canvas.drawBitmap(atlas,source[index],dest,paint);
    }
    void head(Canvas canvas,float cx,float bottom,float width,boolean down){
        if(down){
            float hh=width*downSource.height()/downSource.width();dest.set(cx-width/2,bottom-hh,cx+width/2,bottom);
            paint.setColor(Color.WHITE);paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);canvas.drawBitmap(lookingDown,downSource,dest,paint);
        }else{
            // He is leaning over us from above: a front-facing peek is genuinely upside down.
            float hh=naturalHeight(0,width);canvas.save();canvas.rotate(180,cx,bottom-hh/2);part(canvas,0,cx,bottom,width);canvas.restore();
        }
    }
    void torso(Canvas c,float x,float top,float bottom){
        paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);paint.setAlpha(255);paint.setShader(null);
        float width=420,hh=width*bodySource.height()/bodySource.width();
        dest.set(x-width/2,bottom-hh,x+width/2,bottom);c.drawBitmap(body,bodySource,dest,paint);
    }
    void limb(Canvas c,Bossi b,boolean left){
        float sx=b.shoulderX(left),sy=b.y+55,tx=b.pawX(left),ty=b.pawY(left);
        float length=(float)Math.hypot(tx-sx,ty-sy);
        c.save();c.translate(sx,sy);c.rotate((float)Math.toDegrees(Math.atan2(ty-sy,tx-sx))-90);
        paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);paint.setAlpha(255);
        Rect src=source[left?1:2];int split=src.top+(int)(src.height()*.7f);
        float half=Bossi.PAW_WIDTH*.5f;
        float pawHeight=(src.bottom-split)*Bossi.PAW_WIDTH/src.width();
        float seam=length-pawHeight*.5f;
        limbCrop.set(src.left,src.top,src.right,split);
        dest.set(-half,-90,half,seam+1);c.drawBitmap(atlas,limbCrop,dest,paint);
        // The mitten itself keeps its aspect ratio at twice the old width and height.
        limbCrop.set(src.left,split,src.right,src.bottom);
        dest.set(-half,seam,half,seam+pawHeight);c.drawBitmap(atlas,limbCrop,dest,paint);c.restore();
    }

    void draw(Canvas canvas,Bossi b,float cameraY){
        if(!b.visible()||b.stage==Bossi.LURE)return;
        float rise=-110*(1-b.presence());
        canvas.save();canvas.translate(0,rise);
        if(b.kind==Bossi.TAIL){
            // No sinusoidal motion: during HOLD_TAIL this geometry stays completely still.
            part(canvas,3,b.x,b.y+310,185);
        }else{
            boolean down=b.stage==Bossi.AIM||b.stage==Bossi.STRIKE||b.stage==Bossi.REST;
            // Forelegs pivot at shoulders concealed behind the chest and head.
            limb(canvas,b,true);limb(canvas,b,false);
            torso(canvas,b.x,b.y-140,b.y+130);
            head(canvas,b.x,b.y+225,330,down);

        }
        canvas.restore();
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setColor(0x887debdc);
        
        for(int side=0;side<2;side++){
            boolean left=side==0;
            if(!(left?b.attacksLeft():b.attacksRight()))continue;
            if(b.stage==Bossi.AIM){
                float tx=b.strikeTargetX(left);
                float r=Bossi.PAW_RADIUS+b.progress()*6;paint.setColor(0xccffd58b);
                canvas.drawCircle(tx,b.targetY,r,paint);
                if(b.diagonal()){
                    paint.setStrokeWidth(Bossi.PAW_RADIUS*2);paint.setColor(0x22ffd58b);
                    canvas.drawLine(b.strikeStartX(left),b.strikeStartY(),tx,b.targetY,paint);
                    paint.setStrokeWidth(2);paint.setColor(0xccffd58b);
                    canvas.drawLine(b.strikeStartX(left),b.strikeStartY(),tx,b.targetY,paint);
                }
                canvas.drawLine(tx-9,b.targetY,tx+9,b.targetY,paint);
                canvas.drawLine(tx,b.targetY-9,tx,b.targetY+9,paint);
            }else if(b.stage==Bossi.STRIKE){
                paint.setColor(0x997cecdf);float r=Bossi.PAW_RADIUS+b.progress()*37;
                canvas.drawCircle(b.pawX(left),b.pawY(left),r,paint);
                canvas.drawCircle(b.pawX(left),b.pawY(left),r*.65f,paint);
            }
        }
        paint.setStyle(Paint.Style.FILL);
    }
}
