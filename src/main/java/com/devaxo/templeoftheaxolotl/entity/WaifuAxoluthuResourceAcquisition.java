package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.repository.CelestialRepository;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.tags.ItemTags;

/** Explicit command-triggered, bounded repository withdrawals. No chest mutations. */
public final class WaifuAxoluthuResourceAcquisition {
    private WaifuAxoluthuResourceAcquisition() {}
    public record Result(boolean success, String detail) {}

    private static boolean matches(ItemStack stack, String material) {
        return "iron".equals(material) ? stack.is(Items.IRON_INGOT)
                : stack.is(ItemTags.PLANKS);
    }
    private static int held(WaifuAxoluthuEntity axo, String material) {
        int total = 0;
        var inv = axo.getAcquiredMaterials();
        for (int i=0; i<inv.getContainerSize(); i++)
            if (matches(inv.getItem(i),material)) total += inv.getItem(i).getCount();
        return total;
    }
    private static int store(WaifuAxoluthuEntity axo, ItemStack item) {
        int remaining = item.getCount();
        var inv = axo.getAcquiredMaterials();
        for (int i=0;i<inv.getContainerSize() && remaining>0;i++) {
            ItemStack old=inv.getItem(i);
            if (old.isEmpty() || !ItemStack.isSameItemSameComponents(old,item)) continue;
            int n=Math.min(remaining,old.getMaxStackSize()-old.getCount());
            if(n>0) { old.grow(n); inv.setChanged(); remaining-=n; }
        }
        for(int i=0;i<inv.getContainerSize() && remaining>0;i++) {
            if(!inv.getItem(i).isEmpty()) continue;
            int n=Math.min(remaining,item.getMaxStackSize());
            ItemStack copy=item.copy(); copy.setCount(n);
            inv.setItem(i,copy); remaining-=n;
        }
        return item.getCount()-remaining;
    }
    private static int capacity(WaifuAxoluthuEntity axo, ItemStack item) {
        int count=0;
        var inv=axo.getAcquiredMaterials();
        for(int i=0;i<inv.getContainerSize();i++) {
            ItemStack old=inv.getItem(i);
            if(old.isEmpty()) count+=item.getMaxStackSize();
            else if(ItemStack.isSameItemSameComponents(old,item))
                count+=Math.max(0,old.getMaxStackSize()-old.getCount());
        }
        return count;
    }
    /** Withdraw from explicitly authorized nearby block containers, then Repository.
     * All inventory operations occur on the server thread.
     */
    public static Result acquire(WaifuAxoluthuEntity axo, ServerPlayer player,
                                 String material, int amount) {
        if (axo.level().isClientSide() || axo.level()!=player.level()
                || !player.getUUID().equals(axo.getBondedPlayer()))
            return new Result(false,"DENIED: only bonded companion in same dimension");
        if(amount<1 || amount>64 || (!material.equals("iron") && !material.equals("planks")))
            return new Result(false,"DENIED: invalid material or quantity");
        int remaining=amount, chestTaken=0, repoTaken=0;
        // Only explicitly approved coordinates; never load chunks to retrieve items.
        java.util.List<Long> positions=new java.util.ArrayList<>(axo.getAuthorizedContainers());
        java.util.Collections.sort(positions);
        for(long key:positions) {
            if(remaining==0) break;
            net.minecraft.core.BlockPos pos=net.minecraft.core.BlockPos.of(key);
            if(pos.distSqr(axo.blockPosition())>12*12 || !axo.level().getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) continue;
            var blockEntity=axo.level().getBlockEntity(pos);
            if(!(blockEntity instanceof net.minecraft.world.Container container)) continue;
            for(int slot=0;slot<container.getContainerSize() && remaining>0;slot++) {
                ItemStack original=container.getItem(slot);
                if(original.isEmpty() || !matches(original,material)) continue;
                int n=Math.min(remaining,original.getCount());
                if(capacity(axo,original)<n) n=Math.min(n,capacity(axo,original));
                if(n<=0) continue;
                ItemStack removed=container.removeItem(slot,n);
                int stored=store(axo,removed);
                if(stored<removed.getCount()) {
                    ItemStack refund=removed.copy();refund.setCount(removed.getCount()-stored);
                    ItemStack current=container.getItem(slot);
                    if(current.isEmpty()) container.setItem(slot,refund);
                    else if(ItemStack.isSameItemSameComponents(current,refund)) {
                        current.grow(refund.getCount());container.setChanged();
                    } else player.getInventory().placeItemBackInInventory(refund);
                }
                container.setChanged();
                remaining-=stored;chestTaken+=stored;
            }
        }
        // Repository fallback uses the existing verified Stage 2B method.
        if(remaining>0) {
            Result repo=take(axo,player,material,remaining);
            if(repo.success()) { repoTaken=remaining;remaining=0; }
        }
        int obtained=amount-remaining;
        if(obtained>0)
            axo.recordAcquisition("COMBINED item="+material+" chest="+chestTaken
                    +" repository="+repoTaken+" requested="+amount+" stored="+obtained);
        return new Result(remaining==0,"requested="+amount+" obtained="+obtained
                +" chest="+chestTaken+" repository="+repoTaken
                +(remaining>0?" missing="+remaining:" SUCCESS"));
    }
    /** Task planner integration: fill only missing workshop ingredients.
     * Does not craft or place the workstation yet.
     */
    public static String fulfillWorkshop(WaifuAxoluthuEntity axo, ServerPlayer player) {
        int missingIron=Math.max(0,2-held(axo,"iron"));
        boolean needsCraftingTable = WaifuAxoluthuWorkshopActions.findCraftingTable(axo)==null
                && !WaifuAxoluthuWorkshopActions.hasCraftingTableItem(axo);
        int requiredPlanks=needsCraftingTable ? 8 : 4;
        int missingPlanks=Math.max(0,requiredPlanks-held(axo,"planks"));
        if(missingIron==0 && missingPlanks==0)
            return "MATERIALS_READY: workstation=" + (needsCraftingTable ? "must craft 2x2 table" : "available/carried") + "; smithing craft needs nearby placed crafting table";
        StringBuilder report=new StringBuilder("WORKSHOP ");
        if(missingIron>0) report.append("iron:").append(acquire(axo,player,"iron",missingIron).detail()).append(" ");
        if(missingPlanks>0) report.append("planks:").append(acquire(axo,player,"planks",missingPlanks).detail());
        return report.toString();
    }

    public static Result take(WaifuAxoluthuEntity axo, ServerPlayer player, String material, int amount) {
        if (axo.level().isClientSide() || axo.level() != player.level()
                || !player.getUUID().equals(axo.getBondedPlayer()))
            return new Result(false, "DENIED: player is not the bonded companion in this dimension");
        if (amount < 1 || amount > 64)
            return new Result(false, "DENIED: quantity must be 1..64");
        ItemStack sample;
        if ("iron".equals(material)) sample = new ItemStack(Items.IRON_INGOT);
        else if ("planks".equals(material)) {
            // Choose one existing plank variant, preserving its actual item identity.
            sample = ItemStack.EMPTY;
            for (int page=0; page<CelestialRepository.pages(player) && sample.isEmpty(); page++)
                for (var entry : CelestialRepository.readPage(player,page))
                    if (entry.sample().is(ItemTags.PLANKS)) {
                        sample = entry.sample().copy();
                        sample.setCount(1);
                        break;
                    }
        } else return new Result(false, "DENIED: unsupported material " + material);
        if (sample.isEmpty()) return new Result(false, "No matching " + material + " in Repository");
        int available = CelestialRepository.countAvailable(player, sample);
        if (available < amount)
            return new Result(false, "INSUFFICIENT: requested=" + amount + " available=" + available);
        var inventory = axo.getAcquiredMaterials();
        int capacity = 0;
        for (int slot=0; slot<inventory.getContainerSize(); slot++) {
            ItemStack held = inventory.getItem(slot);
            if (held.isEmpty()) capacity += sample.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(held, sample))
                capacity += Math.max(0, held.getMaxStackSize()-held.getCount());
        }
        if (capacity < amount)
            return new Result(false, "NO SPACE: carried inventory capacity=" + capacity + " requested=" + amount);
        // All preconditions checked before mutating either inventory.
        int taken = CelestialRepository.takeMatching(player, sample, amount);
        if (taken <= 0) return new Result(false, "Repository returned zero items");
        int remaining = taken;
        for (int slot=0; slot<inventory.getContainerSize() && remaining>0; slot++) {
            ItemStack held = inventory.getItem(slot);
            if (!held.isEmpty() && ItemStack.isSameItemSameComponents(held,sample)) {
                int n=Math.min(remaining,held.getMaxStackSize()-held.getCount());
                if (n>0) { held.grow(n); inventory.setChanged(); remaining-=n; }
            }
        }
        for (int slot=0; slot<inventory.getContainerSize() && remaining>0; slot++) {
            if (!inventory.getItem(slot).isEmpty()) continue;
            int n=Math.min(remaining,sample.getMaxStackSize());
            ItemStack stored=sample.copy(); stored.setCount(n);
            inventory.setItem(slot,stored); remaining-=n;
        }
        if (remaining>0) {
            // Defensive compensation for unexpected inventory mutation.
            ItemStack refund=sample.copy(); refund.setCount(remaining);
            int restored=CelestialRepository.insert(player,refund);
            if (restored<remaining) player.getInventory().placeItemBackInInventory(
                    refund.copyWithCount(remaining-restored));
        }
        int stored=taken-remaining;
        axo.recordAcquisition("source=BONDED_REPOSITORY item=" + sample.getHoverName().getString()
                + " requested=" + amount + " stored=" + stored);
        return new Result(stored==amount, "Repository -> Axoluthu inventory: "
                + stored + "/" + amount + " " + sample.getHoverName().getString());
    }
}
