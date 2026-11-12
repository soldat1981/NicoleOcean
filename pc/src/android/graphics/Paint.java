package android.graphics;
public class Paint {
 public static final int ANTI_ALIAS_FLAG=1,FILTER_BITMAP_FLAG=2;public enum Style{FILL,STROKE}public enum Cap{ROUND,BUTT}public enum Align{LEFT,CENTER,RIGHT}
 int color=0xff000000;float stroke=1,size=12;Shader shader;Style style=Style.FILL;Cap cap=Cap.BUTT;Align align=Align.LEFT;Typeface font=Typeface.create("sans-serif",0);
 public Paint(){}public Paint(int flags){}public void setColor(int c){color=c;}public void setAlpha(int a){color=(color&0xffffff)|(Math.max(0,Math.min(255,a))<<24);}public void setStyle(Style s){style=s;}public void setStrokeWidth(float s){stroke=s;}public void setStrokeCap(Cap c){cap=c;}public void setShader(Shader s){shader=s;}public void setTypeface(Typeface t){font=t;}public void setTextSize(float s){size=s;}public void setTextAlign(Align a){align=a;}public void setFilterBitmap(boolean b){}
}
