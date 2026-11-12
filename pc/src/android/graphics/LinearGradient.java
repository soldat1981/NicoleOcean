package android.graphics;
import java.awt.*;import java.awt.geom.*;
public class LinearGradient extends Shader {final Point2D start,end;final java.awt.Color[] colors;final float[] stops;
 public LinearGradient(float x,float y,float xx,float yy,int a,int b,TileMode mode){this(x,y,xx,yy,new int[]{a,b},null,mode);}
 public LinearGradient(float x,float y,float xx,float yy,int[] c,float[] f,TileMode mode){start=new Point2D.Float(x,y);end=new Point2D.Float(xx,yy);colors=colors(c);stops=stops(c.length,f);}
 java.awt.Paint awt(){return new LinearGradientPaint(start,end,stops,colors,MultipleGradientPaint.CycleMethod.NO_CYCLE,MultipleGradientPaint.ColorSpaceType.SRGB,transform);}
}
