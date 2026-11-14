package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Furnishes only the owner-approved exact-site workshop.
 * All positions are inside its 7x7 shell, above the cobblestone floor.
 */
public final class WaifuAxoluthuWorkshopFurnishing {
    private WaifuAxoluthuWorkshopFurnishing() {}

    private record Fixture(int x, int z, Block block, Item item) {}
    private static final Fixture[] FIXTURES = {
        new Fixture(1, 1, Blocks.GRINDSTONE, Items.GRINDSTONE),
        new Fixture(5, 1, Blocks.COMPOSTER, Items.COMPOSTER),
        new Fixture(1, 5, Blocks.BREWING_STAND, Items.BREWING_STAND),
        new Fixture(5, 5, Blocks.CRAFTING_TABLE, Items.CRAFTING_TABLE)
    };

    /** The shared catalog mirrors this workshop's four functional fixtures. */
    public static net.minecraft.nbt.ListTag plannedFixtures(BlockPos origin, int quarterTurns) {
        return WaifuAxoluthuFurnishingLayouts.plan("workshop", origin, quarterTurns);
    }

    public static String step(WaifuAxoluthuEntity axo, ServerLevel level, CompoundTag request) {
        if (!request.getBoolean("designApproved") || !request.getBoolean("constructionAllowed")
                || !"workshop".equals(request.getString("type"))
                || !WaifuAxoluthuExactSiteBuilder.validWorkshop(request))
            return "PAUSE:APPROVAL_OR_BLUEPRINT_INVALID";
        int cursor = request.getInt("furnishingCursor");
        if (cursor >= FIXTURES.length) return "COMPLETE";
        Fixture fixture = FIXTURES[cursor];
        BlockPos origin = BlockPos.of(request.getLong("target"));
        // The palette rotates the shell around its origin in the proposal.
        int turns = Math.floorMod(request.getInt("quarterTurns"), 4);
        int x = fixture.x, z = fixture.z;
        for (int i = 0; i < turns; i++) {
            int nextX = -z;
            z = x;
            x = nextX;
        }
        BlockPos pos = origin.offset(x, 1, z);
        if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()
                || !level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4))
            return "PAUSE:UNLOADED_FURNISHING_SITE";
        BlockState current = level.getBlockState(pos);
        if (current.is(fixture.block)) {
            request.putInt("furnishingCursor", cursor + 1);
            return "CONTINUE";
        }
        if (!current.isAir() || level.getBlockEntity(pos) != null)
            return "PAUSE:FURNISHING_SPACE_OCCUPIED_" + pos.toShortString();
        if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(),
                net.minecraft.core.Direction.UP))
            return "PAUSE:UNSUPPORTED_FURNISHING_" + pos.toShortString();
        var bag = axo.getAcquiredMaterials();
        int slot = -1;
        for (int i = 0; i < bag.getContainerSize(); i++)
            if (bag.getItem(i).is(fixture.item)) { slot = i; break; }
        if (slot < 0) {
            WaifuAxoluthuRecipeBrain.ensure(axo, fixture.item, 1, level.getGameTime());
            return "WAIT_MATERIALS";
        }
        if (WaifuAxoluthuBuildNavigation.approach(axo, level, pos, 1.05) != null)
            return "MOVING";
        if (!axo.actionReady(WaifuAxoluthuEntity.ACTION_FOCUSED, pos, 41, 80)
                || axo.getBoundingBox().intersects(new AABB(pos))) return "MOVING";
        ItemStack removed = bag.removeItem(slot, 1);
        bag.setChanged();
        if (removed.isEmpty()) return "WAIT_MATERIALS";
        BlockState desired = fixture.block.defaultBlockState();
        if (!desired.canSurvive(level, pos) || !level.setBlock(pos, desired, 3)) {
            bag.addItem(removed);
            return "PAUSE:FURNISHING_PLACEMENT_FAILED";
        }
        axo.finishAction();
        WaifuAxoluthuBuildNavigation.completed(axo);
        request.putInt("furnishingCursor", cursor + 1);
        return "CONTINUE";
    }
}
