
package com.devaxo.templeoftheaxolotl.item;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.compass.TempleSeekerItem;
import com.devaxo.templeoftheaxolotl.effect.ModEffects;
import com.devaxo.templeoftheaxolotl.entity.ModEntities;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ArmorItem;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(TempleOfTheAxolotl.MODID);

    // Shipyard-only craftable, boat-style water deployment item.
    public static final DeferredItem<Shorkolotl100Item> SHORKOLOTL_100_ITEM =
            ITEMS.register("shorkolotl_100_item", () -> new Shorkolotl100Item(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<TempleSeekerItem> TEMPLE_SEEKER =
            ITEMS.register("temple_seeker", () -> new TempleSeekerItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<TempleLootbagItem> TEMPLE_LOOTBAG =
            ITEMS.register("temple_lootbag", () -> new TempleLootbagItem(new Item.Properties().stacksTo(1)));

    // 08-D1.5.5.1 celestial soundtrack and collectible materials.
    public static final DeferredItem<Item> COSMIC_ESSENCE = basic("cosmic_essence", 64);
    public static final DeferredItem<Item> CELESTIAL_JUKEBOX = basic("celestial_jukebox", 1);
    public static final DeferredItem<Item> DISC_COSMIC_SEAS = ITEMS.register("disc_cosmic_seas", () -> new Item(new Item.Properties().stacksTo(1).jukeboxPlayable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.JUKEBOX_SONG, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "disc_cosmic_seas")))));
    public static final DeferredItem<Item> DISC_BENEATH_SURFACE = ITEMS.register("disc_beneath_surface", () -> new Item(new Item.Properties().stacksTo(1).jukeboxPlayable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.JUKEBOX_SONG, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "disc_beneath_surface")))));
    public static final DeferredItem<Item> DISC_OUT_OF_REACH = ITEMS.register("disc_out_of_reach", () -> new Item(new Item.Properties().stacksTo(1).jukeboxPlayable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.JUKEBOX_SONG, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "disc_out_of_reach")))));

    // Patch 08-D1.5.2: dedicated equipment modules.
    public static final DeferredItem<Item> CELESTIAL_REPOSITORY = basic("celestial_repository_module", 1);
    public static final DeferredItem<Item> ABYSSAL_TORCH = ITEMS.register("abyssal_torch", () ->
            new net.minecraft.world.item.BlockItem(
                    com.devaxo.templeoftheaxolotl.registry.ModBlocks.ABYSSAL_TORCH.get(), new Item.Properties()));
    public static final DeferredItem<Item> ABYSS_PRESSURE_GAUGE = basic("abyss_pressure_gauge", 1);
    public static final DeferredItem<Item> ABYSSAL_GILLS = basic("abyssal_gills", 1);
    public static final DeferredItem<Item> CELESTIAL_MAGNET = basic("celestial_magnet", 1);
    public static final DeferredItem<Item> LOGISTICS_MAESTRO = basic("logistics_maestro", 1);
    public static final DeferredItem<Item> CELESTIAL_ANVIL = ITEMS.register("celestial_anvil", () ->
            new net.minecraft.world.item.BlockItem(
                    com.devaxo.templeoftheaxolotl.registry.ModBlocks.CELESTIAL_ANVIL_BLOCK.get(),
                    new Item.Properties().stacksTo(1)));

    private ModItems() {}

    // =========================================
    // ITEM HELPERS
    // =========================================

    private static DeferredItem<Item> basic(
            String id,
            int maxStackSize
    ) {
        return ITEMS.registerSimpleItem(
                id,
                new Item.Properties().stacksTo(maxStackSize)
        );
    }

    private static DeferredItem<Item> food(
            String id,
            int maxStackSize,
            int nutrition,
            float saturation,
            boolean alwaysEdible
    ) {
        FoodProperties.Builder builder = new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationModifier(saturation);

        if (alwaysEdible) {
            builder.alwaysEdible();
        }

        return ITEMS.registerSimpleItem(
                id,
                new Item.Properties()
                        .stacksTo(maxStackSize)
                        .food(builder.build())
        );
    }

    // =========================================
    // CELESTIAL SUSHI FOOD HELPER
    // =========================================

    private static DeferredItem<Item> celestialSushi(
            String id,
            int nutrition,
            float saturation,
            boolean oasis
    ) {
        FoodProperties.Builder builder = new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationModifier(saturation)
                .alwaysEdible();

        if (oasis) {

            // Regeneration II - 20 seconds
            builder.effect(
                    () -> new MobEffectInstance(
                            MobEffects.REGENERATION,
                            400,
                            1
                    ),
                    1.0F
            );

            // Absorption IV - 2 minutes
            builder.effect(
                    () -> new MobEffectInstance(
                            MobEffects.ABSORPTION,
                            2400,
                            3
                    ),
                    1.0F
            );

            // Resistance I - 5 minutes
            builder.effect(
                    () -> new MobEffectInstance(
                            MobEffects.DAMAGE_RESISTANCE,
                            6000,
                            0
                    ),
                    1.0F
            );

            // Fire Resistance I - 5 minutes
            builder.effect(
                    () -> new MobEffectInstance(
                            MobEffects.FIRE_RESISTANCE,
                            6000,
                            0
                    ),
                    1.0F
            );

            // Speed I - 5 minutes
            builder.effect(
                    () -> new MobEffectInstance(
                            MobEffects.MOVEMENT_SPEED,
                            6000,
                            0
                    ),
                    1.0F
            );

            // Freezing Immunity - 5 minutes
            builder.effect(
                    () -> new MobEffectInstance(
                            ModEffects.FREEZING_IMMUNITY,
                            6000,
                            0
                    ),
                    1.0F
            );

        } else {

            // Regeneration II - 10 seconds
            builder.effect(
                    () -> new MobEffectInstance(
                            MobEffects.REGENERATION,
                            200,
                            1
                    ),
                    1.0F
            );

            // Absorption II - 3 minutes
            builder.effect(
                    () -> new MobEffectInstance(
                            MobEffects.ABSORPTION,
                            3600,
                            1
                    ),
                    1.0F
            );
        }

        return ITEMS.registerSimpleItem(
                id,
                new Item.Properties()
                        .stacksTo(32)
                        .food(builder.build())
        );
    }

    // =========================================
    // AGRICULTURE & BASIC INGREDIENTS
    // =========================================

    public static final DeferredItem<Item> GRAIN =
            basic("grain", 64);

    public static final DeferredItem<Item> UNCOOKED_RICE =
            basic("uncooked_rice", 64);

    public static final DeferredItem<Item> KOJI =
            basic("koji", 64);

    public static final DeferredItem<Item> SALT =
            basic("salt", 64);

    public static final DeferredItem<Item> SALTWATER_BUCKET =
            basic("saltwater_bucket", 16);

    public static final DeferredItem<Item> UNCOOKED_RICE_BOWL =
            basic("uncooked_rice_bowl", 64);

    public static final DeferredItem<Item> BOWL_OF_RICE =
            food("bowl_of_rice", 64, 6, 0.15F, false);

    public static final DeferredItem<Item> SUSHI_RICE =
            basic("sushi_rice", 64);

    // =========================================
    // SUSHI INGREDIENTS & DISHES
    // =========================================

    public static final DeferredItem<Item> TROPICAL_FISH_FILLET =
            basic("tropical_fish_fillet", 64);

    public static final DeferredItem<Item> TROPICAL_FISH_SUSHI =
            food("tropical_fish_sushi", 64, 4, 0.3F, false);

    public static final DeferredItem<Item> GOLD_PAPER =
            basic("gold_paper", 64);

    public static final DeferredItem<Item> GOLDEN_TROPICAL_FISH_SUSHI =
            food("golden_tropical_fish_sushi", 32, 6, 0.6F, false);

    // Enhanced Golden Apple
    public static final DeferredItem<Item> AXOLOTLS_DREAM_SUSHI =
            celestialSushi(
                    "axolotls_dream_sushi",
                    8,
                    0.9F,
                    false
            );

    // Enhanced Enchanted Golden Apple
    public static final DeferredItem<Item> AXOLOTLS_OASIS_SUSHI =
            celestialSushi(
                    "axolotls_oasis_sushi",
                    10,
                    0.9F,
                    true
            );

    // =========================================
    // BENTO BOXES & TROPHIES
    // =========================================

    public static final DeferredItem<Item> DREAM_BENTO_BOX =
            basic("dream_bento_box", 16);

    public static final DeferredItem<Item> OASIS_BENTO_BOX =
            basic("oasis_bento_box", 16);

    public static final DeferredItem<Item> WHITE_AXOLOTL_TROPHY =
            basic("white_axolotl_trophy", 1);

    public static final DeferredItem<Item> BLACK_AXOLOTL_TROPHY =
            basic("black_axolotl_trophy", 1);

    // =========================================
    // ROYAL KITCHEN TOOLS
    // =========================================

    // 32 durability, loses 2 per crafting use.
    public static final DeferredItem<Item> MEAT_TENDERIZER =
            ITEMS.register(
                    "meat_tenderizer",
                    () -> new CraftingToolItem(
                            new Item.Properties().durability(32),
                            2
                    )
            );

    // 128 durability, loses 1 per crafting use.
    public static final DeferredItem<Item> FILLET_KNIFE =
            ITEMS.register(
                    "fillet_knife",
                    () -> new CraftingToolItem(
                            new Item.Properties().durability(128),
                            1
                    )
            );

    // Reusable crafting catalyst, no durability loss.
    public static final DeferredItem<Item> HANDHELD_HYDRAULIC_PRESS =
            ITEMS.register(
                    "handheld_hydraulic_press",
                    () -> new CraftingToolItem(
                            new Item.Properties().stacksTo(1),
                            0
                    )
            );

    // =========================================
    // CELESTIAL CRAFTING MATERIALS
    // =========================================

    // Celestial progression ingredients.
    public static final DeferredItem<Item> SHARD_OF_DIVINITY = basic("shard_of_divinity", 64);
    public static final DeferredItem<Item> ORB_OF_ENVY = basic("orb_of_envy", 64);
    public static final DeferredItem<Item> CELESTIAL_KEY = basic("celestial_key", 1);

    // =========================================
// CELESTIAL AXOLOTL ARMOR
// =========================================

    public static final DeferredItem<CelestialArmorItem> AXOLOTL_CHESTPLATE =
            ITEMS.register("axolotl_chestplate",
                    () -> new CelestialArmorItem(
                            ArmorMaterials.NETHERITE,
                            ArmorItem.Type.CHESTPLATE,
                            new Item.Properties().stacksTo(1)
                    ));

    public static final DeferredItem<CelestialArmorItem> AXOLOTL_LEGGINGS =
            ITEMS.register("axolotl_leggings",
                    () -> new CelestialArmorItem(
                            ArmorMaterials.NETHERITE,
                            ArmorItem.Type.LEGGINGS,
                            new Item.Properties().stacksTo(1)
                    ));

    public static final DeferredItem<CelestialArmorItem> AXOLOTL_BOOTS =
            ITEMS.register("axolotl_boots",
                    () -> new CelestialArmorItem(
                            ArmorMaterials.NETHERITE,
                            ArmorItem.Type.BOOTS,
                            new Item.Properties().stacksTo(1)
                    ));

    public static final DeferredItem<Item> YIN_SINGULARITY =
            basic("yin_singularity", 1);

    public static final DeferredItem<Item> YANG_SINGULARITY =
            basic("yang_singularity", 1);


    public static final DeferredItem<Item> RING_OF_THE_ONE =
            ITEMS.register(
                    "ring_of_the_one",
                    () -> new RingOfTheOneItem(
                            RingOfTheOneItem.createProperties()
                    )
            );



    public static final DeferredItem<Item> AXOLOTL_ABSOLUTE =
            ITEMS.register(
                    "axolotl_absolute",
                    () -> new AxolotlAbsoluteItem(
                            AxolotlAbsoluteItem.createProperties()
                    )
            );


    // =========================================
    // AXOLOTL ABOVE ALL - TEMPORARY SPAWN EGG
    // =========================================

    public static final DeferredItem<DeferredSpawnEggItem> ABYSSAL_MERFOLK_SPAWN_EGG = ITEMS.register(
            "abyssal_merfolk_spawn_egg", () -> new DeferredSpawnEggItem(
                    ModEntities.ABYSSAL_MERFOLK, 0x153D59, 0x66E7E5, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> AXOLOTL_ABSOLUTE_SPAWN_EGG =
            ITEMS.register(
                    "axolotl_absolute_spawn_egg",
                    () -> new DeferredSpawnEggItem(
                            ModEntities.AXOLOTL_ABSOLUTE,
                            0xF7F5EE,
                            0x69D9FF,
                            new Item.Properties().stacksTo(64)
                    )
            );

    public static final DeferredItem<DeferredSpawnEggItem> CLUCKLES_SPAWN_EGG = ITEMS.register(
            "cluckles_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.CLUCKLES,
                    0xF7F1DB, 0xE8A92E, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> BOSS_WRATH_SPAWN_EGG = ITEMS.register(
            "boss_wrath_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.BOSS_WRATH,
                    0x1F0000, 0xFF2400, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> TAMEABLE_WRATH_SPAWN_EGG = ITEMS.register(
            "tameable_wrath_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.TAMEABLE_WRATH,
                    0x660808, 0xA61CFF, new Item.Properties()));

    // =========================================
    // CREATIVE TAB ICON ITEM
    // =========================================

    public static final DeferredItem<Item> AXOLOTL_TAB_ICON =
            basic("axolotl_tab_icon", 1);

    // =========================================
    // CREATIVE MODE TAB
    // =========================================

    public static void addToCreativeTab(
            CreativeModeTab.Output output
    ) {

        output.accept(GRAIN.get());
        output.accept(UNCOOKED_RICE.get());
        output.accept(KOJI.get());
        output.accept(SALT.get());
        output.accept(SALTWATER_BUCKET.get());
        output.accept(UNCOOKED_RICE_BOWL.get());
        output.accept(BOWL_OF_RICE.get());
        output.accept(SUSHI_RICE.get());

        output.accept(TROPICAL_FISH_FILLET.get());
        output.accept(TROPICAL_FISH_SUSHI.get());
        output.accept(GOLD_PAPER.get());
        output.accept(GOLDEN_TROPICAL_FISH_SUSHI.get());
        output.accept(AXOLOTLS_DREAM_SUSHI.get());
        output.accept(AXOLOTLS_OASIS_SUSHI.get());

        output.accept(DREAM_BENTO_BOX.get());
        output.accept(OASIS_BENTO_BOX.get());
        output.accept(WHITE_AXOLOTL_TROPHY.get());
        output.accept(BLACK_AXOLOTL_TROPHY.get());

        output.accept(MEAT_TENDERIZER.get());
        output.accept(FILLET_KNIFE.get());
        output.accept(HANDHELD_HYDRAULIC_PRESS.get());

        output.accept(SHARD_OF_DIVINITY.get());
        output.accept(ORB_OF_ENVY.get());
        output.accept(CELESTIAL_KEY.get());
        output.accept(AXOLOTL_CHESTPLATE.get());
        output.accept(AXOLOTL_LEGGINGS.get());
        output.accept(AXOLOTL_BOOTS.get());
        output.accept(TEMPLE_SEEKER.get());
        output.accept(TEMPLE_LOOTBAG.get());
        output.accept(CELESTIAL_REPOSITORY.get());
        output.accept(CELESTIAL_MAGNET.get());
        output.accept(ABYSS_PRESSURE_GAUGE.get());
        output.accept(ABYSSAL_GILLS.get());
        output.accept(ABYSSAL_TORCH.get());
        output.accept(LOGISTICS_MAESTRO.get());
        output.accept(CELESTIAL_ANVIL.get());
        output.accept(COSMIC_ESSENCE.get());
        output.accept(CELESTIAL_JUKEBOX.get());
        output.accept(DISC_COSMIC_SEAS.get());
        output.accept(DISC_BENEATH_SURFACE.get());
        output.accept(DISC_OUT_OF_REACH.get());
        output.accept(YIN_SINGULARITY.get());
        output.accept(YANG_SINGULARITY.get());
        output.accept(RING_OF_THE_ONE.get());
        output.accept(AXOLOTL_ABSOLUTE.get());
        output.accept(AXOLOTL_ABSOLUTE_SPAWN_EGG.get());
        output.accept(ABYSSAL_MERFOLK_SPAWN_EGG.get());
        output.accept(CLUCKLES_SPAWN_EGG.get());
        output.accept(TAMEABLE_WRATH_SPAWN_EGG.get());
        output.accept(BOSS_WRATH_SPAWN_EGG.get());
    }
}
