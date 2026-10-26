package com.devaxo.templeoftheaxolotl.loot;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;

/** Snapshot only: server-owned staging is never exposed as a mutable container. */
public final class TempleRewardMenus {
    private TempleRewardMenus() {}
    public static void open(ServerPlayer player) {
        SimpleContainer preview = new SimpleContainer(DungeonRewardInventory.SLOTS);
        var stacks = DungeonRewardInventory.read(player);
        for (int i = 0; i < stacks.size(); i++) preview.setItem(i, stacks.get(i).copy());
        player.openMenu(new SimpleMenuProvider((id, inv, who) ->
                new TempleRewardMenu(id, inv, preview), Component.literal("Temple Reward Vault")));
    }
}
