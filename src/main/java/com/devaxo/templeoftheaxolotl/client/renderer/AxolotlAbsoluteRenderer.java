
package com.devaxo.templeoftheaxolotl.client.renderer;

import com.devaxo.templeoftheaxolotl.client.model.AxolotlAbsoluteModel;
import com.devaxo.templeoftheaxolotl.entity.AxolotlAbsoluteEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AxolotlAbsoluteRenderer
        extends GeoEntityRenderer<AxolotlAbsoluteEntity> {

    public AxolotlAbsoluteRenderer(EntityRendererProvider.Context context) {
        super(context, new AxolotlAbsoluteModel());
    }
}
