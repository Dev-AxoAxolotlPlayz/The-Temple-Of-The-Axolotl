package com.devaxo.templeoftheaxolotl.combat;

import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Server writes settings into the held Ring; normal inventory sync handles the client. */
public final class RingMiningMode {
    private static final String LEGACY = "temple_of_the_axolotl.ring_mining_mode";
    private static final String MINING = "tota_ring_mining", FARM = "tota_sustainable_farming";
    private RingMiningMode() {}
    private static boolean read(ItemStack stack,String key) {
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(ModItems.RING_OF_THE_ONE.get()) && data!=null && data.copyTag().getBoolean(key);
    }
    public static boolean isMining(ItemStack stack) { return read(stack,MINING); }
    public static boolean isMining(ServerPlayer p) { return isMining(p.getMainHandItem()); }
    public static boolean sustainable(ServerPlayer p) { return read(p.getMainHandItem(),FARM); }
    public static void migrate(ServerPlayer p) {
        ItemStack stack=p.getMainHandItem();
        if (!stack.is(ModItems.RING_OF_THE_ONE.get()) || !p.getPersistentData().contains(LEGACY)) return;
        var data=stack.get(DataComponents.CUSTOM_DATA);
        if (data==null || !data.copyTag().contains(MINING))
            write(stack,MINING,p.getPersistentData().getBoolean(LEGACY));
        p.getPersistentData().remove(LEGACY);
    }
    private static void write(ItemStack stack,String key,boolean value) {
        var data=stack.get(DataComponents.CUSTOM_DATA);
        var tag=data==null?new net.minecraft.nbt.CompoundTag():data.copyTag();
        tag.putBoolean(key,value); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }
    public static boolean toggleSustainable(ServerPlayer p) {
        if (!p.getMainHandItem().is(ModItems.RING_OF_THE_ONE.get())) return false;
        write(p.getMainHandItem(),FARM,!sustainable(p)); return true;
    }
    public static void toggle(ServerPlayer p) {
        if (!p.getMainHandItem().is(ModItems.RING_OF_THE_ONE.get())) return;
        migrate(p); write(p.getMainHandItem(),MINING,!isMining(p));
        RingBoomerangCombat.setHeld(p,false);
    }
}
