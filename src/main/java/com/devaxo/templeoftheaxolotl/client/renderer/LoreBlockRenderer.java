package com.devaxo.templeoftheaxolotl.client.renderer;

import com.devaxo.templeoftheaxolotl.client.model.LoreBlockModel;
import com.devaxo.templeoftheaxolotl.lore.LoreBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/** GeckoLib handles FACING itself. Do not apply a second yaw in render(). */
public final class LoreBlockRenderer extends GeoBlockRenderer<LoreBlockEntity> {
    public LoreBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new LoreBlockModel());
    }
}
