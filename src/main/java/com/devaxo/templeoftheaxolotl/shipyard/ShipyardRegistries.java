
package com.devaxo.templeoftheaxolotl.shipyard;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.registry.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

public final class ShipyardRegistries {

 private ShipyardRegistries() {}

 public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
         DeferredRegister.create(
                 Registries.BLOCK_ENTITY_TYPE,
                 TempleOfTheAxolotl.MODID
         );

 public static final DeferredHolder<
         BlockEntityType<?>,
         BlockEntityType<ShipyardCraneBlockEntity>
         > CRANE = BLOCK_ENTITIES.register(
         "shipyard_crane",
         () -> BlockEntityType.Builder.of(
                 ShipyardCraneBlockEntity::new,
                 ModBlocks.SHIPYARD_CRANE.get()
         ).build(null)
 );

 public static final DeferredRegister<MenuType<?>> MENUS =
         DeferredRegister.create(
                 Registries.MENU,
                 TempleOfTheAxolotl.MODID
         );

 public static final DeferredHolder<
         MenuType<?>,
         MenuType<ShipyardMenu>
         > CRANE_MENU = MENUS.register(
         "shipyard_crane",
         () -> IMenuTypeExtension.create(
                 (id, inv, buf) ->
                         new ShipyardMenu(id, inv, buf.readBlockPos())
         )
 );
}
