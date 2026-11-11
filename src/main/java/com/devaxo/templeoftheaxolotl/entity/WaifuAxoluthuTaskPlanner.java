package com.devaxo.templeoftheaxolotl.entity;

import java.util.List;

/** Stage 1D: declarative plans only. No inventory withdrawals or world edits. */
public final class WaifuAxoluthuTaskPlanner {
    private WaifuAxoluthuTaskPlanner() {}

    public enum Desire { EXPLORE, IMPROVE_WORKSHOP }
    public record Step(String id, String description, List<String> prerequisites) {}
    public record Plan(Desire desire, List<Step> steps) {
        public String summary() {
            return desire + " steps=" + steps.size() + " [" +
                    String.join(" -> ", steps.stream().map(Step::id).toList()) + "]";
        }
    }
    public static Plan create(Desire desire) {
        return switch (desire) {
            case EXPLORE -> new Plan(desire, List.of(
                    new Step("FIND_DESTINATION", "Select a reachable location", List.of()),
                    new Step("TRAVEL", "Navigate to selected location", List.of("FIND_DESTINATION")),
                    new Step("OBSERVE", "Survey surroundings", List.of("TRAVEL"))));
            case IMPROVE_WORKSHOP -> new Plan(desire, List.of(
                    new Step("LOCATE_WORKSHOP", "Find a suitable workshop location", List.of()),
                    new Step("CHECK_STORAGE", "Inspect authorized supplies", List.of("LOCATE_WORKSHOP")),
                    new Step("OBTAIN_IRON", "Acquire two iron ingots", List.of("CHECK_STORAGE")),
                    new Step("OBTAIN_PLANKS", "Acquire four planks, or eight if crafting table must be built", List.of("CHECK_STORAGE")),
                    new Step("LOCATE_CRAFTING_TABLE", "Find an accessible crafting table", List.of("LOCATE_WORKSHOP")),
                    new Step("CRAFT_CRAFTING_TABLE", "If absent, craft table in 2x2 grid", List.of("OBTAIN_PLANKS", "LOCATE_CRAFTING_TABLE")),
                    new Step("PLACE_CRAFTING_TABLE", "If needed, place crafted table safely", List.of("CRAFT_CRAFTING_TABLE")),
                    new Step("APPROACH_CRAFTING_TABLE", "Move within crafting reach", List.of("PLACE_CRAFTING_TABLE")),
                    new Step("CRAFT_SMITHING_TABLE", "Craft at 3x3 crafting table", List.of("OBTAIN_IRON", "OBTAIN_PLANKS", "APPROACH_CRAFTING_TABLE")),
                    new Step("PLACE_WORKSTATION", "Place smithing table safely", List.of("CRAFT_SMITHING_TABLE"))));
        };
    }
}
