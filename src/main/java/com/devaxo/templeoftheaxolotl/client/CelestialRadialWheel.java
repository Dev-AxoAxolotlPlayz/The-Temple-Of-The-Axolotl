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
    private static final String[] NAMES = {"Night Vision", "Conduit Power", "Mining", "Magnet", "Diary", "Field Guide", "Book of Axoluthu", "Architect Journal", "Abyssal Lantern"};
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
    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        int cx=width/2, cy=height/2;
        double dx=mx-cx, dy=my-cy;
        double distance=Math.sqrt(dx*dx+dy*dy);
        selected=distance < 28 || distance > 130 ? -1 : (int)Math.floor((Math.atan2(dx,-dy)+Math.PI/9+Math.PI*2)%(Math.PI*2)/(Math.PI*2/9));
        g.fill(cx-140,cy-140,cx+140,cy+140,0x450E243B);
        for (int i=0;i<9;i++) {
            double angle=i*2*Math.PI/9;
            int x=cx+(int)(Math.sin(angle)*100), y=cy-(int)(Math.cos(angle)*100);
            boolean available = i < 4 || (i == 8 ? (moduleFlags & 512) != 0 : (moduleFlags & (1 << i)) != 0);
            boolean enabled = (moduleFlags & (1 << i)) != 0;
            g.fill(x-38,y-18,x+38,y+18,!available ? 0x77505A66 : selected==i ? 0xDD287F9E : 0xBB1D354F);
            g.renderOutline(x-38,y-18,76,36,enabled ? 0xFF6CF1CB : 0xFF5D8199);
            g.drawCenteredString(font,NAMES[i],x,y-9,!available ? 0xFF909AA7 : selected==i ? 0xFFFFFFFF : 0xFFB4DDEB);
            g.drawCenteredString(font,(i < 4 || i == 8) ? (available ? (enabled ? "ON" : "OFF") : "EMPTY") : (available ? "READY" : "EMPTY"),x,y+3,enabled ? 0xFF79FFD4 : 0xFFB3B9C4);
        }
        g.fill(cx-27,cy-18,cx+27,cy+18,0xCC142638);
        g.drawCenteredString(font,"Cancel",cx,cy-4,0xFFE3ECFF);
        super.render(g,mx,my,partial);
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (selected >= 0 && (selected < 4 || (selected == 8 ? (moduleFlags & 512) != 0 : (moduleFlags & (1 << selected)) != 0))) {
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
