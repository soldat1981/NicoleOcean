package com.nicole.ocean;
final class GameAudio {
    final DesktopAudio[] voices=new DesktopAudio[5];int cursor,variation;
    final String[] names={"eat0","eat1","eat2","sonar","growth","hurt","dash","menu"};
    GameAudio(android.content.Context c){for(int i=0;i<voices.length;i++)voices[i]=new DesktopAudio();}
    void play(int index,float pitch){if(index<0||index>=names.length)return;DesktopAudio a=voices[cursor++%voices.length];a.volume=index==7?.32f:.62f;a.play(names[index]+".wav",0);}
    void eat(int combo){play(variation++%3,1);}
    void stop(){for(DesktopAudio a:voices)a.stop();}void release(){stop();}
}
