package com.devaxo.templeoftheaxolotl.combat;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
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
    private static final Set<UUID> HELD = new HashSet<>();

    private RingBoomerangCombat() {}

    public static void setHeld(ServerPlayer player, boolean held) {
        UUID id = player.getUUID();
        if (held && !RingMiningMode.isMining(player) && player.getMainHandItem().is(ModItems.RING_OF_THE_ONE.get())) {
            HELD.add(id);
            LAST_INPUT.put(id, player.serverLevel().getGameTime());
        } else {
            HELD.remove(id);
            LAST_INPUT.remove(id);
        }
    }

    private static final class Flight {
        final Vec3 origin;
        // Initial player hand position; used to track a moving return target.
        final Vec3 launchHand;
        final Vec3 direction;
        final Vec3 side;
        final int handedness;
        final Display.ItemDisplay visual;
        final int duration;
        final Set<UUID> outwardHits = new HashSet<>();
        final Set<UUID> returnHits = new HashSet<>();
        int age = 0;
        int finishDelay = 0;
        Vec3 previous;

        Flight(ServerPlayer player, int duration) {
            this.origin = player.getEyePosition().add(0, -0.25, 0);
            this.launchHand = this.origin;
            // Preserve pitch: the entire ellipse points where the player aimed at launch.
            this.direction = player.getLookAngle().normalize();
            // World-up projected perpendicular to forward gives a stable local right axis.
            Vec3 right = new Vec3(-direction.z, 0, direction.x);
            this.side = right.lengthSqr() < 1.0e-8
                    ? new Vec3(1, 0, 0) : right.normalize();
            this.handedness = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
            this.duration = duration;
            this.visual = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, player.serverLevel());
            // Public NBT loading API: ItemDisplay setters are private in 1.21.1.
            CompoundTag tag = new CompoundTag();
            tag.put("item", player.getMainHandItem().copyWithCount(1).save(player.registryAccess()));
            tag.putString("item_display", "fixed");
            // Entity rotation (not a second local item transformation) lays the blade flat.
            // A one-tick teleport interpolation reduces trailing when the return curves.
            tag.putInt("teleport_duration", 1);
            this.visual.load(tag);
            this.visual.setXRot(90.0f);
            this.visual.setYRot(player.getYRot());
            this.visual.setCustomName(Component.literal("ring_boomerang:" + player.getUUID()));
            this.visual.setCustomNameVisible(false);
            this.visual.setPos(origin.x, origin.y, origin.z);
            player.serverLevel().addFreshEntity(this.visual);
            this.previous = origin;
        }
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID id = player.getUUID();
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();

        if (!player.isAlive() || !player.getMainHandItem().is(ModItems.RING_OF_THE_ONE.get()) || RingMiningMode.isMining(player)) {
            Flight old = ACTIVE.remove(id);
            if (old != null) old.visual.discard();
            HELD.remove(id);
            LAST_INPUT.remove(id);
            NEXT_THROW.remove(id);
            return;
        }

        if (now - LAST_INPUT.getOrDefault(id, -100L) > 15) HELD.remove(id);
        Flight flight = ACTIVE.get(id);
        if (flight == null) {
            if (!HELD.contains(id) || now < NEXT_THROW.getOrDefault(id, 0L)) return;
            double speed = Math.max(0.2, player.getAttributeValue(Attributes.ATTACK_SPEED));
            // An eight-tick round trip only has four ticks to draw the return.
            // Keep a meaningful visible return even with high attack-speed modifiers.
            int duration = Math.max(12, Math.min(60, (int) Math.ceil(96.0 / speed)));
            flight = new Flight(player, duration);
            ACTIVE.put(id, flight);
            NEXT_THROW.put(id, now + duration);
        }

        // Leave the visual alive briefly at the hand so the last network update can render.
        if (flight.age >= flight.duration) {
            if (++flight.finishDelay >= 3) {
                flight.visual.discard();
                ACTIVE.remove(id);
            }
            return;
        }

        flight.age++;
        double t = Math.min(1.0, (double) flight.age / flight.duration);
        Vec3 position = path(flight, t, player);
        flight.visual.setPos(position.x, position.y, position.z);
        // Spin the flat item around the world-up axis (the blade's normal when level).
        flight.visual.setYRot((float) (flight.age * 24.0));

        // Collision follows the actual swept blade path, not the filled ellipse.
        // Substeps avoid missing a target at the outward/return boundary.
        Vec3 previous = flight.previous;
        double priorT = (double) (flight.age - 1) / flight.duration;
        int steps = Math.max(2, (int) Math.ceil(previous.distanceTo(position) / 0.20));
        for (int i = 1; i <= steps; i++) {
            double f0 = (double) (i - 1) / steps;
            double f1 = (double) i / steps;
            double a = priorT + (t - priorT) * f0;
            double b = priorT + (t - priorT) * f1;
            // Interpolate the actual previous->current movement, including
            // any player movement, rather than re-sampling a stale ellipse.
            Vec3 segmentStart = previous.lerp(position, f0);
            Vec3 segmentEnd = previous.lerp(position, f1);
            if (a < 0.5 && b > 0.5) {
                double split = (0.5 - a) / (b - a);
                Vec3 apex = segmentStart.lerp(segmentEnd, split);
                hitSegment(player, level, flight, segmentStart, apex, false);
                hitSegment(player, level, flight, apex, segmentEnd, true);
            } else {
                hitSegment(player, level, flight, segmentStart, segmentEnd, a >= 0.5);
            }
        }
        flight.previous = position;
    }

    /**
     * Keep the launch ellipse untouched on the outbound pass. On return, smoothly
     * translate its endpoint toward the player's CURRENT hand position.
     *
     * The moving endpoint implicitly includes player velocity: changes in the
     * player's position from one tick to the next become corresponding changes
     * in the return curve. No arbitrary chase-speed multiplier is applied.
     */
    private static Vec3 path(Flight flight, double t, ServerPlayer player) {
        double theta = Math.PI * 2.0 * Math.max(0.0, Math.min(1.0, t));
        double forward = 2.5 * (1.0 - Math.cos(theta));
        double sideways = 0.9 * Math.sin(theta) * flight.handedness;
        Vec3 base = flight.origin.add(flight.direction.scale(forward))
                .add(flight.side.scale(sideways));
        if (t <= 0.5) return base;

        // 0 at the apex, 1 at the catch. Smoothstep preserves a continuous
        // position and avoids an immediate sideways jump at the apex.
        double u = Math.min(1.0, (t - 0.5) * 2.0);
        double follow = u * u * (3.0 - 2.0 * u);
        Vec3 currentHand = player.getEyePosition().add(0, -0.25, 0);
        Vec3 playerDisplacement = currentHand.subtract(flight.launchHand);
        return base.add(playerDisplacement.scale(follow));
    }

    private static void hitSegment(ServerPlayer player, ServerLevel level, Flight flight,
                                   Vec3 start, Vec3 end, boolean returning) {
        Set<UUID> hits = returning ? flight.returnHits : flight.outwardHits;
        double radius = 0.45;
        AABB area = new AABB(start, end).inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.isAlive() && entity != player && !entity.isAlliedTo(player))) {
            if (hits.contains(target.getUUID())) continue;
            // Intersect the swept centerline with an expanded target box.
            AABB targetBox = target.getBoundingBox().inflate(radius);
            if (targetBox.clip(start, end).isEmpty() && !targetBox.contains(start)) continue;
            HitResult wall = level.clip(new ClipContext(start, target.getBoundingBox().getCenter(),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (wall.getType() != HitResult.Type.MISS) continue;
            float damage = (float) Math.max(1.0, player.getAttributeValue(Attributes.ATTACK_DAMAGE));
            // Normal hurt cooldowns still apply; never bypass global invulnerability frames.
            if (target.hurt(player.damageSources().playerAttack(player), damage)) {
                hits.add(target.getUUID());
            }
        }
    }

}
