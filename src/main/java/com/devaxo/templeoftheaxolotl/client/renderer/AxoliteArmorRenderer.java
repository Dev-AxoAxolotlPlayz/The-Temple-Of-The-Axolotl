package com.devaxo.templeoftheaxolotl.client.renderer;

import com.devaxo.templeoftheaxolotl.client.model.AxoliteArmorModel;
import com.devaxo.templeoftheaxolotl.item.AxoliteArmorItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/** Uses the existing Celestial/GeckoLib armor rendering pattern. */
public final class AxoliteArmorRenderer extends GeoArmorRenderer<AxoliteArmorItem> {
    public AxoliteArmorRenderer() { super(new AxoliteArmorModel()); }
}
