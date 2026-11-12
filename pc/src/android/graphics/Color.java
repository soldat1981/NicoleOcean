package android.graphics;
public final class Color {
 public static final int WHITE=0xffffffff,BLACK=0xff000000,TRANSPARENT=0;
 public static int red(int c){return c>>>16&255;} public static int green(int c){return c>>>8&255;} public static int blue(int c){return c&255;} public static int alpha(int c){return c>>>24;}
 public static int rgb(int r,int g,int b){return 0xff000000|(r<<16)|(g<<8)|b;}
 public static int HSVToColor(int a,float[] h){return (a<<24)|(java.awt.Color.HSBtoRGB(h[0]/360,h[1],h[2])&0xffffff);}
}
