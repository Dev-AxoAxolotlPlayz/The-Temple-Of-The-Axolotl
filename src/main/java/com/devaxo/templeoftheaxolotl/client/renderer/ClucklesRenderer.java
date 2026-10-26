package com.devaxo.templeoftheaxolotl.client.renderer;
import com.devaxo.templeoftheaxolotl.entity.ClucklesEntity;
import com.devaxo.templeoftheaxolotl.client.model.ClucklesModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
public class ClucklesRenderer extends GeoEntityRenderer<ClucklesEntity> {
    public ClucklesRenderer(EntityRendererProvider.Context context) { super(context, new ClucklesModel()); }
}
