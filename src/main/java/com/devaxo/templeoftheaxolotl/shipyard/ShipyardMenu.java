package com.devaxo.templeoftheaxolotl.shipyard;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ShipyardMenu extends AbstractContainerMenu {
    private static final int INPUT_COUNT = 81;
    private static final int PLAYER_START = 81;
    private static final int PLAYER_END = 117;

    private final Container contents;
    private final ShipyardCraneBlockEntity crane;
    private final DataSlot cargoState;
    private final BlockPos pos;
    private final boolean serverSide;

    public ShipyardMenu(int id, Inventory player, BlockPos pos) {
        this(id, player, new SimpleContainer(INPUT_COUNT), pos);
    }

    public ShipyardMenu(int id, Inventory player, Container contents, BlockPos pos) {
        super(ShipyardRegistries.CRANE_MENU.get(), id);
        this.contents = contents;
        this.pos = pos;
        this.serverSide = !player.player.level().isClientSide;
        this.crane = player.player.level().getBlockEntity(pos) instanceof ShipyardCraneBlockEntity be ? be : null;
        this.cargoState = new DataSlot() {
            private int clientValue;
            @Override public int get() { return serverSide ? (crane != null && crane.hasPendingCargo() ? 1 : 0) : clientValue; }
            @Override public void set(int value) { clientValue = value; }
        };
        addDataSlot(cargoState);
        checkContainerSize(contents, INPUT_COUNT);
        contents.startOpen(player.player);

        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                addSlot(new Slot(contents, x + y * 9, 12 + x * 18, 17 + y * 18) {
                    @Override public int getMaxStackSize() { return 1; }
                    @Override public int getMaxStackSize(ItemStack stack) { return 1; }
                });
            }
        }

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlot(new Slot(player, x + y * 9 + 9, 12 + x * 18, 196 + y * 18));
            }
        }
        for (int x = 0; x < 9; x++) {
            addSlot(new Slot(player, x, 12 + x * 18, 254));
        }
    }

    public boolean canCraft() {
        return cargoState.get() == 0 && ShipyardRecipes.match(contents) != null;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != 0 || !stillValid(player)) return false;
        if (player.level().isClientSide) return true;
        return crane != null && crane.startCraft();
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(pos) instanceof ShipyardCraneBlockEntity
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < INPUT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 0, INPUT_COUNT, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        contents.stopOpen(player);
    }
}
