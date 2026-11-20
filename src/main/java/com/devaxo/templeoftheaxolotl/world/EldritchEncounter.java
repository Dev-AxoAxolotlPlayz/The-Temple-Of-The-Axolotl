package com.devaxo.templeoftheaxolotl.world;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import com.devaxo.templeoftheaxolotl.block.EldritchWatersPortalBlock;
import com.devaxo.templeoftheaxolotl.entity.*;
@EventBusSubscriber(modid="temple_of_the_axolotl")
public final class EldritchEncounter extends SavedData {
    private UUID boss;
    private int bossChunkX=0, bossChunkZ=9;
    private long clearedAt = -1;
    public static EldritchEncounter get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(EldritchEncounter::new,EldritchEncounter::load),"tota_eldritch_encounter"); }
    private static EldritchEncounter load(CompoundTag t, HolderLookup.Provider p) { var s=new EldritchEncounter(); if(t.hasUUID("Boss")) s.boss=t.getUUID("Boss"); if(t.contains("BossChunkX")){s.bossChunkX=t.getInt("BossChunkX");s.bossChunkZ=t.getInt("BossChunkZ");} if(t.contains("ClearedAt")) s.clearedAt=t.getLong("ClearedAt"); return s; }
    @Override public CompoundTag save(CompoundTag t, HolderLookup.Provider p) { if(boss!=null)t.putUUID("Boss",boss); t.putInt("BossChunkX",bossChunkX);t.putInt("BossChunkZ",bossChunkZ);t.putLong("ClearedAt",clearedAt); return t; }
    public boolean owns(UUID id) { return id.equals(boss); }
    public boolean active() { return boss!=null; }
    public static boolean arena(ServerLevel s) { return s.dimension().equals(EldritchWatersPortalBlock.DIMENSION); }
    public static List<net.minecraft.server.level.ServerPlayer> participants(ServerLevel s) { return s.players().stream().filter(p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&Math.abs(p.getX())<256&&Math.abs(p.getZ())<256).toList(); }
    public static void ensure(ServerLevel s) { var state=get(s); if(state.active())return; for(Entity e:s.getAllEntities()) if(e instanceof AxoCthuluEntity b && b.isAlive()){ state.boss=b.getUUID();state.bossChunkX=b.chunkPosition().x;state.bossChunkZ=b.chunkPosition().z;state.setDirty();return; } var b=ModEntities.AXOCTHULU.get().create(s); if(b!=null){ b.moveTo(.5,2,150.5,180,0); s.getChunk(b.chunkPosition().x,b.chunkPosition().z); if(s.addFreshEntity(b)){b.beginEncounter();state.boss=b.getUUID();state.bossChunkX=b.chunkPosition().x;state.bossChunkZ=b.chunkPosition().z;state.setDirty();} } }
    @SubscribeEvent public static void join(EntityJoinLevelEvent e) { if(!(e.getLevel() instanceof ServerLevel s)||!arena(s)||e.getEntity() instanceof Player)return; var state=get(s); Entity entity=e.getEntity(); if(entity instanceof AxoCthuluEntity b){ if(state.active()&&!state.owns(b.getUUID())){e.setCanceled(true);return;} if(!state.active() && state.clearedAt>=0 && e.loadedFromDisk()){e.setCanceled(true);return;} state.boss=b.getUUID();state.bossChunkX=b.chunkPosition().x;state.bossChunkZ=b.chunkPosition().z;state.setDirty(); } else if(entity.getPersistentData().hasUUID("EldritchOwner")&&!state.owns(entity.getPersistentData().getUUID("EldritchOwner"))) e.setCanceled(true); }
    @SubscribeEvent public static void keepBossTicking(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel s) || !arena(s) || s.getGameTime()%20!=0 || s.players().stream().noneMatch(p->p.isAlive()&&!p.isSpectator())) return;
        var state=get(s); if (!state.active()) return;
        if (s.getEntity(state.boss) instanceof AxoCthuluEntity b) {
            var pos=b.chunkPosition();
            if(state.bossChunkX!=pos.x || state.bossChunkZ!=pos.z) {state.bossChunkX=pos.x;state.bossChunkZ=pos.z;state.setDirty();}
        }
        s.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.POST_TELEPORT,new net.minecraft.world.level.ChunkPos(state.bossChunkX,state.bossChunkZ),2,0);
    }
    @SubscribeEvent public static void death(LivingDeathEvent e) { if(!(e.getEntity().level() instanceof ServerLevel s)||!arena(s))return; var state=get(s); Entity entity=e.getEntity(); if(state.boss!=null && s.getEntity(state.boss) instanceof AxoCthuluEntity b) b.forgetSummon(entity.getUUID()); }
    public void complete(UUID id, ServerLevel s){if(owns(id)){boss=null;clearedAt=s.getGameTime();setDirty();}}
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("tota_dev").requires(src->src.hasPermission(2)).then(Commands.literal("eldritchwaters").then(Commands.literal("clearentities").executes(ctx->{var s=ctx.getSource().getServer().getLevel(EldritchWatersPortalBlock.DIMENSION);if(s==null)return 0;var state=get(s);var list=new ArrayList<Entity>();for(Entity entity:s.getAllEntities())if(!(entity instanceof Player))list.add(entity);for(Entity entity:list)entity.discard();state.boss=null;state.clearedAt=s.getGameTime();state.setDirty();int count=list.size();ctx.getSource().sendSuccess(()->Component.literal("Eldritch Waters: cleared "+count+" loaded non-player entities; old bosses rejected on reload."),true);return count;}))));}
}
