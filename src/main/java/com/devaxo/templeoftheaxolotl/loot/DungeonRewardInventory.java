package com.devaxo.templeoftheaxolotl.loot;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative, per-player staging area for unclaimed dungeon rewards.
 * Rewards stay here until success; failure destroys ONLY these staged rewards.
 * Normal inventory and pre-existing items are never scanned or deleted.
 */
public final class DungeonRewardInventory {
    private DungeonRewardInventory() {}
    public static final int SLOTS = 54;
    private static final String KEY = "tota_dungeon_rewards_v1";
    private static final String BINDING = "tota_reward_run_v2";

    /** Binding is held outside the ItemStacks; no security tags can leak into awarded loot. */
    public static void bind(ServerPlayer player, java.util.UUID templeId, long seed) {
        if (occupied(player) != 0) throw new IllegalStateException("Cannot rebind nonempty rewards");
        CompoundTag record = new CompoundTag();
        record.putUUID("temple", templeId);
        record.putUUID("run", java.util.UUID.randomUUID());
        record.putLong("seed", seed);
        record.putString("state", "ACTIVE");
        player.getPersistentData().put(BINDING, record);
    }

    public static String bindingStatus(ServerPlayer player) {
        if (!player.getPersistentData().contains(BINDING, Tag.TAG_COMPOUND)) return "UNBOUND (developer staging)";
        CompoundTag record = player.getPersistentData().getCompound(BINDING);
        return record.getString("state") + " temple=" + record.getUUID("temple")
                + " seed=" + record.getLong("seed") + " run=" + record.getUUID("run");
    }

    /** Called only when the server explicitly retires a temple instance. */
    public static void invalidateTemple(net.minecraft.server.MinecraftServer server, java.util.UUID templeId) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            CompoundTag record = player.getPersistentData().getCompound(BINDING);
            if (record.hasUUID("temple") && record.getUUID("temple").equals(templeId)
                    && "ACTIVE".equals(record.getString("state"))) fail(player);
        }
    }


    public static NonNullList<ItemStack> read(ServerPlayer player) {
        NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ListTag list = player.getPersistentData().getList(KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getInt("slot");
            if (slot >= 0 && slot < SLOTS)
                items.set(slot, ItemStack.parseOptional(player.registryAccess(), entry.getCompound("stack")));
        }
        return items;
    }

    public static void write(ServerPlayer player, NonNullList<ItemStack> items) {
        ListTag list = new ListTag();
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("slot", i);
            entry.put("stack", stack.save(player.registryAccess()));
            list.add(entry);
        }
        player.getPersistentData().put(KEY, list);
    }

    /** Adds a reward to the protected inventory, never to ordinary player slots.
     * Returns false without changing storage when there is insufficient room.
     */
    public static boolean add(ServerPlayer player, ItemStack reward) {
        if (reward.isEmpty()) return true;
        CompoundTag binding = player.getPersistentData().getCompound(BINDING);
        if (binding.contains("state") && !"ACTIVE".equals(binding.getString("state"))) return false;
        NonNullList<ItemStack> items = read(player);
        ItemStack remaining = reward.copy();
        for (int i = 0; i < SLOTS && !remaining.isEmpty(); i++) {
            ItemStack current = items.get(i);
            if (!current.isEmpty() && ItemStack.isSameItemSameComponents(current, remaining)) {
                int moved = Math.min(remaining.getCount(), current.getMaxStackSize() - current.getCount());
                if (moved > 0) { current.grow(moved); remaining.shrink(moved); }
            }
        }
        for (int i = 0; i < SLOTS && !remaining.isEmpty(); i++) {
            if (items.get(i).isEmpty()) {
                int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                ItemStack inserted = remaining.copy();
                inserted.setCount(moved);
                items.set(i, inserted);
                remaining.shrink(moved);
            }
        }
        if (!remaining.isEmpty()) return false;
        write(player, items);
        return true;
    }

    /** Failure removes staged dungeon rewards, not any items already owned. */
    public static void fail(ServerPlayer player) {
        player.getPersistentData().remove(KEY);
        player.getPersistentData().remove(BINDING);
    }

    /** Success packages all staged rewards into one custom, unstackable Temple Lootbag. */
    public static void succeed(ServerPlayer player) {
        NonNullList<ItemStack> items = read(player);
        java.util.List<ItemStack> rewards = new java.util.ArrayList<>();
        for (ItemStack item : items) if (!item.isEmpty()) rewards.add(item.copy());
        if (rewards.isEmpty()) return;
        ItemStack bag = com.devaxo.templeoftheaxolotl.item.TempleLootbagItem.pack(player, rewards);
        // Full inventory must not silently eat the reward. Deliver on the ground
        // after teleport if necessary; the staging record is cleared only on delivery.
        ItemStack delivery = bag.copy();
        boolean inserted = player.getInventory().add(delivery);
        if (!inserted || !delivery.isEmpty()) {
            net.minecraft.world.entity.item.ItemEntity entity = new net.minecraft.world.entity.item.ItemEntity(
                    player.serverLevel(), player.getX(), player.getY() + 0.5, player.getZ(),
                    delivery.isEmpty() ? bag.copy() : delivery.copy());
            entity.setPickUpDelay(0);
            if (!player.serverLevel().addFreshEntity(entity)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "Temple reward could not be delivered; staging retained."));
                return;
            }
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "Inventory full: your Temple Lootbag was placed at your feet."));
        }
        fail(player);
    }

    public static int occupied(ServerPlayer player) {
        int count = 0;
        for (ItemStack item : read(player)) if (!item.isEmpty()) count++;
        return count;
    }

    /** Keep reward data through vanilla death/respawn player-object replacement. */
    public static void copyFrom(ServerPlayer previous, ServerPlayer replacement) {
        if (previous.getPersistentData().contains(KEY))
            replacement.getPersistentData().put(KEY, previous.getPersistentData().get(KEY).copy());
        if (previous.getPersistentData().contains(BINDING))
            replacement.getPersistentData().put(BINDING, previous.getPersistentData().get(BINDING).copy());
    }
}
