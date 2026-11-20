package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

/** A single item ID for ordinary, upgraded, and fused Training Medals. */
public class NoodleMedalItem extends Item {
    public NoodleMedalItem(Properties properties) { super(properties); }
    @Override public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        String kind = NoodleInnates.kind(stack);
        if (kind.isEmpty()) { lines.add(Component.literal("Base medal - upgrade at a smithing table").withStyle(ChatFormatting.GRAY)); return; }
        if (kind.equals("demi")) { lines.add(Component.literal("Fusion catalyst: combines two opposing medals").withStyle(ChatFormatting.AQUA)); return; }
        for (String trait : NoodleInnates.traits(stack))
            lines.add(Component.literal("Innate: " + trait.replace('_',' ')).withStyle(ChatFormatting.AQUA));
        lines.add(Component.literal("Capacity cost: " + NoodleInnates.cost(stack) + "/5").withStyle(ChatFormatting.GRAY));
        if (NoodleInnates.fused(stack)) lines.add(Component.literal("Demi fusion").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
