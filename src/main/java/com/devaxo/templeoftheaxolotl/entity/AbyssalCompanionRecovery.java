package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** Preserve the owner registry across respawn and recover loaded pets at logout/death. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class AbyssalCompanionRecovery {
    private AbyssalCompanionRecovery() {}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) recallLoaded(player);
    }
    @SubscribeEvent public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) recallLoaded(player);
    }
    private static void recallLoaded(ServerPlayer owner) {
        for (var level : owner.server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof AbyssalAxolotlEntity axo) || !axo.isTame()
                        || !owner.getUUID().equals(axo.getOwnerUUID())) continue;
                AbyssalCompanionRegistry.recallLoaded(owner, axo);
            }
        }
    }
}
