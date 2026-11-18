package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import com.devaxo.templeoftheaxolotl.repository.CelestialRepository;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.HashSet;
import java.util.Set;

/** Exact-location workshop builder; requires explicit owner design approval. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class WaifuAxoluthuExactSiteBuilder {
    private static final String KEY = "phase35OwnerSuggestion";
    private WaifuAxoluthuExactSiteBuilder() {}

    public static boolean validWorkshop(CompoundTag request) {
        if (!request.contains("blueprintPreview", 10)) return false;
        CompoundTag preview=request.getCompound("blueprintPreview");
        String type=WaifuAxoluthuVillageStructures.normalize(request.getString("type"));
        if (type.isEmpty() || preview.getInt("schema")!=22
                || !type.equals(preview.getString("type"))) return false;
        ListTag blocks=preview.getList("blocks",10);
        if(blocks.size()<100 || blocks.size()>4000) return false;
        Set<Long> unique=new HashSet<>();
        BlockPos origin=BlockPos.of(request.getLong("target"));
        for(int i=0;i<blocks.size();i++) {
            CompoundTag block=blocks.getCompound(i);
            BlockPos p=BlockPos.of(block.getLong("pos"));
            if(!WaifuAxoluthuPalettePlacement.allowed(block.getString("block"))
                    || Math.abs(p.getX()-origin.getX())>22
                    || Math.abs(p.getZ()-origin.getZ())>22
                    || p.getY()<origin.getY() || p.getY()>origin.getY()+10
                    || !unique.add(p.asLong())) return false;
        }
        return true;
    }

    /** Human-readable reason for rejected blueprint; no world mutations. */
    public static String validationIssue(CompoundTag request) {
        if (!request.contains("blueprintPreview", 10)) return "Missing blueprint preview";
        CompoundTag preview = request.getCompound("blueprintPreview");
        String type = WaifuAxoluthuVillageStructures.normalize(request.getString("type"));
        if (type.isEmpty()) return "Unknown building type";
        if (preview.getInt("schema") != 22) return "Unsupported blueprint schema: " + preview.getInt("schema");
        if (!type.equals(preview.getString("type"))) return "Blueprint building type mismatch";
        ListTag blocks = preview.getList("blocks", 10);
        if (blocks.size() < 100 || blocks.size() > 4000)
            return "Blueprint block count outside builder limit: " + blocks.size();
        Set<Long> unique = new HashSet<>();
        BlockPos origin = BlockPos.of(request.getLong("target"));
        for (int i=0;i<blocks.size();i++) {
            CompoundTag entry=blocks.getCompound(i);
            BlockPos pos=BlockPos.of(entry.getLong("pos"));
            String id=entry.getString("block");
            if (!WaifuAxoluthuPalettePlacement.allowed(id)) return "Unsupported block: " + id;
            if (Math.abs(pos.getX()-origin.getX())>22 || Math.abs(pos.getZ()-origin.getZ())>22
                    || pos.getY()<origin.getY() || pos.getY()>origin.getY()+10)
                return "Blueprint outside builder safety bounds at " + pos.toShortString();
            if (!unique.add(pos.asLong())) return "Duplicate block at " + pos.toShortString();
        }
        return "Blueprint passed validation";
    }

    @SubscribeEvent
    public static void onOwnerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) return;
        ServerLevel level = player.serverLevel();
        for (WaifuAxoluthuEntity axo : level.getEntitiesOfClass(
                WaifuAxoluthuEntity.class, player.getBoundingBox().inflate(48),
                a -> player.getUUID().equals(a.getBondedPlayer()))) {
            // This scheduler runs on the OWNER tick, independently of Axothulu's
            // own AI tick. At night, her FREE-mode bedtime/return-home routine
            // owns navigation. Never issue build approach/placement commands
            // concurrently or the two pathfinders will repeatedly override
            // one another (APPROACH_BUILD oscillation). The persisted request,
            // cursor, materials and approval are untouched until morning.
            if (axo.getMovementMode() == WaifuAxoluthuEntity.MovementMode.FREE
                    && !level.isDay()) {
                continue;
            }
            if (axo.isSleeping()) {
                continue;
            }
            CompoundTag memory = axo.getArchitecturalLearning();
            CompoundTag request = memory.getCompound(KEY);
            // Saved previews may predate the approved-material rule.
            // Only known glass/window variants receive a safe vanilla replacement.
            if (request.contains("blueprintPreview", 10) && !request.getBoolean("trial3BlueprintNormalized")) {
                ListTag entries = request.getCompound("blueprintPreview").getList("blocks", 10);
                boolean changed = false;
                for (int i = 0; i < entries.size(); i++) {
                    CompoundTag entry = entries.getCompound(i);
                    String oldId = entry.getString("block");
                    String newId = WaifuAxoluthuMaterialPolicy.normalizeBlueprintBlock(oldId);
                    if (!oldId.equals(newId)) {
                        entry.putString("block", newId);
                        changed = true;
                    }
                }
                if (changed) {
                    request.remove("supplyBatchItem");
                    request.remove("supplyBatchTarget");
                    request.remove("buildWaitReason");
                    memory.put(KEY, request);

                }
                request.putBoolean("trial3BlueprintNormalized", true);
                memory.put(KEY, request);
            }
            // Old BLOCKED_AT states remain paused until explicit owner action;
            // auto-recovery here could reawaken stale work every owner tick.
            if (false && request.getBoolean("pending") && request.getBoolean("designApproved")
                    && "BUILD_PAUSED".equals(request.getString("phase"))
                    && (request.getString("pauseReason").startsWith("BLOCKED_AT_")
                        || "MISSING_SUPPORT".equals(request.getString("pauseReason"))
                        || "NEED_SUPPORT".equals(request.getString("pauseReason")))
                    && request.hasUUID("owner") && player.getUUID().equals(request.getUUID("owner"))
                    && validWorkshop(request)) {
                request.putBoolean("constructionAllowed", true);
                request.putString("phase", "APPROVED_FOR_CONSTRUCTION");
                request.remove("pauseReason");
                request.remove("buildWaitReason");
                request.remove("buildWaitNoticeTick");
                memory.put(KEY, request);
                player.sendSystemMessage(Component.literal("[Axoluthu] Approved plot restored! Clearing the old BLOCKED_AT pause. :D"));
            }
            // Leave legacy migration and pause recovery available, but only
            // let the active BUILD gear enter the expensive placement pipeline.
            if (WaifuAxoluthuTransmission.gear(axo) != WaifuAxoluthuTransmission.Gear.BUILD)
                continue;
            if (!request.getBoolean("pending") || !request.getBoolean("designApproved")
                    || !request.getBoolean("constructionAllowed")
                    || (!"APPROVED_FOR_CONSTRUCTION".equals(request.getString("phase"))
                        && !"INSTALLING_FURNISHINGS".equals(request.getString("phase"))
                        && !"AWAITING_INTERIOR_APPROVAL".equals(request.getString("phase"))
                        && !"AWAITING_LIGHTING_APPROVAL".equals(request.getString("phase"))
                        && !"AWAITING_STRUCTURE_INSPECTION".equals(request.getString("phase"))
                        && !"AWAITING_FURNISHING_INSPECTION".equals(request.getString("phase")))
                    || !request.hasUUID("owner") || !player.getUUID().equals(request.getUUID("owner"))
                    || !level.dimension().location().toString().equals(request.getString("dimension")))
                continue;
            if (!validWorkshop(request)) {
                pause(request, memory, "INVALID_BLUEPRINT", player);
                continue;
            }
            // Celestial construction uses blueprint blocks directly. No temporary
            // crafting kit, recipe lookup, workstation search or material batches.
            // Previously conjured V2e equipment is cleaned at final inspection.
            if (request.contains("supplyBatchItem") || request.contains("supplyBatchTarget")) {
                request.remove("supplyBatchItem");
                request.remove("supplyBatchTarget");
                request.remove("supplyBatchStarted");
                memory.put(KEY, request);
            }
            // Explicit approval is required between phases; never move or place while waiting.
            if ("AWAITING_INTERIOR_APPROVAL".equals(request.getString("phase"))
                    || "AWAITING_LIGHTING_APPROVAL".equals(request.getString("phase"))
                    || "AWAITING_STRUCTURE_INSPECTION".equals(request.getString("phase"))
                    || "AWAITING_FURNISHING_INSPECTION".equals(request.getString("phase"))) continue;
            if ("INSTALLING_FURNISHINGS".equals(request.getString("phase"))) {
                String type = WaifuAxoluthuVillageStructures.normalize(request.getString("type"));
                String result = WaifuAxoluthuUniversalFurnishing.step(axo, level, request);
                if ("COMPLETE".equals(result) || "WAIT_PLAYER_WORKSTATIONS".equals(result)) {
                    request.putString("phase", "AWAITING_FURNISHING_INSPECTION");
                    request.putBoolean("axoAwaitingFinalReview", true);
                    player.sendSystemMessage(Component.literal(
                            "[Axoluthu] I gave you the workstations! Decorate however you like, then approve my furnishing inspection! ^u^"));
                    WaifuAxoluthuBuildPhaseReview.showControls(player, "furnishing");
                } else if (result.startsWith("PAUSE:")) {
                    pause(request, memory, result.substring(6), player);
                }
                memory.put(KEY, request);
                continue;
            }
            ListTag blocks = request.getCompound("blueprintPreview").getList("blocks", 10);
            int cursor = request.getInt("buildCursor");
            // Never rewrite an approved blueprint or reset its cursor mid-build.
            // Reordering saved instructions can cause already completed blocks to be
            // treated as fresh obstructions and demolished. New blueprints should be
            // canonicalized before approval, not during execution.
            // Trial 3 V1: classify once, then execute cached shell/interior/light ranges.
            // Existing approved blueprints retain their block states and coordinates.
            if (!request.getBoolean("trial3PhaseSorted")) {
                sortConstructionPhases(blocks, cursor, request);
                request.putBoolean("trial3PhaseSorted", true);
                memory.put(KEY, request);
            }
            int shellEnd = request.getInt("trial3ShellEnd");
            int interiorEnd = request.getInt("trial3InteriorEnd");
            // Player inspection replaces destructive phase-by-phase verification.
            // These historical phase markers remain for saved blueprint compatibility,
            // but neither one rewinds the cursor or demands an exact blockstate.
            if (cursor >= shellEnd) request.putBoolean("trial3ShellDone", true);
            if (cursor >= interiorEnd) request.putBoolean("trial3InteriorDone", true);
            if (cursor >= blocks.size()) {
                // Stop here. The player owns all corrections; never rewind for repair.
                request.putBoolean("axoRepairPass", false);
                request.putString("phase", "AWAITING_STRUCTURE_INSPECTION");
                memory.put(KEY, request);
                player.sendSystemMessage(Component.literal(
                        "[Axoluthu] The building is ready! Check it, change anything you want, then approve! :D"));
                WaifuAxoluthuBuildPhaseReview.showControls(player, "structure");
                continue;
            }
            BlockPos pos = BlockPos.of(blocks.getCompound(cursor).getLong("pos"));
            if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()
                    || !level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                pause(request, memory, "UNLOADED_OR_OUT_OF_BOUNDS", player);
                continue;
            }
            CompoundTag entry = blocks.getCompound(cursor);
            BlockState desired = WaifuAxoluthuPalettePlacement.state(entry, request.getInt("quarterTurns"));
            if (desired == null) { pause(request, memory, "INVALID_BLOCKSTATE", player); continue; }
            BlockState existing = level.getBlockState(pos);
            if (sameBlueprintBlock(existing, desired)) {
                request.putInt("buildCursor", cursor + 1);
                memory.put(KEY, request);
                continue;
            }
            // Never dismantle a lantern's supporting wall/fence during an
            // ordinary build or repair. A standing lantern depends on this block;
            // breaking it drops the lantern as an item and can create an endless
            // build/verify/harvest loop. Stop and log the conflicting instruction.
            if (!existing.isAir()
                    && (existing.getBlock() instanceof net.minecraft.world.level.block.WallBlock
                        || existing.getBlock() instanceof net.minecraft.world.level.block.FenceBlock)
                    && level.getBlockState(pos.above()).is(Blocks.LANTERN)) {
                org.slf4j.LoggerFactory.getLogger(WaifuAxoluthuExactSiteBuilder.class)
                        .warn("[Axoluthu/Build] Protected lantern support at {} phase={} cursor={} existing={} desired={}",
                                pos, request.getString("phase"), cursor, existing, desired);
                pause(request, memory, "PROTECTED_LANTERN_SUPPORT_AT_" + pos.toShortString(), player);
                continue;
            }
            // Safety guard: during a repair rewind, never demolish a non-air block
            // that disagrees with the blueprint. Preserve the world and report the
            // exact conflict for diagnosis instead of creating a demolition loop.
            // This is deliberately conservative until the conflicting order is fixed.
            if (!existing.isAir() && request.getBoolean("axoRepairPass")) {
                org.slf4j.LoggerFactory.getLogger(WaifuAxoluthuExactSiteBuilder.class)
                        .warn("[Axoluthu/Build] Repair conflict at {} phase={} cursor={} existing={} desired={}; refusing destructive replacement",
                                pos, request.getString("phase"), cursor, existing, desired);
                pause(request, memory, "REPAIR_CONFLICT_AT_" + pos.toShortString(), player);
                continue;
            }
            // The owner-approved, validated blueprint is the plot permit.
            // Clear obstructing blocks at exact blueprint coordinates, without expanding the plot.
            if (!existing.isAir() && request.getBoolean("axoRepairPass")) {
                pause(request, memory, "NONDESTRUCTIVE_REPAIR_AT_" + pos.toShortString(), player);
                continue;
            }
            if (!existing.isAir()) {
                if (existing.getDestroySpeed(level, pos) < 0) {
                    waiting(request, memory, player, level, "UNBREAKABLE_AT_" + pos.toShortString());
                    continue;
                }
                if (!clearObstruction(axo, player, level, pos, existing)) {
                    waiting(request, memory, player, level, "CLEARANCE_STORAGE_FULL_AT_" + pos.toShortString());
                    continue;
                }
                waiting(request, memory, player, level, "PLOT_CLEARED_AT_" + pos.toShortString());
                continue;
            }
            // Do not place blocks through villagers or owned companions.
            if (clearanceEntity(axo, level, pos, request, memory, player)) continue;
            if (pos.getY() == BlockPos.of(request.getLong("target")).getY()
                    && !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(),
                            net.minecraft.core.Direction.UP)) {
                pause(request, memory, "UNSAFE_FOUNDATION", player);
                continue;
            }
            // All approved building materials are conjured in place, one block
            // per existing scheduled construction action. This never consumes
            // the companion's acquired materials or the owner's Repository.
            // Never silently stall when standing inside the next block.
            // Move out BEFORE approaching; otherwise the approach can succeed
            // while the self-intersection check skips placement indefinitely.
            String clearance = WaifuAxoluthuBuildNavigation.clearSelf(axo, level, pos);
            if (clearance != null) {
                waiting(request, memory, player, level, clearance);
                continue;
            }
            String approach = WaifuAxoluthuBuildNavigation.approach(axo, level, pos, 1.05);
            if (approach != null) {
                waiting(request, memory, player, level, approach);
                continue;
            }
            if (!axo.actionReady(WaifuAxoluthuEntity.ACTION_FOCUSED, pos, 41, 80)) {
                waiting(request, memory, player, level, "WAITING_FOR_PLACEMENT_ACTION");
                continue;
            }
            // Vanilla placement checks are not an absolute blocker for an owner-approved
            // blueprint. Equivalent to a server-side /setblock at this exact position.
            // We do not call canSurvive here: unsupported wall-mounted lighting must
            // not permanently pause the builder.
            if (!level.setBlock(pos, desired, 3)) {
                waiting(request, memory, player, level, "SETBLOCK_RETRY_AT_" + pos.toShortString());
                continue;
            }
            axo.finishAction();
            WaifuAxoluthuBuildNavigation.completed(axo);
            request.putInt("buildCursor", cursor + 1);
            request.putString("buildWaitReason", "PLACING_BLOCKS");
            memory.put(KEY, request);
        }
    }

    /**
     * Full approved-plot verification. Return the first missing/wrong block,
     * or -1 only when every expected coordinate matches its blueprint.
     * Fence and wall states are auto-recomputed from neighbors; verify their
     * block identity only, not their connection or waterlogged properties.
     */
    private static int firstMismatch(ServerLevel level, ListTag blocks, int rotation) {
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag entry = blocks.getCompound(i);
            BlockPos pos = BlockPos.of(entry.getLong("pos"));
            if (pos.getY() < level.getMinBuildHeight()
                    || pos.getY() >= level.getMaxBuildHeight()
                    || !level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4))
                return i;
            BlockState desired = WaifuAxoluthuPalettePlacement.state(entry, rotation);
            if (desired == null) return i;
            BlockState existing = level.getBlockState(pos);
            if (!sameBlueprintBlock(existing, desired)) return i;
        }
        return -1;
    }

    private static boolean sameBlueprintBlock(BlockState existing, BlockState desired) {
        if (existing.equals(desired)) return true;
        if (existing.getBlock() != desired.getBlock()) return false;
        // Vanilla (and modded subclasses of) fences and walls automatically change
        // their connection states as neighbors are placed. Do not repair forever.
        return desired.getBlock() instanceof net.minecraft.world.level.block.FenceBlock
                || desired.getBlock() instanceof net.minecraft.world.level.block.WallBlock
                || desired.getBlock() instanceof net.minecraft.world.level.block.IronBarsBlock;
    }

    /** Collect every block drop before changing the world; never void drops on a full bag. */
    private static boolean clearObstruction(WaifuAxoluthuEntity axo, ServerPlayer owner,
                                             ServerLevel level, BlockPos pos, BlockState state) {
        var bag = axo.getAcquiredMaterials();
        var blockEntity = level.getBlockEntity(pos);
        // Inventory-bearing block entities need specialized item-preserving extraction.
        if (blockEntity != null) return false;
        java.util.List<ItemStack> drops = Block.getDrops(state, level, pos, null);
        net.minecraft.world.SimpleContainer simulation = new net.minecraft.world.SimpleContainer(bag.getContainerSize());
        for (int i = 0; i < bag.getContainerSize(); i++) simulation.setItem(i, bag.getItem(i).copy());
        for (ItemStack drop : drops) {
            ItemStack remainder = simulation.addItem(drop.copy());
            if (!remainder.isEmpty()) {
                return false;
            }
        }
        if (!level.removeBlock(pos, false)) return false;
        for (int i = 0; i < bag.getContainerSize(); i++) bag.setItem(i, simulation.getItem(i));
        bag.setChanged();
        return true;
    }

    /**
     * Remove conflicting orders for the same world coordinate. Keep the last
     * occurrence, because later structure/entrance overlays intentionally win.
     * Return true only when the list actually changes; safe for saved builds.
     */
    private static boolean deduplicateBlueprint(ListTag blocks) {
        java.util.HashSet<Long> seen = new java.util.HashSet<>();
        boolean duplicate = false;
        for (int i = blocks.size() - 1; i >= 0; i--) {
            long pos = blocks.getCompound(i).getLong("pos");
            if (!seen.add(pos)) { duplicate = true; break; }
        }
        if (!duplicate) return false;
        seen.clear();
        java.util.List<CompoundTag> canonical = new java.util.ArrayList<>();
        for (int i = blocks.size() - 1; i >= 0; i--) {
            CompoundTag entry = blocks.getCompound(i);
            if (seen.add(entry.getLong("pos"))) canonical.add(entry.copy());
        }
        blocks.clear();
        for (int i = canonical.size() - 1; i >= 0; i--) blocks.add(canonical.get(i));
        return true;
    }

    /** Partition the unbuilt entries once. No per-tick full-blueprint sorting. */
    private static void sortConstructionPhases(ListTag blocks, int cursor, CompoundTag request) {
        int minX=Integer.MAX_VALUE, maxX=Integer.MIN_VALUE, minZ=Integer.MAX_VALUE, maxZ=Integer.MIN_VALUE;
        int ground=Integer.MAX_VALUE;
        for (int i=0;i<blocks.size();i++) {
            BlockPos p=BlockPos.of(blocks.getCompound(i).getLong("pos"));
            minX=Math.min(minX,p.getX()); maxX=Math.max(maxX,p.getX());
            minZ=Math.min(minZ,p.getZ()); maxZ=Math.max(maxZ,p.getZ());
            ground=Math.min(ground,p.getY());
        }
        java.util.List<CompoundTag> shell=new java.util.ArrayList<>();
        java.util.List<CompoundTag> interior=new java.util.ArrayList<>();
        java.util.List<CompoundTag> lights=new java.util.ArrayList<>();
        for (int i=cursor;i<blocks.size();i++) {
            CompoundTag entry=blocks.getCompound(i).copy();
            BlockPos p=BlockPos.of(entry.getLong("pos"));
            String id=entry.getString("block");
            if (isLighting(id)) lights.add(entry);
            else if (p.getY()>ground && p.getY()<=ground+3
                    && p.getX()>minX && p.getX()<maxX
                    && p.getZ()>minZ && p.getZ()<maxZ
                    && (id.contains("planks") || id.contains("door"))) interior.add(entry);
            else shell.add(entry);
        }
        int at=cursor;
        for (CompoundTag e:shell) blocks.set(at++,e);
        request.putInt("trial3ShellEnd",at);
        for (CompoundTag e:interior) blocks.set(at++,e);
        request.putInt("trial3InteriorEnd",at);
        // Place chains before all lanterns in the finishing sweep.
        for (CompoundTag e:lights) if (e.getString("block").equals("minecraft:chain")) blocks.set(at++,e);
        for (CompoundTag e:lights) if (!e.getString("block").equals("minecraft:chain")
                && !e.getString("block").equals("minecraft:lantern")) blocks.set(at++,e);
        for (CompoundTag e:lights) if (e.getString("block").equals("minecraft:lantern")) blocks.set(at++,e);
        // Existing completed prefix is shell by definition for mid-build migrations.
    }

    private static int firstMismatchRange(ServerLevel level,ListTag blocks,int rotation,int start,int end) {
        for (int i=start;i<end;i++) {
            CompoundTag entry=blocks.getCompound(i);
            BlockPos p=BlockPos.of(entry.getLong("pos"));
            if (!level.getChunkSource().hasChunk(p.getX()>>4,p.getZ()>>4)) return i;
            BlockState state=WaifuAxoluthuPalettePlacement.state(entry,rotation);
            if (state==null || !sameBlueprintBlock(level.getBlockState(p),state)) return i;
        }
        return -1;
    }

    /** Stable partition of only the remaining blueprint entries. */
    private static boolean deferLighting(ListTag blocks, int cursor) {
        boolean encounteredLight = false;
        boolean needsReorder = false;
        for (int i = Math.max(0, cursor); i < blocks.size(); i++) {
            boolean light = isLighting(blocks.getCompound(i).getString("block"));
            if (light) encounteredLight = true;
            else if (encounteredLight) { needsReorder = true; break; }
        }
        if (!needsReorder) return false;
        java.util.List<CompoundTag> structure = new java.util.ArrayList<>();
        java.util.List<CompoundTag> lighting = new java.util.ArrayList<>();
        for (int i = cursor; i < blocks.size(); i++) {
            CompoundTag entry = blocks.getCompound(i).copy();
            (isLighting(entry.getString("block")) ? lighting : structure).add(entry);
        }
        int index = cursor;
        for (CompoundTag entry : structure) blocks.set(index++, entry);
        for (CompoundTag entry : lighting) blocks.set(index++, entry);
        return true;
    }

    private static boolean isLighting(String id) {
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        return path.equals("chain") || path.contains("torch") || path.contains("lantern")
                || path.contains("candle") || path.contains("lamp")
                || path.contains("lightning_rod")
                || path.equals("glowstone") || path.equals("sea_pickle")
                || path.equals("end_rod") || path.equals("shroomlight")
                || path.equals("froglight") || path.endsWith("_froglight")
                || path.equals("ochre_froglight") || path.equals("pearlescent_froglight")
                || path.equals("verdant_froglight");
    }

    /** Protected occupants pause placement; ordinary mobs are directed away via existing combat AI. */
    private static boolean clearanceEntity(WaifuAxoluthuEntity axo, ServerLevel level, BlockPos pos,
                                           CompoundTag request, CompoundTag memory, ServerPlayer owner) {
        java.util.List<LivingEntity> occupants = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(pos).inflate(0.15), entity -> entity.isAlive() && entity != axo);
        for (LivingEntity entity : occupants) {
            if (entity instanceof net.minecraft.world.entity.player.Player
                    || entity instanceof Villager || entity instanceof TamableAnimal tame && tame.isTame()) {
                waiting(request, memory, owner, level, "PLEASE_MOVE_PROTECTED_ENTITY_AT_" + pos.toShortString());
                return true;
            }
            if (entity instanceof Monster mob) {
                axo.setTarget(mob);
                waiting(request, memory, owner, level, "CLEARING_MOB_AT_" + pos.toShortString());
                return true;
            }
            waiting(request, memory, owner, level, "ENTITY_IN_PLOT_AT_" + pos.toShortString());
            return true;
        }
        return false;
    }

    private static int freeSpace(net.minecraft.world.SimpleContainer bag, Item item) {
        int space = 0;
        for (int i = 0; i < bag.getContainerSize(); i++) {
            ItemStack stack = bag.getItem(i);
            if (stack.isEmpty()) space += Math.min(item.getDefaultInstance().getMaxStackSize(), bag.getMaxStackSize());
            else if (stack.is(item)) space += Math.max(0, Math.min(stack.getMaxStackSize(), bag.getMaxStackSize()) - stack.getCount());
        }
        return space;
    }

    private static Map<Item, Integer> remainingMaterials(ListTag blocks, int cursor, int rotation) {
        Map<Item, Integer> needed = new HashMap<>();
        for (int j = cursor; j < blocks.size(); j++) {
            BlockState state = WaifuAxoluthuPalettePlacement.state(blocks.getCompound(j), rotation);
            if (state != null && !state.is(Blocks.SEA_LANTERN) && state.getBlock().asItem() != Items.AIR)
                needed.merge(state.getBlock().asItem(), 1, Integer::sum);
        }
        return needed;
    }

    /** Move only unneeded quantities, and only after Repository confirms acceptance. */
    private static void depositSurplus(WaifuAxoluthuEntity axo, ServerPlayer owner, Map<Item, Integer> needed) {
        var bag = axo.getAcquiredMaterials();
        Map<Item, Integer> retained = new HashMap<>();
        for (int i = 0; i < bag.getContainerSize(); i++) {
            ItemStack stack = bag.getItem(i);
            if (stack.isEmpty()) continue;
            int keep = Math.min(stack.getCount(), Math.max(0,
                    needed.getOrDefault(stack.getItem(), 0) - retained.getOrDefault(stack.getItem(), 0)));
            retained.merge(stack.getItem(), keep, Integer::sum);
            int surplus = stack.getCount() - keep;
            if (surplus <= 0) continue;
            ItemStack offered = stack.copy(); offered.setCount(surplus);
            int accepted = 0;
            for (int page = 0; page < CelestialRepository.pages(owner) && accepted < surplus; page++) {
                ItemStack portion = offered.copy(); portion.setCount(surplus - accepted);
                accepted += CelestialRepository.insertPage(owner, page, portion);
            }
            if (accepted > 0) {
                stack.shrink(accepted);
                bag.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
                bag.setChanged();
            }
        }
    }

    /** Keep construction active, but expose its precise wait reason to the owner. */
    private static void waiting(CompoundTag request, CompoundTag memory,
                                ServerPlayer player, ServerLevel level, String reason) {
        String old = request.getString("buildWaitReason");
        long last = request.getLong("buildWaitNoticeTick");
        request.putString("buildWaitReason", reason);
        if (!reason.equals(old) || level.getGameTime() - last >= 200) {
            request.putLong("buildWaitNoticeTick", level.getGameTime());
            // Wait reason remains available in the journal; no unsolicited chat.
        }
        memory.put(KEY, request);
    }

    private static void pause(CompoundTag request, CompoundTag memory,
                              String reason, ServerPlayer player) {
        request.putBoolean("constructionAllowed", false);
        request.putString("phase", "BUILD_PAUSED");
        request.putString("pauseReason", reason);
        memory.put(KEY, request);
        player.sendSystemMessage(Component.literal("[Axoluthu] I paused construction: " + reason
                + ". I won't overwrite anything!"));
    }
}
