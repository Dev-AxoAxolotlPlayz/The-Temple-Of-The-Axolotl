package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import java.util.*;

/** Stage 5.4h/5.5: persisted, server-side material preparation and phased construction. */
public final class WaifuAxoluthuStage5Build {
    private WaifuAxoluthuStage5Build() {}
    private static final String KEY="stage5HousingProject";
    private static final String[] PHASES={"BUILDING_FLOOR","BUILDING_WALLS","BUILDING_ROOF","BUILDING_WINDOWS","BUILDING_INTERIOR"};
    private static int category(CompoundTag e, BlockPos origin) {
        String id=e.getString("block"); BlockPos pos=BlockPos.of(e.getLong("pos"));
        if(pos.getY()==origin.getY())return 0;
        if(id.contains("glass")||id.contains("door"))return 3;
        if(pos.getY()>=origin.getY()+6)return 2;
        return 1;
    }
    private static int held(WaifuAxoluthuEntity axo, Item item) {
        int n=0; var bag=axo.getAcquiredMaterials();
        for(int i=0;i<bag.getContainerSize();i++)if(bag.getItem(i).is(item))n+=bag.getItem(i).getCount();
        return n;
    }
    private static boolean consume(WaifuAxoluthuEntity axo,Item item) {
        var bag=axo.getAcquiredMaterials();
        for(int i=0;i<bag.getContainerSize();i++) {
            ItemStack stack=bag.getItem(i);
            if(stack.is(item)&&!stack.isEmpty()) {stack.shrink(1);bag.setChanged();return true;}
        }
        return false;
    }
    // The construction chest is an access point, never a second inventory.
    private static net.minecraft.server.level.ServerPlayer owner(WaifuAxoluthuEntity axo, ServerLevel level) {
        return axo.getBondedPlayer()==null?null:level.getServer().getPlayerList().getPlayer(axo.getBondedPlayer());
    }
    private static int warehouseCount(WaifuAxoluthuEntity axo, ServerLevel level,Item item) {
        var player=owner(axo,level);if(player==null)return 0;
        int count=0;
        for(int page=0;page<com.devaxo.templeoftheaxolotl.repository.CelestialRepository.pages(player);page++)
            for(var e:com.devaxo.templeoftheaxolotl.repository.CelestialRepository.readPage(player,page))
                if(e.sample().is(item))count+=e.quantity();
        return count;
    }
    private static int occupied(WaifuAxoluthuEntity axo) {
        var bag=axo.getAcquiredMaterials();int n=0;
        for(int i=0;i<bag.getContainerSize();i++)if(!bag.getItem(i).isEmpty())n++;
        return n;
    }
    private static String approachSiteChest(WaifuAxoluthuEntity axo, ServerLevel level, CompoundTag p) {
        BlockPos chest=BlockPos.of(p.getLong("origin")).offset(-2,0,2);
        if(!level.isLoaded(chest))return "WAITING_FOR_SITE_CHEST_CHUNK";
        if(!level.getBlockState(chest).is(com.devaxo.templeoftheaxolotl.registry.ModBlocks.ABYSSAL_CHEST.get()))
            return "SITE_CHEST_MISSING";
        // A real workstation interaction requires her to stand next to it.
        // This deliberately does not alter the 32-block house-placement reach.
        if(axo.distanceToSqr(chest.getX()+.5,chest.getY()+.5,chest.getZ()+.5)>9.0) {
            String nav=WaifuAxoluthuBuildNavigation.approach(axo,level,chest,1.05);
            return nav==null?"APPROACHING_SITE_CHEST":nav;
        }
        return null;
    }
    private static String unloadToSite(WaifuAxoluthuEntity axo,ServerLevel level,CompoundTag p) {
        String approach=approachSiteChest(axo,level,p);
        if(approach!=null)return approach;
        var player=owner(axo,level);if(player==null)return "REPOSITORY_OWNER_OFFLINE";
        var bag=axo.getAcquiredMaterials();
        for(int i=0;i<bag.getContainerSize();i++){
            ItemStack held=bag.getItem(i);if(held.isEmpty())continue;
            int left=held.getCount();
            for(int page=0;page<com.devaxo.templeoftheaxolotl.repository.CelestialRepository.pages(player)&&left>0;page++)
                left-=com.devaxo.templeoftheaxolotl.repository.CelestialRepository.insertPage(player,page,held.copyWithCount(left));
            if(left!=held.getCount())bag.setItem(i,held.copyWithCount(left));
        }
        p.putBoolean("warehouseHasMaterials",true);
        return occupied(axo)>0?"REPOSITORY_OVERFLOW_RETAINED":"REPOSITORY_DEPOSITED";
    }
    private static String fetchFromSite(WaifuAxoluthuEntity axo,ServerLevel level,CompoundTag p,Item item,int amount) {
        String approach=approachSiteChest(axo,level,p);
        if(approach!=null)return approach;
        var player=owner(axo,level);if(player==null)return "REPOSITORY_OWNER_OFFLINE";
        if(warehouseCount(axo,level,item)==0)return "SITE_MATERIAL_NOT_STORED";
        var bag=axo.getAcquiredMaterials();int need=Math.min(64,amount);
        for(int page=0;page<com.devaxo.templeoftheaxolotl.repository.CelestialRepository.pages(player)&&need>0;page++){
            int index=0;
            while(index<com.devaxo.templeoftheaxolotl.repository.CelestialRepository.readPage(player,page).size()&&need>0){
                var entry=com.devaxo.templeoftheaxolotl.repository.CelestialRepository.readPage(player,page).get(index);
                if(!entry.sample().is(item)){index++;continue;}
                ItemStack taken=com.devaxo.templeoftheaxolotl.repository.CelestialRepository.extractPage(player,page,index,need);
                ItemStack remainder=bag.addItem(taken);
                if(!remainder.isEmpty()){
                    int restored=com.devaxo.templeoftheaxolotl.repository.CelestialRepository.insertPage(player,page,remainder);
                    if(restored<remainder.getCount()){
                        // Fail-safe: preserve overflow with Axoluthu instead of discarding it.
                        axo.spawnAtLocation(remainder.copyWithCount(remainder.getCount()-restored));
                    }
                }
                need-=taken.getCount()-remainder.getCount();
                if(!remainder.isEmpty()||taken.isEmpty())break;
            }
        }
        return held(axo,item)>0?"REPOSITORY_MATERIAL_FETCHED":"SITE_INVENTORY_FULL";
    }
    /** Initial construction logistics: obtain five logs, craft the site kit and place it. */
    private static String prepareSite(WaifuAxoluthuEntity axo, ServerLevel level, CompoundTag p) {
        BlockPos origin=BlockPos.of(p.getLong("origin"));
        int width=p.getInt("width"), depth=p.getInt("depth");
        if(width<5||depth<5)return "INVALID_SITE";
        // Workstation lies outside the house footprint. Reuse an existing site kit on reload.
        BlockPos table=origin.offset(-2,0,0);
        BlockPos left=origin.offset(-2,0,2);
        BlockPos right=origin.offset(-2,0,3);
        if(!level.isLoaded(table)||!level.isLoaded(left)||!level.isLoaded(right))return "WAITING_FOR_SITE_CHUNK";
        boolean hasTable=level.getBlockState(table).is(Blocks.CRAFTING_TABLE);
        boolean hasChest=level.getBlockState(left).is(com.devaxo.templeoftheaxolotl.registry.ModBlocks.ABYSSAL_CHEST.get());
        if(!hasChest){
            if(!level.getBlockState(left).canBeReplaced())return "ABYSSAL_CHEST_SITE_BLOCKED";
            String nav=WaifuAxoluthuBuildNavigation.approach(axo,level,left,1.05);
            if(nav!=null)return nav;
            if(!axo.actionReady(WaifuAxoluthuEntity.ACTION_WORKSTATION,left,20,40))return "PREPARING_ABYSSAL_CHEST";
            if(!level.setBlock(left,com.devaxo.templeoftheaxolotl.registry.ModBlocks.ABYSSAL_CHEST.get().defaultBlockState(),3))return "ABYSSAL_CHEST_PLACEMENT_FAILED";
            if(level.getBlockEntity(left) instanceof com.devaxo.templeoftheaxolotl.block.AbyssalChestEntity chest)
                chest.setOwner(axo.getBondedPlayer());
            return "PLACED_ABYSSAL_CHEST";
        }
        if(hasTable){
            p.putBoolean("siteEquipmentReady",true);
            p.putString("constructionPhase","GATHERING_BASIC_MATERIALS");
            p.putString("notes","My crafting table and Abyssal Chest are ready! >:3");
            return "SITE_READY";
        }
        p.putString("constructionPhase","PREPARING_CONSTRUCTION_SITE");
        int planksNeeded=4;
        int planks=held(axo,Items.OAK_PLANKS);
        if(planks<planksNeeded) {
            var bag=axo.getAcquiredMaterials();
            int logs=0;
            for(int i=0;i<bag.getContainerSize();i++)if(bag.getItem(i).is(ItemTags.LOGS))logs+=bag.getItem(i).getCount();
            int requiredLogs=Math.max(0,(planksNeeded-planks+3)/4);
            if(logs<requiredLogs) {
                BlockPos hut=axo.getSettlementStorageOrigin();
                if(hut!=null) {
                    BlockPos[] chests={hut.offset(1,1,3),hut.offset(2,1,3)};
                    for(BlockPos chestPos:chests) {
                        if(!level.isLoaded(chestPos))continue;
                        if(!(level.getBlockEntity(chestPos) instanceof Container chest))continue;
                        if(axo.blockPosition().distSqr(chestPos)>9) {
                            p.putString("constructionPhase","RETRIEVING_BASIC_MATERIALS");
                            axo.getNavigation().moveTo(chestPos.getX()+.5,chestPos.getY(),chestPos.getZ()+.5,1.0);
                            return "VISITING_STORAGE_HUT";
                        }
                        for(int i=0;i<chest.getContainerSize()&&logs<requiredLogs;i++) {
                            ItemStack stack=chest.getItem(i);
                            if(!stack.is(ItemTags.LOGS))continue;
                            int take=Math.min(stack.getCount(),requiredLogs-logs);
                            ItemStack extracted=chest.removeItem(i,take);
                            ItemStack remainder=bag.addItem(extracted);
                            if(!remainder.isEmpty()) {
                                ItemStack slot=chest.getItem(i);
                                if(slot.isEmpty())chest.setItem(i,remainder);
                                else if(ItemStack.isSameItemSameComponents(slot,remainder)) {slot.grow(remainder.getCount());chest.setChanged();}
                                else axo.spawnAtLocation(remainder);
                            }
                            logs+=take-remainder.getCount();
                        }
                        if(logs>=requiredLogs)break;
                    }
                }
                if(logs<requiredLogs) {
                    p.putString("constructionPhase","RETRIEVING_BASIC_MATERIALS");
                    p.putString("notes","The hut needs more logs, so I'll collect the missing ones! >:3");
                    return "SITE_LOGS_"+WaifuAxoluthuRecipeBrain.ensure(axo,Items.OAK_LOG,requiredLogs,level.getGameTime());
                }
            }
            // One log -> four oak planks, using the materials she actually holds.
            for(int i=0;i<bag.getContainerSize();i++) {
                ItemStack stack=bag.getItem(i);
                if(!stack.is(ItemTags.LOGS))continue;
                stack.shrink(1);bag.setChanged();
                ItemStack remainder=bag.addItem(new ItemStack(Items.OAK_PLANKS,4));
                if(!remainder.isEmpty())axo.spawnAtLocation(remainder);
                p.putString("constructionPhase","CRAFTING_SITE_EQUIPMENT");
                return "CRAFTED_SITE_PLANKS";
            }
            return "WAITING_FOR_LOGS";
        }
        BlockPos target=table;
        int cost=4;
        if(!level.getBlockState(target).canBeReplaced())return "SITE_EQUIPMENT_BLOCKED_"+target.toShortString();
        String navigation=WaifuAxoluthuBuildNavigation.approach(axo,level,target,1.05);
        if(navigation!=null) {
            p.putString("notes", "Setting up crafting table at " + target.toShortString()
                    + ": " + navigation + " >:3");
            return navigation;
        }
        if(!axo.actionReady(WaifuAxoluthuEntity.ACTION_WORKSTATION,target,20,40))return "PREPARING_EQUIPMENT";
        for(int i=0;i<cost;i++)if(!consume(axo,Items.OAK_PLANKS))return "SITE_PLANKS_MISSING";
        if(!level.setBlock(target,Blocks.CRAFTING_TABLE.defaultBlockState(),3)){
            axo.getAcquiredMaterials().addItem(new ItemStack(Items.OAK_PLANKS,cost));return "SITE_PLACEMENT_FAILED";
        }
        p.putString("constructionPhase","ESTABLISHING_WORKSTATION");
        p.putString("notes","Setting up my crafting table and Abyssal Chest! >:3");
        return "PLACED_SITE_EQUIPMENT";
    }
    /** Magical, fuel-free smelting with the focused animation and a cyan poof in front of Axoluthu.
     * Raw inputs are consumed only after inventory capacity is confirmed.
     */
    public static String focusSmelt(WaifuAxoluthuEntity axo, Item input, Item output) {
        return focusSmelt(axo,input,new ItemStack(output));
    }
    public static String focusSmelt(WaifuAxoluthuEntity axo, Item input, ItemStack output) {
        if(!(axo.level() instanceof ServerLevel level))return "SERVER_ONLY";
        var bag=axo.getAcquiredMaterials();
        if(held(axo,input)<1)return "NEED_RAW_MATERIAL";
        int available=0;
        for(int i=0;i<bag.getContainerSize();i++) {
            ItemStack slot=bag.getItem(i);
            if(slot.isEmpty())available+=output.getMaxStackSize();
            else if(ItemStack.isSameItemSameComponents(slot,output))
                available+=slot.getMaxStackSize()-slot.getCount();
        }
        // The input stack may free a slot when it is consumed.
        if(available<output.getCount() && !(held(axo,input)==1 &&
                java.util.stream.IntStream.range(0,bag.getContainerSize())
                    .anyMatch(i->bag.getItem(i).is(input)&&bag.getItem(i).getCount()==1)))
            return "SMELT_INVENTORY_FULL";
        BlockPos focus=axo.blockPosition();
        if(!axo.actionReady(WaifuAxoluthuEntity.ACTION_FOCUSED,focus,30,50))return "FOCUSING_TO_SMELT";
        if(!consume(axo,input))return "NEED_RAW_MATERIAL";
        ItemStack leftover=bag.addItem(output.copy());
        if(!leftover.isEmpty()) {
            bag.addItem(new ItemStack(input));
            axo.finishAction();
            return "SMELT_INVENTORY_FULL";
        }
        var look=axo.getLookAngle();
        level.sendParticles(new DustParticleOptions(new Vector3f(0.12f,0.92f,0.95f),1.3f),
                axo.getX()+look.x*0.85,axo.getY()+axo.getBbHeight()*0.65,
                axo.getZ()+look.z*0.85,24,0.23,0.27,0.23,0.035);
        axo.recordAcquisition("MAGIC_SMELT "+input+" -> "+output);
        axo.finishAction();
        return "MAGIC_SMELTED";
    }
    /** Glass is produced from stored or gathered sand, with no furnace or fuel. */
    public static String ensureGlass(WaifuAxoluthuEntity axo, int quantity, long lastGather) {
        if(!(axo.level() instanceof ServerLevel level))return "SERVER_ONLY";
        if(held(axo,Items.GLASS)>=quantity)return "READY";
        CompoundTag p=axo.getArchitecturalLearning().getCompound(KEY);
        if(held(axo,Items.SAND)==0) {
            if(warehouseCount(axo,level,Items.SAND)>0)
                return "GLASS_"+fetchFromSite(axo,level,p,Items.SAND,Math.min(64,quantity-held(axo,Items.GLASS)));
            return "GATHER_SAND_"+WaifuAxoluthuRecipeBrain.ensure(axo,Items.SAND,Math.min(64,quantity-held(axo,Items.GLASS)),lastGather);
        }
        return focusSmelt(axo,Items.SAND,Items.GLASS);
    }
    /** Reconcile saved progress with the world after sleep/reload or an interruption.
     * Only rewinds the cursor; never removes or replaces player blocks here. */
    public static boolean reconcileProgress(WaifuAxoluthuEntity axo, ServerLevel level) {
        CompoundTag memory = axo.getArchitecturalLearning();
        if (!memory.contains(KEY, 10)) return false;
        CompoundTag project = memory.getCompound(KEY);
        if (!project.getBoolean("siteAuthorized") || !project.contains("blueprintPreview", 10)
                || !level.dimension().location().toString().equals(project.getString("dimension"))) return false;
        CompoundTag blueprint = project.getCompound("blueprintPreview");
        if (!"BUILDABLE".equals(blueprint.getString("status"))) return false;
        ListTag blocks = blueprint.getList("blocks", 10);
        if (blocks.isEmpty() || blocks.size() > 12000) return false;
        BlockPos origin = BlockPos.of(project.getLong("origin"));
        int savedPhase = project.getInt("buildPhaseIndex");
        int savedCursor = project.getInt("buildCursor");
        // Reconcile completed stages and the portion of the current stage already passed.
        // Never infer that a later-phase block should already exist.
        for (int phase = 0; phase <= Math.min(savedPhase, PHASES.length - 1); phase++) {
            int limit = phase < savedPhase ? blocks.size() : Math.min(savedCursor, blocks.size());
            for (int i = 0; i < limit; i++) {
                CompoundTag entry = blocks.getCompound(i);
                if (category(entry, origin) != phase) continue;
                BlockPos target = BlockPos.of(entry.getLong("pos"));
                if (!level.isLoaded(target)) return false;
                ResourceLocation id = ResourceLocation.tryParse(entry.getString("block"));
                if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) continue;
                Block expected = BuiltInRegistries.BLOCK.get(id);
                BlockState actual = level.getBlockState(target);
                // Revisit missing blocks only; never overwrite unrelated solid player builds.
                if (actual.is(expected) || (!actual.isAir() && !actual.canBeReplaced())) continue;
                project.putInt("buildPhaseIndex", phase);
                project.putInt("buildCursor", i);
                project.putString("constructionPhase", PHASES[phase]);
                project.putString("notes", "I found an unfinished part of my house! Back to work! >:3");
                return true;
            }
        }
        return false;
    }

    public static String step(WaifuAxoluthuEntity axo, ServerLevel level, long lastGather) {
        CompoundTag memory=axo.getArchitecturalLearning();
        if(!memory.contains(KEY,10))return "NO_PROJECT";
        CompoundTag p=memory.getCompound(KEY);
        if(!p.getBoolean("siteAuthorized"))return "AWAITING_APPROVAL";
        if(!level.dimension().location().toString().equals(p.getString("dimension")))return "OTHER_DIMENSION";
        if(!p.contains("blueprintPreview",10))return "NEEDS_BLUEPRINT";
        CompoundTag bp=p.getCompound("blueprintPreview");
        if(!"BUILDABLE".equals(bp.getString("status")))return "NEEDS_BUILDABLE_BLUEPRINT";
        ListTag blocks=bp.getList("blocks",10);
        if(blocks.isEmpty()||blocks.size()>12000)return "INVALID_BLUEPRINT";
        BlockPos origin=BlockPos.of(p.getLong("origin"));
        // Periodic recovery also covers interrupted saves without a recorded sleep transition.
        if (level.getGameTime() % 100 == 0 && p.getBoolean("siteEquipmentReady"))
            reconcileProgress(axo, level);
        // Site preparation now needs only the crafting table and double chest.
        if(!p.getBoolean("siteEquipmentReady")) {
            String setup=prepareSite(axo,level,p);
            if(!"SITE_READY".equals(setup))return setup;
        }
        if(!p.getBoolean("materialsCalculated")) {
            p.putBoolean("materialsCalculated",true);
            p.putString("constructionPhase","GATHERING_BASIC_MATERIALS");
            p.putString("notes","I have my complete materials list! Time to collect supplies! >:3");
        }
        // When her 36-slot bag is almost full, empty it into her site double chest.
        // The saved manifest counts both the bag and the site warehouse.
        if(occupied(axo)>=32) {
            String deposit=unloadToSite(axo,level,p);
            p.putString("notes","Unloading my backpack at the construction site: "+deposit);
            return deposit;
        }
        // Acquire the complete manifest before any structural placement.
        if(!p.getBoolean("suppliesReady")) {
            CompoundTag manifest=bp.getCompound("materials");
            ArrayList<String> keys=new ArrayList<>(manifest.getAllKeys());Collections.sort(keys);
            // Persist a round-robin cursor and per-material cooldowns. A missing
            // workstation must not monopolize the entire house project.
            CompoundTag blocked=p.getCompound("materialRetryAfter");
            CompoundTag attempts=p.getCompound("materialAttemptSince");
            long now=level.getGameTime();

            int start=Math.floorMod(p.getInt("materialQueueCursor"),Math.max(1,keys.size()));
            boolean missing=false;
            long earliest=Long.MAX_VALUE;
            for(int offset=0;offset<keys.size();offset++) {
                int index=(start+offset)%keys.size();
                String key=keys.get(index);
                Item item=BuiltInRegistries.ITEM.get(ResourceLocation.parse(key));
                int needed=manifest.getInt(key);
                int have=held(axo,item)+warehouseCount(axo,level,item);
                if(have>=needed) {
                    blocked.remove(key);attempts.remove(key);
                    continue;
                }
                missing=true;
                long retry=blocked.getLong(key);
                if(retry>now) {earliest=Math.min(earliest,retry);continue;}
                if(retry>0)blocked.remove(key);
                long since=attempts.getLong(key);
                if(since==0) {since=now;attempts.putLong(key,since);}
                // Make manageable batches rather than trying to fit an entire roof at once.
                int bagTarget=Math.max(1,Math.min(16,needed-warehouseCount(axo,level,item)));
                String outcome=item==Items.GLASS ? ensureGlass(axo,bagTarget,lastGather)
                        : WaifuAxoluthuRecipeBrain.ensure(axo,item,bagTarget,lastGather);
                int after=held(axo,item)+warehouseCount(axo,level,item);
                // A completed small batch must be banked before requesting the next one.
                // Otherwise ensure() sees the same 16 items forever and returns READY,
                // while the manifest still needs hundreds of stairs.
                if ("READY".equals(outcome) && after<needed && held(axo,item)>0) {
                    String deposit=unloadToSite(axo,level,p);
                    attempts.putLong(key,now);
                    p.putString("materialCurrent",key);
                    p.putString("constructionPhase","GATHERING_BASIC_MATERIALS");
                    p.putString("notes","Banking completed batches of "+key+" ("+after+"/"+needed+"): "+deposit);
                    p.put("materialRetryAfter",blocked);
                    p.put("materialAttemptSince",attempts);
                    return "SUPPLIES_BANKING_"+deposit;
                }
                // Any tangible progress resets the start timeout.
                // Spruce logs and planks count as intermediate progress toward roof stairs.
                int intermediate=held(axo,Items.SPRUCE_LOG)+held(axo,Items.SPRUCE_PLANKS);
                String progressKey="materialIntermediate_"+key;
                int previousIntermediate=p.contains(progressKey)?p.getInt(progressKey):intermediate;
                if(after>have || (item==Items.SPRUCE_STAIRS && intermediate!=previousIntermediate))
                    attempts.putLong(key,now);
                p.putInt(progressKey,intermediate);
                boolean waiting=outcome.contains("WAITING_FOR_HOME_GRINDSTONE")
                        || outcome.contains("BLOCKED_") || outcome.contains("NO_SOURCE_FOR_")
                        || outcome.contains("RECIPE_CYCLE_") || outcome.contains("INVENTORY_FULL");
                boolean activelyWorking=outcome.contains("HARVEST_") || outcome.contains("RETURNING_HOME")
                        || outcome.contains("APPROACH_") || outcome.contains("CRAFTED_")
                        || outcome.contains("MOVING_") || outcome.contains("NAVIGAT")
                        || !axo.getNavigation().isDone();
                if(waiting || (after==have && now-since>=300 && !activelyWorking)) {
                    blocked.putLong(key,now+1200);
                    attempts.remove(key);
                    p.putInt("materialQueueCursor",(index+1)%keys.size());
                    p.putString("constructionPhase","GATHERING_BASIC_MATERIALS");
                    p.putString("notes","I'll try something else while "+key+" is blocked: "+outcome+" QwQ");
                    p.putString("materialCurrent",key);
                    p.putString("materialLastBlocked",key+" : "+outcome);
                    p.put("materialRetryAfter",blocked);
                    p.put("materialAttemptSince",attempts);
                    return "SUPPLIES_SKIPPED_"+outcome;
                }
                p.putInt("materialQueueCursor",index);
                p.putString("materialCurrent",key);
                p.putString("constructionPhase",outcome.contains("CRAFT")?"CRAFTING_NECESSARY_CRAFTS":"GATHERING_BASIC_MATERIALS");
                p.putString("notes","Collecting "+key+" ("+after+"/"+needed+"): "+outcome);
                p.put("materialRetryAfter",blocked);
                p.put("materialAttemptSince",attempts);
                return "SUPPLIES_"+outcome;
            }
            p.put("materialRetryAfter",blocked);
            p.put("materialAttemptSince",attempts);
            if(missing) {
                p.putString("constructionPhase","WAITING_FOR_MATERIAL_RETRY");
                p.putString("notes","Some materials are cooling down; I'll retry them shortly. >:3");
                return "SUPPLIES_RETRY_COOLDOWN_"+(earliest==Long.MAX_VALUE?0:Math.max(0,earliest-now));
            }
            p.putBoolean("suppliesReady",true);
            p.putString("notes","Everything is ready! Now I can build my house! :D");
        }
        // Resume exactly where the entity last saved its progress.
        int phase=p.getInt("buildPhaseIndex");
        int cursor=p.getInt("buildCursor");
        for(;phase<5;phase++) {
            p.putString("constructionPhase",PHASES[phase]);
            for(;cursor<blocks.size();cursor++) {
                CompoundTag e=blocks.getCompound(cursor);
                if(category(e,origin)!=phase)continue;
                BlockPos pos=BlockPos.of(e.getLong("pos"));
                if(!level.isLoaded(pos))return "WAITING_FOR_CHUNK";
                Block block=BuiltInRegistries.BLOCK.get(ResourceLocation.parse(e.getString("block")));
                BlockState state=block.defaultBlockState();
                if(block instanceof StairBlock) {
                    // Opposing stair slopes; direction selected by the roof ridge.
                    state=state.setValue(StairBlock.FACING,pos.getX()-origin.getX()<p.getInt("width")/2?Direction.EAST:Direction.WEST);
                }
                if(block instanceof DoorBlock)state=state.setValue(DoorBlock.FACING,Direction.NORTH).setValue(DoorBlock.HALF,DoubleBlockHalf.LOWER);
                BlockState existing=level.getBlockState(pos);
                if(existing.is(state.getBlock())) {cursor++;p.putInt("buildCursor",cursor);continue;}
                if(!existing.isAir()&&!existing.canBeReplaced())return "BLOCKED_"+pos.toShortString();
                // Celestial construction reach: blueprint placement does not require
                // scaffolding or a path to every individual wall/roof block.
                // Only the active Stage 5 blueprint loop receives this exception;
                // crafting, gathering, storage, and other interactions remain unchanged.
                // Beyond 32 blocks she must still approach the build site normally.
                double buildReachSqr = 32.0 * 32.0;
                if(axo.distanceToSqr(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5) > buildReachSqr) {
                    String navigation=WaifuAxoluthuBuildNavigation.approach(axo,level,pos,16.0);
                    if(navigation!=null) {
                        p.putString("notes", "Moving into building reach for step " + (cursor+1) + "/" + blocks.size()
                                + " at " + pos.toShortString() + ": " + navigation + " >:3");
                        return navigation;
                    }
                    // Re-evaluate range on the next tick; never place beyond the cap.
                    return "APPROACHING_CONSTRUCTION_RANGE";
                }
                if(axo.getBoundingBox().intersects(new AABB(pos)))return WaifuAxoluthuBuildNavigation.clearSelf(axo,level,pos);
                if(held(axo,block.asItem())==0) {
                    String fetch=fetchFromSite(axo,level,p,block.asItem(),64);
                    if(!"SITE_MATERIAL_FETCHED".equals(fetch)) {
                        if("SITE_MATERIAL_NOT_STORED".equals(fetch)) {
                            p.putBoolean("suppliesReady",false);
                            p.putString("notes","Need another "+e.getString("block")+" before I continue! QwQ");
                        }
                        return fetch;
                    }
                    return "SITE_MATERIAL_FETCHED";
                }
                // The construction-only angry poke runs at 2x speed: 2.05 seconds / 2 = 20.5 ticks.
                // Begin it only after range, collision and material checks have passed.
                // Each successful blueprint placement gets its own accelerated poke.
                if(!axo.actionReady(WaifuAxoluthuEntity.ACTION_CONSTRUCTION_POKE,pos,21,23)) {
                    p.putString("notes","Poking wall/build block at "+pos.toShortString()+" (step "+(cursor+1)+"/"+blocks.size()+") >:3");
                    return "FOCUSING_TO_PLACE";
                }
                if(!consume(axo,block.asItem()))return "MATERIAL_CONSUME_RETRY";
                if(!level.setBlock(pos,state,3)) {axo.getAcquiredMaterials().addItem(new ItemStack(block.asItem()));axo.finishAction();return "PLACEMENT_FAILED";}
                axo.finishAction();
                if(block instanceof DoorBlock)level.setBlock(pos.above(),state.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),3);
                cursor++;p.putInt("buildCursor",cursor);
                p.putString("notes","Building "+PHASES[phase].toLowerCase().replace('_',' ')+" (step "+cursor+"/"+blocks.size()+") >:3");
                return "PLACED_"+cursor;
            }
            cursor=0;p.putInt("buildCursor",0);p.putInt("buildPhaseIndex",phase+1);
        }
        p.putString("constructionPhase","COMPLETE");p.putString("phase","HOUSE_COMPLETE");
        p.putString("notes","MY HOUSE IS DONE!!! Come look! :D");
        return "COMPLETE";
    }
}
