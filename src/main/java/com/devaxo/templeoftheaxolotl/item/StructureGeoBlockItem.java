package com.devaxo.templeoftheaxolotl.item;

import com.devaxo.templeoftheaxolotl.client.renderer.StructureItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.function.Consumer;

/** Geometry-backed inventory icon for the Watergate and Shipyard Crane. */
public final class StructureGeoBlockItem extends BlockItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String assetName;
    public StructureGeoBlockItem(Block block, Properties properties, String assetName) {
        super(block, properties);
        this.assetName = assetName;
    }
    public String assetName() { return assetName; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) { }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private StructureItemRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) renderer = new StructureItemRenderer();
                return renderer;
            }
        });
    }
}
