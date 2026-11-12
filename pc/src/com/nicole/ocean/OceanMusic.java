package com.nicole.ocean;
import java.io.*;import java.nio.file.*;import java.util.*;
final class OceanMusic {
    final DesktopAudio audio=new DesktopAudio();final Path file;final Random random=new Random();
    boolean enabled=true,effectsEnabled=true,playing;int track=-1,remaining;long position;Runnable onFocusLost=()->{};
    OceanMusic(android.content.Context c){file=c.getFilesDir().toPath().resolve("music.properties");Properties p=new Properties();try(InputStream in=Files.newInputStream(file)){p.load(in);enabled=Boolean.parseBoolean(p.getProperty("enabled","true"));track=Integer.parseInt(p.getProperty("track","-1"));remaining=Integer.parseInt(p.getProperty("remaining","0"))&1023;position=Long.parseLong(p.getProperty("position","0"));}catch(Exception ignored){}if(track<0||track>9)track=-1;position=Math.max(0,position);}
    void next(){audio.stop();playing=false;if(remaining==0){remaining=1023;if(track>=0)remaining&=~(1<<track);}int pick=random.nextInt(Integer.bitCount(remaining));for(int i=0;i<10;i++)if((remaining&(1<<i))!=0&&pick--==0){track=i;remaining&=~(1<<i);break;}position=0;}
    void userResume(){audio.failed=false;}
    void toggle(){enabled=!enabled;if(!enabled)pause(true);else userResume();save(true);}
    void update(boolean play,boolean speaking,float dt){if(!play||!enabled){if(playing)pause(true);return;}if(Boolean.getBoolean("ocean.mute"))return;if(audio.ended())next();if(!playing&&!audio.failed){if(track<0)next();audio.play(String.format(java.util.Locale.ROOT,"music/ocean_%02d.wav",track+1),position);playing=true;}audio.gain(speaking?.045f:.18f);}
    void pause(boolean user){if(playing&&!audio.loading)position=audio.position();audio.stop();playing=false;}
    void save(boolean wait){Properties p=new Properties();p.setProperty("enabled",Boolean.toString(enabled));p.setProperty("track",Integer.toString(track));p.setProperty("remaining",Integer.toString(remaining));p.setProperty("position",Long.toString(playing&&!audio.loading?audio.position():position));try{Path pending=file.resolveSibling("music.pending");try(OutputStream out=Files.newOutputStream(pending)){p.store(out,"Nicole Ocean PC");}try{Files.move(pending,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(pending,file,StandardCopyOption.REPLACE_EXISTING);}}catch(IOException e){System.err.println("Music preferences: "+e.getMessage());}}
    void release(){audio.stop();}
}
