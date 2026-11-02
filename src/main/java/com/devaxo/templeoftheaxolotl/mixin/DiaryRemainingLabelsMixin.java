package com.devaxo.templeoftheaxolotl.mixin;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.common.book.Book;

/** Style only the progress label and hovered tooltips for the Axolotl diary. */
@Mixin(value = GuiBook.class, remap = false)
public abstract class DiaryRemainingLabelsMixin {
    private static final ResourceLocation DIARY = ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "diary_of_an_axolotl");
    private static final ResourceLocation LILOMON = ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "lilomon");

    @Shadow @Final public Book book;

    @ModifyArg(
        method = "drawProgressBar(Lnet/minecraft/client/gui/GuiGraphics;Lvazkii/patchouli/common/book/Book;IILjava/util/function/Predicate;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"),
        index = 1
    )
    private Component tota$progressLabel(Component original) {
        return DIARY.equals(book.id) ? original.copy().withStyle(Style.EMPTY.withFont(LILOMON)) : original;
    }

    @ModifyArg(
        method = "drawTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderComponentTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V"),
        index = 1
    )
    private List<Component> tota$hoverTooltip(List<Component> original) {
        if (!DIARY.equals(book.id)) return original;

        return original.stream()
                .<Component>map(line -> line.copy().withStyle(Style.EMPTY.withFont(LILOMON)))
                .toList();

    }
}
