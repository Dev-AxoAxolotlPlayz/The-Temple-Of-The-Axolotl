package com.devaxo.templeoftheaxolotl.mixin;

import com.devaxo.templeoftheaxolotl.water.BoatWaveState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.world.entity.vehicle.Boat;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-only cosmetic rocking, applied inside BoatRenderer's pose stack.
 * This never changes boat collisions, controls or server movement.
 */
@Mixin(BoatRenderer.class)
public abstract class BoatRendererWaveMixin {
    @Inject(
        method = "render(Lnet/minecraft/world/entity/vehicle/Boat;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER),
        require = 1
    )
    private void temple$renderWaveTilt(Boat boat, float yaw, float partialTick,
                                       PoseStack poseStack, MultiBufferSource buffers,
                                       int packedLight, CallbackInfo ci) {
        BoatWaveState.Orientation state = BoatWaveState.get(boat);
        if (state == null || boat.level().getGameTime() - state.gameTick() > 2L) return;

        // The renderer subsequently rotates around Y by (180 - yaw).
        // Apply local hull rotations here, then let vanilla render as usual.
        float pitch = state.pitch();
        float roll = state.roll();
        if (Math.abs(pitch) < 0.001f && Math.abs(roll) < 0.001f) return;
        float radians = (float)(Math.PI / 180.0);
        float heading = (180.0f - yaw) * radians;
        Quaternionf rotation = new Quaternionf()
            .rotateY(heading)
            .rotateX(pitch * radians)
            .rotateZ(roll * radians)
            .rotateY(-heading);
        poseStack.mulPose(rotation);
    }
}
