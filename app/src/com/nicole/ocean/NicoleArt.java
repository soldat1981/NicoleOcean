package com.nicole.ocean;

import android.content.Context;
import android.graphics.*;

/** The real face remains a passive translucent background; goggles are runtime game graphics. */
final class NicoleArt {
    final Bitmap atlas;
    final Rect[] faces=new Rect[3];
    final RectF box=new RectF();
    final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    final Path path=new Path();
    NicoleArt(Context context){
        int id=context.getResources().getIdentifier("nicole_faces","drawable",context.getPackageName());
        atlas=BitmapFactory.decodeResource(context.getResources(),id);
        if(atlas==null)throw new IllegalStateException("Nicole faces missing");
        for(int i=0;i<3;i++)faces[i]=trim(atlas,new Rect(atlas.getWidth()*i/3,0,atlas.getWidth()*(i+1)/3,atlas.getHeight()));
    }
    static Rect trim(Bitmap image,Rect bounds){
        int left=bounds.right,top=bounds.bottom,right=bounds.left,bottom=bounds.top;
        int[] row=new int[bounds.width()];
        for(int y=bounds.top;y<bounds.bottom;y++){
            image.getPixels(row,0,row.length,bounds.left,y,row.length,1);
            for(int x=0;x<row.length;x++)if(Color.alpha(row[x])>16){left=Math.min(left,bounds.left+x);right=Math.max(right,bounds.left+x+1);top=Math.min(top,y);bottom=Math.max(bottom,y+1);}
        }
        return left<right&&top<bottom?new Rect(left,top,right,bottom):new Rect(bounds);
    }
    void color(int rgb,int alpha,boolean stroke,float width){paint.setColor(rgb);paint.setAlpha(alpha);paint.setStyle(stroke?Paint.Style.STROKE:Paint.Style.FILL);paint.setStrokeWidth(width);paint.setStrokeCap(Paint.Cap.ROUND);}
    void draw(Canvas canvas,Nicole visit,float w,float h){
        if(!visit.visible)return;
        float fade=visit.opacity();if(fade<=0)return;
        Rect src=faces[visit.face];float ww=visit.width,hh=ww*src.height()/src.width(),cx=visit.x,top=visit.y-hh/2;
        box.set(cx-ww/2,top,cx+ww/2,top+hh);color(Color.WHITE,(int)(155*fade),false,1);
        canvas.drawBitmap(atlas,src,box,paint);
        if(visit.gear==0)return;
        float eyeY=top+hh*(visit.face==2?.55f:.475f),eyeGap=ww*.17f;
        int alpha=(int)(165*fade);
        color(0xffb18bdf,alpha,true,ww*.012f);
        float strapEnd=visit.gear==1?.305f:.36f;
        canvas.drawLine(cx-ww*.45f,eyeY,cx-ww*strapEnd,eyeY,paint);
        canvas.drawLine(cx+ww*strapEnd,eyeY,cx+ww*.45f,eyeY,paint);
        if(visit.gear==1){
            for(int side=-1;side<=1;side+=2){
                box.set(cx+side*eyeGap-ww*.135f,eyeY-ww*.073f,cx+side*eyeGap+ww*.135f,eyeY+ww*.073f);
                color(0xff69ddea,(int)(38*fade),false,1);canvas.drawOval(box,paint);
                color(0xff99ecdd,alpha,true,ww*.011f);canvas.drawOval(box,paint);
            }
            color(0xffa8f0dd,alpha,true,ww*.01f);canvas.drawLine(cx-ww*.04f,eyeY,cx+ww*.04f,eyeY,paint);
        }else{
            box.set(cx-ww*.36f,eyeY-ww*.13f,cx+ww*.36f,eyeY+ww*.14f);
            color(0xff50c9e7,(int)(35*fade),false,1);canvas.drawRoundRect(box,ww*.065f,ww*.065f,paint);
            color(0xfff3b6c4,alpha,true,ww*.013f);canvas.drawRoundRect(box,ww*.065f,ww*.065f,paint);
            path.reset();path.moveTo(cx-ww*.055f,eyeY+ww*.14f);path.lineTo(cx,eyeY+ww*.058f);path.lineTo(cx+ww*.055f,eyeY+ww*.14f);canvas.drawPath(path,paint);
        }
        paint.setStyle(Paint.Style.FILL);paint.setAlpha(255);
    }
}
