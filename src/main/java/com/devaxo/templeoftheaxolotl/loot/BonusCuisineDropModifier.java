
package com.devaxo.templeoftheaxolotl.loot;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Adds bonus cuisine ingredients to vanilla block drops.
 *
 * Rice Grain:
 * 10% chance from short grass.
 *
 * Koji:
 * 5% chance from fully grown wheat.
 *
 * Fortune does not affect these probabilities.
 *
 * Routine diagnostic logging is disabled.
 */
public final class BonusCuisineDropModifier extends LootModifier {

    public static final MapCodec<BonusCuisineDropModifier> CODEC =
            RecordCodecBuilder.mapCodec(instance ->
                    LootModifier.codecStart(instance).and(instance.group(

                            BuiltInRegistries.BLOCK.byNameCodec()
                                    .fieldOf("target_block")
                                    .forGetter(modifier -> modifier.targetBlock),

                            BuiltInRegistries.ITEM.byNameCodec()
                                    .fieldOf("bonus_item")
                                    .forGetter(modifier -> modifier.bonusItem),

                            Codec.FLOAT
                                    .fieldOf("chance")
                                    .forGetter(modifier -> modifier.chance),

                            Codec.BOOL
                                    .optionalFieldOf("mature_only", false)
                                    .forGetter(modifier -> modifier.matureOnly)

                    )).apply(instance, BonusCuisineDropModifier::new)
            );

    private final Block targetBlock;
    private final Item bonusItem;
    private final float chance;
    private final boolean matureOnly;

    public BonusCuisineDropModifier(
            LootItemCondition[] conditions,
            Block targetBlock,
            Item bonusItem,
            float chance,
            boolean matureOnly
    ) {
        super(conditions);

        this.targetBlock = targetBlock;
        this.bonusItem = bonusItem;
        this.chance = chance;
        this.matureOnly = matureOnly;

    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(
            ObjectArrayList<ItemStack> generatedLoot,
            LootContext context
    ) {

        BlockState state =
                context.getParamOrNull(LootContextParams.BLOCK_STATE);

        if (state == null) {

            return generatedLoot;
        }

        if (!state.is(targetBlock)) {
            return generatedLoot;
        }

        if (matureOnly) {
            if (!(state.getBlock() instanceof CropBlock crop)
                    || !crop.isMaxAge(state)) {

                return generatedLoot;
            }
        }

        float roll = context.getRandom().nextFloat();

        if (roll < chance) {

            generatedLoot.add(new ItemStack(bonusItem));
        }

        return generatedLoot;
    }
}
