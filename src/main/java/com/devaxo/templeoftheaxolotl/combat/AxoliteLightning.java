package com.devaxo.templeoftheaxolotl.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import java.util.*;

/** Breadth-first branching lightning with a shared no-repeat target set. */
public final class AxoliteLightning {
    private AxoliteLightning() {}
    private record Node(Vec3 pos,int depth) {}
    public static void chain(ServerLevel level, Player owner, Entity direct,
                             Mob first, int initialDepth, Set<UUID> visited, Set<UUID> cargo) {
        chain(level,owner,direct,first,initialDepth,visited,cargo,false,ItemStack.EMPTY);
    }
    public static void chain(ServerLevel level, Player owner, Entity direct, Mob first,
                             int initialDepth, Set<UUID> visited, Set<UUID> cargo, boolean starfall) {
        chain(level, owner, direct, first, initialDepth, visited, cargo, starfall, owner.getMainHandItem());
    }
    public static void chain(ServerLevel level, Player owner, Entity direct, Mob first,
                             int initialDepth, Set<UUID> visited, Set<UUID> cargo,
                             boolean starfall, ItemStack cosmicWeapon) {
        var queue=new ArrayDeque<Node>();
        queue.add(new Node(first.getBoundingBox().getCenter(),initialDepth));
        while(!queue.isEmpty()) {
            Node node=queue.removeFirst();
            int depth=node.depth();
            float chance=depth==0?1f:depth==1?.8f:depth==2?.6f:.5f;
            // Melee initial 50%, subsequent 40%, 30%, 25%.
            if(direct==owner) chance*=.5f;
            if(level.random.nextFloat()>=chance) continue;
            int branches=level.random.nextBoolean()?2:1;
            for(int i=0;i<branches;i++) {
                Vec3 origin=node.pos();
                Mob target=level.getEntitiesOfClass(Mob.class,new AABB(origin,origin).inflate(20),m ->
                                m.isAlive() && m.isPickable() && !m.getUUID().equals(owner.getUUID()) && !visited.contains(m.getUUID())
                    && !m.isAlliedTo(owner) && !owner.isAlliedTo(m)
                    && !(m instanceof TamableAnimal t && t.isTame())
                    && m.getBoundingBox().getCenter().distanceToSqr(origin)<=400)
                    .stream().min(Comparator.comparingDouble(m -> m.getBoundingBox().getCenter().distanceToSqr(origin)))
                    .orElse(null);
                if(target==null) break;
                visited.add(target.getUUID());
                Vec3 next=target.getBoundingBox().getCenter();
                AABB box=target.getBoundingBox().inflate(2);
                Set<UUID> before=new HashSet<>();
                for(ItemEntity item:level.getEntitiesOfClass(ItemEntity.class,box)) before.add(item.getUUID());
                for(ExperienceOrb orb:level.getEntitiesOfClass(ExperienceOrb.class,box)) before.add(orb.getUUID());
                arc(level,origin,next);
                var magic=level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MAGIC);
                if(!target.hurt(new DamageSource(magic,direct,owner),8f)) continue;
                ShellShockedLogic.apply(target,owner);
                if (starfall) StarfallBurst.doubleBurst(level, next, owner, target, cosmicWeapon);
                if(!target.isAlive() && cargo!=null) {
                    for(ItemEntity item:level.getEntitiesOfClass(ItemEntity.class,box)) if(!before.contains(item.getUUID())) cargo.add(item.getUUID());
                    for(ExperienceOrb orb:level.getEntitiesOfClass(ExperienceOrb.class,box)) if(!before.contains(orb.getUUID())) cargo.add(orb.getUUID());
                }
                queue.addLast(new Node(next,depth+1));
            }
        }
    }
    private static void arc(ServerLevel level,Vec3 from,Vec3 to) {
        Vec3 direction=to.subtract(from);
        double length=direction.length();
        if(length<.001) return;
        Vec3 side=direction.cross(new Vec3(0,1,0));
        if(side.lengthSqr()<.001) side=direction.cross(new Vec3(1,0,0));
        side=side.normalize();
        Vec3 up=direction.normalize().cross(side).normalize();
        Vec3 last=from;
        int steps=Math.max(8,(int)Math.ceil(length*12));
        for(int i=1;i<=steps;i++) {
            double t=(double)i/steps;
            double env=Math.sin(Math.PI*t);
            Vec3 p=from.lerp(to,t).add(side.scale((level.random.nextDouble()-.5)*.38*env))
                    .add(up.scale((level.random.nextDouble()-.5)*.38*env));
            for(int j=0;j<=3;j++) {
                Vec3 q=last.lerp(p,j/3.0);
                level.sendParticles(new DustParticleOptions(new Vector3f(.77f,.99f,1f),1.45f),q.x,q.y,q.z,1,0,0,0,0);
                if((i+j)%2==0) level.sendParticles(new DustParticleOptions(new Vector3f(.15f,.88f,1f),1.05f),q.x,q.y,q.z,1,0,0,0,0);
                if((i+j)%3==0) level.sendParticles(new DustParticleOptions(new Vector3f(1f,.24f,.8f),1.25f),q.x,q.y,q.z,1,.07,.07,.07,0);
            }
            last=p;
        }
        level.playSound(null,BlockPos.containing(to),SoundEvents.TRIDENT_THUNDER.value(),SoundSource.PLAYERS,.22f,1.65f+level.random.nextFloat()*.25f);
    }
}
