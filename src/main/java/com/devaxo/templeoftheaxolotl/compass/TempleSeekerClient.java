
package com.devaxo.templeoftheaxolotl.compass;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** Reuse vanilla's compass animation with a 180-degree visual correction. */
@EventBusSubscriber(
        modid = TempleOfTheAxolotl.MODID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class TempleSeekerClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ResourceLocation angle =
                    ResourceLocation.withDefaultNamespace("angle");

            var vanillaAngle = ItemProperties.getProperty(
                    new ItemStack(Items.COMPASS),
                    angle
            );

            if (vanillaAngle != null) {
                ItemProperties.register(
                        ModItems.TEMPLE_SEEKER.get(),
                        angle,
                        (stack, level, entity, seed) -> {
                            float original =
                                    vanillaAngle.call(stack, level, entity, seed);

                            return (original + 0.5F) % 1.0F;
                        }
                );
            }
        });
    }
}
