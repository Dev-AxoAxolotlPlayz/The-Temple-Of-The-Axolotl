
package com.devaxo.templeoftheaxolotl.client.renderer;

import com.devaxo.templeoftheaxolotl.client.model.CelestialArmorModel;
import com.devaxo.templeoftheaxolotl.item.CelestialArmorItem;

import net.minecraft.world.entity.EquipmentSlot;

import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class CelestialArmorRenderer
        extends GeoArmorRenderer<CelestialArmorItem> {

    public CelestialArmorRenderer() {
        super(new CelestialArmorModel());
    }

    @Override
    public GeoBone getBodyBone(GeoModel<CelestialArmorItem> model) {
        return model.getBone("bipedBody").orElse(null);
    }

    @Override
    public GeoBone getRightArmBone(GeoModel<CelestialArmorItem> model) {
        return model.getBone("bipedRightArm").orElse(null);
    }

    @Override
    public GeoBone getLeftArmBone(GeoModel<CelestialArmorItem> model) {
        return model.getBone("bipedLeftArm").orElse(null);
    }

    @Override
    public GeoBone getRightLegBone(GeoModel<CelestialArmorItem> model) {
        return model.getBone("bipedRightLeg").orElse(null);
    }

    @Override
    public GeoBone getLeftLegBone(GeoModel<CelestialArmorItem> model) {
        return model.getBone("bipedLeftLeg").orElse(null);
    }

    @Override
    public GeoBone getRightBootBone(GeoModel<CelestialArmorItem> model) {
        return model.getBone("bipedRightLeg").orElse(null);
    }

    @Override
    public GeoBone getLeftBootBone(GeoModel<CelestialArmorItem> model) {
        return model.getBone("bipedLeftLeg").orElse(null);
    }

    // Allow leggings to render body-mounted geometry,
    // including the animated axolotl tail.
    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        super.applyBoneVisibilityBySlot(currentSlot);

        if (currentSlot == EquipmentSlot.LEGS) {
            setBoneVisible(this.body, true);
        }
    }
}
