package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import java.util.HashSet;
import java.util.Set;

/** Phase 3.5: shared, render-independent blueprint manifest.
 * All coordinates are world coordinates; no blocks are placed by this class.
 * Client synchronization and the actual renderer are separate responsibilities.
 */
public final class WaifuAxoluthuCelestialBlueprint {
    private WaifuAxoluthuCelestialBlueprint() {}
    public static final int CYAN = 0x50E9EF;
    public static final int DEEP_SEA_BLUE = 0x07538F;
    public static final int CELESTIAL_GOLD = 0xFFD15C;

    private static void add(ListTag blocks, BlockPos pos, String block) {
        CompoundTag entry = new CompoundTag();
        entry.putLong("pos", pos.asLong());
        entry.putString("block", block);
        blocks.add(entry);
    }

    /** Converts the legacy 5x5 starter workshop's actual placement order into a manifest. */
    public static CompoundTag workshop(BlockPos origin) {
        ListTag blocks = new ListTag();
        for (int i = 0; i < 25; i++) add(blocks, origin.offset(i % 5, 0, i / 5), "minecraft:oak_planks");
        for (int i = 0; i < 8; i++) {
            int corner = i / 2;
            add(blocks, origin.offset((corner & 1) == 0 ? 0 : 4, 1 + i % 2,
                    (corner & 2) == 0 ? 0 : 4), "minecraft:oak_planks");
        }
        for (int i = 0; i < 25; i++) add(blocks, origin.offset(i % 5, 3, i / 5), "minecraft:oak_planks");
        return manifest("workshop", origin, origin.offset(2, 1, -1), "north", blocks);
    }

    /** Storage hut: floor, perimeter walls, roof, two chests and a front door. */
    public static CompoundTag storage(BlockPos origin) {
        ListTag blocks = new ListTag();
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++)
            add(blocks, origin.offset(x, 0, z), "minecraft:oak_planks");
        for (int y = 1; y <= 2; y++) for (int z = 0; z < 5; z++) for (int x = 0; x < 5; x++) {
            if (x != 0 && x != 4 && z != 0 && z != 4) continue;
            if (x == 2 && z == 0) continue;
            add(blocks, origin.offset(x, y, z), "minecraft:oak_planks");
        }
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++)
            add(blocks, origin.offset(x, 3, z), "minecraft:oak_planks");
        add(blocks, origin.offset(1, 1, 3), "minecraft:chest");
        add(blocks, origin.offset(2, 1, 3), "minecraft:chest");
        add(blocks, origin.offset(2, 1, 0), "minecraft:oak_door");
        return manifest("storage", origin, origin.offset(2, 1, -1), "north", blocks);
    }

    /** Wraps an existing Stage 5 block-by-block blueprint without regenerating it. */
    public static CompoundTag stage5(CompoundTag project) {
        CompoundTag preview = project.getCompound("blueprintPreview");
        ListTag blocks = preview.getList("blocks", 10);
        if (blocks.isEmpty()) throw new IllegalArgumentException("No Stage 5 blueprint blocks");
        BlockPos origin = BlockPos.of(project.getLong("origin"));
        return manifest("house", origin, origin.offset(project.getInt("width") / 2, 1, -1),
                "north", blocks);
    }

    private static CompoundTag manifest(String type, BlockPos origin, BlockPos entrance,
                                         String facing, ListTag blocks) {
        CompoundTag out = new CompoundTag();
        out.putInt("schema", 1);
        out.putString("type", type);
        out.putLong("origin", origin.asLong());
        out.putLong("entrance", entrance.asLong());
        out.putString("facing", facing);
        out.put("blocks", blocks.copy());
        out.putInt("count", blocks.size());
        return out;
    }

    /** Returns only unfinished block positions. Comparison is delegated to the caller
     * so no client world access or chunk loads occur inside this pure data utility. */
    public static Set<BlockPos> unfinished(CompoundTag manifest, Set<BlockPos> completed) {
        Set<BlockPos> remaining = new HashSet<>();
        ListTag blocks = manifest.getList("blocks", 10);
        for (int i = 0; i < blocks.size(); i++) {
            BlockPos pos = BlockPos.of(blocks.getCompound(i).getLong("pos"));
            if (!completed.contains(pos)) remaining.add(pos);
        }
        return remaining;
    }
}
