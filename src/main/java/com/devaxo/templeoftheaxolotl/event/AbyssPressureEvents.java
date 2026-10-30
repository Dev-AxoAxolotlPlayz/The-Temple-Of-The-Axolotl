package com.devaxo.templeoftheaxolotl.event;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Server-side pressure damage. Requires the player's head to be underwater. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class AbyssPressureEvents {
    private AbyssPressureEvents() {}

    private static final ResourceKey<DamageType> PRESSURE_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "abyss_pressure"));

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.isCreative() || player.isSpectator()
                || !player.isAlive() || !player.isUnderWater()) return;

        Level level = player.level();
        double seaLevel = level.dimension() == Level.OVERWORLD ? 62.0 : 200.0;
        double depthRange = level.dimension() == Level.OVERWORLD ? 92.0 : 200.0;
        double pressure = Math.max(0.0, Math.min(100.0,
                (seaLevel - player.getEyeY()) * 100.0 / depthRange));

        // 0-25 safe; 26-50 warning; >50 helmet zone; >75 submarine zone.
        // Small, frequent true-damage hits overwhelm ordinary regeneration.
        if (pressure <= 50.0) return;
        boolean crushZone = pressure > 75.0;
        int intervalTicks = crushZone ? 2 : 4;
        if (player.tickCount % intervalTicks != 0) return;

        DamageSource source = new RandomizedPressureDamageSource(level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(PRESSURE_DAMAGE), crushZone);
        player.hurt(source, crushZone ? 1.0F : 0.5F);
    }
    /** Keeps the existing true-damage behavior; only changes the death text. */
    private static final class RandomizedPressureDamageSource extends DamageSource {
        private static final int HELMET_MESSAGES = 5;
        private static final int CRUSH_MESSAGES = 8;
        private final boolean crushZone;

        private RandomizedPressureDamageSource(Holder<DamageType> type, boolean crushZone) {
            super(type);
            this.crushZone = crushZone;
        }

        @Override
        public Component getLocalizedDeathMessage(LivingEntity victim) {
            int index = victim.getRandom().nextInt(crushZone ? CRUSH_MESSAGES : HELMET_MESSAGES);
            String key = "death.attack.abyss_pressure." + (crushZone ? "crush." : "helmet.") + index;
            return Component.translatable(key, victim.getDisplayName());
        }
    }
}
