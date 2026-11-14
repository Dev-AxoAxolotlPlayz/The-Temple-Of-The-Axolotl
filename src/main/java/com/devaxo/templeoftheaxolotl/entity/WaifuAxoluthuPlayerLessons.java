package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** Player-approved world revisions plus persistent per-profile, per-building-type lesson counts. */
public final class WaifuAxoluthuPlayerLessons {
    private WaifuAxoluthuPlayerLessons() {}

    public static int accept(ServerPlayer player, ServerLevel level, CompoundTag request, boolean furnishing) {
        CompoundTag original = request.getCompound("blueprintPreview");
        ListTag planned = furnishing
                ? WaifuAxoluthuFurnishingLayouts.plan(original, BlockPos.of(request.getLong("target")), request.getInt("quarterTurns"))
                : original.getList("blocks", 10);
        Set<Long> plannedPositions = new HashSet<>();
        Map<String,Integer> lessons = new HashMap<>();
        ListTag revision = new ListTag();
        for (int i = 0; i < planned.size(); i++) {
            CompoundTag entry = planned.getCompound(i);
            BlockPos pos = BlockPos.of(entry.getLong("pos"));
            plannedPositions.add(pos.asLong());
            if (!level.hasChunkAt(pos)) continue;
            BlockState actual = level.getBlockState(pos);
            String actualId = BuiltInRegistries.BLOCK.getKey(actual.getBlock()).toString();
            String expected = entry.getString("block");
            // Wall/fence connection changes aren't lessons. Compare identity, not volatile states.
            if (!expected.equals(actualId)) {
                CompoundTag change = new CompoundTag();
                change.putLong("pos", pos.asLong());
                change.putString("before", expected);
                change.putString("after", actualId);
                revision.add(change);
                lessons.merge("replace." + expected + ".with." + actualId, 1, Integer::sum);
            }
        }
        // Also recognize blocks the player added in the approved building volume.
        BlockPos origin = BlockPos.of(request.getLong("target"));
        int width = Math.min(22, Math.max(3, original.getInt("width") + 2));
        for (int x = -2; x <= width; x++) for (int z = -2; z <= width; z++)
            for (int y = 1; y <= 8; y++) {
                BlockPos pos = origin.offset(x,y,z);
                if (plannedPositions.contains(pos.asLong()) || !level.hasChunkAt(pos)) continue;
                BlockState actual = level.getBlockState(pos);
                if (actual.isAir()) continue;
                String id = BuiltInRegistries.BLOCK.getKey(actual.getBlock()).toString();
                CompoundTag change = new CompoundTag();
                change.putLong("pos", pos.asLong());
                change.putString("before", "minecraft:air");
                change.putString("after", id);
                revision.add(change);
                lessons.merge("added." + id, 1, Integer::sum);
            }
        String stage = furnishing ? "axoApprovedFurnishingRevision" : "axoApprovedStructuralRevision";
        request.put(stage, revision);
        request.putBoolean(stage + "Accepted", true);
        saveProfile(player, WaifuAxoluthuVillageStructures.normalize(request.getString("type")),
                furnishing ? "furnishing" : "structure", lessons);
        return revision.size();
    }

    /** Apply repeatedly approved, safe material preferences to *new* previews only. */
    public static void personalizePreview(ServerPlayer player, String type, CompoundTag preview) {
        Path file = FMLPaths.GAMEDIR.get().resolve("config").resolve("temple_of_the_axolotl")
                .resolve("axoluthu_lessons").resolve(player.getUUID() + ".properties");
        if (!Files.isRegularFile(file)) return;
        Properties props = new Properties();
        try (var in = Files.newInputStream(file)) { props.load(in); }
        catch (Exception e) { return; }
        String prefix = "structure." + type + ".replace.";
        Map<String,String> preferred = new HashMap<>();
        Map<String,Integer> scores = new HashMap<>();
        for (String key : props.stringPropertyNames()) {
            if (!key.startsWith(prefix) || !key.contains(".with.")) continue;
            String pair = key.substring(prefix.length());
            int at = pair.indexOf(".with.");
            String before = pair.substring(0, at);
            String after = pair.substring(at + 6);
            // Only material-for-material substitutions; never change air, support geometry,
            // entrance blocks, light fixtures, or functional blocks by inference.
            boolean planks = before.endsWith("_planks") && after.endsWith("_planks");
            boolean stone = before.endsWith("_bricks") && after.endsWith("_bricks");
            if (!before.startsWith("minecraft:") || !after.startsWith("minecraft:")
                    || !(planks || stone) || !WaifuAxoluthuPalettePlacement.allowed(after)) continue;
            int count;
            try { count = Integer.parseInt(props.getProperty(key, "0")); }
            catch (NumberFormatException e) { continue; }
            if (count >= 2 && count > scores.getOrDefault(before, 0)) {
                scores.put(before, count);
                preferred.put(before, after);
            }
        }
        ListTag blocks = preview.getList("blocks", 10);
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag entry = blocks.getCompound(i);
            String replacement = preferred.get(entry.getString("block"));
            if (replacement != null) entry.putString("block", replacement);
        }
    }

    private static void saveProfile(ServerPlayer player, String type, String stage, Map<String,Integer> lessons) {
        // This is shared across local saves for the same game installation/profile.
        // Dedicated servers maintain their own authoritative profile store.
        Path dir = FMLPaths.GAMEDIR.get().resolve("config").resolve("temple_of_the_axolotl")
                .resolve("axoluthu_lessons");
        Path file = dir.resolve(player.getUUID() + ".properties");
        try {
            Files.createDirectories(dir);
            Properties props = new Properties();
            if (Files.isRegularFile(file)) try (var in = Files.newInputStream(file)) { props.load(in); }
            String prefix = stage + "." + type + ".";
            for (var lesson : lessons.entrySet()) {
                String key = prefix + lesson.getKey();
                int previous;
                try { previous = Integer.parseInt(props.getProperty(key, "0")); }
                catch (NumberFormatException ignored) { previous = 0; }
                props.setProperty(key, Integer.toString(Math.min(100000, previous + lesson.getValue())));
            }
            Path temp = dir.resolve(player.getUUID() + ".tmp");
            try (var out = Files.newOutputStream(temp)) {
                props.store(out, "Axoluthu learned preferences; per owner UUID and building type");
            }
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(WaifuAxoluthuPlayerLessons.class)
                    .warn("[Axoluthu/Learning] Could not save cross-world lessons", e);
        }
    }
}
