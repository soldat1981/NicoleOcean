package com.nicole.ocean;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.*;
import java.util.Random;

/** Original bundled music; shuffle bag prevents repeats until all ten have played. */
final class OceanMusic {
    final Context context;
    final SharedPreferences prefs;
    final AudioManager manager;
    final Random random=new Random();
    final AudioFocusRequest focus;
    MediaPlayer player;
    int track,position,remaining,last=-1;
    boolean enabled,allowed,interrupted,hasFocus;
    float volume;
    boolean preparing,paused=true,focusRequested;
    boolean effectsEnabled=true;
    Runnable onFocusLost=()->{};
    OceanMusic(Context context){
        this.context=context; prefs=context.getSharedPreferences("ocean-music",Context.MODE_PRIVATE);
        enabled=prefs.getBoolean("enabled",true);track=prefs.getInt("track",-1);
        if(track<0||track>=10)track=-1;
        position=Math.max(0,prefs.getInt("position",0));remaining=prefs.getInt("remaining",0)&1023;last=track;
        manager=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setOnAudioFocusChangeListener(this::focusChanged).build();
    }
    void focusChanged(int change){
        if(change==AudioManager.AUDIOFOCUS_GAIN){interrupted=false;hasFocus=true;}
        else {interrupted=true;hasFocus=false;pause(false);onFocusLost.run();}
    }
    int choose(){
        if(remaining==0){remaining=1023;if(last>=0)remaining&=~(1<<last);}
        int pick=random.nextInt(Integer.bitCount(remaining));
        for(int i=0;i<10;i++)if((remaining&(1<<i))!=0&&pick--==0){remaining&=~(1<<i);last=i;return i;}
        return 0;
    }
    void next(){releasePlayer();track=choose();position=0;save(false);}
    void userResume(){interrupted=false;}
    void update(boolean play,boolean speaking){update(play,speaking,1f/60);}
    void update(boolean play,boolean speaking,float dt){
        allowed=play&&enabled;
        if(!play||(!enabled&&!effectsEnabled)){pause(true);return;}
        if(interrupted)return;
        if(!hasFocus){focusRequested=true;hasFocus=manager.requestAudioFocus(focus)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;if(!hasFocus){interrupted=true;return;}}
        if(!enabled){pause(false);return;}
        try{
            if(player==null){
                if(track<0)next();
                try(AssetFileDescriptor file=context.getAssets().openFd(String.format(java.util.Locale.ROOT,"audio/music/ocean_%02d.wav",track+1))){
                    player=new MediaPlayer();volume=0;player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
                    player.setDataSource(file.getFileDescriptor(),file.getStartOffset(),file.getLength());
                    preparing=true;
                    player.setOnPreparedListener(ready->{
                        if(player!=ready)return;preparing=false;
                        if(position>0)ready.seekTo(Math.min(position,Math.max(0,ready.getDuration()-1)));
                        ready.setVolume(0,0);if(allowed&&!interrupted&&hasFocus){ready.start();paused=false;}
                    });
                    player.setOnErrorListener((failed,what,extra)->{releasePlayer();interrupted=true;return true;});
                    player.setOnCompletionListener(done->{next();});
                    player.prepareAsync();
                }
            }
            if(preparing)return;
            float target=speaking?.045f:.18f;
            if(Math.abs(target-volume)>.0001f){volume+=(target-volume)*(1-(float)Math.exp(-(speaking?20:2.5f)*Math.max(0,dt)));player.setVolume(volume,volume);}
            if(paused){player.start();paused=false;}
        }catch(Exception ex){releasePlayer();interrupted=true;}
    }
    void toggle(){enabled=!enabled;if(!enabled)pause(true);else userResume();save(true);}
    void pause(boolean abandon){
        if(player!=null&&!preparing&&!paused)try{player.pause();position=player.getCurrentPosition();paused=true;}catch(Exception ignored){}
        if(abandon){allowed=false;if(focusRequested)manager.abandonAudioFocusRequest(focus);hasFocus=focusRequested=false;}
    }
    void save(boolean wait){
        if(player!=null&&!preparing)try{position=player.getCurrentPosition();}catch(Exception ignored){}
        SharedPreferences.Editor e=prefs.edit().putBoolean("enabled",enabled).putInt("track",track).putInt("position",position).putInt("remaining",remaining);
        if(wait)e.commit();else e.apply();
    }
    void releasePlayer(){if(player!=null){player.release();player=null;}preparing=false;paused=true;}
    void release(){pause(true);save(true);releasePlayer();}
}
