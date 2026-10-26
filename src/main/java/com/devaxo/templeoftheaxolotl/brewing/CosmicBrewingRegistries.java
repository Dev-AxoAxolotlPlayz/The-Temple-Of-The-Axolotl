package com.devaxo.templeoftheaxolotl.brewing;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
public final class CosmicBrewingRegistries {
 private CosmicBrewingRegistries() {}
 public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(TempleOfTheAxolotl.MODID);
 public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(TempleOfTheAxolotl.MODID);
 public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,TempleOfTheAxolotl.MODID);
 public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,TempleOfTheAxolotl.MODID);
 public static final DeferredBlock<CosmicBrewingBlock> STAND=BLOCKS.register("cosmic_brewing_stand",()->new CosmicBrewingBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(3.5f).lightLevel(s->9).requiresCorrectToolForDrops()));
 public static final DeferredItem<BlockItem> STAND_ITEM=ITEMS.register("cosmic_brewing_stand",()->new BlockItem(STAND.get(),new Item.Properties()));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<CosmicBrewingEntity>> ENTITY=ENTITIES.register("cosmic_brewing_stand",()->BlockEntityType.Builder.of(CosmicBrewingEntity::new,STAND.get()).build(null));
 public static final DeferredHolder<MenuType<?>,MenuType<CosmicBrewingMenu>> MENU=MENUS.register("cosmic_brewing_stand",()->IMenuTypeExtension.create((id,inv,data)->new CosmicBrewingMenu(id,inv,data.readBlockPos())));
}
