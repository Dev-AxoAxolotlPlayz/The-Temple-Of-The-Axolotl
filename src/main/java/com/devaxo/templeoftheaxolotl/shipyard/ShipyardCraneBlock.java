package com.devaxo.templeoftheaxolotl.shipyard;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.RenderShape;
import net.neoforged.neoforge.network.IContainerFactory;
public class ShipyardCraneBlock extends BaseEntityBlock {
 public static final MapCodec<ShipyardCraneBlock> CODEC = simpleCodec(ShipyardCraneBlock::new);
 public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
 public ShipyardCraneBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
 @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
 @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
 @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new ShipyardCraneBlockEntity(pos,state);}
 @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
   if(!level.isClientSide && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof ShipyardCraneBlockEntity be){
     sp.openMenu(be,pos);
   }
   return InteractionResult.sidedSuccess(level.isClientSide);
 }
 @Override @Nullable public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
   return level.isClientSide ? null : createTickerHelper(type, ShipyardRegistries.CRANE.get(), ShipyardCraneBlockEntity::serverTick);
 }
 @Override protected void onRemove(BlockState old,Level level,BlockPos pos,BlockState next,boolean moving){
   if(!old.is(next.getBlock()) && level.getBlockEntity(pos) instanceof ShipyardCraneBlockEntity be){
     be.clearCargo();
     net.minecraft.world.Containers.dropContents(level,pos,be.items());
     level.updateNeighbourForOutputSignal(pos,this);
   }
   super.onRemove(old,level,pos,next,moving);
 }
}
