package com.devaxo.templeoftheaxolotl.abyss;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.entity.MerolotlEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Opening a Merolotl village chest alerts nearby residents together. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class MerolotlVillageAlarm {
    private MerolotlVillageAlarm() {}
    @SubscribeEvent public static void onChestOpened(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getLevel() instanceof ServerLevel level)) return;
        if (!level.dimension().equals(AbyssTrialEvents.ABYSS)) return;
        if (!level.getBlockState(event.getPos()).is(Blocks.CHEST)) return;
        var player = event.getEntity();
        if (player.isCreative() || player.isSpectator()) return;
        // Only protected chests: require a nearby village resident rather than every Abyss chest.
        var residents = level.getEntitiesOfClass(MerolotlEntity.class,
                new AABB(event.getPos()).inflate(36));
        for (var resident : residents) resident.raiseVillageAlarm(player);
    }
}
