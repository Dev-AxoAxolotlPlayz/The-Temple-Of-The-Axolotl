package com.devaxo.templeoftheaxolotl.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppresses only the camera hurt tilt following abyssal pressure damage. */
@Mixin(GameRenderer.class)
public abstract class AbyssPressureNoHurtCameraMixin {
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void tota$ignorePressureHurtTilt(CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player == null || player.hurtTime <= 0) return;
        var last = player.getLastDamageSource();
        if (last != null && last.is(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "abyss_pressure")))) {
            ci.cancel();
        }
    }
}
