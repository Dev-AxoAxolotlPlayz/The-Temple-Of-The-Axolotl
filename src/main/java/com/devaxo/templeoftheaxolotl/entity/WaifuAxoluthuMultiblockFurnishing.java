package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;

/** Safe, inventory-backed bed and paired-chest placement for approved buildings. */
public final class WaifuAxoluthuMultiblockFurnishing {
    private WaifuAxoluthuMultiblockFurnishing() {}

    public static String placeBed(WaifuAxoluthuEntity axo, ServerLevel level,
                                  CompoundTag request, BlockPos foot) {
        Direction facing = rotated(Direction.SOUTH, request.getInt("quarterTurns"));
        BlockPos head = foot.relative(facing);
        BlockState footState = Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.FACING, facing).setValue(BedBlock.PART, BedPart.FOOT);
        BlockState headState = footState.setValue(BedBlock.PART, BedPart.HEAD);
        if (level.getBlockState(foot).equals(footState)
                && level.getBlockState(head).equals(headState)) {
            advance(request, 1);
            return "CONTINUE";
        }
        return placePair(axo, level, request, foot, head, footState, headState,
                Items.RED_BED, 1);
    }

    public static String placeChest(WaifuAxoluthuEntity axo, ServerLevel level,
                                    CompoundTag request, ListTag fixtures,
                                    int cursor, BlockPos first) {
        int secondIndex = -1;
        BlockPos second = null;
        for (int i = cursor + 1; i < fixtures.size(); i++) {
            CompoundTag candidate = fixtures.getCompound(i);
            if (!"minecraft:chest".equals(candidate.getString("block"))) continue;
            BlockPos pos = BlockPos.of(candidate.getLong("pos"));
            if (first.distManhattan(pos) == 1) {
                secondIndex = i;
                second = pos;
                break;
            }
        }
        if (second == null) {
            // A single chest is valid for a small house.
            if (level.getBlockState(first).is(Blocks.CHEST)
                    && level.getBlockState(first).getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
                advance(request, 1);
                return "CONTINUE";
            }
            BlockState state = Blocks.CHEST.defaultBlockState()
                    .setValue(ChestBlock.FACING, rotated(Direction.NORTH, request.getInt("quarterTurns")));
            return placeSingle(axo, level, request, first, state, Items.CHEST);
        }
        // Only consume adjacent sequential entries. Never skip unrelated fixtures.
        if (secondIndex != cursor + 1) return "PAUSE:NONCONSECUTIVE_CHEST_PAIR";
        Direction partner = Direction.fromDelta(second.getX()-first.getX(), 0,
                second.getZ()-first.getZ());
        if (partner == null || partner.getAxis().isVertical()) return "PAUSE:INVALID_CHEST_PAIR";
        Direction facing = partner.getCounterClockWise();
        BlockState left = Blocks.CHEST.defaultBlockState()
                .setValue(ChestBlock.FACING, facing).setValue(ChestBlock.TYPE, ChestType.LEFT);
        BlockState right = left.setValue(ChestBlock.TYPE, ChestType.RIGHT);
        if (level.getBlockState(first).equals(left) && level.getBlockState(second).equals(right)) {
            advance(request, 2);
            return "CONTINUE";
        }
        return placePair(axo, level, request, first, second, left, right, Items.CHEST, 2);
    }

    private static String placeSingle(WaifuAxoluthuEntity axo, ServerLevel level,
                                      CompoundTag request, BlockPos pos,
                                      BlockState state, Item item) {
        if (!clear(level, pos)) return "PAUSE:CHEST_SPACE_OCCUPIED";
        if (!supported(level, pos, state)) return "PAUSE:CHEST_SUPPORT_MISSING";
        var bag = axo.getAcquiredMaterials();
        int slot = slot(bag, item, 1);
        if (slot < 0) {
            WaifuAxoluthuRecipeBrain.ensure(axo, item, 1, level.getGameTime());
            return "WAIT_MATERIALS";
        }
        if (WaifuAxoluthuBuildNavigation.approach(axo, level, pos, 1.05) != null)
            return "MOVING";
        if (!axo.actionReady(WaifuAxoluthuEntity.ACTION_FOCUSED, pos, 41, 80)
                || axo.getBoundingBox().intersects(new AABB(pos))) return "MOVING";
        ItemStack taken = bag.removeItem(slot, 1);
        bag.setChanged();
        if (taken.getCount() != 1) return "WAIT_MATERIALS";
        if (!level.setBlock(pos, state, 3)) {
            bag.addItem(taken);
            return "PAUSE:CHEST_PLACEMENT_FAILED";
        }
        axo.finishAction();
        WaifuAxoluthuBuildNavigation.completed(axo);
        advance(request, 1);
        return "CONTINUE";
    }

    private static String placePair(WaifuAxoluthuEntity axo, ServerLevel level,
                                    CompoundTag request, BlockPos first, BlockPos second,
                                    BlockState a, BlockState b, Item item, int cost) {
        if (!clear(level, first) || !clear(level, second))
            return "PAUSE:MULTIBLOCK_SPACE_OCCUPIED";
        // Bed HEAD requires its FOOT neighbor, which does not exist until
        // the pair is placed. Validate solid support first, then verify
        // the completed pair after placement.
        if (!supported(level, first, a)
                || !(item == Items.RED_BED
                    ? floorSupported(level, second)
                    : supported(level, second, b)))
            return "PAUSE:MULTIBLOCK_SUPPORT_MISSING";
        var bag = axo.getAcquiredMaterials();
        int available = 0;
        for (int i = 0; i < bag.getContainerSize(); i++)
            if (bag.getItem(i).is(item)) available += bag.getItem(i).getCount();
        if (available < cost) {
            WaifuAxoluthuRecipeBrain.ensure(axo, item, cost-available, level.getGameTime());
            return "WAIT_MATERIALS";
        }
        if (WaifuAxoluthuBuildNavigation.approach(axo, level, first, 1.05) != null)
            return "MOVING";
        if (!axo.actionReady(WaifuAxoluthuEntity.ACTION_FOCUSED, first, 41, 80)
                || axo.getBoundingBox().intersects(new AABB(first))
                || axo.getBoundingBox().intersects(new AABB(second))) return "MOVING";
        // Reserve items before modifying the world. Roll back both blocks on failure.
        int remaining = cost;
        for (int i = 0; i < bag.getContainerSize() && remaining > 0; i++) {
            if (!bag.getItem(i).is(item)) continue;
            int take = Math.min(remaining, bag.getItem(i).getCount());
            bag.removeItem(i, take);
            remaining -= take;
        }
        bag.setChanged();
        if (!level.setBlock(first, a, 3)) {
            bag.addItem(new ItemStack(item, cost));
            return "PAUSE:MULTIBLOCK_PLACEMENT_FAILED";
        }
        if (!level.setBlock(second, b, 3)) {
            level.removeBlock(first, false);
            bag.addItem(new ItemStack(item, cost));
            return "PAUSE:MULTIBLOCK_PLACEMENT_FAILED";
        }
        axo.finishAction();
        WaifuAxoluthuBuildNavigation.completed(axo);
        advance(request, cost == 1 ? 1 : 2);
        return "CONTINUE";
    }

    private static boolean clear(ServerLevel level, BlockPos pos) {
        return pos.getY() >= level.getMinBuildHeight()
                && pos.getY() < level.getMaxBuildHeight()
                && level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                && level.getBlockState(pos).isAir()
                && level.getBlockEntity(pos) == null;
    }

    private static boolean floorSupported(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    private static boolean supported(ServerLevel level, BlockPos pos, BlockState state) {
        return level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                && state.canSurvive(level, pos)
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    private static int slot(net.minecraft.world.Container bag, Item item, int count) {
        for (int i = 0; i < bag.getContainerSize(); i++)
            if (bag.getItem(i).is(item) && bag.getItem(i).getCount() >= count) return i;
        return -1;
    }

    private static Direction rotated(Direction initial, int turns) {
        for (int i = 0; i < Math.floorMod(turns, 4); i++)
            initial = initial.getClockWise();
        return initial;
    }

    private static void advance(CompoundTag request, int count) {
        request.putInt("furnishingCursor", request.getInt("furnishingCursor") + count);
    }
}
