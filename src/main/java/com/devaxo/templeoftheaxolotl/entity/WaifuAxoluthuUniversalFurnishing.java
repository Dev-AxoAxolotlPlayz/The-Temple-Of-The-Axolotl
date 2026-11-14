package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * Inventory-backed single-block furnishing installer.
 * Multi-block furniture (beds, paired chests) is deliberately held for
 * specialized placement to avoid broken or unpaired world states.
 */
public final class WaifuAxoluthuUniversalFurnishing {
    private WaifuAxoluthuUniversalFurnishing() {}

    public static String step(WaifuAxoluthuEntity axo, ServerLevel level, CompoundTag request) {
        if (!request.getBoolean("designApproved") || !request.getBoolean("constructionAllowed")
                || !WaifuAxoluthuExactSiteBuilder.validWorkshop(request))
            return "PAUSE:INVALID_APPROVAL";
        String type = WaifuAxoluthuVillageStructures.normalize(request.getString("type"));
        if (type.isEmpty()) return "PAUSE:UNKNOWN_BUILDING";
        BlockPos origin = BlockPos.of(request.getLong("target"));
        ListTag fixtures = WaifuAxoluthuFurnishingLayouts.plan(request.getCompound("blueprintPreview"), origin,
                request.getInt("quarterTurns"));
        int cursor = request.getInt("furnishingCursor");
        if (cursor >= fixtures.size()) {
            return "COMPLETE";
        }
        CompoundTag entry = fixtures.getCompound(cursor);
        String id = entry.getString("block");
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || !key.getNamespace().equals("minecraft")
                || !BuiltInRegistries.BLOCK.containsKey(key))
            return "PAUSE:INVALID_FURNISHING_ID";
        Block block = BuiltInRegistries.BLOCK.get(key);
        if (block == Blocks.AIR) return "PAUSE:INVALID_FURNISHING";
        if (WaifuAxoluthuWorkstationHandoff.isHandoffBlock(block)) {
            return WaifuAxoluthuWorkstationHandoff.offer(axo, level, request, block, cursor);
        }
        // Player-led furnishing: do not auto-place decorative fixtures.
        // Keep the original plan available for comparison at final approval.
        request.putInt("furnishingCursor", cursor + 1);
        return "CONTINUE";
    }

    private static String placeDoor(WaifuAxoluthuEntity axo, ServerLevel level,
                                    CompoundTag request, BlockPos lower, boolean rightHinge) {
        BlockPos upper = lower.above();
        if (!level.getChunkSource().hasChunk(lower.getX() >> 4, lower.getZ() >> 4)
                || upper.getY() >= level.getMaxBuildHeight()) return "PAUSE:DOOR_CHUNK_UNAVAILABLE";
        Direction front = Direction.NORTH;
        for (int i = 0; i < Math.floorMod(request.getInt("quarterTurns"), 4); i++)
            front = front.getClockWise();
        BlockState bottom = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(net.minecraft.world.level.block.DoorBlock.FACING, front)
                .setValue(net.minecraft.world.level.block.DoorBlock.HINGE, rightHinge
                        ? net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT
                        : net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT)
                .setValue(net.minecraft.world.level.block.DoorBlock.HALF,
                        net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
        BlockState top = bottom.setValue(net.minecraft.world.level.block.DoorBlock.HALF,
                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);
        if (level.getBlockState(lower).equals(bottom) && level.getBlockState(upper).equals(top)) {
            request.putInt("furnishingCursor", request.getInt("furnishingCursor") + 1);
            return "CONTINUE";
        }
        if (!level.getBlockState(lower).isAir() && !level.getBlockState(lower).is(Blocks.OAK_DOOR)
                || !level.getBlockState(upper).isAir() && !level.getBlockState(upper).is(Blocks.OAK_DOOR))
            return "PAUSE:DOOR_ENTRANCE_OCCUPIED";
        var bag = axo.getAcquiredMaterials();
        int slot = -1;
        for (int i = 0; i < bag.getContainerSize(); i++)
            if (bag.getItem(i).is(net.minecraft.world.item.Items.OAK_DOOR)) { slot = i; break; }
        if (slot < 0) {
            WaifuAxoluthuRecipeBrain.ensure(axo, net.minecraft.world.item.Items.OAK_DOOR, 1, level.getGameTime());
            return "WAIT_MATERIALS";
        }
        if (WaifuAxoluthuBuildNavigation.approach(axo, level, lower, 1.5) != null)
            return "MOVING";
        if (!axo.actionReady(WaifuAxoluthuEntity.ACTION_FOCUSED, lower, 41, 80))
            return "MOVING";
        BlockState oldBottom = level.getBlockState(lower);
        if (!level.setBlock(lower, bottom, 3)) return "PAUSE:DOOR_LOWER_PLACEMENT_FAILED";
        if (!level.setBlock(upper, top, 3)) {
            level.setBlock(lower, oldBottom, 3);
            return "PAUSE:DOOR_UPPER_PLACEMENT_FAILED";
        }
        bag.removeItem(slot, 1);
        bag.setChanged();
        axo.finishAction();
        WaifuAxoluthuBuildNavigation.completed(axo);
        request.putInt("furnishingCursor", request.getInt("furnishingCursor") + 1);
        return "CONTINUE";
    }

}
