package com.devaxo.templeoftheaxolotl.client.model;

import com.devaxo.templeoftheaxolotl.entity.WaifuAxoluthuEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class WaifuAxoluthuModel extends GeoModel<WaifuAxoluthuEntity> {
    private static final String MODID = "temple_of_the_axolotl";
    @Override public ResourceLocation getModelResource(WaifuAxoluthuEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(MODID, "geo/waifu_axoluthu.geo.json");
    }
    @Override public ResourceLocation getTextureResource(WaifuAxoluthuEntity entity) {
        // Individual 512x512 frames avoid exceeding the GPU texture-height limit.
        int frame = Math.floorMod(entity.tickCount / 3, 50);
        return ResourceLocation.fromNamespaceAndPath(MODID,
                "textures/entity/waifu_axoluthu/ethereal_" + String.format(java.util.Locale.ROOT, "%02d", frame) + ".png");
    }
    @Override public ResourceLocation getAnimationResource(WaifuAxoluthuEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(MODID, "animations/waifu_axoluthu.animation.json");
    }
}
