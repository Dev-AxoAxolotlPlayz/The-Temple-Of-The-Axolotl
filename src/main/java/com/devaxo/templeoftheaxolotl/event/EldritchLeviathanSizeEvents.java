package com.devaxo.templeoftheaxolotl.event;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.entity.AxoleviathanEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;

/** LivingEntity#getDimensions is final in 1.21.1; use NeoForge's size event. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class EldritchLeviathanSizeEvents {
    private EldritchLeviathanSizeEvents() {}

    @SubscribeEvent
    public static void onSize(EntityEvent.Size event) {
        if (event.getEntity() instanceof AxoleviathanEntity leviathan && leviathan.isEldritchVariant()) {
            event.setNewSize(event.getNewSize().scale(0.7F));
        }
    }
}
