package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.repository.CelestialRepository;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.ArrayList;
import java.util.List;

/** Read-only, bonded-player-initiated resource inspection. Never mutates inventories. */
public final class WaifuAxoluthuResourceScanner {
    private WaifuAxoluthuResourceScanner() {}
    public record Report(String summary, String details) {}
    public static Report scan(WaifuAxoluthuEntity axo, ServerPlayer player, int radius) {
        if (axo.level() != player.level() || !player.getUUID().equals(axo.getBondedPlayer()))
            return new Report("DENIED: not bonded or different dimension", "No inventory accessed");
        // Limited to loaded block entities near Axoluthu. Never loads chunks to scan.
        int chestIron = 0, chestPlanks = 0, containers = 0;
        List<String> locations = new ArrayList<>();
        BlockPos origin = axo.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -4, -radius),
                origin.offset(radius, 4, radius))) {
            if (!axo.level().getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) continue;
            BlockEntity blockEntity = axo.level().getBlockEntity(pos);
            if (!(blockEntity instanceof Container container)) continue;
            // Only inspect ordinary inventories; exclude non-container block entities.
            containers++;
            int iron = 0, planks = 0;
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (stack.is(Items.IRON_INGOT)) iron += stack.getCount();
                if (stack.is(net.minecraft.tags.ItemTags.PLANKS)) planks += stack.getCount();
            }
            chestIron += iron;
            chestPlanks += planks;
            if ((iron > 0 || planks > 0) && locations.size() < 8)
                locations.add(pos.toShortString() + ": iron=" + iron + " planks=" + planks);
        }
        int repoIron = CelestialRepository.countAvailable(player, new ItemStack(Items.IRON_INGOT));
        int repoPlanks = 0;
        for (int page = 0; page < CelestialRepository.pages(player); page++)
            for (CelestialRepository.Entry entry : CelestialRepository.readPage(player, page))
                if (entry.sample().is(net.minecraft.tags.ItemTags.PLANKS)) repoPlanks += entry.quantity();
        int totalIron = chestIron + repoIron, totalPlanks = chestPlanks + repoPlanks;
        String summary = "containers=" + containers + " chestIron=" + chestIron
                + " repositoryIron=" + repoIron + " chestPlanks=" + chestPlanks
                + " repositoryPlanks=" + repoPlanks + " | smithingTableNeed=2 iron,4 planks"
                + " missingIron=" + Math.max(0, 2-totalIron)
                + " missingPlanks=" + Math.max(0, 4-totalPlanks)
                + " | READ ONLY";
        String details = "Nearby resource containers (up to 8): "
                + (locations.isEmpty() ? "none containing target materials" : String.join("; ", locations));
        return new Report(summary, details);
    }
}
