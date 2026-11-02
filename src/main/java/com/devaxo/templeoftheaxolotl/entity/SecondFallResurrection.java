package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.item.ModItems;
import com.devaxo.templeoftheaxolotl.maze.OriginTempleState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

/** Dropping a maximally enchanted Ring into soul fire near freed Above All restores redeemed Wrath. */
public final class SecondFallResurrection {
    private final Map<UUID, Long> rejectionNotice = new HashMap<>();
    private static final String[] REQUIRED = {"event_horizon", "celestial_convergence", "lucky_stars", "supernova"};

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % 10 != 0) return;
        ServerLevel origin = server.getLevel(OriginTempleState.DIMENSION);
        if (origin == null || !OriginTempleState.get(origin).originResolved()
                || !OriginTempleState.get(origin).rescueStarted()) return;
        for (ServerLevel level : server.getAllLevels()) {
            // Restrict scans to players' nearby dropped items, never all loaded entities.
            for (ServerPlayer player : level.players()) {
                for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class,
                        player.getBoundingBox().inflate(12), e -> e.isAlive()
                                && e.getItem().is(ModItems.RING_OF_THE_ONE.get()))) {
                    if (!level.getBlockState(item.blockPosition()).is(Blocks.SOUL_FIRE)) continue;
                    var aboveAll = level.getEntitiesOfClass(AxolotlAbsoluteEntity.class,
                            item.getBoundingBox().inflate(4), e -> e.isAlive());
                    if (aboveAll.isEmpty()) {
                        reject(player, item, level);
                        continue;
                    }
                    if (!validRing(item.getItem())) {
                        reject(player, item, level);
                        continue;
                    }
                    // Prevent duplicate redeemed Wraths in the same ritual area.
                    if (!level.getEntitiesOfClass(TameableWrathEntity.class,
                            item.getBoundingBox().inflate(24), e -> e.isAlive()).isEmpty()) {
                        reject(player, item, level);
                        continue;
                    }
                    TameableWrathEntity redeemed = ModEntities.TAMEABLE_WRATH.get().create(level);
                    if (redeemed == null) continue;
                    redeemed.moveTo(item.getX(), item.getY() + 1, item.getZ(), 0, 0);
                    redeemed.addTag("origin_redeemed_wrath");
                    if (level.addFreshEntity(redeemed)) {
                        ItemStack remaining = item.getItem();
                        remaining.shrink(1);
                        if (remaining.isEmpty()) item.discard();
                        player.sendSystemMessage(Component.literal("The Soul of the Damned has accepted the sacrifice."));
                    }
                }
            }
        }
    }
    private void reject(ServerPlayer player, ItemEntity item, ServerLevel level) {
        long now = level.getGameTime();
        if (now - rejectionNotice.getOrDefault(item.getUUID(), -1000L) >= 100) {
            player.displayClientMessage(Component.literal("The Soul of the Damned refuses the sacrifice"), true);
            rejectionNotice.put(item.getUUID(), now);
        }
    }
    private boolean validRing(ItemStack ring) {
        ItemEnchantments enchantments = ring.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (String required : REQUIRED) {
            boolean found = false;
            for (var entry : enchantments.entrySet()) {
                var key = entry.getKey().unwrapKey();
                if (key.isPresent() && key.get().location().getNamespace().equals("temple_of_the_axolotl")
                        && key.get().location().getPath().equals(required)) {
                    int max = entry.getKey().value().getMaxLevel();
                    if (entry.getIntValue() >= max) found = true;
                }
            }
            if (!found) return false;
        }
        return true;
    }
}
