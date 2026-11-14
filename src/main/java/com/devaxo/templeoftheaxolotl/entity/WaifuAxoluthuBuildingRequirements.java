package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Functional building catalog. Read-only validation: no terrain or inventory changes. */
public final class WaifuAxoluthuBuildingRequirements {
    private WaifuAxoluthuBuildingRequirements() {}

    public record Result(boolean complete, String missing) {}

    private static final Map<String, Map<Block, Integer>> REQUIRED = Map.of(
        "workshop", Map.of(Blocks.GRINDSTONE,1, Blocks.COMPOSTER,1,
                Blocks.BREWING_STAND,1, Blocks.CRAFTING_TABLE,1),
        "infirmary", Map.of(Blocks.RED_BED,1, Blocks.CAULDRON,1,
                Blocks.BREWING_STAND,1, Blocks.FLOWER_POT,1),
        "small_house", Map.of(Blocks.RED_BED,1, Blocks.CHEST,1, Blocks.SMOKER,1),
        "medium_house", Map.of(Blocks.RED_BED,2, Blocks.FURNACE,1, Blocks.SMOKER,1),
        "large_house", Map.of(Blocks.RED_BED,4, Blocks.BARREL,2,
                Blocks.FURNACE,2, Blocks.SMOKER,2),
        "library", Map.of(Blocks.BOOKSHELF,32, Blocks.LECTERN,1),
        "smithy", Map.of(Blocks.ANVIL,1, Blocks.FURNACE,1,
                Blocks.BLAST_FURNACE,1, Blocks.SMITHING_TABLE,1),
        "town_center", Map.of(Blocks.JUKEBOX,1, Blocks.BELL,1),
        "storage", Map.of(Blocks.LECTERN,1)
    );

    public static Result inspect(ServerLevel level, String buildingType, BlockPos min, BlockPos max) {
        String type = buildingType.toLowerCase(Locale.ROOT).replace(' ', '_');
        if ("storage_hut".equals(type)) type = "storage";
        Map<Block, Integer> needed = REQUIRED.get(type);
        if (needed == null) return new Result(false, "Unknown building type: " + type);
        if (max.getX()-min.getX()>48 || max.getY()-min.getY()>32
                || max.getZ()-min.getZ()>48) return new Result(false,"Survey area too large");
        Map<Block,Integer> found = new HashMap<>();
        int beds = 0, chests = 0, doubleChests = 0;
        boolean lit = false, pottedOnSlab = false;
        for (BlockPos p : BlockPos.betweenClosed(min,max)) {
            if (!level.getChunkSource().hasChunk(p.getX()>>4,p.getZ()>>4))
                return new Result(false,"Unloaded chunks");
            BlockState state=level.getBlockState(p);
            Block block=state.getBlock();
            found.merge(block,1,Integer::sum);
            if (state.is(net.minecraft.tags.BlockTags.BEDS)
                    && state.hasProperty(net.minecraft.world.level.block.BedBlock.PART)
                    && state.getValue(net.minecraft.world.level.block.BedBlock.PART)
                       == net.minecraft.world.level.block.state.properties.BedPart.HEAD) beds++;
            if (state.is(Blocks.CHEST) && state.hasProperty(ChestBlock.TYPE)) {
                ChestType chestType=state.getValue(ChestBlock.TYPE);
                if (chestType==ChestType.SINGLE) chests++;
                if (chestType==ChestType.LEFT) {
                    Direction facing=state.getValue(ChestBlock.FACING);
                    Direction partnerDir=facing.getCounterClockWise();
                    BlockState partner=level.getBlockState(p.relative(partnerDir));
                    if (partner.is(Blocks.CHEST) && partner.getValue(ChestBlock.TYPE)==ChestType.RIGHT
                            && partner.getValue(ChestBlock.FACING)==facing) doubleChests++;
                }
            }
            if (state.is(Blocks.SEA_LANTERN)) lit=true;
            if (state.is(Blocks.WALL_TORCH) && state.hasProperty(WallTorchBlock.FACING)) lit=true;
            if (state.is(Blocks.LANTERN) && state.hasProperty(LanternBlock.HANGING)
                    && state.getValue(LanternBlock.HANGING)) lit=true;
            if (state.is(Blocks.FLOWER_POT) && level.getBlockState(p.below()).is(net.minecraft.tags.BlockTags.SLABS))
                pottedOnSlab=true;
        }
        StringBuilder missing=new StringBuilder();
        for (var entry:needed.entrySet()) {
            Block b=entry.getKey();
            int actual=b==Blocks.RED_BED?beds:b==Blocks.CHEST?chests+doubleChests:
                b==Blocks.ANVIL?found.getOrDefault(Blocks.ANVIL,0)+found.getOrDefault(Blocks.CHIPPED_ANVIL,0)+found.getOrDefault(Blocks.DAMAGED_ANVIL,0):
                found.getOrDefault(b,0);
            if (actual<entry.getValue()) append(missing,entry.getValue()-actual+" x "+b.getName().getString());
        }
        int requiredPairs="storage".equals(type)?4:"medium_house".equals(type)?1:"large_house".equals(type)?2:0;
        if (doubleChests<requiredPairs) append(missing,(requiredPairs-doubleChests)+" double chest(s)");
        if ("infirmary".equals(type)&&!pottedOnSlab) append(missing,"flower pot on slab");
        if ("town_center".equals(type)) append(missing,"Axoluthu statue + Watergate portal (special validator pending)");
        if (!lit) append(missing,"sea lantern lighting");
        if (found.getOrDefault(Blocks.OAK_DOOR, 0) < 2) append(missing,"complete oak entrance door");
        return new Result(missing.length()==0,missing.toString());
    }
    private static void append(StringBuilder out,String message) {
        if (out.length()>0) out.append(", ");
        out.append(message);
    }
}
