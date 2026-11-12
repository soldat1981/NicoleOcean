package android.content.res;
import java.io.*;import java.util.*;
public class Resources {private final Map<Integer,String> ids=new HashMap<>();public int getIdentifier(String name,String kind,String pkg){int id=name.hashCode();ids.put(id,name);return id;}public InputStream open(int id)throws IOException{InputStream in=Resources.class.getResourceAsStream("/drawable/"+ids.get(id)+".png");if(in==null)throw new FileNotFoundException(ids.get(id));return in;}}
