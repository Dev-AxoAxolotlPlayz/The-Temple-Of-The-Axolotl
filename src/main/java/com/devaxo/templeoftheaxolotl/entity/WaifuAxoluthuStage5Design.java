package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/** Stage 5: bounded, non-destructive, lesson-informed site/design exploration.
 * This does not place blocks or reserve a site; it is a proposal, not a build order.
 */
public final class WaifuAxoluthuStage5Design {
    private WaifuAxoluthuStage5Design() {}
    public record Candidate(BlockPos origin, int width, int depth, String footprint,
                             String roof, int score, int terrainChanges) {}

    public static String envision(WaifuAxoluthuEntity axo) {
        Candidate best = propose(axo);
        if (best == null) return "No safe vacant site found; no blocks changed.";
        return "Stage 5 idea: " + best.footprint() + " " + best.width() + "x" + best.depth()
                + " roof=" + best.roof() + " at " + best.origin().toShortString()
                + ". Read-only proposal; land ownership not verified.";
    }

    public static Candidate propose(WaifuAxoluthuEntity axo) {
        if (!(axo.level() instanceof ServerLevel level)) return null;
        if (axo.getBondedPlayer() == null) return null;
        // The lesson API synchronizes her per-player cross-world knowledge first.
        WaifuAxoluthuBuildingLessons.status(axo);
        ListTag lessons = axo.getArchitecturalLearning().getList("lessons", 10);
        Map<String, Integer> learned = new HashMap<>();
        for (int i = 0; i < lessons.size(); i++) {
            CompoundTag lesson = lessons.getCompound(i);
            for (String key : List.of("stairs", "slabs", "logs", "planks", "glass", "glass_panes", "lanterns"))
                learned.merge(key, lesson.getInt(key), Integer::sum);
        }
        int roofEvidence = learned.getOrDefault("stairs", 0) + learned.getOrDefault("slabs", 0);
        int windowEvidence = learned.getOrDefault("glass", 0) + learned.getOrDefault("glass_panes", 0);
        int timberEvidence = learned.getOrDefault("logs", 0) + learned.getOrDefault("planks", 0);
        BlockPos center = axo.blockPosition();
        List<Candidate> candidates = new ArrayList<>();
        // Multiple site and shape candidates; lesson evidence changes scoring, not hard requirements.
        Random random = new Random(level.getSeed() ^ axo.getUUID().getMostSignificantBits()
                ^ (level.getGameTime() / 24000L));
        for (int attempt = 0; attempt < 48; attempt++) {
            int width = 7 + 2 * random.nextInt(4);
            int depth = 7 + 2 * random.nextInt(4);
            String footprint = switch (random.nextInt(3)) {
                case 0 -> "RECTANGLE";
                case 1 -> "L_WING";
                default -> "OFFSET_WING";
            };
            String roof = roofEvidence > 0 && random.nextInt(4) != 0 ? "PITCHED_GABLE" :
                    (random.nextBoolean() ? "PITCHED_GABLE" : "HIP");
            int dx = random.nextInt(49) - 24, dz = random.nextInt(49) - 24;
            BlockPos origin = center.offset(dx, 0, dz);
            if (!level.getChunkSource().hasChunk(origin.getX() >> 4, origin.getZ() >> 4)
                    || !level.getChunkSource().hasChunk((origin.getX()+width) >> 4, (origin.getZ()+depth) >> 4)) continue;
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    origin.getX(), origin.getZ());
            origin = new BlockPos(origin.getX(), y, origin.getZ());
            int changes = 0;
            boolean valid = true;
            // Strictly read-only: never approve occupied spaces, fluids, or uneven land for now.
            for (int x = 0; x < width && valid; x++) for (int z = 0; z < depth && valid; z++) {
                BlockPos floor = origin.offset(x, -1, z);
                if (!level.getChunkSource().hasChunk(floor.getX() >> 4, floor.getZ() >> 4)) {valid=false;break;}
                if (level.getBlockState(floor).isAir() || !level.getFluidState(floor).isEmpty()) {valid=false;break;}
                for (int h = 0; h <= 7; h++) {
                    BlockPos p = origin.offset(x,h,z);
                    if (!level.getBlockState(p).isAir() || !level.getFluidState(p).isEmpty()) {valid=false;break;}
                }
            }
            if (!valid) continue;
            int score = 100 - Math.abs(dx) - Math.abs(dz) - changes * 3;
            if ("PITCHED_GABLE".equals(roof)) score += Math.min(12,roofEvidence);
            if (!"RECTANGLE".equals(footprint)) score += Math.min(8,timberEvidence / 3);
            score += Math.min(6,windowEvidence / 3);
            candidates.add(new Candidate(origin,width,depth,footprint,roof,score,changes));
        }
        if (candidates.isEmpty()) return null;
        candidates.sort(Comparator.comparingInt(Candidate::score).reversed());
        return candidates.get(0);
    }
}
