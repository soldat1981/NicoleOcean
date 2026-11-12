package android.graphics;
import java.awt.*;import java.awt.geom.*;
public abstract class Shader {public enum TileMode{CLAMP} AffineTransform transform=new AffineTransform();public void setLocalMatrix(Matrix m){transform=new AffineTransform(m.value);}abstract java.awt.Paint awt();static java.awt.Color[] colors(int[] values){java.awt.Color[] a=new java.awt.Color[values.length];for(int i=0;i<a.length;i++)a[i]=new java.awt.Color(values[i],true);return a;}static float[] stops(int n,float[] f){if(f!=null)return f.clone();float[] a=new float[n];for(int i=0;i<n;i++)a[i]=i/(float)(n-1);return a;}}
