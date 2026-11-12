package com.nicole.ocean;
import android.content.Context;
import android.media.*;
/** Short independent Foley voices, separate from Bossi's spoken clips. */
final class GameAudio {
    final SoundPool pool;final int[] clips=new int[8];int loaded,variation;
    GameAudio(Context c){
        pool=new SoundPool.Builder().setMaxStreams(5).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
        pool.setOnLoadCompleteListener((p,id,status)->{if(status==0)loaded++;});
        String[] names={"eat0","eat1","eat2","sonar","growth","hurt","dash","menu"};
        for(int n=0;n<names.length;n++)try(android.content.res.AssetFileDescriptor fd=c.getAssets().openFd("audio/"+names[n]+".wav")){clips[n]=pool.load(fd,1);}catch(Exception e){android.util.Log.w("OceanAudio","Cannot load "+names[n],e);}
    }
    void play(int clip,float pitch){if(clip>=0&&clip<clips.length&&clips[clip]!=0){float v=clip==7?.32f:clip==5?.48f:.62f;pool.play(clips[clip],v,v,1,0,pitch);}}
    void eat(int combo){play(variation,Math.min(1.22f,.96f+Math.min(combo,10)*.02f));variation=(variation+1)%3;}
    void stop(){pool.autoPause();}
    void release(){pool.release();}
}
