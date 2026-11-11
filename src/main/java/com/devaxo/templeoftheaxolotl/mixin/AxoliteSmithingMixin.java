package com.devaxo.templeoftheaxolotl.mixin;

import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies to the Axolite ingot-template recipes only. */
@Mixin(SmithingTransformRecipe.class)
public abstract class AxoliteSmithingMixin {
    private static boolean axoliteRecipeInput(SmithingRecipeInput input) {
        return input.template().is(ModItems.AXOLITE_INGOT.get())
                && input.addition().is(net.minecraft.world.item.Items.DIAMOND)
                && (input.base().is(net.minecraft.world.item.Items.IRON_HELMET)
                 || input.base().is(net.minecraft.world.item.Items.IRON_CHESTPLATE)
                 || input.base().is(net.minecraft.world.item.Items.IRON_LEGGINGS)
                 || input.base().is(net.minecraft.world.item.Items.IRON_BOOTS));
    }

    @Inject(method = "matches(Lnet/minecraft/world/item/crafting/SmithingRecipeInput;Lnet/minecraft/world/level/Level;)Z", at = @At("RETURN"), cancellable = true)
    private void axoliteRequireHealthyIron(SmithingRecipeInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || !axoliteRecipeInput(input)) return;
        ItemStack base = input.base();
        // Strictly more than half of the item's maximum durability must remain.
        if (base.getMaxDamage() <= 0 || (long)(base.getMaxDamage() - base.getDamageValue()) * 2 <= base.getMaxDamage()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "assemble(Lnet/minecraft/world/item/crafting/SmithingRecipeInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), cancellable = true)
    private void axoliteCopyDataAndRepair(SmithingRecipeInput input, HolderLookup.Provider registries, CallbackInfoReturnable<ItemStack> cir) {
        if (!axoliteRecipeInput(input)) return;
        ItemStack output = cir.getReturnValue();
        if (output.isEmpty()) return;
        // Preserve custom name, lore, enchants, trims, repair cost and compatible mod components.
        output.applyComponents(input.base().getComponentsPatch());
        output.set(DataComponents.DAMAGE, 0);
        cir.setReturnValue(output);
    }
}
