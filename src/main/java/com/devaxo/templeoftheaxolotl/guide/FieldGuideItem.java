package com.devaxo.templeoftheaxolotl.guide;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import vazkii.patchouli.api.PatchouliAPI;

public final class FieldGuideItem extends Item {
    private static final ResourceLocation BOOK = ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "axolotls_field_guide");
    public FieldGuideItem(Properties properties) { super(properties); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) PatchouliAPI.get().openBookGUI(BOOK);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
