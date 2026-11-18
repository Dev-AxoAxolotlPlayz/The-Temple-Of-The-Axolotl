package com.devaxo.templeoftheaxolotl.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import vazkii.patchouli.client.base.ClientAdvancements;
import vazkii.patchouli.client.book.gui.GuiBookLanding;

/** Reveal only for the local player, retaining the original book and its discovery IDs. */
@Mixin(value = GuiBookLanding.class, remap = false)
public abstract class DiaryLandingRevealMixin {
    private boolean tota$revealed() {
        return ((vazkii.patchouli.client.book.gui.GuiBook)(Object)this).book.id.equals(
                ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "diary_of_an_axolotl"))
                && ClientAdvancements.hasDone("temple_of_the_axolotl:origin/above_all");
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lvazkii/patchouli/client/book/gui/BookTextRenderer;<init>(Lvazkii/patchouli/client/book/gui/GuiBook;Lnet/minecraft/network/chat/Component;II)V"), index = 1)
    private Component tota$opening(Component original) {
        return tota$revealed() ? Component.literal("My Diary.$(br2)Wrath, Keep your flippers off of it.$(br2)-Above All") : original;
    }

    @ModifyArg(method = "drawHeader", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I", ordinal = 0), index = 1)
    private Component tota$title(Component original) {
        return tota$revealed() ? Component.literal("Diary of an Axolotl").withStyle(Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "lilomon"))) : original;
    }
}
