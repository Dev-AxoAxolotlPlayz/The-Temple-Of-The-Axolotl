package com.devaxo.templeoftheaxolotl.client.model;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.AxoliteArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class AxoliteArmorModel extends GeoModel<AxoliteArmorItem> {
    private ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, path);
    }
    @Override public ResourceLocation getModelResource(AxoliteArmorItem item) {
        return resource("geo/axolite_armor.geo.json");
    }
    @Override public ResourceLocation getTextureResource(AxoliteArmorItem item) {
        return resource("textures/armor/axolite_armor.png");
    }
    @Override public ResourceLocation getAnimationResource(AxoliteArmorItem item) {
        return resource("animations/axolite_armor.animation.json");
    }
}
