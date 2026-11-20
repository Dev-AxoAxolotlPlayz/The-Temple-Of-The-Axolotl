package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.item.Items;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.world.item.ArmorItem;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import java.util.UUID;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Abyss village resident. Independent aquatic resident with no Drowned behavior. */
public class MerolotlEntity extends PathfinderMob implements GeoEntity, RangedAttackMob {
    // Abyss inhabitants persist across player-distance despawn checks.
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);
    private UUID villageAlarmTarget;
    private int villageAlarmTicks;
    private net.minecraft.core.BlockPos villageHome;
    private int villagePatrolRadius = 38;
    private boolean cosmeticHelmetChosen;
    private boolean cosmeticHelmet;
    public MerolotlEntity(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); this.moveControl = new AquaticMoveControl(this); }

    @Override protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        return new net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation(this, level);
    }
    @Override protected int increaseAirSupply(int air) { return getMaxAirSupply(); }
    @Override public void travel(Vec3 input) {
        if (isInWater()) {
            moveRelative(0.02F, input);
            move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.87D));
        } else super.travel(input);
    }
    private static final class AquaticMoveControl extends net.minecraft.world.entity.ai.control.MoveControl {
        private final PathfinderMob swimmer;
        AquaticMoveControl(PathfinderMob swimmer) { super(swimmer); this.swimmer = swimmer; }
        @Override public void tick() {
            if (!swimmer.isInWater()) { super.tick(); return; }
            if (operation != Operation.MOVE_TO) { swimmer.setSpeed(0); return; }
            Vec3 delta = new Vec3(wantedX - swimmer.getX(), wantedY - swimmer.getY(), wantedZ - swimmer.getZ());
            if (delta.lengthSqr() < 0.09D) { swimmer.setSpeed(0); return; }
            Vec3 direction = delta.normalize();
            if (direction.y > 0 && swimmer.level().getFluidState(swimmer.blockPosition().above()).isEmpty()) {
                direction = new Vec3(direction.x, 0, direction.z).normalize();
            }
            Vec3 desired = direction.scale(Math.max(0.08D, speedModifier * 0.23D));
            swimmer.setDeltaMovement(swimmer.getDeltaMovement().lerp(desired, 0.18D));
            swimmer.setYRot((float)(net.minecraft.util.Mth.atan2(direction.z, direction.x) * (180D / Math.PI)) - 90F);
            swimmer.yBodyRot = swimmer.getYRot();
            double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            float pitch = (float)(-net.minecraft.util.Mth.atan2(delta.y, horizontal) * (180D / Math.PI));
            swimmer.setXRot(net.minecraft.util.Mth.rotLerp(0.22F, swimmer.getXRot(), pitch));
            swimmer.setSpeed(0);
        }
    }

    @Override protected int decreaseAirSupply(int currentAir) { return currentAir; }
    // NeoForge retains this deprecated spawn hook for mob initialization.
    @SuppressWarnings("deprecation")
    @Override @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(world, difficulty, reason, data);
        if (reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION || reason == MobSpawnType.SPAWNER) {
            float roll = random.nextFloat();
            if (roll < .01F) setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
            else if (roll < .03F) setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_AXE));
            else if (roll < .06F) setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
            else if (roll < .07F) setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.AXOLITE_TRIDENT.get()));
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                if (random.nextFloat() < .05F) {
                    boolean axolite = random.nextFloat() < .20F;
                    var armor = switch (slot) {
                        case HEAD -> axolite ? ModItems.AXOLITE_HELMET.get() : Items.DIAMOND_HELMET;
                        case CHEST -> axolite ? ModItems.AXOLITE_CHESTPLATE.get() : Items.DIAMOND_CHESTPLATE;
                        case LEGS -> axolite ? ModItems.AXOLITE_LEGGINGS.get() : Items.DIAMOND_LEGGINGS;
                        case FEET -> axolite ? ModItems.AXOLITE_BOOTS.get() : Items.DIAMOND_BOOTS;
                        default -> Items.AIR;
                    };
                    setItemSlot(slot, new ItemStack(armor));
                }
            }
        }
        return result;
    }
    @Override public void performRangedAttack(LivingEntity target, float distanceFactor) {
        ItemStack weapon = getMainHandItem();
        if (!(weapon.is(Items.TRIDENT) || weapon.is(ModItems.AXOLITE_TRIDENT.get()))) return;
        if (!(level() instanceof net.minecraft.server.level.ServerLevel server)) return;
        ThrownTrident projectile = new ThrownTrident(level(), this, weapon.copy());
        double dx = target.getX() - getX();
        double dy = target.getY(0.5) - projectile.getY();
        double dz = target.getZ() - getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        projectile.shoot(dx, dy + horizontal * .12, dz, 1.6F, 3.0F);
        server.addFreshEntity(projectile);
        playSound(net.minecraft.sounds.SoundEvents.DROWNED_SHOOT, 1.0F, 1.0F);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new EldritchSwimGoal(this));
        goalSelector.addGoal(1, new RangedAttackGoal(this, 1.0, 40, 14.0F) {
            @Override public boolean canUse() {
                ItemStack hand = getMainHandItem();
                return (hand.is(Items.TRIDENT) || hand.is(ModItems.AXOLITE_TRIDENT.get())) && super.canUse();
            }
            @Override public boolean canContinueToUse() { return canUse(); }
        });
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true) {
            @Override public boolean canUse() {
                ItemStack hand = getMainHandItem();
                return !hand.is(Items.TRIDENT) && !hand.is(ModItems.AXOLITE_TRIDENT.get()) && super.canUse();
            }
        });
        goalSelector.addGoal(2, new VillageWaterPatrolGoal());
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true,
                entity -> entity instanceof Player player && !player.isCreative() && !player.isSpectator()));
    }
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide) {
            if (villageAlarmTicks > 0) {
                villageAlarmTicks--;
                if (villageAlarmTarget != null && level() instanceof net.minecraft.server.level.ServerLevel server) {
                    var target = server.getPlayerByUUID(villageAlarmTarget);
                    if (target != null && target.isAlive() && !target.isCreative() && !target.isSpectator()) setTarget(target);
                }
            } else if (villageAlarmTarget != null) {
                villageAlarmTarget = null;
                setTarget(null);
            }
            if (!cosmeticHelmetChosen) {
                cosmeticHelmetChosen = true;
                cosmeticHelmet = random.nextFloat() < 0.12F;
            }

            // Helmet is visual-only: do not equip a non-helmet item as armor.
        }
    }
    public void setVillageHome(net.minecraft.core.BlockPos home, int radius) {
        villageHome = home.immutable();
        villagePatrolRadius = Math.max(8, radius);
    }

    /** Picks submerged, collision-free 3D destinations within the resident's village. */
    private final class VillageWaterPatrolGoal extends Goal {
        private net.minecraft.core.BlockPos destination;
        private int nextAttempt;
        VillageWaterPatrolGoal() {
            setFlags(java.util.EnumSet.of(Goal.Flag.MOVE));
        }
        @Override public boolean canUse() {
            if (getTarget() != null || villageAlarmTicks > 0 || !isInWater()) return false;
            if (--nextAttempt > 0) return false;
            nextAttempt = 30 + getRandom().nextInt(50);
            net.minecraft.core.BlockPos home = villageHome != null ? villageHome : blockPosition();
            int radius = villageHome != null ? villagePatrolRadius : 16;
            // Thermocline top is Y=110; Abyss water surface is around Y=200.
            // Leave a block of clearance at both ends.
            for (int attempt = 0; attempt < 24; attempt++) {
                int x = home.getX() + getRandom().nextInt(radius * 2 + 1) - radius;
                int z = home.getZ() + getRandom().nextInt(radius * 2 + 1) - radius;
                if (Math.abs(x - home.getX()) > radius || Math.abs(z - home.getZ()) > radius) continue;
                int y = 112 + getRandom().nextInt(87); // 112..198
                var pos = new net.minecraft.core.BlockPos(x, y, z);
                if (!level().getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) continue;
                if (!level().getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)) continue;
                if (!level().getFluidState(pos.above()).is(net.minecraft.tags.FluidTags.WATER)) continue;
                if (!level().getBlockState(pos).getCollisionShape(level(), pos).isEmpty()) continue;
                destination = pos;
                return true;
            }
            return false;
        }
        @Override public boolean canContinueToUse() {
            return destination != null && getTarget() == null && villageAlarmTicks == 0
                    && !getNavigation().isDone() && distanceToSqr(
                    destination.getX() + 0.5, destination.getY() + 0.5,
                    destination.getZ() + 0.5) > 3.0;
        }
        @Override public void start() {
            if (destination != null) getNavigation().moveTo(
                    destination.getX() + 0.5, destination.getY() + 0.5,
                    destination.getZ() + 0.5, 1.0);
        }
        @Override public void stop() {
            getNavigation().stop();
            destination = null;
        }
    }
    public void raiseVillageAlarm(Player player) {
        if (player.isCreative() || player.isSpectator()) return;
        villageAlarmTarget = player.getUUID();
        villageAlarmTicks = 600;
        setTarget(player);
    }
    public boolean hasCosmeticHelmet() { return cosmeticHelmet; }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (villageAlarmTarget != null) tag.putUUID("VillageAlarmTarget", villageAlarmTarget);
        tag.putInt("VillageAlarmTicks", villageAlarmTicks);
        if (villageHome != null) {
            tag.putInt("VillageHomeX", villageHome.getX());
            tag.putInt("VillageHomeY", villageHome.getY());
            tag.putInt("VillageHomeZ", villageHome.getZ());
            tag.putInt("VillagePatrolRadius", villagePatrolRadius);
            tag.putBoolean("HasVillageHome", true);
        }
        tag.putBoolean("MerolotlHelmetChosen", cosmeticHelmetChosen);
        tag.putBoolean("MerolotlCosmeticHelmet", cosmeticHelmet);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        villageAlarmTarget = tag.hasUUID("VillageAlarmTarget") ? tag.getUUID("VillageAlarmTarget") : null;
        villageAlarmTicks = tag.getInt("VillageAlarmTicks");
        if (tag.getBoolean("HasVillageHome")) {
            setVillageHome(new net.minecraft.core.BlockPos(
                    tag.getInt("VillageHomeX"), tag.getInt("VillageHomeY"),
                    tag.getInt("VillageHomeZ")), tag.getInt("VillagePatrolRadius"));
        }
        cosmeticHelmetChosen = tag.getBoolean("MerolotlHelmetChosen");
        cosmeticHelmet = tag.getBoolean("MerolotlCosmeticHelmet");
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            Vec3 v = getDeltaMovement();
            boolean swimming = v.horizontalDistanceSqr() + v.y * v.y > 0.0025;
            state.setAnimation(RawAnimation.begin().thenLoop(swimming ? "animation.merolotl.swim" : "animation.merolotl.idle"));
            return PlayState.CONTINUE;
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animations; }
}
