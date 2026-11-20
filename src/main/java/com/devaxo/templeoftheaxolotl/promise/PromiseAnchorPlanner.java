package com.devaxo.templeoftheaxolotl.promise;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Stateless candidate scoring and support classification for future server-owned adhesion.
 * Never loads chunks or mutates blocks. Callers MUST validate solid faces, claims, reach,
 * chunk availability, and ownership before attaching any tentacle.
 */
public final class PromiseAnchorPlanner {
    private PromiseAnchorPlanner() {}

    public record Candidate(BlockPos block, Direction face, Vec3 contact, double distanceSquared) {}

    public enum Support { NONE, CONTROLLED_DESCENT, CAUGHT, STABLE }

    public static Support support(int validAnchors) {
        if (validAnchors <= 0) return Support.NONE;
        if (validAnchors == 1) return Support.STABLE;
        if (validAnchors == 2) return Support.CAUGHT;
        return Support.STABLE;
    }

    /** Chebyshev (cubic) reach, not Euclidean spherical reach. */
    public static boolean inCube(Vec3 origin, Vec3 contact, int reach) {
        return reach >= 0 && Math.abs(contact.x - origin.x) <= reach
                && Math.abs(contact.y - origin.y) <= reach
                && Math.abs(contact.z - origin.z) <= reach;
    }

    /** Six-face candidates, nearest first; caller supplies a small bounded list of blocks. */
    public static List<Candidate> candidates(Vec3 origin, Iterable<BlockPos> blocks, int reach, int limit) {
        if (limit <= 0) return List.of();
        List<Candidate> out = new ArrayList<>();
        for (BlockPos pos : blocks) {
            for (Direction face : Direction.values()) {
                Vec3 contact = Vec3.atCenterOf(pos).add(
                        face.getStepX() * .5, face.getStepY() * .5, face.getStepZ() * .5);
                if (inCube(origin, contact, reach))
                    out.add(new Candidate(pos.immutable(), face, contact, origin.distanceToSqr(contact)));
            }
        }
        out.sort(Comparator.comparingDouble(Candidate::distanceSquared));
        return List.copyOf(out.subList(0, Math.min(limit, out.size())));
    }
}
