package com.devaxo.templeoftheaxolotl.client.renderer;

import com.devaxo.templeoftheaxolotl.entity.WrathHeadHitbox;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Invisible proxy; F3+B still draws its native entity bounding box. */
public class WrathHeadHitboxRenderer extends EntityRenderer<WrathHeadHitbox> {
    public WrathHeadHitboxRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(WrathHeadHitbox entity) {
        return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    }
}
