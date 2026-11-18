package com.devaxo.templeoftheaxolotl.client.renderer;

import com.devaxo.templeoftheaxolotl.client.model.AttackTentacleModel;
import com.devaxo.templeoftheaxolotl.entity.AttackTentacleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.culling.Frustum;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AttackTentacleRenderer extends GeoEntityRenderer<AttackTentacleEntity> {
    public AttackTentacleRenderer(EntityRendererProvider.Context context) { super(context, new AttackTentacleModel()); }
    @Override public boolean shouldRender(AttackTentacleEntity entity, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(entity.getBoundingBox().inflate(12));
    }
}
