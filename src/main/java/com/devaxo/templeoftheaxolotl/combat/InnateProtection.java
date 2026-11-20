package com.devaxo.templeoftheaxolotl.combat;
public final class InnateProtection {
    private InnateProtection() {}
    public static boolean villager(net.minecraft.world.entity.Entity e) {
        return e instanceof net.minecraft.world.entity.npc.AbstractVillager;
    }
}
