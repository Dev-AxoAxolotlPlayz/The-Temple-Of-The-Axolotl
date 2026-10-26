package com.devaxo.templeoftheaxolotl.compass;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

/** Nine vertically arranged dedicated slots plus a full player inventory. */
public final class CelestialModulesMenu extends AbstractContainerMenu {
    private final SimpleContainer modules = new SimpleContainer(9);
    private final SimpleContainer discs = new SimpleContainer(9);
    private final Inventory playerInventory;
    private final int[] control = new int[11];
    private boolean loadingSlots = false;
    public int playing() { return control[0] - 1; }
    public boolean paused() { return control[1] != 0; }
    public boolean shuffle() { return control[2] != 0; }
    public int repeat() { return control[3]; }
    public int nextUp() { return control[4] - 1; }
    public int magnetMode() { return control[5]; }
    public boolean hasCluckles() { return control[6] != 0; }
    public boolean inbound() { return control[8] != 0; }
    public boolean outbound() { return control[9] != 0; }
    public int radius() { return control[10]; }
    public boolean logisticsEnabled() { return control[7] != 0; }


    public CelestialModulesMenu(int id, Inventory playerInventory) {
        super(CelestialModulesMenuTypes.MODULES.get(), id);
        this.playerInventory = playerInventory;
        for (int i = 0; i < 9; i++) {
            final int index = i;
            addSlot(new Slot(modules, i, 45, 17 + i * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return TempleHudModules.accepts(index, stack); }
                @Override public int getMaxStackSize() { return 1; }
                @Override public void setChanged() {
                    super.setChanged();
                    if (CelestialModulesMenu.this.playerInventory.player instanceof ServerPlayer server)
                        TempleHudModules.storeSlot(server, index, getItem());
                }
            });
        }
        // Player inventory to the right, in vanilla 9-column order.
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 83 + col * 18, 104 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col, 83 + col * 18, 162));
        // Nine-slot jukebox rack appears to the LEFT of the module column.
        for (int i = 0; i < 9; i++) {
            final int index = i;
            addSlot(new Slot(discs, i, 19, 17 + i * 18) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return CelestialMusicRack.isDisc(stack) && modules.getItem(8).is(com.devaxo.templeoftheaxolotl.item.ModItems.CELESTIAL_JUKEBOX.get());
                }
                @Override public boolean mayPickup(Player player) { return modules.getItem(8).is(com.devaxo.templeoftheaxolotl.item.ModItems.CELESTIAL_JUKEBOX.get()); }
                @Override public boolean isActive() { return modules.getItem(8).is(com.devaxo.templeoftheaxolotl.item.ModItems.CELESTIAL_JUKEBOX.get()); }
                @Override public int getMaxStackSize() { return 1; }
                @Override public void setChanged() {
                    super.setChanged();
                    if (!loadingSlots && CelestialModulesMenu.this.playerInventory.player instanceof ServerPlayer server)
                        CelestialMusicRack.set(server, index, getItem());
                }
            });
        }
        for (int j = 0; j < control.length; j++) {
            final int k = j;
            addDataSlot(new DataSlot() {
                @Override public int get() { return control[k]; }
                @Override public void set(int value) { control[k] = value; }
            });
        }
        if (playerInventory.player instanceof ServerPlayer server) {
            loadingSlots = true;
            for (int i = 0; i < 9; i++) modules.setItem(i, TempleHudModules.getSlot(server, i));
            for (int i = 0; i < 9; i++) discs.setItem(i, CelestialMusicRack.get(server, i));
            loadingSlots = false;
            refresh(server);
        }
    }

    @Override public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (slotIndex < 9 || slotIndex >= 45) {
            if (!moveItemStackTo(original, 9, 45, true)) return ItemStack.EMPTY;
        } else {
            int target = -1;
            for (int i = 0; i < 9; i++)
                if (!slots.get(i).hasItem() && TempleHudModules.accepts(i, original)) { target = i; break; }
            if (target < 0) {
                if (CelestialMusicRack.isDisc(original) && modules.getItem(8).is(com.devaxo.templeoftheaxolotl.item.ModItems.CELESTIAL_JUKEBOX.get())) {
                    for (int i = 45; i < 54; i++) if (!slots.get(i).hasItem()) { target = i; break; }
                }
            }
            if (target < 0 || !moveItemStackTo(original, target, target + 1, false)) return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer server)
            for (int i = 0; i < 9; i++) TempleHudModules.storeSlot(server, i, modules.getItem(i));
        if (player instanceof ServerPlayer server)
            for (int i = 0; i < 9; i++) CelestialMusicRack.set(server, i, discs.getItem(i));
    }
    /** Keep the server playlist in sync with every visible rack slot, including slots 3-9. */
    private void syncDiscRack(ServerPlayer server) {
        if (loadingSlots) return;
        for (int i = 0; i < 9; i++) {
            ItemStack shown = discs.getItem(i);
            ItemStack stored = CelestialMusicRack.get(server, i);
            if (!ItemStack.matches(shown, stored))
                CelestialMusicRack.set(server, i, shown);
        }
    }

    private void refresh(ServerPlayer p) {
        control[0] = CelestialMusicRack.playing(p) + 1;
        control[1] = CelestialMusicRack.paused(p) ? 1 : 0;
        control[2] = CelestialMusicRack.shuffle(p) ? 1 : 0;
        control[3] = CelestialMusicRack.repeat(p);
        control[4] = CelestialMusicRack.nextUp(p) + 1;
        control[5] = com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.magnetMode(p);
        control[6] = com.devaxo.templeoftheaxolotl.entity.ClucklesRegistry.get(p.serverLevel()).has(p.getUUID()) ? 1 : 0;
        control[7] = com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.enabled(p) ? 1 : 0;
        control[8] = com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.inbound(p) ? 1 : 0;
        control[9] = com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.outbound(p) ? 1 : 0;
        control[10] = com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.radius(p);
        broadcastChanges();
    }
    @Override public void broadcastChanges() {
        if (playerInventory.player instanceof ServerPlayer server) {
            syncDiscRack(server);
            control[0] = CelestialMusicRack.playing(server) + 1;
            control[1] = CelestialMusicRack.paused(server) ? 1 : 0;
            control[2] = CelestialMusicRack.shuffle(server) ? 1 : 0;
            control[3] = CelestialMusicRack.repeat(server);
            control[4] = CelestialMusicRack.nextUp(server) + 1;
            control[5] = com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.magnetMode(server);
            control[6] = com.devaxo.templeoftheaxolotl.entity.ClucklesRegistry.get(server.serverLevel()).has(server.getUUID()) ? 1 : 0;
            control[7] = com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.enabled(server) ? 1 : 0;
            control[8] = com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.inbound(server) ? 1 : 0;
            control[9] = com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.outbound(server) ? 1 : 0;
            control[10] = com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.radius(server);
        }
        super.broadcastChanges();
    }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer server)) return false;
        syncDiscRack(server);
        if (id >= 0 && id < 9) CelestialMusicRack.toggle(server, id);
        else if (id == 10) CelestialMusicRack.pauseResume(server);
        else if (id == 11) CelestialMusicRack.toggleShuffle(server);
        else if (id == 12) CelestialMusicRack.cycleRepeat(server);
        else if (id == 13) CelestialMusicRack.advance(server, true);
        else if (id == 15) CelestialMusicRack.previous(server);
        else if (id == 14 && TempleHudModules.getSlot(server, 4).is(com.devaxo.templeoftheaxolotl.item.ModItems.CELESTIAL_MAGNET.get()))
            com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.cycleMagnet(server);
        else if (id >= 20 && id <= 22 && com.devaxo.templeoftheaxolotl.entity.ClucklesRegistry.get(server.serverLevel()).has(server.getUUID()) && com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.enabled(server)) {
            if (id == 20) com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.toggleInbound(server);
            if (id == 21) com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.toggleOutbound(server);
            if (id == 22) com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.cycleRadius(server);
        } else return false;
        refresh(server);
        return true;
    }
    public boolean jukeboxInstalled() { return modules.getItem(8).is(com.devaxo.templeoftheaxolotl.item.ModItems.CELESTIAL_JUKEBOX.get()); }
    @Override public boolean stillValid(Player player) { return true; }
}
