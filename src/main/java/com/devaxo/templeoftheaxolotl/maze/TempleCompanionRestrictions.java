package com.devaxo.templeoftheaxolotl.maze;

import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;

/** Keep companions outside the dungeon; wolves are the explicit exception. */
public final class TempleCompanionRestrictions {
    @SubscribeEvent
    public void onDimensionTravel(EntityTravelToDimensionEvent event) {
        if (!event.getDimension().equals(TempleDimensionManager.TEMPLE_DIMENSION)) return;
        if (event.getEntity() instanceof TamableAnimal companion && !(companion instanceof Wolf)) {
            event.setCanceled(true);
        }
    }
}
