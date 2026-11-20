package com.devaxo.templeoftheaxolotl.pre07;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import org.jetbrains.annotations.Nullable;
public class TrialTrapSpawnerBlock extends BaseEntityBlock {
 public static final net.minecraft.world.level.block.state.properties.BooleanProperty SPENT = net.minecraft.world.level.block.state.properties.BooleanProperty.create("spent");
 public static final MapCodec<TrialTrapSpawnerBlock> CODEC = simpleCodec(TrialTrapSpawnerBlock::new);
 @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
 public TrialTrapSpawnerBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(SPENT,false));}
 @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block,BlockState> builder){builder.add(SPENT);}
 @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new TrialTrapSpawnerEntity(pos,state);}
 @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide()?null:createTickerHelper(type,Pre07BlockEntities.TRAP.get(),TrialTrapSpawnerEntity::tick);}
}
