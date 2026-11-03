package com.devaxo.templeoftheaxolotl.client.model;

import com.devaxo.templeoftheaxolotl.entity.Shorkolotl100Entity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Shorkolotl100Model extends GeoModel<Shorkolotl100Entity> {
    private static final String NS = "temple_of_the_axolotl";
    @Override public ResourceLocation getModelResource(Shorkolotl100Entity entity) {
        return ResourceLocation.fromNamespaceAndPath(NS, "geo/shorkolotl_100.geo.json");
    }
    @Override public ResourceLocation getTextureResource(Shorkolotl100Entity entity) {
        return ResourceLocation.fromNamespaceAndPath(NS, "textures/entity/shorkolotl_100.png");
    }
    @Override public ResourceLocation getAnimationResource(Shorkolotl100Entity entity) {
        return ResourceLocation.fromNamespaceAndPath(NS, "animations/shorkolotl_100.animation.json");
    }
}
