package com.devaxo.templeoftheaxolotl.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import com.devaxo.templeoftheaxolotl.network.CyanHeartsPayload;

@EventBusSubscriber(modid="temple_of_the_axolotl")
public final class CyanHearts {
    private static final Map<UUID,Float> RESERVE=new HashMap<>();
    private static float get(ServerPlayer p){return Math.min(p.getMaxHealth()*.5f,RESERVE.getOrDefault(p.getUUID(),0f));}
    private static void set(ServerPlayer p,float value){
        float next=Math.max(0f,Math.min(p.getMaxHealth()*.5f,value));
        float previous=RESERVE.getOrDefault(p.getUUID(),0f);
        if(next==0f) RESERVE.remove(p.getUUID()); else RESERVE.put(p.getUUID(),next);
        if(previous!=next) CyanHeartsPayload.send(p,next);
    }
    public static void add(ServerPlayer p,float amount){set(p,get(p)+amount);}
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void damage(LivingDamageEvent.Pre e){
        if(!(e.getEntity() instanceof ServerPlayer p) || e.getNewDamage()<=0) return;
        float reserve=get(p), used=Math.min(reserve,e.getNewDamage());
        if(used>0){e.setNewDamage(e.getNewDamage()-used);set(p,reserve-used);}
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post e){if(e.getEntity() instanceof ServerPlayer p) set(p,get(p));}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p){RESERVE.remove(p.getUUID());CyanHeartsPayload.send(p,0f);}}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){RESERVE.remove(e.getEntity().getUUID());}
    @SubscribeEvent public static void death(LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p)set(p,0f);}
    @SubscribeEvent public static void stop(ServerStoppedEvent e){RESERVE.clear();}
}
