package com.devaxo.templeoftheaxolotl.client.renderer;
import com.devaxo.templeoftheaxolotl.client.model.AxokrakenModel;
import com.devaxo.templeoftheaxolotl.entity.AxokrakenEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
public class AxokrakenRenderer extends GeoEntityRenderer<AxokrakenEntity> {
    public AxokrakenRenderer(EntityRendererProvider.Context context) { super(context, new AxokrakenModel()); this.shadowRadius = 1.5F; }
    @Override public void render(AxokrakenEntity entity, float yaw, float partialTick, PoseStack poseStack,
                                 MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.scale(4.0F, 4.0F, 4.0F);
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
        poseStack.popPose();
    }
}
