package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.world.entity.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import java.util.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/** First-stage AxoCthulu: free-floating aquatic boss model, no encounter AI yet. */
public class AxoCthuluEntity extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private Vec3 swimTarget;
    private int targetTicks;
    // 0..3 mob waves; 4 = crimson transition; 5 = final battle.
    private int encounterStage;
    private int waveSpawned;
    private int waveStep; // 0 spawn mobs, 1 clear mobs, 2 clear tentacles, 3 shield strike
    private int awakeningTicks;
    private final Set<UUID> summoned = new HashSet<>();
    public void forgetSummon(UUID id) { summoned.remove(id); overgrowth.remove(id); }
    private final net.minecraft.server.level.ServerBossEvent hud = new net.minecraft.server.level.ServerBossEvent(net.minecraft.network.chat.Component.literal("AxoCthulu"), net.minecraft.world.BossEvent.BossBarColor.PURPLE, net.minecraft.world.BossEvent.BossBarOverlay.NOTCHED_6);
    private void updateHud(ServerLevel s) {
        for(var p:java.util.List.copyOf(hud.getPlayers())) if(p.level()!=s || !p.isAlive() || !s.players().contains(p)) hud.removePlayer(p);
        for(var p:s.players()) if(p.isAlive() && !p.isSpectator()) { hud.addPlayer(p); EldritchHudCleanup.remember(p,hud); }
        hud.setProgress(encounterStage < 4 ? (4-encounterStage)/4f : encounterStage==4 ? 0f : getHealth()/getMaxHealth());
        hud.setName(net.minecraft.network.chat.Component.literal(encounterStage<4 ? "AxoCthulu — Wave "+(encounterStage+1)+"/4 | "+(waveStep==3?"Strike the shield":summoned.size()+" remaining") : encounterStage==4?"AxoCthulu — Awakening":"AxoCthulu — Cosmic Trident only"));
    }
    @Override public void remove(Entity.RemovalReason reason) { hud.removeAllPlayers(); super.remove(reason); }

    private boolean encounterStarted;
    private int encounterCooldown;
    // Final-phase laser: 40 ticks warning, then one server-authoritative hit.
    private int laserCooldown = 100;
    private int laserCharge;
    private UUID laserTarget;
    private int overgrowthCooldown = 180;
    private final Set<UUID> overgrowth = new HashSet<>();

    public int getEncounterStage() { return encounterStage; }
    public int getWaveStep() { return waveStep; }
    public void beginEncounter() {
        if (!level().isClientSide && !encounterStarted) {
            encounterStarted = true;
            encounterStage = 0;
            waveStep = 0;
            encounterCooldown = 20;
        }
    }
    private boolean cosmicHit(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct instanceof net.minecraft.world.entity.projectile.ThrownTrident thrown) {
            return ((com.devaxo.templeoftheaxolotl.mixin.AxoliteArrowStackAccessor) thrown)
                    .tota$pickupStack().is(com.devaxo.templeoftheaxolotl.item.ModItems.COSMIC_TRIDENT.get());
        }
        return direct instanceof net.minecraft.world.entity.player.Player player
                && player.getMainHandItem().is(com.devaxo.templeoftheaxolotl.item.ModItems.COSMIC_TRIDENT.get());
    }
    @Override public boolean hurt(DamageSource source, float amount) {
        if (!cosmicHit(source)) return false;
        if (encounterStage < 4) {
            return strikeShield(source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.ThrownTrident thrown
                    ? ((com.devaxo.templeoftheaxolotl.mixin.AxoliteArrowStackAccessor) thrown).tota$pickupStack()
                    : ((net.minecraft.world.entity.player.Player)source.getDirectEntity()).getMainHandItem());
        }
        if (encounterStage != 5) return false;
        return super.hurt(source, Math.min(24.0F, amount));
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("EldritchStarted", encounterStarted);
        tag.putBoolean("EldritchEyeRewarded", eyeRewarded);
        tag.putInt("EldritchStage", encounterStage);
        tag.putInt("EldritchStep", waveStep);
        tag.putInt("EldritchWaveSpawned", waveSpawned);
        tag.putInt("EldritchAwakening", awakeningTicks);
        tag.putUUID("EldritchInstance", getUUID());
        tag.putInt("EldritchCooldown", encounterCooldown);
        tag.putInt("EldritchLaserCooldown", laserCooldown);
        tag.putInt("EldritchLaserCharge", laserCharge);
        tag.putInt("EldritchOvergrowthCooldown", overgrowthCooldown);
        if (laserTarget != null) tag.putUUID("EldritchLaserTarget", laserTarget);
        net.minecraft.nbt.ListTag growthIds = new net.minecraft.nbt.ListTag();
        for (UUID id : overgrowth) { CompoundTag e = new CompoundTag(); e.putUUID("Id", id); growthIds.add(e); }
        tag.put("EldritchOvergrowth", growthIds);
        net.minecraft.nbt.ListTag ids = new net.minecraft.nbt.ListTag();
        for (UUID uuid : summoned) { CompoundTag entry = new CompoundTag(); entry.putUUID("Id", uuid); ids.add(entry); }
        tag.put("EldritchSummons", ids);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        encounterStarted = tag.getBoolean("EldritchStarted");
        eyeRewarded = tag.getBoolean("EldritchEyeRewarded");
        encounterStage = tag.getInt("EldritchStage");
        waveStep = tag.getInt("EldritchStep");
        waveSpawned = tag.getInt("EldritchWaveSpawned");
        awakeningTicks = tag.getInt("EldritchAwakening");
        encounterCooldown = tag.getInt("EldritchCooldown");
        laserCooldown = tag.contains("EldritchLaserCooldown") ? tag.getInt("EldritchLaserCooldown") : 100;
        laserCharge = tag.getInt("EldritchLaserCharge");
        overgrowthCooldown = tag.contains("EldritchOvergrowthCooldown") ? tag.getInt("EldritchOvergrowthCooldown") : 180;
        laserTarget = tag.hasUUID("EldritchLaserTarget") ? tag.getUUID("EldritchLaserTarget") : null;
        overgrowth.clear();
        var growthIds = tag.getList("EldritchOvergrowth", 10);
        for (int i = 0; i < growthIds.size(); i++) {
            CompoundTag entry = growthIds.getCompound(i);
            if (entry.hasUUID("Id")) overgrowth.add(entry.getUUID("Id"));
        }
        if (encounterStage == 5) {
            var maxHealth = getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) maxHealth.setBaseValue(120.0D);
        }
        summoned.clear();
        var ids = tag.getList("EldritchSummons", 10);
        for (int i = 0; i < ids.size(); i++) { CompoundTag entry = ids.getCompound(i); if (entry.hasUUID("Id")) summoned.add(entry.getUUID("Id")); }
        if (!tag.contains("EldritchWaveSpawned") && encounterStage<4 && waveStep==1 && summoned.isEmpty()) waveStep=0;
    }
    // Award the Eye exactly once, only for a legitimate completed final-phase kill.
    private boolean eyeRewarded;
    private void awardEye(DamageSource source) {
        if (eyeRewarded || encounterStage != 5 || !(level() instanceof ServerLevel server)) return;
        net.minecraft.world.entity.player.Player winner = source.getEntity() instanceof net.minecraft.world.entity.player.Player p ? p : null;
        if (!(winner instanceof net.minecraft.server.level.ServerPlayer player)) return;
        eyeRewarded = true;
        net.minecraft.world.item.ItemStack reward = new net.minecraft.world.item.ItemStack(
                com.devaxo.templeoftheaxolotl.item.ModItems.EYE_OF_THE_ABYSS.get());
        if (!player.getInventory().add(reward)) player.drop(reward, false);
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                getX(), getY(), getZ(), 120, 3.0, 3.0, 3.0, 0.15);
    }
    @Override public void die(DamageSource source) {
        if (level() instanceof ServerLevel s && com.devaxo.templeoftheaxolotl.world.EldritchEncounter.arena(s)) {
            if(com.devaxo.templeoftheaxolotl.world.EldritchEncounter.get(s).owns(getUUID())) { awardEye(source); com.devaxo.templeoftheaxolotl.world.EldritchEncounter.get(s).complete(getUUID(),s); }
        } else awardEye(source);
        if (level() instanceof ServerLevel server) {
            for (UUID id : summoned) {
                Entity add = server.getEntity(id);
                if (add != null && add.isAlive()) add.discard();
            }
            summoned.clear();
            for (UUID id : overgrowth) {
                Entity add = server.getEntity(id);
                if (add != null) add.discard();
            }
            overgrowth.clear();
        }
        super.die(source);
    }

    private boolean spawnWave(ServerLevel server, boolean tentacles) {
        int wave = encounterStage;
        EntityType<?>[] types = switch (wave) {
            case 0 -> new EntityType<?>[]{ModEntities.MEROLOTL.get()};
            case 1 -> new EntityType<?>[]{ModEntities.MEROLOTL.get(), ModEntities.MEGOLOTL.get()};
            case 2 -> new EntityType<?>[]{ModEntities.MEROLOTL.get(), ModEntities.AXOKRAKEN.get(), ModEntities.MEGOLOTL.get()};
            default -> new EntityType<?>[]{ModEntities.MEROLOTL.get(), ModEntities.AXOKRAKEN.get(), ModEntities.MEGOLOTL.get(), ModEntities.AXOLEVIATHAN.get()};
        };
        int[] counts = switch (wave) {
            case 0 -> new int[]{20};
            case 1 -> new int[]{15,5};
            case 2 -> new int[]{10,5,5};
            default -> new int[]{8,5,5,2};
        };
        int total = tentacles ? wave + 1 : 20;
        for (int i = waveSpawned; i < total; i++) {
            Entity entity;
            if (tentacles) {
                AttackTentacleEntity tentacle = ModEntities.ATTACK_TENTACLE.get().create(server);
                if (tentacle == null) return false;
                tentacle.setEncounterOwner(getUUID());
                entity = tentacle;
            } else {
                int index = i;
                int kind = 0;
                while (kind < counts.length - 1 && index >= counts[kind]) index -= counts[kind++];
                entity = types[kind].create(server);
                if (entity == null) return false;
                if (entity instanceof AxoleviathanEntity leviathan) leviathan.setEldritchVariant(true);
            }
            double angle = (Math.PI * 2 * i / total);
            double radius = tentacles ? 18 : 28 + (i % 3) * 6;
            double sx=getX()+Math.cos(angle)*radius, sz=getZ()+Math.sin(angle)*radius;
            double sy=getY()-3;
            if(com.devaxo.templeoftheaxolotl.world.EldritchEncounter.arena(server)) {
                sx=net.minecraft.util.Mth.clamp(sx,-240,240); sz=net.minecraft.util.Mth.clamp(sz,-240,240);
                sy=net.minecraft.util.Mth.clamp(sy,-50,14);
            }
            entity.moveTo(sx, sy, sz, random.nextFloat()*360, 0);
            entity.getPersistentData().putUUID("EldritchOwner",getUUID());
            if(entity instanceof Mob mob) mob.setPersistenceRequired();
            server.getChunk(entity.chunkPosition().x, entity.chunkPosition().z);
            server.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.POST_TELEPORT, entity.chunkPosition(), 2, entity.getId());
            if (!server.addFreshEntity(entity)) return false;
            summoned.add(entity.getUUID());
            waveSpawned++;
            } 
        return true;
    }
    private void tickEncounter(ServerLevel server) {
        if (!encounterStarted) return;
        if (encounterStage == 4) {
            if (awakeningTicks % 10 == 0) {
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH,
                        getX(), getY(), getZ(), 18, 7, 5, 7, 0.03);
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                        getX(), getY(), getZ(), 10, 4, 4, 4, 0.08);
            }
            if (--awakeningTicks <= 0) {
                encounterStage = 5;
                var maxHealth = getAttribute(Attributes.MAX_HEALTH);
                if (maxHealth != null) maxHealth.setBaseValue(120.0D);
                setHealth(getMaxHealth());
            }
            return;
        }
        if (encounterStage == 5) { tickFinalBattle(server); return; }
        if (encounterStage > 5) return;
        if (encounterCooldown-- > 0) return;
        if (waveStep == 0) {
            if (spawnWave(server, false)) { waveStep = 1; waveSpawned = 0; }
            else encounterCooldown = 20;
            return;
        }
        if (waveStep == 4) {
            if (spawnWave(server, true)) { waveStep = 2; waveSpawned = 0; }
            else encounterCooldown = 20;
            return;
        }
        if (waveStep == 1 || waveStep == 2) {
            // Never mistake an unloaded entity for a kill: wait for its chunk to load.
            summoned.removeIf(id -> { Entity e = server.getEntity(id); return e != null && !e.isAlive(); });
            if (!summoned.isEmpty()) return;
            if (waveStep == 1) { waveStep = 4; waveSpawned = 0; }
            else { waveStep = 3; }
        }
    }
    /** The laser and independent overgrowth run only on the logical server. */
    private void tickFinalBattle(ServerLevel server) {
        var players = server.getEntitiesOfClass(net.minecraft.server.level.ServerPlayer.class,
                getBoundingBox().inflate(80), p -> p.isAlive() && !p.isCreative() && !p.isSpectator());
        if (players.isEmpty()) return;

        // Overgrowth is separate from the four shield-wave tentacle rosters.
        overgrowth.removeIf(id -> {
            Entity e = server.getEntity(id);
            return e != null && !e.isAlive();
        });
        if (--overgrowthCooldown <= 0) {
            // Limit final-phase growth to four active tentacles, and never exceed
            // twenty tracked encounter adds across both collections.
            if (overgrowth.size() < 4 && overgrowth.size() + summoned.size() < 20) {
                AttackTentacleEntity tentacle = ModEntities.ATTACK_TENTACLE.get().create(server);
                if (tentacle != null) {
                    tentacle.setEncounterOwner(getUUID());
                    tentacle.getPersistentData().putUUID("EldritchOwner",getUUID());
                    double angle = random.nextDouble() * Math.PI * 2;
                    tentacle.moveTo(getX() + Math.cos(angle) * 16, getY() - 3,
                            getZ() + Math.sin(angle) * 16, random.nextFloat() * 360, 0);
                    if (server.addFreshEntity(tentacle)) overgrowth.add(tentacle.getUUID());
                }
            }
            overgrowthCooldown = 180;
        }

        if (laserCharge > 0) {
            laserCharge--;
            net.minecraft.server.level.ServerPlayer target = laserTarget == null ? null : server.getServer().getPlayerList().getPlayer(laserTarget);
            if (target == null || !target.isAlive() || target.level() != server || target.isCreative() || target.isSpectator()) {
                laserCharge = 0;
                laserTarget = null;
                return;
            }
            Vec3 origin = getEyePosition();
            Vec3 end = target.getEyePosition();
            Vec3 delta = end.subtract(origin);
            double distance = delta.length();
            if (distance > 0.01) {
                Vec3 direction = delta.scale(1.0 / distance);
                for (double d = 0; d < Math.min(distance, 64); d += 2.0) {
                    Vec3 at = origin.add(direction.scale(d));
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                            at.x, at.y, at.z, 1, 0, 0, 0, 0);
                }
            }
            if (laserCharge == 0) {
                // A telegraphed, line-of-sight beam; no damage through solid walls.
                if (distance <= 64 && hasLineOfSight(target)) {
                    target.hurt(damageSources().mobAttack(this), 16.0F);
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                            end.x, end.y, end.z, 6, 0.5, 0.5, 0.5, 0.05);
                }
                laserTarget = null;
                laserCooldown = 120;
            }
        } else if (--laserCooldown <= 0) {
            net.minecraft.server.level.ServerPlayer target = players.get(random.nextInt(players.size()));
            laserTarget = target.getUUID();
            laserCharge = 40;
        }
    }

    /** Shield can only be advanced by the registered Cosmic Trident. */
    public boolean strikeShield(net.minecraft.world.item.ItemStack weapon) {
        if (!encounterStarted || encounterStage >= 4 || waveStep != 3) return false;
        if (!net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(weapon.getItem()).getPath().equals("cosmic_trident")) return false;
        summoned.clear();
        encounterStage++;
        waveStep = 0;
        encounterCooldown = 40;
        if (encounterStage == 4) awakeningTicks = 220;
        return true;
    }


    public AxoCthuluEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 240.0)
                .add(Attributes.MOVEMENT_SPEED, 0.20)
                .add(Attributes.FOLLOW_RANGE, 96.0)
                .add(Attributes.ARMOR, 30.0)
                .add(Attributes.ARMOR_TOUGHNESS, 20.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override protected void registerGoals() { /* Encounter AI comes later. */ }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType fluidType) { return false; }
    @Override protected int decreaseAirSupply(int air) { return air; }
    @Override protected int increaseAirSupply(int air) { return getMaxAirSupply(); }
    @Override public boolean isPushable() { return false; }
    @Override public void knockback(double strength, double x, double z) { }
    @Override public void push(double x, double y, double z) { }

    @Override public void tick() {
        setNoGravity(true);
        super.tick();
        if (level().isClientSide) return;
        if (level() instanceof ServerLevel server) {
            if(com.devaxo.templeoftheaxolotl.world.EldritchEncounter.arena(server)) {
                if(!com.devaxo.templeoftheaxolotl.world.EldritchEncounter.get(server).owns(getUUID())) { discard(); return; }
                if(tickCount%10==0) updateHud(server);
                if(server.players().stream().noneMatch(p -> p.isAlive() && !p.isSpectator())) {setDeltaMovement(Vec3.ZERO);return;}
            }
            // Begin only when a Survival participant approaches the boss in Eldritch Waters.
            if (!encounterStarted && server.dimension().location().getPath().equals("eldritch_waters")
                    && !server.players().stream().noneMatch(p -> p.isAlive() && !p.isSpectator())) {
                beginEncounter();
            }
            tickEncounter(server);
        }
        // Fully three-dimensional, water-only wandering. Never choose a waypoint
        // outside water or through solid terrain; no gravity/buoyancy correction.
        if (isInWater()) {
            if (--targetTicks <= 0 || swimTarget == null || position().distanceToSqr(swimTarget) < 3.0) {
                swimTarget = null;
                for (int i = 0; i < 12; i++) {
                    Vec3 candidate = position().add((random.nextDouble() - 0.5) * 20,
                            (random.nextDouble() - 0.5) * 14, (random.nextDouble() - 0.5) * 20);
                    net.minecraft.core.BlockPos at = net.minecraft.core.BlockPos.containing(candidate);
                    if (level().getFluidState(at).is(net.minecraft.tags.FluidTags.WATER)
                            && level().noCollision(this, getBoundingBox().move(candidate.subtract(position())))) {
                        swimTarget = candidate;
                        break;
                    }
                }
                targetTicks = 60 + random.nextInt(80);
            }
            Vec3 desired = swimTarget == null ? Vec3.ZERO : swimTarget.subtract(position()).normalize().scale(0.09);
            setDeltaMovement(getDeltaMovement().lerp(desired, 0.045));
            Vec3 velocity = getDeltaMovement();
            if (velocity.horizontalDistanceSqr() > 0.00001) {
                setYRot((float)(Mth.atan2(velocity.z, velocity.x) * 180.0 / Math.PI) - 90.0F);
                yBodyRot = getYRot();
            }
        } else {
            swimTarget = null;
            setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override public void travel(Vec3 input) {
        // Apply velocity in all three axes; never use vanilla land gravity or water sinking.
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(0.96));
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "tentacles", 0, state -> {
            state.setAnimation(RawAnimation.begin().thenLoop("Tentacles_Idle"));
            return PlayState.CONTINUE;
        }));
        controllers.add(new AnimationController<>(this, "shields", 0, state -> {
            state.setAnimation(RawAnimation.begin().thenLoop("Shield_Rings"));
            return PlayState.CONTINUE;
        }));
        controllers.add(new AnimationController<>(this, "eye", 0, state -> {
            state.setAnimation(RawAnimation.begin().thenLoop("Eye_Look_Around"));
            return PlayState.CONTINUE;
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
