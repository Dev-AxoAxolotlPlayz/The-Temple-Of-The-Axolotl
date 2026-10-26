package com.devaxo.templeoftheaxolotl.repository;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.Container;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.TransientCraftingContainer;

/** Bulk counts live only in the persistent repository, never in client ItemStacks. */
public final class CelestialRepositoryMenu extends AbstractContainerMenu {
    private final SimpleContainer display = new SimpleContainer(CelestialRepository.TYPES);
    private final int[] counts = new int[CelestialRepository.TYPES];
    private final Inventory inventory;
    // Server-authoritative embedded workstation inventories.
    private final CraftingContainer craft = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer craftResult = new ResultContainer();
    private final SimpleContainer repair = new SimpleContainer(1);
    private final SimpleContainer repairResult = new SimpleContainer(1);
    private boolean updatingCraft = false;
    private boolean updatingRepair = false;
    private int craftEnabled = 0, repairEnabled = 0;
    public boolean hasCrafting() { return craftEnabled != 0; }
    public boolean hasAnvil() { return repairEnabled != 0; }
    private boolean hasModule(ServerPlayer p, int slot, net.minecraft.world.item.Item item) {
        return com.devaxo.templeoftheaxolotl.compass.TempleHudModules.getSlot(p, slot).is(item);
    }

    private int logisticsEquipped = 0, logisticsInbound = 1, logisticsOutbound = 0;
    private int logisticsRadius = 16, magnetMode = 0, logisticsPanel = 0;
    public boolean hasLogistics() { return logisticsEquipped != 0; }
    public boolean logisticsPanel() { return logisticsPanel != 0; }
    public boolean logisticsInbound() { return logisticsInbound != 0; }
    public boolean logisticsOutbound() { return logisticsOutbound != 0; }
    public int logisticsRadius() { return logisticsRadius; }
    public int magnetMode() { return magnetMode; }
    private int activePage = 0;
    private int moduleEquipped = 0;
    public int activePage() { return activePage; }
    public boolean hasSecondPage() { return moduleEquipped != 0; }
    private boolean equipped(ServerPlayer player) {
        return com.devaxo.templeoftheaxolotl.compass.TempleHudModules.getSlot(player, 3)
            .is(com.devaxo.templeoftheaxolotl.item.ModItems.CELESTIAL_REPOSITORY.get());
    }

    public static final int PLAYER_X = 155;

    public CelestialRepositoryMenu(int id, Inventory inventory) {
        super(CelestialRepositoryMenuTypes.REPOSITORY.get(), id);
        this.inventory = inventory;
        for (int i = 0; i < CelestialRepository.TYPES; i++) {
            final int index = i;
            addSlot(new Slot(display, i, 8 + (i % 7) * 18, 26 + (i / 7) * 18) {
                @Override public boolean mayPickup(Player player) { return false; }
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public boolean allowModification(Player player) { return false; }
            });
            addDataSlot(new DataSlot() {
                @Override public int get() { return counts[index]; }
                @Override public void set(int value) { counts[index] = value; }
            });
        }
        addDataSlot(new DataSlot() {
            @Override public int get() { return activePage; }
            @Override public void set(int value) { activePage = value; }
        });
        addDataSlot(new DataSlot() {
            @Override public int get() { return moduleEquipped; }
            @Override public void set(int value) { moduleEquipped = value; }
        });
        addDataSlot(new DataSlot() {
            @Override public int get() { return craftEnabled; }
            @Override public void set(int v) { craftEnabled = v; }
        });
        addDataSlot(new DataSlot() {
            @Override public int get() { return repairEnabled; }
            @Override public void set(int v) { repairEnabled = v; }
        });
        for (int field = 0; field < 6; field++) {
            final int index = field;
            addDataSlot(new DataSlot() {
                @Override public int get() { return switch (index) {
                    case 0 -> logisticsEquipped; case 1 -> logisticsInbound;
                    case 2 -> logisticsOutbound; case 3 -> logisticsRadius;
                    case 4 -> magnetMode; default -> logisticsPanel;
                }; }
                @Override public void set(int v) { switch (index) {
                    case 0 -> logisticsEquipped = v; case 1 -> logisticsInbound = v;
                    case 2 -> logisticsOutbound = v; case 3 -> logisticsRadius = v;
                    case 4 -> magnetMode = v; default -> logisticsPanel = v;
                } }
            });
        }
        // Workstations occupy the empty upper-right region.
        for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++)
            addSlot(new Slot(craft, c + r * 3, 159 + c * 18, 27 + r * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return craftEnabled != 0; }
            });
        addSlot(new Slot(craftResult, 0, 229, 45) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return craftEnabled != 0 && super.mayPickup(player); }
            @Override public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                if (player instanceof ServerPlayer server) {
                    var input = craft.asCraftInput();
                    server.serverLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, server.serverLevel()).ifPresent(holder -> {
                        var remaining = holder.value().getRemainingItems(input);
                        for (int i = 0; i < 9; i++) {
                            ItemStack ingredient = craft.getItem(i);
                            if (!ingredient.isEmpty()) craft.removeItem(i, 1);
                            ItemStack leftover = remaining.get(i);
                            if (!leftover.isEmpty()) {
                                ItemStack current = craft.getItem(i);
                                if (current.isEmpty()) craft.setItem(i, leftover.copy());
                                else if (ItemStack.isSameItemSameComponents(current, leftover)) current.grow(leftover.getCount());
                                else if (!server.getInventory().add(leftover.copy())) server.drop(leftover.copy(), false);
                            }
                        }
                    });
                    updateCraft(server);
                }
            }
        });
        addSlot(new Slot(repair, 0, 277, 29) {
            @Override public boolean mayPlace(ItemStack stack) { return repairEnabled != 0 && stack.isDamageableItem(); }
        });
        addSlot(new Slot(repairResult, 0, 277, 65) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return repairEnabled != 0 && super.mayPickup(player); }
            @Override public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                if (player instanceof ServerPlayer server) {
                    ItemStack original = repair.getItem(0);
                    if (!original.isEmpty() && original.isDamageableItem()) {
                        int cost = Math.max(1, original.getDamageValue() - stack.getDamageValue());
                        if (hasMending(server, original)) cost = (cost + 1) / 2;
                        int available = server.totalExperience;
                        if (!server.getAbilities().instabuild) server.giveExperiencePoints(-Math.min(cost, available));
                        repair.setItem(0, ItemStack.EMPTY);
                        repairResult.setItem(0, ItemStack.EMPTY);
                    }
                }
            }
        });
        // Full 3x9 inventory and hotbar beside the 7x8 bulk repository.
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, PLAYER_X + col * 18, 104 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col, PLAYER_X + col * 18, 162));
        if (inventory.player instanceof ServerPlayer server) refresh(server);
    }

    private void refresh(ServerPlayer player) {
        moduleEquipped = equipped(player) ? 1 : 0;
        logisticsEquipped = hasModule(player, 7, com.devaxo.templeoftheaxolotl.item.ModItems.LOGISTICS_MAESTRO.get()) ? 1 : 0;
        logisticsInbound = com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.inbound(player) ? 1 : 0;
        logisticsOutbound = com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.outbound(player) ? 1 : 0;
        logisticsRadius = com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.radius(player);
        magnetMode = com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.magnetMode(player);
        craftEnabled = hasModule(player, 5, Items.CRAFTING_TABLE) ? 1 : 0;
        repairEnabled = hasModule(player, 6, com.devaxo.templeoftheaxolotl.item.ModItems.CELESTIAL_ANVIL.get()) ? 1 : 0;
        updateCraft(player);
        updateRepair(player);
        if (moduleEquipped == 0) activePage = 0;
        var entries = activePage == 1 ? CelestialRepository.readSecond(player) : CelestialRepository.read(player);
        for (int i = 0; i < CelestialRepository.TYPES; i++) {
            if (i < entries.size()) {
                var entry = entries.get(i);
                display.setItem(i, entry.sample().copy());
                counts[i] = entry.quantity();
            } else {
                display.setItem(i, ItemStack.EMPTY);
                counts[i] = 0;
            }
        }
        broadcastFullState();
    }

    private static boolean hasMending(ServerPlayer player, ItemStack stack) {
        var holder = player.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
            .getOrThrow(Enchantments.MENDING);
        return EnchantmentHelper.getItemEnchantmentLevel(holder, stack) > 0;
    }
    private void updateCraft(ServerPlayer player) {
        if (updatingCraft) return;
        updatingCraft = true;
        ItemStack output = ItemStack.EMPTY;
        if (craftEnabled != 0) {
            var input = craft.asCraftInput();
            var match = player.serverLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, player.serverLevel());
            if (match.isPresent()) output = match.get().value().assemble(input, player.registryAccess());
        }
        craftResult.setItem(0, output);
        updatingCraft = false;
    }
    private void updateRepair(ServerPlayer player) {
        if (updatingRepair) return;
        updatingRepair = true;
        ItemStack input = repair.getItem(0);
        ItemStack output = ItemStack.EMPTY;
        if (repairEnabled != 0 && input.isDamageableItem() && input.isDamaged()) {
            int available = player.getAbilities().instabuild ? Integer.MAX_VALUE : player.totalExperience;
            int durability = hasMending(player, input) ? available * 2 : available;
            if (durability > 0) {
                output = input.copy();
                output.setDamageValue(Math.max(0, input.getDamageValue() - durability));
            }
        }
        repairResult.setItem(0, output);
        updatingRepair = false;
    }
    @Override public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (inventory != null && inventory.player instanceof ServerPlayer player) {
            if (container == craft) updateCraft(player);
            if (container == repair) updateRepair(player);
        }
    }
    @Override public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer server) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = craft.removeItemNoUpdate(i);
                if (!stack.isEmpty() && !server.getInventory().add(stack)) server.drop(stack, false);
            }
            ItemStack stack = repair.removeItemNoUpdate(0);
            if (!stack.isEmpty() && !server.getInventory().add(stack)) server.drop(stack, false);
        }
    }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer server)) return false;
        if (id == 1 && equipped(server)) { activePage = 1 - activePage; refresh(server); return true; }
        if (id == 2 && logisticsEquipped != 0) { logisticsPanel = 1 - logisticsPanel; refresh(server); return true; }
        if (id == 3 && logisticsEquipped != 0) { com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.toggleInbound(server); refresh(server); return true; }
        if (id == 4 && logisticsEquipped != 0) { com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.toggleOutbound(server); refresh(server); return true; }
        if (id == 5 && logisticsEquipped != 0) { com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.cycleRadius(server); refresh(server); return true; }
        if (id == 6) { com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.cycleMagnet(server); refresh(server); return true; }
        return false;
    }

    public int quantity(int index) { return counts[index]; }

    @Override public void clicked(int slotId, int button, ClickType type, Player player) {
        if (!(player instanceof ServerPlayer server)) return;
        if (slotId >= CelestialRepository.TYPES && slotId < CelestialRepository.TYPES + 12) {
            if (slotId < CelestialRepository.TYPES + 10 && craftEnabled == 0) return;
            if (slotId >= CelestialRepository.TYPES + 10 && repairEnabled == 0) return;
            super.clicked(slotId, button, type, player);
            updateCraft(server);
            updateRepair(server);
            return;
        }
        if (slotId >= CelestialRepository.TYPES + 12 && slotId < slots.size()) {
            // Normal player slots remain vanilla-clickable. Shift-click deposits safely.
            if (type == ClickType.QUICK_MOVE) {
                Slot slot = slots.get(slotId);
                if (slot.hasItem()) {
                    ItemStack stack = slot.getItem();
                    int moved = (activePage == 1 ? CelestialRepository.insertSecond(server, stack) : CelestialRepository.insert(server, stack));
                    if (moved > 0) { stack.shrink(moved); slot.setChanged(); }
                }
                refresh(server);
                return;
            }
            super.clicked(slotId, button, type, player);
            return;
        }
        if (slotId < 0 || slotId >= CelestialRepository.TYPES) {
            super.clicked(slotId, button, type, player);
            return;
        }
        if (type == ClickType.PICKUP && button == 1) {
            // Right-click repository entry: deposit main-hand stack.
            ItemStack hand = server.getMainHandItem();
            int moved = (activePage == 1 ? CelestialRepository.insertSecond(server, hand) : CelestialRepository.insert(server, hand));
            if (moved > 0) hand.shrink(moved);
        } else if ((type == ClickType.PICKUP && button == 0) || type == ClickType.QUICK_MOVE) {
            // Withdraw one native stack, or as much as inventory can hold.
            ItemStack withdrawn = (activePage == 1 ? CelestialRepository.extractSecond(server, slotId, 64) : CelestialRepository.extract(server, slotId, 64));
            if (!withdrawn.isEmpty()) {
                server.getInventory().add(withdrawn);
                if (!withdrawn.isEmpty()) server.drop(withdrawn, false);
            }
        }
        refresh(server);
    }

    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return false; }
    @Override public boolean stillValid(Player player) { return true; }
}
