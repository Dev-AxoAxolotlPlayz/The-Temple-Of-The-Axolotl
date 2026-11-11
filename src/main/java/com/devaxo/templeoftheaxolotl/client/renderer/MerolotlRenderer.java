package com.devaxo.templeoftheaxolotl.client.renderer;
import com.devaxo.templeoftheaxolotl.entity.MerolotlEntity;
import com.devaxo.templeoftheaxolotl.client.model.MerolotlModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
public class MerolotlRenderer extends GeoEntityRenderer<MerolotlEntity> {
    public MerolotlRenderer(EntityRendererProvider.Context context) { super(context, new MerolotlModel()); }
    // Yaw-independent bounds cover the scaled model even when its center is offscreen.
    @Override
    public boolean shouldRender(MerolotlEntity entity, Frustum frustum,
                                double camX, double camY, double camZ) {
        double horizontal = Math.max(2.0, entity.getBbWidth() * 2.0 * 1.5);
        double vertical = Math.max(2.0, entity.getBbHeight() * 2.0);
        AABB visualBounds = entity.getBoundingBox().inflate(horizontal, vertical, horizontal);
        return frustum.isVisible(visualBounds);
    }
}
