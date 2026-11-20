package com.devaxo.templeoftheaxolotl.mixin;

import com.devaxo.templeoftheaxolotl.client.PromiseWearableRender;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hide head-slot equipment only during the local immersive body render. */
@Mixin(Player.class)
public abstract class PromiseFirstPersonEquipmentMixin {
    @Inject(method = "getItemBySlot", at = @At("HEAD"), cancellable = true)
    private void tota$hideRenderedHeadEquipment(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
        if (slot == EquipmentSlot.HEAD && PromiseWearableRender.isFirstPersonBody((Player)(Object)this)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
