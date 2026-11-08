package com.devaxo.templeoftheaxolotl.kiln;
import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.registry.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
public final class KilnRegistries {
 public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,TempleOfTheAxolotl.MODID);
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<AnnealingKilnEntity>> KILN=ENTITIES.register("annealing_kiln",()->BlockEntityType.Builder.of(AnnealingKilnEntity::new,ModBlocks.ANNEALING_KILN.get()).build(null));
 public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,TempleOfTheAxolotl.MODID);
 public static final DeferredHolder<MenuType<?>,MenuType<AnnealingKilnMenu>> MENU=MENUS.register("annealing_kiln",()->IMenuTypeExtension.create((id,inv,buf)->new AnnealingKilnMenu(id,inv,buf.readBlockPos())));
}
