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
    private static final String SECOND_KEY = "tota_celestial_repository_page_two_prototype";
    private static final String THIRD_KEY = "tota_celestial_repository_page_three";
    private CelestialRepository() {}
    public record Entry(ItemStack sample, int quantity) {
        public int capacity() { return sample.getMaxStackSize() * STACK_MULTIPLIER; }
    }
    public static int tier(ServerPlayer player) {
        ItemStack module = com.devaxo.templeoftheaxolotl.compass.TempleHudModules.getSlot(player, 3);
        if (module.is(com.devaxo.templeoftheaxolotl.item.ModItems.DIAMOND_REPOSITORY.get())) return 4;
        if (module.is(com.devaxo.templeoftheaxolotl.item.ModItems.GOLD_REPOSITORY.get())) return 3;
        if (module.is(com.devaxo.templeoftheaxolotl.item.ModItems.IRON_REPOSITORY.get())) return 2;
        if (module.is(com.devaxo.templeoftheaxolotl.item.ModItems.CELESTIAL_REPOSITORY.get())) return 1;
        return 0;
    }
    public static int pages(ServerPlayer player) { return tier(player) >= 2 ? 3 : tier(player) >= 1 ? 2 : 1; }
    public static int multiplier(ServerPlayer player) { return tier(player) >= 3 ? 64 : 32; }
    private static String key(int page) { return page == 0 ? KEY : page == 1 ? SECOND_KEY : THIRD_KEY; }
    public static java.util.List<Entry> readPage(ServerPlayer player, int page) {
        java.util.ArrayList<Entry> result = new java.util.ArrayList<>();
        ListTag list = player.getPersistentData().getList(key(page), Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && result.size() < TYPES; i++) {
            CompoundTag tag = list.getCompound(i);
            ItemStack sample = ItemStack.parseOptional(player.registryAccess(), tag.getCompound("item"));
            if (sample.isEmpty()) continue;
            sample.setCount(1);
            int count = tag.getInt("quantity");
            // Do not clamp old counts when a player removes a higher-tier module.
            if (count > 0) result.add(new Entry(sample, count));
        }
        return result;
    }
    private static void writePage(ServerPlayer player, int page, java.util.List<Entry> entries) {
        ListTag list = new ListTag();
        for (Entry entry : entries) {
            if (entry.quantity() <= 0 || entry.sample().isEmpty()) continue;
            CompoundTag tag = new CompoundTag();
            ItemStack sample = entry.sample().copy(); sample.setCount(1);
            tag.put("item", sample.save(player.registryAccess()));
            tag.putInt("quantity", entry.quantity()); list.add(tag);
        }
        player.getPersistentData().put(key(page), list);
    }
    public static java.util.List<Entry> read(ServerPlayer p) { return readPage(p, 0); }
    public static java.util.List<Entry> readSecond(ServerPlayer p) { return readPage(p, 1); }
    public static int insert(ServerPlayer p, ItemStack s) { return insertPage(p, 0, s); }
    public static int insertSecond(ServerPlayer p, ItemStack s) { return insertPage(p, 1, s); }
    public static ItemStack extract(ServerPlayer p, int i, int n) { return extractPage(p, 0, i, n); }
    public static ItemStack extractSecond(ServerPlayer p, int i, int n) { return extractPage(p, 1, i, n); }
    public static int insertPage(ServerPlayer player, int page, ItemStack offered) {
        if (offered.isEmpty() || page < 0 || page >= pages(player)) return 0;
        if (tier(player) == 4) consolidate(player);
        int remaining = offered.getCount();
        int cap = offered.getMaxStackSize() * multiplier(player);
        int start = tier(player) == 4 ? 0 : page;
        int end = tier(player) == 4 ? pages(player) : page + 1;
        for (int p = start; p < end && remaining > 0; p++) {
            var entries = readPage(player, p);
            for (int i = 0; i < entries.size() && remaining > 0; i++) {
                Entry e = entries.get(i);
                if (!ItemStack.isSameItemSameComponents(e.sample(), offered)) continue;
                int amount = Math.min(remaining, Math.max(0, cap - e.quantity()));
                if (amount > 0) { entries.set(i, new Entry(e.sample(), e.quantity() + amount)); remaining -= amount; }
            }
            while (remaining > 0 && entries.size() < TYPES) {
                ItemStack sample = offered.copy(); sample.setCount(1);
                int amount = Math.min(remaining, cap);
                entries.add(new Entry(sample, amount)); remaining -= amount;
            }
            writePage(player, p, entries);
        }
        return offered.getCount() - remaining;
    }
    public static ItemStack extractPage(ServerPlayer player, int page, int index, int requested) {
        if (page < 0 || page >= pages(player)) return ItemStack.EMPTY;
        var entries = readPage(player, page);
        if (index < 0 || index >= entries.size() || requested <= 0) return ItemStack.EMPTY;
        Entry e = entries.get(index);
        int n = Math.min(Math.min(requested, e.quantity()), e.sample().getMaxStackSize());
        ItemStack result = e.sample().copy(); result.setCount(n);
        if (e.quantity() == n) entries.remove(index);
        else entries.set(index, new Entry(e.sample(), e.quantity() - n));
        writePage(player, page, entries); return result;
    }
    /** Stable consolidation across all unlocked pages, respecting item components and cap. */
    public static void consolidate(ServerPlayer player) {
        if (tier(player) != 4) return;
        java.util.List<Entry> all = new java.util.ArrayList<>();
        for (int page = 0; page < 3; page++) all.addAll(readPage(player, page));
        java.util.List<Entry> merged = new java.util.ArrayList<>();
        for (Entry incoming : all) {
            int remaining = incoming.quantity();
            int cap = incoming.sample().getMaxStackSize() * multiplier(player);
            for (int i = 0; i < merged.size() && remaining > 0; i++) {
                Entry e = merged.get(i);
                if (!ItemStack.isSameItemSameComponents(e.sample(), incoming.sample())) continue;
                int amount = Math.min(remaining, Math.max(0, cap - e.quantity()));
                if (amount > 0) { merged.set(i, new Entry(e.sample(), e.quantity() + amount)); remaining -= amount; }
            }
            while (remaining > 0) {
                int amount = Math.min(remaining, cap);
                merged.add(new Entry(incoming.sample(), amount)); remaining -= amount;
            }
        }
        // 3*56 input entries cannot require more than 3*56 output entries.
        for (int page = 0; page < 3; page++) {
            int from = Math.min(page * TYPES, merged.size());
            int to = Math.min((page + 1) * TYPES, merged.size());
            writePage(player, page, new java.util.ArrayList<>(merged.subList(from, to)));
        }
    }
    /** Extract ingredients across pages for the embedded workstation; server-only. */
    public static int countAvailable(ServerPlayer player, ItemStack ingredient) {
        int count = 0;
        for (int p = 0; p < pages(player); p++)
            for (Entry e : readPage(player, p))
                if (ItemStack.isSameItemSameComponents(e.sample(), ingredient)) count += e.quantity();
        return count;
    }
    public static int takeMatching(ServerPlayer player, ItemStack ingredient, int requested) {
        int taken = 0;
        for (int p = 0; p < pages(player) && taken < requested; p++) {
            var entries = readPage(player, p);
            for (int i = entries.size()-1; i >= 0 && taken < requested; i--) {
                Entry e = entries.get(i);
                if (!ItemStack.isSameItemSameComponents(e.sample(), ingredient)) continue;
                int n = Math.min(requested-taken, e.quantity()); taken += n;
                if (n == e.quantity()) entries.remove(i);
                else entries.set(i, new Entry(e.sample(), e.quantity()-n));
            }
            writePage(player, p, entries);
        }
        return taken;
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        CompoundTag original = event.getOriginal().getPersistentData();
        if (original.contains(THIRD_KEY, Tag.TAG_LIST))
            event.getEntity().getPersistentData().put(THIRD_KEY, original.get(THIRD_KEY).copy());
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
