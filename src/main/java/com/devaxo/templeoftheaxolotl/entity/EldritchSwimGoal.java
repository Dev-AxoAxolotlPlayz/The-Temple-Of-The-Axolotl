package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.world.EldritchEncounter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import java.util.EnumSet;
import java.util.Comparator;

/** Direct swimming in the open arena, without village or thermocline navigation. */
public final class EldritchSwimGoal extends Goal {
    private final PathfinderMob mob;
    private Player target;
    private Vec3 waypoint;
    private int refresh, cooldown, wander;
    public EldritchSwimGoal(PathfinderMob mob) { this.mob=mob; setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
    public static boolean active(PathfinderMob mob) {
        return mob.level() instanceof ServerLevel s && EldritchEncounter.arena(s) && mob.getPersistentData().hasUUID("EldritchOwner");
    }
    public static Player nearest(ServerLevel s, PathfinderMob mob) {
        return EldritchEncounter.participants(s).stream().min(Comparator.comparingDouble(p->mob.distanceToSqr(p))).orElse(null);
    }
    public static Vec3 waypoint(PathfinderMob mob) {
        for(int i=0;i<24;i++) {
            Vec3 v=mob.position().add(mob.getRandom().nextInt(33)-16,mob.getRandom().nextInt(17)-8,mob.getRandom().nextInt(33)-16);
            BlockPos p=BlockPos.containing(v);
            if(Math.abs(v.x)>244 || Math.abs(v.z)>244 || !mob.level().getChunkSource().hasChunk(p.getX()>>4,p.getZ()>>4)) continue;
            if(mob.level().getFluidState(p).is(FluidTags.WATER) && mob.level().noCollision(mob,mob.getBoundingBox().move(v.subtract(mob.position())))) return v;
        }
        return null;
    }
    @Override public boolean canUse(){return active(mob)&&mob.isInWater();}
    @Override public boolean canContinueToUse(){return canUse();}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void start(){mob.getNavigation().stop();refresh=0;}
    @Override public void stop(){mob.getNavigation().stop();target=null;waypoint=null;mob.setTarget(null);}
    @Override public void tick(){
        if(!(mob.level() instanceof ServerLevel s))return;
        if (--refresh<=0 || (target!=null && !target.isAlive())) {target=nearest(s,mob);refresh=10;}
        mob.setTarget(target);
        if(cooldown>0)cooldown--;
        if(target==null){
            if(--wander<=0 || (waypoint!=null && mob.position().distanceToSqr(waypoint)<4)){waypoint=waypoint(mob);wander=60;}
            if(waypoint!=null)mob.getMoveControl().setWantedPosition(waypoint.x,waypoint.y,waypoint.z,0.85);
            return;
        }
        waypoint=null;
        double dist=mob.distanceToSqr(target);
        boolean ranged=mob instanceof MerolotlEntity && (mob.getMainHandItem().is(net.minecraft.world.item.Items.TRIDENT) || mob.getMainHandItem().is(com.devaxo.templeoftheaxolotl.item.ModItems.AXOLITE_TRIDENT.get()));
        double reach=mob instanceof AxokrakenEntity? 9:4;
        // Giant models contact players long before their entity origins coincide.
        if(mob instanceof MegolotlEntity) reach=Math.pow(mob.getBbWidth()*0.5+1.5,2);
        Vec3 aim=target.getBoundingBox().getCenter().subtract(mob.getBoundingBox().getCenter());
        if(aim.lengthSqr()>0.01) {
            mob.setYRot(net.minecraft.util.Mth.rotLerp(.22F,mob.getYRot(),(float)Math.toDegrees(Math.atan2(aim.z,aim.x))-90));
            mob.yBodyRot=mob.getYRot();mob.setYHeadRot(mob.getYRot());
            mob.setXRot(net.minecraft.util.Mth.rotLerp(.22F,mob.getXRot(),(float)-Math.toDegrees(Math.atan2(aim.y,Math.hypot(aim.x,aim.z)))));
        }
        if(dist>(ranged?100:reach))mob.getMoveControl().setWantedPosition(target.getX(),target.getY()+target.getBbHeight()*0.4,target.getZ(),1.15);
        else {mob.getMoveControl().setWantedPosition(mob.getX(),mob.getY(),mob.getZ(),0);mob.setDeltaMovement(mob.getDeltaMovement().scale(0.65));}
        if(cooldown==0 && mob.hasLineOfSight(target)){
            if(ranged && dist<=196){((MerolotlEntity)mob).performRangedAttack(target,1);cooldown=40;}
            else if(!(mob instanceof AxokrakenEntity) && dist<=reach){if(mob instanceof MegolotlEntity) target.hurt(mob.damageSources().mobAttack(mob),(float)mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)); else mob.doHurtTarget(target);cooldown=20;}
        }
    }
}
