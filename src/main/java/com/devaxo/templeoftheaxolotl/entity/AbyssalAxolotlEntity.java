package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.*;

/** Phase 1 companion. Commands and item rewards are authoritative on the server. */
public class AbyssalAxolotlEntity extends TamableAnimal implements GeoEntity {
    private static final EntityDataAccessor<Boolean> SITTING = SynchedEntityData.defineId(AbyssalAxolotlEntity.class, EntityDataSerializers.BOOLEAN);
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private String permanentName = "";
    private int jobTicks = 0;
    private int jobQuantity = 0;
    private Item jobItem = Items.AIR;
    
    private java.util.UUID greetingTarget;
    private int greetingTicks = 0;
    private boolean greetingLeft;
    private boolean greetingNuzzled;
    private boolean scoutingDiamonds;
    private net.minecraft.core.BlockPos diamondLocation;
    private boolean guarding = false;
    private boolean activelyFollowingOwner = false;
    private boolean delivering = false;
    private final java.util.Map<Item, Integer> materialDelivery = new java.util.LinkedHashMap<>();

    public AbyssalAxolotlEntity(EntityType<? extends TamableAnimal> type, Level level) { super(type, level); this.moveControl = new NoodleSwimControl(this); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder); builder.define(SITTING, false);
    }
    @Override public void setOrderedToSit(boolean sit) { super.setOrderedToSit(sit); entityData.set(SITTING, sit); }
    // Dynamic movement cap: 103% of the owner's current velocity (including vehicles).
    private static final double OWNER_SPEED_MULTIPLIER = 1.03D;
    private double ownerSpeedCap() {
        if (!(getOwner() instanceof LivingEntity owner)) return 0.0D;
        net.minecraft.world.entity.Entity moving = owner.getVehicle() != null
                ? owner.getRootVehicle() : owner;
        return Math.max(0.12D, moving.getDeltaMovement().length() * OWNER_SPEED_MULTIPLIER);
    }
    private void limitMovementToOwnerSpeed() {
        if (!activelyFollowingOwner) return;
        double cap = ownerSpeedCap();
        net.minecraft.world.phys.Vec3 velocity = getDeltaMovement();
        if (velocity.lengthSqr() > cap * cap && velocity.lengthSqr() > 0)
            setDeltaMovement(velocity.normalize().scale(cap));
    }
    private static final double OWNER_DEFENSE_RADIUS = 24.0D;
    private static final double BENDS_CURE_RADIUS = 8.0D;

    @Override protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        return new net.minecraft.world.entity.ai.navigation.GroundPathNavigation(this, level);
    }


    @Override public int getMaxAirSupply() { return 300; }
    @Override protected int increaseAirSupply(int air) { return getMaxAirSupply(); }

    @Override public void travel(net.minecraft.world.phys.Vec3 input) {
        if (isInWater()) {
            // Use the custom 3D swim controller without land-style upward buoyancy.
            moveRelative(0.02F, input);
            limitMovementToOwnerSpeed();
            net.minecraft.world.phys.Vec3 motion = getDeltaMovement();
            // Never launch above the water surface while pursuing a navigation target.
            net.minecraft.core.BlockPos above = blockPosition().above();
            if (motion.y > 0 && level().getFluidState(above).isEmpty()) {
                motion = new net.minecraft.world.phys.Vec3(motion.x, 0, motion.z);
                setDeltaMovement(motion);
            }
            move(net.minecraft.world.entity.MoverType.SELF, motion);
            setDeltaMovement(getDeltaMovement().scale(0.86D));
        } else {
            limitMovementToOwnerSpeed();
            super.travel(input);
            limitMovementToOwnerSpeed();
        }
    }

    @Override protected void registerGoals() {
        // Aquatic companions must not use FloatGoal: it constantly pushes them to the surface.
        // Their 3D water navigation handles depth instead.
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.Goal() {
            { setFlags(java.util.EnumSet.of(Flag.MOVE)); }
            @Override public boolean canUse() { return busy(); }
            @Override public boolean canContinueToUse() { return busy(); }
            @Override public void tick() {
                if (getOwner() instanceof Player owner) {
                    if (delivering) getNavigation().moveTo(owner, 1.2);
                    else if (tickCount % 40 == 0) {
                        double theta = random.nextDouble() * Math.PI * 2;
                        getNavigation().moveTo(owner.getX() + Math.cos(theta)*8, owner.getY(), owner.getZ() + Math.sin(theta)*8, 1.05);
                    }
                }
            }
        });
        goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.Goal() {
            { setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK)); }
            @Override public boolean canUse() { return greetingTarget != null; }
            @Override public boolean canContinueToUse() { return greetingTarget != null; }
            @Override public void tick() { tickGreeting(); }
        });
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.25, true));
        // Vanilla FollowOwnerGoal rejects water-bound navigation in 1.21.1.
        // Use a water-compatible following goal; never teleport or replace the companion.
        goalSelector.addGoal(4, new Goal() {
            { setFlags(EnumSet.of(Flag.MOVE)); }
            private LivingEntity owner;
            @Override public boolean canUse() {
                owner = getOwner();
                return isTame() && !isOrderedToSit() && !busy() && greetingTarget == null
                        && owner != null && owner.isAlive()
                        && distanceToSqr(owner) > 12.0D * 12.0D;
            }
            @Override public boolean canContinueToUse() {
                return isTame() && !isOrderedToSit() && !busy() && greetingTarget == null
                        && owner != null && owner.isAlive()
                        && distanceToSqr(owner) > 3.0D * 3.0D;
            }
            @Override public void start() {
                activelyFollowingOwner = true;
                getNavigation().stop();
            }
            @Override public void tick() {
                if (owner == null) return;
                if (isInWater()) {
                    // Direct 3D pursuit is more reliable than a surface-biased water path.
                    getNavigation().stop();
                    getMoveControl().setWantedPosition(owner.getX(), owner.getY() + owner.getBbHeight() * 0.35D,
                            owner.getZ(), 1.1D);
                } else {
                    if (tickCount % 5 == 0) getNavigation().moveTo(owner, 1.1D);
                    // A short, collision-checked ledge step allows safe two-block descents.
                    if (getNavigation().isDone() && onGround()
                            && owner.getY() < getY() - 0.5D && owner.getY() >= getY() - 2.5D
                            && distanceToSqr(owner) < 12.0D * 12.0D) {
                        net.minecraft.world.phys.Vec3 toward = new net.minecraft.world.phys.Vec3(
                                owner.getX() - getX(), 0, owner.getZ() - getZ());
                        if (toward.lengthSqr() > 0.01D) {
                            toward = toward.normalize().scale(0.13D);
                            if (level().noCollision(AbyssalAxolotlEntity.this,
                                    getBoundingBox().move(toward.x, 0, toward.z)))
                                setDeltaMovement(getDeltaMovement().add(toward.x, 0, toward.z));
                        }
                    }
                }
            }
            @Override public void stop() {
                activelyFollowingOwner = false;
                owner = null;
                getNavigation().stop();
            }
        });
        // Defend the owner from Axoleviathans even when ordinary guarding is disabled.
        targetSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                this, AxoleviathanEntity.class, 10, true, false,
                candidate -> isTame() && getOwner() != null && candidate.isAlive()
                        && candidate.distanceToSqr(getOwner()) <= OWNER_DEFENSE_RADIUS * OWNER_DEFENSE_RADIUS));
        targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                this, net.minecraft.world.entity.LivingEntity.class, 10, true, false,
                candidate -> guarding && getOwner() != null && candidate != getOwner()
                        && candidate.distanceToSqr(getOwner()) <= 144
                        && (candidate instanceof net.minecraft.world.entity.monster.Monster
                            || (candidate instanceof net.minecraft.world.entity.NeutralMob neutral
                                && neutral.isAngryAt(getOwner())))));
        // RandomStrollGoal uses land routes; aquatic wandering needs a 3D destination.
        goalSelector.addGoal(5, new Goal() {
            { setFlags(EnumSet.of(Flag.MOVE)); }
            private int remaining;
            @Override public boolean canUse() {
                return isInWater() && !isOrderedToSit() && !busy() && greetingTarget == null
                        && getTarget() == null && random.nextInt(55) == 0;
            }
            @Override public boolean canContinueToUse() {
                return isInWater() && remaining-- > 0 && !isOrderedToSit() && !busy()
                        && greetingTarget == null && getTarget() == null && !activelyFollowingOwner;
            }
            @Override public void start() {
                remaining = 30 + random.nextInt(50);
                for (int attempt = 0; attempt < 8; attempt++) {
                    net.minecraft.core.BlockPos candidate = blockPosition().offset(
                            random.nextInt(17) - 8, random.nextInt(11) - 5, random.nextInt(17) - 8);
                    if (level().getFluidState(candidate).is(net.minecraft.tags.FluidTags.WATER)
                            && level().getBlockState(candidate).getCollisionShape(level(), candidate).isEmpty()) {
                        getMoveControl().setWantedPosition(candidate.getX() + .5D,
                                candidate.getY() + .5D, candidate.getZ() + .5D, .65D);
                        return;
                    }
                }
                remaining = 0;
            }
            @Override public void stop() { getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0); }
        });
        goalSelector.addGoal(6, new RandomStrollGoal(this, 0.75) {
            @Override public boolean canUse() { return !isInWater() && super.canUse(); }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /** Smooth 3D aquatic pursuit, limited to 103% of the owner's current vehicle-adjusted speed. */
    private static final class NoodleSwimControl extends net.minecraft.world.entity.ai.control.MoveControl {
        private final AbyssalAxolotlEntity noodle;
        NoodleSwimControl(AbyssalAxolotlEntity noodle) { super(noodle); this.noodle = noodle; }
        @Override public void tick() {
            if (!noodle.isInWater()) {
                // Restore vanilla land steering; otherwise ground pathfinding can never move.
                super.tick();
                return;
            }
            if (operation != Operation.MOVE_TO) {
                noodle.setSpeed(0);
                return;
            }
            net.minecraft.world.phys.Vec3 delta = new net.minecraft.world.phys.Vec3(
                    wantedX - noodle.getX(), wantedY - noodle.getY(), wantedZ - noodle.getZ());
            if (delta.lengthSqr() < 0.04D) { noodle.setSpeed(0); return; }
            net.minecraft.world.phys.Vec3 direction = delta.normalize();
            double desiredSpeed = Math.max(0.1D, speedModifier * 0.75D);
            if (noodle.activelyFollowingOwner) {
                desiredSpeed = Math.min(noodle.ownerSpeedCap(), desiredSpeed);
            }
            // Don't chase a target above the waterline with a vertical leap.
            if (direction.y > 0 && noodle.level().getFluidState(noodle.blockPosition().above()).isEmpty()) {
                direction = new net.minecraft.world.phys.Vec3(direction.x, 0, direction.z).normalize();
            }
            net.minecraft.world.phys.Vec3 desired = direction.scale(desiredSpeed);
            noodle.setDeltaMovement(noodle.getDeltaMovement().lerp(desired, 0.20D));
            noodle.limitMovementToOwnerSpeed();
            noodle.setYRot((float)(net.minecraft.util.Mth.atan2(direction.z, direction.x) * (180D / Math.PI)) - 90F);
            noodle.yBodyRot = noodle.getYRot();
            // Vanilla X rotation follows the actual 3D swim target (positive pitch = down).
            double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            float targetPitch = (float) (-net.minecraft.util.Mth.atan2(delta.y, horizontal) * (180D / Math.PI));
            noodle.setXRot(net.minecraft.util.Mth.rotLerp(0.22F, noodle.getXRot(), targetPitch));
            noodle.setSpeed(0);
        }
    }
    @Override public boolean isFood(ItemStack stack) { return false; }
    @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mate) { return null; }
    public String permanentName() { return permanentName; }
    public boolean hasPermanentName() { return !permanentName.isBlank(); }
    public boolean assignName(ServerPlayer player, String requested) {
        if (!isTame() || !isOwnedBy(player) || hasPermanentName()) return false;
        String clean = requested.strip();
        if (clean.length() < 1 || clean.length() > 32 || !clean.matches("[\\p{L}\\p{N} _-]+")) return false;
        permanentName = clean; setCustomName(Component.literal(clean)); setCustomNameVisible(true); acknowledge(); return true;
    }
    @Override public void setCustomName(Component name) {
        if (hasPermanentName() && (name == null || !permanentName.equals(name.getString()))) return;
        super.setCustomName(name);
    }
    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.NAME_TAG)) return InteractionResult.FAIL;
        if (!isTame() && stack.is(ModItems.ABYSSAL_TORCH.get())) {
            if (!level().isClientSide && player instanceof ServerPlayer owner) {
                tame(owner); setOrderedToSit(false);
                if (!owner.getAbilities().instabuild) stack.shrink(1);
                owner.sendSystemMessage(Component.literal("Your Abyssal Axolotl trusts you! Name it in chat: Your name is <name>"));
                level().broadcastEntityEvent(this, (byte)7);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (isTame() && isOwnedBy(player) && stack.is(Items.WATER_BUCKET)) {
            if (!level().isClientSide && player instanceof ServerPlayer owner) {
                ItemStack bucket = com.devaxo.templeoftheaxolotl.entity.NoodleItems.makeFilledBucket(this);
                if (!owner.getAbilities().instabuild) stack.shrink(1);
                if (!owner.getInventory().add(bucket)) owner.drop(bucket, false);
                discard();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (isTame() && isOwnedBy(player) && stack.isEmpty()) {
            if (!level().isClientSide) setOrderedToSit(!isOrderedToSit());
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }
    public void acknowledge() { if (!level().isClientSide) { triggerAnim("ack_head", "ack"); triggerAnim("ack_face", "ack"); } }
    public boolean isGuarding() { return guarding; }
    public void setGuarding(boolean value) { guarding = value; if (!value) setTarget(null); }
    public boolean busy() { return jobTicks > 0 || delivering || greetingTarget != null; }
    public boolean greet(net.minecraft.world.entity.LivingEntity target) {
        if (busy() || !target.isAlive() || target == this || target.getCustomName() == null) return false;
        greetingTarget = target.getUUID(); greetingTicks = 0; greetingNuzzled = false;
        setGuarding(false); setOrderedToSit(false); return true;
    }
    private void tickGreeting() {
        if (!(level() instanceof ServerLevel server) || greetingTarget == null) return;
        net.minecraft.world.entity.Entity entity = server.getEntity(greetingTarget);
        if (!(entity instanceof net.minecraft.world.entity.LivingEntity target) || !target.isAlive() || distanceToSqr(target) > 1024 || ++greetingTicks > 240) {
            finishGreeting(); return;
        }
        if (!greetingNuzzled) {
            // Stand to the side of the target, using its horizontal facing vector.
            net.minecraft.world.phys.Vec3 forward = target.getLookAngle();
            net.minecraft.world.phys.Vec3 side = new net.minecraft.world.phys.Vec3(-forward.z, 0, forward.x).normalize();
            net.minecraft.world.phys.Vec3 desired = target.position().add(side.scale(1.2));
            if (distanceToSqr(desired) > 2.25) {
                getNavigation().moveTo(desired.x, desired.y, desired.z, 1.05); return;
            }
            getNavigation().stop();
            net.minecraft.world.phys.Vec3 toTarget = target.position().subtract(position());
            net.minecraft.world.phys.Vec3 facing = getLookAngle();
            greetingLeft = facing.x * toTarget.z - facing.z * toTarget.x > 0;
            triggerAnim("greet_head", greetingLeft ? "left" : "right");
            triggerAnim("greet_face", "smile");
            greetingNuzzled = true; greetingTicks = 0;
        } else if (greetingTicks == 10) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                (getX()+target.getX())/2, Math.min(getY()+getBbHeight(), target.getY()+target.getBbHeight()),
                (getZ()+target.getZ())/2, 7, .3, .25, .3, .02);
        } else if (greetingTicks >= 55) finishGreeting();
    }
    private void finishGreeting() { greetingTarget = null; greetingTicks = 0; greetingNuzzled = false; getNavigation().stop(); }
    public boolean scoutDiamond(ServerPlayer owner) {
        if (busy()) return false;
        net.minecraft.core.BlockPos origin = blockPosition();
        net.minecraft.core.BlockPos best = null; double bestDistance = Double.MAX_VALUE;
        // Loaded chunks only: no chunk generation or blocking world scans.
        for (int x = origin.getX()-16; x <= origin.getX()+16; x += 1)
            for (int z = origin.getZ()-16; z <= origin.getZ()+16; z += 1) {
                if (!owner.serverLevel().hasChunk(x >> 4, z >> 4)) continue;
                for (int y = owner.serverLevel().getMinBuildHeight(); y < Math.min(32, owner.serverLevel().getMaxBuildHeight()); y++) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x,y,z);
                    var block = owner.serverLevel().getBlockState(pos);
                    if (block.is(net.minecraft.world.level.block.Blocks.DIAMOND_ORE) || block.is(net.minecraft.world.level.block.Blocks.DEEPSLATE_DIAMOND_ORE)) {
                        double distance = pos.distSqr(origin);
                        if (distance < bestDistance) { bestDistance = distance; best = pos; }
                    }
                }
            }
        if (best == null) return false;
        diamondLocation = best; scoutingDiamonds = true;
        jobItem = Items.AIR; jobQuantity = 1; jobTicks = 100; delivering = false;
        setOrderedToSit(false); acknowledge(); return true;
    }
    public void fetch(Item item, int quantity) {
        jobItem = item; jobQuantity = Math.max(1, Math.min(4096, quantity));
        jobTicks = 20 * (5 + Math.round(10f * Math.min(4095, jobQuantity - 1) / 4095f));
        materialDelivery.clear(); delivering = false; setOrderedToSit(false); acknowledge();
    }
    public void fetchMaterials(java.util.Map<Item,Integer> materials, int craftCount) {
        materialDelivery.clear(); materialDelivery.putAll(materials);
        jobItem = Items.AIR; jobQuantity = Math.max(1, craftCount);
        jobTicks = 20 * (5 + Math.round(10f * Math.min(4095, Math.max(0, materials.values().stream().mapToInt(Integer::intValue).sum()-1)) / 4095f));
        delivering = false; setOrderedToSit(false); acknowledge();
    }
    public void cancelJob() { scoutingDiamonds = false; diamondLocation = null; finishGreeting(); jobTicks = 0; jobQuantity = 0; jobItem = Items.AIR; delivering = false; materialDelivery.clear(); }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;
        limitMovementToOwnerSpeed();
        // Restore air and cure only the actual owner, using the existing persistent Bends state.
        if (isInWater()) setAirSupply(getMaxAirSupply());
        if (tickCount % 20 == 0 && isTame() && getOwner() instanceof ServerPlayer owner
                && distanceToSqr(owner) <= BENDS_CURE_RADIUS * BENDS_CURE_RADIUS
                && com.devaxo.templeoftheaxolotl.event.BendsEvents.hasBends(owner)) {
            com.devaxo.templeoftheaxolotl.event.BendsEvents.cure(owner);
            if (level() instanceof ServerLevel server) server.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.HEART, owner.getX(), owner.getY() + 1,
                    owner.getZ(), 6, .35, .35, .35, .03);
        }
        if (getTarget() instanceof AxoleviathanEntity leviathan && getOwner() != null) {
            if (!leviathan.isAlive() || leviathan.distanceToSqr(getOwner()) > OWNER_DEFENSE_RADIUS * OWNER_DEFENSE_RADIUS) {
                setTarget(null);
                getNavigation().stop();
            } else if (isOrderedToSit()) setOrderedToSit(false);
        }
        if (guarding && getOwner() != null && (getTarget() == null || !getTarget().isAlive())) {
            setTarget(null);
        }
        if (guarding && getTarget() != null && !(getTarget() instanceof AxoleviathanEntity) && getOwner() != null && getTarget().distanceToSqr(getOwner()) > 196) {
            setTarget(null); getNavigation().stop();
        }

        if (jobTicks > 0) {
            if (--jobTicks == 0) delivering = true;
        }
        if (delivering && getOwner() instanceof Player owner) {
            getNavigation().moveTo(owner, 1.2);
            if (distanceToSqr(owner) < 9) {
                if (scoutingDiamonds && diamondLocation != null) {
                    ItemStack fish = new ItemStack(Items.TROPICAL_FISH);
                    fish.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Diamond Prospecting Fish"));
                    fish.set(net.minecraft.core.component.DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(java.util.List.of(
                        Component.literal("Diamond ore located: X " + diamondLocation.getX() + ", Z " + diamondLocation.getZ()))));
                    net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(level(), getX(), getY()+.5, getZ(), fish);
                    level().addFreshEntity(drop);
                    if (owner instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal(permanentName + " found a diamond ore location! Check the tropical fish."));
                } else if (materialDelivery.isEmpty()) {
                    dropSupply(jobItem, jobQuantity);
                    if (owner instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal(permanentName + " brought back " + jobQuantity + " " + jobItem.getDescription().getString() + "!"));
                } else {
                    materialDelivery.forEach(this::dropSupply);
                    if (owner instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal(permanentName + " delivered the requested recipe ingredients!"));
                }
                cancelJob();
            }
        }
    }
    private void dropSupply(Item item, int amount) {
        int remaining = amount;
        while (remaining > 0 && item != Items.AIR) {
            int n = Math.min(remaining, item.getDefaultMaxStackSize());
            net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
                    level(), getX(), getY() + getBbHeight() * 0.6, getZ(), new ItemStack(item, n));
            if (getOwner() != null) {
                net.minecraft.world.phys.Vec3 direction = getOwner().position().subtract(position()).normalize();
                drop.setDeltaMovement(direction.x * 0.3, 0.22, direction.z * 0.3);
            }
            level().addFreshEntity(drop); remaining -= n;
        }
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); tag.putString("PermanentName", permanentName);
        tag.putBoolean("NoodleGuarding", guarding);
        tag.putBoolean("NoodleSitting", entityData.get(SITTING));
        tag.putInt("JobTicks", jobTicks); tag.putInt("JobQuantity", jobQuantity);
        tag.putString("JobItem", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(jobItem).toString());
        tag.putBoolean("Delivering", delivering);
        tag.putBoolean("ScoutingDiamonds", scoutingDiamonds);
        if (diamondLocation != null) tag.putLong("DiamondLocation", diamondLocation.asLong());
        net.minecraft.nbt.ListTag entries = new net.minecraft.nbt.ListTag();
        materialDelivery.forEach((item, count) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("Id", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString());
            entry.putInt("Count", count); entries.add(entry);
        });
        tag.put("Materials", entries);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); permanentName = tag.getString("PermanentName");
        if (!permanentName.isBlank()) { super.setCustomName(Component.literal(permanentName)); setCustomNameVisible(true); }
        setOrderedToSit(tag.getBoolean("NoodleSitting")); guarding = tag.getBoolean("NoodleGuarding");
        jobTicks = tag.getInt("JobTicks"); jobQuantity = tag.getInt("JobQuantity"); delivering = tag.getBoolean("Delivering");
        net.minecraft.resources.ResourceLocation savedItem = net.minecraft.resources.ResourceLocation.tryParse(tag.getString("JobItem"));
        jobItem = savedItem != null && net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(savedItem)
                ? net.minecraft.core.registries.BuiltInRegistries.ITEM.get(savedItem) : Items.AIR;
        scoutingDiamonds = tag.getBoolean("ScoutingDiamonds");
        diamondLocation = tag.contains("DiamondLocation") ? net.minecraft.core.BlockPos.of(tag.getLong("DiamondLocation")) : null;
        materialDelivery.clear();
        net.minecraft.nbt.ListTag entries = tag.getList("Materials", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(entry.getString("Id"));
            if (id != null && net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id)) {
                materialDelivery.put(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id), Math.max(0, Math.min(4096, entry.getInt("Count"))));
            }
        }
        if (jobItem == Items.AIR && materialDelivery.isEmpty() && !scoutingDiamonds && !delivering && jobTicks > 0) cancelJob();
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            if (entityData.get(SITTING)) return state.setAndContinue(RawAnimation.begin().thenLoop("sitting"));
            if (state.isMoving()) return state.setAndContinue(RawAnimation.begin().thenLoop(isInWater() ? "swim_animation" : "land_walking"));
            return state.setAndContinue(RawAnimation.begin().thenLoop("idle_gills"));
        }));
        // Two controllers play in parallel: facial features reset while the head returns to neutral.
        // The smile controller does not serialize the head's return transition.
        controllers.add(new AnimationController<>(this, "ack_head", 0, state -> PlayState.STOP)
            .triggerableAnim("ack", RawAnimation.begin()
                .thenPlay("neutral_to_look_up")
                .thenWait(26)
                .thenPlay("look_up_to_neutral")));
        controllers.add(new AnimationController<>(this, "greet_head", 0, state -> PlayState.STOP)
            .triggerableAnim("left", RawAnimation.begin().thenPlay("neutral_to_look_left").thenWait(20).thenPlay("look_left_to_neutral"))
            .triggerableAnim("right", RawAnimation.begin().thenPlay("neutral_to_look_right").thenWait(20).thenPlay("look_right_to_neutral")));
        controllers.add(new AnimationController<>(this, "greet_face", 0, state -> PlayState.STOP)
            .triggerableAnim("smile", RawAnimation.begin().thenPlay("normal_to_smile").thenWait(20).thenPlay("smile_to_normal")));
        controllers.add(new AnimationController<>(this, "ack_face", 0, state -> PlayState.STOP)
            .triggerableAnim("ack", RawAnimation.begin()
                .thenWait(5)
                .thenPlay("normal_to_smile")
                .thenWait(20)
                .thenPlay("smile_to_normal")));

    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
