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
    // Abyss inhabitants persist across player-distance despawn checks.
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    private static final EntityDataAccessor<Integer> BEAM_TARGET = SynchedEntityData.defineId(AbyssalAxolotlEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BEAM_UNTIL = SynchedEntityData.defineId(AbyssalAxolotlEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SITTING = SynchedEntityData.defineId(AbyssalAxolotlEntity.class, EntityDataSerializers.BOOLEAN);
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private String permanentName = "";
    private final java.util.List<String> learnedInnates = new java.util.ArrayList<>();
    private int innateSlotsUsed = 0;
    private int innateBlinkCooldown = 0;
    private int innateGhostCooldown = 0;
    private boolean innateStatsDirty = true;
    private static final net.minecraft.resources.ResourceLocation INNATE_MAX_HP = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "noodle_innate_hp");
    private static final net.minecraft.resources.ResourceLocation INNATE_ARMOR = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "noodle_innate_armor");
    private static final net.minecraft.resources.ResourceLocation INNATE_TOUGHNESS = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "noodle_innate_toughness");
    private boolean hasInnate(String id){return learnedInnates.contains(id);}
    private void updateInnateStats(){
        if (level().isClientSide) return;
        var hp=getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        var armor=getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        var toughness=getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
        if(hp!=null){
            hp.removeModifier(INNATE_MAX_HP);
            // Compute percentage bonus from base HP, then apply the flat penalty.
            double delta=(hasInnate("tank")?hp.getBaseValue():0.0) - (hasInnate("berserker")?20.0:0.0);
            if(delta!=0) hp.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(INNATE_MAX_HP,delta,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        }
        if(armor!=null){armor.removeModifier(INNATE_ARMOR);if(hasInnate("tank"))armor.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(INNATE_ARMOR,30,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));}
        if(toughness!=null){toughness.removeModifier(INNATE_TOUGHNESS);if(hasInnate("tank"))toughness.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(INNATE_TOUGHNESS,20,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));}
        innateStatsDirty=false;
        if(getHealth()>getMaxHealth())setHealth(getMaxHealth());
    }
    @Override public boolean fireImmune(){return hasInnate("devilish") || hasInnate("impervious") || super.fireImmune();}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){
        return !hasInnate("impervious") && super.hurt(source,amount);
    }
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
    private int boopCooldown;
    private int ownerAttackStamp = -1, ownerHurtStamp = -1;
    private net.minecraft.core.BlockPos digSite;
    private boolean previousSit, previousGuard;
    private int digStallTicks;
    private boolean diggingJob;
    private boolean activelyFollowingOwner = false;
    private boolean delivering = false;
    private final java.util.Map<Item, Integer> materialDelivery = new java.util.LinkedHashMap<>();

    public AbyssalAxolotlEntity(EntityType<? extends TamableAnimal> type, Level level) { super(type, level); this.moveControl = new NoodleSwimControl(this); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder); builder.define(SITTING, false); builder.define(BEAM_TARGET, -1); builder.define(BEAM_UNTIL, 0);
    }
    @Override public void setOrderedToSit(boolean sit) { super.setOrderedToSit(sit); entityData.set(SITTING, sit); }
    // Target speeds in blocks/tick: normal/follow 9.6 b/s, guard 13 b/s.
    private static final double SWIM_SPEED = 0.48D;
    private static final double GUARD_SPEED = 0.65D;
    private static final double FETCH_LAND_SPEED = 0.32D;
    private static final double GUARD_RADIUS_SQR = 100.0D;
    private void limitMovementToOwnerSpeed() { /* Fixed mode speeds replace the old owner-velocity cap. */ }
    private static final double OWNER_DEFENSE_RADIUS = 18.0D;
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
            @Override public boolean canUse() { return jobTicks > 0 || delivering; }
            @Override public boolean canContinueToUse() { return jobTicks > 0 || delivering; }
            @Override public void tick() {
                // Gathering and delivery movement are controlled by the job tick.

            }
        });
        goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.Goal() {
            { setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK)); }
            @Override public boolean canUse() { return greetingTarget != null; }
            @Override public boolean canContinueToUse() { return greetingTarget != null; }
            @Override public void tick() { tickGreeting(); }
        });
        goalSelector.addGoal(3, new Goal() {
            { setFlags(EnumSet.of(Flag.MOVE)); }
            @Override public boolean canUse() { return guarding && !busy() && !isOrderedToSit() && validEnemy(getTarget()) && getOwner() != null && distanceToSqr(getOwner()) <= GUARD_RADIUS_SQR; }
            @Override public boolean canContinueToUse() { return canUse(); }
            @Override public void tick() {
                LivingEntity target = getTarget();
                if (target == null) return;
                if (getOwner() == null || distanceToSqr(getOwner()) > GUARD_RADIUS_SQR) {
                    setTarget(null);
                    stopMovement();
                } else if (distanceToSqr(target) <= 64 && hasLineOfSight(target)) stopMovement();
                else if (target.distanceToSqr(getOwner()) <= GUARD_RADIUS_SQR)
                    moveToward(target.getX(), target.getY(), target.getZ());
                else stopMovement();
            }
            @Override public void stop() { stopMovement(); }
        });
        // Vanilla FollowOwnerGoal rejects water-bound navigation in 1.21.1.
        // Use a water-compatible following goal; never teleport or replace the companion.
        goalSelector.addGoal(4, new Goal() {
            { setFlags(EnumSet.of(Flag.MOVE)); }
            private LivingEntity owner;
            @Override public boolean canUse() {
                owner = getOwner();
                return isTame() && !isOrderedToSit() && !busy() && greetingTarget == null
                        && owner != null && owner.isAlive()
                        && distanceToSqr(owner) > 8.0D * 8.0D;
            }
            @Override public boolean canContinueToUse() {
                return isTame() && !isOrderedToSit() && !busy() && greetingTarget == null
                        && owner != null && owner.isAlive()
                        && distanceToSqr(owner) > 5.0D * 5.0D;
            }
            @Override public void start() {
                activelyFollowingOwner = true;
                getNavigation().stop();
            }
            @Override public void tick() {
                if (owner == null) return;
                // Trail behind the owner's velocity, preserving Y for aquatic and future Angelic flight.
                net.minecraft.world.phys.Vec3 heading = owner.getDeltaMovement();
                heading = new net.minecraft.world.phys.Vec3(heading.x, 0, heading.z);
                if (heading.lengthSqr() < 0.01D) {
                    float yaw = owner.getYRot() * ((float)Math.PI / 180F);
                    heading = new net.minecraft.world.phys.Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
                }
                net.minecraft.world.phys.Vec3 trail = owner.position().subtract(heading.normalize().scale(5.0D));
                double gap = distanceTo(owner);
                double boost = gap > 16 ? 2.2D : gap > 10 ? 1.65D : gap > 8 ? 1.3D : 1.0D;
                if (isInWater()) {
                    getNavigation().stop();
                    getMoveControl().setWantedPosition(trail.x, owner.getY() + owner.getBbHeight() * 0.35D,
                            trail.z, boost);
                } else {
                    if (tickCount % 5 == 0) getNavigation().moveTo(trail.x, trail.y, trail.z, 1.1D * boost);
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
        // RandomStrollGoal uses land routes; aquatic wandering needs a 3D destination.
        goalSelector.addGoal(5, new Goal() {
            { setFlags(EnumSet.of(Flag.MOVE)); }
            private int remaining;
            @Override public boolean canUse() {
                return !isTame() && isInWater() && !isOrderedToSit() && !busy() && greetingTarget == null
                        && getTarget() == null && random.nextInt(55) == 0;
            }
            @Override public boolean canContinueToUse() {
                return !isTame() && isInWater() && remaining-- > 0 && !isOrderedToSit() && !busy()
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
            @Override public boolean canUse() { return !isTame() && !isInWater() && !busy() && !isOrderedToSit() && super.canUse(); }
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
            double gap = noodle.getOwner() == null ? 0 : noodle.distanceTo(noodle.getOwner());
            double desiredSpeed = noodle.guarding ? GUARD_SPEED : SWIM_SPEED * (noodle.activelyFollowingOwner && gap > 8 ? (gap > 16 ? 2.2D : gap > 10 ? 1.65D : 1.3D) : 1.0D);
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
        if (stack.is(ModItems.NOODLE_TRAINING_MEDAL.get()) && !com.devaxo.templeoftheaxolotl.entity.NoodleInnates.traits(stack).isEmpty()) {
            if (!isTame() || !isOwnedBy(player)) return InteractionResult.FAIL;
            if (!level().isClientSide && player instanceof ServerPlayer owner) {
                var incoming = com.devaxo.templeoftheaxolotl.entity.NoodleInnates.traits(stack);
                boolean valid = com.devaxo.templeoftheaxolotl.entity.NoodleInnates.canApply(learnedInnates, stack)
                    && innateSlotsUsed + com.devaxo.templeoftheaxolotl.entity.NoodleInnates.cost(stack) <= 5;
                if (valid) {
                    learnedInnates.addAll(incoming);
                    innateSlotsUsed += com.devaxo.templeoftheaxolotl.entity.NoodleInnates.cost(stack);
                    innateStatsDirty=true;
                    if (!owner.getAbilities().instabuild) stack.shrink(1);
                    owner.sendSystemMessage(Component.literal("Innate learned! Capacity: " + innateSlotsUsed + "/5"));
                } else owner.sendSystemMessage(Component.literal("This medal is incompatible or exceeds the five-slot capacity."));
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
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
    public net.minecraft.world.entity.Entity activeLaserTarget() {
        // Server clears the synchronized target; client and server tick counts need not match.
        int id = entityData.get(BEAM_TARGET);
        return id < 0 ? null : level().getEntity(id);
    }
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
        beginDigJob(); diamondLocation = best; scoutingDiamonds = true;
        jobItem = Items.AIR; jobQuantity = 1; jobTicks = 100; delivering = false;
        setOrderedToSit(false); acknowledge(); return true;
    }
    public void fetch(Item item, int quantity) {
        beginDigJob(); jobItem = item; jobQuantity = Math.max(1, Math.min(4096, quantity));
        jobTicks = 20 * (5 + Math.round(10f * Math.min(4095, jobQuantity - 1) / 4095f));
        materialDelivery.clear(); delivering = false; setOrderedToSit(false); acknowledge();
    }
    public void fetchMaterials(java.util.Map<Item,Integer> materials, int craftCount) {
        beginDigJob(); materialDelivery.clear(); materialDelivery.putAll(materials);
        jobItem = Items.AIR; jobQuantity = Math.max(1, craftCount);
        jobTicks = 20 * (5 + Math.round(10f * Math.min(4095, Math.max(0, materials.values().stream().mapToInt(Integer::intValue).sum()-1)) / 4095f));
        delivering = false; setOrderedToSit(false); acknowledge();
    }
    private void beginDigJob() { previousSit=isOrderedToSit(); previousGuard=guarding; guarding=false; setTarget(null); diggingJob=true; digSite=null; digStallTicks=0; getNavigation().stop(); }
    public void cancelJob() { if(diggingJob){diggingJob=false;setOrderedToSit(previousSit);guarding=previousGuard;} digSite=null; scoutingDiamonds = false; diamondLocation = null; finishGreeting(); jobTicks = 0; jobQuantity = 0; jobItem = Items.AIR; delivering = false; materialDelivery.clear(); }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (entityData.get(BEAM_TARGET) >= 0 && tickCount >= entityData.get(BEAM_UNTIL)) {
            entityData.set(BEAM_TARGET, -1);
        }
        limitMovementToOwnerSpeed();
        if(innateStatsDirty)updateInnateStats();
        tickInnateAbilities();
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
        tickBoop();
        if (jobTicks > 0 && getOwner() instanceof Player owner) {
            if (digSite == null || !suitableDigSite(digSite)) digSite = chooseDigSite(owner);
            // Recovery: never allow a failed route or inaccessible site to freeze a job indefinitely.
            if (++digStallTicks >= 100) {
                digSite = chooseDigSite(owner);
                getNavigation().stop();
                digStallTicks = 0;
            }
            if (digSite == null && digStallTicks == 99) {
                // Use current safe ground if no distant site can be reached.
                var here = blockPosition();
                if (suitableDigSite(here)) digSite = here;
            }
            if (digSite != null) {
                var center = net.minecraft.world.phys.Vec3.atBottomCenterOf(digSite);
                if (distanceToSqr(center) > 1.5) moveToward(center.x, center.y, center.z);
                else {
                    stopMovement();
                    if (boopCooldown != 25) getLookControl().setLookAt(getX(), getY()-1, getZ(), 90, 90);
                    if (tickCount % 5 == 0 && level() instanceof ServerLevel server)
                        server.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, level().getBlockState(digSite.below())), getX(), getY()+.1, getZ(), 5, .2,.1,.2,.02);
                    digStallTicks = 0;
                    if (--jobTicks == 0) delivering = true;
                }
            }
        }
        if (delivering && getOwner() instanceof Player owner) {
            moveToward(owner.getX(), owner.getY(), owner.getZ());
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
                    // One 15% roll per completed Fetch order; no additional gathering time.
                    int count=hasInnate("prospector") && random.nextFloat()<0.15f ? jobQuantity*2 : jobQuantity;
                    dropSupply(jobItem, count);
                    if (owner instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal(permanentName + " brought back " + jobQuantity + " " + jobItem.getDescription().getString() + "!"));
                } else {
                    boolean doubled=hasInnate("prospector") && random.nextFloat()<0.15f;
                    materialDelivery.forEach((item,count)->dropSupply(item,doubled ? count*2 : count));
                    if (owner instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal(permanentName + " delivered the requested recipe ingredients!"));
                }
                cancelJob();
            }
        }
    }
    private void stopMovement() {
        getNavigation().stop();
        getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0);
        if (isInWater()) setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    }
    private void moveToward(double x, double y, double z) {
        if (isInWater()) { getNavigation().stop(); getMoveControl().setWantedPosition(x,y,z,1.0); }
        else if (tickCount % 5 == 0 || getNavigation().isDone()) getNavigation().moveTo(x,y,z,1.15);
    }
    private boolean validEnemy(LivingEntity e) {
        return e != null && e != this && e.isAlive() && e != getOwner() && !isAlliedTo(e)
                && !(e instanceof TamableAnimal pet && pet.isTame())
                && !(e instanceof Player p && (p.isCreative() || p.isSpectator())) && canAttack(e);
    }
    private boolean autoEnemy(LivingEntity e, LivingEntity owner) {
        if (!validEnemy(e)) return false;
        if (e instanceof NeutralMob neutral) return neutral.isAngryAt(owner);
        return e instanceof net.minecraft.world.entity.monster.Enemy || e instanceof AxoleviathanEntity
                || e.getType().is(net.minecraft.tags.EntityTypeTags.AXOLOTL_ALWAYS_HOSTILES)
                || e.getType().is(net.minecraft.tags.EntityTypeTags.AXOLOTL_HUNT_TARGETS);
    }
    private void tickBoop() {
        if (boopCooldown > 0) --boopCooldown;
        LivingEntity owner = getOwner();
        if (!isTame() || owner == null || !owner.isAlive() || hasInnate("ghost")) { setTarget(null); return; }
        boolean pursue = guarding && !busy() && !isOrderedToSit();
        double range = pursue ? OWNER_DEFENSE_RADIUS : 8;
        LivingEntity target = getTarget();
        if (!validEnemy(target) || (!pursue && distanceToSqr(target)>64) || (pursue && target.distanceToSqr(owner)>GUARD_RADIUS_SQR)) target = null;
        LivingEntity assist = null;
        if (owner.getLastHurtMobTimestamp() != ownerAttackStamp) {
            ownerAttackStamp = owner.getLastHurtMobTimestamp();
            assist = owner.getLastHurtMob();
        }
        if (owner.getLastHurtByMobTimestamp() != ownerHurtStamp) {
            ownerHurtStamp = owner.getLastHurtByMobTimestamp();
            LivingEntity attacker = owner.getLastHurtByMob();
            if (validEnemy(attacker) && (!validEnemy(assist) || distanceToSqr(attacker)<distanceToSqr(assist))) assist=attacker;
        }
        if (validEnemy(assist) && distanceToSqr(assist)<=range*range && (target==null || distanceToSqr(assist)<distanceToSqr(target))) target=assist;
        if (target == null && tickCount % 5 == 0) {
            target = level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(range),e->autoEnemy(e,owner) && distanceToSqr(e)<=range*range && (pursue || hasLineOfSight(e)))
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        }
        setTarget(target);
        if (target == null || boopCooldown != 0 || distanceToSqr(target)>64 || !hasLineOfSight(target)) return;
        getLookControl().setLookAt(target,90,90);
        var end=target.getBoundingBox().getCenter();
        var start=getEyePosition();
        var direction=end.subtract(start).normalize();
        start=start.add(direction.scale(getBbWidth()*.4));
        // Client renders one continuous dark-blue beam; no dust-particle approximation.
        entityData.set(BEAM_TARGET, target.getId());
        entityData.set(BEAM_UNTIL, tickCount + 5);
        float multiplier=1f + (hasInnate("berserker")?1f:0f) + (hasInnate("tank")?.5f:0f) + (hasInnate("devilish")?.5f:0f);
        float damage=9f*multiplier;
        if(hasInnate("creeper_sense") && target instanceof net.minecraft.world.entity.monster.Creeper)damage=Math.max(damage,target.getHealth()+1);
        target.hurt(damageSources().mobAttack(this),damage);
        float attackSpeed=1f + (hasInnate("berserker")?1f:0f) - (hasInnate("tank")?.1f:0f) + (hasInnate("devilish")?.25f:0f);
        boopCooldown=Math.max(2,Math.round(25/attackSpeed));
        if(hasInnate("creeper_sense") && target instanceof net.minecraft.world.entity.monster.Creeper)boopCooldown=Math.min(boopCooldown,5);
    }
    private void tickInnateAbilities(){
        if(innateBlinkCooldown>0)--innateBlinkCooldown;
        if(innateGhostCooldown>0)--innateGhostCooldown;
        if(!(level() instanceof ServerLevel server))return;
        var owner=getOwner();
        if(hasInnate("angelic")){
            setNoGravity(true);
            if(tickCount%12==0)server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,getX(),getY()+0.4,getZ(),2,.35,.25,.35,.01);
            if(!isInWater() && !onGround() && getDeltaMovement().y<-.08)setDeltaMovement(getDeltaMovement().x,-.08,getDeltaMovement().z);
        }else if(isNoGravity())setNoGravity(false);
        if(owner!=null && hasInnate("blink") && innateBlinkCooldown==0){
            // Protect the owner in dangerous lava or below-world falls by exchanging positions.
            boolean rescue=owner.isInLava() || owner.getY()<server.getMinBuildHeight()+4;
            if(rescue){
                var p=position();
                if(!isInLava() && getY()>server.getMinBuildHeight()+5){
                    var danger=owner.position();
                    owner.teleportTo(p.x,p.y,p.z);
                    teleportTo(danger.x,Math.max(server.getMinBuildHeight()+2,danger.y),danger.z);
                    innateBlinkCooldown=20*30;
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,p.x,p.y,p.z,32,.5,.5,.5,.1);
                }
            }else if(distanceToSqr(owner)>24*24 && !busy() && !isOrderedToSit()){
                teleportTo(owner.getX()+1,owner.getY(),owner.getZ()+1);
                innateBlinkCooldown=20*10;
            }
        }
        if(hasInnate("ghost") && innateGhostCooldown==0 && owner!=null){
            innateGhostCooldown=20*12;
            var pos=blockPosition();
            // Only use natural darkness and vacant, supported positions; never replace a player block.
            if(server.getMaxLocalRawBrightness(pos)<=4 && server.getBlockState(pos).isAir()
                && server.getBlockState(pos.below()).isFaceSturdy(server,pos.below(),net.minecraft.core.Direction.UP)){
                server.setBlock(pos,net.minecraft.world.level.block.Blocks.SOUL_LANTERN.defaultBlockState(),3);
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,getX(),getY()+.5,getZ(),8,.3,.3,.3,.01);
            }
        }
    }
    private boolean suitableDigSite(net.minecraft.core.BlockPos pos) {
        return level().hasChunk(pos.getX()>>4,pos.getZ()>>4) && level().getBlockState(pos).getCollisionShape(level(),pos).isEmpty()
            && level().getBlockState(pos.below()).isFaceSturdy(level(),pos.below(),net.minecraft.core.Direction.UP)
            && level().getFluidState(pos).isEmpty() || level().hasChunk(pos.getX()>>4,pos.getZ()>>4) && level().getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER) && level().getBlockState(pos.below()).isFaceSturdy(level(),pos.below(),net.minecraft.core.Direction.UP);
    }
    private net.minecraft.core.BlockPos chooseDigSite(Player owner) {
        var origin=owner.blockPosition();
        for(int angle=0;angle<16;angle++) {
            double theta=angle*Math.PI/8;
            for(int y=0;y<=12;y++) for(int sign:new int[]{-1,1}) {
                var candidate=origin.offset((int)Math.round(10*Math.cos(theta)),y*sign,(int)Math.round(10*Math.sin(theta)));
                if(suitableDigSite(candidate) && (isInWater() || getNavigation().createPath(candidate,0)!=null)) return candidate;
            }
        }
        return null;
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
        com.devaxo.templeoftheaxolotl.entity.NoodleInnates.writeTraits(tag, learnedInnates);
        tag.putInt("NoodleInnateSlots", innateSlotsUsed);
        tag.putInt("NoodleBlinkCooldown",innateBlinkCooldown);
        tag.putInt("NoodleGhostCooldown",innateGhostCooldown);
        tag.putBoolean("NoodleGuarding", guarding);
        tag.putBoolean("NoodleSitting", entityData.get(SITTING));
        tag.putInt("JobTicks", jobTicks); tag.putInt("JobQuantity", jobQuantity);
        tag.putString("JobItem", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(jobItem).toString());
        tag.putBoolean("Delivering", delivering);
        tag.putBoolean("DiggingJob",diggingJob); tag.putBoolean("PreviousSit",previousSit); tag.putBoolean("PreviousGuard",previousGuard);
        if(digSite!=null) tag.putLong("DigSite",digSite.asLong());
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
        learnedInnates.clear(); learnedInnates.addAll(com.devaxo.templeoftheaxolotl.entity.NoodleInnates.readTraits(tag));
        innateBlinkCooldown=tag.getInt("NoodleBlinkCooldown");innateGhostCooldown=tag.getInt("NoodleGhostCooldown");innateStatsDirty=true;
        innateSlotsUsed = tag.contains("NoodleInnateSlots") ? Math.max(learnedInnates.size(), Math.min(5,tag.getInt("NoodleInnateSlots"))) : learnedInnates.size();
        if (!permanentName.isBlank()) { super.setCustomName(Component.literal(permanentName)); setCustomNameVisible(true); }
        setOrderedToSit(tag.getBoolean("NoodleSitting")); guarding = tag.getBoolean("NoodleGuarding");
        jobTicks = tag.getInt("JobTicks"); jobQuantity = tag.getInt("JobQuantity"); delivering = tag.getBoolean("Delivering");
        net.minecraft.resources.ResourceLocation savedItem = net.minecraft.resources.ResourceLocation.tryParse(tag.getString("JobItem"));
        jobItem = savedItem != null && net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(savedItem)
                ? net.minecraft.core.registries.BuiltInRegistries.ITEM.get(savedItem) : Items.AIR;
        diggingJob=tag.getBoolean("DiggingJob"); previousSit=tag.getBoolean("PreviousSit"); previousGuard=tag.getBoolean("PreviousGuard"); digSite=tag.contains("DigSite")?net.minecraft.core.BlockPos.of(tag.getLong("DigSite")):null;
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
