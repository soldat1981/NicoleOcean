package android.graphics;
public class Typeface {public static final int NORMAL=0,BOLD=1; final int style; private Typeface(int s){style=s;} public static Typeface create(String name,int s){return new Typeface(s);} }
