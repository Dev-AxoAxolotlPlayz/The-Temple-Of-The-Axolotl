package com.devaxo.templeoftheaxolotl.lore;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.function.Consumer;

/** Uses the same Blockbench geometry for the item icon and the placed block. */
public final class LoreBlockItem extends BlockItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final boolean broken;
    public LoreBlockItem(Block block, Properties properties, boolean broken) {
        super(block, properties);
        this.broken = broken;
    }
    public boolean isBroken() { return broken; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) { }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private com.devaxo.templeoftheaxolotl.client.renderer.LoreBlockItemRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) renderer = new com.devaxo.templeoftheaxolotl.client.renderer.LoreBlockItemRenderer();
                return renderer;
            }
        });
    }
}
