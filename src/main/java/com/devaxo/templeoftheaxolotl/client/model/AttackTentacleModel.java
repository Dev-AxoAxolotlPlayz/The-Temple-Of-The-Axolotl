package com.devaxo.templeoftheaxolotl.client.model;

import com.devaxo.templeoftheaxolotl.entity.AttackTentacleEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AttackTentacleModel extends GeoModel<AttackTentacleEntity> {
    private static ResourceLocation resource(String path) { return ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", path); }
    @Override public ResourceLocation getModelResource(AttackTentacleEntity entity) { return resource("geo/attack_tentacle.geo.json"); }
    @Override public ResourceLocation getTextureResource(AttackTentacleEntity entity) { return resource("textures/entity/attack_tentacle.png"); }
    @Override public ResourceLocation getAnimationResource(AttackTentacleEntity entity) { return resource("animations/attack_tentacle.animation.json"); }
}
