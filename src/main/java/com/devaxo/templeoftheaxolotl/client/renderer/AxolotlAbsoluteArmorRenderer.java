
package com.devaxo.templeoftheaxolotl.client.renderer;

import com.devaxo.templeoftheaxolotl.client.model.AxolotlAbsoluteArmorModel;
import com.devaxo.templeoftheaxolotl.item.AxolotlAbsoluteItem;

import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class AxolotlAbsoluteArmorRenderer
        extends GeoArmorRenderer<AxolotlAbsoluteItem> {

    public AxolotlAbsoluteArmorRenderer() {
        super(new AxolotlAbsoluteArmorModel());
    }

    @Override
    public GeoBone getHeadBone(GeoModel<AxolotlAbsoluteItem> model) {
        return model.getBone("bipedHead").orElse(null);
    }
}
