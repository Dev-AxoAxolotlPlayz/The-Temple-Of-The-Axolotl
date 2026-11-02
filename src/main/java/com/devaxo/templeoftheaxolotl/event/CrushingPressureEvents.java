package com.devaxo.templeoftheaxolotl.event;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Phase 6.6.1: underwater movement resistance and tier bookkeeping.
 *  No boat or vehicle motion is modified. Bends logic will consume the tier
 *  transition data in the following implementation stage.
 */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class CrushingPressureEvents {
    private CrushingPressureEvents() {}

    private static final String LAST_TIER = "tota_crushing_pressure_tier";

    /** 0 = safe; 1 = warning; 2 = helmet zone; 3 = crush zone. */
    public static int tier(double pressure) {
        if (pressure > 75.0) return 3;
        if (pressure > 50.0) return 2;
        if (pressure > 25.0) return 1;
        return 0;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !player.isAlive()
                || player.isCreative() || player.isSpectator()) return;

        // This also protects all existing boat/submarine movement handling.
        // Exposure tracking is separate from movement resistance.
        // Do not reset exposure when the player surfaces or enters a vehicle.
        if (player.isPassenger()) return;
        // Treat Temple bubble elevators as pressure-neutral transit. Reset exposure
        // while inside, so exiting at another floor cannot trigger elevator Bends.
        if (TempleBubbleElevatorSafety.inside(player)) {
            BendsEvents.resetExposureForTempleElevator(player);
            player.getPersistentData().putInt(LAST_TIER, 0);
            return;
        }
        if (!player.isUnderWater()) {
            BendsEvents.onPressureTier(player, 0, AbyssalGillsProtection.installed(player) && AbyssalGillsProtection.enabled(player));
            player.getPersistentData().putInt(LAST_TIER, 0);
            return;
        }

        Level level = player.level();
        double pressure = TemplePressureScale.pressure(player);
        int currentTier = tier(pressure);
        boolean protectedByGills = AbyssalGillsProtection.protects(player, pressure);
        // Track ascent before updating the stored tier.
        BendsEvents.onPressureTier(player, currentTier, protectedByGills);
        player.getPersistentData().putInt(LAST_TIER, currentTier);

        // Gentle horizontal drag: ascending remains possible at every depth.
        // Values are intentionally conservative until user playtesting.
        double multiplier = protectedByGills ? 1.0 : switch (currentTier) {
            case 1 -> 0.985;
            case 2 -> 0.955;
            case 3 -> 0.915;
            default -> 1.0;
        };
        if (multiplier == 1.0) return;
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x * multiplier, velocity.y,
                velocity.z * multiplier);
        player.hurtMarked = true;
    }
}
