
package com.devaxo.templeoftheaxolotl.client;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.CelestialArmorItem;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.IdentityHashMap;
import java.util.Map;

@EventBusSubscriber(
        modid = TempleOfTheAxolotl.MODID,
        value = Dist.CLIENT
)
public final class CelestialArmorSkinOverlayHandler {

    private static final Map<PlayerModel<?>, boolean[]> SAVED =
            new IdentityHashMap<>();

    private CelestialArmorSkinOverlayHandler() {}

    @SubscribeEvent
    public static void beforeRender(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        PlayerModel<?> model = event.getRenderer().getModel();

        ModelPart[] overlays = {
                model.hat,
                model.jacket,
                model.leftSleeve,
                model.rightSleeve,
                model.leftPants,
                model.rightPants
        };

        boolean hideChest =
                player.getItemBySlot(EquipmentSlot.CHEST).getItem()
                        instanceof CelestialArmorItem;

        boolean hideLegs =
                player.getItemBySlot(EquipmentSlot.LEGS).getItem()
                        instanceof CelestialArmorItem;

        if (!hideChest && !hideLegs) {
            return;
        }

        boolean[] previous = new boolean[overlays.length];

        for (int i = 0; i < overlays.length; i++) {
            previous[i] = overlays[i].visible;
        }

        SAVED.put(model, previous);

        if (hideChest) {
            model.jacket.visible = false;
            model.leftSleeve.visible = false;
            model.rightSleeve.visible = false;
        }

        if (hideLegs) {
            model.leftPants.visible = false;
            model.rightPants.visible = false;
        }

        // Boots deliberately change no skin overlays.
    }

    @SubscribeEvent
    public static void afterRender(RenderPlayerEvent.Post event) {
        PlayerModel<?> model = event.getRenderer().getModel();
        boolean[] previous = SAVED.remove(model);

        if (previous == null) {
            return;
        }

        ModelPart[] overlays = {
                model.hat,
                model.jacket,
                model.leftSleeve,
                model.rightSleeve,
                model.leftPants,
                model.rightPants
        };

        for (int i = 0; i < overlays.length; i++) {
            overlays[i].visible = previous[i];
        }
    }
}
