package com.devaxo.templeoftheaxolotl.client;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.network.InventoryStorageOpenPayload;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.IdentityHashMap;
import java.util.Map;

/** Stage 1: R/L/H/X tabs follow the active panel's left edge. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID, value = Dist.CLIENT)
public final class CelestialPanelTabs {
    private static final Map<AbstractContainerScreen<?>, Button[]> BUTTONS = new IdentityHashMap<>();
    private static boolean employedInSession;
    private static double savedCursorX, savedCursorY;
    private static boolean restoreCursor;
    private CelestialPanelTabs() {}

    private static int panel(AbstractContainerScreen<?> screen) {
        if (screen instanceof CelestialRepositoryScreen) return 0;
        if (screen instanceof TempleRewardScreen) return 1;
        if (screen instanceof ClucklesControlsScreen) return 4;
        if (screen instanceof CelestialModulesScreen) return 2;
        return -1;
    }

    private static boolean clucklesEmployed(AbstractContainerScreen<?> screen) {
        if (screen instanceof ClucklesControlsScreen) return true;
        if (screen instanceof CelestialModulesScreen modules) return modules.getMenu().hasCluckles() || employedInSession;
        // Other panels receive no live registry state; Cluckles tab is available from Modules.
        return employedInSession;
    }
    @SubscribeEvent public static void init(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        int active = panel(screen);
        if (active < 0) return;
        if (screen instanceof CelestialModulesScreen modules && modules.getMenu().hasCluckles()) employedInSession = true;
        String[] letters = {"R", "L", "H", "X", "C"};
        String[] names = {"Celestial Repository", "Dungeon Lootbag", "Celestial Modules", "Vanilla Inventory", "Cluckles Controls"};
        Button[] buttons = new Button[5];
        int next = 0;
        for (int i = 0; i < 5; i++) {
            if (i == active) continue;
            // Create the tab even before server menu data arrives; update visibility every frame.
            final int destination = i;
            Button button = Button.builder(Component.literal(letters[i]), b -> {
                    // Switching container screens must not teleport the pointer.
                    net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
                    savedCursorX = client.mouseHandler.xpos();
                    savedCursorY = client.mouseHandler.ypos();
                    restoreCursor = true;
                    if (destination == 3) {
                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                        if (mc.player != null) {
                            mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundContainerClosePacket(screen.getMenu().containerId));
                            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
                        }
                    } else {
                        PacketDistributor.sendToServer(new InventoryStorageOpenPayload(destination));
                    }
                })
                    .bounds(0, 0, 18, 18)
                    .tooltip(Tooltip.create(Component.literal(names[i])))
                    .build();
            buttons[next++] = button;
            event.addListener(button);
        }
        BUTTONS.clear();
        BUTTONS.put(screen, buttons);
        position(screen);
    }

    @SubscribeEvent public static void render(ScreenEvent.Render.Pre event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            if (restoreCursor) {
                restoreCursor = false;
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), savedCursorX, savedCursorY);
            }
            position(screen);
        }
    }

    private static void position(AbstractContainerScreen<?> screen) {
        Button[] buttons = BUTTONS.get(screen);
        if (buttons == null) return;
        int x = screen.getGuiLeft() - 20;
        if (x < 0) x = screen.width - 20; // Keep tabs reachable when the module GUI fills the left edge.
        int y = screen.getGuiTop() + 8;
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i] == null) continue;
            buttons[i].setX(x);
            buttons[i].setY(y + i * 20);
            boolean show = x >= 0 && (buttons[i].getMessage().getString().equals("C") ? clucklesEmployed(screen) : true);
            buttons[i].visible = show;
            buttons[i].active = show;
        }
    }
}
