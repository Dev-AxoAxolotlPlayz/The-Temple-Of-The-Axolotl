package com.devaxo.templeoftheaxolotl.repository;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Patch 08-D1: persistent, server-authoritative bulk storage.
 * A stored entry is one unique item+component variant, with a long-independent
 * integer quantity up to 32 native stacks. Stored ItemStack is a ONE-item
 * prototype so vanilla stack serialization never truncates bulk counts.
 */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class CelestialRepository {
    public static final int TYPES = 56;
    public static final int STACK_MULTIPLIER = 32;
    private static final String KEY = "tota_celestial_repository_v1";
    private CelestialRepository() {}

    public record Entry(ItemStack sample, int quantity) {
        public int capacity() { return sample.getMaxStackSize() * STACK_MULTIPLIER; }
    }

    public static java.util.List<Entry> read(ServerPlayer player) {
        java.util.ArrayList<Entry> result = new java.util.ArrayList<>();
        ListTag list = player.getPersistentData().getList(KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && result.size() < TYPES; i++) {
            CompoundTag entry = list.getCompound(i);
            ItemStack sample = ItemStack.parseOptional(player.registryAccess(), entry.getCompound("item"));
            if (sample.isEmpty()) continue;
            sample.setCount(1);
            int quantity = entry.getInt("quantity");
            if (quantity > 0) result.add(new Entry(sample, Math.min(quantity, sample.getMaxStackSize() * STACK_MULTIPLIER)));
        }
        return result;
    }

    private static void write(ServerPlayer player, java.util.List<Entry> entries) {
        ListTag list = new ListTag();
        for (Entry entry : entries) {
            if (entry.quantity() <= 0 || entry.sample().isEmpty()) continue;
            CompoundTag tag = new CompoundTag();
            ItemStack sample = entry.sample().copy();
            sample.setCount(1);
            tag.put("item", sample.save(player.registryAccess()));
            tag.putInt("quantity", entry.quantity());
            list.add(tag);
        }
        player.getPersistentData().put(KEY, list);
    }

    // Page two uses a separate persistent list; the menu selects the active key.
    private static final String SECOND_KEY = "tota_celestial_repository_page_two_prototype";
    public static java.util.List<Entry> readSecond(ServerPlayer player) {
        java.util.ArrayList<Entry> result = new java.util.ArrayList<>();
        ListTag list = player.getPersistentData().getList(SECOND_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && result.size() < TYPES; i++) {
            CompoundTag e = list.getCompound(i);
            ItemStack item = ItemStack.parseOptional(player.registryAccess(), e.getCompound("item"));
            if (item.isEmpty()) continue;
            item.setCount(1);
            int count = Math.min(e.getInt("quantity"), item.getMaxStackSize() * STACK_MULTIPLIER);
            if (count > 0) result.add(new Entry(item, count));
        }
        return result;
    }
    private static void writeSecond(ServerPlayer player, java.util.List<Entry> entries) {
        ListTag list = new ListTag();
        for (Entry e : entries) {
            if (e.quantity() <= 0 || e.sample().isEmpty()) continue;
            CompoundTag tag = new CompoundTag();
            ItemStack sample = e.sample().copy(); sample.setCount(1);
            tag.put("item", sample.save(player.registryAccess()));
            tag.putInt("quantity", e.quantity()); list.add(tag);
        }
        player.getPersistentData().put(SECOND_KEY, list);
    }
    public static int insertSecond(ServerPlayer player, ItemStack offered) {
        if (offered.isEmpty()) return 0;
        var entries = readSecond(player);
        int remaining = offered.getCount();
        for (int i = 0; i < entries.size() && remaining > 0; i++) {
            Entry e = entries.get(i);
            if (!ItemStack.isSameItemSameComponents(e.sample(), offered)) continue;
            int n = Math.min(remaining, e.capacity() - e.quantity());
            if (n > 0) { entries.set(i, new Entry(e.sample(), e.quantity() + n)); remaining -= n; }
        }
        while (remaining > 0 && entries.size() < TYPES) {
            ItemStack sample = offered.copy(); sample.setCount(1);
            int n = Math.min(remaining, sample.getMaxStackSize() * STACK_MULTIPLIER);
            entries.add(new Entry(sample, n)); remaining -= n;
        }
        if (remaining != offered.getCount()) writeSecond(player, entries);
        return offered.getCount() - remaining;
    }
    public static ItemStack extractSecond(ServerPlayer player, int index, int requested) {
        var entries = readSecond(player);
        if (index < 0 || index >= entries.size() || requested <= 0) return ItemStack.EMPTY;
        Entry e = entries.get(index);
        int n = Math.min(Math.min(requested, e.quantity()), e.sample().getMaxStackSize());
        ItemStack result = e.sample().copy(); result.setCount(n);
        if (e.quantity() == n) entries.remove(index);
        else entries.set(index, new Entry(e.sample(), e.quantity() - n));
        writeSecond(player, entries); return result;
    }
    /** Returns how many units were actually stored; never deletes overflow. */
    public static int insert(ServerPlayer player, ItemStack offered) {
        if (offered.isEmpty()) return 0;
        var entries = read(player);
        int remaining = offered.getCount();
        int stored = 0;
        // Fill existing matching entries, then allocate additional entries even for
        // the same item/component combination once an entry reaches 32 stacks.
        for (int i = 0; i < entries.size() && remaining > 0; i++) {
            Entry entry = entries.get(i);
            if (!ItemStack.isSameItemSameComponents(entry.sample(), offered)) continue;
            int accepted = Math.min(remaining, Math.max(0, entry.capacity() - entry.quantity()));
            if (accepted > 0) {
                entries.set(i, new Entry(entry.sample(), entry.quantity() + accepted));
                remaining -= accepted;
                stored += accepted;
            }
        }
        while (remaining > 0 && entries.size() < TYPES) {
            ItemStack sample = offered.copy();
            sample.setCount(1);
            int accepted = Math.min(remaining, sample.getMaxStackSize() * STACK_MULTIPLIER);
            entries.add(new Entry(sample, accepted));
            remaining -= accepted;
            stored += accepted;
        }
        if (stored > 0) write(player, entries);
        return stored;
    }

    /** Index is zero-based; extraction is limited to one vanilla stack at a time. */
    public static ItemStack extract(ServerPlayer player, int index, int requested) {
        var entries = read(player);
        if (index < 0 || index >= entries.size() || requested <= 0) return ItemStack.EMPTY;
        Entry entry = entries.get(index);
        int amount = Math.min(Math.min(requested, entry.quantity()), entry.sample().getMaxStackSize());
        ItemStack result = entry.sample().copy();
        result.setCount(amount);
        int leftover = entry.quantity() - amount;
        if (leftover == 0) entries.remove(index);
        else entries.set(index, new Entry(entry.sample(), leftover));
        write(player, entries);
        return result;
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        CompoundTag original = event.getOriginal().getPersistentData();
        if (original.contains(SECOND_KEY, Tag.TAG_LIST))
            event.getEntity().getPersistentData().put(SECOND_KEY, original.get(SECOND_KEY).copy());
        if (original.contains(KEY, Tag.TAG_LIST))
            event.getEntity().getPersistentData().put(KEY, original.get(KEY).copy());
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tota")
            .then(Commands.literal("repository")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    var entries = read(player);
                    player.sendSystemMessage(Component.literal("Celestial Repository: " + entries.size() + "/" + TYPES + " variants"));
                    for (int i = 0; i < entries.size(); i++) {
                        Entry e = entries.get(i);
                        player.sendSystemMessage(Component.literal("#" + (i + 1) + " " + e.sample().getHoverName().getString() + " x" + e.quantity() + "/" + e.capacity()));
                    }
                    return entries.size();
                })
                .then(Commands.literal("open").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, who) ->
                            new CelestialRepositoryMenu(id, inv), Component.literal("Celestial Repository")));
                    return 1;
                }))
                .then(Commands.literal("deposit_hand").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ItemStack hand = player.getMainHandItem();
                    int moved = insert(player, hand);
                    if (moved > 0) hand.shrink(moved);
                    player.sendSystemMessage(Component.literal("Deposited " + moved + " items; " + hand.getCount() + " remain in hand."));
                    return moved;
                }))
                .then(Commands.literal("withdraw")
                    .then(Commands.argument("slot", IntegerArgumentType.integer(1, TYPES))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                int index = IntegerArgumentType.getInteger(ctx, "slot") - 1;
                                int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                ItemStack taken = extract(player, index, amount);
                                if (taken.isEmpty()) return 0;
                                int count = taken.getCount();
                                if (!player.getInventory().add(taken) && !taken.isEmpty()) player.drop(taken, false);
                                else if (!taken.isEmpty()) player.drop(taken, false);
                                player.sendSystemMessage(Component.literal("Withdrew " + count + " items from Repository slot " + (index + 1)));
                                return count;
                            }))))));
    }
}
