package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Attackable and physically solid head extension for the boss. Not a separate health pool. */
public class WrathHeadHitbox extends Entity {
    private static final EntityDataAccessor<Integer> PARENT_ID =
            SynchedEntityData.defineId(WrathHeadHitbox.class, EntityDataSerializers.INT);

    public WrathHeadHitbox(EntityType<? extends WrathHeadHitbox> type, Level level) {
        super(type, level);
        this.noPhysics = true; // avoid terrain obstruction while following the animated boss
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PARENT_ID, -1);
    }
    public void setParentId(int id) { entityData.set(PARENT_ID, id); }
    public int getParentId() { return entityData.get(PARENT_ID); }
    public BossWrathEntity getParentBoss() {
        Entity parent = level().getEntity(getParentId());
        return parent instanceof BossWrathEntity boss ? boss : null;
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;
        BossWrathEntity boss = getParentBoss();
        if (boss == null || !boss.isAlive() || boss.isRemoved()) { discard(); return; }
        // Standard entity push checks won't always run for a noPhysics proxy.
        // Apply player/mob displacement explicitly without pushing the boss itself.
        for (Entity other : level().getEntities(this, getBoundingBox(),
                e -> e != boss && e.isPushable() && !e.isSpectator())) {
            AABB a = getBoundingBox();
            AABB b = other.getBoundingBox();
            if (!a.intersects(b)) continue;
            double dx = other.getX() - getX();
            double dz = other.getZ() - getZ();
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length < 0.0001) { dx = 1; dz = 0; length = 1; }
            double overlapX = Math.min(a.maxX - b.minX, b.maxX - a.minX);
            double overlapZ = Math.min(a.maxZ - b.minZ, b.maxZ - a.minZ);
            double push = Math.min(0.14, Math.max(0, Math.min(overlapX, overlapZ)) * 0.30);
            other.push(dx / length * push, 0, dz / length * push);
        }
    }
    @Override public boolean isPickable() { return true; }
    @Override public boolean isPushable() { return false; } // handled explicitly in tick
    @Override public boolean hurt(DamageSource source, float amount) {
        BossWrathEntity boss = getParentBoss();
        return boss != null && boss.hurt(source, amount);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
}
