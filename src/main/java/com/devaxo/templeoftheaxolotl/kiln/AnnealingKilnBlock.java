package com.devaxo.templeoftheaxolotl.kiln;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
public class AnnealingKilnBlock extends BaseEntityBlock {
 public static final MapCodec<AnnealingKilnBlock> CODEC=simpleCodec(AnnealingKilnBlock::new);
 public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
 public AnnealingKilnBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
 @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
 @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
 @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new AnnealingKilnEntity(pos,state);}
 @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
  if(!level.isClientSide && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof AnnealingKilnEntity be)sp.openMenu(be,pos);
  return InteractionResult.sidedSuccess(level.isClientSide);
 }
 @Override @Nullable public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,KilnRegistries.KILN.get(),AnnealingKilnEntity::tick);}
 @Override protected void onRemove(BlockState old,Level level,BlockPos pos,BlockState next,boolean moving){
  if(!old.is(next.getBlock()) && level.getBlockEntity(pos) instanceof AnnealingKilnEntity be){be.cancelAndRefund();Containers.dropContents(level,pos,be.items());}
  super.onRemove(old,level,pos,next,moving);
 }
}
