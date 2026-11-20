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
    private static final String[] NAMES = {"Night Vision", "Conduit Power", "Mining", "Magnet", "Diary", "Field Guide", "Book of Axoluthu", "Companion Registry", "Abyssal Lantern", "Trident Mode", "Sustainable Farming", "Axoluthu’s Promise"};
    private int selected = -1;
    private static int moduleFlags = 0;
    public static void receiveState(int flags) { moduleFlags = flags; }
    private CelestialRadialWheel() { super(Component.literal("Celestial Wheel")); }
    public static final class Registration {
        public static void register(RegisterKeyMappingsEvent event) { event.register(OPEN); }
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.tickCount % 20 == 0) PacketDistributor.sendToServer(new com.devaxo.templeoftheaxolotl.network.TridentFovPayload(mc.options.fov().get()));
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
        if ((moduleFlags & (1 << 16)) != 0) result.add(10);
        if ((moduleFlags & (1 << 18)) != 0) result.add(11);
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
            double a=wedge*Math.PI*2/count;
            if(Math.abs(mx-(cx+Math.round(Math.sin(a)*100)))<=16 && Math.abs(my-(cy-Math.round(Math.cos(a)*100)))<=16) selected = actions.get(wedge);
        }
        g.fill(cx - 140, cy - 140, cx + 140, cy + 140, 0x450E243B);
        for (int position = 0; position < count; position++) {
            int index = actions.get(position);
            double angle = position * Math.PI * 2 / count;
            int x = cx + (int) Math.round(Math.sin(angle) * 100);
            int y = cy - (int) Math.round(Math.cos(angle) * 100);
            boolean enabled = index==10 ? (moduleFlags & (1<<17))!=0 : index==9 || index==11 || (moduleFlags & (1 << index)) != 0;
            g.fill(x - 16, y - 16, x + 16, y + 16, selected == index ? 0xDD287F9E : 0xBB1D354F);
            g.renderOutline(x - 16, y - 16, 32, 32, enabled ? 0xFF6CF1CB : 0xFF5D8199);
            ModuleIcons.item(g,x-8,y-8,index==11?12:index);

        }
        g.fill(cx - 27, cy - 18, cx + 27, cy + 18, 0xCC142638);
        ModuleIcons.glyph(g,cx-8,cy-8,"cancel");
        super.render(g, mx, my, partial);
        if(selected>=0) {
            boolean on=(moduleFlags & (1 << (selected==10?17:selected)))!=0;
            String state=selected==9?((moduleFlags & (1<<15))!=0?"Riptide X":"Loyalty V"):selected<4||selected==8||selected==10?(on?"ON":"OFF"):"READY";
            g.renderTooltip(font,Component.literal(NAMES[selected]+": "+state),mx,my);
        }
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (selected >= 0 && activeActions().contains(selected)) {
                PacketDistributor.sendToServer(new com.devaxo.templeoftheaxolotl.network.CelestialRadialActionPayload(selected));
            }
            if(selected != 11) onClose();
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
