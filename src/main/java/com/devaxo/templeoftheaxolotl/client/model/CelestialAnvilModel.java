package com.devaxo.templeoftheaxolotl.client.model;

import com.devaxo.templeoftheaxolotl.eclipse.EclipseAltarEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Renders the original hand-UV-mapped Celestial Anvil geometry. */
public final class CelestialAnvilModel extends GeoModel<EclipseAltarEntity> {
    private static final String NS = "temple_of_the_axolotl";
    @Override public ResourceLocation getModelResource(EclipseAltarEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(NS, "geo/celestial_anvil.geo.json");
    }
    @Override public ResourceLocation getTextureResource(EclipseAltarEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(NS, "textures/block/celestial_anvil_64.png");
    }
    @Override public ResourceLocation getAnimationResource(EclipseAltarEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(NS, "animations/celestial_anvil.animation.json");
    }
}
