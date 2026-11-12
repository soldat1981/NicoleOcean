package com.nicole.ocean;
import java.util.*;
final class LegacyFixtures {
 static byte[] v9(Game g)throws Exception{
  byte[] current=g.snapshot();byte[] old=Arrays.copyOf(current,current.length-Game.Codec.extraBytes(g));old[7]=9;
  java.util.zip.CRC32 crc=new java.util.zip.CRC32();crc.update(old,0,old.length-8);java.nio.ByteBuffer.wrap(old,old.length-8,8).putLong(crc.getValue());return old;
 }
}
