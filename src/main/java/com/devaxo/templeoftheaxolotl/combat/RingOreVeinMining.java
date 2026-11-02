package com.devaxo.templeoftheaxolotl.combat;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 26-neighbor ore vein mining; 256 blocks maximum INCLUDING the original block. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class RingOreVeinMining {
    private static final int LIMIT = 256;
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);
    private RingOreVeinMining() {}

    /** Vanilla ore families and their corresponding raw storage blocks. */
    private static String family(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String path = id.getPath();
        if (id.getNamespace().equals("minecraft")) {
            if (path.equals("raw_iron_block") || path.equals("iron_ore") || path.equals("deepslate_iron_ore")) return "minecraft:iron";
            if (path.equals("raw_copper_block") || path.equals("copper_ore") || path.equals("deepslate_copper_ore")) return "minecraft:copper";
            if (path.equals("raw_gold_block") || path.equals("gold_ore") || path.equals("deepslate_gold_ore") || path.equals("nether_gold_ore")) return "minecraft:gold";
        }
        // Exact registry ore type for all other vanilla and modded *_ore blocks.
        // Deepslate and regular versions of the same named ore share a family.
        if (path.endsWith("_ore")) {
            String base = path.startsWith("deepslate_") ? path.substring("deepslate_".length()) : path;
            return id.getNamespace() + ":" + base;
        }
        return null;
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (ACTIVE.get() || event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)
                || !player.getMainHandItem().is(ModItems.RING_OF_THE_ONE.get())
                || !RingMiningMode.isMining(player)) return;
        String target = family(event.getState());
        if (target == null) return;
        BlockPos origin = event.getPos();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> matched = new ArrayList<>();
        seen.add(origin.immutable());
        queue.add(origin.immutable());
        while (!queue.isEmpty() && matched.size() < LIMIT) {
            BlockPos pos = queue.removeFirst();
            if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4) || !target.equals(family(level.getBlockState(pos)))) continue;
            matched.add(pos);
            if (matched.size() == LIMIT) break;
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dy == 0 && dz == 0) continue;
                BlockPos next = pos.offset(dx, dy, dz);
                if (seen.add(next)) queue.addLast(next);
            }
        }
        // Break through player game mode: normal permission events, tool/loot logic,
        // enchantment hooks, and durability behavior are retained.
        ACTIVE.set(true);
        try {
            for (BlockPos pos : matched) {
                if (pos.equals(origin)) continue; // Minecraft handles the initial break.
                if (!player.getMainHandItem().is(ModItems.RING_OF_THE_ONE.get()) || !RingMiningMode.isMining(player)) break;
                if (level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4) && target.equals(family(level.getBlockState(pos)))) {
                    player.gameMode.destroyBlock(pos);
                }
            }
        } finally {
            ACTIVE.remove();
        }
    }
}
