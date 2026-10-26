package com.devaxo.templeoftheaxolotl.client.renderer;
import com.devaxo.templeoftheaxolotl.brewing.CosmicBrewingEntity;
import com.devaxo.templeoftheaxolotl.client.model.CosmicBrewingModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
public class CosmicBrewingRenderer extends GeoBlockRenderer<CosmicBrewingEntity> {
 public CosmicBrewingRenderer(BlockEntityRendererProvider.Context context){super(new CosmicBrewingModel());}
}
