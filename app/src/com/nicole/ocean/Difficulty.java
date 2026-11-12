package com.nicole.ocean;

/** Stage-locked balance. Argument is the ocean index, never the growing hero level; elapsed time never scales difficulty. */
public final class Difficulty {
    public static int stage(int level){return Math.max(0,Math.min(Oceans.COUNT-1,level));}
    public static float chaseRange(int level){return 180+140*progress(level);}
    public static float chaseSpeed(int level){return 100+30*progress(level);}
    public static float chaseDuration(int level){return 2+2*progress(level);}
    public static float chaseRest(int level){return 3.5f-progress(level);}
    public static int chaseLimit(int level){return 1+stage(level)/20;}
    public static float fleeRange(int level){return 95+105*progress(level);}
    public static float fleeSpeed(int level){return 65+45*progress(level);}
    public static boolean fishDashes(int level){return stage(level)>=1;}
    public static int dashLimit(int level){return 1+stage(level)/20;}
    public static float progress(int level){return stage(level)/(float)(Oceans.COUNT-1);}
    public static float predators(int level){return .12f+.34f*progress(level);}
    public static float dashCooldown(int level){return 4+3*progress(level);}
    public static float bossWarning(int level){return 1.6f-.9f*progress(level);}
    public static float eventWarning(int level){return 3.2f-1.4f*progress(level);}
    public static float eventInterval(int level){return 18-10*progress(level);}
    public static int extraDamage(int level){return Math.round(12*progress(level));}
    public static float heal(int level){return 24-12*progress(level);}
    public static float monsterDamage(int level){return 12+16*progress(level);}
    public static float catDamage(int level){return 14+8*progress(level);}
    public static float fishWarning(int level){return 1.25f-.4f*progress(level);}
}
