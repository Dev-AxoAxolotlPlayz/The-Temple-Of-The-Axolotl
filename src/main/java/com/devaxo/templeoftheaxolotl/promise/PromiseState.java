package com.devaxo.templeoftheaxolotl.promise;

import com.devaxo.templeoftheaxolotl.compass.TempleHudModules;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import com.devaxo.templeoftheaxolotl.entity.WaifuAxoluthuEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** Server-owned foundation. No movement or automated actions in this stage. */
public final class PromiseState {
    public static final int SLOT = 12;
    private static final String RECEIVED = "tota_promise_received", SETTINGS = "tota_promise_settings", DELIVERY = "tota_promise_delivery";
    private PromiseState() {}
    public static boolean installed(ServerPlayer p) { return TempleHudModules.getSlot(p,SLOT).is(ModItems.AXOLUTHUS_PROMISE.get()); }
    public static CompoundTag settings(ServerPlayer p) { return p.getPersistentData().getCompound(SETTINGS); }
    public static void set(ServerPlayer p, CompoundTag value) { p.getPersistentData().put(SETTINGS,value.copy()); }
    public static boolean gift(ServerPlayer p) {
        if (p.getPersistentData().getBoolean(RECEIVED)) return false;
        if (installed(p) || p.getInventory().contains(new ItemStack(ModItems.AXOLUTHUS_PROMISE.get()))) {
            p.getPersistentData().putBoolean(RECEIVED,true); return false;
        }
        ItemStack stack = new ItemStack(ModItems.AXOLUTHUS_PROMISE.get());
        if (!p.getInventory().add(stack) && !stack.isEmpty()) p.drop(stack,false);
        p.getPersistentData().putBoolean(RECEIVED,true);
        p.sendSystemMessage(Component.literal("Axoluthu gifted you her Promise."));
        return true;
    }
    public static boolean requestDelivery(WaifuAxoluthuEntity a, ServerPlayer target) {
        if (a.level()!=target.level() || target.getPersistentData().getBoolean(RECEIVED) || installed(target)) return false;
        if (a.getPersistentData().hasUUID(DELIVERY)) return false;
        a.getPersistentData().putUUID(DELIVERY,target.getUUID());
        a.getPersistentData().putLong(DELIVERY+"_until",a.level().getGameTime()+1200);
        return true;
    }
    public static boolean deliveryTick(WaifuAxoluthuEntity a, ServerLevel level) {
        var data=a.getPersistentData();
        if (!data.hasUUID(DELIVERY)) return false;
        ServerPlayer p=level.getServer().getPlayerList().getPlayer(data.getUUID(DELIVERY));
        if (p==null || p.level()!=level || !p.isAlive() || level.getGameTime()>data.getLong(DELIVERY+"_until") || p.getPersistentData().getBoolean(RECEIVED)) {
            data.remove(DELIVERY); a.getNavigation().stop(); return false;
        }
        if (a.distanceToSqr(p)<=4) {
            gift(p); data.remove(DELIVERY); a.getNavigation().stop(); return false;
        }
        if (a.tickCount%10==0) a.getNavigation().moveTo(p,1.15);
        return true;
    }
}
