package com.devaxo.templeoftheaxolotl.event;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.maze.OriginTempleState;
import net.minecraft.world.entity.monster.EnderMan;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/** Natural Endermen never spawn inside the Fractured Origin. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class FracturedOriginEndermanEvents {
    private FracturedOriginEndermanEvents() {}
    @SubscribeEvent
    public static void check(MobSpawnEvent.PositionCheck event) {
        if (event.getEntity() instanceof EnderMan &&
                event.getEntity().level().dimension().equals(OriginTempleState.DIMENSION)) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }
}
