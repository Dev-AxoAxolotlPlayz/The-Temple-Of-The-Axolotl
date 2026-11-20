package com.devaxo.templeoftheaxolotl.compass;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

/** Optional adapters: no external classes in our linkage signatures. */
public final class ExternalNavigation {
    private ExternalNavigation() {}
    public record Target(String heading, String label, boolean valid, int x, int z) {}
    public static String kind(ItemStack stack) {
        return switch (BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()) {
            case "explorerscompass:explorerscompass" -> "STRUCTURE NAVIGATION";
            case "naturescompass:naturescompass" -> "BIOME NAVIGATION";
            case "travelerscompass:travelerscompass" -> "TARGET NAVIGATION";
            default -> "";
        };
    }
    private static Object read(ItemStack stack, String method) throws ReflectiveOperationException {
        return stack.getItem().getClass().getMethod(method, ItemStack.class).invoke(stack.getItem(), stack);
    }
    public static Target target(ItemStack stack) {
        String heading = kind(stack);
        if (heading.isEmpty()) return new Target("", "", false, 0, 0);
        try {
            if (heading.equals("TARGET NAVIGATION")) {
                Object found = read(stack, "getFoundPos");
                if (found != null && Boolean.TRUE.equals(found.getClass().getMethod("isValid").invoke(found))) {
                    BlockPos pos = (BlockPos) found.getClass().getMethod("blockPos").invoke(found);
                    return new Target(heading, "Target detected", true, pos.getX(), pos.getZ());
                }
            } else {
                String state = ((Enum<?>) read(stack, "getState")).name();
                if (state.equals("FOUND")) {
                    String type = heading.equals("BIOME NAVIGATION") ? "Biome" : "Structure";
                    Object key = read(stack, "get" + type + "Key");
                    String label = String.valueOf(key).replace('_', ' ');
                    return new Target(heading, label, true, (Integer) read(stack, "getFound" + type + "X"), (Integer) read(stack, "getFound" + type + "Z"));
                }
                return new Target(heading, state.equals("SEARCHING") ? "Searching..." : state.equals("NOT_FOUND") ? "Not found" : "No target", false, 0, 0);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return new Target(heading, "Compass unavailable", false, 0, 0);
        }
        return new Target(heading, "No target", false, 0, 0);
    }
}
