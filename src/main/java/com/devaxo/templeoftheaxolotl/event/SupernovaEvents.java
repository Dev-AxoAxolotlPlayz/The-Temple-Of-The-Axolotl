
package com.devaxo.templeoftheaxolotl.event;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.ModItems;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(
        modid = TempleOfTheAxolotl.MODID
)
public class SupernovaEvents {

    private static final ResourceKey<Enchantment> SUPERNOVA =
            ResourceKey.create(
                    Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(
                            TempleOfTheAxolotl.MODID,
                            "supernova"
                    )
            );

    public static boolean hasSupernova(Player player) {

        ItemStack ring = player.getMainHandItem();

        if (!ring.is(ModItems.RING_OF_THE_ONE.get())) {
            return false;
        }

        Holder<Enchantment> enchantment =
                player.level()
                        .registryAccess()
                        .registryOrThrow(Registries.ENCHANTMENT)
                        .getHolder(SUPERNOVA)
                        .orElse(null);

        return enchantment != null
                && ring.getEnchantmentLevel(enchantment) > 0;
    }

    @SubscribeEvent
    public static void onHarvestCheck(
            PlayerEvent.HarvestCheck event
    ) {
        if (hasSupernova(event.getEntity())
                && !event.getTargetBlock().is(Blocks.BEDROCK)) {
            event.setCanHarvest(true);
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(
            PlayerEvent.BreakSpeed event
    ) {
        if (!hasSupernova(event.getEntity())) {
            return;
        }

        if (event.getState().is(Blocks.BEDROCK)) {
            return;
        }

        // Preserve Haste and faster existing modifiers.
        // The Ring is a SwordItem. For blocks requiring the correct tool,
        // vanilla divides destroy progress by 100 if the stack is not a valid
        // mining tool. HarvestCheck alone does not undo that speed penalty.
        float targetSpeed = 35.0F;
        if (event.getState().requiresCorrectToolForDrops()
                && !event.getEntity().getMainHandItem().isCorrectToolForDrops(event.getState())) {
            targetSpeed *= 100.0F;
        }
        event.setNewSpeed(Math.max(event.getNewSpeed(), targetSpeed));
    }
}
