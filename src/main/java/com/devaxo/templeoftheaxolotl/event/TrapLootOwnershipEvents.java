package com.devaxo.templeoftheaxolotl.event;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/** Server-enforced loot attribution, including vanilla drops and the Sentinel reward. */
@EventBusSubscriber(modid=TempleOfTheAxolotl.MODID)
public final class TrapLootOwnershipEvents {
    private TrapLootOwnershipEvents() {}
    public static boolean canCollect(ServerPlayer player, ItemEntity item) {
        var data=item.getPersistentData();
        return !data.hasUUID("tota_trap_loot_owner") ||
                player.getUUID().equals(data.getUUID("tota_trap_loot_owner"));
    }
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!event.getEntity().getPersistentData().getBoolean("tota_sentinel_loot_protected")) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            event.getDrops().clear();
            return;
        }
        for (ItemEntity drop:event.getDrops())
            drop.getPersistentData().putUUID("tota_trap_loot_owner",killer.getUUID());
    }
    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        if (event.getPlayer() instanceof ServerPlayer player && !canCollect(player,event.getItemEntity()))
            event.setCanPickup(net.neoforged.neoforge.common.util.TriState.FALSE);
    }
}
