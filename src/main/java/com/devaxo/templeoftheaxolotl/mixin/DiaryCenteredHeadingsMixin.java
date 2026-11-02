package com.devaxo.templeoftheaxolotl.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.common.book.Book;

/** Only replace the font on Patchouli's centered headings for our diary. */
@Mixin(value = GuiBook.class, remap = false)
public abstract class DiaryCenteredHeadingsMixin {
    private static final ResourceLocation DIARY =
            ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "diary_of_an_axolotl");
    private static final ResourceLocation LILOMON =
            ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "lilomon");

    @Shadow @Final public Book book;

    @Inject(method = "drawCenteredStringNoShadow(Lnet/minecraft/client/gui/GuiGraphics;Ljava/lang/String;III)V",
            at = @At("HEAD"), cancellable = true)
    private void tota$centeredString(GuiGraphics graphics, String text, int x, int y, int color, CallbackInfo ci) {
        if (!DIARY.equals(book.id)) return;
        FormattedCharSequence styled = Component.literal(text).withStyle(Style.EMPTY.withFont(LILOMON)).getVisualOrderText();
        var font = net.minecraft.client.Minecraft.getInstance().font;
        graphics.drawString(font, styled, x - font.width(styled) / 2, y, color, false);
        ci.cancel();
    }

    @Inject(method = "drawCenteredStringNoShadow(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/util/FormattedCharSequence;III)V",
            at = @At("HEAD"), cancellable = true)
    private void tota$centeredFormatted(GuiGraphics graphics, FormattedCharSequence text, int x, int y, int color, CallbackInfo ci) {
        if (!DIARY.equals(book.id)) return;
        FormattedCharSequence styled = sink -> text.accept((index, style, codePoint) ->
                sink.accept(index, style.withFont(LILOMON), codePoint));
        var font = net.minecraft.client.Minecraft.getInstance().font;
        graphics.drawString(font, styled, x - font.width(styled) / 2, y, color, false);
        ci.cancel();
    }
}
