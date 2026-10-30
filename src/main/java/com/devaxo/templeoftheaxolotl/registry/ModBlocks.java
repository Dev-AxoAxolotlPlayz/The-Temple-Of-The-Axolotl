package com.devaxo.templeoftheaxolotl.registry;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.block.TemplePortalBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** Register the portal block and its matching inventory item. */
public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(TempleOfTheAxolotl.MODID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(TempleOfTheAxolotl.MODID);
    public static void addCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(TEMPLE_PORTAL_ITEM.get());
        }
    }

    public static final DeferredBlock<TemplePortalBlock> TEMPLE_PORTAL =
            BLOCKS.register("temple_portal", () -> new TemplePortalBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.EMERALD_BLOCK)
                            .strength(-1.0F, 3600000.0F)
                            .noLootTable()));

    public static final DeferredBlock<Block> ABYSSAL_TORCH = BLOCKS.register("abyssal_torch",
            () -> new com.devaxo.templeoftheaxolotl.block.AbyssalTorchBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE).strength(2.5F).noOcclusion().lightLevel(state -> 15)));

    /** Placeable dark Celestial Anvil; vanilla anvil interaction and geometry. */
    public static final DeferredBlock<Block> CELESTIAL_ANVIL_BLOCK =
            BLOCKS.register("celestial_anvil", () -> new com.devaxo.templeoftheaxolotl.eclipse.EclipseAltarBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.ANVIL).requiresCorrectToolForDrops().lightLevel(state -> 3)));

    public static final DeferredItem<BlockItem> TEMPLE_PORTAL_ITEM =
            ITEMS.register("temple_portal", () ->
                    new BlockItem(TEMPLE_PORTAL.get(), new Item.Properties()));


    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }
}
