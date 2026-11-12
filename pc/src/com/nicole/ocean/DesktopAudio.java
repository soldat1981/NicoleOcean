package com.nicole.ocean;

import javax.sound.sampled.*;
import java.io.*;
import java.util.concurrent.*;

/** Decode/open off the UI thread. Generation tokens cancel stale playback requests. */
final class DesktopAudio {
    static final ExecutorService IO=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"Ocean audio");t.setDaemon(true);return t;});
    volatile Clip clip;
    private volatile int generation;
    volatile boolean loading,failed;
    volatile float volume=0.6f;
    void play(String name,long position){
        stop();if(Boolean.getBoolean("ocean.mute"))return;
        int token=generation;loading=true;failed=false;
        IO.execute(()->{
            Clip opened=null;
            try(InputStream raw=DesktopAudio.class.getResourceAsStream("/audio/"+name);
                AudioInputStream stream=AudioSystem.getAudioInputStream(new BufferedInputStream(raw))){
                opened=AudioSystem.getClip();opened.open(stream);
                synchronized(this){
                    if(token!=generation){opened.close();return;}
                    clip=opened;opened.setMicrosecondPosition(Math.min(position,Math.max(0,opened.getMicrosecondLength()-1000)));
                    gain(volume);opened.start();loading=false;
                }
            }catch(Exception ex){if(opened!=null)opened.close();if(token==generation){failed=true;loading=false;}System.err.println("Audio unavailable: "+name+": "+ex.getMessage());}
        });
    }
    synchronized void gain(float value){volume=value;Clip c=clip;if(c!=null&&c.isOpen()&&c.isControlSupported(FloatControl.Type.MASTER_GAIN)){FloatControl g=(FloatControl)c.getControl(FloatControl.Type.MASTER_GAIN);g.setValue(Math.max(g.getMinimum(),Math.min(g.getMaximum(),20f*(float)Math.log10(Math.max(.001f,value)))));}}
    synchronized void stop(){generation++;loading=false;Clip c=clip;clip=null;if(c!=null){c.stop();c.close();}}
    long position(){Clip c=clip;return c==null?0:c.getMicrosecondPosition();}
    boolean ended(){Clip c=clip;return c!=null&&!c.isRunning()&&c.getFramePosition()>=c.getFrameLength();}
}
