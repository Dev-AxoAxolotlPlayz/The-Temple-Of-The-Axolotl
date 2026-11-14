package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.SimpleContainer;

/**
 * Stage 2E/F: one explicitly scoped workstation recipe and safe placement.
 * All calls must run on the logical server thread. No existing blocks are replaced.
 */
public final class WaifuAxoluthuWorkshopActions {
    private WaifuAxoluthuWorkshopActions() {}
    public record Result(boolean success, String detail) {}

    public static int count(SimpleContainer inv, String material) {
        int total=0;
        for (int i=0;i<inv.getContainerSize();i++) {
            ItemStack stack=inv.getItem(i);
            if (material.equals("iron") ? stack.is(Items.IRON_INGOT) : stack.is(ItemTags.PLANKS))
                total+=stack.getCount();
        }
        return total;
    }
    private static int freeCapacity(SimpleContainer inv, ItemStack sample) {
        int count=0;
        for(int i=0;i<inv.getContainerSize();i++) {
            ItemStack stack=inv.getItem(i);
            if(stack.isEmpty()) count+=sample.getMaxStackSize();
            else if(ItemStack.isSameItemSameComponents(stack,sample))
                count+=Math.max(0,stack.getMaxStackSize()-stack.getCount());
        }
        return count;
    }
    private static void remove(SimpleContainer inv, String material, int amount) {
        int remaining=amount;
        for(int i=0;i<inv.getContainerSize() && remaining>0;i++) {
            ItemStack stack=inv.getItem(i);
            if(material.equals("iron") ? stack.is(Items.IRON_INGOT) : stack.is(ItemTags.PLANKS)) {
                int n=Math.min(remaining,stack.getCount());
                inv.removeItem(i,n);
                remaining-=n;
            }
        }
    }
    /** Returns the nearest loaded crafting table near Axoluthu's home. */
    public static BlockPos findCraftingTable(WaifuAxoluthuEntity axo) {
        if (!(axo.level() instanceof ServerLevel level)) return null;
        BlockPos home = axo.getHomePosition();
        if (home == null) return null;
        BlockPos best = null;
        double distance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(home.offset(-12,-3,-12),home.offset(12,3,12))) {
            if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4) || !level.getBlockState(pos).is(Blocks.CRAFTING_TABLE)) continue;
            double d = pos.distSqr(axo.blockPosition());
            if (d < distance) { distance=d; best=pos.immutable(); }
        }
        return best;
    }
    public static BlockPos findSmithingTable(WaifuAxoluthuEntity axo) {
        if(!(axo.level() instanceof ServerLevel level)) return null;
        BlockPos home=axo.getHomePosition();
        if(home==null) return null;
        for(BlockPos pos:BlockPos.betweenClosed(home.offset(-12,-3,-12),home.offset(12,3,12))) {
            if(level.getChunkSource().hasChunk(pos.getX()>>4,pos.getZ()>>4)
                    && level.getBlockState(pos).is(Blocks.SMITHING_TABLE)) return pos.immutable();
        }
        return null;
    }
    /**
     * Workstation navigation uses Minecraft's actual ground paths. The remote
     * interaction fallback remains available so an unreachable workstation
     * cannot softlock the autonomous crafting dependency chain.
     */
    private static final double WORKSTATION_REACH_SQR = 3.5 * 3.5;
    private static final double REMOTE_FALLBACK_SQR = 24.0 * 24.0;
    private static final java.util.Map<WaifuAxoluthuEntity, StationRoute> ROUTES = new java.util.WeakHashMap<>();
    private static final class StationRoute {
        final BlockPos station;
        final BlockPos stand;
        long lastAttempt;
        net.minecraft.world.phys.Vec3 lastPosition;
        int stagnantTicks;
        StationRoute(BlockPos station, BlockPos stand, long time, net.minecraft.world.phys.Vec3 position) {
            this.station=station; this.stand=stand; this.lastAttempt=time; this.lastPosition=position;
        }
    }
    public static boolean canInteract(WaifuAxoluthuEntity axo, BlockPos station) {
        if (!(axo.level() instanceof ServerLevel level) || station==null) return false;
        if (!level.getChunkSource().hasChunk(station.getX()>>4,station.getZ()>>4)) return false;
        if (axo.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(station)) <= WORKSTATION_REACH_SQR) {
            ROUTES.remove(axo);
            return true;
        }
        return false;
    }
    public static boolean approachStation(WaifuAxoluthuEntity axo, BlockPos station) {
        if (!(axo.level() instanceof ServerLevel level) || station==null) return false;
        if (axo.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(station))<=WORKSTATION_REACH_SQR) {
            ROUTES.remove(axo);
            return true;
        }
        long now=level.getGameTime();
        StationRoute previous=ROUTES.get(axo);
        if(previous!=null && previous.station.equals(station)) {
            if(previous.lastPosition.distanceToSqr(axo.position()) > .25) {
                previous.lastPosition=axo.position(); previous.stagnantTicks=0;
            } else previous.stagnantTicks++;
            if(!axo.getNavigation().isDone() && previous.stagnantTicks<50) return false;
            if(now-previous.lastAttempt<20 && previous.stagnantTicks<50) return false;
            ROUTES.remove(axo);
        }
        // A valid standing tile must have floor support, headroom, AND the same
        // Y as the workstation. A successful path is mandatory.
        net.minecraft.world.level.pathfinder.Path bestPath=null;
        BlockPos bestStand=null;
        int bestLength=Integer.MAX_VALUE;
        for(net.minecraft.core.Direction dir:net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos stand=station.relative(dir);
            if(!level.getChunkSource().hasChunk(stand.getX()>>4,stand.getZ()>>4)) continue;
            if(!level.getBlockState(stand).getCollisionShape(level,stand).isEmpty()) continue;
            if(!level.getBlockState(stand.above()).getCollisionShape(level,stand.above()).isEmpty()) continue;
            if(!level.getBlockState(stand.below()).isFaceSturdy(level,stand.below(),net.minecraft.core.Direction.UP)) continue;
            net.minecraft.world.level.pathfinder.Path path=axo.getNavigation().createPath(stand,0);
            if(path==null || !path.canReach()) continue;
            if(path.getNodeCount()<bestLength) {bestPath=path;bestStand=stand;bestLength=path.getNodeCount();}
        }
        if(bestPath!=null && axo.getNavigation().moveTo(bestPath,1.05)) {
            ROUTES.put(axo,new StationRoute(station,bestStand,now,axo.position()));
            return false;
        }
        // No reachable route: do not issue direct movement through a wall.
        ROUTES.remove(axo);
        return axo.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(station))<=REMOTE_FALLBACK_SQR;
    }
    public static boolean approachCraftingTable(WaifuAxoluthuEntity axo, BlockPos table) {
        return approachStation(axo,table);
    }
    public static boolean isNearCraftingTable(WaifuAxoluthuEntity axo) {
        return canInteract(axo, findCraftingTable(axo));
    }
    public static boolean hasCraftingTableItem(WaifuAxoluthuEntity axo) {
        return countItem(axo.getAcquiredMaterials(), Items.CRAFTING_TABLE)>0;
    }
    public static boolean hasSmithingTableItem(WaifuAxoluthuEntity axo) {
        return countItem(axo.getAcquiredMaterials(), Items.SMITHING_TABLE)>0;
    }
    /** 2x2 crafting: four planks make one crafting table without a workstation. */
    public static Result craftCraftingTable(WaifuAxoluthuEntity axo) {
        if (!(axo.level() instanceof ServerLevel)) return new Result(false,"SERVER_ONLY");
        if (!axo.getSelectedDesire().equals("IMPROVE_WORKSHOP")) return new Result(false,"NO_WORKSHOP_PLAN");
        if (findCraftingTable(axo)!=null) return new Result(false,"CRAFTING_TABLE_ALREADY_AVAILABLE");
        if (hasCraftingTableItem(axo)) return new Result(false,"CRAFTING_TABLE_ALREADY_CARRIED");
        SimpleContainer inv=axo.getAcquiredMaterials();
        if (count(inv,"planks")<4) return new Result(false,"MISSING_PLANKS_FOR_CRAFTING_TABLE");
        SimpleContainer simulated=copyInventory(inv);
        remove(simulated,"planks",4);
        if (freeCapacity(simulated,new ItemStack(Items.CRAFTING_TABLE))<1)
            return new Result(false,"NO_OUTPUT_SLOT_FOR_CRAFTING_TABLE");
        remove(inv,"planks",4);
        addOne(inv,Items.CRAFTING_TABLE);
        axo.recordWorkshopCraft("CRAFTED crafting_table using 4 planks (2x2)");
        return new Result(true,"CRAFTED crafting table using 4 planks");
    }
    private static SimpleContainer copyInventory(SimpleContainer original) {
        SimpleContainer copy=new SimpleContainer(original.getContainerSize());
        for(int i=0;i<original.getContainerSize();i++) copy.setItem(i,original.getItem(i).copy());
        return copy;
    }
    private static void addOne(SimpleContainer inv,net.minecraft.world.item.Item item) {
        for(int i=0;i<inv.getContainerSize();i++) {
            ItemStack stack=inv.getItem(i);
            if(stack.is(item) && stack.getCount()<stack.getMaxStackSize()) {
                stack.grow(1); inv.setChanged(); return;
            }
        }
        for(int i=0;i<inv.getContainerSize();i++)
            if(inv.getItem(i).isEmpty()) {inv.setItem(i,new ItemStack(item));return;}
    }
    private static BlockPos findSafePlacement(WaifuAxoluthuEntity axo, int radius) {
        if (!(axo.level() instanceof ServerLevel level)) return null;
        BlockPos home=axo.getHomePosition();
        if(home==null) return null;
        BlockPos selected=null;
        double best=Double.MAX_VALUE;
        for(BlockPos pos:BlockPos.betweenClosed(home.offset(-radius,-2,-radius),home.offset(radius,2,radius))) {
            if(!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) continue;
            if(pos.distSqr(axo.blockPosition())>49) continue;
            if(!level.getBlockState(pos).isAir() || !level.getFluidState(pos).isEmpty()) continue;
            if(!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),net.minecraft.core.Direction.UP)) continue;
            if(!level.getEntities(axo,new AABB(pos),entity->entity.isAlive()).isEmpty()) continue;
            double score=pos.distSqr(home)+pos.distSqr(axo.blockPosition())*.25;
            if(score<best) {best=score;selected=pos.immutable();}
        }
        BlockPos pending=axo.getPendingWorkshopPosition();
        if(pending!=null && isSafeTarget(level,axo,pending)) return pending;
        return selected;
    }
    private static boolean isSafeTarget(ServerLevel level,WaifuAxoluthuEntity axo,BlockPos pos) {
        return level.getChunkSource().hasChunk(pos.getX() >> 4,pos.getZ() >> 4)
                && level.getBlockState(pos).isAir() && level.getFluidState(pos).isEmpty()
                && level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),net.minecraft.core.Direction.UP)
                && level.getEntities(axo,new AABB(pos),e->e.isAlive()).isEmpty();
    }
    private static Result approachPlacement(WaifuAxoluthuEntity axo,BlockPos target,String type) {
        if(!(axo.level() instanceof ServerLevel level) || !isSafeTarget(level,axo,target))
            return new Result(false,"PLACEMENT_TARGET_BLOCKED");
        // Center-to-center distance within 2.75 blocks, rather than remote placement.
        if(axo.distanceToSqr(target.getX()+.5,target.getY()+.5,target.getZ()+.5)<=2.75*2.75) {
            axo.clearPendingWorkshopPosition();
            return new Result(true,"IN_REACH");
        }
        axo.setPendingWorkshopPosition(target,type);
        // Path to an adjacent AIR block, not inside the future workstation block.
        BlockPos stand=null;
        double nearest=Double.MAX_VALUE;
        for(net.minecraft.core.Direction direction:net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos p=target.relative(direction);
            if(!level.getChunkSource().hasChunk(p.getX() >> 4,p.getZ() >> 4)) continue;
            if(!level.getBlockState(p).isAir() || !level.getBlockState(p.above()).isAir()) continue;
            if(!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),net.minecraft.core.Direction.UP)) continue;
            double distance=p.distSqr(axo.blockPosition());
            if(distance<nearest) {nearest=distance;stand=p;}
        }
        if(stand==null) { axo.clearPendingWorkshopPosition(); return new Result(false,"NO_REACHABLE_STANDING_SPACE near "+target.toShortString()); }
        if(!axo.getNavigation().moveTo(stand.getX()+.5,stand.getY(),stand.getZ()+.5,.85)) {
            axo.clearPendingWorkshopPosition();
            return new Result(false,"PATH_TO_PLACEMENT_FAILED "+stand.toShortString());
        }
        return new Result(false,"APPROACHING_"+type.toUpperCase()+" placement="+target.toShortString()+" stand="+stand.toShortString());
    }
    public static Result placeCraftingTable(WaifuAxoluthuEntity axo) {
        if(!(axo.level() instanceof ServerLevel level)) return new Result(false,"SERVER_ONLY");
        if(!axo.getSelectedDesire().equals("IMPROVE_WORKSHOP")) return new Result(false,"NO_WORKSHOP_PLAN");
        if(findCraftingTable(axo)!=null) return new Result(false,"CRAFTING_TABLE_ALREADY_AVAILABLE");
        if(!hasCraftingTableItem(axo)) return new Result(false,"MISSING_CRAFTING_TABLE_ITEM");
        BlockPos target=findSafePlacement(axo,6);
        if(target==null) return new Result(false,"NO_SAFE_CRAFTING_TABLE_POSITION");
        Result approach=approachPlacement(axo,target,"crafting_table");
        if(!approach.success()) return approach;
        if(!level.setBlock(target,Blocks.CRAFTING_TABLE.defaultBlockState(),3))
            return new Result(false,"CRAFTING_TABLE_PLACE_REJECTED");
        removeItem(axo.getAcquiredMaterials(),Items.CRAFTING_TABLE);
        axo.recordWorkshopStep("PLACED crafting_table at "+target.toShortString());
        return new Result(true,"PLACED crafting table at "+target.toShortString());
    }
    private static void removeItem(SimpleContainer inv,net.minecraft.world.item.Item item) {
        for(int i=0;i<inv.getContainerSize();i++) if(inv.getItem(i).is(item)) {
            inv.removeItem(i,1);return;
        }
    }
    /** 3x3 crafting requires an actual nearby crafting table. */
    public static Result craft(WaifuAxoluthuEntity axo) {
        if(!(axo.level() instanceof ServerLevel)) return new Result(false,"SERVER_ONLY");
        if(!axo.getSelectedDesire().equals("IMPROVE_WORKSHOP")) return new Result(false,"NO_WORKSHOP_PLAN");
        BlockPos table=findCraftingTable(axo);
        if(table==null) return new Result(false,"MISSING_WORKSTATION: CRAFTING_TABLE");
        if(!canInteract(axo,table)) {
            approachStation(axo,table);
            return new Result(false,"APPROACH_CRAFTING_TABLE: "+table.toShortString());
        }
        SimpleContainer inv=axo.getAcquiredMaterials();
        if(hasSmithingTableItem(axo)) return new Result(false,"ALREADY_CRAFTED");
        int iron=count(inv,"iron"),planks=count(inv,"planks");
        if(iron<2 || planks<4) return new Result(false,"MISSING_INGREDIENTS iron="+iron+"/2 planks="+planks+"/4");
        SimpleContainer simulation=copyInventory(inv);
        remove(simulation,"iron",2);remove(simulation,"planks",4);
        if(freeCapacity(simulation,new ItemStack(Items.SMITHING_TABLE))<1)
            return new Result(false,"INVENTORY_FULL");
        remove(inv,"iron",2);remove(inv,"planks",4);
        addOne(inv,Items.SMITHING_TABLE);
        axo.recordWorkshopCraft("CRAFTED smithing_table at crafting_table="+table.toShortString()+" consumed=2 iron,4 planks");
        return new Result(true,"CRAFTED smithing table at "+table.toShortString());
    }
    private static int countItem(SimpleContainer inv,net.minecraft.world.item.Item item) {
        int n=0;
        for(int i=0;i<inv.getContainerSize();i++) if(inv.getItem(i).is(item)) n+=inv.getItem(i).getCount();
        return n;
    }
    public static Result place(WaifuAxoluthuEntity axo) {
        if(!(axo.level() instanceof ServerLevel level)) return new Result(false,"SERVER_ONLY");
        if(!axo.getSelectedDesire().equals("IMPROVE_WORKSHOP")) return new Result(false,"NO_WORKSHOP_PLAN");
        BlockPos home=axo.getHomePosition();
        if(home==null) return new Result(false,"NO_HOME");
        if(axo.blockPosition().distSqr(home)>144) return new Result(false,"TOO_FAR_FROM_HOME");
        if(!hasSmithingTableItem(axo)) return new Result(false,"MISSING_SMITHING_TABLE_ITEM");
        for(BlockPos pos:BlockPos.betweenClosed(home.offset(-8,-3,-8),home.offset(8,3,8)))
            if(level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4) && level.getBlockState(pos).is(Blocks.SMITHING_TABLE))
                return new Result(false,"EXISTING_SMITHING_TABLE_NEAR_HOME at "+pos.toShortString());
        BlockPos target=findSafePlacement(axo,6);
        if(target==null) return new Result(false,"NO_SAFE_SMITHING_TABLE_POSITION");
        Result approach=approachPlacement(axo,target,"smithing_table");
        if(!approach.success()) return approach;
        if(!level.setBlock(target,Blocks.SMITHING_TABLE.defaultBlockState(),3))
            return new Result(false,"SMITHING_TABLE_PLACE_REJECTED");
        removeItem(axo.getAcquiredMaterials(),Items.SMITHING_TABLE);
        axo.recordWorkshopPlacement(target);
        return new Result(true,"PLACED smithing table at "+target.toShortString());
    }
}
