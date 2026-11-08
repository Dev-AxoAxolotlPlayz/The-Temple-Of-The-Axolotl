package com.devaxo.templeoftheaxolotl.abyss;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public final class AbyssExtractionAnchorBlock extends BaseEntityBlock implements EntityBlock {
    public static final MapCodec<AbyssExtractionAnchorBlock> CODEC = simpleCodec(AbyssExtractionAnchorBlock::new);
    public AbyssExtractionAnchorBlock(BlockBehaviour.Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new AbyssExtractionAnchorEntity(pos,state); }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level,pos,state,placer,stack);
        if (placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof AbyssExtractionAnchorEntity anchor) anchor.setOwner(player.getUUID());
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        if (server.dimension() != AbyssTrialEvents.ABYSS) { sp.displayClientMessage(Component.literal("The anchor only responds within the Ethereal Abyss."),true); return InteractionResult.SUCCESS; }
        if (!(server.getBlockEntity(pos) instanceof AbyssExtractionAnchorEntity anchor) || !anchor.isOwner(sp.getUUID())) {
            sp.displayClientMessage(Component.literal("This extraction anchor belongs to another explorer."),true); return InteractionResult.SUCCESS;
        }
        if (AbyssTrialEvents.extract(sp)) server.removeBlock(pos,false);
        return InteractionResult.SUCCESS;
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        useWithoutItem(state, level, pos, player, hit);
        return ItemInteractionResult.SUCCESS;
    }
}
