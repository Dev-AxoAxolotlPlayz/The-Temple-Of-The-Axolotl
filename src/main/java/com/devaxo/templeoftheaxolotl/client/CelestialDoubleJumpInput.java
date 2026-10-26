
package com.devaxo.templeoftheaxolotl.client;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import com.devaxo.templeoftheaxolotl.network.CelestialDoubleJumpPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(
        modid = TempleOfTheAxolotl.MODID,
        value = Dist.CLIENT
)
public final class CelestialDoubleJumpInput {

    private static final ResourceKey<Enchantment> POUNCE_KEY =
            ResourceKey.create(
                    Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(
                            TempleOfTheAxolotl.MODID,
                            "wraths_pounce"
                    )
            );

    private static int jumpsUsed = 0;

    private static boolean previousJumpPressed = false;

    // An air jump is only permitted after Space
    // has been released while the player is airborne.
    private static boolean airJumpArmed = false;

    public static int hudUsedAirJumps() { return jumpsUsed; }
    public static int hudMaxAirJumps() { return getMaximumAirJumps(Minecraft.getInstance()); }

    private CelestialDoubleJumpInput() {
    }

    private static int getMaximumAirJumps(Minecraft minecraft) {
        if (minecraft.player == null) {
            return 0;
        }

        ItemStack boots = minecraft.player.getItemBySlot(
                EquipmentSlot.FEET
        );

        if (!boots.is(ModItems.AXOLOTL_BOOTS.get())) {
            return 0;
        }

        var registry = minecraft.player.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT);

        var pounce = registry.getOrThrow(POUNCE_KEY);

        int level = EnchantmentHelper.getItemEnchantmentLevel(
                pounce, boots
        );

        return 1 + Math.min(level, 6);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.level == null) {
            jumpsUsed = 0;
            previousJumpPressed = false;
            airJumpArmed = false;
            return;
        }

        boolean jumpPressed = minecraft.options.keyJump.isDown();

        int maximum = getMaximumAirJumps(minecraft);

        if (maximum == 0) {
            jumpsUsed = 0;
            previousJumpPressed = jumpPressed;
            airJumpArmed = false;
            return;
        }

        // Landing restores all available air jumps.
        if (minecraft.player.onGround()) {
            jumpsUsed = 0;
            airJumpArmed = false;
        }

        boolean canAirJump =
                !minecraft.player.onGround()
                        && !minecraft.player.isInWater()
                        && !minecraft.player.isInLava()
                        && !minecraft.player.isPassenger()
                        && !minecraft.player.getAbilities().flying
                        && !minecraft.player.isFallFlying();

        // The player must release Space in midair
        // before the next air jump can activate.
        if (canAirJump && !jumpPressed) {
            airJumpArmed = true;
        }

        boolean newJumpPress =
                jumpPressed && !previousJumpPressed;

        // Each fresh Space press consumes exactly
        // one available air jump.
        if (newJumpPress
                && airJumpArmed
                && canAirJump
                && jumpsUsed < maximum) {

            jumpsUsed++;

            // Require another release before
            // allowing the following air jump.
            airJumpArmed = false;

            // Match the server's air-jump horizontal impulse on the prediction side.
            // No movement-speed constants: preserve actual sprint/potion/modded momentum.
            var motion = minecraft.player.getDeltaMovement();
            minecraft.player.setDeltaMovement(motion.x * 1.35D, motion.y, motion.z * 1.35D);
            PacketDistributor.sendToServer(new CelestialDoubleJumpPayload());
        }

        previousJumpPressed = jumpPressed;

    }
}
