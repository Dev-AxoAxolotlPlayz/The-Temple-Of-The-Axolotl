package com.devaxo.templeoftheaxolotl.promise;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
public final class PromiseInputState {
 private record Input(boolean up,int forward,int strafe,int tick) {}
 private static final WeakHashMap<ServerPlayer,Input> INPUT=new WeakHashMap<>();
 public static void receive(ServerPlayer p,boolean up,int f,int s){if(PromiseState.installed(p)) INPUT.put(p,new Input(up,Integer.signum(f),Integer.signum(s),p.tickCount));}
 private static Input get(ServerPlayer p){var i=INPUT.get(p);return i!=null&&p.tickCount-i.tick()<=10?i:new Input(false,0,0,0);}
 public static boolean up(ServerPlayer p){return get(p).up();}
 public static Vec3 direction(ServerPlayer p){var i=get(p);return new Vec3(i.strafe(),0,i.forward()).normalize().yRot((float)-Math.toRadians(p.getYRot()));}
}
