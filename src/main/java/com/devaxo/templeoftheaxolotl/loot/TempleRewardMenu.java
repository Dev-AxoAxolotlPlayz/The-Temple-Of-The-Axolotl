package com.devaxo.templeoftheaxolotl.loot;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Dedicated, reward-only display menu. No player inventory slots are exposed. */
public final class TempleRewardMenu extends AbstractContainerMenu {
    public static final int REWARD_SLOTS = 54;

    public TempleRewardMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, new net.minecraft.world.SimpleContainer(REWARD_SLOTS));
    }

    public TempleRewardMenu(int id, Inventory playerInventory, Container snapshot) {
        super(TempleRewardMenuTypes.REWARD_VAULT.get(), id);
        if (snapshot.getContainerSize() != REWARD_SLOTS) throw new IllegalArgumentException("Expected 54 reward slots");
        for (int i = 0; i < REWARD_SLOTS; i++) {
            int x = 8 + (i % 9) * 18;
            int y = 24 + (i / 9) * 18;
            addSlot(new Slot(snapshot, i, x, y) {
                @Override public boolean mayPickup(Player player) { return false; }
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public boolean allowModification(Player player) { return false; }
            });
        }
    }

    @Override public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Deliberately NO super call: rejects F, 1-9, shift-click, drag, throw,
        // pickup, double-click and every other slot mutation on BOTH sides.
        if (!player.level().isClientSide) broadcastFullState();
    }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return false; }
    @Override public boolean stillValid(Player player) { return true; }
}
