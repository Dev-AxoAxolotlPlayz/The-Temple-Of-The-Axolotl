package com.devaxo.templeoftheaxolotl.mixin;
import com.devaxo.templeoftheaxolotl.item.AxolitePlating;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ItemStack.class) public abstract class PlatedNameMixin {
 @Inject(method="getHoverName",at=@At("RETURN"),cancellable=true) private void platedName(CallbackInfoReturnable<Component> cir){if(AxolitePlating.plated((ItemStack)(Object)this)){var name=cir.getReturnValue();cir.setReturnValue(Component.literal("Axolite-Plated ").withStyle(name.getStyle()).append(name));}}
}
