package com.devaxo.templeoftheaxolotl.client.renderer;
import com.devaxo.templeoftheaxolotl.entity.MerolotlEntity;
import com.devaxo.templeoftheaxolotl.client.model.MerolotlModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
public class MerolotlRenderer extends GeoEntityRenderer<MerolotlEntity> {
    public MerolotlRenderer(EntityRendererProvider.Context context) { super(context, new MerolotlModel()); }
}
