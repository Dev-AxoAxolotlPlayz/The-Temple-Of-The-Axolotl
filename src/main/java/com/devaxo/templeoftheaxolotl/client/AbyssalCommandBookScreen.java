package com.devaxo.templeoftheaxolotl.client;

import com.devaxo.templeoftheaxolotl.entity.AbyssalAxolotlEntity;
import com.devaxo.templeoftheaxolotl.entity.NoodleTraining;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Style;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Page one: shared conversational prefix and named companions. Other pages: copyable phrases. */
public final class AbyssalCommandBookScreen extends Screen {
    private static final int W = 340, H = 230;
    private static net.minecraft.nbt.CompoundTag roster = new net.minecraft.nbt.CompoundTag();
    public static void updateRoster(String serialized) {
        try { roster = net.minecraft.nbt.TagParser.parseTag(serialized); }
        catch (com.mojang.brigadier.exceptions.CommandSyntaxException ignored) { }
        if (Minecraft.getInstance().screen instanceof AbyssalCommandBookScreen screen) screen.rebuildWidgets();
    }
    private static int loreMask;
    private int loreSelected = -1;
    private int lorePage;
    public static void updateLore(int mask) {
        loreMask = mask;
        if (Minecraft.getInstance().screen instanceof AbyssalCommandBookScreen screen) screen.rebuildWidgets();
    }
    private static java.util.List<String> orderedCompanionIds() {
        java.util.List<String> ids = new java.util.ArrayList<>(roster.getAllKeys());
        java.util.Map<String,Integer> order = java.util.Map.of("Abyssal",0,"Celestial",1,"Normal",2);
        ids.sort(java.util.Comparator.comparingInt((String id) ->
                order.getOrDefault(roster.getCompound(id).getString("Category"), 0))
                .thenComparing(id -> roster.getCompound(id).getString("Name"), String.CASE_INSENSITIVE_ORDER));
        return ids;
    }
    private int companionPage;
    private int companionTab = 0;
    private static final String[] COMPANION_TABS = {"Abyssal", "Celestial", "Normal"};
    private int scrollTab = 0; // 0 = roster, 1 = requests; independent of category selection
    private java.util.List<String> visibleCompanionIds() {
        String category = COMPANION_TABS[companionTab];
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (String id : orderedCompanionIds()) {
            String stored = roster.getCompound(id).getString("Category");
            if ((stored.isEmpty() ? "Abyssal" : stored).equals(category)) ids.add(id);
        }
        return ids;
    }
    /** The roster is populated by server-owned UUID checks, never rendering distance. */
    private boolean hasBondedAxoluthu() {
        for (String id : roster.getAllKeys())
            if (roster.getCompound(id).getBoolean("AxoluthuBond")) return true;
        return false;
    }

    private boolean journalTabShown;
    @Override public void tick() {
        super.tick();
        if (!registryScroll && minecraft != null && minecraft.player != null
                && minecraft.player.tickCount % 20 == 0) {
            boolean bonded = hasBondedAxoluthu();
            if (bonded != journalTabShown) { journalTabShown = bonded; rebuildWidgets(); }
        }
    }
    private boolean rosterRequested;
    private void requestRoster() {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.devaxo.templeoftheaxolotl.network.AbyssalRosterRequestPayload());
    }

    private static final ResourceLocation TITLE_FONT = ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "bony_bones");
    private static final ResourceLocation BUTTON_FONT = ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "bony_bones");
    private static Component pirateButton(String value) { return Component.literal(value).withStyle(Style.EMPTY.withFont(BUTTON_FONT)); }
    private static final ResourceLocation TEXT_FONT = ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "bony_bones");
    private static Component pirateText(String value) { return Component.literal(value).withStyle(Style.EMPTY.withFont(TEXT_FONT)); }
    private static Component pirateTitle(String value) { return Component.literal(value).withStyle(Style.EMPTY.withFont(TITLE_FONT).withBold(true)); }
    /** Display the original entity custom-name component (font, color, decorations).
     * The roster is server-supplied and the serialized name comes from vanilla entity NBT. */
    /** The registry uses vanilla font; styled nametags remain untouched in the world. */
    private Component companionName(net.minecraft.nbt.CompoundTag entry) {
        return Component.literal(entry.getString("Name"));
    }
    private int categoryCount(String category) {
        int count = 0;
        for (String id : roster.getAllKeys()) {
            String stored = roster.getCompound(id).getString("Category");
            if ((stored.isEmpty() ? "Abyssal" : stored).equals(category)) count++;
        }
        return count;
    }

    private static final int PER_PAGE = 5;
    private static final net.minecraft.resources.ResourceLocation BODY_FONT = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "bony_bones");
    private static Component styled(String text, net.minecraft.resources.ResourceLocation font) {
        return Component.literal(text).withStyle(net.minecraft.network.chat.Style.EMPTY.withFont(font));
    }
    private void drawInk(GuiGraphics g, Object value, int x, int y, int color, boolean shadow) {
        if (value instanceof net.minecraft.util.FormattedCharSequence seq) g.drawString(font, seq, x, y, color, shadow);
        else if (value instanceof Component component) g.drawString(font, component, x, y, color, shadow);
        else g.drawString(font, styled(String.valueOf(value), BODY_FONT), x, y, color, shadow);
    }

    private static final String[][] BUILT_INS = {
        {"Follow", "follow me"}, {"Sit", "sit"}, {"Stay", "stay"},
        {"Stop", "stop"}, {"Fetch logs", "fetch me 16 logs"},
        {"Crafting materials", "fetch me the things to make three pistons"},
        {"Protect me", "please protect me"}
    };
    private final Map<String, List<String>> commands;
    private String selected;
    private int page, scroll;
    private int section = 0;
    private boolean confirmForget;
    private final boolean registryScroll;
    private long openedAt;
    private AbyssalCommandBookScreen(ItemStack book, boolean registryScroll) {
        super(Component.literal(registryScroll ? "Abyssal Companion Registry" : "Book of Axoluthu"));
        this.registryScroll = registryScroll;
        this.section = registryScroll ? 1 : 0;
        this.openedAt = System.currentTimeMillis();
        commands = new LinkedHashMap<>(NoodleTraining.getCommands(book));
    }
    public static void open(ItemStack book) { Minecraft.getInstance().setScreen(new AbyssalCommandBookScreen(book, false)); }
    public static void openRegistry(ItemStack book) { Minecraft.getInstance().setScreen(new AbyssalCommandBookScreen(book, true)); }
    private List<String[]> entries() {
        List<String[]> entries = new ArrayList<>();
        Collections.addAll(entries, BUILT_INS);
        commands.keySet().forEach(phrase -> entries.add(new String[]{"Training Medal", phrase}));
        return entries;
    }
    private int pageCount() { return 1 + Math.max(1, (entries().size() + PER_PAGE - 1) / PER_PAGE); }
    private String firstName() {
        if (minecraft == null || minecraft.player == null || minecraft.level == null) return "<name>";
        UUID uuid = minecraft.player.getUUID();
        for (var entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof AbyssalAxolotlEntity a && a.isTame() && a.getCustomName() != null
                    && uuid.equals(a.getOwnerUUID())) return a.getCustomName().getString();
        }
        return "<name>";
    }
    private String fullPhrase(String phrase) {
        return "hey " + firstName() + ", could you " + phrase;
    }
    private void copy(String phrase) {
        if (minecraft != null) minecraft.keyboardHandler.setClipboard(fullPhrase(phrase));
    }
    @Override protected void init() {
        super.init();
        if (!rosterRequested) { rosterRequested = true; requestRoster(); }
        int x = (width - W) / 2, y = (height - H) / 2;
        if (selected == null) page = Math.max(0, Math.min(page, pageCount() - 1));
        journalTabShown = !registryScroll && hasBondedAxoluthu();
        String[] sections = {"Field Guide"};
        if (journalTabShown) {
            addRenderableWidget(Button.builder(pirateButton("Architect's Journal"), b ->
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new com.devaxo.templeoftheaxolotl.network.AxoluthuJournalRequestPayload()))
                .bounds(x + 125, y + 31, 190, 18).build());
        }
        if (registryScroll) {
            String[] ribbons = {"Registry", "Requests"};
            for (int i = 0; i < ribbons.length; i++) {
                final int next = i;
                addRenderableWidget(new RibbonButton(x + W - 3, y + 58 + i * 40, 91, 22,
                        pirateButton(ribbons[i]), () -> {
                    scrollTab = next; section = next == 0 ? 1 : 2;
                    selected = null; page = 0; scroll = 0; companionPage = 0; rebuildWidgets();
                }));
            }
        }
        // Category ribbons live on the LEFT, only while viewing Registry.
        if (registryScroll && section == 1) {
            for (int i = 0; i < COMPANION_TABS.length; i++) {
                final int category = i;
                addRenderableWidget(new RibbonButton(x - 89, y + 62 + i * 38, 91, 22,
                        pirateButton(COMPANION_TABS[i] + " (" + categoryCount(COMPANION_TABS[i]) + ")"), () -> {
                    companionTab = category;
                    companionPage = 0;
                    rebuildWidgets();
                }));
            }
        }
        for (int index = 0; !registryScroll && index < sections.length; index++) {
            final int target = index;
            addRenderableWidget(Button.builder(pirateButton(sections[index]), b -> {
                section = target; companionPage = 0; rosterRequested = false; selected = null; loreSelected = -1; page = 0; scroll = 0; rebuildWidgets();
            }).bounds(x + 13 + index * 105, y + 31, 103, 18).build());
        }
        if (section != 0 || loreSelected < 0) addRenderableWidget(Button.builder(pirateButton("<"), b -> {
            if (section == 0) lorePage = Math.max(0,lorePage-1);
            else if (selected != null) scroll = Math.max(0, scroll - 5);
            else if (section == 1) companionPage = Math.max(0, companionPage - 1);
            else page = Math.max(0, page - 1);
            rebuildWidgets();
        }).bounds(x + 16, y + 196, 26, 20).build());
        if (section != 0 || loreSelected < 0) addRenderableWidget(Button.builder(pirateButton(">"), b -> {
            if (section == 0) lorePage = Math.min((com.devaxo.templeoftheaxolotl.lore.AbyssalLoreIndex.ENTRIES.length-1)/5,lorePage+1);
            else if (selected != null) scroll = Math.min(Math.max(0, commands.getOrDefault(selected, List.of()).size() - 5), scroll + 5);
            else if (section == 1) companionPage = Math.min(Math.max(0, (visibleCompanionIds().size() - 1) / 5), companionPage + 1);
            else page = Math.min(pageCount() - 1, page + 1);
            rebuildWidgets();
        }).bounds(x + 298, y + 196, 26, 20).build());
        // The index has a decorative Contents caption; a detail page has a real Back button.
        if (section == 0 && loreSelected >= 0) {
            addRenderableWidget(Button.builder(pirateButton("Back to Field Guide"), b -> {
                loreSelected = -1;
                rebuildWidgets();
            }).bounds(x + 99, y + 196, 142, 20).build());
        }
        if (section == 0 && loreSelected < 0) {
            var names = com.devaxo.templeoftheaxolotl.lore.AbyssalLoreIndex.ENTRIES;
            for (int i=0;i<5 && lorePage*5+i<names.length;i++) {
                final int idx=lorePage*5+i;
                boolean discovered=(loreMask & (1<<idx))!=0;
                addRenderableWidget(Button.builder(pirateButton(discovered?names[idx]:"??? Locked"),b->{
                    if (discovered) { loreSelected=idx; rebuildWidgets(); }
                }).bounds(x+22,y+70+i*23,290,20).build());
            }
        }
        if (section == 1) {
            for (int tab = 0; !registryScroll && tab < COMPANION_TABS.length; tab++) {
                final int next = tab;
                addRenderableWidget(Button.builder(pirateButton(
                        (tab == companionTab ? "[" : "") + COMPANION_TABS[tab] +
                        (tab == companionTab ? "]" : "")), b -> {
                    companionTab = next;
                    companionPage = 0;
                    rebuildWidgets();
                }).bounds((registryScroll ? x + W - 3 : x + 22 + tab * 99), (registryScroll ? y + 67 + tab * 37 : y + 80), (registryScroll ? 87 : 96), 18).build());
            }
            java.util.List<String> ids = visibleCompanionIds();
            for (int i = 0; i < 5 && companionPage * 5 + i < ids.size(); i++) {
                String id = ids.get(companionPage * 5 + i);
                var entry = roster.getCompound(id);
                String state = entry.getString("State");
                String action = state.equals("active") ? "Recall" : state.equals("stored") ? "Summon" : state.equals("pending") ? "Queued" : "Revive";
                final int actionColor = state.equals("stored") ? 0xFF3176D1
                        : state.equals("revive") ? 0xFFC73945 : 0xFF777777;
                addRenderableWidget(new CompanionActionButton(x + 235, y + 102 + i * 16,
                        77, 16, pirateButton(action), actionColor,
                        !state.equals("pending"), () ->
                        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                                new com.devaxo.templeoftheaxolotl.network.AbyssalCompanionActionPayload(id))));
            }
        }
        if (section == 2 && selected == null && page > 0) {
            List<String[]> all = entries();
            int start = (page - 1) * PER_PAGE;
            for (int i = 0; i < PER_PAGE && start + i < all.size(); i++) {
                String[] entry = all.get(start + i);
                int yy = y + 71 + i * 24;
                addRenderableWidget(Button.builder(pirateButton("Copy"), b -> copy(entry[1]))
                        .bounds(x + 266, yy, 51, 19).build());
                if (commands.containsKey(entry[1])) {
                    addRenderableWidget(Button.builder(pirateButton("View"), b -> {
                        selected = entry[1]; scroll = 0; confirmForget = false; rebuildWidgets();
                    }).bounds(x + 210, yy, 51, 19).build());
                }
            }
        } else if (section == 2 && selected != null) {
            addRenderableWidget(Button.builder(pirateButton("Copy"), b -> copy(selected))
                    .bounds(x + 224, y + 163, 75, 20).build());
            addRenderableWidget(Button.builder(pirateButton(confirmForget ? "Confirm Forget" : "Forget Command"), b -> {
                if (!confirmForget) { confirmForget = true; rebuildWidgets(); return; }
                if (minecraft != null && minecraft.getConnection() != null)
                    minecraft.getConnection().sendCommand("tota_noodle_forget " + selected);
                commands.remove(selected); selected = null; page = 1; scroll = 0; confirmForget = false; rebuildWidgets();
            }).bounds(x + 40, y + 163, 170, 20).build());
        }
    }
    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g, mx, my, partial);
        int x = (width - W) / 2, y = (height - H) / 2;
        if (registryScroll) {
            // The rollers remain fixed while the parchment unfurls between them.
            int elapsed = (int)Math.min(750, System.currentTimeMillis() - openedAt);
            float t = elapsed / 750.0F;
            float eased = 1.0F - (1.0F - t) * (1.0F - t);
            int bottom = y + 12 + (int)((H - 24) * eased);
            g.fill(x + 9, y + 12, x + W - 9, bottom, 0xFFF1DEB5);
            g.fill(x + 3, y + 5, x + W - 3, y + 18, 0xFF493020);
            g.fill(x, y + 4, x + 13, y + 19, 0xFFD5AA55);
            g.fill(x + W - 13, y + 4, x + W, y + 19, 0xFFD5AA55);
            g.fill(x + 3, bottom - 4, x + W - 3, bottom + 8, 0xFF493020);
            g.fill(x, bottom - 5, x + 13, bottom + 9, 0xFFD5AA55);
            g.fill(x + W - 13, bottom - 5, x + W, bottom + 9, 0xFFD5AA55);
            // Three matching left-hand velvet ribbons, only on the Registry page.
            if (section == 1) {
                for (int i = 0; i < COMPANION_TABS.length; i++) {
                    int ry = y + 62 + i * 38;
                    int rx = x - 89;
                    int shade = i == companionTab ? 0xFF9E293B : 0xFF671A2B;
                    g.fill(rx, ry - 3, rx + 94, ry + 22, shade);
                    g.fill(rx, ry - 3, rx + 94, ry - 1, 0xFFD6AC63);
                    g.fill(rx, ry + 20, rx + 94, ry + 22, 0xFFD6AC63);
                    g.fill(rx, ry + 22, rx + 12, ry + 27, shade);
                }
            }
            // Deep crimson ribbon tabs, gold trim, protruding beyond the writing area.
            for (int i = 0; i < 2; i++) {
                int ry = y + 58 + i * 40;
                int rx = x + W - 6;
                g.fill(rx, ry - 3, rx + 94, ry + 22, i == scrollTab ? 0xFF9E293B : 0xFF671A2B);
                g.fill(rx, ry - 3, rx + 94, ry - 1, 0xFFD6AC63);
                g.fill(rx, ry + 20, rx + 94, ry + 22, 0xFFD6AC63);
                // Pointed velvet tail, framed by a fine gold stitch.
                g.fill(rx + 82, ry + 22, rx + 94, ry + 27, 0xFF671A2B);
            }
        } else {
            g.fill(x, y, x + W, y + H, 0xFF60472F);
            g.fill(x + 5, y + 5, x + W - 5, y + H - 5, 0xFFEAD9B3);
            g.fill(x + 13, y + 13, x + W - 13, y + H - 11, 0xFFF7EACC);
        }
        if (registryScroll && System.currentTimeMillis() - openedAt < 750L) return;
        // Bony Bones font trial: bold title, no Minecraft text shadow.
        Component titleText = pirateTitle(registryScroll ? "Abyssal Companion Registry" : "Book of Axoluthu");
        float titleScale = 1.25F;
        g.pose().pushPose();
        // Keep the heading below the top wooden roller on the animated scroll.
        g.pose().translate(width / 2.0F, y + (registryScroll ? 30.0F : 12.0F), 0.0F);
        g.pose().scale(titleScale, titleScale, 1.0F);
        g.drawString(font, titleText, -font.width(titleText) / 2, 0, 0xFF533B27, false);
        g.pose().popPose();
        if (section == 0) {
            if (loreSelected >= 0 && loreSelected < com.devaxo.templeoftheaxolotl.lore.AbyssalLoreIndex.ENTRIES.length
                    && (loreMask & (1 << loreSelected)) != 0) {
                // Detail view is exclusive: never paint its text behind the index buttons.
                var names = com.devaxo.templeoftheaxolotl.lore.AbyssalLoreIndex.ENTRIES;
                Component entryTitle = pirateText(names[loreSelected]);
                g.drawString(font, entryTitle, x + (W - font.width(entryTitle)) / 2, y + 58, 0xFF533B27, false);
                wrapped(g, com.devaxo.templeoftheaxolotl.lore.AbyssalLoreText.text(loreSelected),
                        x + 24, y + 85, W - 48, 0xFF60452D, 9);
            } else {
                drawInk(g, pirateText("Abyssal Field Guide"), x + 24, y + 55, 0xFF533B27, false);
                // Keep the hint centered and below the last entry button.
                Component loreHint = pirateText("Find Abyssal Lore Blocks to unlock entries.");
                float hintScale = 0.75F;
                g.pose().pushPose();
                g.pose().translate(x + W / 2.0F, y + 183.0F, 0.0F);
                g.pose().scale(hintScale, hintScale, 1.0F);
                g.drawString(font, loreHint, -font.width(loreHint) / 2, 0, 0xFF765B42, false);
                g.pose().popPose();
            }
        } else if (section == 1) {
            drawInk(g, pirateText("Companion Registry"), x + 24, y + 62, 0xFF533B27, false);
            java.util.List<String> ids = visibleCompanionIds();
            int line = 0;
            for (int index = companionPage * 5; index < ids.size() && line < 5; index++) {
                String id = ids.get(index);
                var entry = roster.getCompound(id);
                String state = entry.getString("State");
                int color = 0xFF533B27;
                // Draw the actual inscribed name component, not a plain-string substitute.
                // Its font/color survive recall and revival through the saved entity NBT.
                Component name = companionName(entry);
                g.pose().pushPose();
                g.pose().translate(x + 30, y + 105 + line * 16, 0);
                float scale = font.width(name) > 198 ? 198.0F / font.width(name) : 1.0F;
                g.pose().scale(scale, scale, 1.0F);
                g.drawString(font, name, 0, 0, color, false);
                g.pose().popPose();
                line++;
            }
            if (line == 0) drawInk(g, "No companions registered yet.", x + 30, y + 105, 0xFF765B42, false);
        } else if (selected != null) {
            wrapped(g, selected, x + 23, y + 58, 290, 0xFF60452D, 2);
            List<String> tasks = commands.getOrDefault(selected, List.of());
            drawInk(g, tasks.size() + " tasks", x + 23, y + 80, 0xFF806A50, false);
            for (int i = 0; i < 5 && scroll + i < tasks.size(); i++)
                wrapped(g, (scroll + i + 1) + ". " + tasks.get(scroll + i), x + 24, y + 96 + i * 13, 290, 0xFF4C3C2E, 1);
        } else if (page == 0) {
            // Command introduction belongs below the section tabs (which end at y + 49).
            // Companion names belong exclusively in the Abyss Companions section.
            drawInk(g, "Talking to Your Companion", x + 24, y + 65, 0xFF533B27, false);
            drawInk(g, "Command prefix:", x + 24, y + 89, 0xFF533B27, false);
            wrapped(g, "hey <name>, could you <action>", x + 24, y + 105, 290, 0xFF765B42, 2);
            wrapped(g, "You can say please before the action or at the end.", x + 24, y + 136, 290, 0xFF765B42, 2);
            wrapped(g, "Turn the page for commands and copyable phrases.", x + 24, y + 165, 290, 0xFF765B42, 1);
        } else {
            drawInk(g, "Command phrases", x + 24, y + 55, 0xFF533B27, false);
            List<String[]> all = entries();
            int start = (page - 1) * PER_PAGE;
            for (int i = 0; i < PER_PAGE && start + i < all.size(); i++) {
                String[] entry = all.get(start + i);
                drawInk(g, entry[0] + ":", x + 24, y + 68 + i * 24, 0xFF806A50, false);
                wrapped(g, entry[1], x + 24, y + 80 + i * 24, commands.containsKey(entry[1]) ? 176 : 235, 0xFF4C3C2E, 1);
            }
        }
        if (section == 2 && selected == null) {
            Component counter = pirateText((page + 1) + " / " + pageCount());
            g.drawString(font, counter, (width - font.width(counter)) / 2, y + 185, 0xFF765B42, false);
        }
        if (section == 0 && loreSelected < 0) {
            Component contentsLabel = pirateText("Contents");
            g.drawString(font, contentsLabel, x + (W - font.width(contentsLabel)) / 2,
                    y + 201, 0xFF533B27, false);
        }
        // Delay interactive widgets until the unfurl completes.
        if (!registryScroll || System.currentTimeMillis() - openedAt >= 750L)
            super.render(g, mx, my, partial);
    }
    /** Text-only hitbox; the hand-drawn velvet ribbon remains visible underneath. */
    private static final class RibbonButton extends net.minecraft.client.gui.components.AbstractButton {
        private final Runnable action;
        RibbonButton(int x, int y, int width, int height, Component label, Runnable action) {
            super(x, y, width, height, label);
            this.action = action;
        }
        @Override public void onPress() { action.run(); }
        @Override protected void updateWidgetNarration(
                net.minecraft.client.gui.narration.NarrationElementOutput narration) {
            this.defaultButtonNarrationText(narration);
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int color = isHoveredOrFocused() ? 0xFFFFE4A8 : 0xFFF6E5D0;
            graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                    getX() + getWidth() / 2, getY() + 6, color);
        }
    }

    /** Custom colored action surface, preserving the existing gray/blue/red states. */
    private static final class CompanionActionButton extends net.minecraft.client.gui.components.AbstractButton {
        private final int color;
        private final Runnable action;
        private final boolean permitted;
        private CompanionActionButton(int x, int y, int width, int height, Component label,
                                      int color, boolean permitted, Runnable action) {
            super(x, y, width, height, label);
            this.color = color;
            this.action = action;
            this.permitted = permitted;
            this.active = permitted;
        }
        @Override public void onPress() { if (permitted) action.run(); }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int fill = isHoveredOrFocused() && permitted ? lighten(color) : color;
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF382C25);
            graphics.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1,
                    getY() + getHeight() - 1, fill);
            graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                    getX() + getWidth() / 2, getY() + 4, 0xFFFFFFFF);
        }
        private static int lighten(int color) {
            int r = Math.min(255, ((color >> 16) & 255) + 24);
            int g = Math.min(255, ((color >> 8) & 255) + 24);
            int b = Math.min(255, (color & 255) + 24);
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }
        @Override protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {
            defaultButtonNarrationText(narration);
        }
    }

    private void wrapped(GuiGraphics g, String text, int x, int y, int maxWidth, int color, int maxLines) {
        var lines = font.split(pirateText(text), maxWidth);
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++)
            drawInk(g, lines.get(i), x, y + i * 10, color, false);
    }
    @Override public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (selected != null) { scroll = Math.max(0, Math.min(Math.max(0, commands.getOrDefault(selected, List.of()).size() - 5), scroll - (int)Math.signum(dy))); return true; }
        return super.mouseScrolled(mx, my, dx, dy);
    }
    @Override public boolean isPauseScreen() { return false; }
}
