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
            event.accept(SHALLOWS_PORTAL_ITEM.get());
            event.accept(SHALLOWS_EXIT_PORTAL_ITEM.get());
            event.accept(LORE_BLOCK_ITEM.get());
            event.accept(BROKEN_LORE_BLOCK_ITEM.get());
            event.accept(DIARY_ITEM.get());
            event.accept(FIELD_GUIDE_ITEM.get());
        }
    }

    // Stage-one Annealing Kiln: placeable cyan workstation shell; inventory/processing follows in next phase.
    public static final DeferredBlock<Block> ANNEALING_KILN = BLOCKS.register("annealing_kiln",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.COLOR_CYAN).strength(3.5F).requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> ANNEALING_KILN_ITEM = ITEMS.register("annealing_kiln",
            () -> new BlockItem(ANNEALING_KILN.get(), new Item.Properties()));

    /** Supernova-treated bedrock; mined through the ring's 600-tick server-controlled ritual. */
    public static final DeferredBlock<Block> STAR_BLASTED_BEDROCK =
            BLOCKS.register("star_blasted_bedrock", () -> new Block(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK)
                            .strength(1000000.0F, 3600000.0F).noLootTable()));

    public static final DeferredBlock<TemplePortalBlock> TEMPLE_PORTAL =
            BLOCKS.register("temple_portal", () -> new TemplePortalBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.EMERALD_BLOCK)
                            .strength(-1.0F, 3600000.0F)
                            .noLootTable()));

    public static final DeferredBlock<com.devaxo.templeoftheaxolotl.block.ShallowsPortalBlock> SHALLOWS_PORTAL =
            BLOCKS.register("shallows_portal", () -> new com.devaxo.templeoftheaxolotl.block.ShallowsPortalBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.LAPIS_BLOCK).strength(-1.0F, 3600000.0F).noLootTable(), false));
    public static final DeferredBlock<com.devaxo.templeoftheaxolotl.block.ShallowsPortalBlock> SHALLOWS_EXIT_PORTAL =
            BLOCKS.register("shallows_exit_portal", () -> new com.devaxo.templeoftheaxolotl.block.ShallowsPortalBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_BLOCK).strength(-1.0F, 3600000.0F).noLootTable(), true));
    public static final DeferredItem<BlockItem> SHALLOWS_PORTAL_ITEM = ITEMS.register("shallows_portal",
            () -> new BlockItem(SHALLOWS_PORTAL.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> SHALLOWS_EXIT_PORTAL_ITEM = ITEMS.register("shallows_exit_portal",
            () -> new BlockItem(SHALLOWS_EXIT_PORTAL.get(), new Item.Properties()));

    public static final DeferredBlock<Block> ABYSSAL_TORCH = BLOCKS.register("abyssal_torch",
            () -> new com.devaxo.templeoftheaxolotl.block.AbyssalTorchBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE).strength(2.5F).noOcclusion().lightLevel(state -> 15)));

    public static final DeferredBlock<com.devaxo.templeoftheaxolotl.abyss.WatergateBlock> ABYSSAL_WATERGATE =
            BLOCKS.register("abyssal_watergate", () -> new com.devaxo.templeoftheaxolotl.abyss.WatergateBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.PRISMARINE).noOcclusion().strength(-1.0F, 3600000.0F).noLootTable()));
    // Invisible solid blocks for the Watergate's two pillars and top lintel.
    public static final DeferredBlock<com.devaxo.templeoftheaxolotl.abyss.WatergateFrameBlock> ABYSSAL_WATERGATE_FRAME =
            BLOCKS.register("abyssal_watergate_frame", () -> new com.devaxo.templeoftheaxolotl.abyss.WatergateFrameBlock(
                    BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.NONE).strength(-1.0F, 3600000.0F).noOcclusion().noLootTable()));

    public static final DeferredItem<BlockItem> ABYSSAL_WATERGATE_ITEM = ITEMS.register("abyssal_watergate",
            () -> new BlockItem(ABYSSAL_WATERGATE.get(), new Item.Properties()));

    /** Placeable dark Celestial Anvil; vanilla anvil interaction and geometry. */
    public static final DeferredBlock<Block> CELESTIAL_ANVIL_BLOCK =
            BLOCKS.register("celestial_anvil", () -> new com.devaxo.templeoftheaxolotl.eclipse.EclipseAltarBlock(
                    BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(5.0F, 1200.0F).requiresCorrectToolForDrops().lightLevel(state -> 3)));

    // One-use lore sources: the intact version transforms into the globally depleted variant.
    public static final DeferredBlock<com.devaxo.templeoftheaxolotl.lore.LoreBlock> LORE_BLOCK =
            BLOCKS.register("lore_block", () -> new com.devaxo.templeoftheaxolotl.lore.LoreBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).noOcclusion().strength(-1.0F, 3600000.0F).noLootTable()));
    public static final DeferredBlock<com.devaxo.templeoftheaxolotl.lore.BrokenLoreBlock> BROKEN_LORE_BLOCK =
            BLOCKS.register("broken_lore_block", () -> new com.devaxo.templeoftheaxolotl.lore.BrokenLoreBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).noOcclusion().strength(-1.0F, 3600000.0F).noLootTable()));
    public static final DeferredItem<com.devaxo.templeoftheaxolotl.lore.LoreBlockItem> LORE_BLOCK_ITEM = ITEMS.register("lore_block",
            () -> new com.devaxo.templeoftheaxolotl.lore.LoreBlockItem(LORE_BLOCK.get(), new Item.Properties(), false));
    public static final DeferredItem<com.devaxo.templeoftheaxolotl.lore.LoreBlockItem> BROKEN_LORE_BLOCK_ITEM = ITEMS.register("broken_lore_block",
            () -> new com.devaxo.templeoftheaxolotl.lore.LoreBlockItem(BROKEN_LORE_BLOCK.get(), new Item.Properties(), true));

    public static final DeferredItem<com.devaxo.templeoftheaxolotl.guide.FieldGuideItem> FIELD_GUIDE_ITEM =
            ITEMS.register("axolotls_field_guide", () -> new com.devaxo.templeoftheaxolotl.guide.FieldGuideItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<com.devaxo.templeoftheaxolotl.lore.DiaryItem> DIARY_ITEM =
            ITEMS.register("diary_of_an_axolotl", () -> new com.devaxo.templeoftheaxolotl.lore.DiaryItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<BlockItem> TEMPLE_PORTAL_ITEM =
            ITEMS.register("temple_portal", () ->
                    new BlockItem(TEMPLE_PORTAL.get(), new Item.Properties()));


    public static final DeferredBlock<com.devaxo.templeoftheaxolotl.shipyard.ShipyardCraneBlock> SHIPYARD_CRANE =
            BLOCKS.register("shipyard_crane", () -> new com.devaxo.templeoftheaxolotl.shipyard.ShipyardCraneBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.WARPED_HYPHAE).strength(4.0F).noOcclusion()));
    public static final DeferredItem<BlockItem> SHIPYARD_CRANE_ITEM = ITEMS.register("shipyard_crane",
            () -> new BlockItem(SHIPYARD_CRANE.get(), new Item.Properties()));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }
}
