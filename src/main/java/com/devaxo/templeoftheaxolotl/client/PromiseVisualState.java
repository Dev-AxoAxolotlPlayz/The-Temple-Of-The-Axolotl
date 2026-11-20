package com.devaxo.templeoftheaxolotl.client;
import com.devaxo.templeoftheaxolotl.promise.PromiseSnapshot;
import java.util.*;
import net.minecraft.client.Minecraft;
/** Render-thread state; dimension changes invalidate stale remote anchors. */
public final class PromiseVisualState {
    private static final Map<UUID,PromiseSnapshot> STATES=new HashMap<>();
    private static Object world;
    private PromiseVisualState(){}
    public static void receive(UUID id,PromiseSnapshot state){ if(world!=Minecraft.getInstance().level){world=Minecraft.getInstance().level;STATES.clear();} if(!state.equipped())STATES.remove(id);else STATES.put(id,state); }
    public static PromiseSnapshot get(UUID id){var mc=Minecraft.getInstance();if(world!=mc.level){world=mc.level;STATES.clear();}return STATES.getOrDefault(id,PromiseSnapshot.EMPTY);}
}
