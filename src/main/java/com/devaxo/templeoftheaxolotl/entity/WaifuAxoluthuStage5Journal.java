package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.server.network.Filterable;
import java.util.ArrayList;
import java.util.List;

/** Stage 5.3: world-specific, owner-controlled, non-destructive project planning.
 * A design is a proposal, NOT permission to terraform or start construction.
 */
public final class WaifuAxoluthuStage5Journal {
    private static final String KEY = "stage5HousingProject";
    private WaifuAxoluthuStage5Journal() {}

    public static String plan(WaifuAxoluthuEntity axo) {
        if (axo.getBondedPlayer() == null) return "BOND_REQUIRED";
        CompoundTag memory = axo.getArchitecturalLearning();
        if (memory.contains(KEY, 10)) return "Existing project preserved. Use architecture journal to review it.";
        WaifuAxoluthuStage5Design.Candidate design = WaifuAxoluthuStage5Design.propose(axo);
        if (design == null) return "No vacant site found. No project committed and no blocks changed.";
        CompoundTag project = new CompoundTag();
        project.putInt("version", 1);
        project.putString("dimension", axo.level().dimension().location().toString());
        project.putLong("origin", design.origin().asLong());
        project.putInt("width", design.width());
        project.putInt("depth", design.depth());
        project.putString("footprint", design.footprint());
        project.putString("roof", design.roof());
        project.putString("phase", "DESIGN_REVIEW");
        project.putBoolean("siteAuthorized", false);
        project.putBoolean("materialsCalculated", false);
        project.putBoolean("constructionAllowed", false);
        project.putString("notes", "Four-block clear interior; doors, rooms, windows, roof, foundations and material manifest must be resolved before construction.");
        memory.put(KEY, project);
        return "Project saved at " + design.origin().toShortString() + " (" + design.footprint() + ", "
                + design.width() + "x" + design.depth() + ", " + design.roof()
                + "). DESIGN_REVIEW only; no site reserved, materials gathered, or blocks placed.";
    }

    /** Stage 5.4: conservative planning estimate, not an exact placed-block manifest.
     * An exact manifest requires the eventual Stage 5.5 block-by-block blueprint.
     * No terrain or inventory is changed by this method.
     */
    public static String materials(WaifuAxoluthuEntity axo) {
        CompoundTag memory = axo.getArchitecturalLearning();
        if (!memory.contains(KEY, 10)) return "No project. Use architecture plan first.";
        CompoundTag project = memory.getCompound(KEY);
        if (!axo.level().dimension().location().toString().equals(project.getString("dimension")))
            return "Project belongs to another dimension; no preparation performed.";
        if (project.getBoolean("constructionAllowed"))
            return "Construction already authorized; refusing to replace its manifest.";
        int w = project.getInt("width"), d = project.getInt("depth");
        if (w < 5 || d < 5 || w > 31 || d > 31) return "Invalid project dimensions.";
        int area = w * d;
        // A reduced footprint is only an estimate until the actual plan is rasterized.
        if ("L_WING".equals(project.getString("footprint"))) area = Math.max(1, area - (w / 3) * (d / 3));
        if ("OFFSET_WING".equals(project.getString("footprint"))) area = Math.max(1, area - (w / 4) * (d / 4));
        int perimeter = 2 * (w + d) - 4;
        int floor = area;
        int wall = Math.max(0, 5 * perimeter - 12); // 4 clear air blocks plus structural allowance
        int roof = (int)Math.ceil(area * ("HIP".equals(project.getString("roof")) ? 1.8 : 2.0));
        int glass = Math.max(8, perimeter / 3);
        int supports = Math.max(16, perimeter / 2);
        int lighting = Math.max(4, area / 18);
        CompoundTag manifest = new CompoundTag();
        manifest.putInt("oak_planks", floor + wall);
        manifest.putInt("spruce_stairs", roof);
        manifest.putInt("oak_logs", supports);
        manifest.putInt("glass_panes", glass);
        manifest.putInt("lanterns", lighting);
        manifest.putInt("oak_door", 1);
        manifest.putInt("chest", 1);
        manifest.putInt("crafting_table", 1);
        project.put("preliminaryManifest", manifest);
        project.putBoolean("materialsCalculated", false);
        project.putString("phase", "MATERIAL_ESTIMATE");
        project.putString("notes", "Preliminary estimate only; exact room geometry, door/window placement, roof slopes, recipes, site authorization and block-by-block material quantities are not yet finalized. Do not gather or build from this estimate.");
        return "Preliminary estimate recorded (NOT a build-ready manifest): " + summary(manifest)
                + ". Use architecture book for a snapshot. No blocks or inventories changed.";
    }

    /** Deterministic block coordinates and exact counts for the current preview geometry.
     * No resource collection or construction is authorized by this action.
     */
    public static String blueprint(WaifuAxoluthuEntity axo) {
        CompoundTag memory = axo.getArchitecturalLearning();
        if (!memory.contains(KEY, 10)) return "No project. Use architecture plan first.";
        CompoundTag project = memory.getCompound(KEY);
        if (!axo.level().dimension().location().toString().equals(project.getString("dimension")))
            return "Project belongs to another dimension.";
        if (project.getBoolean("constructionAllowed")) return "Cannot replace an authorized construction plan.";
        try {
            CompoundTag blueprint = WaifuAxoluthuStage5Blueprint.create(project);
            project.put("blueprintPreview", blueprint);
            project.putString("phase", "BLUEPRINT_REVIEW");
            project.putBoolean("materialsCalculated", false);
            project.putBoolean("constructionAllowed", false);
            project.putString("notes", "Exact counts for preview geometry only. Stair orientation, detailed rooms, player-built block protection, site authorization, recipes, staging and gathering are pending. No construction permitted.");
            return "Preview blueprint saved: " + blueprint.getInt("placements") + " block placements. Materials: "
                    + previewSummary(blueprint.getCompound("materials")) + ". No world changes.";
        } catch (IllegalArgumentException ex) { return "Blueprint rejected: " + ex.getMessage(); }
    }

    private static String previewSummary(CompoundTag materials) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        java.util.List<String> keys = new java.util.ArrayList<>(materials.getAllKeys());
        java.util.Collections.sort(keys);
        for (String key : keys) lines.add(key + " x" + materials.getInt(key));
        return String.join(", ", lines);
    }

    private static String summary(CompoundTag m) {
        java.util.List<String> entries = new java.util.ArrayList<>();
        for (String key : java.util.List.of("oak_planks", "spruce_stairs", "oak_logs", "glass_panes", "lanterns", "oak_door", "chest", "crafting_table"))
            entries.add(key.replace('_', ' ') + " x" + m.getInt(key));
        return String.join(", ", entries);
    }

    /** Non-destructive preparation survey. Preview geometry is NOT a build-ready plan. */
    public static String survey(WaifuAxoluthuEntity axo) {
        CompoundTag memory=axo.getArchitecturalLearning();
        if (!memory.contains(KEY,10)) return "Plan a house first.";
        CompoundTag p=memory.getCompound(KEY);
        if (!axo.level().dimension().location().toString().equals(p.getString("dimension")))
            return "The project is in a different dimension.";
        if (!p.contains("blueprintPreview",10)) return "Generate the blueprint preview first.";
        net.minecraft.nbt.ListTag blocks=p.getCompound("blueprintPreview").getList("blocks",10);
        if (blocks.size()>12000) return "Too many blocks to survey safely.";
        int obstructed=0, unloaded=0, examined=0;
        for (int i=0;i<blocks.size();i++) {
            BlockPos pos=BlockPos.of(blocks.getCompound(i).getLong("pos"));
            if (!axo.level().isLoaded(pos)) { unloaded++; continue; }
            examined++;
            net.minecraft.world.level.block.state.BlockState state=axo.level().getBlockState(pos);
            if (!state.isAir() && !state.canBeReplaced()) obstructed++;
        }
        p.putInt("surveyObstructions",obstructed);
        p.putInt("surveyUnloaded",unloaded);
        p.putString("phase","SITE_SURVEY_REVIEW");
        p.putBoolean("siteAuthorized",false);
        p.putBoolean("constructionAllowed",false);
        p.putString("notes","Preconstruction survey only. " + obstructed + " occupied positions and " + unloaded
            + " unloaded positions. Player-placed block detection and exact blueprint completion are still required. No terrain modified.");
        return "Surveyed " + examined + " positions: " + obstructed + " occupied, " + unloaded
            + " unloaded. Construction remains LOCKED; no blocks or inventory changed.";
    }

    /** Stage 5.4e: audit the entity's OWN inventory, never count or withdraw player supplies.
     * Preview materials are informative only and cannot authorize construction. */
    public static String inventoryAudit(WaifuAxoluthuEntity axo) {
        CompoundTag memory = axo.getArchitecturalLearning();
        if (!memory.contains(KEY, 10)) return "Plan a house first.";
        CompoundTag project = memory.getCompound(KEY);
        if (!axo.level().dimension().location().toString().equals(project.getString("dimension")))
            return "This project belongs to another dimension.";
        if (!project.contains("blueprintPreview", 10)) return "Generate a blueprint preview first.";
        CompoundTag required = project.getCompound("blueprintPreview").getCompound("materials");
        CompoundTag available = new CompoundTag();
        CompoundTag missing = new CompoundTag();
        var bag = axo.getAcquiredMaterials();
        int missingTypes = 0;
        for (String key : required.getAllKeys()) {
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    net.minecraft.resources.ResourceLocation.parse(key));
            int count = 0;
            for (int i = 0; i < bag.getContainerSize(); i++) {
                var stack = bag.getItem(i);
                if (stack.is(item)) count += stack.getCount();
            }
            available.putInt(key, count);
            int deficit = Math.max(0, required.getInt(key) - count);
            if (deficit > 0) { missing.putInt(key, deficit); missingTypes++; }
        }
        project.put("inventoryAuditAvailable", available);
        project.put("inventoryAuditMissing", missing);
        project.putString("phase", "MATERIAL_AUDIT");
        project.putBoolean("constructionAllowed", false);
        project.putBoolean("siteAuthorized", false);
        project.putString("notes", "Inventory audit only. The geometry remains a preview; player-built protection, exact recipes and construction staging are pending. No supplies moved.");
        return "Checked Axoluthu's own inventory: " + missingTypes + " material types still needed. "
                + previewSummary(missing) + ". No items taken or blocks placed.";
    }


    public static String liveView(WaifuAxoluthuEntity axo) {
        CompoundTag memory=axo.getArchitecturalLearning();
        if (!memory.contains(KEY,10)) return "No house planned yet.\nUse /tota axoluthu architecture plan to let me choose a design!";
        CompoundTag p=memory.getCompound(KEY);
        StringBuilder out=new StringBuilder("MY HOUSE PROJECT\n\n");
        out.append("Phase: ").append(p.getString("phase")).append("\n")
            .append("Site: ").append(BlockPos.of(p.getLong("origin")).toShortString()).append("\n")
            .append("Dimension: ").append(p.getString("dimension")).append("\n")
            .append("Shape: ").append(p.getString("footprint")).append("\n")
            .append("Size: ").append(p.getInt("width")).append(" x ").append(p.getInt("depth")).append("\n")
            .append("Roof: ").append(p.getString("roof")).append("\n\n");
        if (p.contains("blueprintPreview",10)) {
            CompoundTag preview=p.getCompound("blueprintPreview");
            out.append("BLUEPRINT PREVIEW: ").append(preview.getInt("placements")).append(" blocks\n");
            appendMaterials(out,preview.getCompound("materials"));
        } else if (p.contains("preliminaryManifest",10)) {
            out.append("ESTIMATED MATERIALS (not exact)\n");
            appendMaterials(out,p.getCompound("preliminaryManifest"));
        }
        if (p.contains("inventoryAuditMissing", 10)) {
            out.append("\nMISSING FROM MY BAG\n");
            appendMaterials(out, p.getCompound("inventoryAuditMissing"));
        }
        out.append("\nSITE: ").append(p.getBoolean("siteAuthorized")?"APPROVED":("AWAITING_SITE_APPROVAL".equals(p.getString("phase")) || "CONFIRM_TERRAIN_CHANGES".equals(p.getString("phase")) ? "AWAITING APPROVAL" : "NOT APPROVED"))
            .append("\nCONSTRUCTION: ").append(p.getBoolean("siteAuthorized")?p.getString("constructionPhase").replace('_',' '):"LOCKED")
            .append("\n\nNOTES\n").append(p.getString("notes"));
        return out.substring(0,Math.min(11000,out.length()));
    }
    private static void appendMaterials(StringBuilder out,CompoundTag materials) {
        java.util.List<String> keys=new java.util.ArrayList<>(materials.getAllKeys());
        java.util.Collections.sort(keys);
        for (String key:keys) out.append(key.replace("minecraft:","")).append(": ").append(materials.getInt(key)).append("\n");
    }

    public static String status(WaifuAxoluthuEntity axo) {
        CompoundTag memory = axo.getArchitecturalLearning();
        if (!memory.contains(KEY, 10)) return "No active Stage 5 project. Use architecture plan.";
        CompoundTag p = memory.getCompound(KEY);
        return "Project " + p.getString("phase") + " at " + BlockPos.of(p.getLong("origin")).toShortString()
                + " in " + p.getString("dimension") + ", " + p.getString("footprint") + " "
                + p.getInt("width") + "x" + p.getInt("depth") + ", roof=" + p.getString("roof")
                + "; site authorization=" + p.getBoolean("siteAuthorized")
                + "; manifest complete=" + p.getBoolean("materialsCalculated")
                + (p.contains("blueprintPreview", 10) ? "; preview placements=" + p.getCompound("blueprintPreview").getInt("placements") : "")
                + (p.contains("preliminaryManifest", 10) ? "; estimate: " + summary(p.getCompound("preliminaryManifest")) : "") + ".";
    }

    public static String giveBook(WaifuAxoluthuEntity axo, ServerPlayer player) {
        CompoundTag memory = axo.getArchitecturalLearning();
        if (!memory.contains(KEY, 10)) return "No project to record. Use architecture plan first.";
        CompoundTag p = memory.getCompound(KEY);
        List<Filterable<String>> pages = new ArrayList<>();
        pages.add(Filterable.passThrough("Axoluthu's House Plan\n\nStage: " + p.getString("phase")
                + "\nDimension: " + p.getString("dimension")
                + "\nSite: " + BlockPos.of(p.getLong("origin")).toShortString()
                + "\nShape: " + p.getString("footprint")
                + "\nSize: " + p.getInt("width") + " x " + p.getInt("depth")
                + "\nRoof: " + p.getString("roof")));
        pages.add(Filterable.passThrough("Construction notes\n\n" + p.getString("notes")
                + "\n\nExact manifest: NOT YET CALCULATED\nSite: NOT AUTHORIZED\nThis is not a building order."));
        if (p.contains("preliminaryManifest", 10)) {
            CompoundTag estimate = p.getCompound("preliminaryManifest");
            StringBuilder page = new StringBuilder("Preliminary supplies\nESTIMATE ONLY\n\n");
            for (String key : java.util.List.of("oak_planks", "spruce_stairs", "oak_logs", "glass_panes", "lanterns", "oak_door", "chest", "crafting_table")) {
                String line = key.replace('_', ' ') + ": " + estimate.getInt(key) + "\n";
                if (page.length() + line.length() > 230) {
                    pages.add(Filterable.passThrough(page.toString()));
                    page = new StringBuilder("Supplies continued\n\n");
                }
                page.append(line);
            }
            pages.add(Filterable.passThrough(page.toString()));
        }
        if (p.contains("blueprintPreview", 10)) {
            CompoundTag preview = p.getCompound("blueprintPreview");
            pages.add(Filterable.passThrough("Blueprint preview\n\n" + preview.getInt("placements")
                    + " placements\nNot approved for building\n\nGeometry material counts:"));
            CompoundTag counts = preview.getCompound("materials");
            StringBuilder page = new StringBuilder();
            java.util.List<String> keys = new java.util.ArrayList<>(counts.getAllKeys());
            java.util.Collections.sort(keys);
            for (String key : keys) {
                String line = key.replace("minecraft:", "") + " x" + counts.getInt(key) + "\n";
                if (page.length() + line.length() > 200) {
                    pages.add(Filterable.passThrough(page.toString()));
                    page = new StringBuilder();
                }
                page.append(line);
            }
            if (!page.isEmpty()) pages.add(Filterable.passThrough(page.toString()));
        }
        ItemStack book = new ItemStack(Items.WRITABLE_BOOK);
        book.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(pages));
        if (!player.getInventory().add(book)) player.drop(book, false);
        return "Project draft copied into a Book and Quill. The entity's saved project is authoritative; book is a snapshot.";
    }
}
