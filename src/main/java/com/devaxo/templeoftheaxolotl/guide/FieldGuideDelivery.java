package com.devaxo.templeoftheaxolotl.guide;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.registry.ModBlocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class FieldGuideDelivery {
    private static final String KEY = "tota_field_guide_received";
    private static final String STARTER_KEY = "tota_baby_noodle_starter_received";
    private FieldGuideDelivery() {}
    @SubscribeEvent public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        event.getOriginal().revive();
        CompoundTag original = event.getOriginal().getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (original.getBoolean(KEY) || original.getBoolean(STARTER_KEY)) {
            CompoundTag next = event.getEntity().getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            if (original.getBoolean(KEY)) next.putBoolean(KEY, true);
            if (original.getBoolean(STARTER_KEY)) next.putBoolean(STARTER_KEY, true);
            event.getEntity().getPersistentData().put(Player.PERSISTED_NBT_TAG, next);
        }
    }
    @SubscribeEvent public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.getBoolean(KEY)) {
            ItemStack guide = new ItemStack(ModBlocks.FIELD_GUIDE_ITEM.get());
            if (!player.getInventory().add(guide)) player.drop(guide, false);
            persisted.putBoolean(KEY, true);
        }
        if (!persisted.getBoolean(STARTER_KEY)) {
            ItemStack starter = new ItemStack(com.devaxo.templeoftheaxolotl.item.ModItems.NOODLE_STARTER.get());
            if (!player.getInventory().add(starter)) player.drop(starter, false);
            persisted.putBoolean(STARTER_KEY, true);
        }
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }
}
