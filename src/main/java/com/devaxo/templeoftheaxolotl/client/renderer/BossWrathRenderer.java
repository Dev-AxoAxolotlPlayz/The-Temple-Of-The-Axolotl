package com.devaxo.templeoftheaxolotl.client.renderer;

import com.devaxo.templeoftheaxolotl.entity.BossWrathEntity;
import com.devaxo.templeoftheaxolotl.client.model.BossWrathModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BossWrathRenderer extends GeoEntityRenderer<BossWrathEntity> {
    public BossWrathRenderer(EntityRendererProvider.Context context) { super(context, new BossWrathModel()); }
}
