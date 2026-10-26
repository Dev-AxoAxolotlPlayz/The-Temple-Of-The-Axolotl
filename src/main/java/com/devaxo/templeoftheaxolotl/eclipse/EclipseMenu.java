package com.devaxo.templeoftheaxolotl.eclipse;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.DyeItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Stage 1: two real menu types, shared persistent offerings, synchronized raw XP bank. */
public final class EclipseMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final EclipseAltarEntity altar;
    private final boolean inscription;
    private int xpLow, xpHigh;
    private int costLow, costHigh;

    public EclipseMenu(int id, Inventory inventory, BlockPos pos, boolean inscription) {
        super(inscription ? EclipseMenus.INSCRIPTION.get() : EclipseMenus.TRANSMUTATION.get(), id);
        this.pos = pos;
        this.inscription = inscription;
        this.altar = inventory.player.level().getBlockEntity(pos) instanceof EclipseAltarEntity a ? a : null;
        var container = altar != null ? altar.offerings : new SimpleContainer(6);
        // 0 item I, 1 item II, 2 diamond/dye, 3 style, 4 feather, 5 output.
        // Inactive slots are positioned offscreen and refuse interaction.
        addSlot(new Slot(container, 0, inscription ? 120 : 62, inscription ? 71 : 62));
        addSlot(new Slot(container, 1, inscription ? -1000 : 178, inscription ? -1000 : 62) {
            @Override public boolean mayPlace(ItemStack stack) { return !inscription; }
            @Override public boolean mayPickup(Player player) { return !inscription; }
        });
        addSlot(new Slot(container, 2, inscription ? 62 : 120, inscription ? 112 : 105) {
            @Override public boolean mayPlace(ItemStack stack) {
                return inscription ? stack.getItem() instanceof net.minecraft.world.item.DyeItem : stack.is(Items.DIAMOND_BLOCK);
            }
        });
        addSlot(new Slot(container, 3, inscription ? 120 : -1000, inscription ? 112 : -1000) {
            @Override public boolean mayPlace(ItemStack stack) { return inscription && (stack.is(Items.GLOW_INK_SAC) || stack.is(Items.BLUE_ORCHID) || stack.is(Items.STRING) || stack.is(Items.IRON_SWORD) || stack.is(Items.SKELETON_SKULL) || stack.is(Items.WITHER_SKELETON_SKULL) || stack.is(Items.ZOMBIE_HEAD) || stack.is(Items.CREEPER_HEAD) || stack.is(Items.DRAGON_HEAD) || stack.is(Items.PIGLIN_HEAD) || stack.is(Items.PLAYER_HEAD)); }
            @Override public boolean mayPickup(Player player) { return inscription; }
        });
        addSlot(new Slot(container, 4, inscription ? 178 : -1000, inscription ? 112 : -1000) {
            @Override public boolean mayPlace(ItemStack stack) { return inscription && stack.is(Items.FEATHER); }
            @Override public boolean mayPickup(Player player) { return inscription; }
        });
        addSlot(new Slot(container, 5, inscription ? -1000 : 120, inscription ? -1000 : 62) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return !inscription && hasItem(); }
        });
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++)
            addSlot(new Slot(inventory, 9 + r * 9 + c, 47 + c * 18, 151 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inventory, c, 47 + c * 18, 209));
        addDataSlot(new DataSlot() {
            @Override public int get() { return altar != null ? altar.getXpPoints() & 0xffff : xpLow; }
            @Override public void set(int v) { xpLow = v & 0xffff; }
        });
        addDataSlot(new DataSlot() {
            @Override public int get() { return altar != null ? (altar.getXpPoints() >>> 16) & 0x7fff : xpHigh; }
            @Override public void set(int v) { xpHigh = v & 0x7fff; }
        });
        addDataSlot(new DataSlot() {
            @Override public int get() { return altar != null && !inventory.player.level().isClientSide ? ritualCost() & 0xffff : costLow; }
            @Override public void set(int v) { costLow = v & 0xffff; }
        });
        addDataSlot(new DataSlot() {
            @Override public int get() { return altar != null && !inventory.player.level().isClientSide ? (ritualCost() >>> 16) & 0x7fff : costHigh; }
            @Override public void set(int v) { costHigh = v & 0x7fff; }
        });
    }
    public int displayedCost() { return (costHigh << 16) | costLow; }

    /** Stage 2: enchantment merging; other transmutation recipes follow in stage 3. */
    private ItemStack enchantResult() {
        if (altar == null) return ItemStack.EMPTY;
        ItemStack left = altar.offerings.getItem(0);
        ItemStack right = altar.offerings.getItem(1);
        if (left.isEmpty() || right.isEmpty()) return ItemStack.EMPTY;
        ItemEnchantments source = EnchantmentHelper.getEnchantmentsForCrafting(right);
        if (source.isEmpty()) return ItemStack.EMPTY;
        ItemStack output = left.copy();
        output.setCount(1);
        ItemEnchantments.Mutable merged = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(output));
        boolean changed = false;
        for (Holder<Enchantment> enchantment : source.keySet()) {
            int existing = merged.getLevel(enchantment);
            int incoming = source.getLevel(enchantment);
            int max = enchantment.value().getMaxLevel() + 1;
            int next = Math.min(max, existing == incoming ? incoming + 1 : Math.max(existing, incoming));
            if (next <= existing) continue;
            boolean fortune = enchantment.is(Enchantments.FORTUNE);
            boolean silk = enchantment.is(Enchantments.SILK_TOUCH);
            boolean opposite = merged.keySet().stream().anyMatch(h ->
                    (fortune && h.is(Enchantments.SILK_TOUCH)) || (silk && h.is(Enchantments.FORTUNE)));
            if (opposite) continue;
            merged.set(enchantment, next);
            changed = true;
        }
        if (!changed) return ItemStack.EMPTY;
        EnchantmentHelper.setEnchantments(output, merged.toImmutable());
        return output;
    }

    private int ritualCost() {
        if (inscription || enchantResult().isEmpty()) return 0;
        ItemStack left = altar.offerings.getItem(0);
        ItemStack right = altar.offerings.getItem(1);
        ItemEnchantments incoming = EnchantmentHelper.getEnchantmentsForCrafting(right);
        ItemEnchantments current = EnchantmentHelper.getEnchantmentsForCrafting(left);
        long levels = 0;
        for (Holder<Enchantment> enchantment : incoming.keySet()) {
            int old = current.getLevel(enchantment);
            int in = incoming.getLevel(enchantment);
            int next = Math.min(enchantment.value().getMaxLevel() + 1, old == in ? in + 1 : Math.max(old, in));
            if (next <= old) continue;
            boolean fortune = enchantment.is(Enchantments.FORTUNE);
            boolean silk = enchantment.is(Enchantments.SILK_TOUCH);
            boolean opposite = current.keySet().stream().anyMatch(h ->
                    (fortune && h.is(Enchantments.SILK_TOUCH)) || (silk && h.is(Enchantments.FORTUNE)));
            if (!opposite) levels += (long) enchantment.value().getAnvilCost() * next;
        }
        // Incorporate vanilla prior-work penalties (already measured in levels).
        levels += (long) left.getOrDefault(net.minecraft.core.component.DataComponents.REPAIR_COST, 0)
                + right.getOrDefault(net.minecraft.core.component.DataComponents.REPAIR_COST, 0);
        levels = Math.max(1L, levels);
        // Minecraft cumulative XP from level zero; 50% raw-point discount rounded up.
        long raw = levels <= 16 ? levels * levels + 6 * levels
                : levels <= 31 ? (5 * levels * levels - 81 * levels + 720) / 2
                : (9 * levels * levels - 325 * levels + 4440) / 2;
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE, (raw + 1) / 2));
    }

    /** Server-side inscription: the client only supplies text and font choice. */
    public boolean inscribe(ServerPlayer player, String requestedName, boolean uniformFont) {
        if (!inscription || altar == null || !stillValid(player) || requestedName == null) return false;
        String name = requestedName.strip();
        if (name.isEmpty() || name.length() > 50 || name.chars().anyMatch(c -> Character.isISOControl((char)c))) return false;
        ItemStack item = altar.offerings.getItem(0);
        ItemStack dyeStack = altar.offerings.getItem(2);
        ItemStack styleStack = altar.offerings.getItem(3);
        ItemStack feather = altar.offerings.getItem(4);
        // Require a single item so renaming never changes an entire stack for one offering.
        if (item.isEmpty() || item.getCount() != 1 || !feather.is(Items.FEATHER)) return false;
        if (!dyeStack.isEmpty() && !(dyeStack.getItem() instanceof DyeItem)) return false;
        boolean bold = styleStack.is(Items.GLOW_INK_SAC);
        boolean italic = styleStack.is(Items.BLUE_ORCHID);
        boolean underline = styleStack.is(Items.STRING);
        boolean strike = styleStack.is(Items.IRON_SWORD);
        boolean obfuscated = styleStack.is(Items.SKELETON_SKULL) || styleStack.is(Items.WITHER_SKELETON_SKULL)
                || styleStack.is(Items.ZOMBIE_HEAD) || styleStack.is(Items.CREEPER_HEAD)
                || styleStack.is(Items.DRAGON_HEAD) || styleStack.is(Items.PIGLIN_HEAD)
                || styleStack.is(Items.PLAYER_HEAD);
        if (!styleStack.isEmpty() && !(bold || italic || underline || strike || obfuscated)) return false;
        // One level for naming, plus one for each optional offering; 50% off raw XP points.
        int levels = 1 + (!dyeStack.isEmpty() ? 1 : 0) + (!styleStack.isEmpty() ? 1 : 0);
        int raw = levels * levels + 6 * levels; // Levels 1..3, cumulative vanilla XP.
        int price = Math.max(1, (raw + 1) / 2);
        if (altar.getXpPoints() < price) return false;
        var style = net.minecraft.network.chat.Style.EMPTY.withItalic(italic).withBold(bold)
                .withUnderlined(underline).withStrikethrough(strike).withObfuscated(obfuscated);
        if (dyeStack.getItem() instanceof DyeItem dye) style = style.withColor(dye.getDyeColor().getTextColor());
        if (uniformFont) style = style.withFont(ResourceLocation.withDefaultNamespace("uniform"));
        ItemStack result = item.copy();
        result.set(DataComponents.CUSTOM_NAME, Component.literal(name).setStyle(style));
        // Commit only after all validation, cost checks, and result creation succeed.
        altar.offerings.setItem(0, result);
        if (!dyeStack.isEmpty()) altar.offerings.removeItem(2, 1);
        if (!styleStack.isEmpty()) altar.offerings.removeItem(3, 1);
        altar.offerings.removeItem(4, 1);
        altar.setXpPoints(altar.getXpPoints() - price);
        altar.setChanged();
        broadcastChanges();
        return true;
    }

    public boolean isInscription() { return inscription; }
    public int bankXp() { return altar != null && !altar.getLevel().isClientSide ? altar.getXpPoints() : (xpHigh << 16) | xpLow; }

    @Override public boolean stillValid(Player player) {
        return altar != null && !altar.isRemoved() && player.distanceToSqr(
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer server) || altar == null || !stillValid(player)) return false;
        if (id == 0 || id == 1) {
            boolean targetInscription = id == 1;
            if (targetInscription != inscription) EclipseAltarBlock.open(server, pos, targetInscription);
            return true;
        }
        if (id == 2) { // Deposit up to 100 raw XP points.
            int amount = Math.min(100, Math.min(player.totalExperience, Integer.MAX_VALUE - altar.getXpPoints()));
            if (amount > 0) { player.giveExperiencePoints(-amount); altar.setXpPoints(altar.getXpPoints() + amount); }
            return true;
        }
        if (id == 3) { // Withdraw up to 100 raw XP points.
            int amount = Math.min(100, altar.getXpPoints());
            if (amount > 0) { altar.setXpPoints(altar.getXpPoints() - amount); player.giveExperiencePoints(amount); }
            return true;
        }
        if (id == 4 && !inscription) {
            ItemStack result = enchantResult();
            int price = ritualCost();
            if (result.isEmpty() || price <= 0 || altar.getXpPoints() < price
                    || !altar.offerings.getItem(2).is(Items.DIAMOND_BLOCK)
                    || !altar.offerings.getItem(5).isEmpty()) return false;
            altar.offerings.setItem(5, result.copy());
            altar.setXpPoints(altar.getXpPoints() - price);
            altar.offerings.removeItem(0, 1);
            altar.offerings.removeItem(1, 1);
            altar.offerings.removeItem(2, 1); // One diamond block per successful ritual.
            altar.setChanged();
            broadcastChanges();
            return true;
        }
        return false;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return result;
        ItemStack source = slot.getItem();
        result = source.copy();
        if (index < 6) {
            if ((inscription && (index == 1 || index == 5)) || (!inscription && (index == 3 || index == 4))) return ItemStack.EMPTY;
            if (!moveItemStackTo(source, 6, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // Deliberately avoid guessing offering slot when shift-clicking.
            return ItemStack.EMPTY;
        }
        if (source.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }
}
