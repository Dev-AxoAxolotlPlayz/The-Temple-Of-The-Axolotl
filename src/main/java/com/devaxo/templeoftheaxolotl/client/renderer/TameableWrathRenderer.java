package com.devaxo.templeoftheaxolotl.client.renderer;
import com.devaxo.templeoftheaxolotl.entity.TameableWrathEntity;
import com.devaxo.templeoftheaxolotl.client.model.TameableWrathModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
public class TameableWrathRenderer extends GeoEntityRenderer<TameableWrathEntity> {
    public TameableWrathRenderer(EntityRendererProvider.Context context) { super(context, new TameableWrathModel()); }
}
