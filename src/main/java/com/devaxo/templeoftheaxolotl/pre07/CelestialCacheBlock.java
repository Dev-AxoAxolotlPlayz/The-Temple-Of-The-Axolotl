package com.devaxo.templeoftheaxolotl.pre07;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
public class CelestialCacheBlock extends BaseEntityBlock {
 public static final MapCodec<CelestialCacheBlock> CODEC = simpleCodec(CelestialCacheBlock::new);
 @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
 public CelestialCacheBlock(Properties properties){super(properties);}
 @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new CelestialCacheEntity(pos,state);}
 @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
 @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
  if(!level.isClientSide() && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof CelestialCacheEntity cache) cache.claim(sp);
  return InteractionResult.sidedSuccess(level.isClientSide());
 }
 @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
  return level.isClientSide()?null:createTickerHelper(type,Pre07BlockEntities.CACHE.get(),CelestialCacheEntity::tick);
 }
}
