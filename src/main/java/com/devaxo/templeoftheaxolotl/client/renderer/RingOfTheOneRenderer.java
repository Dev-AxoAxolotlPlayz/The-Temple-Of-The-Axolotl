package com.devaxo.templeoftheaxolotl.client.renderer;

import com.devaxo.templeoftheaxolotl.client.model.RingOfTheOneModel;
import com.devaxo.templeoftheaxolotl.item.RingOfTheOneItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** GeckoLib item renderer used for held, inventory and ItemDisplay rendering. */
public final class RingOfTheOneRenderer extends GeoItemRenderer<RingOfTheOneItem> {
    public RingOfTheOneRenderer() {
        super(new RingOfTheOneModel());
    }
}
