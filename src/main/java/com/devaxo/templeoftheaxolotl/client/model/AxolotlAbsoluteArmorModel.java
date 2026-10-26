package com.devaxo.templeoftheaxolotl.client.model;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.AxolotlAbsoluteItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AxolotlAbsoluteArmorModel extends GeoModel<AxolotlAbsoluteItem> {
    private ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, path);
    }

    @Override
    public ResourceLocation getModelResource(AxolotlAbsoluteItem item) {
        return resource("geo/axolotl_absolute_item.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AxolotlAbsoluteItem item) {
        return resource("textures/item/axolotl_absolute_item.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AxolotlAbsoluteItem item) {
        return resource("animations/axolotl_absolute_item.animation.json");
    }
}
