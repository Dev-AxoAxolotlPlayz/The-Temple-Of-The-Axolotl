package com.devaxo.templeoftheaxolotl.pre07;
import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.*;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.*;
import java.util.function.Supplier;
/** Lava-paced fluids: 30 tick flow delay, 2-block spread, 4-tick slope search. */
public final class Pre07Fluids {
 private static final class IchorFluidType extends FluidType {
  private IchorFluidType(Properties properties, int tint) { super(properties); }

 }
 public static final DeferredRegister<FluidType> TYPES=DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.FLUID_TYPES,TempleOfTheAxolotl.MODID);
 public static final DeferredRegister<Fluid> FLUIDS=DeferredRegister.create(Registries.FLUID,TempleOfTheAxolotl.MODID);
 public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(TempleOfTheAxolotl.MODID);
 public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(TempleOfTheAxolotl.MODID);
 public static final DeferredHolder<FluidType,FluidType> PURE_TYPE=TYPES.register("pure_ichor_type",()->new IchorFluidType(FluidType.Properties.create().density(3000).viscosity(6000).lightLevel(12).canSwim(true).canDrown(false), 0xFFFFC936));
 public static final DeferredHolder<FluidType,FluidType> DILUTED_TYPE=TYPES.register("diluted_ichor_type",()->new IchorFluidType(FluidType.Properties.create().density(3000).viscosity(6000).lightLevel(8).canSwim(true).canDrown(false), 0xFFF3E2A6));
 public static final DeferredHolder<Fluid,BaseFlowingFluid.Source> PURE=FLUIDS.register("pure_ichor",()->new BaseFlowingFluid.Source(pureProps()));
 public static final DeferredHolder<Fluid,BaseFlowingFluid.Flowing> PURE_FLOWING=FLUIDS.register("flowing_pure_ichor",()->new BaseFlowingFluid.Flowing(pureProps()));
 public static final DeferredHolder<Fluid,BaseFlowingFluid.Source> DILUTED=FLUIDS.register("diluted_ichor",()->new BaseFlowingFluid.Source(dilutedProps()));
 public static final DeferredHolder<Fluid,BaseFlowingFluid.Flowing> DILUTED_FLOWING=FLUIDS.register("flowing_diluted_ichor",()->new BaseFlowingFluid.Flowing(dilutedProps()));
 public static final DeferredBlock<LiquidBlock> PURE_BLOCK=BLOCKS.register("pure_ichor",()->new LiquidBlock(PURE.get(),BlockBehaviour.Properties.ofFullCopy(Blocks.LAVA).lightLevel(s->12)));
 public static final DeferredBlock<LiquidBlock> DILUTED_BLOCK=BLOCKS.register("diluted_ichor",()->new DecayingDilutedIchorBlock(DILUTED.get(),BlockBehaviour.Properties.ofFullCopy(Blocks.LAVA).lightLevel(s->8)));
 public static final DeferredItem<Item> PURE_BUCKET=ITEMS.register("pure_ichor_bucket",()->new BucketItem(PURE.get(),new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
 public static final DeferredItem<Item> DILUTED_BUCKET=ITEMS.register("diluted_ichor_bucket",()->new DrinkableDilutedIchorBucket(DILUTED.get(),new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
 private static BaseFlowingFluid.Properties pureProps(){return new BaseFlowingFluid.Properties(PURE_TYPE,PURE,PURE_FLOWING).block(PURE_BLOCK).bucket(PURE_BUCKET).slopeFindDistance(2).levelDecreasePerBlock(2).tickRate(30);}
 private static BaseFlowingFluid.Properties dilutedProps(){return new BaseFlowingFluid.Properties(DILUTED_TYPE,DILUTED,DILUTED_FLOWING).block(DILUTED_BLOCK).bucket(DILUTED_BUCKET).slopeFindDistance(2).levelDecreasePerBlock(2).tickRate(30);}
}
