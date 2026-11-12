package com.nicole.ocean;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;

/** Bundled WAV clips, never a network TTS request. One clip at a time, stopped with the game. */
final class BossiAudio {
    final Context context;
    MediaPlayer player;
    int meowVariation;
    BossiAudio(Context context){this.context=context;}
    void stop(){if(player!=null){try{player.release();}catch(Exception ignored){}player=null;}}
    boolean play(int kind){
        stop();String[] files={"","bossi_meow.wav","bossi_no.wav","bossi_away.wav","bossi_food.wav","nicole_bloop.wav","nicole_fish.wav","nicole_swim.wav","nicole_dive.wav","nicole_nyam.wav","nicole_playground.wav"};
        if(kind<1||kind>=files.length)return false;
        String name=files[kind];if(kind==1){int variation=meowVariation++%3;name=variation==0?"bossi_meow.wav":"bossi_meow"+(variation+1)+".wav";}
        try(AssetFileDescriptor file=context.getAssets().openFd("audio/"+name)){
            MediaPlayer current=new MediaPlayer();player=current;
            current.setAudioAttributes(new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_GAME).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH).build());
            current.setDataSource(file.getFileDescriptor(),file.getStartOffset(),file.getLength());
            current.setVolume(.75f,.75f);current.setOnCompletionListener(done->{if(player==done)stop();});
            current.setOnPreparedListener(ready->{if(player==ready)ready.start();});
            current.setOnErrorListener((failed,what,extra)->{if(player==failed)stop();return true;});
            current.prepareAsync();return true;
        }catch(Exception ex){stop();return false;}
    }
}
