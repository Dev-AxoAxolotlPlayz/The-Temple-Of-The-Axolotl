
package com.devaxo.templeoftheaxolotl.client.model;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.entity.AxolotlAbsoluteEntity;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;

public class AxolotlAbsoluteModel extends GeoModel<AxolotlAbsoluteEntity> {

    @Override
    public ResourceLocation getModelResource(AxolotlAbsoluteEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(
                TempleOfTheAxolotl.MODID,
                "geo/axolotl_absolute.geo.json"
        );
    }

    @Override
    public ResourceLocation getTextureResource(AxolotlAbsoluteEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(
                TempleOfTheAxolotl.MODID,
                "textures/entity/axolotl_absolute.png"
        );
    }

    @Override
    public ResourceLocation getAnimationResource(AxolotlAbsoluteEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(
                TempleOfTheAxolotl.MODID,
                "animations/axolotl_absolute.animation.json"
        );
    }
}
