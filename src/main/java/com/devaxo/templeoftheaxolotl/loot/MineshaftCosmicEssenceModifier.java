package com.devaxo.templeoftheaxolotl.loot;

import com.devaxo.templeoftheaxolotl.item.ModItems;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/** A rare bonus independent of the ordinary abandoned-mineshaft chest pool. */
public final class MineshaftCosmicEssenceModifier extends LootModifier {
    public static final MapCodec<MineshaftCosmicEssenceModifier> CODEC =
            RecordCodecBuilder.mapCodec(i -> LootModifier.codecStart(i)
                    .apply(i, MineshaftCosmicEssenceModifier::new));
    public MineshaftCosmicEssenceModifier(LootItemCondition[] conditions) { super(conditions); }
    @Override public MapCodec<? extends IGlobalLootModifier> codec() { return CODEC; }
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (context.getRandom().nextFloat() < 0.035f) loot.add(new ItemStack(ModItems.COSMIC_ESSENCE.get()));
        return loot;
    }
}
