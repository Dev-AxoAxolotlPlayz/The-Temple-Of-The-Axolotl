package com.devaxo.templeoftheaxolotl.lore;

import com.devaxo.templeoftheaxolotl.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Separate Abyss registry, sharing Diary block-entity renderer/model/texture. */
public final class AbyssalLoreBlock extends BaseEntityBlock {
    public static final MapCodec<AbyssalLoreBlock> CODEC = simpleCodec(p -> new AbyssalLoreBlock(p, false));
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final boolean broken;
    public AbyssalLoreBlock(Properties properties, boolean broken) {
        super(properties); this.broken = broken;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> b) { b.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()); }
    @Override public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) { return rotate(state, mirror.getRotation(state.getValue(FACING))); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LoreBlockEntity(pos, state); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer server)) return InteractionResult.PASS;
        if (broken) {
            server.sendSystemMessage(Component.literal("The Depths hide more than this can hold"));
            return InteractionResult.SUCCESS;
        }
        int id = AbyssalLoreIndex.unlockNext(server);
        if (id < 0) server.sendSystemMessage(Component.literal("All Abyssal lore entries are already discovered."));
        else {
            server.sendSystemMessage(Component.literal("Abyssal discovery: " + AbyssalLoreIndex.ENTRIES[id]));
            if (!broken) level.setBlock(pos, ModBlocks.BROKEN_ABYSSAL_LORE_BLOCK.get().defaultBlockState().setValue(FACING, state.getValue(FACING)), 3);
        }
        return InteractionResult.SUCCESS;
    }
}
