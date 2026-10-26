package com.devaxo.templeoftheaxolotl.brewing;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
public class CosmicBrewingBlock extends BaseEntityBlock {
 public static final MapCodec<CosmicBrewingBlock> CODEC = simpleCodec(CosmicBrewingBlock::new);
 @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
 public CosmicBrewingBlock(BlockBehaviour.Properties properties){super(properties);}
 @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
 @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,CosmicBrewingRegistries.ENTITY.get(),(lvl,pos,st,e)->e.process());}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new CosmicBrewingEntity(pos,state);}
 @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
  if(!level.isClientSide && player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof CosmicBrewingEntity){
   server.openMenu(new MenuProvider(){
    @Override public Component getDisplayName(){return Component.literal("Cosmic Brewing Stand");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new CosmicBrewingMenu(id,inv,pos);}
   },buf->buf.writeBlockPos(pos));
  }
  return InteractionResult.sidedSuccess(level.isClientSide);
 }
 @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
  if(!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof CosmicBrewingEntity e){
   net.minecraft.world.Containers.dropContents(level,pos,e.items);
  }
  super.onRemove(state,level,pos,next,moving);
 }

}
