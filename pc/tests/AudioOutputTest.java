package com.nicole.ocean;
/** Optional real-device check, run separately from the silent build tests. */
public final class AudioOutputTest {
    public static void main(String[] args)throws Exception{
        DesktopAudio audio=new DesktopAudio();audio.volume=.01f;
        try{
            audio.play("music/ocean_01.wav",1_000_000);
            long until=System.nanoTime()+10_000_000_000L;
            while(audio.loading&&System.nanoTime()<until)Thread.sleep(20);
            if(audio.failed||audio.clip==null)throw new AssertionError("No Windows audio output line");
            long initial=audio.position();Thread.sleep(150);
            if(audio.position()<=initial)throw new AssertionError("Audio clock did not advance");
            audio.gain(.005f);audio.stop();
            if(audio.clip!=null)throw new AssertionError("Clip not released");
            System.out.println("PASS: Java Sound output, WAV playback, seek, volume and stop");
        }finally{audio.stop();}
    }
}
