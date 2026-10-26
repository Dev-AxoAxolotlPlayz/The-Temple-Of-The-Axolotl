package com.devaxo.templeoftheaxolotl.client.model;

import com.devaxo.templeoftheaxolotl.entity.BossWrathEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BossWrathModel extends GeoModel<BossWrathEntity> {
    private static ResourceLocation resource(String path) { return ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", path); }
    @Override public ResourceLocation getModelResource(BossWrathEntity entity) { return resource("geo/boss_fallen_wrath.geo.json"); }
    @Override public ResourceLocation getTextureResource(BossWrathEntity entity) { return resource("textures/entity/boss_fallen_wrath.png"); }
    @Override public ResourceLocation getAnimationResource(BossWrathEntity entity) { return resource("animations/boss_fallen_wrath.animation.json"); }
}
