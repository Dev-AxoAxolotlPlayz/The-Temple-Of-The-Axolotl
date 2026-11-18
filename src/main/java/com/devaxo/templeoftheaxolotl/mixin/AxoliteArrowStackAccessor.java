package com.devaxo.templeoftheaxolotl.mixin;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(AbstractArrow.class)
public interface AxoliteArrowStackAccessor {
    @Accessor("pickupItemStack") ItemStack tota$pickupStack();
}
