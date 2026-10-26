package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.*;
import java.util.EnumSet;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.chat.Component;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/** A non-combat companion that retrieves nearby item stacks and sheds feathers while seated. */
public class ClucklesEntity extends TamableAnimal implements GeoEntity {
    // TamableAnimal ordered-to-sit is not guaranteed to be available client-side.
    // GeckoLib evaluates animations on the client, so explicitly sync the command.
    private static final EntityDataAccessor<Boolean> ANIM_SITTING =
            SynchedEntityData.defineId(ClucklesEntity.class, EntityDataSerializers.BOOLEAN);

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANIM_SITTING, false);
    }

    @Override public void setOrderedToSit(boolean sitting) {
        super.setOrderedToSit(sitting);
        entityData.set(ANIM_SITTING, sitting);
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private ItemStack carried = ItemStack.EMPTY;
    private int featherTimer = 6000;
    private int pickupCooldown = 0;
    private boolean lastAnimatedSit = false;
    private boolean animationInitialized = false;
    private int standTransitionUntil = 0;
    public ClucklesEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        setCustomName(Component.literal("Cluckles"));
        setCustomNameVisible(true);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(2, new RetrieveItemsGoal());
        goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.15, 3, 12));
        goalSelector.addGoal(4, new RandomStrollGoal(this, 0.7));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 7));
    }
    @Override public boolean isFood(ItemStack stack) { return false; }
    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isTame() && isOwnedBy(player) && player.getItemInHand(hand).isEmpty()) {
            if (!level().isClientSide) setOrderedToSit(!isOrderedToSit());
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide || !isTame() || !(getOwner() instanceof Player owner)) return;
        if (pickupCooldown > 0) pickupCooldown--;
        if (isOrderedToSit()) {
            if (--featherTimer <= 0) {
                spawnAtLocation(new ItemStack(Items.FEATHER, 1 + random.nextInt(3)));
                featherTimer = 6000 + random.nextInt(6001);
            }
            return;
        }
    }

    /**
     * Item retrieval owns the MOVE goal flag so FollowOwnerGoal cannot
     * overwrite navigation every tick. Returning a carried stack has priority.
     */
    private final class RetrieveItemsGoal extends Goal {
        private ItemEntity target;

        private RetrieveItemsGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override public boolean canUse() {
            if (!isTame() || isOrderedToSit() || !(getOwner() instanceof Player)
                    || !isAlive()) return false;
            if (!carried.isEmpty()) return true;
            if (pickupCooldown > 0) return false;
            var items = level().getEntitiesOfClass(ItemEntity.class,
                    getBoundingBox().inflate(12),
                    e -> e.isAlive() && !e.getItem().isEmpty()
                            && e.getOwner() == null && !e.hasPickUpDelay());
            target = null;
            double best = Double.MAX_VALUE;
            for (ItemEntity item : items) {
                double distance = distanceToSqr(item);
                if (distance < best && getNavigation().createPath(item, 0) != null) {
                    best = distance;
                    target = item;
                }
            }
            return target != null;
        }

        @Override public boolean canContinueToUse() {
            if (!isTame() || isOrderedToSit() || !(getOwner() instanceof Player)) return false;
            return !carried.isEmpty() || (target != null && target.isAlive()
                    && !target.getItem().isEmpty() && distanceToSqr(target) <= 256);
        }

        @Override public void tick() {
            if (!(getOwner() instanceof Player owner)) return;
            if (!carried.isEmpty()) {
                if (owner instanceof net.minecraft.server.level.ServerPlayer serverOwner
                        && com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.enabled(serverOwner)
                        && com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.inbound(serverOwner)) {
                    int stored = com.devaxo.templeoftheaxolotl.repository.CelestialRepository.insert(
                            serverOwner, carried);
                    if (stored > 0) carried.shrink(stored);
                    if (carried.isEmpty()) { pickupCooldown = 40; target = null; return; }
                }
                if (distanceToSqr(owner) < 5.0) {
                    ItemStack delivery = carried.copy();
                    // Drop two blocks toward the owner and 1.5 blocks above Cluckles,
                    // instead of spawning inside the ground beneath his feet.
                    double dx = owner.getX() - getX();
                    double dz = owner.getZ() - getZ();
                    double length = Math.sqrt(dx * dx + dz * dz);
                    if (length < 0.001) {
                        double yaw = Math.toRadians(getYRot());
                        dx = -Math.sin(yaw);
                        dz = Math.cos(yaw);
                    } else {
                        dx /= length;
                        dz /= length;
                    }
                    ItemEntity packageDrop = new ItemEntity(level(),
                            getX() + dx * 2.0, getY() + 1.5, getZ() + dz * 2.0, delivery);
                    packageDrop.setDeltaMovement(0.0, 0.0, 0.0);
                    packageDrop.setPickUpDelay(40);
                    if (level().addFreshEntity(packageDrop)) {
                        carried = ItemStack.EMPTY;
                    }
                    pickupCooldown = 60;
                    target = null;
                } else {
                    getNavigation().moveTo(owner, 1.25);
                }
                return;
            }
            if (target == null || !target.isAlive() || target.getItem().isEmpty()) return;
            if (distanceToSqr(target) <= 2.25) {
                carried = target.getItem().copy();
                target.discard();
                target = null;
                getNavigation().moveTo(owner, 1.25);
            } else {
                getNavigation().moveTo(target, 1.25);
            }
        }

        @Override public void stop() {
            target = null;
            getNavigation().stop();
        }
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ClucklesFeatherTimer", featherTimer);
        tag.putInt("ClucklesPickupCooldown", pickupCooldown);
        if (!carried.isEmpty()) tag.put("ClucklesCarried", carried.save(registryAccess()));
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ANIM_SITTING, isOrderedToSit());
        if (tag.contains("ClucklesFeatherTimer")) featherTimer = tag.getInt("ClucklesFeatherTimer");
        pickupCooldown = tag.getInt("ClucklesPickupCooldown");
        if (tag.contains("ClucklesCarried")) carried = ItemStack.parseOptional(registryAccess(), tag.getCompound("ClucklesCarried"));
        if (!hasCustomName()) setCustomName(Component.literal("Cluckles"));
        setCustomNameVisible(true);
    }
    @Override public TamableAnimal getBreedOffspring(ServerLevel level, AgeableMob mate) { return null; }
    /** One controller owns the pose, preventing idle/walk from overriding Sit or Stand. */
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 0, state -> {
            boolean sitting = entityData.get(ANIM_SITTING);
            if (!animationInitialized) {
                animationInitialized = true;
                lastAnimatedSit = sitting;
                if (sitting) {
                    state.setAnimation(RawAnimation.begin().thenPlayAndHold("Sit"));
                    return PlayState.CONTINUE;
                }
            }
            if (lastAnimatedSit != sitting) {
                lastAnimatedSit = sitting;
                state.getController().forceAnimationReset();
                if (sitting) {
                    standTransitionUntil = 0;
                    state.setAnimation(RawAnimation.begin().thenPlayAndHold("Sit"));
                } else {
                    // GeckoLib predicates run per render frame, not once per game tick.
                    // Use entity age so the 0.25-second Stand clip can actually finish.
                    standTransitionUntil = tickCount + 6;
                    state.setAnimation(RawAnimation.begin().thenPlay("Stand"));
                }
                return PlayState.CONTINUE;
            }
            if (sitting) return PlayState.CONTINUE; // Hold the completed Sit pose.
            if (tickCount < standTransitionUntil) return PlayState.CONTINUE;
            state.setAnimation(RawAnimation.begin().thenLoop(
                    state.isMoving() ? "animation.cluckles.walk" : "animation.cluckles.idle"));
            return PlayState.CONTINUE;
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
