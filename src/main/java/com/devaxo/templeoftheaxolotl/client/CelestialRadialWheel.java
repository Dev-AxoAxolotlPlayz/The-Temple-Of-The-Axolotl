package com.devaxo.templeoftheaxolotl.client;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Press C to toggle the radial menu; left-click a segment to activate; C/Esc cancels. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID, value = Dist.CLIENT)
public final class CelestialRadialWheel extends Screen {
    public static final KeyMapping OPEN = new KeyMapping("key.temple_of_the_axolotl.celestial_radial", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,"key.categories.temple_of_the_axolotl");
    private static final String[] NAMES = {"Night Vision", "Conduit Power", "Mining", "Magnet", "Diary", "Field Guide", "Book of Axoluthu", "Companion Registry", "Abyssal Lantern", "Trident Mode"};
    private int selected = -1;
    private static int moduleFlags = 0;
    public static void receiveState(int flags) { moduleFlags = flags; }
    private CelestialRadialWheel() { super(Component.literal("Celestial Wheel")); }
    public static final class Registration {
        public static void register(RegisterKeyMappingsEvent event) { event.register(OPEN); }
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.screen == null && OPEN.consumeClick()) {
            mc.setScreen(new CelestialRadialWheel());
            PacketDistributor.sendToServer(new com.devaxo.templeoftheaxolotl.network.CelestialRadialActionPayload(-1));
        }
    }
    @Override public boolean isPauseScreen() { return false; }
    /** Only actual equipped actions are drawn; packet indices remain unchanged. */
    private static java.util.List<Integer> activeActions() {
        java.util.List<Integer> result = new java.util.ArrayList<>();
        // Server-authoritative equipment bits, separate from enabled-state bits.
        for (int i = 0; i < 4; i++) if ((moduleFlags & (1 << (10 + i))) != 0) result.add(i);
        for (int i = 4; i <= 7; i++) if ((moduleFlags & (1 << i)) != 0) result.add(i);
        if ((moduleFlags & 512) != 0) result.add(8);
        if ((moduleFlags & (1 << 14)) != 0) result.add(9);
        return result;
    }
    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        int cx = width / 2, cy = height / 2;
        java.util.List<Integer> actions = activeActions();
        int count = actions.size();
        double dx = mx - cx, dy = my - cy;
        double distance = Math.hypot(dx, dy);
        selected = -1;
        if (count > 0 && distance >= 54 && distance <= 135) {
            double angle = (Math.atan2(dx, -dy) + Math.PI * 2) % (Math.PI * 2);
            int wedge = (int) Math.floor((angle + Math.PI / count) % (Math.PI * 2) / (Math.PI * 2 / count));
            selected = actions.get(wedge);
        }
        g.fill(cx - 140, cy - 140, cx + 140, cy + 140, 0x450E243B);
        for (int position = 0; position < count; position++) {
            int index = actions.get(position);
            double angle = position * Math.PI * 2 / count;
            int x = cx + (int) Math.round(Math.sin(angle) * 100);
            int y = cy - (int) Math.round(Math.cos(angle) * 100);
            boolean enabled = (moduleFlags & (1 << index)) != 0;
            g.fill(x - 38, y - 18, x + 38, y + 18, selected == index ? 0xDD287F9E : 0xBB1D354F);
            g.renderOutline(x - 38, y - 18, 76, 36, enabled ? 0xFF6CF1CB : 0xFF5D8199);
            g.drawCenteredString(font, NAMES[index], x, y - 9, selected == index ? 0xFFFFFFFF : 0xFFB4DDEB);
            g.drawCenteredString(font, index == 9 ? ((moduleFlags & (1 << 15)) != 0 ? "Riptide X" : "Loyalty V") : index < 4 || index == 8 ? (enabled ? "ON" : "OFF") : "READY", x, y + 3,
                    enabled ? 0xFF79FFD4 : 0xFFB3B9C4);
        }
        g.fill(cx - 27, cy - 18, cx + 27, cy + 18, 0xCC142638);
        g.drawCenteredString(font, "Cancel", cx, cy - 4, 0xFFE3ECFF);
        super.render(g, mx, my, partial);
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (selected >= 0 && activeActions().contains(selected)) {
                PacketDistributor.sendToServer(new com.devaxo.templeoftheaxolotl.network.CelestialRadialActionPayload(selected));
            }
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override public boolean keyPressed(int key,int scan,int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE || key == OPEN.getKey().getValue()) {
            onClose();
            return true;
        }
        return super.keyPressed(key,scan,mods);
    }
}
