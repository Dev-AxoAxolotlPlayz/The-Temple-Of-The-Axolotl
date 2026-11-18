package com.devaxo.templeoftheaxolotl.mixin;
import com.devaxo.templeoftheaxolotl.entity.AxoliteTridentVisualState;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import com.devaxo.templeoftheaxolotl.client.renderer.AxoliteTridentMesh;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ThrownTridentRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ThrownTridentRenderer.class)
public class AxoliteTridentFlightRenderMixin {
    @Inject(method="render(Lnet/minecraft/world/entity/projectile/ThrownTrident;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At("HEAD"),cancellable=true)
    private void tota$flight(ThrownTrident entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light,CallbackInfo ci) {
        if(!((AxoliteTridentVisualState)entity).tota$isAxolite()) return;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partial,entity.yRotO,entity.getYRot())-90));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partial,entity.xRotO,entity.getXRot())+90));
        // Center the complete 47-Blockbench-unit mesh around the projectile's
        // renderer origin. Vanilla trident pitch/yaw and projectile physics remain in charge.
        // Mesh Y bounds: [-2.0625, 0.89164] after its original baked transforms.
        pose.translate(0.0D, 0.58543D, 0.0D);
        if (((AxoliteTridentVisualState)entity).tota$isCosmic()) {
            // Cosmic mesh is centered around its own midpoint, unlike the Axolite baked mesh.
            pose.translate(0.0D, -0.58543D, 0.0D);
            // Align the seven prongs with the projectile's forward direction,
            // rather than launching the pommel first.
            pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
            com.devaxo.templeoftheaxolotl.client.renderer.CosmicTridentMesh.render(pose,buffers,light,OverlayTexture.NO_OVERLAY);
        } else AxoliteTridentMesh.render(pose,buffers,light,OverlayTexture.NO_OVERLAY);
        pose.popPose(); ci.cancel();
    }
}
