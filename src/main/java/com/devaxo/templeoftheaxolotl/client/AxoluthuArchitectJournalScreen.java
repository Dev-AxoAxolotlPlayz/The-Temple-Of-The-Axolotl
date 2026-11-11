package com.devaxo.templeoftheaxolotl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.List;

/** Live server-sourced parchment journal; refresh retrieves the latest saved project. */
public final class AxoluthuArchitectJournalScreen extends Screen {
    private String content;
    private int scroll;
    private AxoluthuArchitectJournalScreen(String content) {
        super(Component.literal("Axoluthu's Architect's Journal"));
        this.content = content;
    }
    public static void open(String content) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof AxoluthuArchitectJournalScreen journal) {
            journal.content = content;
            journal.scroll = 0;
        } else mc.setScreen(new AxoluthuArchitectJournalScreen(content));
    }
    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Refresh"), b ->
            PacketDistributor.sendToServer(new com.devaxo.templeoftheaxolotl.network.AxoluthuJournalRequestPayload()))
            .bounds(width / 2 - 100, height / 2 + 83, 96, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
            .bounds(width / 2 + 4, height / 2 + 83, 96, 20).build());
    }
    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g,mx,my,partial);
        int left=width/2-135, top=height/2-110;
        g.fill(left,top,left+270,top+190,0xFFF1E3C7);
        g.renderOutline(left,top,270,190,0xFF7D6654);
        g.drawCenteredString(font,"Axoluthu's Architect's Journal",width/2,top+10,0xFF513F38);
        List<String> lines = new ArrayList<>();
        for (String part : content.split("\n",-1)) {
            if (part.isEmpty()) { lines.add(""); continue; }
            for (int i=0;i<part.length();i+=43) lines.add(part.substring(i,Math.min(part.length(),i+43)));
        }
        for (int i=scroll;i<Math.min(lines.size(),scroll+14);i++)
            g.drawString(font,lines.get(i),left+12,top+30+(i-scroll)*10,0xFF46372E,false);
        g.drawString(font,"Scroll for more  |  Refresh to sync",left+12,top+176,0xFF7D6654,false);
        super.render(g,mx,my,partial);
    }
    @Override public boolean mouseScrolled(double mx,double my,double dx,double dy) {
        scroll=Math.max(0,scroll+(dy<0?3:-3));
        return true;
    }
    @Override public boolean isPauseScreen() { return false; }
}
