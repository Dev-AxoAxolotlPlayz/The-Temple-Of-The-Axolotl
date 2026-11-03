package com.devaxo.templeoftheaxolotl.shipyard;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class ShipyardHookAttackEvents {
    private ShipyardHookAttackEvents() {}

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof ArmorStand stand)) return;
        if (!stand.getTags().contains("tota_shipyard_test_hook")) return;
        // Cancel vanilla damage even if the crafting recipe was changed.
        event.setCanceled(true);
        if (event.getEntity().level().isClientSide) return;
        BlockPos controller = BlockPos.of(stand.getPersistentData().getLong("tota_shipyard_controller_pos"));
        if (event.getEntity().level().getBlockEntity(controller) instanceof ShipyardCraneBlockEntity crane) {
            crane.releaseCargo(event.getEntity(), stand);
        }
    }
}
