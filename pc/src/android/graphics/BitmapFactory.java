package android.graphics;
public class BitmapFactory {public static Bitmap decodeResource(android.content.res.Resources r,int id){try(java.io.InputStream in=r.open(id)){return new Bitmap(javax.imageio.ImageIO.read(in));}catch(Exception e){throw new IllegalStateException("Missing game image",e);}}}
