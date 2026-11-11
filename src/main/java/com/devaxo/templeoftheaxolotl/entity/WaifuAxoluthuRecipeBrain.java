package com.devaxo.templeoftheaxolotl.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** RecipeManager-driven crafting, ingredient resolution and project execution.
 * All changes occur on the server thread, one step per call. Does not grant items
 * for missing recipes. Workstation access and gathering remain separate actions.
 */
public final class WaifuAxoluthuRecipeBrain {
    private WaifuAxoluthuRecipeBrain() {}
    private static final int MAX_DEPTH=12;
    // One workstation per active recipe. Weak keys avoid retaining despawned entities.
    private static final Map<WaifuAxoluthuEntity, BlockPos> ACTIVE_TABLES = new WeakHashMap<>();

    private static BlockPos recipeTable(WaifuAxoluthuEntity axo, ServerLevel level) {
        // An approved Stage 5 house has a dedicated site crafting table.
        // Resolve it before the cached/nearest home table, so recipe work
        // happens at the actual construction site rather than across town.
        CompoundTag architecture = axo.getArchitecturalLearning();
        if (architecture.contains("stage5HousingProject", 10)) {
            CompoundTag house = architecture.getCompound("stage5HousingProject");
            if (house.getBoolean("siteAuthorized") && house.contains("origin")
                    && level.dimension().location().toString().equals(house.getString("dimension"))) {
                BlockPos siteTable = BlockPos.of(house.getLong("origin")).offset(-2, 0, 0);
                if (level.getChunkSource().hasChunk(siteTable.getX() >> 4, siteTable.getZ() >> 4)
                        && level.getBlockState(siteTable).is(Blocks.CRAFTING_TABLE)) {
                    ACTIVE_TABLES.put(axo, siteTable.immutable());
                    return siteTable;
                }
            }
        }
        BlockPos locked = ACTIVE_TABLES.get(axo);
        if (locked != null) {
            if (level.getChunkSource().hasChunk(locked.getX() >> 4, locked.getZ() >> 4)
                    && level.getBlockState(locked).is(Blocks.CRAFTING_TABLE)) return locked;
            ACTIVE_TABLES.remove(axo);
        }
        // Prefer the site workstation over the home workstation. Only loaded chunks.
        BlockPos origin = axo.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-16,-4,-16), origin.offset(16,4,16))) {
            if (!level.getChunkSource().hasChunk(p.getX() >> 4, p.getZ() >> 4)
                    || !level.getBlockState(p).is(Blocks.CRAFTING_TABLE)) continue;
            double distance = p.distSqr(origin);
            if (distance < bestDistance) { bestDistance = distance; best = p.immutable(); }
        }
        if (best == null) best = WaifuAxoluthuWorkshopActions.findCraftingTable(axo);
        if (best != null) ACTIVE_TABLES.put(axo,best);
        return best;
    }

    private static int count(SimpleContainer bag,Item item) {
        int total=0;
        for(int i=0;i<bag.getContainerSize();i++) if(bag.getItem(i).is(item))total+=bag.getItem(i).getCount();
        return total;
    }
    private static int room(SimpleContainer bag,ItemStack result) {
        int total=0;
        for(int i=0;i<bag.getContainerSize();i++) {
            ItemStack s=bag.getItem(i);
            if(s.isEmpty())total+=result.getMaxStackSize();
            else if(ItemStack.isSameItemSameComponents(s,result))total+=s.getMaxStackSize()-s.getCount();
        }
        return total;
    }
    private static CraftingRecipe recipe(ServerLevel level,Item target) {
        for(var holder:level.getRecipeManager().getRecipes()) {
            if(holder.value() instanceof CraftingRecipe crafting &&
                    crafting.getResultItem(level.registryAccess()).is(target))return crafting;
        }
        return null;
    }
    // Use actual registered furnace/blast-furnace recipes, including modded outputs.
    private static AbstractCookingRecipe cookingRecipe(ServerLevel level, Item target) {
        for (var holder : level.getRecipeManager().getRecipes()) {
            var value=holder.value();
            if ((value instanceof SmeltingRecipe || value instanceof BlastingRecipe)
                    && value instanceof AbstractCookingRecipe cooking
                    && cooking.getResultItem(level.registryAccess()).is(target)) return cooking;
        }
        return null;
    }
    private static Item choose(Ingredient ingredient,SimpleContainer bag,ServerLevel level,Set<Item> visited) {
        Item fallback=null;
        int best=Integer.MIN_VALUE;
        for(ItemStack option:ingredient.getItems()) {
            if(option.isEmpty())continue;
            Item item=option.getItem();
            if(visited.contains(item))continue;
            int score=count(bag,item)*100;
            if(item==Items.OAK_PLANKS || item==Items.COBBLESTONE || item==Items.IRON_INGOT)score+=15;
            if(recipe(level,item)!=null)score+=5;
            if(score>best){best=score;fallback=item;}
        }
        return fallback;
    }
    private static void remove(SimpleContainer bag,Item item,int amount) {
        for(int i=0;i<bag.getContainerSize() && amount>0;i++) {
            if(!bag.getItem(i).is(item))continue;
            int take=Math.min(amount,bag.getItem(i).getCount());
            bag.removeItem(i,take);amount-=take;
        }
    }
    private static boolean isTransmutableLog(Item item) {
        // Tag membership includes VanillaBackport and other compatible modded logs.
        return new ItemStack(item).is(net.minecraft.tags.ItemTags.LOGS);
    }
    private static String source(WaifuAxoluthuEntity axo,Item item,long lastGather) {
        SimpleContainer bag=axo.getAcquiredMaterials();
        if(isTransmutableLog(item)) return "HARVEST_"+WaifuAxoluthuForestry.step(axo,item);
        if(item==Items.GLASS) {
            return WaifuAxoluthuStage5Build.ensureGlass(axo,1,lastGather);
        }
        if(item==Items.IRON_INGOT) {
            if(count(bag,Items.RAW_IRON)==0)return source(axo,Items.RAW_IRON,lastGather);
            return "SMELTING_"+WaifuAxoluthuStage5Build.focusSmelt(axo,Items.RAW_IRON,Items.IRON_INGOT);
        }
        if(item==Items.SAND || item==Items.COBBLESTONE || item==Items.REDSTONE || item==Items.RAW_IRON ||
                item==Items.COAL || item==Items.DIAMOND ||
                item==Items.COBBLED_DEEPSLATE || item==Items.RAW_GOLD || item==Items.RAW_COPPER) {
            String station=WaifuAxoluthuDependencyBrain.ensure(axo,Blocks.GRINDSTONE,lastGather);
            if(!station.equals("READY"))return "NEED_GRINDSTONE_"+station;
            return "MINING_"+WaifuAxoluthuMagicGathering.gather(axo,item,lastGather,160).status();
        }
        if(item==Items.RED_SAND ||
                item==Items.CLAY_BALL || item==Items.NETHERRACK || item==Items.BLACKSTONE ||
                item==Items.BASALT || item==Items.END_STONE || item==Items.SOUL_SAND ||
                item==Items.CRIMSON_STEM || item==Items.WARPED_STEM) {
            String station=WaifuAxoluthuDependencyBrain.ensure(axo,Blocks.STONECUTTER,lastGather);
            if(!station.equals("READY"))return "NEED_STONECUTTER_"+station;
            return "STONECUTTER_GATHER_"+WaifuAxoluthuMagicGathering.gather(axo,item,lastGather,160).status();
        }
        if(item==Items.SUGAR_CANE || item==Items.WHEAT || item==Items.WHEAT_SEEDS ||
                item==Items.POPPY || item==Items.DANDELION) {
            String station=WaifuAxoluthuDependencyBrain.ensure(axo,Blocks.CARTOGRAPHY_TABLE,lastGather);
            if(!station.equals("READY"))return "NEED_CARTOGRAPHY_"+station;
            return "FORAGING_"+WaifuAxoluthuMagicGathering.gather(axo,item,lastGather,160).status();
        }
        return "NO_SOURCE_FOR_"+item;
    }
    /** Ensure quantity exists; resolves one recipe or resource step at a time. */
    public static String ensure(WaifuAxoluthuEntity axo,Item target,int quantity,long lastGather) {
        return ensure(axo,target,quantity,lastGather,new HashSet<>(),0);
    }
    private static String ensure(WaifuAxoluthuEntity axo,Item target,int quantity,long lastGather,
                                 Set<Item> visiting,int depth) {
        if(!(axo.level() instanceof ServerLevel level))return "SERVER_ONLY";
        SimpleContainer bag=axo.getAcquiredMaterials();
        if(count(bag,target)>=quantity){ ACTIVE_TABLES.remove(axo); return "READY"; }
        if(depth>=MAX_DEPTH || !visiting.add(target))return "RECIPE_CYCLE_"+target;
        try {
            // Resource-first policy: never reverse-craft mined/foraged materials
            // from storage blocks (e.g. redstone dust <- redstone block).
            // Those recipes are valid for crafting but poor acquisition plans.
            if (target==Items.REDSTONE || target==Items.COBBLESTONE ||
                    target==Items.RAW_IRON || target==Items.RAW_GOLD ||
                    target==Items.RAW_COPPER || target==Items.COAL ||
                    target==Items.DIAMOND || target==Items.EMERALD ||
                    target==Items.LAPIS_LAZULI || target==Items.COBBLED_DEEPSLATE ||
                    target==Items.SAND || target==Items.RED_SAND ||
                    target==Items.CLAY_BALL || target==Items.NETHERRACK || target==Items.BLACKSTONE ||
                    target==Items.BASALT || target==Items.END_STONE || target==Items.SOUL_SAND ||
                    target==Items.CRIMSON_STEM || target==Items.WARPED_STEM ||
                    isTransmutableLog(target) || target==Items.SUGAR_CANE ||
                    target==Items.WHEAT_SEEDS || target==Items.POPPY ||
                    target==Items.DANDELION) {
                return source(axo,target,lastGather);
            }
            // Cooking is an independent magical action, not a crafting-table recipe.
            // Prefer furnace/blast recipes for blocks and ingots which can be smelted.
            AbstractCookingRecipe cooking=cookingRecipe(level,target);
            if(cooking!=null) {
                ItemStack result=cooking.getResultItem(level.registryAccess()).copy();
                Item input=null;
                for(Ingredient ingredient:cooking.getIngredients()) {
                    if(ingredient.isEmpty())continue;
                    input=choose(ingredient,bag,level,visiting);
                    break;
                }
                if(input==null)return "UNRESOLVED_SMELTING_INPUT_"+target;
                if(count(bag,input)==0)
                    return "FOR_SMELTING_"+target+"_"+ensure(axo,input,1,lastGather,visiting,depth+1);
                return "MAGIC_SMELTING_"+WaifuAxoluthuStage5Build.focusSmelt(axo,input,result);
            }
            CraftingRecipe crafting=recipe(level,target);
            if(crafting==null)return source(axo,target,lastGather);
            ItemStack output=crafting.getResultItem(level.registryAccess()).copy();
            if(output.isEmpty())return "EMPTY_RECIPE_"+target;
            int batches=(quantity-count(bag,target)+output.getCount()-1)/output.getCount();
            List<Item> selected=new ArrayList<>();
            for(Ingredient ingredient:crafting.getIngredients()) {
                if(ingredient.isEmpty())continue;
                // Preserve spruce throughout the roofing recipe chain.
                Item option=target==Items.SPRUCE_PLANKS && ingredient.test(new ItemStack(Items.SPRUCE_LOG))
                        && !visiting.contains(Items.SPRUCE_LOG) ? Items.SPRUCE_LOG
                        : choose(ingredient,bag,level,visiting);
                if(option==null)return "UNRESOLVED_INGREDIENT_"+target;
                selected.add(option);
            }
            for(Item option:new HashSet<>(selected)) {
                int required=0;
                for(Item chosen:selected)if(chosen==option)required+=batches;
                if(count(bag,option)<required) {
                    return "FOR_"+target+"_"+ensure(axo,option,required,lastGather,visiting,depth+1);
                }
            }
            // All normal recipes use the established home workstation, even 2x2.
            // Only the initial crafting-table bootstrap may be made without one.
            BlockPos table=recipeTable(axo,level);
            boolean bootstrap=target==Items.CRAFTING_TABLE && table==null;
            if(!bootstrap) {
                if(table==null) {
                    if(selected.size()>4) return "NEED_CRAFTING_TABLE_"+
                            WaifuAxoluthuDependencyBrain.ensure(axo,Blocks.CRAFTING_TABLE,lastGather);
                    // Don't fabricate a workstation: the existing dependency brain
                    // handles the initial table placement.
                    return "NEED_CRAFTING_TABLE_"+
                            WaifuAxoluthuDependencyBrain.ensure(axo,Blocks.CRAFTING_TABLE,lastGather);
                }
                if(table.distSqr(axo.blockPosition())>16) {
                    // Returning from forestry may be many chunks away. Navigate via
                    // loaded short waypoints before attempting the table's adjacent tile.
                    if(table.distSqr(axo.blockPosition())>144) {
                        axo.navigateToSavedLocation(table,1.15);
                        return "RETURNING_HOME_TO_CRAFT";
                    }
                    if(axo.getNavigation().isDone())
                        WaifuAxoluthuWorkshopActions.approachCraftingTable(axo,table);
                    return "APPROACH_CRAFTING_TABLE";
                }
            }
            ItemStack made=output.copyWithCount(output.getCount()*batches);
            if(room(bag,made)<made.getCount())return "INVENTORY_FULL";
            for(Item option:new HashSet<>(selected)) {
                int required=0;
                for(Item chosen:selected)if(chosen==option)required+=batches;
                remove(bag,option,required);
            }
            ItemStack remainder=bag.addItem(made);
            if(!remainder.isEmpty())return "INVENTORY_FULL_AFTER_CRAFT";
            ACTIVE_TABLES.remove(axo);
            axo.recordAcquisition("RECIPE_MANAGER_CRAFT "+target+" count="+made.getCount());
            // Only a successful batch teaches the recipe. Remember before speaking so
            // speech cooldowns and future builds never reset the learned status.
            boolean firstCraft = !axo.hasCraftedBefore(target);
            axo.rememberCrafted(target);
            String itemName = made.getHoverName().getString();
            if (firstCraft) {
                axo.speak("recipe_discovered_"+target,
                        "i figured how to make "+itemName+" :D!");
            } else {
                axo.speak("recipe_repeat_"+target,
                        "Here's some more of these "+itemName+"! :3");
            }
            return "CRAFTED_"+target;
        } finally { visiting.remove(target); }
    }
}
