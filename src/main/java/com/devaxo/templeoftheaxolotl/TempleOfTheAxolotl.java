package com.devaxo.templeoftheaxolotl;

import com.devaxo.templeoftheaxolotl.effect.ModEffects;
import com.devaxo.templeoftheaxolotl.pre07.*;
import com.devaxo.templeoftheaxolotl.compass.TempleSeekerEvents;
import com.devaxo.templeoftheaxolotl.worldgen.TempleWorldgenPlacements;
import com.devaxo.templeoftheaxolotl.entity.ModEntities;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import com.devaxo.templeoftheaxolotl.loot.ModLootModifiers;
import com.devaxo.templeoftheaxolotl.loot.DungeonRewardEvents;
import com.devaxo.templeoftheaxolotl.loot.DungeonRewardDevCommands;
import com.devaxo.templeoftheaxolotl.maze.AxoMazeCommands;
import com.devaxo.templeoftheaxolotl.traps.ModTrapBlocks;
import com.devaxo.templeoftheaxolotl.traps.ModTrapItems;
import net.neoforged.neoforge.common.NeoForge;
import com.devaxo.templeoftheaxolotl.maze.TempleDimensionManager;
import com.devaxo.templeoftheaxolotl.maze.TempleTravel;
import com.mojang.logging.LogUtils;
import com.devaxo.templeoftheaxolotl.registry.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import org.slf4j.Logger;
import com.devaxo.templeoftheaxolotl.traps.TrapRuntime;
import com.devaxo.templeoftheaxolotl.loot.TempleRewardMenuTypes;

@Mod(TempleOfTheAxolotl.MODID)
public class TempleOfTheAxolotl {

    public static final String MODID = "temple_of_the_axolotl";
    public static final Logger LOGGER = LogUtils.getLogger();

    // =========================================
    // CREATIVE MODE TAB
    // =========================================

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> AXO_TAB =
            CREATIVE_MODE_TABS.register("axo_tab", () ->
                    CreativeModeTab.builder()
                            .title(Component.literal("Temple of the Axolotl"))

                            .icon(() ->
                                    ModItems.AXOLOTL_TAB_ICON
                                            .get()
                                            .getDefaultInstance()
                            )

                            .displayItems((parameters, output) -> {

                                // Existing mod items
                                ModItems.addToCreativeTab(output);

                                // Temple portal (custom emerald-looking block)
                                output.accept(ModBlocks.TEMPLE_PORTAL_ITEM.get());
                                output.accept(ModBlocks.SHALLOWS_PORTAL_ITEM.get());
                                output.accept(ModBlocks.SHALLOWS_EXIT_PORTAL_ITEM.get());
                output.accept(ModBlocks.LORE_BLOCK_ITEM.get());
                output.accept(ModBlocks.BROKEN_LORE_BLOCK_ITEM.get());
                                output.accept(ModBlocks.DIARY_ITEM.get());
                                output.accept(ModBlocks.FIELD_GUIDE_ITEM.get());
                                output.accept(ModBlocks.SHIPYARD_CRANE_ITEM.get());
                                output.accept(ModBlocks.ANNEALING_KILN_ITEM.get());
                                output.accept(ModBlocks.ABYSSAL_WATERGATE_ITEM.get());
                                output.accept(com.devaxo.templeoftheaxolotl.brewing.CosmicBrewingRegistries.STAND_ITEM.get());

                                // =====================================
                                // THE FOUR CURSED FLOOR BRICKS
                                // =====================================

                                output.accept(ModTrapItems.RNGESUS_TRAP.get());
                                output.accept(ModTrapItems.SPIKE_TRAP.get());
                                output.accept(ModTrapItems.HONEYBRICK_TRAP.get());
                                output.accept(ModTrapItems.SNEAK_TRAP.get());
                                output.accept(Pre07Items.CELESTIAL_CACHE.get());
                                output.accept(Pre07Fluids.DILUTED_BUCKET.get());
                                output.accept(Pre07Items.TRIAL_TRAP_SPAWNER.get());
                                for(var item:Pre07Items.STONES) output.accept(item.get());
                                for(var item:Pre07Items.CRACKED) output.accept(item.get());
                            })
                            .build()
            );

    // =========================================
    // MOD INITIALIZATION
    // =========================================

    public TempleOfTheAxolotl(
            IEventBus modEventBus,
            ModContainer modContainer
    ) {

        // Register custom spawn-relative temple structure placement.
        com.devaxo.templeoftheaxolotl.abyss.WatergateEntities.TYPES.register(modEventBus);
        TempleWorldgenPlacements.PLACEMENTS.register(modEventBus);
        com.devaxo.templeoftheaxolotl.worldgen.ShallowsRuinStructures.STRUCTURES.register(modEventBus);
        com.devaxo.templeoftheaxolotl.worldgen.ShallowsRuinPieceTypes.TYPES.register(modEventBus);
        TempleRewardMenuTypes.MENUS.register(modEventBus);
        com.devaxo.templeoftheaxolotl.brewing.CosmicBrewingRegistries.BLOCKS.register(modEventBus);
        com.devaxo.templeoftheaxolotl.brewing.CosmicBrewingRegistries.ITEMS.register(modEventBus);
        com.devaxo.templeoftheaxolotl.brewing.CosmicBrewingRegistries.ENTITIES.register(modEventBus);
        com.devaxo.templeoftheaxolotl.brewing.CosmicBrewingRegistries.MENUS.register(modEventBus);
        com.devaxo.templeoftheaxolotl.eclipse.EclipseMenus.MENUS.register(modEventBus);
        com.devaxo.templeoftheaxolotl.eclipse.EclipseBlockEntities.TYPES.register(modEventBus);
        com.devaxo.templeoftheaxolotl.compass.CelestialModulesMenuTypes.MENUS.register(modEventBus);
        com.devaxo.templeoftheaxolotl.repository.CelestialRepositoryMenuTypes.MENUS.register(modEventBus);

        // Entity registration
        ModEntities.ENTITY_TYPES.register(modEventBus);

        // Temple portal block and item registration (register once)
        ModBlocks.register(modEventBus);
        com.devaxo.templeoftheaxolotl.shipyard.ShipyardRegistries.BLOCK_ENTITIES.register(modEventBus);
        com.devaxo.templeoftheaxolotl.shipyard.ShipyardRegistries.MENUS.register(modEventBus);
        com.devaxo.templeoftheaxolotl.lore.LoreBlockEntities.TYPES.register(modEventBus);
        NeoForge.EVENT_BUS.register(new com.devaxo.templeoftheaxolotl.abyss.AbyssTrialEvents());
        NeoForge.EVENT_BUS.register(new com.devaxo.templeoftheaxolotl.abyss.AbyssTerrainEvents());

        // Item registration
        ModItems.ITEMS.register(modEventBus);
        com.devaxo.templeoftheaxolotl.sound.CelestialMusicSounds.SOUNDS.register(modEventBus);

        // Floor trap block and item registration
        ModTrapBlocks.BLOCKS.register(modEventBus);
        ModTrapItems.ITEMS.register(modEventBus);
        Pre07Blocks.BLOCKS.register(modEventBus);
        Pre07Items.ITEMS.register(modEventBus);
        Pre07BlockEntities.TYPES.register(modEventBus);
        Pre07Fluids.TYPES.register(modEventBus);
        Pre07Fluids.FLUIDS.register(modEventBus);
        Pre07Fluids.BLOCKS.register(modEventBus);
        Pre07Fluids.ITEMS.register(modEventBus);

        // Mob effect registration
        ModEffects.EFFECTS.register(modEventBus);

        // Loot modifier registration
        ModLootModifiers.SERIALIZERS.register(modEventBus);

        // Creative tab registration
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register formerly annotation-driven payload handlers on the MOD bus.
        modEventBus.addListener(com.devaxo.templeoftheaxolotl.compass.TempleNavigationPayload.Registration::register);
        modEventBus.addListener(com.devaxo.templeoftheaxolotl.eclipse.EclipseInscriptionPayload.Registration::register);
        modEventBus.addListener(com.devaxo.templeoftheaxolotl.network.InventoryStorageOpenPayload.Registration::register);
        modEventBus.addListener(com.devaxo.templeoftheaxolotl.network.ShorkolotlHeadlightPayload.Registration::register);
        modEventBus.addListener(com.devaxo.templeoftheaxolotl.network.RepositoryRecipeFillPayload::register);

        // Explicit mod-bus lifecycle/network registrations (no deprecated annotation bus selectors).
        modEventBus.addListener(com.devaxo.templeoftheaxolotl.network.BedrockMiningPayload.Registration::register);
        modEventBus.addListener(com.devaxo.templeoftheaxolotl.network.CelestialJumpNetworking.Registration::registerPayloads);
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
            modEventBus.addListener(com.devaxo.templeoftheaxolotl.compass.TempleSeekerClient::onClientSetup);
            modEventBus.addListener(com.devaxo.templeoftheaxolotl.pre07.Pre07FluidClientExtensions::register);
            modEventBus.addListener(com.devaxo.templeoftheaxolotl.client.TempleRewardScreenRegistration::register);
            // Diary formatting must register before Patchouli parses the book.
            com.devaxo.templeoftheaxolotl.client.DiaryFontRegistration.register();
            modEventBus.addListener(com.devaxo.templeoftheaxolotl.eclipse.EclipseScreenRegistration::register);
            modEventBus.addListener(com.devaxo.templeoftheaxolotl.shipyard.ShipyardClient::screens);
            modEventBus.addListener(com.devaxo.templeoftheaxolotl.shipyard.ShipyardClient::renderers);
        }

        // Maze development commands
        NeoForge.EVENT_BUS.addListener(AxoMazeCommands::register);
        NeoForge.EVENT_BUS.addListener(com.devaxo.templeoftheaxolotl.abyss.MerfolkVillageTestCommands::register);
        NeoForge.EVENT_BUS.addListener(com.devaxo.templeoftheaxolotl.maze.OriginMazeCommands::register);
        NeoForge.EVENT_BUS.register(new com.devaxo.templeoftheaxolotl.maze.OriginTempleTravel());
        NeoForge.EVENT_BUS.register(new com.devaxo.templeoftheaxolotl.maze.OriginSanctumRescue());
        NeoForge.EVENT_BUS.register(new DungeonRewardEvents());
        NeoForge.EVENT_BUS.register(new DungeonRewardDevCommands());
        NeoForge.EVENT_BUS.addListener(TrapRuntime::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.devaxo.templeoftheaxolotl.pre07.TrialTrapDeathEvents::onDeath);
        NeoForge.EVENT_BUS.register(new TempleDimensionManager());
        NeoForge.EVENT_BUS.register(new TempleTravel());
        NeoForge.EVENT_BUS.register(new com.devaxo.templeoftheaxolotl.maze.ShallowsExpedition());
        NeoForge.EVENT_BUS.register(new com.devaxo.templeoftheaxolotl.entity.SecondFallResurrection());
        NeoForge.EVENT_BUS.register(new com.devaxo.templeoftheaxolotl.maze.TempleCompanionRestrictions());
        NeoForge.EVENT_BUS.register(new com.devaxo.templeoftheaxolotl.pre07.DilutedIchorAlchemy());
        NeoForge.EVENT_BUS.register(new TempleSeekerEvents());

        // Mod configuration
        modContainer.registerConfig(
                ModConfig.Type.COMMON,
                Config.SPEC
        );

        LOGGER.info("The Temple of the Axolotl awakens!");
    }
}
