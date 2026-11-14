package com.devaxo.templeoftheaxolotl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.List;

/** Live, read-only, single-page-at-a-time Architect's Journal. */
public final class AxoluthuArchitectJournalScreen extends Screen {
    private static final ResourceLocation BONES = ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "bony_bones");
    private static Component bones(String s) { return Component.literal(s).withStyle(Style.EMPTY.withFont(BONES)); }
    private static final int LINES_PER_PAGE = 12;
    private String content;
    private int page;
    private int liveSyncTicks;
    private Button previousButton, nextButton, claimButton, attentionButton, boundaryButton;
    private AxoluthuArchitectJournalScreen(String content) {
        super(Component.literal("Axoluthu's Architect's Journal"));
        this.content = content;
    }
    public static void open(String content) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof AxoluthuArchitectJournalScreen journal) {
            journal.content = content;
            journal.updateNavigation();
        } else mc.setScreen(new AxoluthuArchitectJournalScreen(content));
    }
    private List<List<Component>> pageContents() {
        List<List<Component>> result = new ArrayList<>();
        String normalized = content.replace("[CLAIMABLE_BLUEPRINT]", "").replace("\r", "");
        String[] sections = normalized.split("\\n\\[LEDGER]\\n", 2);
        for (String section : sections) {
            List<Component> lines = new ArrayList<>();
            for (String raw : section.split("\n", -1)) {
                if(raw.startsWith("PLOT #")&&!lines.isEmpty()){for(int j=0;j<lines.size();j+=LINES_PER_PAGE)result.add(new ArrayList<>(lines.subList(j,Math.min(j+LINES_PER_PAGE,lines.size()))));lines.clear();}
                if (raw.isEmpty()) { lines.add(bones(" ")); continue; }
                StringBuilder segment = new StringBuilder();
                for (String word : raw.split(" ", -1)) {
                    String candidate = segment.length() == 0 ? word : segment + " " + word;
                    if (font.width(bones(candidate)) > 245 && segment.length() > 0) {
                        lines.add(bones(segment.toString()));
                        segment.setLength(0); segment.append(word);
                    } else { segment.setLength(0); segment.append(candidate); }
                }
                if (segment.length() > 0) lines.add(bones(segment.toString()));
            }
            for (int i = 0; i < lines.size(); i += LINES_PER_PAGE)
                result.add(new ArrayList<>(lines.subList(i, Math.min(i + LINES_PER_PAGE, lines.size()))));
        }
        if (result.isEmpty()) result.add(new ArrayList<>());
        return result;
    }
    private int selectedPlot() {
        var all=pageContents(); int found=0;
        for(int i=0;i<=page&&i<all.size();i++)for(Component line:all.get(i)) {
            var m=java.util.regex.Pattern.compile("^PLOT #([0-9]+)").matcher(line.getString());
            if(m.find())found=Integer.parseInt(m.group(1));
        }
        return found;
    }
    private void command(String action) {
        int plot=selectedPlot(); var connection=Minecraft.getInstance().getConnection();
        if(plot>0&&connection!=null)connection.sendCommand("tota_buildphase "+action+" "+plot);
    }
    private int pages() { return pageContents().size(); }
    private void updateNavigation() {
        page = Math.max(0, Math.min(page, pages() - 1));
        if (previousButton != null) previousButton.active = page > 0;
        if (nextButton != null) nextButton.active = page + 1 < pages();
        if(attentionButton!=null)attentionButton.visible=selectedPlot()>0;
        if(boundaryButton!=null)boundaryButton.visible=selectedPlot()>0;
        if (claimButton != null) claimButton.visible = content.contains("[CLAIMABLE_BLUEPRINT]");
    }
    @Override public void tick() {
        super.tick();
        if (++liveSyncTicks >= 10) {
            liveSyncTicks = 0;
            PacketDistributor.sendToServer(new com.devaxo.templeoftheaxolotl.network.AxoluthuJournalRequestPayload());
        }
    }
    @Override protected void init() {
        int left = width / 2 - 145, top = height / 2 - 120;
        previousButton = addRenderableWidget(Button.builder(bones("< Previous"), b -> { page--; updateNavigation(); })
                .bounds(left + 10, top + 205, 94, 20).build());
        nextButton = addRenderableWidget(Button.builder(bones("Next >"), b -> { page++; updateNavigation(); })
                .bounds(left + 186, top + 205, 94, 20).build());
        claimButton = addRenderableWidget(Button.builder(bones("Claim Blueprint"), b ->
                PacketDistributor.sendToServer(new com.devaxo.templeoftheaxolotl.network.AxoluthuBlueprintClaimPayload()))
                .bounds(left + 90, top + 229, 110, 20).build());
        addRenderableWidget(Button.builder(bones("Close"), b -> onClose())
                .bounds(left + 200, top + 229, 80, 20).build());
        addRenderableWidget(Button.builder(bones("Refresh"), b ->
                PacketDistributor.sendToServer(new com.devaxo.templeoftheaxolotl.network.AxoluthuJournalRequestPayload()))
                .bounds(left + 10, top + 229, 80, 20).build());
        attentionButton=addRenderableWidget(Button.builder(bones("Needs Attention"),b->command("attention")).bounds(left+10,top+181,130,20).build());
        boundaryButton=addRenderableWidget(Button.builder(bones("Toggle Boundary"),b->command("boundary")).bounds(left+150,top+181,130,20).build());
        updateNavigation();
    }
    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g, mx, my, partial);
        int left = width / 2 - 145, top = height / 2 - 120;
        g.fill(left, top, left + 290, top + 256, 0xFFF1E3C7);
        g.renderOutline(left, top, 290, 256, 0xFF7D6654);
        g.drawCenteredString(font, bones("Axoluthu's Architect's Journal"), width / 2, top + 10, 0xFF513F38);
        g.fill(left + 12, top + 25, left + 278, top + 26, 0xFFAB9170);
        List<List<Component>> allPages = pageContents();
        List<Component> visible = allPages.get(Math.max(0, Math.min(page, allPages.size() - 1)));
        for (int i = 0; i < visible.size(); i++)
            g.drawString(font, visible.get(i), left + 17, top + 36 + i * 12, 0xFF46372E, false);
        g.drawCenteredString(font, bones("Page " + (page + 1) + " / " + pages()), width / 2, top + 210, 0xFF765B42);
        super.render(g, mx, my, partial);
    }
    @Override public boolean isPauseScreen() { return false; }
}
