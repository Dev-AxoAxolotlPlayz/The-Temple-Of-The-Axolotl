package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashMap;
import java.util.Map;

/** Persistent, owner-placed workstation handoff. Does not modify the owner's chosen placement. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class WaifuAxoluthuWorkstationHandoff {
    private WaifuAxoluthuWorkstationHandoff() {}
    private static final String KEY = "phase35OwnerSuggestion";
    private static final java.util.Set<Block> HANDOFF = java.util.Set.of(
            Blocks.GRINDSTONE, Blocks.COMPOSTER, Blocks.BREWING_STAND, Blocks.CRAFTING_TABLE,
            Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER, Blocks.SMITHING_TABLE,
            Blocks.ANVIL, Blocks.LECTERN, Blocks.CAULDRON);

    public static boolean isHandoffBlock(Block block) { return HANDOFF.contains(block); }

    /** Give exactly one planned workstation, then persist its obligation before advancing. */
    public static String offer(WaifuAxoluthuEntity axo, ServerLevel level,
                               CompoundTag request, Block block, int cursor) {
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(request.getUUID("owner"));
        if (owner == null || owner.level() != level) return "WAIT_OWNER_FOR_HANDOFF";
        var bag = axo.getAcquiredMaterials();
        int slot = -1;
        for (int i=0; i<bag.getContainerSize(); i++) {
            if (bag.getItem(i).is(block.asItem())) { slot=i; break; }
        }
        if (slot < 0) {
            WaifuAxoluthuRecipeBrain.ensure(axo, block.asItem(), 1, level.getGameTime());
            return "WAIT_MATERIALS";
        }
        String type=WaifuAxoluthuVillageStructures.normalize(request.getString("type"));
        ListTag learned=axo.getArchitecturalLearning().getList("approvedFurnishingLayout_"+type,10);
        for(int j=0;j<learned.size();j++) {
            CompoundTag item=learned.getCompound(j);
            if(item.getInt("fixtureIndex")!=cursor||!item.getString("block").equals(BuiltInRegistries.BLOCK.getKey(block).toString())||!item.contains("fullState"))continue;
            int x=item.getInt("dx"),z=item.getInt("dz"), turns=Math.floorMod(request.getInt("quarterTurns"),4);
            for(int k=0;k<turns;k++){int next=-z;z=x;x=next;}
            BlockPos pos=BlockPos.of(request.getLong("target")).offset(x,item.getInt("dy"),z);
            if(!WaifuAxoluthuVillageRegistry.withinPreview(request,pos)||!level.getChunkSource().hasChunk(pos.getX()>>4,pos.getZ()>>4))break;
            var state=net.minecraft.nbt.NbtUtils.readBlockState(level.holderLookup(net.minecraft.core.registries.Registries.BLOCK),item.getCompound("fullState")).rotate(level, pos, net.minecraft.world.level.block.Rotation.values()[turns]);
            if(!state.is(block)||!level.getBlockState(pos).isAir()||!state.canSurvive(level,pos))break;
            if(WaifuAxoluthuBuildNavigation.approach(axo,level,pos,1.5)!=null)return "MOVING";
            if(!level.setBlock(pos,state,3))break;
            bag.removeItem(slot,1);bag.setChanged();
            CompoundTag placed=item.copy();placed.putBoolean("placed",true);placed.putLong("placedPos",pos.asLong());
            ListTag list=request.getList("trial3HandedWorkstations",10);list.add(placed);request.put("trial3HandedWorkstations",list);request.putInt("furnishingCursor",cursor+1);return "CONTINUE";
        }
        ItemStack single = bag.getItem(slot).copy();
        single.setCount(1);
        // Do not discard or drop items if the owner's inventory is full.
        if (!owner.getInventory().add(single)) return "WAIT_OWNER_INVENTORY";
        bag.removeItem(slot, 1);
        bag.setChanged();
        CompoundTag issued = new CompoundTag();
        issued.putString("block", BuiltInRegistries.BLOCK.getKey(block).toString());
        issued.putBoolean("placed", false);
        issued.putInt("fixtureIndex", cursor);
        ListTag list = request.getList("trial3HandedWorkstations", 10);
        list.add(issued);
        request.put("trial3HandedWorkstations", list);
        request.putInt("furnishingCursor", cursor + 1);
        owner.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "[Axoluthu] Here's your " + block.getName().getString()
                        + "! Put it wherever you want. I'll remember where! ^u^"));
        return "CONTINUE";
    }

    /** Each item must have a surviving owner-placed block, not just a remembered coordinate. */
    public static boolean allPlaced(ServerLevel level, CompoundTag request) {
        ListTag list = request.getList("trial3HandedWorkstations", 10);
        for (int i=0; i<list.size(); i++) {
            CompoundTag item = list.getCompound(i);
            if (!item.getBoolean("placed")) return false;
            BlockPos pos = BlockPos.of(item.getLong("placedPos"));
            if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return false;
            ResourceLocation key = ResourceLocation.tryParse(item.getString("block"));
            if (key == null || !BuiltInRegistries.BLOCK.containsKey(key)
                    || !level.getBlockState(pos).is(BuiltInRegistries.BLOCK.get(key))) return false;
        }
        return true;
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer owner)
                || !(owner.level() instanceof ServerLevel level)) return;
        Block block = event.getPlacedBlock().getBlock();
        if (!isHandoffBlock(block)) return;
        for (WaifuAxoluthuEntity axo : level.getEntitiesOfClass(WaifuAxoluthuEntity.class,
                owner.getBoundingBox().inflate(64), a -> owner.getUUID().equals(a.getBondedPlayer()))) {
            CompoundTag memory = axo.getArchitecturalLearning();
            CompoundTag request = memory.getCompound(KEY);
            if (!request.getBoolean("pending") || !request.hasUUID("owner")
                    || !owner.getUUID().equals(request.getUUID("owner"))
                    || !level.dimension().location().toString().equals(request.getString("dimension"))) continue;
            if(!WaifuAxoluthuVillageRegistry.withinPreview(request,event.getPos()))continue;
            ListTag list = request.getList("trial3HandedWorkstations", 10);
            for (int i=0; i<list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                if (!entry.getBoolean("placed")
                        && entry.getString("block").equals(BuiltInRegistries.BLOCK.getKey(block).toString())) {
                    entry.putBoolean("placed", true);
                    entry.putLong("placedPos", event.getPos().asLong());
                    request.put("trial3HandedWorkstations", list);
                    memory.put(KEY, request);
                    owner.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "[Axoluthu] Got it! I'll remember that workstation location! :3"));
                    return;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        for (WaifuAxoluthuEntity axo : level.getEntitiesOfClass(WaifuAxoluthuEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(64))) {
            CompoundTag memory = axo.getArchitecturalLearning();
            CompoundTag request = memory.getCompound(KEY);
            ListTag list = request.getList("trial3HandedWorkstations", 10);
            boolean changed = false;
            for (int i=0; i<list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                if (entry.getBoolean("placed") && entry.getLong("placedPos") == pos.asLong()) {
                    entry.putBoolean("placed", false);
                    entry.remove("placedPos");
                    changed = true;
                }
            }
            if (changed) { request.put("trial3HandedWorkstations", list); memory.put(KEY, request); }
        }
    }

    /** Re-evaluate the mandatory fixture checklist with only verified external workstations.
     * All non-workstation requirements still use the normal in-world building inspector.
     */
    public static boolean satisfiesExternalWorkstations(ServerLevel level, CompoundTag request,
                WaifuAxoluthuBuildingRequirements.Result baseline) {
        if (!allPlaced(level, request)) return false;
        String missing = baseline.missing();
        if (missing == null || missing.isEmpty()) return true;
        // Conservative: only bypass requirements consisting exclusively of handed-off
        // workstations. Never bypass lighting, doors, beds, storage or special structures.
        String[] parts = missing.split(", ");
        Map<String, Integer> outside = new HashMap<>();
        BlockPos origin = BlockPos.of(request.getLong("target"));
        int width = WaifuAxoluthuVillageStructures.width(
                WaifuAxoluthuVillageStructures.normalize(request.getString("type")));
        for (int i=0; i<request.getList("trial3HandedWorkstations",10).size(); i++) {
            CompoundTag entry = request.getList("trial3HandedWorkstations",10).getCompound(i);
            BlockPos pos = BlockPos.of(entry.getLong("placedPos"));
            if (Math.abs(pos.getX()-origin.getX()) <= width+2
                    && Math.abs(pos.getZ()-origin.getZ()) <= width+2
                    && pos.getY() >= origin.getY() && pos.getY() <= origin.getY()+7) continue;
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("block"));
            if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) return false;
            String name = BuiltInRegistries.BLOCK.get(id).getName().getString();
            outside.merge(name,1,Integer::sum);
        }
        for (String part : parts) {
            boolean covered = false;
            for (var e : outside.entrySet()) {
                if (part.endsWith(" x " + e.getKey())) {
                    try { covered = Integer.parseInt(part.substring(0, part.indexOf(" x "))) <= e.getValue(); }
                    catch (NumberFormatException ignored) { return false; }
                    break;
                }
            }
            if (!covered) return false;
        }
        return true;
    }
}
