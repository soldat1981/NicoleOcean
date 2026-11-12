package com.nicole.ocean;

import java.awt.*;
import java.awt.event.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import javax.imageio.ImageIO;

/** Windows shell around the shared game and renderer. */
public final class DesktopLauncher {
    static FileChannel lockChannel;static FileLock instanceLock;
    public static void main(String[] args)throws Exception{
        if(args.length>0&&args[0].equals("--self-test")){DesktopTest.run(args.length>1?args[1]:"pc-test-results");return;}
        boolean smoke=args.length>0&&args[0].equals("--smoke-test");
        if(smoke){System.setProperty("ocean.saveDir",Files.createTempDirectory("ocean-pc-smoke-").toString());System.setProperty("ocean.mute","true");}
        Path data=new android.content.Context().getFilesDir().toPath();
        lockChannel=FileChannel.open(data.resolve("running.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
        instanceLock=lockChannel.tryLock();if(instanceLock==null){lockChannel.close();JOptionPane.showMessageDialog(null,"Океан Николь уже запущен.");return;}
        Thread.setDefaultUncaughtExceptionHandler((thread,error)->{error.printStackTrace();try{Files.writeString(data.resolve("error.log"),java.time.Instant.now()+"\n"+stack(error));}catch(Exception ignored){}SwingUtilities.invokeLater(()->JOptionPane.showMessageDialog(null,"Ошибка записана в "+data.resolve("error.log"),"Океан Николь",JOptionPane.ERROR_MESSAGE));});
        SwingUtilities.invokeLater(()->open(smoke));
    }
    static String stack(Throwable t){java.io.StringWriter s=new java.io.StringWriter();t.printStackTrace(new java.io.PrintWriter(s));return s.toString();}
    static void open(boolean smoke){
        MainActivity activity=new MainActivity();MainActivity.OceanView view=new MainActivity.OceanView(activity);
        JFrame frame=new JFrame("Океан Николь — мышь / WASD / стрелки · Пробел: рывок · Esc: пауза");
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        JPanel surround=new JPanel(null){@Override public void doLayout(){int width=Math.min(getWidth(),(int)(getHeight()*420.0/800));int height=getHeight();view.setBounds((getWidth()-width)/2,0,width,height);}};
        surround.setBackground(new Color(3,24,38));surround.add(view);frame.setContentPane(surround);
        Dimension screen=Toolkit.getDefaultToolkit().getScreenSize();frame.setSize(Math.min(620,screen.width-80),Math.min(1000,screen.height-80));frame.setMinimumSize(new Dimension(380,500));frame.setLocationRelativeTo(null);
        try{frame.setIconImage(ImageIO.read(DesktopLauncher.class.getResource("/drawable/nicole_icon_art.png")));}catch(Exception ignored){}
        Controls controls=new Controls(view,frame);KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(controls);
        view.desktopInput=controls::apply;
        view.addMouseWheelListener(e->{if(view.screen!=MainActivity.OceanView.PLAY){view.menuScroll=Game.clamp(view.menuScroll+(float)e.getPreciseWheelRotation()*35,0,Math.max(0,view.h-view.viewportH));view.repaint();}});
        activity.close=()->{controls.clear();view.suspend();view.store.close();view.music.release();view.bossiAudio.stop();view.gameAudio.release();KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(controls);frame.dispose();try{instanceLock.release();lockChannel.close();}catch(Exception ignored){}System.exit(0);};
        frame.addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){activity.finish();}public void windowIconified(WindowEvent e){controls.clear();view.pauseGame();}});
        frame.addWindowFocusListener(new WindowAdapter(){public void windowLostFocus(WindowEvent e){controls.clear();view.pauseGame();}public void windowGainedFocus(WindowEvent e){view.resumeFrames();}});
        frame.setVisible(true);view.requestFocusInWindow();
        if(smoke)new javax.swing.Timer(1200,new ActionListener(){int step;float startX;public void actionPerformed(ActionEvent e){try{
            if(step==0){view.newRun();startX=view.g.x;controls.keys.add(KeyEvent.VK_D);}
            else if(step==1){if(view.g.x<=startX)throw new AssertionError("Keyboard movement failed");controls.clear();startX=view.g.x;view.dispatchEvent(new MouseEvent(view,MouseEvent.MOUSE_PRESSED,0,MouseEvent.BUTTON1_DOWN_MASK,(int)(60*view.scale),(int)(300*view.scale),1,false,MouseEvent.BUTTON1));}
            else if(step==2){if(view.g.x>=startX)throw new AssertionError("Mouse movement failed");view.dispatchEvent(new MouseEvent(view,MouseEvent.MOUSE_RELEASED,0,0,0,0,1,false,MouseEvent.BUTTON1));if(controls.mouseHeld||view.inputX!=0)throw new AssertionError("Mouse release failed");view.pauseGame();if(view.screen!=MainActivity.OceanView.PAUSE)throw new AssertionError("Pause failed");frame.setSize(920,750);}
            else {java.awt.image.BufferedImage shot=new java.awt.image.BufferedImage(surround.getWidth(),surround.getHeight(),2);Graphics2D g=shot.createGraphics();surround.paint(g);g.dispose();Path output=Path.of("dist/qa-pc/window-smoke.png");Files.createDirectories(output.getParent());ImageIO.write(shot,"png",output.toFile());Files.writeString(Path.of("dist/qa-pc/window-smoke.txt"),"PASS: native window, keyboard and mouse movement, mouse release, pause, resize, rendering, close/save\n");((javax.swing.Timer)e.getSource()).stop();activity.finish();}step++;
        }catch(Exception|AssertionError ex){ex.printStackTrace();System.exit(1);}}}).start();
    }
    static final class Controls implements KeyEventDispatcher {
        final MainActivity.OceanView view;final JFrame frame;final Set<Integer> keys=new HashSet<>();boolean mouseHeld;float mouseX,mouseY;
        Controls(MainActivity.OceanView v,JFrame f){view=v;frame=f;view.desktopPointer=this::pointer;view.desktopClear=()->mouseHeld=false;}
        boolean pointer(android.view.MotionEvent event){
            int action=event.getActionMasked();
            if(view.screen!=MainActivity.OceanView.PLAY){mouseHeld=false;return false;}
            float x=event.getX(0)/view.scale,y=(event.getY(0)-view.padTop)/view.scale;
            if(action==android.view.MotionEvent.ACTION_DOWN){
                for(MainActivity.OceanView.Button button:view.buttons)if(button.r.contains(x,y+view.menuScroll))return false;
                mouseHeld=true;mouseX=x;mouseY=y;apply();return true;
            }
            if(!mouseHeld)return false;
            if(action==android.view.MotionEvent.ACTION_MOVE){mouseX=x;mouseY=y;}
            else if(action==android.view.MotionEvent.ACTION_UP||action==android.view.MotionEvent.ACTION_CANCEL)mouseHeld=false;
            apply();return true;
        }
        public boolean dispatchKeyEvent(KeyEvent e){if(frame!=null&&!frame.isActive())return false;int k=e.getKeyCode();if(e.getID()==KeyEvent.KEY_RELEASED){keys.remove(k);return recognized(k);}if(e.getID()!=KeyEvent.KEY_PRESSED||!recognized(k))return false;if(!keys.add(k))return true;
            if(k==KeyEvent.VK_ESCAPE){clear();keys.add(k);view.back();}else if(k==KeyEvent.VK_P){if(view.screen==MainActivity.OceanView.PLAY)view.pauseGame();else if(view.screen==MainActivity.OceanView.PAUSE)view.doAction(7);}
            else if(k==KeyEvent.VK_ENTER){if(view.screen==MainActivity.OceanView.HOME)view.doAction(1);else if(view.screen==MainActivity.OceanView.TUTORIAL)view.doAction(11);else if(view.screen==MainActivity.OceanView.PAUSE)view.doAction(7);}
            else if(k==KeyEvent.VK_F11&&frame!=null){frame.setExtendedState(frame.getExtendedState()==JFrame.MAXIMIZED_BOTH?JFrame.NORMAL:JFrame.MAXIMIZED_BOTH);}return true;}
        boolean recognized(int k){return k==KeyEvent.VK_W||k==KeyEvent.VK_A||k==KeyEvent.VK_S||k==KeyEvent.VK_D||k==KeyEvent.VK_UP||k==KeyEvent.VK_DOWN||k==KeyEvent.VK_LEFT||k==KeyEvent.VK_RIGHT||k==KeyEvent.VK_SPACE||k==KeyEvent.VK_SHIFT||k==KeyEvent.VK_ESCAPE||k==KeyEvent.VK_ENTER||k==KeyEvent.VK_P||k==KeyEvent.VK_F11;}
        int axis(int positive,int positive2,int negative,int negative2){return (keys.contains(positive)||keys.contains(positive2)?1:0)-(keys.contains(negative)||keys.contains(negative2)?1:0);}
        void apply(){
            view.bossiAudio.update();float x=axis(KeyEvent.VK_D,KeyEvent.VK_RIGHT,KeyEvent.VK_A,KeyEvent.VK_LEFT),y=axis(KeyEvent.VK_S,KeyEvent.VK_DOWN,KeyEvent.VK_W,KeyEvent.VK_UP);
            if(x==0&&y==0&&mouseHeld){
                float cameraX=Game.clamp(view.g.x-view.w*.5f,0,Game.WORLD_W-view.w),cameraY=Game.clamp(view.g.y-view.h*.46f,80,Game.WORLD_H-view.h+30);
                x=mouseX-(view.g.x-cameraX);y=mouseY-(view.g.y-cameraY);
                float distance=(float)Math.hypot(x,y);
                if(distance<=8){x=y=0;}else{float strength=Math.min(1,(distance-8)/32);x=x/distance*strength;y=y/distance*strength;}
            }
            float length=Math.max(1,(float)Math.hypot(x,y));view.inputX=x/length;view.inputY=y/length;
            view.boostRequested=keys.contains(KeyEvent.VK_SPACE)||keys.contains(KeyEvent.VK_SHIFT);
        }
        void clear(){keys.clear();view.clearInput();}
    }
}
