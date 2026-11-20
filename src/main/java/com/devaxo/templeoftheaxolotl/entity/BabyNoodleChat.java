
package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.Holder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import java.util.*;
import java.util.regex.*;

/** Owner-only natural-language chat commands; never intercepts unrelated chat. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class BabyNoodleChat {
    private static final Pattern GREET = Pattern.compile("(?i)^meet\\s+(.+?)\\s+and\\s+go\\s+say\\s+hi[!.]?\\s*$");
    private static final Pattern NAME = Pattern.compile("(?i)^your name is\\s+(.+?)\\s*$");
    private static final Pattern LOGS = Pattern.compile("(?i)^(?:fetch|gather|collect|bring|get)\\s+(?:me\\s+)?(?:(\\d+)\\s+)?(?:logs?|wood)(?:\\s+(\\d+))?\\s*$");
    private static final Pattern THINGS = Pattern.compile("(?i)^fetch\\s+me\\s+the\\s+things\\s+to\\s+make\\s+(\\d+|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety|hundred|[a-z]+-[a-z]+)\\s+([a-z_ :]+?)\\s*$");
    private static final Pattern MATERIALS = Pattern.compile("(?i)^(?:fetch|gather|collect|bring|get)\\s+(?:the\\s+)?(?:materials|ingredients)\\s+(?:for|to craft)\\s+(?:(\\d+)\\s+)?(.+?)\\s*$");
    private static final Pattern GENERIC = Pattern.compile("(?i)^(?:fetch|gather|collect|bring|get)\\s+(?:me\\s+)?(?:(\\d+)\\s+)?([a-z_ :]+?)\\s*$");

    /** Validate signed-book instructions before an anvil can program them. */
    public static boolean isAllowedTask(String instruction) {
        if (instruction.length() > 120) return false;
        String clean = stripCourtesy(instruction);
        return LOGS.matcher(clean).matches() || THINGS.matcher(clean).matches()
                || MATERIALS.matcher(clean).matches() || GENERIC.matcher(clean).matches();
    }

    private static java.util.Map<Item,Integer> plan(ServerPlayer owner, AbyssalAxolotlEntity a, String line) {
        String command = stripCourtesy(line);
        Matcher logs = LOGS.matcher(command);
        if (logs.matches()) {
            String qty = logs.group(1) != null ? logs.group(1) : logs.group(2);
            int count = qty == null ? 16 : safeCount(qty);
            return java.util.Map.of(biomeLog(a), count);
        }
        Matcher things = THINGS.matcher(command);
        Matcher materials = MATERIALS.matcher(command);
        Matcher generic = GENERIC.matcher(command);
        String requested; int count;
        if (things.matches()) { count = wordCount(things.group(1)); requested = things.group(2); }
        else if (materials.matches()) { count = materials.group(1) == null ? 1 : safeCount(materials.group(1)); requested = materials.group(2); }
        else if (generic.matches()) { count = generic.group(1) == null ? 1 : safeCount(generic.group(1)); requested = generic.group(2); }
        else return null;
        requested = requested.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        ResourceLocation id = ResourceLocation.tryParse(requested.contains(":") ? requested : "minecraft:" + requested);
        if (id != null && !BuiltInRegistries.ITEM.containsKey(id) && requested.endsWith("s"))
            id = ResourceLocation.tryParse("minecraft:" + requested.substring(0, requested.length()-1));
        if (id == null || !id.getNamespace().equals("minecraft")
                || !BuiltInRegistries.ITEM.containsKey(id) || !eligible(id.getPath())) return null;
        return findIngredients(owner, BuiltInRegistries.ITEM.get(id), count);
    }

    private static String stripCourtesy(String value) {
        return value.replaceFirst("(?i)^could you\\s+", "")
                .replaceFirst("(?i)^please\\s+", "")
                .replaceFirst("(?i)\\s+please[!?.]*$", "").trim();
    }

    private static int wordCount(String value) {
        if (value.matches("\\d+")) return safeCount(value);
        String[] names = {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"};
        String word = value.toLowerCase(Locale.ROOT).replace('-', ' ');
        for (int i = 1; i < names.length; i++) if (names[i].equals(word)) return i;
        String[] tens = {"twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"};
        for (int i = 0; i < tens.length; i++) {
            if (word.equals(tens[i])) return (i + 2) * 10;
            for (int n = 1; n <= 9; n++) if (word.equals(tens[i] + " " + names[n])) return (i + 2) * 10 + n;
        }
        return 1;
    }

    private static int safeCount(String value) {
        try { return Math.max(1, Math.min(4096, Integer.parseInt(value))); }
        catch (NumberFormatException ignored) { return 1; }
    }

    private static java.util.Map<Item,Integer> customPlan(ServerPlayer owner, AbyssalAxolotlEntity a, String phrase) {
        // Only the server profile library is executable: forgetting a phrase cannot
        // be undone by an old book stored in a chest.
        Map<String,List<String>> commands = NoodleCommandLibrary.load(owner.getUUID());
        List<String> instructions = commands.get(phrase.toLowerCase(Locale.ROOT));
        if (instructions == null) return null;
        Map<Item,Integer> combined = new LinkedHashMap<>();
        for (String line : instructions) {
            Map<Item,Integer> ingredients = plan(owner, a, line);
            if (ingredients == null) return null;
            for (var ingredient : ingredients.entrySet()) {
                combined.merge(ingredient.getKey(), ingredient.getValue(), Integer::sum);
                if (combined.get(ingredient.getKey()) > 4096) return null;
            }
        }
        return combined;
    }

    @SubscribeEvent
    public static void chat(ServerChatEvent event) {
        handleText(event.getPlayer(), event.getRawText().trim(), null);
    }

    /** Shared entry for chat and /hey. A non-null target restricts execution to one owned companion. */
    public static void handleText(ServerPlayer owner, String text, java.util.UUID target) {
        var nearby = owner.serverLevel().getEntitiesOfClass(AbyssalAxolotlEntity.class,
                owner.getBoundingBox().inflate(32), a -> a.isAlive() && a.isTame() && a.isOwnedBy(owner)
                        && (target == null || target.equals(a.getUUID())));
        Matcher naming = NAME.matcher(text);
        if (naming.matches()) {
            for (AbyssalAxolotlEntity a : nearby) if (!a.hasPermanentName()) {
                if (a.assignName(owner, naming.group(1))) owner.sendSystemMessage(Component.literal("Your Abyssal Axolotl is now permanently named " + a.permanentName() + "!"));
                else owner.sendSystemMessage(Component.literal("Name must be 1-32 letters, numbers, spaces, hyphens, or underscores."));
                return;
            }
            return;
        }

        if (!text.regionMatches(true, 0, "hey ", 0, 4)) return;
        for (AbyssalAxolotlEntity a : nearby) {
            if (!a.hasPermanentName()) continue;
            String prefix = "hey " + a.permanentName();
            if (!text.regionMatches(true, 0, prefix, 0, prefix.length())) continue;
            if (text.length() > prefix.length() && text.charAt(prefix.length()) != ',' && text.charAt(prefix.length()) != ' ') continue;
            String command = text.substring(prefix.length()).replaceFirst("^[,\\s]+", "").trim();
            // Politeness is optional, including either leading or trailing 'please'.
            command = stripCourtesy(command);
            Matcher greeting = GREET.matcher(command);
            if (greeting.matches()) {
                String targetName = greeting.group(1).trim();
                var matches = owner.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                        a.getBoundingBox().inflate(24), entity -> entity != a && entity.isAlive()
                                && entity.getCustomName() != null && entity.getCustomName().getString().equalsIgnoreCase(targetName)
                                && (entity instanceof net.minecraft.world.entity.animal.Animal
                                || entity instanceof net.minecraft.world.entity.npc.AbstractVillager));
                matches.sort(java.util.Comparator.comparingDouble(a::distanceToSqr));
                if (matches.isEmpty()) owner.sendSystemMessage(Component.literal("No named passive friend found nearby: " + targetName));
                else if (!a.greet(matches.get(0))) owner.sendSystemMessage(Component.literal(a.permanentName() + " is busy right now."));
                return;
            }
            if (command.isEmpty()) { a.acknowledge(); return; }
            if (command.equalsIgnoreCase("follow me") || command.equalsIgnoreCase("follow")) {
                a.cancelJob(); a.setGuarding(false); a.setOrderedToSit(false); a.acknowledge(); return;
            }
            if (command.equalsIgnoreCase("sit") || command.equalsIgnoreCase("stay")) {
                a.cancelJob(); a.setGuarding(false); a.setOrderedToSit(true); a.acknowledge(); return;
            }
            if (command.equalsIgnoreCase("stop") || command.equalsIgnoreCase("cancel")) {
                a.cancelJob(); a.setGuarding(false); a.acknowledge(); return;
            }
            if (command.equalsIgnoreCase("protect me") || command.equalsIgnoreCase("please protect me")) {
                a.cancelJob(); a.setOrderedToSit(false); a.setGuarding(true); a.acknowledge();
                owner.sendSystemMessage(Component.literal(a.permanentName() + " is guarding you.")); return;
            }
            if (command.toLowerCase(Locale.ROOT).startsWith("fetch ")) {
                String[] parts = command.substring(6).trim().split("\\s+");
                if (parts.length == 2) {
                    ResourceLocation requestedId = ResourceLocation.tryParse(parts[0].contains(":") ? parts[0] : "minecraft:" + parts[0]);
                    int amount;
                    try { amount = Integer.parseInt(parts[1]); } catch (NumberFormatException ex) { amount = 0; }
                    if (requestedId == null || !requestedId.getNamespace().equals("minecraft")
                            || !BuiltInRegistries.ITEM.containsKey(requestedId) || !eligible(requestedId.getPath())
                            || amount < 1 || amount > 4096) {
                        owner.sendSystemMessage(Component.literal("Fetch requires an unprocessed vanilla resource and an amount from 1 to 4096.")); return;
                    }
                    if (a.busy()) { owner.sendSystemMessage(Component.literal(a.permanentName() + " is already on a mission.")); return; }
                    Item requestedItem = BuiltInRegistries.ITEM.get(requestedId);
                    a.fetch(requestedItem, amount);
                    owner.sendSystemMessage(Component.literal(a.permanentName() + " is fetching " + amount + " " + requestedItem.getDescription().getString() + "!"));
                    return;
                }
            }
            Matcher things = THINGS.matcher(command);
            Matcher materials = MATERIALS.matcher(command);
            if (things.matches() || materials.matches()) {
                if (a.busy()) { owner.sendSystemMessage(Component.literal(a.permanentName() + " is already on a mission.")); return; }
                int crafts;
                try { crafts = things.matches() ? wordCount(things.group(1)) : (materials.group(1) == null ? 1 : Integer.parseInt(materials.group(1))); }
                catch (NumberFormatException ex) { crafts = 8; }
                crafts = Math.max(1, Math.min(8, crafts));
                String requested = (things.matches() ? things.group(2) : materials.group(2)).trim().toLowerCase(Locale.ROOT).replace(' ', '_');
                ResourceLocation id = ResourceLocation.tryParse(requested.contains(":") ? requested : "minecraft:" + requested);
                if (id != null && !BuiltInRegistries.ITEM.containsKey(id) && requested.endsWith("s")) id = ResourceLocation.tryParse("minecraft:" + requested.substring(0, requested.length()-1));
                if (id != null && id.getPath().contains("diamond")) {
                    if (a.scoutDiamond(owner)) owner.sendSystemMessage(Component.literal(a.permanentName() + " is scouting nearby loaded terrain for diamond ore!"));
                    else owner.sendSystemMessage(Component.literal("No diamond ore found in nearby loaded terrain, or the noodle is busy."));
                    return;
                }
                if (id == null || !id.getNamespace().equals("minecraft") || !BuiltInRegistries.ITEM.containsKey(id)
                        || !eligible(id.getPath())) {
                    owner.sendSystemMessage(Component.literal("That item is not eligible for Abyssal Axolotl recipe gathering.")); return;
                }

                // Fixed: targetItem avoids collision with the UUID target parameter.
                Item targetItem = BuiltInRegistries.ITEM.get(id);
                java.util.Map<Item,Integer> ingredients = findIngredients(owner, targetItem, crafts);
                if (ingredients == null || ingredients.isEmpty()) {
                    owner.sendSystemMessage(Component.literal("No eligible vanilla crafting recipe found for " + targetItem.getDescription().getString() + ".")); return;
                }
                a.fetchMaterials(ingredients, crafts);
                owner.sendSystemMessage(Component.literal(a.permanentName() + " is collecting ingredients for " + crafts + " " + targetItem.getDescription().getString() + "!"));
                return;
            }
            Matcher logs = LOGS.matcher(command);
            if (logs.matches()) {
                if (a.busy()) { owner.sendSystemMessage(Component.literal(a.permanentName() + " is already on a mission.")); return; }
                String amount = logs.group(1) != null ? logs.group(1) : logs.group(2);
                int quantity;
                try { quantity = amount == null ? 16 : Integer.parseInt(amount); } catch (NumberFormatException ex) { quantity = 64; }
                quantity = Math.max(1, Math.min(4096, quantity));
                Item log = biomeLog(a);
                a.fetch(log, quantity);
                owner.sendSystemMessage(Component.literal(a.permanentName() + " is gathering " + quantity + " " + log.getDescription().getString() + "!"));
                return;
            }
            var custom = customPlan(owner, a, command);
            if (custom != null && !custom.isEmpty()) {
                if (a.busy()) { owner.sendSystemMessage(Component.literal(a.permanentName() + " is already on a mission.")); return; }
                a.fetchMaterials(custom, custom.values().stream().mapToInt(Integer::intValue).sum());
                owner.sendSystemMessage(Component.literal(a.permanentName() + " is gathering the entire shopping list in one trip!"));
                return;
            }
            owner.sendSystemMessage(Component.literal(a.permanentName() + " doesn't know that command yet."));
            a.acknowledge(); return;
        }
    }

    /** Resolve crafting-grid recipes recursively, stopping at non-crafting inputs (e.g. glass).
     * Never recursively smelt, stonecut, smith, or process ingredients. */
    private static java.util.Map<Item,Integer> findIngredients(ServerPlayer owner, Item result, int count) {
        java.util.Map<Item,Integer> found = new java.util.LinkedHashMap<>();
        if (!expandCrafting(owner, result, count, found, new java.util.HashSet<>(), 0)) return null;
        for (var pair : java.util.List.of(java.util.Map.entry(Items.IRON_NUGGET, Items.RAW_IRON),
                java.util.Map.entry(Items.GOLD_NUGGET, Items.RAW_GOLD))) {
            int nuggets = found.getOrDefault(pair.getKey(), 0);
            if (nuggets > 0) { found.remove(pair.getKey()); found.merge(pair.getValue(), (nuggets + 8) / 9, Integer::sum); }
        }
        return found;
    }

    private static boolean expandCrafting(ServerPlayer owner, Item item, int count,
                                          java.util.Map<Item,Integer> output, java.util.Set<Item> visiting, int depth) {
        if (depth > 12 || count < 1 || count > 4096 || !visiting.add(item)) return false;
        try {
            if (item == Items.IRON_NUGGET || item == Items.GOLD_NUGGET) {
                Item raw = item == Items.IRON_NUGGET ? Items.RAW_IRON : Items.RAW_GOLD;
                output.merge(item, count, Integer::sum);
                return output.get(item) <= 4096;
            }
            if (item == Items.IRON_INGOT || item == Items.GOLD_INGOT || item == Items.COPPER_INGOT) {
                Item raw = item == Items.IRON_INGOT ? Items.RAW_IRON : item == Items.GOLD_INGOT ? Items.RAW_GOLD : Items.RAW_COPPER;
                output.merge(raw, count, Integer::sum);
                return output.get(raw) <= 4096;
            }
            if (!eligible(BuiltInRegistries.ITEM.getKey(item).getPath())) return false;
            net.minecraft.world.item.crafting.CraftingRecipe chosen = null;
            for (var holder : owner.serverLevel().getRecipeManager().getRecipes()) {
                var recipe = holder.value();
                if (recipe instanceof net.minecraft.world.item.crafting.CraftingRecipe craft
                        && recipe.getResultItem(owner.registryAccess()).is(item)) {
                    chosen = craft; break;
                }
            }
            if (chosen == null) {
                if (!eligible(BuiltInRegistries.ITEM.getKey(item).getPath())) return false;
                output.merge(item, count, Integer::sum);
                return output.get(item) <= 4096;
            }
            int produced = Math.max(1, chosen.getResultItem(owner.registryAccess()).getCount());
            int batches = (count + produced - 1) / produced;
            for (var ingredient : chosen.getIngredients()) {
                if (ingredient.isEmpty()) continue;
                Item selected = null;
                for (var stack : ingredient.getItems()) {
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    if (id.getNamespace().equals("minecraft") && eligible(id.getPath())) {
                        selected = stack.getItem(); break;
                    }
                }
                if (selected == null || !expandCrafting(owner, selected, batches, output, visiting, depth + 1)) return false;
            }
            return true;
        } finally { visiting.remove(item); }
    }

    /** Only naturally harvested, mined or dropped vanilla materials. Never processed products. */
    public static boolean eligible(String id) {
        if (id.endsWith("_log") || id.endsWith("_stem") || id.endsWith("_wood")
                || id.endsWith("_hyphae") || id.endsWith("_leaves") || id.endsWith("_sapling")
                || id.endsWith("_ore") || id.endsWith("_seeds") || id.endsWith("_flower")
                || id.endsWith("_coral") || id.endsWith("_coral_fan") || id.endsWith("_mushroom")) return true;
        return RAW_RESOURCES.contains(id);
    }
    private static final Set<String> RAW_RESOURCES = Set.of(
            "dirt", "coarse_dirt", "rooted_dirt", "grass_block", "podzol", "mycelium", "mud", "clay", "clay_ball",
            "sand", "red_sand", "gravel", "flint", "stone", "cobblestone", "cobbled_deepslate", "deepslate", "tuff", "calcite", "dripstone_block", "pointed_dripstone", "granite", "diorite", "andesite", "obsidian", "crying_obsidian",
            "netherrack", "soul_sand", "soul_soil", "basalt", "blackstone", "magma_block", "end_stone", "ice", "packed_ice", "blue_ice", "snow", "snowball", "snow_block",
            "coal", "raw_iron", "raw_gold", "raw_copper", "diamond", "emerald", "lapis_lazuli", "redstone", "quartz", "amethyst_shard", "glowstone_dust", "ancient_debris",
            "wheat", "wheat_seeds", "beetroot", "beetroot_seeds", "carrot", "potato", "pumpkin", "melon", "melon_slice", "cocoa_beans", "sugar_cane", "bamboo", "cactus", "kelp", "seagrass", "sea_pickle", "lily_pad", "vine", "glow_berries", "sweet_berries", "nether_wart", "chorus_fruit", "apple",
            "leather", "feather", "string", "spider_eye", "rotten_flesh", "bone", "gunpowder", "slime_ball", "ender_pearl", "blaze_rod", "ghast_tear", "phantom_membrane", "rabbit_hide", "rabbit_foot", "ink_sac", "glow_ink_sac", "prismarine_shard", "prismarine_crystals", "nautilus_shell", "scute", "armadillo_scute", "turtle_scute", "honeycomb", "egg", "tropical_fish", "cod", "salmon", "pufferfish", "beef", "porkchop", "chicken", "mutton", "rabbit", "wool", "white_wool", "black_wool", "brown_wool", "gray_wool", "light_gray_wool", "pink_wool", "red_wool", "orange_wool", "yellow_wool", "lime_wool", "green_wool", "cyan_wool", "light_blue_wool", "blue_wool", "purple_wool", "magenta_wool", "brown_mushroom", "red_mushroom", "crimson_fungus", "warped_fungus", "crimson_roots", "warped_roots", "nether_sprouts", "shroomlight", "moss_block", "azalea", "flowering_azalea", "small_dripleaf", "big_dripleaf", "hanging_roots", "spore_blossom", "short_grass", "fern", "dead_bush", "stick", "oak_log", "spruce_log", "birch_log", "jungle_log", "acacia_log", "dark_oak_log", "mangrove_log", "cherry_log", "pale_oak_log");

    public static java.util.List<String> rawSuggestions() {
        return BuiltInRegistries.ITEM.keySet().stream().filter(id -> id.getNamespace().equals("minecraft") && eligible(id.getPath()))
                .map(ResourceLocation::toString).sorted().toList();
    }

    private static Item biomeLog(AbyssalAxolotlEntity a) {
        Holder<Biome> biome = a.level().getBiome(a.blockPosition());
        if (biome.is(Biomes.BIRCH_FOREST) || biome.is(Biomes.OLD_GROWTH_BIRCH_FOREST)) return Items.BIRCH_LOG;
        if (biome.is(Biomes.TAIGA) || biome.is(Biomes.SNOWY_TAIGA) || biome.is(Biomes.OLD_GROWTH_PINE_TAIGA) || biome.is(Biomes.OLD_GROWTH_SPRUCE_TAIGA) || biome.is(Biomes.GROVE)) return Items.SPRUCE_LOG;
        if (biome.is(Biomes.JUNGLE) || biome.is(Biomes.SPARSE_JUNGLE) || biome.is(Biomes.BAMBOO_JUNGLE)) return Items.JUNGLE_LOG;
        if (biome.is(Biomes.SAVANNA) || biome.is(Biomes.SAVANNA_PLATEAU) || biome.is(Biomes.WINDSWEPT_SAVANNA)) return Items.ACACIA_LOG;
        if (biome.is(Biomes.DARK_FOREST)) return Items.DARK_OAK_LOG;
        if (biome.is(Biomes.MANGROVE_SWAMP)) return Items.MANGROVE_LOG;
        if (biome.is(Biomes.CHERRY_GROVE)) return Items.CHERRY_LOG;
        if (biome.is(Biomes.CRIMSON_FOREST)) return Items.CRIMSON_STEM;
        if (biome.is(Biomes.WARPED_FOREST)) return Items.WARPED_STEM;
        return Items.OAK_LOG;
    }
}
