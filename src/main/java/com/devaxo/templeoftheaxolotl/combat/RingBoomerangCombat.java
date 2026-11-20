package com.devaxo.templeoftheaxolotl.combat;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class RingBoomerangCombat {
    private static final Map<UUID, Flight> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> NEXT_THROW = new HashMap<>();
    private static final Map<UUID, Long> LAST_INPUT = new HashMap<>();
    private static final Set<UUID> RECALLED = new HashSet<>();
    private static final Set<UUID> HELD = new HashSet<>();

    private RingBoomerangCombat() {}

    public static void setHeld(ServerPlayer player, boolean held) {
        UUID id = player.getUUID();
        if (!held) RECALLED.remove(id);
        if (held && !RECALLED.contains(id) && !RingMiningMode.isMining(player) && player.getMainHandItem().is(ModItems.RING_OF_THE_ONE.get())) {
            HELD.add(id);
            LAST_INPUT.put(id, player.serverLevel().getGameTime());
        } else {
            HELD.remove(id);
            LAST_INPUT.remove(id);
        }
    }

    public static boolean recall(ServerPlayer player) {
        Flight f = ACTIVE.get(player.getUUID());
        if (f == null || f.visual.level() != player.level()) return false;
        f.returning = true;
        HELD.remove(player.getUUID()); RECALLED.add(player.getUUID());
        return true;
    }

    public static boolean inFlight(ServerPlayer player) { return ACTIVE.containsKey(player.getUUID()); }

    private static final class Flight {
        final Display.ItemDisplay visual;
        final net.minecraft.world.item.ItemStack ring;
        final double reach, speed;
        final Set<UUID> outwardHits = new HashSet<>(), returnHits = new HashSet<>();
        final Set<UUID> exitedAfterOutbound = new HashSet<>();
        Vec3 previous, direction;
        double remaining;
        boolean returning;
        int age, stuck;
        float heading;
        final Map<UUID,Long> lastHit = new HashMap<>();
        Flight(ServerPlayer player, int duration) {
            ring = player.getMainHandItem();
            previous = player.getEyePosition().add(0,-0.25,0);
            direction = player.getLookAngle().normalize();
            reach = Math.max(0.1, player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE));
            remaining = reach + 3;
            speed = Math.max(0.05, 2 * reach / duration);
            visual = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, player.serverLevel());
            CompoundTag tag = new CompoundTag();
            tag.put("item", ring.copyWithCount(1).save(player.registryAccess()));
            tag.putString("item_display", "fixed");
            tag.putInt("teleport_duration", 1);
            visual.load(tag);
            orient(this, direction);
            visual.setCustomName(Component.literal("ring_boomerang:" + player.getUUID()));
            visual.setPos(previous.x, previous.y, previous.z);
            player.serverLevel().addFreshEntity(visual);
        }
    }
    private static void clear(UUID id) {
        Flight f = ACTIVE.remove(id);
        if (f != null) f.visual.discard();
        HELD.remove(id); LAST_INPUT.remove(id); NEXT_THROW.remove(id); RECALLED.remove(id);
    }
    @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) { clear(e.getEntity().getUUID()); }
    @SubscribeEvent public static void dimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent e) { clear(e.getEntity().getUUID()); if (e.getEntity() instanceof ServerPlayer p) state(p,false); }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        RingMiningMode.migrate(player);
        UUID id = player.getUUID(); ServerLevel level = player.serverLevel(); long now = level.getGameTime();
        if (now % 20 == 0) for (Display.ItemDisplay d : level.getEntitiesOfClass(Display.ItemDisplay.class, player.getBoundingBox().inflate(96),
                e -> e.getCustomName() != null && e.getCustomName().getString().startsWith("ring_boomerang:"))) {
            if (ACTIVE.values().stream().noneMatch(f -> f.visual == d)) d.discard();
        }
        Flight f = ACTIVE.get(id);
        if (!player.isAlive() || !player.getMainHandItem().is(ModItems.RING_OF_THE_ONE.get()) || RingMiningMode.isMining(player)
                || (f != null && (f.ring != player.getMainHandItem() || f.visual.level() != level || f.visual.isRemoved()))) { if (f!=null) state(player,false); clear(id); return; }
        if (now - LAST_INPUT.getOrDefault(id,-100L) > 15) HELD.remove(id);
        if (f == null) {
            if (!HELD.contains(id) || now < NEXT_THROW.getOrDefault(id,0L)) return;
            double reach = Math.max(.1, player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE));
            int duration = Math.max(4, Math.min(200, (int)Math.ceil(20 / Math.max(.2,player.getAttributeValue(Attributes.ATTACK_SPEED)) * reach / 5)));
            f = new Flight(player,duration); ACTIVE.put(id,f); state(player,true); NEXT_THROW.put(id,now+duration);
        }
        f.age++;
        if (f.returning) {
            Vec3 hand = player.getEyePosition().add(0,-.25,0);
            double distance = f.previous.distanceTo(hand);
            if (!level.getChunkSource().hasChunk(net.minecraft.core.BlockPos.containing(f.previous).getX() >> 4, net.minecraft.core.BlockPos.containing(f.previous).getZ() >> 4)) { f.visual.discard(); ACTIVE.remove(id); state(player,false); return; }
            Vec3 end = distance <= Math.max(f.speed,1) ? hand : f.previous.add(hand.subtract(f.previous).normalize().scale(Math.max(f.speed,1)));
            Vec3 returnDirection = end.subtract(f.previous);
            if (returnDirection.lengthSqr() > 1e-12) f.direction = returnDirection.normalize();
            hitSegment(player,level,f,f.previous,end,true); f.previous=end;
            if (end.equals(hand)) { f.visual.discard(); ACTIVE.remove(id); state(player,false); return; }
        } else {
            double budget = f.speed;
            // Bounded per-tick work, not a lifetime bounce limit.
            for (int step=0; step<128 && budget>1e-6 && !f.returning; step++) {
                double distance = Math.min(.20,Math.min(budget,f.remaining));
                if (distance <= 1e-6) { f.returning=true; break; }
                Vec3 start=f.previous, end=start.add(f.direction.scale(distance));
                if (!level.getChunkSource().hasChunk(net.minecraft.core.BlockPos.containing(start).getX() >> 4, net.minecraft.core.BlockPos.containing(start).getZ() >> 4) || !level.getChunkSource().hasChunk(net.minecraft.core.BlockPos.containing(end).getX() >> 4, net.minecraft.core.BlockPos.containing(end).getZ() >> 4)) { f.returning=true; break; }
                var wall=level.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
                // Harvest the actual swept cells, never an endpoint beyond a solid wall.
                RingVegetation.clear(player,net.minecraft.core.BlockPos.containing(start));
                if (wall.getType()==HitResult.Type.MISS) RingVegetation.clear(player,net.minecraft.core.BlockPos.containing(end));
                else RingVegetation.clear(player,wall.getBlockPos());
                wall=level.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player) {
                    @Override public net.minecraft.world.phys.shapes.VoxelShape getBlockShape(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.BlockGetter world,net.minecraft.core.BlockPos pos) {
                        if (RingVegetation.preservedBase(player,pos,state)) return net.minecraft.world.phys.shapes.Shapes.empty();
                        return super.getBlockShape(state,world,pos);
                    }
                });
                if (wall.getType()==HitResult.Type.MISS) {
                    f.stuck=0; hitSegment(player,level,f,start,end,false); f.previous=end; budget-=distance; f.remaining-=distance;
                    if (f.remaining<=1e-6) f.returning=true;
                } else {
                    Vec3 impact=wall.getLocation();
                    if (start.distanceTo(impact)<.003 && ++f.stuck>=4) { f.returning=true; break; }
                    hitSegment(player,level,f,start,impact,false);
                    Vec3 normal=Vec3.atLowerCornerOf(wall.getDirection().getNormal());
                    f.previous=impact.add(normal.scale(.002));
                    Vec3 reflected=f.direction.subtract(normal.scale(2*f.direction.dot(normal))).normalize();
                    LivingEntity target=nearestEnemy(player,level,f.previous,f.reach,normal);
                    Vec3 aim=target==null?reflected:target.getBoundingBox().getCenter().subtract(f.previous).normalize();
                    // Avoid immediate re-entry into the same solid face; try other candidates next bounce.
                    f.direction=aim.dot(normal)>1e-5?aim:reflected;
                    f.outwardHits.clear(); f.remaining=f.reach + 3; budget-=Math.max(.002,start.distanceTo(impact));
                }
            }
        }
        f.visual.setPos(f.previous.x,f.previous.y,f.previous.z); orient(f, f.direction);
    }
    // Preserve the original flat spinning pose, then tilt that plane along travel.
    private static void orient(Flight f, Vec3 direction) {
        if (direction.lengthSqr() < 1e-12) return;
        double horizontal = Math.hypot(direction.x, direction.z);
        if (horizontal > 1e-8) f.heading = (float) Math.atan2(direction.x, direction.z);
        float pitch = (float) -Math.atan2(direction.y, horizontal);
        float spin = (f.age % 15) * (float) Math.toRadians(24.0);
        org.joml.Quaternionf rotation = new org.joml.Quaternionf()
                .rotateY(f.heading).rotateX(pitch).rotateY(spin)
                .rotateX((float) (Math.PI / 2.0));
        ((com.devaxo.templeoftheaxolotl.mixin.RingDisplayTransformInvoker) f.visual)
                .tota$setTransformation(new com.mojang.math.Transformation(
                        new org.joml.Vector3f(), rotation, new org.joml.Vector3f(1, 1, 1),
                        new org.joml.Quaternionf()));
    }
    private static void state(ServerPlayer p,boolean active) { net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p,new com.devaxo.templeoftheaxolotl.network.RingFlightStatePayload(active)); }
    private static boolean eligible(ServerPlayer p, LivingEntity e) {
        return e.isAlive() && e!=p && !e.isAlliedTo(p) && !p.isAlliedTo(e)
            && !(e instanceof net.minecraft.world.entity.TamableAnimal t && t.isTame())
            && !(e instanceof com.devaxo.templeoftheaxolotl.entity.WaifuAxoluthuEntity a && a.getBondedPlayer()!=null)
            && !(e instanceof net.minecraft.world.entity.player.Player other && (!p.canHarmPlayer(other) || (p.getTags().contains("in_temple") && other.getTags().contains("in_temple"))));
    }
    private static LivingEntity nearestEnemy(ServerPlayer p, ServerLevel l, Vec3 pos,double reach,Vec3 normal) {
        LivingEntity best=null; double bestDist=reach*reach;
        for (LivingEntity e:l.getEntitiesOfClass(LivingEntity.class,new AABB(pos,pos).inflate(reach),x->eligible(p,x) && (x instanceof net.minecraft.world.entity.monster.Enemy || (x instanceof net.minecraft.world.entity.Mob m && m.getTarget()==p)))) {
            double d=e.getBoundingBox().getCenter().distanceToSqr(pos);
            if(d<bestDist && e.getBoundingBox().getCenter().subtract(pos).dot(normal)>1e-5 && l.clip(new ClipContext(pos,e.getBoundingBox().getCenter(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()==HitResult.Type.MISS) {best=e;bestDist=d;}
        }
        return best;
    }
    private static void hitSegment(ServerPlayer player, ServerLevel level, Flight flight,
                                   Vec3 start, Vec3 end, boolean returning) {
        Set<UUID> hits = returning ? flight.returnHits : flight.outwardHits;
        double radius = 0.45;
        // Record a genuine departure from the target hitbox before permitting a return hit.
        // This check runs on both outbound and returning substeps.
        for (UUID targetId : flight.lastHit.keySet()) {
            var oldTarget = level.getEntity(targetId);
            if (oldTarget != null && !oldTarget.getBoundingBox().inflate(radius).contains(start)) {
                flight.exitedAfterOutbound.add(targetId);
            }
        }
        AABB area = new AABB(start, end).inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> eligible(player, entity))) {
            if (hits.contains(target.getUUID())) continue;
            if (!returning && level.getGameTime()-flight.lastHit.getOrDefault(target.getUUID(),-100L)<10) continue;
            if (returning && flight.lastHit.containsKey(target.getUUID())
                    && !flight.exitedAfterOutbound.contains(target.getUUID())) continue;
            // Intersect the swept centerline with an expanded target box.
            AABB targetBox = target.getBoundingBox().inflate(radius);
            if (targetBox.clip(start, end).isEmpty() && !targetBox.contains(start)) continue;
            HitResult wall = returning ? null : level.clip(new ClipContext(start, target.getBoundingBox().getCenter(),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (!returning && wall.getType() != HitResult.Type.MISS) continue;
            // Route through the real enchanted weapon attack pipeline. This invokes
            // vanilla damage/enchantment effects and our existing LivingDamageEvent
            // listeners (Event Horizon, Celestial Convergence, etc.) with the actual
            // Ring in the player's main hand. The ItemDisplay is visual only.
            //
            // IMPORTANT: Player.attack uses vanilla attack-strength cooldown and may
            // perform normal melee side effects. A future dedicated enchantment
            // pipeline is needed if independent per-target arc damage is required.
            float healthBefore = target.getHealth();
            player.attack(target);
            if (target.getHealth() < healthBefore || !target.isAlive()) {
                hits.add(target.getUUID()); flight.exitedAfterOutbound.remove(target.getUUID()); flight.lastHit.put(target.getUUID(),level.getGameTime());
            }
        }
    }

}
