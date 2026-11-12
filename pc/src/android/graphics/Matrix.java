package android.graphics;
public class Matrix {final java.awt.geom.AffineTransform value=new java.awt.geom.AffineTransform();public void setScale(float x,float y){value.setToScale(x,y);}public void postTranslate(float x,float y){value.preConcatenate(java.awt.geom.AffineTransform.getTranslateInstance(x,y));}}
