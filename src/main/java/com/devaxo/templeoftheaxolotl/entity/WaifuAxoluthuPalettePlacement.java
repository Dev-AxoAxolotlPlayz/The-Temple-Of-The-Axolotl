package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.Map;

/** Explicit allowlist: no arbitrary blocks may be injected through saved NBT. */
public final class WaifuAxoluthuPalettePlacement {
    private WaifuAxoluthuPalettePlacement() {}
    private static final Map<String, BlockState> PALETTE = Map.ofEntries(
        Map.entry("minecraft:air", Blocks.AIR.defaultBlockState()),
        Map.entry("minecraft:oak_door", Blocks.OAK_DOOR.defaultBlockState()),
        Map.entry("minecraft:cobblestone", Blocks.COBBLESTONE.defaultBlockState()),
        Map.entry("minecraft:oak_planks", Blocks.OAK_PLANKS.defaultBlockState()),
        Map.entry("minecraft:stripped_oak_log", Blocks.STRIPPED_OAK_LOG.defaultBlockState()),
        Map.entry("minecraft:glass", Blocks.GLASS.defaultBlockState()),
        Map.entry("minecraft:glass_pane", Blocks.GLASS_PANE.defaultBlockState()),
        Map.entry("minecraft:oak_slab", Blocks.OAK_SLAB.defaultBlockState()),
        Map.entry("minecraft:spruce_planks", Blocks.SPRUCE_PLANKS.defaultBlockState()),
        Map.entry("minecraft:spruce_slab", Blocks.SPRUCE_SLAB.defaultBlockState()),
        Map.entry("minecraft:spruce_stairs", Blocks.SPRUCE_STAIRS.defaultBlockState()),
        Map.entry("minecraft:oak_stairs", Blocks.OAK_STAIRS.defaultBlockState()),
        Map.entry("minecraft:wall_torch", Blocks.WALL_TORCH.defaultBlockState()),
        Map.entry("minecraft:oak_fence", Blocks.OAK_FENCE.defaultBlockState()),
        Map.entry("minecraft:cobblestone_wall", Blocks.COBBLESTONE_WALL.defaultBlockState()),
        Map.entry("minecraft:sea_lantern", Blocks.SEA_LANTERN.defaultBlockState()),
        Map.entry("minecraft:lantern", Blocks.LANTERN.defaultBlockState()),
        Map.entry("minecraft:chain", Blocks.CHAIN.defaultBlockState())
    );
    public static boolean allowed(String id) { return PALETTE.containsKey(id); }
    public static BlockState state(String id, int quarterTurns) {
        BlockState state = PALETTE.get(id);
        if (state == null) return null;
        int turns = Math.floorMod(quarterTurns, 4);
        // Original blueprint's front points north.
        Direction front = Direction.NORTH;
        for (int i=0;i<turns;i++) front=front.getClockWise();
        if (id.equals("minecraft:wall_torch"))
            return state.setValue(BlockStateProperties.HORIZONTAL_FACING, front);
        if (id.equals("minecraft:spruce_stairs") || id.equals("minecraft:oak_stairs"))
            return state.setValue(BlockStateProperties.HORIZONTAL_FACING, front);
        if (id.equals("minecraft:lantern"))
            return state.setValue(BlockStateProperties.HANGING, true);
        return state;
    }

    /** Resolve the same exact intended state for placement, verification and holograms. */
    public static BlockState state(net.minecraft.nbt.CompoundTag entry, int quarterTurns) {
        String id = entry.getString("block");
        BlockState state = state(id, quarterTurns);
        if (state == null) return null;
        if (id.equals("minecraft:lantern"))
            state = state.setValue(BlockStateProperties.HANGING,
                    entry.contains("hanging", 1) && entry.getBoolean("hanging"));
        if (entry.contains("facing", 8) && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction direction = Direction.byName(entry.getString("facing"));
            if (direction != null && direction.getAxis().isHorizontal()) {
                for (int i = 0; i < Math.floorMod(quarterTurns, 4); i++) direction = direction.getClockWise();
                state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
            }
        }
        if (entry.contains("half", 8) && state.hasProperty(BlockStateProperties.HALF)) {
            net.minecraft.world.level.block.state.properties.Half half =
                    "top".equals(entry.getString("half"))
                    ? net.minecraft.world.level.block.state.properties.Half.TOP
                    : net.minecraft.world.level.block.state.properties.Half.BOTTOM;
            state = state.setValue(BlockStateProperties.HALF, half);
        }
        if (entry.contains("slabType", 8) && state.hasProperty(BlockStateProperties.SLAB_TYPE)) {
            net.minecraft.world.level.block.state.properties.SlabType slab =
                    "top".equals(entry.getString("slabType"))
                    ? net.minecraft.world.level.block.state.properties.SlabType.TOP
                    : net.minecraft.world.level.block.state.properties.SlabType.BOTTOM;
            state = state.setValue(BlockStateProperties.SLAB_TYPE, slab);
        }
        if (entry.contains("doorHalf", 8) && state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            state = state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
                "upper".equals(entry.getString("doorHalf"))
                ? net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER
                : net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
        }
        if (entry.contains("hinge", 8) && state.hasProperty(BlockStateProperties.DOOR_HINGE)) {
            state = state.setValue(BlockStateProperties.DOOR_HINGE,
                "right".equals(entry.getString("hinge"))
                ? net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT
                : net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT);
        }
        return state;
    }
}
