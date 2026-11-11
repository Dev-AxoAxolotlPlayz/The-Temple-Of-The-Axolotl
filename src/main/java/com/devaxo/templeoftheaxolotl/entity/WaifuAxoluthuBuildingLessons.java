package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.*;
import java.util.*;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;

/** Owner-initiated architectural observation. Never edits the world. */
public final class WaifuAxoluthuBuildingLessons {
    private WaifuAxoluthuBuildingLessons() {}
    private static final int RADIUS=16, HEIGHT=14, MAX_LESSONS=48;
    private static final String[] CATEGORIES={"slabs","planks","logs","woods","fences","walls","glass","glass_panes","stairs","torches","lanterns","signs","hanging_signs","item_frames"};

    private static String category(BlockState s) {
        Block b=s.getBlock();
        if (b instanceof CeilingHangingSignBlock
                || b instanceof WallHangingSignBlock) {
            return "hanging_signs";
        }
        if(b instanceof SignBlock || b instanceof WallSignBlock)return "signs";
        if(b instanceof LanternBlock)return "lanterns";
        if(b instanceof TorchBlock || b instanceof WallTorchBlock)return "torches";
        if(b instanceof SlabBlock)return "slabs";
        if(b instanceof StairBlock)return "stairs";
        if(b instanceof FenceBlock)return "fences";
        if(b instanceof WallBlock)return "walls";
        if(b instanceof StainedGlassPaneBlock || b instanceof IronBarsBlock && s.is(Blocks.GLASS_PANE))return "glass_panes";
        if(b instanceof StainedGlassBlock || b==Blocks.GLASS || b==Blocks.TINTED_GLASS)return "glass";
        if(s.is(BlockTags.PLANKS))return "planks";
        if(s.is(BlockTags.LOGS)) {
            String id=BuiltInRegistries.BLOCK.getKey(b).getPath();
            return id.endsWith("_wood")||id.endsWith("_hyphae")?"woods":"logs";
        }
        // Fallback for tagged modded glass panes and glass.
        String id=BuiltInRegistries.BLOCK.getKey(b).getPath();
        if(id.endsWith("glass_pane"))return "glass_panes";
        if(id.endsWith("glass"))return "glass";
        return null;
    }
    private static String signature(BlockState s) {
        StringBuilder out=new StringBuilder(BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString());
        for(Property<?> property:s.getProperties())out.append('|').append(property.getName()).append('=').append(s.getValue(property));
        return out.toString();
    }
    private static ListTag capture(ServerLevel level,BlockPos center) {
        ListTag list=new ListTag();
        for(int dx=-RADIUS;dx<=RADIUS;dx++)for(int dy=-3;dy<=HEIGHT;dy++)for(int dz=-RADIUS;dz<=RADIUS;dz++) {
            BlockPos pos=center.offset(dx,dy,dz);
            if(!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4))continue;
            BlockState state=level.getBlockState(pos);
            String cat=category(state);
            if(cat==null)continue;
            CompoundTag entry=new CompoundTag();
            entry.putLong("pos",pos.asLong());
            entry.putString("kind",cat);
            entry.putString("state",signature(state));
            list.add(entry);
        }
        // Item frames are entities rather than blocks. Capture their facing and display item.
        AABB bounds = new AABB(
                center.getX() - RADIUS,
                center.getY() - 3,
                center.getZ() - RADIUS,
                center.getX() + RADIUS + 1,
                center.getY() + HEIGHT + 1,
                center.getZ() + RADIUS + 1
        );
        for(ItemFrame frame:level.getEntitiesOfClass(ItemFrame.class,bounds)) {
            CompoundTag entry=new CompoundTag();
            entry.putLong("pos",frame.blockPosition().asLong());
            entry.putString("kind","item_frames");
            entry.putString("state","item_frame|facing="+frame.getDirection()+"|item="
                    +BuiltInRegistries.ITEM.getKey(frame.getItem().getItem())+"|rotation="+frame.getRotation());
            // Use a separate key space to avoid overwriting block entries at the same position.
            entry.putLong("pos",entry.getLong("pos") ^ Long.MIN_VALUE);
            list.add(entry);
        }
        return list;
    }
    private static Map<Long,CompoundTag> asMap(ListTag list) {
        Map<Long,CompoundTag> result=new HashMap<>();
        for(int i=0;i<list.size();i++) {CompoundTag t=list.getCompound(i);result.put(t.getLong("pos"),t);}
        return result;
    }
    public static String baseline(WaifuAxoluthuEntity axo, BlockPos selectedCenter) {
        if(!(axo.level() instanceof ServerLevel level))return "SERVER_ONLY";
        BlockPos center=selectedCenter;
        if(center==null)return "NO_SELECTED_BUILDING";
        if(!level.getChunkSource().hasChunk(center.getX() >> 4, center.getZ() >> 4))return "WORKSHOP_UNLOADED";
        CompoundTag data=axo.getArchitecturalLearning();
        data.putLong("center",center.asLong());
        data.putString("dimension",level.dimension().location().toString());
        ListTag captured=capture(level,center);
        data.put("baseline",captured);
        return "Building reference saved at "+center.toShortString()+" ("+captured.size()+" teachable blocks). Edit this building, then use /tota axoluthu architecture learn.";
    }
    /** Merge transferable lessons without transferring world-specific baselines. */
    public static void syncKnowledge(WaifuAxoluthuEntity axo) {
        UUID owner = axo.getBondedPlayer();
        if (owner == null) return;
        CompoundTag data = axo.getArchitecturalLearning();
        ListTag local = data.getList("lessons", 10);
        ListTag shared = WaifuAxoluthuKnowledgeLibrary.load(owner);
        // A saved profile is authoritative once it exists; migrate older local lessons otherwise.
        if (shared.isEmpty() && !local.isEmpty()) {
            WaifuAxoluthuKnowledgeLibrary.save(owner, local);
        } else if (!shared.isEmpty()) {
            data.put("lessons", shared);
        }
    }
    public static String learn(WaifuAxoluthuEntity axo) {
        if(!(axo.level() instanceof ServerLevel level))return "SERVER_ONLY";
        CompoundTag data=axo.getArchitecturalLearning();
        if(!data.contains("baseline"))return "NO_BASELINE: run /tota axoluthu architecture baseline first";
        BlockPos center=BlockPos.of(data.getLong("center"));
        if(data.contains("dimension") && !data.getString("dimension").equals(level.dimension().location().toString()))
            return "WRONG_DIMENSION: return to the saved building";
        if(!level.getChunkSource().hasChunk(center.getX() >> 4, center.getZ() >> 4))return "BUILDING_UNLOADED";
        syncKnowledge(axo);
        ListTag before=data.getList("baseline",10), after=capture(level,center);
        Map<Long,CompoundTag> old=asMap(before), now=asMap(after);
        Map<String,Integer> changes=new LinkedHashMap<>();
        Set<Long> positions=new HashSet<>(old.keySet());positions.addAll(now.keySet());
        int total=0;
        ListTag spatial=new ListTag();
        for(long pos:positions) {
            CompoundTag a=old.get(pos),b=now.get(pos);
            String as=a==null?"":a.getString("state"),bs=b==null?"":b.getString("state");
            if(as.equals(bs))continue;
            String category=b!=null?b.getString("kind"):a.getString("kind");
            changes.merge(category,1,Integer::sum);total++;
            // Store the exact new block/entity signature and its position relative to the
            // workshop origin. This is a reusable observation, not just a material count.
            if(b!=null && spatial.size()<1024) {
                CompoundTag delta=b.copy();
                BlockPos at=BlockPos.of(pos ^ ("item_frames".equals(category)?Long.MIN_VALUE:0));
                delta.putInt("dx",at.getX()-center.getX());
                delta.putInt("dy",at.getY()-center.getY());
                delta.putInt("dz",at.getZ()-center.getZ());
                spatial.add(delta);
            }
        }
        if(total==0)return "No teachable block changes found near the saved building at "+center.toShortString()+".";
        ListTag lessons=data.getList("lessons",10);
        CompoundTag lesson=new CompoundTag();
        lesson.putLong("time",level.getGameTime());lesson.putInt("changed",total);
        lesson.put("spatial",spatial);
        for(var e:changes.entrySet())lesson.putInt(e.getKey(),e.getValue());
        lessons.add(lesson);
        while(lessons.size()>MAX_LESSONS)lessons.remove(0);
        data.put("lessons",lessons);
        if (!WaifuAxoluthuKnowledgeLibrary.save(axo.getBondedPlayer(), lessons))
            return "Lesson observed, but cross-world knowledge could not be saved. Check server logs and disk permissions.";
        data.put("baseline",after); // never double-count the same edit
        // Newly taught details may now be applied to a previously finished storage hut.
        data.remove("storageDecoration");
        return "I noticed "+total+" changes! Learned materials: "+changes+". I'll remember this design! :D";
    }
    public static String status(WaifuAxoluthuEntity axo) {
        syncKnowledge(axo);
        CompoundTag data=axo.getArchitecturalLearning();
        ListTag lessons=data.getList("lessons",10);
        Map<String,Integer> totals=new LinkedHashMap<>();
        for(String cat:CATEGORIES)totals.put(cat,0);
        for(int i=0;i<lessons.size();i++)for(String cat:CATEGORIES)totals.put(cat,totals.get(cat)+lessons.getCompound(i).getInt(cat));
        return "Saved lessons="+lessons.size()+"; observed modifications="+totals+"; baseline="+data.contains("baseline")
                +(data.contains("baseline")?" at "+BlockPos.of(data.getLong("center")).toShortString():"")
                +" (spatial examples saved; limited autonomous decoration enabled)";
    }
}
