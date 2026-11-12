package com.nicole.ocean;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Reproducible demonstration: real simulation/rendering, staged starting positions, no AI video. */
public final class ReefVideo {
    static final int WIDTH=504,HEIGHT=960,FPS=30,OVERVIEW=45,LIVE=195;
    public static void main(String[] args)throws Exception{
        if(args.length!=2)throw new IllegalArgumentException("ReefVideo ffmpeg.exe output.mp4");
        Path output=Path.of(args[1]).toAbsolutePath();Files.createDirectories(output.getParent());
        System.setProperty("ocean.mute","true");System.setProperty("ocean.saveDir",Files.createTempDirectory("ocean-reef-film-").toString());
        Process encoder=new ProcessBuilder(args[0],"-y","-hide_banner","-loglevel","warning","-f","rawvideo","-pixel_format","bgr24","-video_size",WIDTH+"x"+HEIGHT,"-framerate","30","-i","pipe:0","-stream_loop","-1","-i","app/assets/audio/music/ocean_01.wav","-map","0:v","-map","1:a","-c:v","libx264","-preset","fast","-crf","19","-pix_fmt","yuv420p","-c:a","aac","-b:a","128k","-af","volume=0.35,afade=t=in:d=1,afade=t=out:st=86:d=2","-shortest","-movflags","+faststart",output.toString()).redirectError(output.resolveSibling("reef-video-encoder.log").toFile()).start();
        Throwable[] failure={null};int[] deaths={0};
        try(OutputStream pipe=new BufferedOutputStream(encoder.getOutputStream(),WIDTH*HEIGHT*3)){
            SwingUtilities.invokeAndWait(()->{MainActivity.OceanView v=null;try{
                v=new MainActivity.OceanView(new MainActivity());v.setSize(WIDTH,HEIGHT);v.running=false;
                BufferedImage frame=new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_3BYTE_BGR);byte[] pixels=((DataBufferByte)frame.getRaster().getDataBuffer()).getData();
                for(int tier=0;tier<=10;tier++){
                    Game g=new Game();g.seed=87123+tier*729;g.stage=g.highestStage=Math.min(58,tier*6);g.home.victories=(1<<tier)-1;g.decor=tier*8/10;g.start();g.tutorialSeen=true;g.time=25;g.x=HomeReef.X;g.y=HomeReef.Y+170;g.level=Math.min(Oceans.goalLevel(g.stage)-1,g.level+(tier==0?0:1));g.bossi.wait=g.hazard.wait=1000;g.messageTime=0;g.fish.clear();g.items.clear();
                    for(int n=0;n<6;n++){float a=n*.7854f;Game.Fish fish=new Game.Fish(HomeReef.X+(float)Math.cos(a)*220,HomeReef.Y+100+(float)Math.sin(a)*220,Math.max(1,g.level-2-n%3),n%4);fish.vx=(float)Math.cos(a+.7f);fish.vy=(float)Math.sin(a+.7f);fish.turn=3;g.fish.add(fish);}
                    for(int n=0;n<1;n++){Game.Fish fish=new Game.Fish(HomeReef.X+(n==0?-330:330),HomeReef.Y-160,Math.min(56,g.level+1+n),0);fish.turn=3;g.fish.add(fish);}
                    for(int n=0;n<5;n++)g.home.food.add(new HomeReef.Food(HomeReef.X-100+n*45,HomeReef.Y+145+n%2*30,n%3));
                    g.home.fireWait=.8f;g.home.powerWait=2.5f;v.g=g;v.screen=MainActivity.OceanView.REEF;v.clock=0;
                    for(int n=0;n<OVERVIEW+LIVE;n++){
                        boolean playing=n>=OVERVIEW;v.screen=playing?MainActivity.OceanView.PLAY:MainActivity.OceanView.REEF;
                        if(playing&&g.active){float t=(n-OVERVIEW)/(float)FPS;
                            float tx=HomeReef.X+(float)Math.sin(t*.8f)*160,ty=HomeReef.Y+90+(float)Math.cos(t*.65f)*95;
                            float ix=(tx-g.x)/90,iy=(ty-g.y)/90;
                            for(Game.Fish fish:g.fish)if(fish.level>g.level){float d=Game.dist(g.x,g.y,fish.x,fish.y);if(d<220){ix+=(g.x-fish.x)/Math.max(1,d)*1.5f;iy+=(g.y-fish.y)/Math.max(1,d)*1.5f;}}
                            for(int tick=0;tick<2;tick++)g.update(FrameStepper.STEP,ix,iy,420,800);
                            if(!g.active)deaths[0]++;
                        }
                        v.clock+=1f/FPS;Graphics2D graphics=frame.createGraphics();
                        try(android.graphics.Canvas c=new android.graphics.Canvas(graphics)){
                            v.onDraw(c);c.save();c.scale(WIDTH/420f,WIDTH/420f);
                            v.box(c,25,playing?746:154,370,34,12,0xe5083044);v.text(c,"Риф "+tier+" / 10 · "+HomeReef.BUILDINGS[tier],210,playing?768:176,12,MainActivity.OceanView.GOLD,true,true);
                            if(playing&&!g.active){v.box(c,45,360,330,80,18,0xe5083044);v.text(c,"Риф не даёт неуязвимости",210,394,17,MainActivity.OceanView.WHITE,true,true);v.text(c,"От сильных рыб всё ещё нужно уплывать",210,419,10,MainActivity.OceanView.MINT,false,true);}
                            c.restore();
                        }finally{graphics.dispose();}
                        if(n==OVERVIEW+90)ImageIO.write(frame,"png",output.resolveSibling(String.format(java.util.Locale.ROOT,"reef-video-stage-%02d.png",tier)).toFile());
                        pipe.write(pixels);
                    }
                    System.out.println("Video chapter "+tier+"/10 complete");
                }
                Files.writeString(output.resolveSibling("reef-video-report.txt"),"11 chapters; 88 seconds; 504x960; 30 fps; real Game.update at 60 Hz; real OceanView renderer; staged fish and cooldowns; visitors/environmental events deferred to focus on home defence; simulated hero deaths: "+deaths[0]+"\n");
            }catch(Throwable t){failure[0]=t;}finally{if(v!=null){v.store.close();v.music.release();v.bossiAudio.stop();v.gameAudio.release();}}});
        }
        int exit=encoder.waitFor();if(failure[0]!=null)throw new RuntimeException(failure[0]);if(exit!=0)throw new IOException("FFmpeg exit "+exit);System.out.println("Created "+output);
    }
}

