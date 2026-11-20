package com.devaxo.templeoftheaxolotl.promise;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.commands.Commands;

@EventBusSubscriber(modid="temple_of_the_axolotl")
public final class PromiseLocomotion {
    private static final Map<ServerPlayer,State> STATES = new WeakHashMap<>();
    private static class State {
        final List<PromiseAnchorPlanner.Candidate> anchors = new ArrayList<>();
        Object level;
        int cursor;
        int nextArm;
        int lastTransfer=-1000;
        final Map<PromiseAnchorPlanner.Candidate,Integer> slots=new HashMap<>();
        int lastSync=-1000;
        double altitude=Double.NaN;
        boolean suspended, engaged, lastUp, activationHeld, swimming;
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("tota").then(Commands.literal("promise")
            .then(Commands.literal("mobility").executes(c -> {
                ServerPlayer p=c.getSource().getPlayerOrException();
                var s=PromiseState.settings(p); s.putBoolean("mobility", !s.getBoolean("mobility")); PromiseState.set(p,s); return 1;
            }))));
    }
    private static boolean active(ServerPlayer p) { var settings=PromiseState.settings(p); return settings.getBoolean("mobility") || settings.getBoolean("mining") || settings.getInt("combat")!=0; }
    private static boolean valid(ServerPlayer p, PromiseAnchorPlanner.Candidate a,int reach) {
        var l=p.serverLevel(); var b=a.block(); var outside=b.relative(a.face());
        return l.hasChunk(b.getX()>>4,b.getZ()>>4) && l.hasChunk(outside.getX()>>4,outside.getZ()>>4)
            && PromiseAnchorPlanner.inCube(p.position().add(0,1,0),a.contact(),reach)
            && l.getBlockState(b).isFaceSturdy(l,b,a.face()) && l.getBlockState(outside).getCollisionShape(l,outside).isEmpty();
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (!PromiseState.installed(p) || !PromiseState.settings(p).getBoolean("mobility") || p.isSpectator() || p.getAbilities().flying || p.isFallFlying() || p.isPassenger()) {
            if(STATES.remove(p)!=null) {
                var packet=new com.devaxo.templeoftheaxolotl.network.PromiseSyncPayload(p.getUUID(),PromiseSnapshot.EMPTY);
                for(ServerPlayer viewer:p.serverLevel().players()) if(viewer.distanceToSqr(p)<16384)
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(viewer,packet);
            }
            if(p.tickCount%2==0 && PromiseState.installed(p)) {
                var packet=new com.devaxo.templeoftheaxolotl.network.PromiseSyncPayload(p.getUUID(),new PromiseSnapshot(true,PromiseState.settings(p).getBoolean("mobility"),PromiseState.settings(p).getBoolean("mining"),PromiseState.settings(p).getInt("combat")!=0,PromiseUpgrade.level(p),PromiseActions.contacts(p,8)));
                for(ServerPlayer viewer:p.serverLevel().players()) if(viewer.distanceToSqr(p)<16384) net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(viewer,packet);
            } else if(p.tickCount%20==0 && !PromiseState.installed(p)) {
                var packet=new com.devaxo.templeoftheaxolotl.network.PromiseSyncPayload(p.getUUID(),PromiseSnapshot.EMPTY);
                for(ServerPlayer viewer:p.serverLevel().players()) if(viewer.distanceToSqr(p)<16384) net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(viewer,packet);
            }
            return;
        }
        State s=STATES.computeIfAbsent(p,k->new State());
        if(s.level!=p.level()){s.level=p.level();s.anchors.clear();s.slots.clear();s.cursor=0;s.nextArm=0;s.lastTransfer=-1000;s.altitude=Double.NaN;s.suspended=false;s.engaged=false;s.lastUp=false;s.activationHeld=false;}
        int reach=5+PromiseUpgrade.level(p);
        Vec3 origin=p.position().add(0,1,0), v=p.getDeltaMovement();
        var settings=PromiseState.settings(p);
        boolean up=PromiseInputState.up(p), down=p.isShiftKeyDown(), hover=settings.getBoolean("suspend");
        Vec3 input=PromiseInputState.direction(p);
        boolean swimming=p.isInWater() && p.isSwimming();
        if(swimming){
            s.anchors.clear();s.slots.clear();s.altitude=Double.NaN;s.engaged=false;s.lastUp=up;s.swimming=true;
            int phase=Math.floorMod(p.tickCount,24);
            Vec3 dir=p.getLookAngle().normalize();
            if(phase>=6 && phase<10 && input.lengthSqr()>0.001){
                Vec3 next=v.add(dir.scale(.12));if(next.length()>.65)next=next.normalize().scale(.65);
                p.setDeltaMovement(next);p.hurtMarked=true;
                if(phase==6){Vec3 center=p.getBoundingBox().getCenter().subtract(dir.scale(.8));Vec3 side=dir.cross(new Vec3(0,1,0));if(side.lengthSqr()<.01)side=new Vec3(1,0,0);side=side.normalize();Vec3 other=dir.cross(side).normalize();
                    for(int i=0;i<24;i++){double a=i*Math.PI/12;Vec3 pt=center.add(side.scale(Math.cos(a)*.65)).add(other.scale(Math.sin(a)*.65));p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE,pt.x,pt.y,pt.z,1,.08,.08,.08,.05);}
                }
            }
            var packet=new com.devaxo.templeoftheaxolotl.network.PromiseSyncPayload(p.getUUID(),new PromiseSnapshot(true,true,settings.getBoolean("mining"),settings.getInt("combat")!=0,reach-5,PromiseActions.contacts(p,8).stream().filter(c->c.arm()%4<2).toList()));
            for(ServerPlayer viewer:p.serverLevel().players())if(viewer.distanceToSqr(p)<16384)net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(viewer,packet);
            return;
        }
        if(s.swimming){s.swimming=false;s.engaged=p.isInWater();}
        s.anchors.removeIf(a->!valid(p,a,reach));
        boolean activate=up&&!s.lastUp&&down&&!s.engaged;
        s.lastUp=up;
        if(activate){s.engaged=true;s.activationHeld=true;s.altitude=p.getY();}
        if(!up)s.activationHeld=false;
        if(s.activationHeld)down=false;
        if(s.engaged&&!activate&&!s.activationHeld&&p.onGround()&&down){s.engaged=false;}
        if(!s.engaged&&!hover&&v.y<-.45&&!p.onGround()){s.engaged=true;}
        if(!s.engaged){s.altitude=Double.NaN;s.anchors.clear();}
        if(activate&&hover&&p.onGround())s.altitude=p.getY()+2;
        s.suspended=hover;
        boolean assist=s.engaged;
        s.slots.keySet().retainAll(s.anchors);
        int capacity=4;
        if(assist){
            Vec3 travel=new Vec3(input.x,0,input.z);
            if(travel.lengthSqr()<.001)travel=new Vec3(v.x,0,v.z);
            Vec3 prediction=origin.add(travel.lengthSqr()<.001?Vec3.ZERO:travel.normalize().scale(5)).add(0,up?1:down?-1:0,0);
            // Keep one physical arm available for the next transfer.
            int desired=capacity-1;
            if(s.anchors.size()>desired && s.anchors.size()>1)s.anchors.remove(s.anchors.size()-1);
            if((p.tickCount%2==0 && p.tickCount-s.lastTransfer>=8) || s.anchors.size()<desired){
                List<BlockPos> blocks=new ArrayList<>();BlockPos center=BlockPos.containing(origin);
                int side=2*reach+1,volume=side*side*side;
                for(int i=0;i<1024;i++){int n=Math.floorMod(s.cursor++,volume);blocks.add(center.offset(n%side-reach,(n/side)%side-reach,n/(side*side)-reach));}
                for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=-reach;y<=reach;y++)blocks.add(center.offset(x,y,z));
                BlockPos ahead=BlockPos.containing(prediction);
                for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=-reach;y<=reach;y++)blocks.add(ahead.offset(x,y,z));
                var candidates=new ArrayList<>(PromiseAnchorPlanner.candidates(origin,new LinkedHashSet<>(blocks),reach,20000));
                int[] order={2,6,3,7};
                int free=-1;
                for(int i=0;i<4;i++){int index=(s.nextArm+i)%4;if(!s.slots.containsValue(order[index])){free=index;break;}}
                final int chosen=free<0?0:free;
                final int arm=order[chosen];
                double angle=Math.toRadians(p.yBodyRot);
                Vec3 left=new Vec3(Math.cos(angle),0,Math.sin(angle));
                candidates.sort(Comparator.comparingDouble(a->prediction.distanceToSqr(a.contact())+Math.max(0,-a.contact().subtract(origin).dot(left)*(arm<4?1:-1))*3));
                for(var a:candidates){
                    if(!valid(p,a,reach)||s.anchors.stream().anyMatch(old->old.block().equals(a.block())||old.contact().distanceToSqr(a.contact())<12.25))continue;
                    if(s.anchors.size()<desired){s.anchors.add(a);s.slots.put(a,arm);s.nextArm=(chosen+1)%4;break;}
                    var worst=s.anchors.stream().max(Comparator.comparingDouble(old->prediction.distanceToSqr(old.contact()))).orElse(null);
                    if(worst!=null&&prediction.distanceToSqr(a.contact())+9<prediction.distanceToSqr(worst.contact())){
                        s.anchors.add(a);s.slots.put(a,arm);s.nextArm=(chosen+1)%4;
                        s.anchors.remove(worst);s.slots.remove(worst);s.lastTransfer=p.tickCount;break;
                    }
                }
            }
            if(!s.anchors.isEmpty()){
                if(!Double.isFinite(s.altitude))s.altitude=p.getY();
                if(up&&!down)s.altitude+=.22;else if(down&&!up)s.altitude-=.22;
                // A valid floor attachment supports altitude just like a wall or ceiling.
                double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
                for(var a:s.anchors){min=Math.min(min,a.contact().y-reach-1);max=Math.max(max,a.contact().y+reach-1);}
                s.altitude=Math.max(min+.12,Math.min(max-.12,s.altitude));
                double y=Math.max(-.28,Math.min(.28,(s.altitude-p.getY())*.32));
                Vec3 horizontal=new Vec3(v.x,0,v.z).add(input.scale(.045));
                double speed=p.isSprinting()?.40:.25;
                if(horizontal.length()>speed)horizontal=horizontal.normalize().scale(speed);
                if(input.lengthSqr()<.001)horizontal=horizontal.scale(.85);
                Vec3 next=new Vec3(horizontal.x,y,horizontal.z);
                Vec3 predictedNext=origin.add(next);
                if(s.anchors.stream().noneMatch(a->PromiseAnchorPlanner.inCube(predictedNext,a.contact(),reach))){
                    next=new Vec3(0,0,0);s.altitude=p.getY();
                }
                p.fallDistance=0;p.setDeltaMovement(next);p.hurtMarked=true;
            }else s.altitude=Double.NaN;
        }else{s.anchors.clear();s.altitude=Double.NaN;}
        if(p.tickCount-s.lastSync>=1){
            s.slots.keySet().retainAll(s.anchors);
            int[] order={2,6,3,7};
            List<PromiseSnapshot.Contact> contacts=new ArrayList<>();
            for(var a:s.anchors){
                if(!s.slots.containsKey(a))for(int arm:order)if(!s.slots.containsValue(arm)){s.slots.put(a,arm);break;}
                contacts.add(new PromiseSnapshot.Contact(s.slots.get(a),a.contact(),a.face().get3DDataValue()));
            }
            for(var c:PromiseActions.contacts(p,8))if(contacts.stream().noneMatch(a->a.arm()==c.arm()))contacts.add(c);
            var packet=new com.devaxo.templeoftheaxolotl.network.PromiseSyncPayload(p.getUUID(),new PromiseSnapshot(true,true,settings.getBoolean("mining"),settings.getInt("combat")!=0,reach-5,contacts));
            for(ServerPlayer viewer:p.serverLevel().players())if(viewer.distanceToSqr(p)<16384)net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(viewer,packet);
            s.lastSync=p.tickCount;
        }
    }
}
