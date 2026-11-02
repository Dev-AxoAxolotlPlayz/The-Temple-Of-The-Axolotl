package com.devaxo.templeoftheaxolotl.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import vazkii.patchouli.api.PatchouliAPI;

public final class DiaryOpeningScreen extends Screen {
    private static final ResourceLocation COVER = ResourceLocation.fromNamespaceAndPath(
            "temple_of_the_axolotl", "textures/gui/diary_cover.png");
    private static final ResourceLocation BOOK = ResourceLocation.fromNamespaceAndPath(
            "temple_of_the_axolotl", "diary_of_an_axolotl");
    private int age;
    private boolean opened;
    private DiaryOpeningScreen() { super(Component.literal("A forgotten diary")); }
    public static void open() { Minecraft.getInstance().setScreen(new DiaryOpeningScreen()); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void tick() {
        if (opened || ++age < 16) return;
        opened = true;
        Minecraft mc = Minecraft.getInstance();
        // Close the animation screen, then open Patchouli on the client.
        // A failed book lookup is reported rather than silently swallowing the error.
        mc.setScreen(null);
        try {
            if (PatchouliAPI.get().isStub()) {
                if (mc.player != null) mc.player.displayClientMessage(Component.literal(
                        "Patchouli API is unavailable. Check that Patchouli is installed and loaded."), false);
                return;
            }
            PatchouliAPI.get().openBookGUI(BOOK);
            if (mc.screen == null && mc.player != null) {
                mc.player.displayClientMessage(Component.literal(
                        "Patchouli did not open the diary. Check latest.log for Patchouli book loading errors."), false);
            }
        } catch (RuntimeException e) {
            if (mc.player != null) mc.player.displayClientMessage(Component.literal(
                    "Diary could not open: " + e.getMessage()), false);
            throw e;
        }
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xCC070F1B);
        float t = Math.min(1f, (age + partialTick) / 16f);
        float scale = 0.65f + t * 0.45f;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(width / 2f, height / 2f - 8, 0);
        pose.scale(scale, scale, 1f);
        graphics.blit(COVER, -48, -48, 0, 0, 96, 96, 96, 96);
        pose.popPose();
        graphics.drawCenteredString(font, "A memory stirs...", width / 2, height / 2 + 55, 0xA4E9F2);
    }
}
