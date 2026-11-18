package com.devaxo.templeoftheaxolotl.event;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class GlassMoldingAnvilEvents {
    private GlassMoldingAnvilEvents() {}

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (!left.is(ModItems.MOLTEN_SAND.get()) || !right.is(ModItems.GLASS_MOLD.get())) return;
        // Vanilla anvil consumes the entire left slot; reject stacked sand to prevent loss.
        if (left.getCount() != 1) {
            event.setCanceled(true);
            return;
        }
        event.setOutput(new ItemStack(ModItems.MOLTEN_GLASS_IN_A_MOLD.get()));
        event.setCost(1);
        event.setMaterialCost(1);
    }
}
