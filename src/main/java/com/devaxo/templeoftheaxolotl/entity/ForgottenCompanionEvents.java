package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

/** The Forgotten: a peaceful, owner-bound midnight reckoning for unnamed lost tameables. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class ForgottenCompanionEvents {
    private static final String GHOST = "tota_forgotten_ghost";
    private static final String ORIGINAL = "tota_forgotten_original";
    private static final String OWNER = "tota_forgotten_owner";
    private static final long MIDNIGHT = 18000L;
    private static final int SPAWN_INTERVAL = 80;
    private static final int PARTICLE_INTERVAL = 10;
    private ForgottenCompanionEvents() {}

    @SubscribeEvent public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof TamableAnimal pet) || !pet.isTame()
                || pet.hasCustomName() || pet.getOwnerUUID() == null
                || !(pet.level() instanceof ServerLevel level)
                || pet.getPersistentData().getBoolean(GHOST)) return;
        // Unnamed pets have never been added to the named companion registry.
        // Do not create a second ghost if the death callback is invoked repeatedly.
        ForgottenCompanionData.get(level).remember(pet.getUUID(), pet.getOwnerUUID(),
                BuiltInRegistries.ENTITY_TYPE.getKey(pet.getType()).toString());
    }

    // If an old ghost's chunk loads after a newer manifestation, discard the stale copy.
    @SubscribeEvent public static void ghostLoaded(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Entity entity = event.getEntity();
        CompoundTag marker = entity.getPersistentData();
        if (!marker.getBoolean(GHOST)) return;
        if (!marker.hasUUID(ORIGINAL) || !marker.hasUUID(OWNER)) {
            event.setCanceled(true);
            return;
        }
        CompoundTag record = ForgottenCompanionData.get(level).byOriginal(marker.getUUID(ORIGINAL));
        if (record == null || "resolved".equals(record.getString("Stage"))
                || !record.hasUUID("Ghost") || !entity.getUUID().equals(record.getUUID("Ghost")))
            event.setCanceled(true);
    }

    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();
        if (overworld == null) return;
        long time = overworld.getDayTime() % 24000L;
        boolean night = time >= 13000 && time <= 23000;
        ForgottenCompanionData data = ForgottenCompanionData.get(overworld);
        boolean active = false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() != overworld) continue;
            Collection<CompoundTag> unresolved = data.forOwner(player.getUUID());
            if (unresolved.isEmpty()) continue;
            if (night) {
                active = true;
                if (server.getTickCount() % SPAWN_INTERVAL == 0)
                    for (CompoundTag record : unresolved) ensureGhost(overworld, player, record, data);
            }
        }
        // Hold midnight only while an affected owner is online in the Overworld.
        // Release naturally when everyone leaves or the final apology is accepted.
        if (active && time >= MIDNIGHT) overworld.setDayTime(
                overworld.getDayTime() - time + MIDNIGHT);

        if (server.getTickCount() % PARTICLE_INTERVAL == 0) {
            for (Entity entity : overworld.getAllEntities()) {
                if (!entity.getPersistentData().getBoolean(GHOST)) continue;
                CompoundTag ghost = entity.getPersistentData();
                if (!ghost.hasUUID(OWNER) || !ghost.hasUUID(ORIGINAL)) { entity.discard(); continue; }
                ServerPlayer owner = server.getPlayerList().getPlayer(ghost.getUUID(OWNER));
                CompoundTag record = data.byOriginal(ghost.getUUID(ORIGINAL));
                if (owner == null || owner.level() != overworld || !night || record == null
                        || "resolved".equals(record.getString("Stage"))) { entity.discard(); continue; }
                // The spectral silhouette remains close to its owner without pathfinding.
                if (entity.distanceToSqr(owner) > 256)
                    entity.teleportTo(owner.getX() + 2, owner.getY(), owner.getZ() + 2);
                overworld.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,
                        entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(),
                        2, 0.25, 0.35, 0.25, 0.005);
            }
        }
    }

    private static void ensureGhost(ServerLevel level, ServerPlayer owner, CompoundTag record,
                                    ForgottenCompanionData data) {
        if (record.hasUUID("Ghost")) {
            Entity existing = level.getEntity(record.getUUID("Ghost"));
            if (existing != null && existing.isAlive()) return;
        }
        EntityType<?> type = EntityType.byString(record.getString("Type")).orElse(null);
        if (type == null) return;
        Entity entity = type.create(level);
        if (!(entity instanceof TamableAnimal ghost)) return;
        ghost.setOwnerUUID(owner.getUUID());
        ghost.setInvulnerable(true);
        ghost.setNoAi(true);
        ghost.setSilent(true);
        ghost.setGlowingTag(true);
        ghost.setPersistenceRequired();
        ghost.setCustomName(Component.literal(record.getString("Stage").equals("apology")
                ? record.getString("Name") : "The Forgotten"));
        ghost.setCustomNameVisible(true);
        ghost.getPersistentData().putBoolean(GHOST, true);
        ghost.getPersistentData().putUUID(ORIGINAL, record.getUUID("Original"));
        ghost.getPersistentData().putUUID(OWNER, owner.getUUID());
        ghost.moveTo(owner.getX() + 2, owner.getY(), owner.getZ() + 2, owner.getYRot(), 0);
        if (level.addFreshEntity(ghost)) {
            record.putUUID("Ghost", ghost.getUUID());
            data.update(record);
            owner.sendSystemMessage(Component.literal("A forgotten companion has returned. It will not let midnight pass without a name and an apology."));
        }
    }

    @SubscribeEvent public static void nameGhost(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof TamableAnimal ghost)
                || !ghost.getPersistentData().getBoolean(GHOST)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        CompoundTag marker = ghost.getPersistentData();
        if (!marker.hasUUID(OWNER) || !player.getUUID().equals(marker.getUUID(OWNER))) return;
        ForgottenCompanionData data = ForgottenCompanionData.get(player.serverLevel());
        CompoundTag record = data.byOriginal(marker.getUUID(ORIGINAL));
        if (record == null || "resolved".equals(record.getString("Stage"))) return;
        if (!"waiting".equals(record.getString("Stage"))) {
            player.sendSystemMessage(Component.literal("Say: I'm sorry " + record.getString("Name") + ". You were the best"));
            return;
        }
        if (!event.getItemStack().is(Items.NAME_TAG) || !event.getItemStack().has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
            player.sendSystemMessage(Component.literal("The Forgotten needs a renamed Name Tag first."));
            return;
        }
        String name = event.getItemStack().getHoverName().getString().trim();
        if (name.isEmpty()) return;
        record.putString("Name", name);
        record.putString("Stage", "apology");
        data.update(record);
        ghost.setCustomName(Component.literal(name));
        if (!player.getAbilities().instabuild) event.getItemStack().shrink(1);
        player.sendSystemMessage(Component.literal("The ghost recognizes its name. Say: I'm sorry " + name + ". You were the best"));
    }

    @SubscribeEvent public static void apology(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        ForgottenCompanionData data = ForgottenCompanionData.get(player.serverLevel());
        String spoken = event.getRawText().trim().replaceAll("\\s+", " ");
        for (CompoundTag record : data.forOwner(player.getUUID())) {
            if (!"apology".equals(record.getString("Stage"))) continue;
            String expected = "I'm sorry " + record.getString("Name") + ". You were the best";
            if (!spoken.equalsIgnoreCase(expected)) continue;
            record.putString("Stage", "resolved");
            data.update(record);
            if (record.hasUUID("Ghost")) {
                for (ServerLevel level : player.server.getAllLevels()) {
                    Entity ghost = level.getEntity(record.getUUID("Ghost"));
                    if (ghost != null && ghost.getPersistentData().getBoolean(GHOST)) {
                        level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                                ghost.getX(), ghost.getY() + 0.5, ghost.getZ(), 24, 0.5, 0.5, 0.5, 0.04);
                        ghost.discard();
                    }
                }
            }
            player.sendSystemMessage(Component.literal(record.getString("Name") + " forgives you. The night feels a little warmer."));
            return;
        }
    }

    @SubscribeEvent public static void preventSleep(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        BlockState state = player.level().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof BedBlock)) return;
        if (ForgottenCompanionData.get(player.serverLevel()).forOwner(player.getUUID()).isEmpty()) return;
        long time = player.server.overworld().getDayTime() % 24000L;
        if (time < 13000 || time > 23000) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        player.sendSystemMessage(Component.literal("The Forgotten will not let you sleep until you make amends."));
    }
}
