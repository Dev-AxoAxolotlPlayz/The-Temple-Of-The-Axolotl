package com.devaxo.templeoftheaxolotl.entity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;
import net.neoforged.fml.loading.FMLPaths;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;

/** Cross-world, per-bonded-player architectural lessons. World coordinates stay in entity NBT. */
public final class WaifuAxoluthuKnowledgeLibrary {
    private static final Gson GSON = new Gson();
    private WaifuAxoluthuKnowledgeLibrary() {}
    private static Path file(UUID owner) {
        return FMLPaths.GAMEDIR.get().resolve("config").resolve("temple_of_the_axolotl")
                .resolve("axoluthu_architecture").resolve(owner + ".json");
    }
    public static ListTag load(UUID owner) {
        ListTag empty = new ListTag();
        if (owner == null) return empty;
        Path path = file(owner);
        try {
            if (!Files.isRegularFile(path) || Files.size(path) > 2_000_000) return empty;
            JsonObject json = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            if (json.get("version").getAsInt() != 1) return empty;
            CompoundTag wrapper = TagParser.parseTag(json.get("lessons_snbt").getAsString());
            return wrapper.getList("lessons", 10).copy();
        } catch (Exception ignored) { return empty; }
    }
    public static boolean save(UUID owner, ListTag lessons) {
        if (owner == null || lessons == null || lessons.size() > 48) return false;
        try {
            Path target = file(owner);
            Files.createDirectories(target.getParent());
            CompoundTag wrapper = new CompoundTag();
            wrapper.put("lessons", lessons.copy());
            JsonObject json = new JsonObject();
            json.addProperty("version", 1);
            json.addProperty("lessons_snbt", wrapper.toString());
            Path temp = Files.createTempFile(target.getParent(), "axoluthu-", ".tmp");
            try {
                Files.writeString(temp, GSON.toJson(json), StandardCharsets.UTF_8);
                try { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
                catch (AtomicMoveNotSupportedException ex) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
            } finally { Files.deleteIfExists(temp); }
            return true;
        } catch (Exception ignored) { return false; }
    }
}
