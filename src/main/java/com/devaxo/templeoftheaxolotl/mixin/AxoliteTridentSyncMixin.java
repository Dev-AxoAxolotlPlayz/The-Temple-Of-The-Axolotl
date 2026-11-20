package com.devaxo.templeoftheaxolotl.mixin;
import com.devaxo.templeoftheaxolotl.entity.AxoliteTridentVisualState;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ThrownTrident.class)
public abstract class AxoliteTridentSyncMixin implements AxoliteTridentVisualState {
    @org.spongepowered.asm.mixin.Shadow private boolean dealtDamage;
    @Unique private static final EntityDataAccessor<Boolean> TOTA_AXOLITE = SynchedEntityData.defineId(ThrownTrident.class, EntityDataSerializers.BOOLEAN);
    @Unique private static final EntityDataAccessor<Boolean> TOTA_COSMIC = SynchedEntityData.defineId(ThrownTrident.class, EntityDataSerializers.BOOLEAN);
    @Inject(method="defineSynchedData",at=@At("TAIL"))
    private void tota$define(SynchedEntityData.Builder builder, CallbackInfo ci) { builder.define(TOTA_AXOLITE,false); builder.define(TOTA_COSMIC,false); }
    @Override public boolean tota$isCosmic() { return ((ThrownTrident)(Object)this).getEntityData().get(TOTA_COSMIC); }
    @Override public boolean tota$isAxolite() { return ((ThrownTrident)(Object)this).getEntityData().get(TOTA_AXOLITE); }
    @Unique private Mob tota$cachedNearbyTarget;
    @Unique private int tota$idleTicks;
    @Unique private boolean tota$detonated;
    @Unique private boolean tota$returning;
    @Unique private boolean tota$voidRescued;
    @Unique private int tota$lootSweepTicks;
    @Unique private Vec3 tota$lastImpactPosition;

    @Unique private final java.util.Set<java.util.UUID> tota$visited = new java.util.HashSet<>();
    @Unique private boolean tota$eligible(ThrownTrident e, Mob mob) {
        var owner=e.getOwner();
        return !com.devaxo.templeoftheaxolotl.combat.InnateProtection.villager(mob) && !(mob instanceof com.devaxo.templeoftheaxolotl.entity.AxoCthuluEntity) && mob.isAlive() && mob.isPickable() && mob != owner && !tota$visited.contains(mob.getUUID())
            && (owner == null || (!owner.isAlliedTo(mob) && !mob.isAlliedTo(owner)))
            && !(mob instanceof net.minecraft.world.entity.TamableAnimal t && t.isTame());
    }
    // Purely visual particle lightning: never spawn a LightningBolt entity.
    @Unique private void tota$arc(net.minecraft.server.level.ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 direction=to.subtract(from);
        double length=direction.length();
        if(length<0.001) return;
        Vec3 sideways=direction.cross(new Vec3(0,1,0));
        if(sideways.lengthSqr()<0.001) sideways=direction.cross(new Vec3(1,0,0));
        sideways=sideways.normalize();
        Vec3 vertical=direction.normalize().cross(sideways).normalize();
        int steps=Math.max(8,(int)Math.ceil(length*12));
        Vec3 last=from;
        for(int i=1;i<=steps;i++) {
            double t=(double)i/steps;
            double envelope=Math.sin(Math.PI*t);
            Vec3 point=from.lerp(to,t).add(sideways.scale((level.random.nextDouble()-0.5)*0.38*envelope))
                .add(vertical.scale((level.random.nextDouble()-0.5)*0.38*envelope));
            // Draw between successive jittered vertices, avoiding disconnected dots.
            int subdivisions=3;
            for(int j=0;j<=subdivisions;j++) {
                Vec3 p=last.lerp(point,(double)j/subdivisions);
                level.sendParticles(new DustParticleOptions(new Vector3f(0.77f,0.99f,1f),1.45f),p.x,p.y,p.z,1,0,0,0,0);
                if((i+j)%2==0) level.sendParticles(new DustParticleOptions(new Vector3f(0.15f,0.88f,1f),1.05f),p.x,p.y,p.z,1,0,0,0,0);
                if((i+j)%3==0) level.sendParticles(new DustParticleOptions(new Vector3f(1f,0.24f,0.80f),1.25f),p.x,p.y,p.z,1,0.07,0.07,0.07,0);
            }
            // Tiny ornamental forks, not damaging extra targets.
            if(i%7==0 && i<steps-2) {
                Vec3 fork=point.add(sideways.scale((level.random.nextBoolean()?1:-1)*(0.3+level.random.nextDouble()*0.45)))
                    .add(vertical.scale(level.random.nextDouble()*0.35));
                for(int k=0;k<5;k++) {
                    Vec3 p=point.lerp(fork,k/4.0);
                    level.sendParticles(new DustParticleOptions(new Vector3f(0.72f,0.43f,1f),0.8f),p.x,p.y,p.z,1,0,0,0,0);
                }
            }
            last=point;
        }
        for(int i=0;i<12;i++) {
            double dx=(level.random.nextDouble()-0.5)*0.5,dy=(level.random.nextDouble()-0.5)*0.5,dz=(level.random.nextDouble()-0.5)*0.5;
            level.sendParticles(new DustParticleOptions(new Vector3f(i%2==0?0.2f:1f,0.65f,i%2==0?1f:0.85f),0.85f),to.x,to.y,to.z,1,dx,dy,dz,0.08);
        }
        level.playSound(null,net.minecraft.core.BlockPos.containing(to),net.minecraft.sounds.SoundEvents.TRIDENT_THUNDER.value(),
            net.minecraft.sounds.SoundSource.PLAYERS,0.22f,1.65f+level.random.nextFloat()*0.25f);
    }
    @Unique private final java.util.Set<java.util.UUID> tota$cargo = new java.util.HashSet<>();
    @Unique private boolean tota$loyal(ThrownTrident e) {
        return tota$isAxolite(); // Built-in Loyalty: no enchantment required.
    }
    @Unique private double tota$homingRadius(ThrownTrident e) {
        var key=net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl","auto_target"));
        var enchant=e.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(key).orElse(null);
        return enchant != null && ((AxoliteArrowStackAccessor)e).tota$pickupStack()
            .getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS,
                net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY).getLevel(enchant)>0 ? 15.0 : 7.0;
    }
    @Unique private double tota$range = -1.0;
    @Unique private void tota$return(ThrownTrident e) {
        tota$returning=true;
        dealtDamage=true;
        e.setNoGravity(true);
        e.setNoPhysics(true);
        tota$cachedNearbyTarget=null;
    }
    @Unique private void tota$detonate(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.Entity hit, Player owner) {
        if(tota$detonated) return;
        tota$detonated=true;
        Vec3 center=hit.getBoundingBox().getCenter();
        // Eight overlapping cyan / purple / pink firework-like bursts, no terrain damage.
        Vector3f[] colors={new Vector3f(.314f,.914f,.937f),new Vector3f(.643f,.361f,1f),new Vector3f(1f,.396f,.784f)};
        for(int burst=0;burst<8;burst++) {
            Vector3f color=colors[burst%3];
            for(int p=0;p<24;p++) {
                double theta=level.random.nextDouble()*Math.PI*2;
                double z=level.random.nextDouble()*2-1;
                double r=Math.sqrt(1-z*z);
                double radius=.35+level.random.nextDouble()*1.4;
                level.sendParticles(new DustParticleOptions(color,1.2f),
                    center.x+Math.cos(theta)*r*radius,
                    center.y+z*radius,
                    center.z+Math.sin(theta)*r*radius,
                    1,0,0,0,0);
            }
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FIREWORK,
            center.x,center.y,center.z,75,.8,.8,.8,.12);
        level.playSound(null,net.minecraft.core.BlockPos.containing(center),
            net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST,
            net.minecraft.sounds.SoundSource.PLAYERS,1.4f,1.05f);
        for(Mob mob:level.getEntitiesOfClass(Mob.class,
                new net.minecraft.world.phys.AABB(center,center).inflate(5),
                m -> m.isAlive() && m != hit && tota$eligible((ThrownTrident)(Object)this,m))) {
            double distance=mob.getBoundingBox().getCenter().distanceTo(center);
            float damage=(float)(8.0*Math.max(0,1-distance/5.0));
            if(damage>0) mob.hurt(level.damageSources().playerAttack(owner),damage);
        }
    }
    @Inject(method="onHitEntity",at=@At("HEAD"))
    private void tota$beforeLoot(EntityHitResult hit, CallbackInfo ci) {
        ThrownTrident e=(ThrownTrident)(Object)this;
        if(e.level().isClientSide || !tota$isAxolite() || !tota$loyal(e)) return;
        tota$impactSucceeded=false;
        tota$visited.clear();
        tota$visited.add(hit.getEntity().getUUID());
        var box=hit.getEntity().getBoundingBox().inflate(2);
        tota$existing.clear();
        for(var item:e.level().getEntitiesOfClass(ItemEntity.class,box)) tota$existing.add(item.getUUID());
        for(var orb:e.level().getEntitiesOfClass(ExperienceOrb.class,box)) tota$existing.add(orb.getUUID());
    }
    @Unique private boolean tota$impactSucceeded;
    @Redirect(method="onHitEntity", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean tota$damage(net.minecraft.world.entity.Entity target, net.minecraft.world.damagesource.DamageSource source, float amount) {
        ThrownTrident e=(ThrownTrident)(Object)this;
        if(tota$isAxolite() && target instanceof net.minecraft.world.entity.monster.EnderMan && e.getOwner() instanceof Player owner) {
            source=e.damageSources().playerAttack(owner);
        }
        // Prevent owner damage from an Axolite projectile, including on its return.
        if (tota$isAxolite() && e.getOwner() != null
                && target.getUUID().equals(e.getOwner().getUUID())) {
            tota$impactSucceeded = false;
            return false;
        }
        boolean success=target.hurt(source,amount + (tota$isAxolite() ? 4.0f : 0.0f));
        tota$impactSucceeded=success;
        return success;
    }
    @Unique private final java.util.Set<java.util.UUID> tota$existing = new java.util.HashSet<>();
    @Inject(method="onHitEntity",at=@At("RETURN"))
    private void tota$afterLoot(EntityHitResult hit, CallbackInfo ci) {
        ThrownTrident e=(ThrownTrident)(Object)this;
        if(!(e.level() instanceof net.minecraft.server.level.ServerLevel level) || !tota$isAxolite() || !tota$loyal(e)) return;
        var box=hit.getEntity().getBoundingBox().inflate(2);
        if(!hit.getEntity().isAlive()) {
            tota$lastImpactPosition = hit.getEntity().getBoundingBox().getCenter();
            tota$lootSweepTicks = 12;
            for(var item:level.getEntitiesOfClass(ItemEntity.class,box)) if(!tota$existing.contains(item.getUUID())) tota$cargo.add(item.getUUID());
            for(var orb:level.getEntitiesOfClass(ExperienceOrb.class,box)) if(!tota$existing.contains(orb.getUUID())) tota$cargo.add(orb.getUUID());
        }
        tota$existing.clear();
        if(tota$impactSucceeded && hit.getEntity() instanceof net.minecraft.world.entity.LivingEntity victim
                && e.getOwner() instanceof Player attacker) {
            com.devaxo.templeoftheaxolotl.combat.ShellShockedLogic.apply(victim, attacker);
        }
        if(!tota$impactSucceeded || !(e.getOwner() instanceof Player owner)) return;
        if (com.devaxo.templeoftheaxolotl.combat.InnateProtection.villager(hit.getEntity())) { tota$return(e); return; }
        if (((AxoliteArrowStackAccessor)e).tota$pickupStack().is(ModItems.COSMIC_TRIDENT.get())) {
            com.devaxo.templeoftheaxolotl.combat.StarfallBurst.doubleBurst(level,
                hit.getEntity().getBoundingBox().getCenter(), owner, hit.getEntity(),
                ((AxoliteArrowStackAccessor)e).tota$pickupStack());
        } else tota$detonate(level,hit.getEntity(),owner);
        tota$return(e);
        if(!(hit.getEntity() instanceof Mob)) return;
        // Shared lightning engine: thrown attacks start at 100%; melee starts at 50%.
        com.devaxo.templeoftheaxolotl.combat.AxoliteLightning.chain(
            level, owner, e, (Mob)hit.getEntity(), 0, tota$visited, tota$cargo,
            ((AxoliteArrowStackAccessor)e).tota$pickupStack().is(ModItems.COSMIC_TRIDENT.get()),
            ((AxoliteArrowStackAccessor)e).tota$pickupStack());

    }
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void tota$sync(CallbackInfo ci) {
        ThrownTrident e=(ThrownTrident)(Object)this;
        if(!e.level().isClientSide) {
            boolean axolite = com.devaxo.templeoftheaxolotl.item.AxoliteTridentMode.isAxoliteFamily(((AxoliteArrowStackAccessor)e).tota$pickupStack());
            if (tota$isAxolite() != axolite) e.getEntityData().set(TOTA_AXOLITE, axolite);
            boolean cosmic = ((AxoliteArrowStackAccessor)e).tota$pickupStack().is(ModItems.COSMIC_TRIDENT.get());
            if (tota$isCosmic() != cosmic) e.getEntityData().set(TOTA_COSMIC, cosmic);
            var weapon = ((AxoliteArrowStackAccessor)e).tota$pickupStack();
            boolean retained=com.devaxo.templeoftheaxolotl.trident.PersistentTridents.marked(e);
            if(retained){
                if(!(e.getOwner() instanceof Player owner)||!owner.isAlive()||!com.devaxo.templeoftheaxolotl.trident.PersistentTridents.valid(e,owner,weapon)) {e.discard();ci.cancel();return;}
                if(dealtDamage || e.isNoPhysics() || e.getPersistentData().getBoolean(com.devaxo.templeoftheaxolotl.trident.PersistentTridents.RECALL))tota$return(e);
            }
            int voidLevel = com.devaxo.templeoftheaxolotl.trident.EternalVoid.level(e.level(), weapon);
            if (voidLevel > 0 && e.getY() <= -63.0 && !tota$voidRescued) {
                tota$voidRescued = true;
                e.setPos(e.getX(), -62.0, e.getZ());
                e.setDeltaMovement(Vec3.ZERO);
                e.setNoGravity(true);
                // Vanilla Loyalty uses dealtDamage to begin its return; the Axolite
                // family uses its own owner-tracking return implementation.
                dealtDamage = true;
                if (tota$isAxolite()) tota$return(e);
            }
            // Moving magnet, both outbound and returning; the existing cargo
            // delivery routine credits the owner rather than the trident.
            if (voidLevel > 0 && e.getOwner() instanceof Player magnetOwner && magnetOwner.isAlive()
                    && e.level() instanceof net.minecraft.server.level.ServerLevel magnetLevel && e.tickCount % 2 == 0) {
                double radius = com.devaxo.templeoftheaxolotl.trident.EternalVoid.radius(voidLevel);
                var sweep = e.getBoundingBox().inflate(radius);
                for (var item : magnetLevel.getEntitiesOfClass(ItemEntity.class, sweep))
                    if (item.isAlive() && !item.getItem().isEmpty()) tota$cargo.add(item.getUUID());
                for (var orb : magnetLevel.getEntitiesOfClass(ExperienceOrb.class, sweep))
                    if (orb.isAlive()) tota$cargo.add(orb.getUUID());
            }
            if((tota$isAxolite() || voidLevel > 0) && e.level() instanceof net.minecraft.server.level.ServerLevel level && e.getOwner() instanceof Player owner && owner.isAlive()) {
                // Pick up loose cargo along the actual flight path, not only on the hit tick.
                // A short post-impact sweep also catches drops spawned after onHitEntity returns.
                if (e.tickCount % 2 == 0 || tota$lootSweepTicks > 0) {
                    var flightBox=e.getBoundingBox().inflate(1.5);
                    for (var item:level.getEntitiesOfClass(ItemEntity.class,flightBox))
                        if (item.isAlive() && !item.getItem().isEmpty()) tota$cargo.add(item.getUUID());
                    for (var orb:level.getEntitiesOfClass(ExperienceOrb.class,flightBox))
                        if (orb.isAlive()) tota$cargo.add(orb.getUUID());
                    if (tota$lootSweepTicks > 0) {
                        tota$lootSweepTicks--;
                        if (tota$lastImpactPosition != null) {
                            var impactBox=new net.minecraft.world.phys.AABB(tota$lastImpactPosition,tota$lastImpactPosition).inflate(2);
                            for (var item:level.getEntitiesOfClass(ItemEntity.class,impactBox))
                                if (item.isAlive() && !tota$existing.contains(item.getUUID())) tota$cargo.add(item.getUUID());
                            for (var orb:level.getEntitiesOfClass(ExperienceOrb.class,impactBox))
                                if (orb.isAlive() && !tota$existing.contains(orb.getUUID())) tota$cargo.add(orb.getUUID());
                        }
                    }
                }
                boolean arrived=e.distanceToSqr(owner)<4 || (retained && tota$returning && owner.getEyePosition().distanceToSqr(e.position())<4);
                var it=tota$cargo.iterator();
                while(it.hasNext()) {
                    var cargo=level.getEntity(it.next());
                    if(cargo==null || !cargo.isAlive()) { it.remove(); continue; }
                    if (cargo instanceof ItemEntity item && voidLevel>0 && owner instanceof net.minecraft.server.level.ServerPlayer sp) {
                        var stack=item.getItem();
                        if(item.getOwner()!=null && !item.getOwner().equals(owner)){it.remove();continue;}
                        for(int page=0;page<com.devaxo.templeoftheaxolotl.repository.CelestialRepository.pages(sp)&&!stack.isEmpty();page++) {
                            int moved=com.devaxo.templeoftheaxolotl.repository.CelestialRepository.insertPage(sp,page,stack);
                            stack.shrink(moved);
                        }
                        if(stack.isEmpty())item.discard();else item.setItem(stack);
                        it.remove();continue;
                    }
                    if (arrived) {
                        if (cargo instanceof ExperienceOrb orb) {
                            orb.setPos(owner.getX(),owner.getY()+0.2,owner.getZ());
                            orb.setDeltaMovement(Vec3.ZERO);
                        } else if (cargo instanceof ItemEntity item) {
                            var stack=item.getItem();
                            owner.getInventory().add(stack);
                            if (stack.isEmpty()) item.discard();
                            else {
                                item.setPos(owner.getX(),owner.getY()+0.4,owner.getZ());
                                item.setNoPickUpDelay();
                            }
                        }
                        it.remove();
                    } else {
                        var dest=e.position();
                        cargo.setPos(dest.x,dest.y,dest.z);
                        cargo.setDeltaMovement(Vec3.ZERO);
                        if (cargo instanceof ItemEntity item) item.setPickUpDelay(10);
                    }
                }
            }
            if(tota$isAxolite()) {
                if(tota$returning) {
                    if(e.getOwner() instanceof Player owner && owner.isAlive()) {
                        Vec3 destination=owner.getEyePosition().subtract(e.position());
                        if(destination.lengthSqr()<2.25 || (retained && e.getBoundingBox().inflate(0.75).intersects(owner.getBoundingBox()))) {
                            if(retained) com.devaxo.templeoftheaxolotl.trident.PersistentTridents.clear(e,owner,weapon);
                            else {var stack=((AxoliteArrowStackAccessor)e).tota$pickupStack().copy();
                            if(!owner.getInventory().add(stack) && !stack.isEmpty()) owner.drop(stack,false);}
                            e.discard();
                            ci.cancel();return;
                        }
                        Vec3 step=destination.normalize().scale(Math.min(2.0,Math.max(0.6,destination.length()*0.35)));
                        e.setDeltaMovement(step);
                        e.hasImpulse=true;
                        if(retained) {
                            // Own the return movement; vanilla Loyalty must not steer or pick up this copy.
                            e.setPos(e.getX()+step.x,e.getY()+step.y,e.getZ()+step.z);
                            tota$orientFlight(ci);
                            ci.cancel();return;
                        }
                    } else {
                        e.setNoGravity(false);
                        tota$returning=false;
                    }
                }
                if(tota$range<0) tota$range=tota$homingRadius(e);
                if(!tota$returning && !dealtDamage) {
                    // Scan only every four ticks, steer cached target every tick.
                    Vec3 pos=e.position();
                    double radius=tota$range;
                    if(tota$cachedNearbyTarget!=null &&
                            (!tota$eligible(e,tota$cachedNearbyTarget)
                            || tota$cachedNearbyTarget.distanceToSqr(e)>radius*radius))
                        tota$cachedNearbyTarget=null;
                    if(tota$cachedNearbyTarget==null && e.tickCount%4==0) {
                        double best=radius*radius;
                        for(Mob mob:e.level().getEntitiesOfClass(Mob.class,
                                new net.minecraft.world.phys.AABB(pos,pos).inflate(radius),
                                m->tota$eligible(e,m) && com.devaxo.templeoftheaxolotl.network.TridentFovPayload.withinView(e.getOwner(),m))) {
                            double dist=mob.distanceToSqr(e);
                            if(dist<best) { best=dist; tota$cachedNearbyTarget=mob; }
                        }
                    }
                    Mob target=tota$cachedNearbyTarget;
                    if(target!=null) {
                        tota$idleTicks=0;
                        Vec3 aim=target.getBoundingBox().getCenter().subtract(pos);
                        if(aim.lengthSqr()>0.0001) {
                            Vec3 velocity=e.getDeltaMovement();
                            double speed=Math.max(velocity.length(),1.25);
                            Vec3 direction=velocity.lengthSqr()>0.0001
                                ? velocity.normalize().scale(.15).add(aim.normalize().scale(.85)).normalize()
                                : aim.normalize();
                            e.setDeltaMovement(direction.scale(Math.min(speed,Math.max(.55,aim.length()*.85))));
                        }
                    } else if(++tota$idleTicks>=100) tota$return(e);
                }
                // Gravity stays disabled during outbound flight AND innate Loyalty return.
                if(!e.isNoGravity()) e.setNoGravity(true);
            }
        } else if(tota$isAxolite() && e.getDeltaMovement().lengthSqr()>0.04 && e.tickCount%2==0) {
            var color = switch ((e.tickCount/2)%3) {
                case 0 -> new Vector3f(0.20f,0.95f,1.0f);
                case 1 -> new Vector3f(1.0f,0.30f,0.76f);
                default -> new Vector3f(0.68f,0.45f,1.0f);
            };
            e.level().addParticle(new DustParticleOptions(color,0.9f),e.getX(),e.getY(),e.getZ(),0,0,0);
            e.level().addParticle(new DustParticleOptions(color,0.5f),e.getX()+0.12,e.getY(),e.getZ()-0.12,0,0,0);
        }
    }
    @Inject(method="tryPickup",at=@At("HEAD"),cancellable=true)
    private void tota$noRetainedPickup(Player p, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if(com.devaxo.templeoftheaxolotl.trident.PersistentTridents.marked((ThrownTrident)(Object)this))cir.setReturnValue(false);
    }
    @Inject(method="tick", at=@At("TAIL"))
    private void tota$orientFlight(CallbackInfo ci) {
        if (!tota$isAxolite()) return;
        ThrownTrident e=(ThrownTrident)(Object)this;
        Vec3 motion=e.getDeltaMovement();
        if (motion.lengthSqr()<1.0e-5) return;
        double horizontal=Math.sqrt(motion.x*motion.x+motion.z*motion.z);
        float yaw=(float)(Math.atan2(motion.x,motion.z)*180.0/Math.PI);
        float pitch=(float)(Math.atan2(motion.y,horizontal)*180.0/Math.PI);
        e.setYRot(yaw);
        e.setXRot(pitch);
        e.yRotO=yaw;
        e.xRotO=pitch;
    }
}
