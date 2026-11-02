package com.devaxo.templeoftheaxolotl.event;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.effect.ModEffects;
import com.devaxo.templeoftheaxolotl.pre07.Pre07Fluids;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Server-authoritative decompression exposure. Never alters boats or vehicles. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class BendsEvents {
    private BendsEvents() {}
    private static final String EXPOSURE = "tota_bends_exposure_tier";
    private static final String BENDS = "tota_bends_level";
    private static final String INITIALIZED = "tota_bends_initialized";

    public static void onPressureTier(Player player, int tier) {
        onPressureTier(player, tier, false);
    }

    public static void onPressureTier(Player player, int tier, boolean protectedByGills) {
        CompoundTag tag = player.getPersistentData();
        if (!tag.getBoolean(INITIALIZED)) {
            tag.putBoolean(INITIALIZED, true);
            tag.putInt(EXPOSURE, tier);
            return;
        }
        int old = tag.getInt(EXPOSURE);
        // Exposure above the 75% Gills rating (tier III) still causes decompression
        // when ascending into the protected range. Gills cannot erase prior exposure.
        if (tier < old && !player.isPassenger() && (!protectedByGills || old == 3)) {
            tag.putInt(BENDS, Math.min(3, tag.getInt(BENDS) + old - tier));
        }
        tag.putInt(EXPOSURE, tier);
    }

    /** Clear only pressure exposure from safe Temple elevator travel.
     * Existing Bends severity remains unchanged and still requires a cure.
     */
    public static void resetExposureForTempleElevator(Player player) {
        CompoundTag tag = player.getPersistentData();
        tag.putBoolean(INITIALIZED, true);
        tag.putInt(EXPOSURE, 0);
    }

    public static boolean hasBends(Player player) {
        return player.getPersistentData().getInt(BENDS) > 0;
    }

    public static void cure(Player player) {
        player.getPersistentData().putInt(BENDS, 0);
        player.removeEffect(ModEffects.THE_BENDS);
        // Clear temporary penalties applied by this mechanic immediately.
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.DIG_SLOWDOWN);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.CONFUSION);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        CompoundTag tag = player.getPersistentData();
        int bends = tag.getInt(BENDS);
        if (bends <= 0) return;
        // Short-lived penalties are refreshed periodically, so curing removes them promptly.
        if (player.tickCount % 20 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 45, bends - 1, false, false, true));
            if (bends >= 2) player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 45, bends - 2, false, false, true));
            if (bends >= 3) player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 45, 0, false, false, true));
            // Short, occasional nausea; average interval ~100 seconds at any severity.
            if (player.getRandom().nextInt(100) == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, true, true));
            }
        }
        // Reapply after milk, relogging, or normal effect clearing.
        MobEffectInstance active = player.getEffect(ModEffects.THE_BENDS);
        if (active == null || active.getAmplifier() != bends - 1 || active.getDuration() < 120) {
            player.addEffect(new MobEffectInstance(ModEffects.THE_BENDS, 600, bends - 1, false, true, true));
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        CompoundTag from = event.getOriginal().getPersistentData();
        CompoundTag to = event.getEntity().getPersistentData();
        for (String key : new String[]{EXPOSURE, BENDS, INITIALIZED}) {
            if (from.contains(key)) to.put(key, from.get(key).copy());
        }
    }

}
