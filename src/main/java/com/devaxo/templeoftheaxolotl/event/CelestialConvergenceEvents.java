
package com.devaxo.templeoftheaxolotl.event;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.ModItems;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(
        modid = TempleOfTheAxolotl.MODID,
        bus = EventBusSubscriber.Bus.GAME
)
public class CelestialConvergenceEvents {

    private static final ResourceKey<Enchantment> CONVERGENCE =
            ResourceKey.create(
                    Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(
                            TempleOfTheAxolotl.MODID,
                            "celestial_convergence"
                    )
            );

    private static final TagKey<EntityType<?>> IMMUNE =
            TagKey.create(
                    Registries.ENTITY_TYPE,
                    ResourceLocation.fromNamespaceAndPath(
                            TempleOfTheAxolotl.MODID,
                            "convergence_immune"
                    )
            );

    private static final ResourceLocation DISINTEGRATION_ID =
            ResourceLocation.fromNamespaceAndPath(
                    TempleOfTheAxolotl.MODID,
                    "celestial_disintegration"
            );

    private static final int DEBUFF_TICKS = 100;
    private static final int STUN_TICKS = 40;
    private static final int STUN_COOLDOWN_TICKS = 200;

    private static final Map<UUID, Long> STUN_COOLDOWNS =
            new HashMap<>();

    private static final Map<UUID, Long> DISINTEGRATION_EXPIRY =
            new HashMap<>();

    private static int getConvergenceLevel(Player player) {

        if (!player.getMainHandItem().is(
                ModItems.RING_OF_THE_ONE.get()
        )) {
            return 0;
        }

        Holder<Enchantment> enchantment =
                player.level()
                        .registryAccess()
                        .registryOrThrow(Registries.ENCHANTMENT)
                        .getHolder(CONVERGENCE)
                        .orElse(null);

        if (enchantment == null) {
            return 0;
        }

        return Math.min(
                6,
                player.getMainHandItem()
                        .getEnchantmentLevel(enchantment)
        );
    }

    private static boolean isImmune(LivingEntity target) {
        return target.getType().is(IMMUNE);
    }

    @SubscribeEvent
    public static void onSuccessfulHit(
            LivingDamageEvent.Post event
    ) {

        LivingEntity target = event.getEntity();

        if (target.level().isClientSide()) {
            return;
        }

        if (event.getNewDamage() <= 0.0F) {
            return;
        }

        Entity source = event.getSource().getEntity();

        if (!(source instanceof Player player)) {
            return;
        }

        // Only direct melee hits with the Ring qualify.
        if (event.getSource().getDirectEntity() != player) {
            return;
        }

        int level = getConvergenceLevel(player);

        if (level <= 0 || isImmune(target)) {
            return;
        }

        long now = target.level().getGameTime();

        applyDisintegration(target, level, now);

        applySlownessAndStun(target, now);
    }

    private static void applyDisintegration(
            LivingEntity target,
            int level,
            long now
    ) {

        AttributeInstance armor =
                target.getAttribute(Attributes.ARMOR);

        if (armor == null) {
            return;
        }

        // Level VI = 20% effective armor reduction.
        double reduction = -0.20D * level / 6.0D;

        armor.removeModifier(DISINTEGRATION_ID);

        armor.addTransientModifier(
                new AttributeModifier(
                        DISINTEGRATION_ID,
                        reduction,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                )
        );

        DISINTEGRATION_EXPIRY.put(
                target.getUUID(),
                now + DEBUFF_TICKS
        );
    }

    private static void applySlownessAndStun(
            LivingEntity target,
            long now
    ) {

        UUID id = target.getUUID();

        Long nextStunTime = STUN_COOLDOWNS.get(id);

        // A victim can only be stunned once per cooldown.
        boolean canStun =
                nextStunTime == null || now >= nextStunTime;

        if (canStun
                && target.getRandom().nextFloat() < 0.05F) {

            target.addEffect(
                    new MobEffectInstance(
                            MobEffects.MOVEMENT_SLOWDOWN,
                            STUN_TICKS,
                            254,
                            false,
                            true,
                            true
                    )
            );

            STUN_COOLDOWNS.put(
                    id,
                    now + STUN_COOLDOWN_TICKS
            );

            return;
        }

        // Do not replace an active stun with weaker Slowness.
        MobEffectInstance current =
                target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);

        if (current != null && current.getAmplifier() >= 254) {
            return;
        }

        target.addEffect(
                new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN,
                        DEBUFF_TICKS,
                        0,
                        false,
                        true,
                        true
                )
        );
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {

        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        if (target.level().isClientSide()) {
            return;
        }

        UUID id = target.getUUID();
        long now = target.level().getGameTime();

        Long expiry = DISINTEGRATION_EXPIRY.get(id);

        if (expiry != null && now >= expiry) {

            AttributeInstance armor =
                    target.getAttribute(Attributes.ARMOR);

            if (armor != null) {
                armor.removeModifier(DISINTEGRATION_ID);
            }

            DISINTEGRATION_EXPIRY.remove(id);
        }

        Long cooldown = STUN_COOLDOWNS.get(id);

        if (cooldown != null && now >= cooldown) {
            STUN_COOLDOWNS.remove(id);
        }
    }
}
