package com.devaxo.templeoftheaxolotl.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

/** Non-destructive Starfall blast; combat modifiers read from the thrown Cosmic Trident stack. */
@net.neoforged.fml.common.EventBusSubscriber(modid = "temple_of_the_axolotl")
public final class StarfallBurst {
    private StarfallBurst() {}
    private static int level(ServerLevel world, ItemStack weapon, String enchantment) {
        var key = ResourceKey.create(Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath("minecraft", enchantment));
        var holder = world.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(key).orElse(null);
        if (holder == null || weapon.isEmpty()) return 0;
        return weapon.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(holder);
    }
    private record Pending(net.minecraft.resources.ResourceKey<Level> dimension, long due,
            Vec3 center, java.util.UUID ownerId, java.util.UUID primaryId, ItemStack weapon) {}
    private static final java.util.List<Pending> PENDING = new java.util.ArrayList<>();
    public static void doubleBurst(ServerLevel world, Vec3 center, Player owner, Entity primary, ItemStack weapon) {
        explode(world, center, owner, primary, weapon);
        PENDING.add(new Pending(world.dimension(), world.getGameTime() + 5,
                center, owner.getUUID(), primary == null ? null : primary.getUUID(), weapon.copy()));
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        var iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            Pending pending = iterator.next();
            ServerLevel world = event.getServer().getLevel(pending.dimension());
            if (world == null) { iterator.remove(); continue; }
            if (world.getGameTime() < pending.due()) continue;
            iterator.remove();
            Entity entity = world.getEntity(pending.ownerId());
            if (!(entity instanceof Player owner) || !owner.isAlive()) continue;
            Entity primary = pending.primaryId() == null ? null : world.getEntity(pending.primaryId());
            explode(world, pending.center(), owner, primary, pending.weapon());
        }
    }
    public static void explode(ServerLevel world, Vec3 center, Player owner, Entity primary, ItemStack weapon) {
        world.sendParticles(ParticleTypes.FIREWORK, center.x, center.y, center.z, 75, .8, .8, .8, .12);
        world.playSound(null, BlockPos.containing(center), SoundEvents.FIREWORK_ROCKET_BLAST,
                SoundSource.PLAYERS, 1.4f, 1.05f);
        int sharpness = level(world, weapon, "sharpness");
        int smite = level(world, weapon, "smite");
        int bane = level(world, weapon, "bane_of_arthropods");
        int impaling = level(world, weapon, "impaling");
        int fireAspect = level(world, weapon, "fire_aspect");
        int knockback = level(world, weapon, "knockback");
        for (Mob mob : world.getEntitiesOfClass(Mob.class, new AABB(center, center).inflate(5),
                m -> m.isAlive() && !m.isAlliedTo(owner)
                        && !owner.isAlliedTo(m) && !(m instanceof TamableAnimal t && t.isTame()))) {
            double distance = mob.getBoundingBox().getCenter().distanceTo(center);
            float damage = (float) (8.0 * Math.max(0, 1 - distance / 5.0));
            if (sharpness > 0) damage += .5f * sharpness + .5f;
            if (smite > 0 && mob.getType().is(EntityTypeTags.UNDEAD)) damage += 2.5f * smite;
            if (bane > 0 && mob.getType().is(EntityTypeTags.ARTHROPOD)) damage += 2.5f * bane;
            if (impaling > 0 && mob.getType().is(EntityTypeTags.SENSITIVE_TO_IMPALING)) damage += 2.5f * impaling;
            if (damage > 0 && mob.hurt(world.damageSources().playerAttack(owner), damage)) {
                if (fireAspect > 0) mob.igniteForSeconds(4 * fireAspect);
                if (knockback > 0) {
                    Vec3 away = mob.position().subtract(center);
                    if (away.horizontalDistanceSqr() > .001)
                        mob.knockback(.5 * knockback, -away.x, -away.z);
                }
            }
        }
    }
}
