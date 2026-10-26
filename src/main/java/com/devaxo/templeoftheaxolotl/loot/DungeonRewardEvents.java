package com.devaxo.templeoftheaxolotl.loot;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class DungeonRewardEvents {
    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer oldPlayer
                && event.getEntity() instanceof ServerPlayer newPlayer) {
            DungeonRewardInventory.copyFrom(oldPlayer, newPlayer);
        }
    }
}
