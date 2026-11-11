
package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import net.minecraft.world.phys.AABB;

/** Conservative forestry: bounded single-tree harvesting with mandatory sapling replanting.
 * A heuristic cannot prove a log is original worldgen. Harvesting is restricted to
 * explicitly permitted natural-looking trees in disposable development worlds.
 */
public final class WaifuAxoluthuForestry {
    private WaifuAxoluthuForestry() {}
    // Failed tree targets are cooled down to prevent endless retargeting.
    // Generous limits support giant vanilla and modded trees without unbounded scans.
    // Decorative stripped wood is never a forestry target, including modded variants.
    // Check before pathfinding so rejected workshop trim cannot be selected repeatedly.
    private static boolean isForestryLog(BlockState state) {
        if (!state.is(BlockTags.LOGS)) return false;
        String name = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return !name.startsWith("stripped_");
    }
    private static final int[] TREE_SEARCH_RADII = {20, 32, 44};
    private static final Map<java.util.UUID, Long> NO_TREE_RETRY_AT = new HashMap<>();
    private static final int MAX_TREE_LOGS = 8192;
    private static final int MAX_TREE_LEAVES = 24000;
    private static final int MAX_TREE_HORIZONTAL = 96;
    private static final int MAX_TREE_VERTICAL = 160;
    private static final Map<java.util.UUID, Map<BlockPos, Long>> FAILED_APPROACHES = new HashMap<>();
    private static final Map<java.util.UUID, BlockPos> ACTIVE_TREE = new HashMap<>();
    private static final Map<java.util.UUID, Long> ACTIVE_SINCE = new HashMap<>();
    private static final Map<java.util.UUID, BlockPos> ACTIVE_STANDING = new HashMap<>();
    private static final Map<java.util.UUID, BlockPos> ACTIVE_ACCESS = new HashMap<>();

    public static void resetNavigationMemory(WaifuAxoluthuEntity axo) {
        FAILED_APPROACHES.remove(axo.getUUID());
        NO_TREE_RETRY_AT.remove(axo.getUUID());
        ACTIVE_TREE.remove(axo.getUUID());
        ACTIVE_SINCE.remove(axo.getUUID());
        ACTIVE_STANDING.remove(axo.getUUID());
        ACTIVE_ACCESS.remove(axo.getUUID());
    }
    public static BlockPos findStandingSpot(ServerLevel level,BlockPos target,WaifuAxoluthuEntity axo) {
        return approachSpot(level,target,axo);
    }
    private static boolean safeStandingSpot(ServerLevel level, BlockPos feet, WaifuAxoluthuEntity axo) {
        if (!level.getChunkSource().hasChunk(feet.getX() >> 4, feet.getZ() >> 4)) return false;
        if (!level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), net.minecraft.core.Direction.UP)) return false;
        AABB body = new AABB(feet.getX()+0.2, feet.getY(), feet.getZ()+0.2,
                feet.getX()+0.8, feet.getY()+1.8, feet.getZ()+0.8);
        return level.noCollision(axo, body);
    }

    private static BlockPos approachSpot(ServerLevel level, BlockPos trunk, WaifuAxoluthuEntity axo) {
        BlockPos best = null;
        double distance = Double.MAX_VALUE;
        for (int dx=-2; dx<=2; dx++) for (int dz=-2; dz<=2; dz++) {
            if (dx==0 && dz==0) continue;
            for (int dy=-1; dy<=1; dy++) {
                BlockPos feet=trunk.offset(dx,dy,dz);
                if (!safeStandingSpot(level,feet,axo)) continue;
                double d=feet.distSqr(axo.blockPosition());
                if (d<distance) {distance=d;best=feet.immutable();}
            }
        }
        return best;
    }
    private static net.minecraft.world.level.block.Block originalSapling(BlockState state) {
        if(state.is(Blocks.OAK_LOG))return Blocks.OAK_SAPLING;
        if(state.is(Blocks.SPRUCE_LOG))return Blocks.SPRUCE_SAPLING;
        if(state.is(Blocks.BIRCH_LOG))return Blocks.BIRCH_SAPLING;
        if(state.is(Blocks.JUNGLE_LOG))return Blocks.JUNGLE_SAPLING;
        if(state.is(Blocks.ACACIA_LOG))return Blocks.ACACIA_SAPLING;
        if(state.is(Blocks.DARK_OAK_LOG))return Blocks.DARK_OAK_SAPLING;
        if(state.is(Blocks.MANGROVE_LOG))return Blocks.MANGROVE_PROPAGULE;
        if(state.is(Blocks.CHERRY_LOG))return Blocks.CHERRY_SAPLING;
        return null;
    }
    /** Find a reachable canopy leaf attached to this trunk, without following leaf bridges. */
    private static BlockPos reachableAccessLeaf(ServerLevel level, BlockPos trunk, WaifuAxoluthuEntity axo) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos at : BlockPos.betweenClosed(trunk.offset(-3,0,-3), trunk.offset(3,8,3))) {
            if (!level.getChunkSource().hasChunk(at.getX() >> 4, at.getZ() >> 4)) continue;
            if (!level.getBlockState(at).is(BlockTags.LEAVES)) continue;
            boolean attached = false;
            for (BlockPos nearby : BlockPos.betweenClosed(at.offset(-1,-1,-1), at.offset(1,1,1))) {
                if (level.getBlockState(nearby).is(level.getBlockState(trunk).getBlock())) {
                    attached = true;
                    break;
                }
            }
            if (!attached) continue;
            BlockPos spot = approachSpot(level, at, axo);
            if (spot == null) continue;
            double distance = spot.distSqr(axo.blockPosition());
            if (distance < bestDistance) { bestDistance = distance; best = at.immutable(); }
        }
        return best;
    }

    public static String step(WaifuAxoluthuEntity axo) {
        return step(axo, null);
    }
    /** Harvest any eligible tree, transmuting only trunk drops into the requested log item. */
    public static String step(WaifuAxoluthuEntity axo, net.minecraft.world.item.Item requestedLog) {
        if(!(axo.level() instanceof ServerLevel level))return "SERVER_ONLY";
        var bag=axo.getAcquiredMaterials();
        // The original tree is replanted during harvest. Spare saplings are supplies,
        // not mandatory planting jobs. In particular, lack of bare soil must never
        // prevent a resource request from finding another tree.
        BlockPos center=axo.blockPosition();
        java.util.UUID id=axo.getUUID();
        long now=level.getGameTime();
        Map<BlockPos,Long> blocked=FAILED_APPROACHES.computeIfAbsent(id, ignored->new HashMap<>());
        blocked.entrySet().removeIf(entry->entry.getValue()<=now);

        // Keep one target and one standing position for the entire approach.
        // Scanning every tick in BlockPos iteration order made her switch trees
        // and repeatedly recalculate different standing destinations.
        BlockPos p=ACTIVE_TREE.get(id);
        BlockPos standing=ACTIVE_STANDING.get(id);
        BlockPos accessTarget=ACTIVE_ACCESS.get(id);
        if(p!=null && (blocked.containsKey(p) ||
                !level.getChunkSource().hasChunk(p.getX()>>4,p.getZ()>>4) ||
                !isForestryLog(level.getBlockState(p)) ||
                standing==null || accessTarget==null)) {
            clearActive(id);
            axo.getNavigation().stop();
            p=null;
        }
        if(p==null) {
            // Search nearby first, then expand twice only if no eligible tree was found.
            // No forced chunk loading: unloaded regions are skipped.
            if(now < NO_TREE_RETRY_AT.getOrDefault(id, 0L)) return "NO_REACHABLE_TREE";
            double bestScore=Double.MAX_VALUE;
            BlockPos bestTree=null, bestStanding=null, bestAccess=null;
            for (int radius : TREE_SEARCH_RADII) {
                for(BlockPos candidate:BlockPos.betweenClosed(center.offset(-radius,-5,-radius),center.offset(radius,9,radius))) {
                    if(!level.getChunkSource().hasChunk(candidate.getX()>>4,candidate.getZ()>>4))continue;
                    if(blocked.containsKey(candidate) || !axo.autonomousWithinTerritory(candidate))continue;
                    BlockState state=level.getBlockState(candidate);
                    if(!isForestryLog(state) || level.getBlockState(candidate.below()).is(BlockTags.LOGS))continue;
                    if(!level.getBlockState(candidate.below()).is(BlockTags.DIRT))continue;
                    BlockPos candidateStanding=approachSpot(level,candidate,axo);
                    BlockPos candidateAccess=candidate.immutable();
                    BlockPos accessLeaf=reachableAccessLeaf(level,candidate,axo);
                    if(accessLeaf!=null) {
                        BlockPos leafStanding=approachSpot(level,accessLeaf,axo);
                        if(leafStanding!=null && (candidateStanding==null ||
                                leafStanding.distSqr(center)+4<candidateStanding.distSqr(center))) {
                            candidateStanding=leafStanding;
                            candidateAccess=accessLeaf;
                        }
                    }
                    if(candidateStanding==null)continue;
                    double score=candidateStanding.distSqr(center);
                    if(score<bestScore) {
                        bestScore=score;
                        bestTree=candidate.immutable();
                        bestStanding=candidateStanding;
                        bestAccess=candidateAccess;
                    }
                }
                if(bestTree!=null)break;
            }
            if(bestTree==null) {
                // Avoid a full three-radius rescan every AI tick, and avoid chat spam.
                NO_TREE_RETRY_AT.put(id, now+1200);
                axo.speak("forestry_no_trees", "I can't find any trees anywhere... QwQ Can I use coral instead? ...Just kidding! :3");
                return "NO_REACHABLE_TREE";
            }
            NO_TREE_RETRY_AT.remove(id);
            p=bestTree;
            standing=bestStanding;
            accessTarget=bestAccess;
            ACTIVE_TREE.put(id,p);
            ACTIVE_STANDING.put(id,standing);
            ACTIVE_ACCESS.put(id,accessTarget);
            ACTIVE_SINCE.put(id,now);
            axo.recordAcquisition("TREE_TARGET_LOCK trunk="+p.toShortString()+" stand="+standing.toShortString());
        }
        {
            if(standing.distSqr(axo.blockPosition())>3.0) {
                if(now-ACTIVE_SINCE.getOrDefault(axo.getUUID(),now)>160) {
                    blocked.put(p.immutable(),now+600);
                    clearActive(id);
                    axo.getNavigation().stop();
                    return "TREE_APPROACH_RETRY";
                }
                if(axo.getNavigation().isDone() && !axo.getNavigation().moveTo(standing.getX()+.5,standing.getY(),standing.getZ()+.5,1.3)) {
                    blocked.put(p.immutable(),now+600);
                    clearActive(id);
                    return "TREE_APPROACH_RETRY";
                }
                return "APPROACH_TREE";
            }
            axo.getNavigation().stop();
            if(!axo.actionReady(WaifuAxoluthuEntity.ACTION_FOCUSED,accessTarget,41,80))return "FOCUSED_CHOPPING";
            // Fully validate the connected tree BEFORE modifying any world blocks.
            // Match the trunk's exact log block to avoid crossing into mixed-species
            // neighboring trees. This is a conservative heuristic, not proof of origin.
            BlockState originalTrunk=level.getBlockState(p);
            Set<BlockPos> logs=new HashSet<>();
            Set<BlockPos> visited=new HashSet<>();
            ArrayDeque<BlockPos> queue=new ArrayDeque<>();queue.add(p.immutable());
            boolean incomplete=false;
            while(!queue.isEmpty()) {
                BlockPos at=queue.removeFirst();
                if(!visited.add(at))continue;
                if(!level.getChunkSource().hasChunk(at.getX()>>4,at.getZ()>>4)) {incomplete=true;break;}
                if(!isForestryLog(level.getBlockState(at)) || !level.getBlockState(at).is(originalTrunk.getBlock()))continue;
                if(Math.abs(at.getX()-p.getX())>MAX_TREE_HORIZONTAL ||
                   Math.abs(at.getZ()-p.getZ())>MAX_TREE_HORIZONTAL ||
                   Math.abs(at.getY()-p.getY())>MAX_TREE_VERTICAL) {incomplete=true;break;}
                logs.add(at.immutable());
                if(logs.size()>MAX_TREE_LOGS) {incomplete=true;break;}
                for(BlockPos n:BlockPos.betweenClosed(at.offset(-1,-1,-1),at.offset(1,1,1))) {
                    if(!visited.contains(n))queue.add(n.immutable());
                }
            }
            if(incomplete) {
                blocked.put(p.immutable(),now+1200);
                axo.recordAcquisition("TREE_SCAN_INCOMPLETE_OR_TOO_LARGE trunk="+p.toShortString());
                clearActive(id);
                return "TREE_SCAN_RETRY";
            }
            if(logs.size()<3) { blocked.put(p.immutable(),now+600); clearActive(id); return "TREE_SCAN_RETRY"; }
            // Leaves are collected near the validated logs; don't traverse a
            // connected canopy into neighboring trees.
            Set<BlockPos> leaves=new HashSet<>();
            for(BlockPos log:logs) {
                for(BlockPos nearbyLeaf:BlockPos.betweenClosed(log.offset(-2,-1,-2),log.offset(2,2,2))) {
                    if(!level.getChunkSource().hasChunk(nearbyLeaf.getX()>>4,nearbyLeaf.getZ()>>4))continue;
                    if(level.getBlockState(nearbyLeaf).is(BlockTags.LEAVES))leaves.add(nearbyLeaf.immutable());
                    if(leaves.size()>MAX_TREE_LEAVES) {incomplete=true;break;}
                }
                if(incomplete)break;
            }
            if(incomplete) {blocked.put(p.immutable(),now+1200);clearActive(id);return "TREE_SCAN_RETRY";}
            // For a 2x2 trunk, preserve the footprint and plant all four saplings.
            List<BlockPos> replantPositions=new ArrayList<>();
            BlockPos base=p;
            for(int dx=-1;dx<=0;dx++)for(int dz=-1;dz<=0;dz++) {
                BlockPos corner=p.offset(dx,0,dz);
                boolean square=true;
                for(int sx=0;sx<2;sx++)for(int sz=0;sz<2;sz++) {
                    BlockPos test=corner.offset(sx,0,sz);
                    if(!logs.contains(test) || !level.getBlockState(test.below()).is(BlockTags.DIRT))square=false;
                }
                if(square) {base=corner;break;}
            }
            boolean giant = true;
            for(int dx=0;dx<2;dx++)for(int dz=0;dz<2;dz++) {
                if(!logs.contains(base.offset(dx,0,dz)))giant=false;
            }
            if(giant)for(int dx=0;dx<2;dx++)for(int dz=0;dz<2;dz++)replantPositions.add(base.offset(dx,0,dz));
            else replantPositions.add(p.immutable());
            // Harvest only if the whole operation can fit in carried inventory.
            var simulation=new net.minecraft.world.SimpleContainer(bag.getContainerSize());
            for(int i=0;i<bag.getContainerSize();i++)simulation.setItem(i,bag.getItem(i).copy());
            for(BlockPos log:logs) {
                ItemStack drop=new ItemStack(requestedLog==null?level.getBlockState(log).getBlock().asItem():requestedLog);
                if(!simulation.addItem(drop).isEmpty())return "INVENTORY_FULL";
            }
            // Logs take priority. Leaf drops are optional: do not deadlock a
            // giant-tree harvest just because the canopy produces many sticks.
            for(BlockPos leafPos:leaves) {
                for(ItemStack drop:net.minecraft.world.level.block.Block.getDrops(level.getBlockState(leafPos),level,leafPos,null))
                    simulation.addItem(drop.copy());
            }
            for(BlockPos log:logs)level.removeBlock(log,false);
            for(BlockPos leafPos:leaves)level.removeBlock(leafPos,false);
            for(int i=0;i<bag.getContainerSize();i++)bag.setItem(i,simulation.getItem(i));
            // Replant the original species at the original trunk, not the transmuted species.
            net.minecraft.world.level.block.Block saplingBlock=originalSapling(originalTrunk);
            if(saplingBlock!=null) {
                int replanted=0;
                for(BlockPos seed:replantPositions) {
                    if(level.getBlockState(seed).isAir() && saplingBlock.defaultBlockState().canSurvive(level,seed)) {
                        level.setBlock(seed,saplingBlock.defaultBlockState(),3);
                        replanted++;
                    }
                }
                axo.recordAcquisition("REPLANTED_ORIGINAL_TREE "+p.toShortString()+" saplings="+replanted);
            }
            clearActive(id);
            axo.finishAction();
            axo.recordAcquisition("HARVESTED_TREE access="+(accessTarget.equals(p)?"TRUNK":"LEAF")+" logs="+logs.size()+" leaves="+leaves.size()+" output="+(requestedLog==null?"original":requestedLog.toString())+" REPLANT_ATTEMPTED");
            return "HARVESTED_TREE";
        }

    }
    private static void clearActive(java.util.UUID id) {
        ACTIVE_TREE.remove(id);
        ACTIVE_SINCE.remove(id);
        ACTIVE_STANDING.remove(id);
        ACTIVE_ACCESS.remove(id);
    }
}

