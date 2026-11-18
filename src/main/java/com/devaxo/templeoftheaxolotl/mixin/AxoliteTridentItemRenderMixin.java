package com.devaxo.templeoftheaxolotl.mixin;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import com.devaxo.templeoftheaxolotl.client.renderer.AxoliteTridentMesh;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(BlockEntityWithoutLevelRenderer.class)
public class AxoliteTridentItemRenderMixin {
    @Inject(method="renderByItem",at=@At("HEAD"),cancellable=true)
    private void tota$trident(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay,CallbackInfo ci) {
        boolean cosmic = stack.is(ModItems.COSMIC_TRIDENT.get());
        if(!cosmic && !stack.is(ModItems.AXOLITE_TRIDENT.get())) return;
        pose.pushPose();
        // Move the visible model down by 12 Blockbench units in hand only.
        // The mesh coordinates are already baked; leave the thrown entity untouched.
        if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            pose.translate(0.0D, -12.0D / 16.0D, 0.0D);
        }
        // The Cosmic Blockbench mesh was already baked upright in model coordinates.
        // The Axolite mesh uses the older inverted-Y convention; do not reuse it.
        if (cosmic) pose.scale(-1,1,-1);
        else pose.scale(1,-1,-1);
        // Only the third-person hand layer needs a charging correction. Use the
        // entity currently rendered, NOT Minecraft.getInstance().player.
        if (context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            var holder = TridentChargeRenderContext.get();
            if (holder != null && holder.isUsingItem() && holder.getUseItem().is(stack.getItem())) {
                // Charge animation pitches the held shaft across the shoulders;
                // rotate around the model's shaft-normal axis instead of yawing it.
                pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0F));
            }
        }
        if(context==ItemDisplayContext.GUI) pose.scale(.55f,.55f,.55f);
        if (cosmic) com.devaxo.templeoftheaxolotl.client.renderer.CosmicTridentMesh.render(pose,buffers,light,overlay);
        else AxoliteTridentMesh.render(pose,buffers,light,overlay);
        pose.popPose(); ci.cancel();
    }
}
