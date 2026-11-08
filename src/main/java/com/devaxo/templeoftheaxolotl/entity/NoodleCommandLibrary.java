package com.devaxo.templeoftheaxolotl.entity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Per-player JSON routines shared by worlds using the same game directory/server installation. */
public final class NoodleCommandLibrary {
    private static final Gson GSON = new Gson();
    private NoodleCommandLibrary() {}
    private static Path folder(UUID player) {
        return FMLPaths.GAMEDIR.get().resolve("config").resolve("temple_of_the_axolotl")
                .resolve("abyssal_axolotl_commands").resolve(player.toString());
    }
    public static String normalize(String phrase) { return phrase.strip().toLowerCase(Locale.ROOT); }
    private static String filename(String phrase) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(normalize(phrase).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest) + ".json";
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    public static Map<String,List<String>> load(UUID player) {
        Map<String,List<String>> result = new LinkedHashMap<>();
        Path folder = folder(player);
        if (!Files.isDirectory(folder)) return result;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(folder, "*.json")) {
            for (Path file : files) {
                if (Files.size(file) > 16384 || result.size() >= 256) continue;
                try {
                    JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                    String phrase = normalize(json.get("phrase").getAsString());
                    if (!phrase.matches("[\\p{L}\\p{N} _-]{1,48}") || !file.getFileName().toString().equals(filename(phrase))) continue;
                    JsonArray tasks = json.getAsJsonArray("tasks");
                    if (tasks.size() < 1 || tasks.size() > 32) continue;
                    List<String> lines = new ArrayList<>();
                    for (var task : tasks) {
                        String line = task.getAsString();
                        if (!BabyNoodleChat.isAllowedTask(line)) { lines.clear(); break; }
                        lines.add(line);
                    }
                    if (!lines.isEmpty()) result.put(phrase, List.copyOf(lines));
                } catch (Exception ignored) { /* Skip corrupt entries without breaking other commands. */ }
            }
        } catch (Exception ignored) { }
        return result;
    }
    public static boolean save(UUID player, String phrase, List<String> tasks) {
        phrase = normalize(phrase);
        if (!phrase.matches("[\\p{L}\\p{N} _-]{1,48}") || tasks.isEmpty() || tasks.size() > 32) return false;
        for (String task : tasks) if (!BabyNoodleChat.isAllowedTask(task)) return false;
        try {
            Path folder = folder(player); Files.createDirectories(folder);
            JsonObject json = new JsonObject(); json.addProperty("version", 1); json.addProperty("phrase", phrase);
            JsonArray lines = new JsonArray(); for (String task : tasks) lines.add(task); json.add("tasks", lines);
            Path target = folder.resolve(filename(phrase));
            Path temp = Files.createTempFile(folder, "noodle-", ".tmp");
            Files.writeString(temp, GSON.toJson(json), StandardCharsets.UTF_8);
            try { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
            return true;
        } catch (Exception e) { return false; }
    }
    public static boolean forget(UUID player, String phrase) {
        try { return Files.deleteIfExists(folder(player).resolve(filename(phrase))); }
        catch (Exception ignored) { return false; }
    }
}
