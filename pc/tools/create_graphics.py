"""Create the deliberately small Java2D adapter used by the shared game renderer."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'src'
files={
'android/graphics/Color.java': '''package android.graphics;
public final class Color {
 public static final int WHITE=0xffffffff,BLACK=0xff000000,TRANSPARENT=0;
 public static int red(int c){return c>>>16&255;} public static int green(int c){return c>>>8&255;} public static int blue(int c){return c&255;} public static int alpha(int c){return c>>>24;}
 public static int rgb(int r,int g,int b){return 0xff000000|(r<<16)|(g<<8)|b;}
 public static int HSVToColor(int a,float[] h){return (a<<24)|(java.awt.Color.HSBtoRGB(h[0]/360,h[1],h[2])&0xffffff);}
}''',
'android/graphics/Rect.java': '''package android.graphics;
public class Rect { public int left,top,right,bottom; public Rect(){} public Rect(int l,int t,int r,int b){set(l,t,r,b);} public Rect(Rect r){this(r.left,r.top,r.right,r.bottom);} public void set(int l,int t,int r,int b){left=l;top=t;right=r;bottom=b;} public int width(){return right-left;} public int height(){return bottom-top;} }''',
'android/graphics/RectF.java': '''package android.graphics;
public class RectF { public float left,top,right,bottom; public RectF(){} public RectF(float l,float t,float r,float b){set(l,t,r,b);} public void set(float l,float t,float r,float b){left=l;top=t;right=r;bottom=b;} public float width(){return right-left;} public float height(){return bottom-top;} public boolean contains(float x,float y){return x>=left&&x<right&&y>=top&&y<bottom;} public static boolean intersects(RectF a,RectF b){return a.left<b.right&&b.left<a.right&&a.top<b.bottom&&b.top<a.bottom;} }''',
'android/graphics/Typeface.java': '''package android.graphics;
public class Typeface {public static final int NORMAL=0,BOLD=1; final int style; private Typeface(int s){style=s;} public static Typeface create(String name,int s){return new Typeface(s);} }''',
'android/graphics/Matrix.java': '''package android.graphics;
public class Matrix {final java.awt.geom.AffineTransform value=new java.awt.geom.AffineTransform();public void setScale(float x,float y){value.setToScale(x,y);}public void postTranslate(float x,float y){value.preConcatenate(java.awt.geom.AffineTransform.getTranslateInstance(x,y));}}''',
'android/graphics/Path.java': '''package android.graphics;
public class Path {final java.awt.geom.Path2D.Float value=new java.awt.geom.Path2D.Float();public void reset(){value.reset();}public void moveTo(float x,float y){value.moveTo(x,y);}public void lineTo(float x,float y){value.lineTo(x,y);}public void quadTo(float a,float b,float c,float d){value.quadTo(a,b,c,d);}public void cubicTo(float a,float b,float c,float d,float e,float f){value.curveTo(a,b,c,d,e,f);}public void close(){value.closePath();}}''',
'android/graphics/Shader.java': '''package android.graphics;
import java.awt.*;import java.awt.geom.*;
public abstract class Shader {public enum TileMode{CLAMP} AffineTransform transform=new AffineTransform();public void setLocalMatrix(Matrix m){transform=new AffineTransform(m.value);}abstract java.awt.Paint awt();static java.awt.Color[] colors(int[] values){java.awt.Color[] a=new java.awt.Color[values.length];for(int i=0;i<a.length;i++)a[i]=new java.awt.Color(values[i],true);return a;}static float[] stops(int n,float[] f){if(f!=null)return f.clone();float[] a=new float[n];for(int i=0;i<n;i++)a[i]=i/(float)(n-1);return a;}}''',
'android/graphics/LinearGradient.java': '''package android.graphics;
import java.awt.*;import java.awt.geom.*;
public class LinearGradient extends Shader {final Point2D start,end;final java.awt.Color[] colors;final float[] stops;
 public LinearGradient(float x,float y,float xx,float yy,int a,int b,TileMode mode){this(x,y,xx,yy,new int[]{a,b},null,mode);}
 public LinearGradient(float x,float y,float xx,float yy,int[] c,float[] f,TileMode mode){start=new Point2D.Float(x,y);end=new Point2D.Float(xx,yy);colors=colors(c);stops=stops(c.length,f);}
 java.awt.Paint awt(){return new LinearGradientPaint(start,end,stops,colors,MultipleGradientPaint.CycleMethod.NO_CYCLE,MultipleGradientPaint.ColorSpaceType.SRGB,transform);}
}''',
'android/graphics/RadialGradient.java': '''package android.graphics;
import java.awt.*;import java.awt.geom.*;
public class RadialGradient extends Shader {final Point2D center;final float radius;final java.awt.Color[] colors;final float[] stops;
 public RadialGradient(float x,float y,float r,int[] c,float[] f,TileMode mode){center=new Point2D.Float(x,y);radius=r;colors=colors(c);stops=stops(c.length,f);}
 java.awt.Paint awt(){return new RadialGradientPaint(center,radius,center,stops,colors,MultipleGradientPaint.CycleMethod.NO_CYCLE,MultipleGradientPaint.ColorSpaceType.SRGB,transform);}
}''',
'android/graphics/Paint.java': '''package android.graphics;
public class Paint {
 public static final int ANTI_ALIAS_FLAG=1,FILTER_BITMAP_FLAG=2;public enum Style{FILL,STROKE}public enum Cap{ROUND,BUTT}public enum Align{LEFT,CENTER,RIGHT}
 int color=0xff000000;float stroke=1,size=12;Shader shader;Style style=Style.FILL;Cap cap=Cap.BUTT;Align align=Align.LEFT;Typeface font=Typeface.create("sans-serif",0);
 public Paint(){}public Paint(int flags){}public void setColor(int c){color=c;}public void setAlpha(int a){color=(color&0xffffff)|(Math.max(0,Math.min(255,a))<<24);}public void setStyle(Style s){style=s;}public void setStrokeWidth(float s){stroke=s;}public void setStrokeCap(Cap c){cap=c;}public void setShader(Shader s){shader=s;}public void setTypeface(Typeface t){font=t;}public void setTextSize(float s){size=s;}public void setTextAlign(Align a){align=a;}public void setFilterBitmap(boolean b){}
}''',
'android/graphics/Bitmap.java': '''package android.graphics;
import java.awt.image.BufferedImage;
public class Bitmap {public enum Config{ARGB_8888}public final BufferedImage image;public Bitmap(BufferedImage i){image=i;}public static Bitmap createBitmap(int w,int h,Config c){return new Bitmap(new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB));}public int getWidth(){return image.getWidth();}public int getHeight(){return image.getHeight();}public void getPixels(int[] p,int offset,int stride,int x,int y,int w,int h){image.getRGB(x,y,w,h,p,offset,stride);}public boolean hasAlpha(){return image.getColorModel().hasAlpha();}}
''',
'android/graphics/BitmapFactory.java': '''package android.graphics;
public class BitmapFactory {public static Bitmap decodeResource(android.content.res.Resources r,int id){try(java.io.InputStream in=r.open(id)){return new Bitmap(javax.imageio.ImageIO.read(in));}catch(Exception e){throw new IllegalStateException("Missing game image",e);}}}''',
'android/graphics/Canvas.java': '''package android.graphics;
import java.awt.*;import java.awt.geom.*;import java.util.ArrayDeque;
public class Canvas implements AutoCloseable {
 private Graphics2D g;private final ArrayDeque<Graphics2D> stack=new ArrayDeque<>();
 public Canvas(Bitmap b){this(b.image.createGraphics(),false);}public Canvas(Graphics2D graphics){this(graphics,true);}private Canvas(Graphics2D graphics,boolean copy){g=copy?(Graphics2D)graphics.create():graphics;g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);}
 public void save(){stack.push(g);g=(Graphics2D)g.create();}public void restore(){g.dispose();g=stack.pop();}public void translate(float x,float y){g.translate(x,y);}public void scale(float x,float y){g.scale(x,y);}public void rotate(float degrees){g.rotate(Math.toRadians(degrees));}public void rotate(float degrees,float x,float y){g.rotate(Math.toRadians(degrees),x,y);}public void clipRect(float l,float t,float r,float b){g.clip(new Rectangle2D.Float(l,t,r-l,b-t));}
 private void apply(Paint p){g.setComposite(AlphaComposite.SrcOver);if(p.shader==null)g.setPaint(new java.awt.Color(p.color,true));else{g.setPaint(p.shader.awt());g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,(p.color>>>24)/255f));}g.setStroke(new BasicStroke(Math.max(.001f,p.stroke),p.cap==Paint.Cap.ROUND?BasicStroke.CAP_ROUND:BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND));}
 private void shape(Shape s,Paint p){apply(p);if(p.style==Paint.Style.STROKE)g.draw(s);else g.fill(s);}
 public void drawRect(float l,float t,float r,float b,Paint p){shape(new Rectangle2D.Float(l,t,r-l,b-t),p);}public void drawRect(RectF r,Paint p){drawRect(r.left,r.top,r.right,r.bottom,p);}public void drawOval(RectF r,Paint p){shape(new Ellipse2D.Float(r.left,r.top,r.width(),r.height()),p);}public void drawCircle(float x,float y,float r,Paint p){shape(new Ellipse2D.Float(x-r,y-r,r*2,r*2),p);}public void drawRoundRect(RectF r,float rx,float ry,Paint p){shape(new RoundRectangle2D.Float(r.left,r.top,r.width(),r.height(),rx*2,ry*2),p);}public void drawPath(Path path,Paint p){shape(path.value,p);}public void drawLine(float x,float y,float xx,float yy,Paint p){apply(p);g.draw(new Line2D.Float(x,y,xx,yy));}public void drawArc(RectF r,float start,float sweep,boolean center,Paint p){shape(new Arc2D.Float(r.left,r.top,r.width(),r.height(),-start,-sweep,center?Arc2D.PIE:Arc2D.OPEN),p);}
 public void drawText(String text,float x,float y,Paint p){apply(p);g.setFont(new Font("SansSerif",p.font.style,1).deriveFont(p.size));float width=(float)g.getFont().getStringBounds(text,g.getFontRenderContext()).getWidth();if(p.align==Paint.Align.CENTER)x-=width/2;else if(p.align==Paint.Align.RIGHT)x-=width;g.drawString(text,x,y);}
 public void drawBitmap(Bitmap b,Rect source,RectF dest,Paint p){if(b==null)return;Rect s=source==null?new Rect(0,0,b.getWidth(),b.getHeight()):source;g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,(p.color>>>24)/255f));g.drawImage(b.image,(int)dest.left,(int)dest.top,(int)dest.right,(int)dest.bottom,s.left,s.top,s.right,s.bottom,null);g.setComposite(AlphaComposite.SrcOver);}
 public void drawColor(int c){g.setColor(new java.awt.Color(c,true));g.fillRect(0,0,10000,10000);}public void close(){while(!stack.isEmpty())restore();g.dispose();}
}''',
'android/util/SparseArray.java': '''package android.util;public class SparseArray<T> {private final java.util.HashMap<Integer,T> map=new java.util.HashMap<>();public T get(int key){return map.get(key);}public void put(int key,T v){map.put(key,v);}}''',
'android/util/AtomicFile.java': '''package android.util;
import java.io.*;import java.nio.file.*;
public class AtomicFile {private final File file;public AtomicFile(File f){file=f;}public File getBaseFile(){return file;}public byte[] readFully()throws IOException{return Files.readAllBytes(file.toPath());}public FileOutputStream startWrite()throws IOException{Files.createDirectories(file.toPath().getParent());return new FileOutputStream(file+".pending");}public void finishWrite(FileOutputStream out)throws IOException{out.getFD().sync();out.close();try{Files.move(Path.of(file+".pending"),file.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(Path.of(file+".pending"),file.toPath(),StandardCopyOption.REPLACE_EXISTING);}}public void failWrite(FileOutputStream out){try{out.close();Files.deleteIfExists(Path.of(file+".pending"));}catch(IOException ignored){}}}''',
'android/content/res/Resources.java': '''package android.content.res;
import java.io.*;import java.util.*;
public class Resources {private final Map<Integer,String> ids=new HashMap<>();public int getIdentifier(String name,String kind,String pkg){int id=name.hashCode();ids.put(id,name);return id;}public InputStream open(int id)throws IOException{InputStream in=Resources.class.getResourceAsStream("/drawable/"+ids.get(id)+".png");if(in==null)throw new FileNotFoundException(ids.get(id));return in;}}''',
'android/content/Context.java': '''package android.content;
import java.io.*;import java.nio.file.*;
public class Context {private final android.content.res.Resources resources=new android.content.res.Resources();public File getFilesDir(){String override=System.getProperty("ocean.saveDir");String local=System.getenv("LOCALAPPDATA");File dir=new File(override!=null?override:(local!=null?local:System.getProperty("user.home"))+"/NicoleOceanPC");if(!dir.isDirectory()&&!dir.mkdirs())throw new IllegalStateException("Cannot create save folder: "+dir);return dir;}public String getPackageName(){return "com.nicole.ocean";}public android.content.res.Resources getResources(){return resources;}}''',
'android/app/AlertDialog.java': '''package android.app;
import javax.swing.*;
public class AlertDialog {public interface Click{void onClick(Object d,int which);}public static class Builder {String title="",message="",yes="OK",no;Click positive,negative;public Builder(android.content.Context c){}public Builder setTitle(String s){title=s;return this;}public Builder setMessage(String s){message=s;return this;}public Builder setPositiveButton(String s,Click l){yes=s;positive=l;return this;}public Builder setNegativeButton(String s,Click l){no=s;negative=l;return this;}public Builder setCancelable(boolean b){return this;}public void show(){Object[] choices=no==null?new Object[]{yes}:new Object[]{yes,no};int result=JOptionPane.showOptionDialog(null,message,title,JOptionPane.DEFAULT_OPTION,JOptionPane.INFORMATION_MESSAGE,null,choices,choices[0]);if(result==0&&positive!=null)positive.onClick(this,0);else if(negative!=null)negative.onClick(this,1);}}}''',
'android/view/MotionEvent.java': '''package android.view;
public class MotionEvent {public static final int ACTION_DOWN=0,ACTION_UP=1,ACTION_MOVE=2,ACTION_CANCEL=3,ACTION_POINTER_DOWN=5,ACTION_POINTER_UP=6;final int action;final float x,y;public MotionEvent(int a,float x,float y){action=a;this.x=x;this.y=y;}public int getActionMasked(){return action;}public int getActionIndex(){return 0;}public float getX(int i){return x;}public float getY(int i){return y;}public int getPointerId(int i){return 0;}public int findPointerIndex(int i){return i==0?0:-1;}}''',
'android/view/View.java': '''package android.view;
import java.awt.*;import java.awt.event.*;import javax.swing.*;
public class View extends JPanel {
 private final Timer timer=new Timer(16,e->repaint());public View(android.content.Context c){setDoubleBuffered(true);MouseAdapter mouse=new MouseAdapter(){public void mousePressed(MouseEvent e){if(!SwingUtilities.isLeftMouseButton(e))return;requestFocusInWindow();onTouchEvent(new MotionEvent(0,e.getX(),e.getY()));}public void mouseDragged(MouseEvent e){if((e.getModifiersEx()&MouseEvent.BUTTON1_DOWN_MASK)==0)return;onTouchEvent(new MotionEvent(2,e.getX(),e.getY()));}public void mouseReleased(MouseEvent e){if(!SwingUtilities.isLeftMouseButton(e))return;onTouchEvent(new MotionEvent(1,e.getX(),e.getY()));}};addMouseListener(mouse);addMouseMotionListener(mouse);}
 protected void onDraw(android.graphics.Canvas c){}@Override protected void paintComponent(Graphics raw){super.paintComponent(raw);try(android.graphics.Canvas c=new android.graphics.Canvas((Graphics2D)raw)){onDraw(c);}}
 public void setBackgroundColor(int c){setBackground(new java.awt.Color(c,true));}public void setContentDescription(String s){getAccessibleContext().setAccessibleDescription(s);}public interface InsetsListener{Object apply(View v,Insets i);}public static class Insets{public int getSystemWindowInsetTop(){return 0;}public int getSystemWindowInsetBottom(){return 0;}}public void setOnApplyWindowInsetsListener(InsetsListener l){l.apply(this,new Insets());}
 public boolean onTouchEvent(MotionEvent e){return false;}public boolean performClick(){return true;}public void post(Runnable r){SwingUtilities.invokeLater(r);}public void postInvalidateOnAnimation(){timer.setDelay(16);if(isDisplayable()&&!timer.isRunning())timer.start();}public void postInvalidateDelayed(int ms){timer.setDelay(ms);if(isDisplayable()&&!timer.isRunning())timer.start();}@Override public void removeNotify(){timer.stop();super.removeNotify();}@Override public void invalidate(){super.invalidate();repaint();}
}'''
}
for name,source in files.items():
    p=root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(source+'\n',encoding='utf-8')
print('Created',len(files),'Java2D/platform adapter files')
