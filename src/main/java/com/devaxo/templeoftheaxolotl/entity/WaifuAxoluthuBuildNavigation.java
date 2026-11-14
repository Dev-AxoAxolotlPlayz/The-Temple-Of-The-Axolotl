package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import java.util.*;

/** Construction-only navigation. Does not change forestry or general navigation. */
public final class WaifuAxoluthuBuildNavigation {
    private WaifuAxoluthuBuildNavigation() {}
    private record Attempt(BlockPos target, BlockPos stand, long tick, double x, double z, int retries) {}
    private static final Map<UUID, Attempt> attempts = new HashMap<>();
    private record Recovery(BlockPos destination, long checked, double x, double z,
                            int failures, boolean returningHome, boolean usedHome, long nextRequest) {}
    private static final Map<UUID, Recovery> recoveries = new HashMap<>();

    /** Constant-time watchdog; never creates candidate paths during recovery. */
    private static String travel(WaifuAxoluthuEntity axo, ServerLevel level, BlockPos destination) {
        UUID id = axo.getUUID();
        long now = level.getGameTime();
        Recovery r = recoveries.get(id);
        if (r == null || !r.destination().equals(destination)) {
            r = new Recovery(destination, now, axo.getX(), axo.getZ(), 0, false, false, 0);
        }
        BlockPos home = axo.getHomePosition();
        if (r.returningHome()) {
            if (home == null) { recoveries.remove(id); return "ALL_ROADS_LEAD_HOME_NO_ANCHOR"; }
            if (axo.distanceToSqr(home.getX()+.5,home.getY(),home.getZ()+.5) <= 9.0) {
                // Arrival is a radius, not an exact coordinate or the bed itself.
                r = new Recovery(destination, now, axo.getX(), axo.getZ(), 0, false, true, now+40);
                axo.getNavigation().stop();
                recoveries.put(id,r);
                return "ALL_ROADS_LEAD_HOME_HOME_REACHED";
            }
            if (now - r.checked() >= 100) {
                double moved = Math.hypot(axo.getX()-r.x(), axo.getZ()-r.z());
                if (moved < 0.8 && r.failures() >= 2) {
                    recoveries.remove(id);
                    return "ALL_ROADS_LEAD_HOME_HOME_UNREACHABLE";
                }
                r = new Recovery(destination, now, axo.getX(), axo.getZ(),
                        moved < 0.8 ? r.failures()+1 : 0, true, true, now);
            }
            if (now >= r.nextRequest() && (axo.getNavigation().isDone() || now-r.checked()>=100)) {
                axo.navigateToSavedLocation(home, 1.1);
                r = new Recovery(destination,r.checked(),r.x(),r.z(),r.failures(),true,true,now+40);
            }
            recoveries.put(id,r);
            return "ALL_ROADS_LEAD_HOME_RETURNING";
        }
        if (now - r.checked() >= 100) {
            double moved = Math.hypot(axo.getX()-r.x(), axo.getZ()-r.z());
            int failures = moved < 0.8 ? r.failures()+1 : 0;
            if (failures >= 3) {
                if (r.usedHome()) {
                    recoveries.remove(id);
                    return "ALL_ROADS_LEAD_HOME_BUILD_DESTINATION_UNREACHABLE";
                }
                if (home != null && axo.distanceToSqr(home.getX()+.5,home.getY(),home.getZ()+.5)>9) {
                    r = new Recovery(destination,now,axo.getX(),axo.getZ(),0,true,true,now);
                    recoveries.put(id,r);
                    return "ALL_ROADS_LEAD_HOME_ACTIVATED";
                }
                // Already at home: a home trip cannot improve the route.
                recoveries.remove(id);
                return "ALL_ROADS_LEAD_HOME_BUILD_DESTINATION_UNREACHABLE";
            }
            r = new Recovery(destination,now,axo.getX(),axo.getZ(),failures,false,r.usedHome(),now);
        }
        if (now >= r.nextRequest() && axo.getNavigation().isDone()) {
            axo.navigateToSavedLocation(destination,1.15);
            r = new Recovery(destination,r.checked(),r.x(),r.z(),r.failures(),false,r.usedHome(),now+40);
        }
        recoveries.put(id,r);
        return "RETURNING_TO_BUILD_SITE";
    }


    private static boolean safe(ServerLevel level, BlockPos feet, WaifuAxoluthuEntity axo) {
        if (!level.getChunkSource().hasChunk(feet.getX() >> 4, feet.getZ() >> 4)) return false;
        BlockPos below=feet.below();
        if (!level.getBlockState(below).isFaceSturdy(level,below,net.minecraft.core.Direction.UP)) return false;
        return level.noCollision(axo,new AABB(feet.getX()+.2,feet.getY(),feet.getZ()+.2,
                feet.getX()+.8,feet.getY()+1.8,feet.getZ()+.8));
    }

    public static String approach(WaifuAxoluthuEntity axo, ServerLevel level, BlockPos target, double speed) {
        if (WaifuAxoluthuBuildReach.inRange(axo,target)) { attempts.remove(axo.getUUID()); recoveries.remove(axo.getUUID()); return null; }
        // From distant forestry trips, first return to the build site through
        // short loaded waypoints; remote candidate paths cannot be reached.
        if (target.distSqr(axo.blockPosition()) > 144) {
            return travel(axo,level,target);
        }
        recoveries.remove(axo.getUUID());
        UUID id=axo.getUUID();
        long now=level.getGameTime();
        Attempt prior=attempts.get(id);
        if(prior!=null && !prior.target().equals(target)) {attempts.remove(id);prior=null;}
        if(prior!=null) {
            double moved=(axo.getX()-prior.x())*(axo.getX()-prior.x())+(axo.getZ()-prior.z())*(axo.getZ()-prior.z());
            if(now-prior.tick()<50 && !axo.getNavigation().isDone())return "APPROACH_BUILD";
            if(now-prior.tick()>=50 && moved>=.5*.5) {
                attempts.put(id,new Attempt(target,prior.stand(),now,axo.getX(),axo.getZ(),prior.retries()));
                if(!axo.getNavigation().isDone())return "APPROACH_BUILD";
            }
        }
        // Rank safe spots by distance from the companion, but only accept a path
        // whose endpoint is close to the candidate. Avoid permanently locking
        // onto an inaccessible position across successive AI decisions.
        List<BlockPos> spots=new ArrayList<>();
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)for(int dy=-4;dy<=2;dy++) {
            BlockPos feet=target.offset(dx,dy,dz);
            if(!safe(level,feet,axo))continue;
            double rx=target.getX()-feet.getX(),rz=target.getZ()-feet.getZ();
            if(rx*rx+rz*rz>9)continue;
            // Structural placement may be performed from the ground below a roof.
            // Reject candidates outside the same horizontal/vertical reach rules.
            double vertical=target.getY()+.5-(feet.getY()+1.62);
            if(Math.abs(vertical)>8)continue;
            if(prior!=null && prior.stand().equals(feet))continue;
            spots.add(feet.immutable());
        }
        spots.sort(Comparator.comparingDouble(p->p.distSqr(axo.blockPosition())));
        int tested=0;
        for(BlockPos spot:spots) {
            if(++tested>100)break;
            Path path=axo.getNavigation().createPath(spot,0);
            if(path==null || !path.canReach())continue;
            if(axo.getNavigation().moveTo(path,speed)) {
                attempts.put(id,new Attempt(target,spot,now,axo.getX(),axo.getZ(),prior==null?0:prior.retries()+1));
                return "APPROACH_BUILD";
            }
        }
        // Pathfinding can report no fully reachable path for an otherwise safe
        // nearby construction stand (particularly beside a newly built wall).
        // Try a direct navigation request to safe stands before giving up.
        for (BlockPos spot : spots) {
            if (axo.getNavigation().moveTo(spot.getX()+0.5, spot.getY(), spot.getZ()+0.5, speed)) {
                attempts.put(id, new Attempt(target, spot, now, axo.getX(), axo.getZ(),
                        prior == null ? 1 : prior.retries()+1));
                return "APPROACH_BUILD_FALLBACK";
            }
        }
        attempts.remove(id);
        return "WAITING_FOR_REACHABLE_BUILD_POSITION";
    }

    /** Move out of the block being placed, rather than restarting the placement animation forever. */
    public static String clearSelf(WaifuAxoluthuEntity axo, ServerLevel level, BlockPos target) {
        UUID id=axo.getUUID();
        long now=level.getGameTime();
        Attempt prior=attempts.get(id);
        if(prior!=null && !prior.target().equals(target)) { attempts.remove(id); prior=null; }
        if(!axo.getBoundingBox().intersects(new AABB(target))) {
            attempts.remove(id);
            return null;
        }
        if(prior!=null && now-prior.tick()<40 && !axo.getNavigation().isDone())
            return "MOVING_OUT_OF_PLACEMENT_BLOCK";
        List<BlockPos> spots=new ArrayList<>();
        BlockPos center=axo.blockPosition();
        for(int dx=-3;dx<=3;dx++) for(int dz=-3;dz<=3;dz++) for(int dy=-1;dy<=1;dy++) {
            BlockPos feet=center.offset(dx,dy,dz);
            if(!safe(level,feet,axo))continue;
            if(new AABB(feet.getX()+.15,feet.getY(),feet.getZ()+.15,
                    feet.getX()+.85,feet.getY()+1.8,feet.getZ()+.85).intersects(new AABB(target)))continue;
            double hx=target.getX()+.5-(feet.getX()+.5);
            double hz=target.getZ()+.5-(feet.getZ()+.5);
            if(hx*hx+hz*hz>7.0)continue;
            if(prior!=null && feet.equals(prior.stand()))continue;
            spots.add(feet.immutable());
        }
        spots.sort(Comparator.comparingDouble(pos->pos.distSqr(center)));
        for(BlockPos spot:spots) {
            Path path=axo.getNavigation().createPath(spot,0);
            if(path!=null && path.canReach() && axo.getNavigation().moveTo(path,1.1)) {
                attempts.put(id,new Attempt(target,spot,now,axo.getX(),axo.getZ(),prior==null?0:prior.retries()+1));
                return "MOVING_OUT_OF_PLACEMENT_BLOCK";
            }
        }
        return "WAITING_FOR_SELF_CLEARANCE_PATH";
    }

    public static void completed(WaifuAxoluthuEntity axo) {attempts.remove(axo.getUUID()); recoveries.remove(axo.getUUID());}
}
