package com.devaxo.templeoftheaxolotl.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/** Lightweight cyan missing-block blueprint; no block-model rendering. */
public final class WaifuAxoluthuCelestialProjection {
    private WaifuAxoluthuCelestialProjection() {}
    private static final int MAX_VISIBLE_BLOCKS = 512;
    private static final double MAX_DISTANCE_SQR = 48.0 * 48.0;
    public enum Detail { EXTERIOR, FULL, LAYER }
    private static Detail detail = Detail.EXTERIOR;
    public static Detail detail() { return detail; }
    public static void cycleDetail() {
        detail = switch (detail) {
            case EXTERIOR -> Detail.FULL;
            case FULL -> Detail.LAYER;
            case LAYER -> Detail.EXTERIOR;
        };
    }
    private record Entry(BlockPos pos, BlockState state) {}
    private record Bounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {}
    private static final WeakHashMap<CompoundTag, Bounds> BOUNDS = new WeakHashMap<>();
    private static Bounds bounds(CompoundTag manifest, List<Entry> entries) {
        Bounds cached = BOUNDS.get(manifest);
        if (cached != null) return cached;
        int minX=Integer.MAX_VALUE,minY=Integer.MAX_VALUE,minZ=Integer.MAX_VALUE;
        int maxX=Integer.MIN_VALUE,maxY=Integer.MIN_VALUE,maxZ=Integer.MIN_VALUE;
        for (Entry e : entries) {
            BlockPos p=e.pos();
            minX=Math.min(minX,p.getX()); maxX=Math.max(maxX,p.getX());
            minY=Math.min(minY,p.getY()); maxY=Math.max(maxY,p.getY());
            minZ=Math.min(minZ,p.getZ()); maxZ=Math.max(maxZ,p.getZ());
        }
        Bounds b = new Bounds(minX,maxX,minY,maxY,minZ,maxZ);
        BOUNDS.put(manifest,b);
        return b;
    }
    private static final WeakHashMap<CompoundTag, List<Entry>> CACHE = new WeakHashMap<>();

    /** Decode each immutable client-side manifest only once, rather than per frame. */
    private static List<Entry> entries(CompoundTag manifest) {
        List<Entry> cached = CACHE.get(manifest);
        if (cached != null) return cached;
        List<Entry> result = new ArrayList<>();
        ListTag blocks = manifest.getList("blocks", 10);
        int turns = manifest.getInt("quarterTurns");
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag entry = blocks.getCompound(i);
            if (!entry.contains("pos") || !entry.contains("block", 8)) continue;
            BlockState state = com.devaxo.templeoftheaxolotl.entity.WaifuAxoluthuPalettePlacement.state(entry, turns);
            if (state != null) result.add(new Entry(BlockPos.of(entry.getLong("pos")), state));
        }
        CACHE.put(manifest, result);
        return result;
    }

    public static void clearCache() { CACHE.clear(); BOUNDS.clear(); }

    public static void render(CompoundTag manifest, Level level, PoseStack stack,
                              MultiBufferSource buffers, Vec3 cameraPosition,
                              float partialTick, long gameTime) {
        if (manifest == null || !manifest.contains("blocks", 9)) return;
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        List<Entry> all = entries(manifest);
        if (all.isEmpty()) return;
        Bounds b = bounds(manifest, all);
        // Precomputed topmost occupied Y per X/Z column: reveals the roof
        // without drawing all the hidden furniture, floor and wall interiors.
        Map<Long,Integer> roof = new HashMap<>();
        for (Entry e : all) {
            BlockPos p=e.pos();
            long key = (((long)p.getX()) << 32) ^ (p.getZ() & 0xffffffffL);
            roof.merge(key, p.getY(), Math::max);
        }
        int rendered = 0;
        int layerY = b.minY() + 1;
        for (Entry entry : all) {
            if (rendered >= MAX_VISIBLE_BLOCKS) break;
            BlockPos pos = entry.pos();
            if (detail == Detail.EXTERIOR) {
                long key = (((long)pos.getX()) << 32) ^ (pos.getZ() & 0xffffffffL);
                boolean wall = pos.getX() == b.minX() || pos.getX() == b.maxX()
                        || pos.getZ() == b.minZ() || pos.getZ() == b.maxZ();
                boolean roofTop = pos.getY() == roof.getOrDefault(key, Integer.MIN_VALUE);
                if (!wall && !roofTop) continue;
            } else if (detail == Detail.LAYER && pos.getY() != layerY) continue;
            double x = pos.getX() - cameraPosition.x;
            double y = pos.getY() - cameraPosition.y;
            double z = pos.getZ() - cameraPosition.z;
            if (x*x + y*y + z*z > MAX_DISTANCE_SQR) continue;
            if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) continue;
            if (level.getBlockState(pos).equals(entry.state())) continue;
            LevelRenderer.renderLineBox(stack, lines,
                x + .015, y + .015, z + .015,
                x + .985, y + .985, z + .985,
                .15f, .85f, 1.0f, .80f);
            rendered++;
        }
        // Gold footprint: only four long edges, rather than a wire cube per floor block.
        double fy = b.minY() - cameraPosition.y + .025;
        LevelRenderer.renderLineBox(stack, lines,
            b.minX()-cameraPosition.x, fy, b.minZ()-cameraPosition.z,
            b.maxX()+1-cameraPosition.x, fy+.025, b.maxZ()+1-cameraPosition.z,
            1.0f, .70f, .12f, .95f);
        // The entrance marker is independent of missing-block outlines.
        if (manifest.contains("entrance", 4)) {
            BlockPos entrance = BlockPos.of(manifest.getLong("entrance"));
            double x = entrance.getX() - cameraPosition.x;
            double y = entrance.getY() - cameraPosition.y;
            double z = entrance.getZ() - cameraPosition.z;
            if (x*x + y*y + z*z <= MAX_DISTANCE_SQR) {
                LevelRenderer.renderLineBox(stack, lines,
                    x - .20, y + .02, z - .20,
                    x + 1.20, y + 2.55, z + 1.20,
                    1.0f, .72f, .12f, 1.0f);
                // Gold arrow projects outward from the nearest footprint edge.
                double cx = entrance.getX() + .5 - cameraPosition.x;
                double cz = entrance.getZ() + .5 - cameraPosition.z;
                double dx=0, dz=0;
                int west=Math.abs(entrance.getX()-b.minX());
                int east=Math.abs(entrance.getX()-b.maxX());
                int north=Math.abs(entrance.getZ()-b.minZ());
                int south=Math.abs(entrance.getZ()-b.maxZ());
                int nearest=Math.min(Math.min(west,east),Math.min(north,south));
                if (nearest==west) dx=-1;
                else if (nearest==east) dx=1;
                else if (nearest==north) dz=-1;
                else dz=1;
                double ax=cx+dx*2.0, az=cz+dz*2.0, ay=y+1.15;
                // The arrow tip is a bright raised box, visible from above.
                LevelRenderer.renderLineBox(stack, lines,
                    ax-.32,ay-.32,az-.32, ax+.32,ay+.32,az+.32,
                    1.0f,.82f,.08f,1.0f);
                LevelRenderer.renderLineBox(stack, lines,
                    Math.min(cx,ax)-.08,ay-.08,Math.min(cz,az)-.08,
                    Math.max(cx,ax)+.08,ay+.08,Math.max(cz,az)+.08,
                    1.0f,.72f,.12f,1.0f);
            }
        }
    }
}
