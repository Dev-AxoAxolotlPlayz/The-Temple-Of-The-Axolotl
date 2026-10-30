package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.compass.TempleHudModules;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import com.devaxo.templeoftheaxolotl.repository.CelestialRepository;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;

/** Server-only owner-bound logistics; never force-loads chunks. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class ClucklesLogistics {
    private static final String PREFIX = "tota_cluckles_";
    private ClucklesLogistics() {}
    public static boolean inbound(ServerPlayer p) { return !p.getPersistentData().contains(PREFIX+"in") || p.getPersistentData().getBoolean(PREFIX+"in"); }
    public static boolean outbound(ServerPlayer p) { return p.getPersistentData().getBoolean(PREFIX+"out"); }
    public static int radius(ServerPlayer p) { int r=p.getPersistentData().getInt(PREFIX+"radius"); return r==8||r==16||r==24||r==32?r:16; }
    public static int magnetMode(ServerPlayer p) { return Math.floorMod(p.getPersistentData().getInt(PREFIX+"magnet"),3); }
    public static void toggleInbound(ServerPlayer p) { p.getPersistentData().putBoolean(PREFIX+"in", !inbound(p)); }
    public static void toggleOutbound(ServerPlayer p) { p.getPersistentData().putBoolean(PREFIX+"out", !outbound(p)); }
    public static void cycleRadius(ServerPlayer p) { p.getPersistentData().putInt(PREFIX+"radius",radius(p)==32?8:radius(p)+8); }
    public static void cycleMagnet(ServerPlayer p) { p.getPersistentData().putInt(PREFIX+"magnet",magnetMode(p)==2 ? 0 : 2); }
    public static boolean enabled(ServerPlayer p) { return TempleHudModules.getSlot(p,7).is(ModItems.LOGISTICS_MAESTRO.get()); }

    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer owner) || owner.tickCount%40!=0 || !enabled(owner)) return;
        for (ClucklesEntity chicken : owner.serverLevel().getEntitiesOfClass(ClucklesEntity.class,
                new AABB(owner.blockPosition()).inflate(64), c -> c.isTame() && c.isOwnedBy(owner) && !c.isOrderedToSit())) {
            // One bounded chest operation per chicken per scan. No chunk loading or unloaded-inventory access.
            int r=radius(owner); BlockPos center=chicken.blockPosition();
            // Rotate through a bounded grid of 64 positions per cycle, never all 4k+ positions at once.
            int diameter=2*r+1;
            int total=diameter*diameter*3;
            int start=Math.floorMod(owner.tickCount/40*64,total);
            for (int n=0;n<64;n++) {
                int idx=(start+n)%total;
                int dx=idx%diameter-r;
                int dz=(idx/diameter)%diameter-r;
                int dy=idx/(diameter*diameter)-1;
                if (dx*dx+dz*dz>r*r) continue;
                BlockPos pos=center.offset(dx,dy,dz);
                if (!owner.serverLevel().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) continue;
                BlockEntity be=owner.serverLevel().getBlockEntity(pos);
                if (!(be instanceof Container container)) continue;
                if (outbound(owner) && distribute(owner,container)) return;
            }
        }
    }
    /** Only transfers to containers with an existing matching item (sorting by established contents). */
    private static boolean distribute(ServerPlayer owner, Container target) {
        for (int page=0;page<2;page++) {
            var entries=page==0?CelestialRepository.read(owner):CelestialRepository.readSecond(owner);
            for (int i=0;i<entries.size();i++) {
                var entry=entries.get(i);
                for (int slot=0;slot<target.getContainerSize();slot++) {
                    ItemStack current=target.getItem(slot);
                    if (current.isEmpty() || !ItemStack.isSameItemSameComponents(current,entry.sample()) || !target.canPlaceItem(slot,entry.sample())) continue;
                    int space=Math.min(target.getMaxStackSize(),current.getMaxStackSize())-current.getCount();
                    if (space<=0) continue;
                    ItemStack taken=page==0?CelestialRepository.extract(owner,i,Math.min(space,64)):CelestialRepository.extractSecond(owner,i,Math.min(space,64));
                    if (taken.isEmpty()) continue;
                    current.grow(taken.getCount());target.setChanged();return true;
                }
            }
        }
        return false;
    }
}
