package com.devaxo.templeoftheaxolotl.loot;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    TempleOfTheAxolotl.MODID);

    public static final Supplier<MapCodec<BonusCuisineDropModifier>> BONUS_CUISINE_DROP =
            SERIALIZERS.register("bonus_cuisine_drop", () -> BonusCuisineDropModifier.CODEC);

    public static final Supplier<MapCodec<MineshaftCosmicEssenceModifier>> MINESHAFT_ESSENCE =
            SERIALIZERS.register("mineshaft_cosmic_essence", () -> MineshaftCosmicEssenceModifier.CODEC);

    private ModLootModifiers() {}
}
