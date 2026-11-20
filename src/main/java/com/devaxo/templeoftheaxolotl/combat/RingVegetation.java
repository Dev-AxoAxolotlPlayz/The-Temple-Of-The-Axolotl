package com.devaxo.templeoftheaxolotl.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Explicit whitelist: never use generic hardness or replaceability as permission. */
final class RingVegetation {
    private RingVegetation() {}
    static boolean preservedBase(ServerPlayer p,BlockPos pos,BlockState state) {
        return RingMiningMode.sustainable(p) && (state.is(Blocks.BAMBOO)||state.is(Blocks.SUGAR_CANE))
                && !p.serverLevel().getBlockState(pos.below()).is(state.getBlock());
    }
    static void clear(ServerPlayer player, BlockPos pos) {
        var level=player.serverLevel();
        BlockState state=level.getBlockState(pos);
        Block b=state.getBlock();
        if (b==Blocks.BAMBOO || b==Blocks.SUGAR_CANE) {
            BlockPos base=pos;
            for(int i=0;i<256 && level.getBlockState(base.below()).is(b);i++) base=base.below();
            if (RingMiningMode.sustainable(player)) {
                if (level.getBlockState(base.above()).is(b)) remove(player,base.above());
            } else {
                if (!player.mayInteract(level,base) || !player.mayInteract(level,pos)) return;
                remove(player,base);
            }
        } else if (state.is(net.minecraft.tags.BlockTags.SMALL_FLOWERS) || state.is(net.minecraft.tags.BlockTags.TALL_FLOWERS) || b==Blocks.SHORT_GRASS || b==Blocks.TALL_GRASS
                || b==Blocks.FERN || b==Blocks.LARGE_FERN || b==Blocks.AZALEA || b==Blocks.FLOWERING_AZALEA
                || b==Blocks.CAVE_VINES || b==Blocks.CAVE_VINES_PLANT || b==Blocks.SWEET_BERRY_BUSH
                || b==Blocks.VINE || b==Blocks.WEEPING_VINES || b==Blocks.WEEPING_VINES_PLANT
                || b==Blocks.TWISTING_VINES || b==Blocks.TWISTING_VINES_PLANT || b==Blocks.SCAFFOLDING
                || b==Blocks.BAMBOO_SAPLING) remove(player,pos);
    }
    private static void remove(ServerPlayer player,BlockPos pos) {
        var level=player.serverLevel();
        if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4) || !level.getWorldBorder().isWithinBounds(pos) || !player.mayInteract(level,pos)
                || !player.mayUseItemAt(pos,net.minecraft.core.Direction.UP,player.getMainHandItem()) || player.isSpectator() || player.getAbilities().mayBuild==false) return;
        var state=level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level,pos)<0) return;
        if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level,pos,state,player)).isCanceled()) return;
        if (level.getBlockState(pos)!=state) return;
        var blockEntity=level.getBlockEntity(pos);
        if (level.destroyBlock(pos,false,player) && !player.isCreative())
            Block.dropResources(state,level,pos,blockEntity,player,player.getMainHandItem());
    }
}
