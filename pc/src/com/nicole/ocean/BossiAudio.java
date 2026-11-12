package com.nicole.ocean;
final class BossiAudio {
    final DesktopAudio audio=new DesktopAudio();Object player;int variation;
    final String[] names={"","bossi_meow","bossi_no","bossi_away","bossi_food","nicole_bloop","nicole_fish","nicole_swim","nicole_dive","nicole_nyam","nicole_playground"};
    BossiAudio(android.content.Context c){}void stop(){audio.stop();player=null;}
    boolean play(int kind){stop();if(kind<1||kind>=names.length)return false;String name=names[kind];if(kind==1){int n=variation++%3;if(n>0)name+=(n+1);}audio.volume=.75f;audio.play(name+".wav",0);player=audio;return true;}
    void update(){if(audio.ended()||audio.failed)stop();}
}
