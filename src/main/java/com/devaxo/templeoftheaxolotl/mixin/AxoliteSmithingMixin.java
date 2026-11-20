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
                 || input.base().is(net.minecraft.world.item.Items.IRON_BOOTS)
                 || input.base().is(net.minecraft.world.item.Items.IRON_PICKAXE)
                 || input.base().is(net.minecraft.world.item.Items.IRON_AXE)
                 || input.base().is(net.minecraft.world.item.Items.IRON_SHOVEL)
                 || input.base().is(net.minecraft.world.item.Items.IRON_HOE)
                 || input.base().is(net.minecraft.world.item.Items.IRON_SWORD));
    }

    @Inject(method = "matches(Lnet/minecraft/world/item/crafting/SmithingRecipeInput;Lnet/minecraft/world/level/Level;)Z", at = @At("RETURN"), cancellable = true)
    private void axoliteRequireHealthyIron(SmithingRecipeInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (input.base().is(ModItems.NOODLE_TRAINING_MEDAL.get()) &&
                (input.template().is(ModItems.NOODLE_TRAINING_MEDAL.get()) ||
                 !com.devaxo.templeoftheaxolotl.entity.NoodleInnates.kind(input.base()).isEmpty())) {
            cir.setReturnValue(!com.devaxo.templeoftheaxolotl.entity.NoodleInnates.smith(input).isEmpty());
            return;
        }
        if (cir.getReturnValue() && input.template().is(ModItems.AXOLITE_PLATING.get()) && com.devaxo.templeoftheaxolotl.item.AxolitePlating.armor(input.base()) && com.devaxo.templeoftheaxolotl.item.AxolitePlating.plated(input.base())) { cir.setReturnValue(false); return; }
        if (!cir.getReturnValue() || !axoliteRecipeInput(input)) return;
        ItemStack base = input.base();
        // Strictly more than half of the item's maximum durability must remain.
        if (base.getMaxDamage() <= 0 || (long)(base.getMaxDamage() - base.getDamageValue()) * 2 <= base.getMaxDamage()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "assemble(Lnet/minecraft/world/item/crafting/SmithingRecipeInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), cancellable = true)
    private void axoliteCopyDataAndRepair(SmithingRecipeInput input, HolderLookup.Provider registries, CallbackInfoReturnable<ItemStack> cir) {
        if (!cir.getReturnValue().isEmpty() && input.base().is(ModItems.NOODLE_TRAINING_MEDAL.get())) {
            ItemStack medal = com.devaxo.templeoftheaxolotl.entity.NoodleInnates.smith(input);
            if (!medal.isEmpty()) { cir.setReturnValue(medal); return; }
        }
        if (input.template().is(ModItems.AXOLITE_PLATING.get()) && input.addition().is(net.minecraft.world.item.Items.NETHERITE_BLOCK) && com.devaxo.templeoftheaxolotl.item.AxolitePlating.armor(input.base()) && !cir.getReturnValue().isEmpty()) { cir.setReturnValue(com.devaxo.templeoftheaxolotl.item.AxolitePlating.apply(input.base())); return; }
        if (input.template().is(ModItems.HEART_OF_THE_ABYSS.get()) && input.base().is(ModItems.AXOLITE_TRIDENT.get()) && input.addition().is(ModItems.COSMIC_ESSENCE.get()) && !cir.getReturnValue().isEmpty()) { var out=cir.getReturnValue(); out.applyComponents(input.base().getComponentsPatch()); out.set(DataComponents.ATTRIBUTE_MODIFIERS,new ItemStack(ModItems.COSMIC_TRIDENT.get()).get(DataComponents.ATTRIBUTE_MODIFIERS)); cir.setReturnValue(out); return; }
        if (com.devaxo.templeoftheaxolotl.item.AxolitePlating.tier(input.base())>0 && com.devaxo.templeoftheaxolotl.item.AxolitePlating.tier(cir.getReturnValue())>com.devaxo.templeoftheaxolotl.item.AxolitePlating.tier(input.base())) { var out=cir.getReturnValue(); out.applyComponents(input.base().getComponentsPatch()); cir.setReturnValue(out); return; }
        boolean upgrade = input.template().is(ModItems.AXOLITE_INGOT.get()) &&
                ((input.base().is(ModItems.AXOLITE_PICKAXE.get()) && input.addition().is(ModItems.AXOLITE_INGOT.get())) ||
                 (input.base().is(ModItems.AXOLITE_PICKAXE_TIER_2.get()) && input.addition().is(net.minecraft.world.item.Items.DIAMOND_BLOCK)));
        if (!axoliteRecipeInput(input) && !upgrade) return;
        ItemStack output = cir.getReturnValue();
        if (output.isEmpty()) return;
        // Preserve custom name, lore, enchants, trims, repair cost and compatible mod components.
        output.applyComponents(input.base().getComponentsPatch());
        if (upgrade) {
            int oldMax = input.base().getMaxDamage();
            int newMax = output.getMaxDamage();
            int scaledDamage = oldMax <= 0 ? 0 : (int)Math.round((double)input.base().getDamageValue() * newMax / oldMax);
            output.set(DataComponents.DAMAGE, Math.min(Math.max(0, scaledDamage), newMax - 1));
        } else {
            output.set(DataComponents.DAMAGE, 0);
        }
        // Native enchantments on smithing output, while preserving inherited compatible enchants.
        var registry = registries.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key = null;
        int level = 0;
        if (ModItems.isAxolitePickaxe(output)) {
            key = net.minecraft.world.item.enchantment.Enchantments.FORTUNE; level = 3;
        } else if (output.is(ModItems.AXOLITE_AXE.get())) {
            key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "treecapitator")); level = 3;
        } else if (output.is(ModItems.AXOLITE_SWORD.get())) {
            key = net.minecraft.world.item.enchantment.Enchantments.LOOTING; level = 2;
        }
        if (key != null) {
            final int enchantmentLevel = level;
            registry.get(key).ifPresent(enchant ->
                    output.enchant(enchant, Math.max(enchantmentLevel, output.getEnchantmentLevel(enchant))));
        }
        cir.setReturnValue(output);
    }
}
