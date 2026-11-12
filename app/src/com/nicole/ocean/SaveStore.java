package com.nicole.ocean;

import android.content.Context;
import android.util.AtomicFile;
import java.io.*;
import java.util.concurrent.*;

/** Two independently verified, alternating slots, each written atomically. No cloud access. */
final class SaveStore {
    private final AtomicFile[] slots;
    private final ExecutorService writer=Executors.newSingleThreadExecutor();
    private final Object lock=new Object();
    private byte[] pending;
    private long pendingSequence,completedSequence=-1;
    private boolean draining,closed;
    volatile boolean failed;
    boolean recovered, damaged;
    SaveStore(Context context){
        File dir=context.getFilesDir();
        slots=new AtomicFile[]{new AtomicFile(new File(dir,"ocean-a.save")),new AtomicFile(new File(dir,"ocean-b.save"))};
    }
    Game load(){
        Game latest=null;int invalid=0;
        for(AtomicFile slot:slots){
            if(!slot.getBaseFile().exists()&&!new File(slot.getBaseFile()+".bak").exists())continue;
            try{Game g=Game.restore(slot.readFully());if(latest==null||g.sequence>latest.sequence)latest=g;}
            catch(Exception e){invalid++;}
        }
        recovered=invalid>0&&latest!=null;damaged=invalid>0&&latest==null;
        return latest==null?new Game():latest;
    }
    void save(Game game,boolean wait){
        if(damaged)return; // Keep damaged originals until the user explicitly starts a new profile.
        try{
            game.sequence++;final long seq=game.sequence;final byte[] data=game.snapshot();
            synchronized(lock){
                if(closed){failed=true;return;}
                // At most one in-flight and one newest snapshot, even on a slow/full disk.
                pending=data;pendingSequence=seq;
                if(!draining){draining=true;writer.execute(this::drain);}
                if(wait){
                    long end=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(1500);
                    while(completedSequence<seq){long remaining=end-System.nanoTime();if(remaining<=0){failed=true;break;}TimeUnit.NANOSECONDS.timedWait(lock,remaining);}
                }
            }
        }catch(InterruptedException ex){Thread.currentThread().interrupt();failed=true;
        }catch(Exception ex){failed=true;}
    }
    private void drain(){
        while(true){
            byte[] bytes;long seq;
            synchronized(lock){if(pending==null){draining=false;lock.notifyAll();return;}bytes=pending;seq=pendingSequence;pending=null;}
            AtomicFile slot=slots[(int)(seq&1)];FileOutputStream out=null;
            try{out=slot.startWrite();out.write(bytes);slot.finishWrite(out);failed=false;}
            catch(Exception ex){if(out!=null)slot.failWrite(out);failed=true;}
            synchronized(lock){completedSequence=seq;lock.notifyAll();}
        }
    }
    void close(){synchronized(lock){closed=true;writer.shutdown();}}
}
