package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.HashMap;
import java.util.Map;

/** Player-directed construction. No autonomous search, blueprint regeneration or async tasks.
 *  All progress lives in the owner's persisted architectural request.
 */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class AxoluthuCooperativeBuilder {
    private AxoluthuCooperativeBuilder() {}
    private static final String KEY="phase35OwnerSuggestion";

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer owner) || owner.tickCount % 5 != 0) return;
        ServerLevel level=owner.serverLevel();
        for (WaifuAxoluthuEntity axo: level.getEntitiesOfClass(WaifuAxoluthuEntity.class,
                owner.getBoundingBox().inflate(96), a -> owner.getUUID().equals(a.getBondedPlayer()))) {
            CompoundTag memory=axo.getArchitecturalLearning();
            CompoundTag job=memory.getCompound(KEY);
            // Recover legacy paused jobs without requiring another one-use blueprint.
            if (job.getBoolean("pending") && job.getBoolean("siteLocked")
                    && "COOP_PAUSED".equals(job.getString("phase"))
                    && (job.getString("pauseReason").startsWith("PROTECTED_AT_")
                        || job.getString("pauseReason").startsWith("INVENTORY_FULL_OR_UNSAFE_AT_"))) {
                job.putString("phase", "COOP_BUILDING");
                job.remove("pauseReason");
                owner.sendSystemMessage(Component.literal("[Axoluthu] Lucky pencil recovered! Resuming the saved blueprint. :3"));
            }
            if (!job.getBoolean("pending") || !job.getBoolean("siteLocked")
                    || !"COOP_BUILDING".equals(job.getString("phase"))
                    || !job.hasUUID("owner") || !owner.getUUID().equals(job.getUUID("owner"))
                    || !level.dimension().location().toString().equals(job.getString("dimension"))
                    || !job.contains("blueprintPreview",10)) continue;
            CompoundTag manifest=job.getCompound("blueprintPreview");
            ListTag blocks=manifest.getList("blocks",10);
            if (blocks.isEmpty() || blocks.size()>4000) { pause(job,owner,"INVALID_MANIFEST"); continue; }
            Bounds b=bounds(blocks);
            long volume=(long)b.dx()*b.dy()*b.dz();
            if (volume>12000 || b.minY<level.getMinBuildHeight() || b.maxY>=level.getMaxBuildHeight()) {
                pause(job,owner,"INVALID_VOLUME"); continue;
            }
            // Legacy recovery can restore COOP_BUILDING without a valid cooperative stage.
            // Repair metadata in place; never discard the locked blueprint or valid cursors.
            String stage=job.getString("coopStage");
            if (!"CLEAR".equals(stage) && !"PLACE".equals(stage)
                    && !"VERIFY".equals(stage) && !"DONE".equals(stage)) {
                job.putString("coopStage", "CLEAR");
                job.putInt("coopClearCursor", 0);
                job.putInt("coopPlaceCursor", 0);
                job.putInt("coopVerifyCursor", 0);
                job.remove("pauseReason");
                owner.sendSystemMessage(Component.literal("[Axoluthu] Repaired missing construction stage; clearing the saved plot! :3"));
            }
            if (job.getInt("coopClearCursor")<0 || job.getInt("coopClearCursor")>volume)
                job.putInt("coopClearCursor",0);
            if (job.getInt("coopPlaceCursor")<0 || job.getInt("coopPlaceCursor")>blocks.size())
                job.putInt("coopPlaceCursor",0);
            if (job.getInt("coopVerifyCursor")<0 || job.getInt("coopVerifyCursor")>volume)
                job.putInt("coopVerifyCursor",0);

            // A job's planned blocks are fixed. Never regenerate based on the world.
            Map<Long,BlockState> desired=new HashMap<>();
            for(int i=0;i<blocks.size();i++) {
                CompoundTag e=blocks.getCompound(i);
                BlockState state=WaifuAxoluthuPalettePlacement.state(e,job.getInt("quarterTurns"));
                if (state==null || desired.put(BlockPos.of(e.getLong("pos")).asLong(),state)!=null) {
                    pause(job,owner,"INVALID_BLOCKS"); return;
                }
            }
            if ("CLEAR".equals(job.getString("coopStage"))) {
                int cursor=job.getInt("coopClearCursor");
                if (cursor>=volume) {
                    job.putString("coopStage","PLACE");
                    job.putInt("coopPlaceCursor",0);
                    owner.sendSystemMessage(Component.literal("[Axoluthu] Plot ready! Let's build! :D"));
                    continue;
                }
                BlockPos p=at(b,cursor);
                if (!loaded(level,p)) continue;
                BlockState current=level.getBlockState(p);
                BlockState target=desired.get(p.asLong());
                // A block already matching the plan is player contribution.
                if (target==null || !current.equals(target)) {
                    if (!current.isAir()) {
                        if (level.getBlockEntity(p)!=null) { pause(job,owner,"CONTAINER_AT_"+p.toShortString()); continue; }
                        if (occupied(level,p)) { waitForEntity(job,p); continue; }
                        if (!clearWithLoot(level,p,axo)) { waitForInventory(job,owner,p); continue; }
                    }
                }
                job.remove("pauseReason");
                job.putInt("coopClearCursor",cursor+1);
            } else if ("PLACE".equals(job.getString("coopStage"))) {
                int cursor=job.getInt("coopPlaceCursor");
                if (cursor>=blocks.size()) {
                    job.putString("coopStage", "DONE");
                    job.putString("phase", "AWAITING_STRUCTURE_INSPECTION");
                    job.putBoolean("constructionAllowed", false);
                    owner.sendSystemMessage(Component.literal("[Axoluthu] The structure is ready! Inspect and edit it, then approve! :D"));
                    WaifuAxoluthuBuildPhaseReview.showControls(owner, "structure");
                    continue;
                }
                CompoundTag entry=blocks.getCompound(cursor);
                BlockPos p=BlockPos.of(entry.getLong("pos"));
                if (!loaded(level,p)) continue;
                BlockState target=desired.get(p.asLong());
                BlockState current=level.getBlockState(p);
                if (!current.equals(target)) {
                    if (!current.isAir()) {
                        if (level.getBlockEntity(p)!=null) { pause(job,owner,"CONTAINER_AT_"+p.toShortString()); continue; }
                        if (occupied(level,p)) { waitForEntity(job,p); continue; }
                        if (!clearWithLoot(level,p,axo)) { waitForInventory(job,owner,p); continue; }
                    }
                    if (occupied(level,p)) { waitForEntity(job,p); continue; }
                    if (!level.setBlock(p,target,3)) continue;
                    cyanPoof(level,p);
                }
                job.remove("pauseReason");
                job.putInt("coopPlaceCursor",cursor+1);
            } else if ("VERIFY".equals(job.getString("coopStage"))
                    || "DONE".equals(job.getString("coopStage"))) {
                // Migrate saved jobs out of the retired destructive verification loop.
                job.putString("coopStage", "DONE");
                job.putString("phase", "AWAITING_STRUCTURE_INSPECTION");
                job.putBoolean("constructionAllowed", false);
                owner.sendSystemMessage(Component.literal("[Axoluthu] Ready for your structural inspection! :3"));
                WaifuAxoluthuBuildPhaseReview.showControls(owner, "structure");
            }
        }
    }

    /** Use vanilla block loot with an unenchanted diamond pickaxe, but never spawn loose items.
     *  A complete inventory preflight occurs BEFORE modifying the world.
     */
    private static boolean clearWithLoot(ServerLevel level, BlockPos pos, WaifuAxoluthuEntity axo) {
        BlockState old = level.getBlockState(pos);
        if (old.isAir()) return true;
        // Celestial override: hardness does not prevent replacement.
        // Block entities are handled separately to avoid losing stored contents.
        if (level.getBlockEntity(pos)!=null) return false;
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(
                old, level, pos, null, axo, tool);
        SimpleContainer inventory = axo.getAcquiredMaterials();
        // Copy the complete container, so partial insertion cannot eat items on failure.
        List<ItemStack> staged = new ArrayList<>();
        for (int i=0;i<inventory.getContainerSize();i++) staged.add(inventory.getItem(i).copy());
        for (ItemStack drop : drops) {
            ItemStack remainder = drop.copy();
            for (int i=0;i<staged.size() && !remainder.isEmpty();i++) {
                ItemStack slot=staged.get(i);
                if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot,remainder)) {
                    int space=Math.min(inventory.getMaxStackSize(),slot.getMaxStackSize())-slot.getCount();
                    if (space>0) {
                        int n=Math.min(space,remainder.getCount());
                        slot.grow(n); remainder.shrink(n);
                    }
                }
            }
            for (int i=0;i<staged.size() && !remainder.isEmpty();i++) {
                if (staged.get(i).isEmpty()) {
                    int n=Math.min(Math.min(inventory.getMaxStackSize(),remainder.getMaxStackSize()),remainder.getCount());
                    ItemStack placed=remainder.copy(); placed.setCount(n);
                    staged.set(i,placed); remainder.shrink(n);
                }
            }
            if (!remainder.isEmpty()) return false;
        }
        // setBlock avoids vanilla block-breaking debris and entity item drops.
        if (!level.setBlock(pos, Blocks.AIR.defaultBlockState(),3)) return false;
        for (int i=0;i<staged.size();i++) inventory.setItem(i,staged.get(i));
        inventory.setChanged();
        cyanPoof(level,pos);
        return true;
    }

    private static void cyanPoof(ServerLevel level, BlockPos pos) {
        level.sendParticles(new DustParticleOptions(new Vector3f(0.10f,0.92f,1.0f),1.25f),
                pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5,
                12,0.28,0.28,0.28,0.015);
    }

    private static void waitForEntity(CompoundTag job, BlockPos pos) {
        job.putString("pauseReason", "WAITING_FOR_ENTITY_AT_"+pos.toShortString());
    }

    private static void waitForInventory(CompoundTag job, ServerPlayer owner, BlockPos pos) {
        // Keep the job active and its cursor unchanged. Once space is freed, the next tick retries.
        String reason="WAITING_FOR_INVENTORY_AT_"+pos.toShortString();
        if (!reason.equals(job.getString("pauseReason"))) {
            job.putString("pauseReason",reason);
            owner.sendSystemMessage(Component.literal("[Axoluthu] My backpack is full! Make some space and I'll continue. ^u^"));
        }
    }

    private static void pause(CompoundTag job, ServerPlayer owner, String reason) {
        job.putString("phase","COOP_PAUSED");
        job.putString("pauseReason",reason);
        owner.sendSystemMessage(Component.literal("[Axoluthu] Construction paused: "+reason));
    }
    private static boolean loaded(ServerLevel level,BlockPos p) {
        return p.getY()>=level.getMinBuildHeight() && p.getY()<level.getMaxBuildHeight()
                && level.getChunkSource().hasChunk(p.getX()>>4,p.getZ()>>4);
    }
    private static boolean occupied(ServerLevel level,BlockPos p) {
        return !level.getEntitiesOfClass(LivingEntity.class,new AABB(p),LivingEntity::isAlive).isEmpty();
    }
    private record Bounds(int minX,int maxX,int minY,int maxY,int minZ,int maxZ) {
        int dx(){return maxX-minX+1;} int dy(){return maxY-minY+1;} int dz(){return maxZ-minZ+1;}
    }
    private static Bounds bounds(ListTag blocks) {
        int minX=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,minY=Integer.MAX_VALUE,maxY=Integer.MIN_VALUE,minZ=Integer.MAX_VALUE,maxZ=Integer.MIN_VALUE;
        for(int i=0;i<blocks.size();i++) {
            BlockPos p=BlockPos.of(blocks.getCompound(i).getLong("pos"));
            minX=Math.min(minX,p.getX()); maxX=Math.max(maxX,p.getX());
            minY=Math.min(minY,p.getY()); maxY=Math.max(maxY,p.getY());
            minZ=Math.min(minZ,p.getZ()); maxZ=Math.max(maxZ,p.getZ());
        }
        return new Bounds(minX,maxX,minY,maxY,minZ,maxZ);
    }
    private static BlockPos at(Bounds b,int index) {
        int dz=b.dz(),dx=b.dx();
        int y=index/(dx*dz), rem=index%(dx*dz);
        return new BlockPos(b.minX+rem/dz,b.minY+y,b.minZ+rem%dz);
    }
}
