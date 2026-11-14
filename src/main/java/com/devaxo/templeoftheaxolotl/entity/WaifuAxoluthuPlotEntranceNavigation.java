package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.pathfinder.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Route plot-boundary crossings through the saved, rotated blueprint entrance. */
public final class WaifuAxoluthuPlotEntranceNavigation {
    private WaifuAxoluthuPlotEntranceNavigation() {}
    private record Crossing(BlockPos entrance, boolean leaving, int stage, long started) {}
    private static final Map<UUID,Crossing> CROSSINGS = new HashMap<>();
    private static final Map<UUID,Long> RETRY_AFTER = new HashMap<>();
    private static final Map<UUID,BlockPos> LAST_POSITION = new HashMap<>();
    private static final Map<UUID,Long> LAST_PROGRESS = new HashMap<>();
    private static void release(UUID id, long now) {
        CROSSINGS.remove(id);
        LAST_POSITION.remove(id);
        LAST_PROGRESS.remove(id);
        RETRY_AFTER.put(id,now+100);
    }

    /** Returns true while an entrance waypoint owns navigation; false when normal AI can proceed. */
    public static boolean route(WaifuAxoluthuEntity axo, ServerLevel level, BlockPos destination) {
        UUID id=axo.getUUID();
        long now=level.getGameTime();
        if (now<RETRY_AFTER.getOrDefault(id,0L)) return false;
        CompoundTag request=axo.getArchitecturalLearning().getCompound("phase35OwnerSuggestion");
        if(!request.getBoolean("pending") || !request.getBoolean("designApproved")
                || !request.getBoolean("constructionAllowed") || !request.contains("blueprintPreview",10)
                || !level.dimension().location().toString().equals(request.getString("dimension"))) {
            CROSSINGS.remove(id); return false;
        }
        CompoundTag preview=request.getCompound("blueprintPreview");
        if(!preview.contains("entrance",4)) {CROSSINGS.remove(id);return false;}
        BlockPos entrance=BlockPos.of(preview.getLong("entrance"));
        ListTag blocks=preview.getList("blocks",10);
        if(blocks.isEmpty()) {CROSSINGS.remove(id);return false;}
        int minX=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,minZ=Integer.MAX_VALUE,maxZ=Integer.MIN_VALUE;
        for(int i=0;i<blocks.size();i++) {
            BlockPos p=BlockPos.of(blocks.getCompound(i).getLong("pos"));
            minX=Math.min(minX,p.getX());maxX=Math.max(maxX,p.getX());
            minZ=Math.min(minZ,p.getZ());maxZ=Math.max(maxZ,p.getZ());
        }
        // Derive the inward direction from the actual rotated entrance and the plot's center.
        // Avoid treating a patio/eave as a wall by using the entrance's facing metadata.
        int dx=0,dz=0;
        switch(preview.getString("facing")) {
            case "north" -> dz=1;
            case "south" -> dz=-1;
            case "east" -> dx=-1;
            case "west" -> dx=1;
            default -> {CROSSINGS.remove(id);return false;}
        }
        BlockPos inside=entrance.offset(dx,0,dz);
        BlockPos outside=entrance.offset(-dx,0,-dz);
        // The doorway itself may be at Y+1 in the blueprint. Walk at the doorway's feet level.
        BlockPos current=axo.blockPosition();
        boolean atPlot=current.getX()>=minX+1 && current.getX()<=maxX-1
                && current.getZ()>=minZ+1 && current.getZ()<=maxZ-1;
        boolean targetAtPlot=destination.getX()>=minX+1 && destination.getX()<=maxX-1
                && destination.getZ()>=minZ+1 && destination.getZ()<=maxZ-1;
        Crossing crossing=CROSSINGS.get(id);
        if(crossing!=null && (!crossing.entrance().equals(entrance) || now-crossing.started()>160)) {
            release(id,now);return false;
        }
        if(crossing==null) {
            if(atPlot==targetAtPlot) return false;
            // Distant destinations must first be approached by the normal navigator.
            // Do not take ownership of a trip across the whole world.
            if(current.distSqr(entrance)>144) return false;
            crossing=new Crossing(entrance,atPlot,0,level.getGameTime());
            CROSSINGS.put(id,crossing);
            LAST_POSITION.put(id,current.immutable());
            LAST_PROGRESS.put(id,now);
        }
        BlockPos first=crossing.leaving()?inside:outside;
        BlockPos second=crossing.leaving()?outside:inside;
        BlockPos waypoint=crossing.stage()==0?first:second;
        // Give up if another AI cancelled the route or navigation is not progressing.
        BlockPos previous=LAST_POSITION.get(id);
        if(previous==null || current.distSqr(previous)>=2.0) {
            LAST_POSITION.put(id,current.immutable());
            LAST_PROGRESS.put(id,now);
        } else if(now-LAST_PROGRESS.getOrDefault(id,now)>55) {
            release(id,now);return false;
        }
        if(current.distSqr(waypoint)<=2.25) {
            if(crossing.stage()==1) {
                CROSSINGS.remove(id);LAST_POSITION.remove(id);LAST_PROGRESS.remove(id);
                return false;
            }
            crossing=new Crossing(entrance,crossing.leaving(),1,crossing.started());
            CROSSINGS.put(id,crossing);
            waypoint=second;
        }
        if(axo.getNavigation().isDone()) {
            Path path=axo.getNavigation().createPath(waypoint,0);
            if(path==null || !path.canReach() || !axo.getNavigation().moveTo(path,1.15)) {
                // Fail open: never block forestry/construction on an unreachable marker.
                release(id,now);
                return false;
            }
        }
        return true;
    }
}
