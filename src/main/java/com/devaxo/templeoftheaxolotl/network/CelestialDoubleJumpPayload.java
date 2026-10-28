
package com.devaxo.templeoftheaxolotl.network;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CelestialDoubleJumpPayload()
        implements CustomPacketPayload {

    public static final Type<CelestialDoubleJumpPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    TempleOfTheAxolotl.MODID,
                    "celestial_double_jump"
            ));

    public static final StreamCodec<
            net.minecraft.network.RegistryFriendlyByteBuf,
            CelestialDoubleJumpPayload> STREAM_CODEC =
            StreamCodec.unit(new CelestialDoubleJumpPayload());

    private static final String USED_KEY =
            "temple_celestial_air_jumps_used";

    public static final String FALL_PROTECTION_KEY =
            "temple_celestial_jump_fall_protection_until";

    private static final int FALL_PROTECTION_TICKS = 40;

    private static final ResourceKey<Enchantment> POUNCE_KEY =
            ResourceKey.create(
                    Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(
                            TempleOfTheAxolotl.MODID,
                            "wraths_pounce"
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static int getMaximumAirJumps(ServerPlayer player) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);

        if (!boots.is(ModItems.AXOLOTL_BOOTS.get())) {
            return 0;
        }

        var enchantments = player.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT);

        var pounce = enchantments.getOrThrow(POUNCE_KEY);

        int level = EnchantmentHelper.getEnchantmentsForCrafting(boots).getLevel(pounce);

        return 1 + Math.min(level, 6);
    }

    public static int getUsedAirJumps(ServerPlayer player) {
        return player.getPersistentData().getInt(USED_KEY);
    }

    public static void resetIfGrounded(ServerPlayer player) {
        if (player.onGround()) {
            player.getPersistentData().remove(USED_KEY);
        }
    }

    public static void handle(
            CelestialDoubleJumpPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            int maximum = getMaximumAirJumps(player);

            if (maximum == 0
                    || player.onGround()
                    || player.isInWater()
                    || player.isInLava()
                    || player.isPassenger()
                    || player.getAbilities().flying
                    || player.isFallFlying()) {
                return;
            }

            int used = getUsedAirJumps(player);

            if (used >= maximum) {
                return;
            }

            player.getPersistentData().putInt(USED_KEY, used + 1);

            Vec3 motion = player.getDeltaMovement();

            player.setDeltaMovement(
                    motion.x * 1.35D,
                    0.65D,
                    motion.z * 1.35D
            );

            player.hurtMarked = true;
            player.resetFallDistance();

            player.getPersistentData().putLong(
                    FALL_PROTECTION_KEY,
                    player.level().getGameTime()
                            + FALL_PROTECTION_TICKS
            );

            player.serverLevel().sendParticles(
                    ParticleTypes.CLOUD,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    14,
                    0.3D,
                    0.12D,
                    0.3D,
                    0.04D
            );
        });
    }
}
