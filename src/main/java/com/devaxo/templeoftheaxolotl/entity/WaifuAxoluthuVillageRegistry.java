package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import java.util.Locale;

/**
 * Persistent, per-Axoluthu building registry. Read-only planning; only explicit
 * builder completion hooks may register a completed building.
 */
public final class WaifuAxoluthuVillageRegistry {
    private static final String KEY = "phase35VillageBuildings";
    private WaifuAxoluthuVillageRegistry() {}

    public static void registerCompleted(WaifuAxoluthuEntity axo, String type,
                                         BlockPos origin, int width, int height, int depth) {
        if (origin == null || !(axo.level() instanceof ServerLevel level)) return;
        if (width <= 0 || height <= 0 || depth <= 0) return;
        String normalized = type.toLowerCase(Locale.ROOT);
        CompoundTag learning = axo.getArchitecturalLearning();
        ListTag entries = learning.getList(KEY, Tag.TAG_COMPOUND);
        String dimension = level.dimension().location().toString();
        String key = normalized + ":" + dimension + ":" + origin.asLong();
        for (int i = 0; i < entries.size(); i++) {
            if (key.equals(entries.getCompound(i).getString("id"))) {
                CompoundTag existing = entries.getCompound(i);
                existing.putString("status", "COMPLETE");
                existing.putLong("completedAt", level.getGameTime());
                existing.putInt("width", width);
                existing.putInt("height", height);
                existing.putInt("depth", depth);
                learning.put(KEY, entries);
                return;
            }
        }
        CompoundTag entry = new CompoundTag();
        entry.putString("id", key);
        entry.putString("type", normalized);
        entry.putString("dimension", dimension);
        entry.putLong("origin", origin.asLong());
        entry.putInt("width", width);
        entry.putInt("height", height);
        entry.putInt("depth", depth);
        entry.putLong("completedAt", level.getGameTime());
        entry.putString("status", "COMPLETE");
        entries.add(entry);
        learning.put(KEY, entries);
    }

    /** Remember a proposed plot without granting construction permission or reserving land.
     * The proposal is historical information; only approved/active plots block planning.
     */
    public static void registerPlanned(WaifuAxoluthuEntity axo, String type,
                                       BlockPos origin, int width, int height, int depth) {
        if (origin == null || !(axo.level() instanceof ServerLevel level)) return;
        if (width <= 0 || height <= 0 || depth <= 0) return;
        String normalized = type.toLowerCase(Locale.ROOT);
        String dimension = level.dimension().location().toString();
        String id = normalized + ":" + dimension + ":" + origin.asLong();
        CompoundTag learning = axo.getArchitecturalLearning();
        ListTag entries = learning.getList(KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            if (id.equals(entries.getCompound(i).getString("id"))) return;
        }
        CompoundTag entry = new CompoundTag();
        entry.putString("id", id);
        entry.putString("type", normalized);
        entry.putString("dimension", dimension);
        entry.putLong("origin", origin.asLong());
        entry.putInt("width", width);
        entry.putInt("height", height);
        entry.putInt("depth", depth);
        entry.putString("status", "PLANNED");
        entry.putLong("plannedAt", level.getGameTime());
        entries.add(entry);
        learning.put(KEY, entries);
    }

    /** Update an existing plot through an explicit builder lifecycle event.
     * No implicit authorization is granted by this ledger operation.
     */
    public static void markActive(WaifuAxoluthuEntity axo, String type,
                                  BlockPos origin, int width, int height, int depth,
                                  String phase, int placed, int total) {
        registerPlanned(axo, type, origin, width, height, depth);
        if (!(axo.level() instanceof ServerLevel level)) return;
        CompoundTag memory = axo.getArchitecturalLearning();
        ListTag entries = memory.getList(KEY, Tag.TAG_COMPOUND);
        String id = type.toLowerCase(Locale.ROOT) + ":" + level.dimension().location() + ":" + origin.asLong();
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag e = entries.getCompound(i);
            if (!id.equals(e.getString("id"))) continue;
            String current = e.getString("status");
            if ("COMPLETE".equals(current) || "ABANDONED".equals(current)) return;
            e.putString("status", "ACTIVE");
            e.putString("phase", phase);
            e.putInt("placed", Math.max(0, placed));
            e.putInt("total", Math.max(0, total));
            e.putLong("lastProgressAt", level.getGameTime());
            memory.put(KEY, entries);
            return;
        }
    }

    /** Explicit rejection or cancellation; never inferred from unloaded chunks. */
    public static void markAbandoned(WaifuAxoluthuEntity axo, String type, BlockPos origin,
                                     String reason) {
        if (origin == null || !(axo.level() instanceof ServerLevel level)) return;
        CompoundTag memory = axo.getArchitecturalLearning();
        ListTag entries = memory.getList(KEY, Tag.TAG_COMPOUND);
        String id = type.toLowerCase(Locale.ROOT) + ":" + level.dimension().location() + ":" + origin.asLong();
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag e = entries.getCompound(i);
            if (!id.equals(e.getString("id")) || "COMPLETE".equals(e.getString("status"))) continue;
            e.putString("status", "ABANDONED");
            e.putString("attention", reason);
            e.putLong("abandonedAt", level.getGameTime());
            memory.put(KEY, entries);
            return;
        }
    }

    /** A deliberately requested, read-only workshop inspection.
     * All footprint chunks must already be loaded. No chunks are force-loaded.
     * Only workshops with a known 58-block shell are currently supported.
     */
    public static String inspectWorkshop(WaifuAxoluthuEntity axo, BlockPos origin) {
        if (origin == null || !(axo.level() instanceof ServerLevel level)) return "UNAVAILABLE";
        CompoundTag memory = axo.getArchitecturalLearning();
        ListTag entries = memory.getList(KEY, Tag.TAG_COMPOUND);
        String id = "workshop:" + level.dimension().location() + ":" + origin.asLong();
        CompoundTag record = null;
        for (int i = 0; i < entries.size(); i++)
            if (id.equals(entries.getCompound(i).getString("id"))) {
                record = entries.getCompound(i); break;
            }
        if (record == null || !"COMPLETE".equals(record.getString("status")))
            return "NOT_A_COMPLETED_WORKSHOP";
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) {
            BlockPos p = origin.offset(x, 0, z);
            if (!level.getChunkSource().hasChunk(p.getX() >> 4, p.getZ() >> 4))
                return "DEFERRED_UNLOADED";
        }
        int missing = 0;
        for (int index = 0; index < 58; index++) {
            BlockPos p;
            if (index < 25) p = origin.offset(index % 5, 0, index / 5);
            else if (index < 33) {
                int corner = (index - 25) / 2;
                p = origin.offset((corner & 1) == 0 ? 0 : 4,
                                  1 + (index - 25) % 2,
                                  (corner & 2) == 0 ? 0 : 4);
            } else {
                int roof = index - 33;
                p = origin.offset(roof % 5, 3, roof / 5);
            }
            if (!level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.OAK_PLANKS))
                missing++;
        }
        record.putInt("missingShellBlocks", missing);
        record.putLong("lastInspectedAt", level.getGameTime());
        record.putString("condition", missing == 0 ? "INTACT" : missing == 58 ? "SHELL_MISSING" : "DAMAGED");
        memory.put(KEY, entries);
        return record.getString("condition");
    }

    // Final owner-approved snapshot uses actual world positions, including rotated shells.
    public static boolean finalizeFurnished(WaifuAxoluthuEntity axo, ServerLevel level,
                                            CompoundTag request, String type, BlockPos origin, int width) {
        ListTag shell = request.getCompound("blueprintPreview").getList("blocks", Tag.TAG_COMPOUND);
        if (shell.isEmpty()) return false;
        ListTag snapshot = new ListTag();
        ListTag fixtures = request.getList("trial3HandedWorkstations", Tag.TAG_COMPOUND);
        int minX=origin.getX(), minY=origin.getY(), minZ=origin.getZ();
        int maxX=minX,maxY=minY,maxZ=minZ;
        for(int i=0;i<shell.size();i++) {
            BlockPos p=BlockPos.of(shell.getCompound(i).getLong("pos"));
            minX=Math.min(minX,p.getX()); minY=Math.min(minY,p.getY()); minZ=Math.min(minZ,p.getZ());
            maxX=Math.max(maxX,p.getX()); maxY=Math.max(maxY,p.getY()); maxZ=Math.max(maxZ,p.getZ());
        }
        if((long)(maxX-minX+1)*(maxY-minY+1)*(maxZ-minZ+1)>65536)return false;
        java.util.Set<Long> stationPositions=new java.util.HashSet<>();
        for(int i=0;i<fixtures.size();i++) stationPositions.add(fixtures.getCompound(i).getLong("placedPos"));
        for(BlockPos p:BlockPos.betweenClosed(new BlockPos(minX,minY,minZ),new BlockPos(maxX,maxY,maxZ))) {
            if(!level.getChunkSource().hasChunk(p.getX()>>4,p.getZ()>>4))return false;
            CompoundTag block=new CompoundTag(); block.putLong("pos",p.asLong());
            block.put("fullState",net.minecraft.nbt.NbtUtils.writeBlockState(level.getBlockState(p)));
            block.putBoolean("fixture",stationPositions.contains(p.asLong())); snapshot.add(block);
        }
        if(snapshot.isEmpty()) return false;
        registerCompleted(axo,type,origin,width+2,6,width+4);
        CompoundTag memory=axo.getArchitecturalLearning();
        ListTag plots=memory.getList(KEY,Tag.TAG_COMPOUND);
        String id=type.toLowerCase(Locale.ROOT)+":"+level.dimension().location()+":"+origin.asLong();
        for(int j=0;j<plots.size();j++) {
            CompoundTag plot=plots.getCompound(j);
            if(!id.equals(plot.getString("id"))) continue;
            plot.put("finalBlueprint",snapshot);
            plot.putLong("plotMin",new BlockPos(minX,minY,minZ).asLong());
            plot.putLong("plotMax",new BlockPos(maxX,maxY,maxZ).asLong());
            plot.putString("condition","INTACT");
            plot.putLong("baselineAt",level.getGameTime());
            ListTag learned=new ListTag();
            for(int k=0;k<fixtures.size();k++) {
                CompoundTag item=fixtures.getCompound(k).copy();
                BlockPos pos=BlockPos.of(item.getLong("placedPos"));
                int x=pos.getX()-origin.getX(),z=pos.getZ()-origin.getZ();
                for(int r=0;r<Math.floorMod(request.getInt("quarterTurns"),4);r++){int next=z;z=-x;x=next;}
                item.putInt("dx",x); item.putInt("dy",pos.getY()-origin.getY()); item.putInt("dz",z);
                item.putString("state",level.getBlockState(pos).toString());
                item.put("fullState",net.minecraft.nbt.NbtUtils.writeBlockState(level.getBlockState(pos).rotate(level, pos, net.minecraft.world.level.block.Rotation.values()[Math.floorMod(-request.getInt("quarterTurns"),4)])));
                learned.add(item);
            }
            plot.put("workstations",learned.copy());
            memory.put("approvedFurnishingLayout_"+type,learned);
            memory.put(KEY,plots);
            return true;
        }
        return false;
    }

    public static BlockPos rememberedStation(WaifuAxoluthuEntity axo, ServerLevel level, net.minecraft.world.level.block.Block block) {
        ListTag plots=axo.getArchitecturalLearning().getList(KEY,Tag.TAG_COMPOUND);
        BlockPos best=null; double distance=Double.MAX_VALUE;
        for(int i=0;i<plots.size();i++) {
            CompoundTag plot=plots.getCompound(i);
            if(!level.dimension().location().toString().equals(plot.getString("dimension")))continue;
            ListTag stations=plot.getList("workstations",Tag.TAG_COMPOUND);
            for(int j=0;j<stations.size();j++) {
                BlockPos p=BlockPos.of(stations.getCompound(j).getLong("placedPos"));
                if(!level.getChunkSource().hasChunk(p.getX()>>4,p.getZ()>>4)||!level.getBlockState(p).is(block))continue;
                double d=p.distSqr(axo.blockPosition()); if(d<distance){distance=d;best=p;}
            }
        }
        return best;
    }

    static ListTag plots(WaifuAxoluthuEntity axo) { return axo.getArchitecturalLearning().getList(KEY,Tag.TAG_COMPOUND); }
    static BlockPos min(CompoundTag p) { return BlockPos.of(p.contains("plotMin") ? p.getLong("plotMin") : p.getLong("origin")); }
    static BlockPos max(CompoundTag p) { return p.contains("plotMax") ? BlockPos.of(p.getLong("plotMax")) : min(p).offset(Math.max(0,p.getInt("width")-1),Math.max(0,p.getInt("height")-1),Math.max(0,p.getInt("depth")-1)); }
    public static boolean withinPreview(CompoundTag request, BlockPos pos) {
        ListTag shell=request.getCompound("blueprintPreview").getList("blocks",Tag.TAG_COMPOUND);
        if(shell.isEmpty())return false;
        BlockPos o=BlockPos.of(request.getLong("target"));
        int x=o.getX(),y=o.getY(),z=o.getZ(),X=x,Y=y,Z=z;
        for(int i=0;i<shell.size();i++){BlockPos p=BlockPos.of(shell.getCompound(i).getLong("pos")); x=Math.min(x,p.getX());y=Math.min(y,p.getY());z=Math.min(z,p.getZ());X=Math.max(X,p.getX());Y=Math.max(Y,p.getY());Z=Math.max(Z,p.getZ());}
        return pos.getX()>=x&&pos.getX()<=X&&pos.getY()>=y&&pos.getY()<=Y&&pos.getZ()>=z&&pos.getZ()<=Z;
    }
    public static boolean requestAttention(WaifuAxoluthuEntity axo,int number) {
        ListTag plots=axo.getArchitecturalLearning().getList(KEY,Tag.TAG_COMPOUND);
        if(number<1||number>plots.size())return false;
        CompoundTag plot=plots.getCompound(number-1);
        if(!plot.contains("finalBlueprint",Tag.TAG_LIST))return false;
        plot.putString("condition","NEEDS_ATTENTION"); plot.putInt("repairCursor",0); plot.putBoolean("repairConflict",false); return true;
    }

    /** Capture a baseline only for a completed structure and only when all its
     * footprint chunks are already loaded. Never force-load or modify blocks.
     * A baseline is evidence of the completed build, not a player-land claim.
     */
    public static String captureCompletedBaseline(WaifuAxoluthuEntity axo,
                                                   String type, BlockPos origin) {
        if (origin == null || !(axo.level() instanceof ServerLevel level)) return "UNAVAILABLE";
        CompoundTag memory = axo.getArchitecturalLearning();
        ListTag entries = memory.getList(KEY, Tag.TAG_COMPOUND);
        String id = type.toLowerCase(Locale.ROOT) + ":" + level.dimension().location() + ":" + origin.asLong();
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag e = entries.getCompound(i);
            if (!id.equals(e.getString("id")) || !"COMPLETE".equals(e.getString("status"))) continue;
            int w=e.getInt("width"), h=e.getInt("height"), d=e.getInt("depth");
            if(w<=0||h<=0||d<=0||(long)w*h*d>32768) return "INVALID_FOOTPRINT";
            for(int x=0;x<w;x++)for(int z=0;z<d;z++) {
                BlockPos p=origin.offset(x,0,z);
                if(!level.getChunkSource().hasChunk(p.getX()>>4,p.getZ()>>4))
                    return "DEFERRED_UNLOADED";
            }
            ListTag baseline=new ListTag();
            for(int y=0;y<h;y++)for(int z=0;z<d;z++)for(int x=0;x<w;x++) {
                BlockPos p=origin.offset(x,y,z);
                CompoundTag b=new CompoundTag();
                b.putInt("index",(y*d+z)*w+x);
                b.putString("block",net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .getKey(level.getBlockState(p).getBlock()).toString());
                baseline.add(b);
            }
            e.put("baseline",baseline);
            e.putLong("baselineAt",level.getGameTime());
            memory.put(KEY,entries);
            return "BASELINE_SAVED";
        }
        return "NOT_REGISTERED_COMPLETE";
    }

    /** Read-only, chunk-safe inspection of any completed structure with a
     * captured baseline. Changed blocks are reported, never overwritten.
     * An older save without a baseline is NOT presumed intact.
     */
    public static String inspectRecordedPlot(WaifuAxoluthuEntity axo,
                                             String type, BlockPos origin) {
        if(origin==null||!(axo.level() instanceof ServerLevel level))return "UNAVAILABLE";
        CompoundTag memory=axo.getArchitecturalLearning();
        ListTag entries=memory.getList(KEY,Tag.TAG_COMPOUND);
        String id=type.toLowerCase(Locale.ROOT)+":"+level.dimension().location()+":"+origin.asLong();
        for(int i=0;i<entries.size();i++) {
            CompoundTag e=entries.getCompound(i);
            if(!id.equals(e.getString("id"))||!"COMPLETE".equals(e.getString("status")))continue;
            if(!e.contains("baseline",Tag.TAG_LIST))return "BASELINE_REQUIRED";
            int w=e.getInt("width"),h=e.getInt("height"),d=e.getInt("depth");
            if(w<=0||h<=0||d<=0||(long)w*h*d>32768)return "INVALID_FOOTPRINT";
            for(int x=0;x<w;x++)for(int z=0;z<d;z++) {
                BlockPos p=origin.offset(x,0,z);
                if(!level.getChunkSource().hasChunk(p.getX()>>4,p.getZ()>>4))
                    return "DEFERRED_UNLOADED";
            }
            ListTag baseline=e.getList("baseline",Tag.TAG_COMPOUND);
            int changed=0, solidExpected=0, missing=0;
            for(int n=0;n<baseline.size();n++) {
                CompoundTag b=baseline.getCompound(n);
                int index=b.getInt("index");
                if(index<0||index>=(long)w*h*d)continue;
                int x=index%w,z=(index/w)%d,y=index/(w*d);
                BlockPos p=origin.offset(x,y,z);
                String actual=net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .getKey(level.getBlockState(p).getBlock()).toString();
                String expected=b.getString("block");
                if(!"minecraft:air".equals(expected)) {
                    solidExpected++;
                    if(level.getBlockState(p).isAir())missing++;
                }
                if(!actual.equals(expected))changed++;
            }
            String condition=changed==0?"INTACT":
                solidExpected>0&&missing==solidExpected?"SHELL_MISSING":"DAMAGED";
            e.putInt("changedBlocks",changed);
            e.putInt("missingBlocks",missing);
            e.putString("condition",condition);
            e.putLong("lastInspectedAt",level.getGameTime());
            memory.put(KEY,entries);
            return condition;
        }
        return "NOT_REGISTERED_COMPLETE";
    }

    /** Render a permanent, individual plot ledger from Axoluthu's saved architectural memory. */
    public static String ledgerView(WaifuAxoluthuEntity axo) {
        ListTag entries = axo.getArchitecturalLearning().getList(KEY, Tag.TAG_COMPOUND);
        StringBuilder out = new StringBuilder("CONSTRUCTION LEDGER\n\n");
        out.append("Registered plots: ").append(entries.size()).append("\n");
        int complete = 0, planned = 0, active = 0, abandoned = 0;
        for (int i = 0; i < entries.size(); i++) {
            String status = entries.getCompound(i).getString("status");
            if (status.isEmpty() || "COMPLETE".equals(status)) complete++;
            else if ("PLANNED".equals(status)) planned++;
            else if ("ACTIVE".equals(status)) active++;
            else if ("ABANDONED".equals(status)) abandoned++;
        }
        out.append("Completed: ").append(complete).append(" | Planned: ").append(planned)
           .append(" | Active: ").append(active).append(" | Abandoned: ").append(abandoned).append("\n\n");
        if (entries.isEmpty()) return out.append("No completed plots recorded yet.\n").toString();
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag e = entries.getCompound(i);
            BlockPos o = BlockPos.of(e.getLong("origin"));
            out.append("PLOT #").append(String.format(Locale.ROOT, "%03d", i + 1)).append(" - ")
               .append(e.getString("type").replace('_', ' ').toUpperCase(Locale.ROOT)).append("\n")
               .append("Site: ").append(o.toShortString()).append("\n")
               .append("Dimension: ").append(e.getString("dimension")).append("\n")
               .append("Bounds: ").append(e.getInt("width")).append(" x ")
               .append(e.getInt("height")).append(" x ").append(e.getInt("depth")).append("\n")
               .append("Occupancy: ").append(e.getString("type")).append("\n")
               .append("Status: ").append(e.getString("status").isEmpty()
                   ? "COMPLETE" : e.getString("status")).append("\n");
            if (e.contains("phase")) out.append("Phase: ").append(e.getString("phase")).append("\n");
            if (e.contains("total")) out.append("Progress: ").append(e.getInt("placed")).append(" / ").append(e.getInt("total")).append("\n");
            if (e.contains("condition")) out.append("Condition: ").append(e.getString("condition")).append("\n");
            if(e.contains("missingFixtures")) out.append("Changed workstations: ").append(e.getInt("missingFixtures")).append("\n");
            ListTag stations=e.getList("workstations",Tag.TAG_COMPOUND);
            for(int j=0;j<stations.size();j++){CompoundTag station=stations.getCompound(j);out.append(station.getString("block").replace("minecraft:","")).append(": ").append(BlockPos.of(station.getLong("placedPos")).toShortString()).append("\n");}
            if (e.contains("attention")) out.append("Note: ").append(e.getString("attention")).append("\n");
            out.append("\n");
        }
        return out.toString();
    }

    public static int count(WaifuAxoluthuEntity axo, String type) {
        ListTag entries = axo.getArchitecturalLearning().getList(KEY, Tag.TAG_COMPOUND);
        int count = 0;
        for (int i = 0; i < entries.size(); i++)
            if (type.equalsIgnoreCase(entries.getCompound(i).getString("type"))
                    && (entries.getCompound(i).getString("status").isEmpty()
                    || "COMPLETE".equals(entries.getCompound(i).getString("status")))) count++;
        return count;
    }

    /** Pure recommendation; DOES NOT start a build or grant approval. */
    public static String recommendNext(WaifuAxoluthuEntity axo) {
        if (!axo.isStage4WorkshopComplete() || count(axo, "workshop") == 0)
            return "workshop";
        if (!axo.isSettlementStorageComplete() || count(axo, "storage") == 0)
            return "storage";
        return "house";
    }

    public static boolean overlaps(WaifuAxoluthuEntity axo, String dimension,
                                   BlockPos min, BlockPos max, int clearance) {
        ListTag entries = axo.getArchitecturalLearning().getList(KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag e = entries.getCompound(i);
            if (!dimension.equals(e.getString("dimension"))) continue;
            // A proposal is not a reserved site and must not prevent future planning.
            if ("PLANNED".equals(e.getString("status")) || "ABANDONED".equals(e.getString("status"))) continue;
            BlockPos o = min(e), upper=max(e);
            if (min.getX() <= upper.getX() + clearance
                    && max.getX() >= o.getX() - clearance
                    && min.getY() <= upper.getY() + clearance
                    && max.getY() >= o.getY() - clearance
                    && min.getZ() <= upper.getZ() + clearance
                    && max.getZ() >= o.getZ() - clearance) return true;
        }
        return false;
    }
}
