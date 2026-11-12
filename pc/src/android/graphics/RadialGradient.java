package android.graphics;
import java.awt.*;import java.awt.geom.*;
public class RadialGradient extends Shader {final Point2D center;final float radius;final java.awt.Color[] colors;final float[] stops;
 public RadialGradient(float x,float y,float r,int[] c,float[] f,TileMode mode){center=new Point2D.Float(x,y);radius=r;colors=colors(c);stops=stops(c.length,f);}
 java.awt.Paint awt(){return new RadialGradientPaint(center,radius,center,stops,colors,MultipleGradientPaint.CycleMethod.NO_CYCLE,MultipleGradientPaint.ColorSpaceType.SRGB,transform);}
}
