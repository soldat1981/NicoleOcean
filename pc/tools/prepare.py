"""Prepare desktop copies; the Android gameplay/rendering sources remain authoritative."""
from pathlib import Path
root = Path(__file__).resolve().parents[2]
target = root/'build/pc/generated/com/nicole/ocean'
target.mkdir(parents=True, exist_ok=True)
source = root/'app/src/com/nicole/ocean'
for p in source.glob('*.java'):
    if p.stem in ('MainActivity','GameAudio','BossiAudio','OceanMusic'): continue
    text = p.read_text(encoding='utf-8')
    if p.stem == 'OceanBackdrop':
        text = text.replace('Canvas cached=new Canvas(base);', 'try(Canvas cached=new Canvas(base)){')
        text = text.replace('cached.drawRect(0,0,w,h,p);p.setShader(null);','cached.drawRect(0,0,w,h,p);p.setShader(null);}')
    (target/p.name).write_text(text,encoding='utf-8')
text = (source/'MainActivity.java').read_text(encoding='utf-8')
text = '''package com.nicole.ocean;
import android.graphics.*;import android.view.*;import android.app.AlertDialog;import java.util.*;
public final class MainActivity extends android.content.Context {
 Runnable close=()->{}; public void finish(){close.run();}
''' + text[text.index('    static final class OceanView'):]
text = text.replace('long previous,renderedFrames;', 'Runnable desktopInput=()->{};long previous,renderedFrames;')
text = text.replace('Runnable desktopInput=()->{};', 'Runnable desktopInput=()->{},desktopClear=()->{};java.util.function.Predicate<MotionEvent> desktopPointer=e->false;')
text = text.replace('void clearInput(){', 'void clearInput(){desktopClear.run();')
text = text.replace('public boolean onTouchEvent(MotionEvent e){', 'public boolean onTouchEvent(MotionEvent e){if(desktopPointer.test(e))return true;')
text = text.replace('if(screen==PLAY){\n                    int beforeSnack', 'if(screen==PLAY){\n                    desktopInput.run();\n                    int beforeSnack')
text = text.replace('Океан Николь. Плыви джойстиком; выведи палец чуть за круг для ускорения. Меню и настройки доступны касанием.', 'Океан Николь. WASD или стрелки — движение, пробел — рывок, Esc — пауза. Меню — мышью.')
text = text.replace('Выведи палец чуть за круг — ускоришься.', 'WASD / стрелки — плыть, пробел — рывок.')
text = text.replace('на телефоне','на компьютере')
text = text.replace('Плыви за пальцем','Плыви мышью или WASD')
text = text.replace('WASD / стрелки — плыть, пробел — рывок.','Зажми левую кнопку — плыви к курсору.')
text = text.replace('За круг — ускорение','Пробел — ускорение')
start = text.index('        void drawControls(Canvas c){')
end = text.index('        void drawPause(Canvas c){', start)
text = text[:start] + '''        void drawControls(Canvas c){
            box(c,35,h-53,350,41,14,0x99083c51);
            text(c,"ЛКМ — плыть к курсору · WASD / стрелки",210,h-36,10,MUTED,true,true);
            text(c,g.cooldown<=0?"Пробел / Shift — рывок":"Рывок через "+(Math.ceil(g.cooldown*10)/10)+" с",210,h-21,10,MINT,true,true);
        }
''' + text[end:]
(target/'MainActivity.java').write_text(text,encoding='utf-8')
