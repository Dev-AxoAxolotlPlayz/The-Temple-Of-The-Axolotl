package com.devaxo.templeoftheaxolotl.client.model;

import com.devaxo.templeoftheaxolotl.lore.LoreBlockEntity;
import com.devaxo.templeoftheaxolotl.registry.ModBlocks;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class LoreBlockModel extends GeoModel<LoreBlockEntity> {
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", path);
    }
    private static boolean broken(LoreBlockEntity entity) {
        return entity.getBlockState().is(ModBlocks.BROKEN_LORE_BLOCK.get());
    }
    @Override public ResourceLocation getModelResource(LoreBlockEntity entity) {
        return id(broken(entity) ? "geo/broken_lore_block.geo.json" : "geo/lore_block.geo.json");
    }
    @Override public ResourceLocation getTextureResource(LoreBlockEntity entity) {
        return id(broken(entity) ? "textures/block/faded_lore_block.png" : "textures/block/lore_block.png");
    }
    @Override public ResourceLocation getAnimationResource(LoreBlockEntity entity) {
        return id("animations/lore_block.animation.json");
    }
}
