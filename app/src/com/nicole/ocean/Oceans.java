package com.nicole.ocean;
/** 5 ordinary oceans followed by one guardian, repeated ten times. Zero-based ocean indices. */
public final class Oceans {
 public static final int COUNT=60,NORMAL_COUNT=50;
 public static boolean guardian(int ocean){return ocean%6==5;}
 public static int completedNormals(int ocean){return ocean-ocean/6;}
 public static int startLevel(int ocean){return 1+completedNormals(ocean);}
 public static int goalLevel(int ocean){return startLevel(ocean)+(guardian(ocean)?4:3);}
 public static int fromNormal(int normal){return normal+normal/5;}
 public static float decorX(int ocean,int n){return 110+(n*397+ocean*83)%1980;}
 public static float decorY(int ocean,int n){return 720+(n*293+ocean*131)%2110;}
}
