package com.devaxo.templeoftheaxolotl.mixin;

import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.patchouli.common.book.Book;

/** Apply the existing Lilomon font only to the Diary's Patchouli UI text style. */
@Mixin(value = Book.class, remap = false)
public abstract class DiaryTitleFontMixin {
    private static final ResourceLocation DIARY =
            ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "diary_of_an_axolotl");
    private static final ResourceLocation LILOMON =
            ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "lilomon");

    @Shadow @Final public ResourceLocation id;

    @Inject(method = "getFontStyle", at = @At("RETURN"), cancellable = true)
    private void tota$useLilomonForDiaryTitles(CallbackInfoReturnable<Style> cir) {
        if (DIARY.equals(id)) {
            cir.setReturnValue(cir.getReturnValue().withFont(LILOMON));
        }
    }
}
