package com.devaxo.templeoftheaxolotl.client.renderer;
import com.devaxo.templeoftheaxolotl.entity.TameableWrathEntity;
import com.devaxo.templeoftheaxolotl.client.model.TameableWrathModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;
import software.bernie.geckolib.cache.object.GeoBone;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
public class TameableWrathRenderer extends GeoEntityRenderer<TameableWrathEntity> {
    public TameableWrathRenderer(EntityRendererProvider.Context context) {
        super(context, new TameableWrathModel());
        addRenderLayer(new BlockAndItemGeoLayer<TameableWrathEntity>(this) {
            @Override
            protected ItemStack getStackForBone(GeoBone bone, TameableWrathEntity animatable) {
                return "weapon_anchor".equals(bone.getName())
                        ? animatable.getItemBySlot(EquipmentSlot.MAINHAND)
                        : ItemStack.EMPTY;
            }

            @Override
            protected ItemDisplayContext getTransformTypeForStack(GeoBone bone,
                    ItemStack stack, TameableWrathEntity animatable) {
                return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            }
        });
    }
}
