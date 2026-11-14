package com.devaxo.templeoftheaxolotl.entity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.*;
@EventBusSubscriber(modid=com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl.MODID)
public final class WaifuAxoluthuMaintenance {
    private record Bounds(String dimension, BlockPos min, BlockPos max) {}
    private static final Map<UUID,Bounds> visible=new HashMap<>();
    public static int toggle(ServerPlayer p,int n){
        for(var a:p.serverLevel().getEntitiesOfClass(WaifuAxoluthuEntity.class,p.getBoundingBox().inflate(128),e->p.getUUID().equals(e.getBondedPlayer()))){
            ListTag plots=WaifuAxoluthuVillageRegistry.plots(a);if(n<1||n>plots.size())return 0;
            CompoundTag plot=plots.getCompound(n-1);Bounds b=new Bounds(plot.getString("dimension"),WaifuAxoluthuVillageRegistry.min(plot),WaifuAxoluthuVillageRegistry.max(plot));
            if(b.equals(visible.get(p.getUUID())))visible.remove(p.getUUID());else visible.put(p.getUUID(),b);return 1;
        }return 0;
    }
    @SubscribeEvent public static void particles(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer p)||p.tickCount%10!=0)return;
        Bounds b=visible.get(p.getUUID());if(b==null||!b.dimension.equals(p.level().dimension().location().toString()))return;
        double[] lo={b.min.getX(),b.min.getY(),b.min.getZ()},hi={b.max.getX()+1.,b.max.getY()+1.,b.max.getZ()+1.};
        var dust=new DustParticleOptions(new org.joml.Vector3f(0.1f,0.9f,1f),0.8f);
        for(int axis=0;axis<3;axis++)for(int mask=0;mask<4;mask++){
            int u=(axis+1)%3,v=(axis+2)%3;double[] q=lo.clone();q[u]=(mask&1)==0?lo[u]:hi[u];q[v]=(mask&2)==0?lo[v]:hi[v];int count=Math.min(64,Math.max(1,(int)Math.ceil((hi[axis]-lo[axis])*2)));
            for(int j=0;j<=count;j++){q[axis]=lo[axis]+(hi[axis]-lo[axis])*j/count;if(p.distanceToSqr(q[0],q[1],q[2])<4096)p.serverLevel().sendParticles(p,dust,false,q[0],q[1],q[2],1,0,0,0,0);}
        }
    }
    @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event){visible.remove(event.getEntity().getUUID());}
    public static boolean step(WaifuAxoluthuEntity a,ServerLevel l){
        ListTag plots=WaifuAxoluthuVillageRegistry.plots(a);
        for(int i=0;i<plots.size();i++){
            CompoundTag p=plots.getCompound(i);if(!"NEEDS_ATTENTION".equals(p.getString("condition"))||!l.dimension().location().toString().equals(p.getString("dimension")))continue;
            if(a.tickCount%20!=0)return true;
            ListTag blueprint=p.getList("finalBlueprint",10);if(blueprint.isEmpty())continue;
            int cursor=Math.max(0,p.getInt("repairCursor"));boolean conflict=p.getBoolean("repairConflict")&&cursor>0;
            for(int budget=0;budget<64&&cursor<blueprint.size();budget++,cursor++){
                CompoundTag entry=blueprint.getCompound(cursor);BlockPos pos=BlockPos.of(entry.getLong("pos"));
                if(!l.getChunkSource().hasChunk(pos.getX()>>4,pos.getZ()>>4)){p.putInt("repairCursor",cursor);return true;}
                BlockState expected=NbtUtils.readBlockState(l.holderLookup(Registries.BLOCK),entry.getCompound("fullState"));BlockState actual=l.getBlockState(pos);
                if(actual.equals(expected))continue;
                if(expected.isAir()||!actual.isAir()||expected.getBlock().asItem()==Items.AIR||expected.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)||expected.getBlock() instanceof net.minecraft.world.level.block.BedBlock||expected.getBlock() instanceof net.minecraft.world.level.block.ChestBlock){conflict=true;continue;}
                p.putInt("repairCursor",cursor);p.putBoolean("repairConflict",conflict);
                if(WaifuAxoluthuBuildNavigation.approach(a,l,pos,1.5)!=null)return true;
                if(!expected.canSurvive(l,pos)){conflict=true;continue;}
                var bag=a.getAcquiredMaterials();int slot=-1;for(int k=0;k<bag.getContainerSize();k++)if(bag.getItem(k).is(expected.getBlock().asItem())){slot=k;break;}
                if(slot<0){WaifuAxoluthuRecipeBrain.ensure(a,expected.getBlock().asItem(),1,l.getGameTime());return true;}
                if(l.setBlock(pos,expected,3)){bag.removeItem(slot,1);bag.setChanged();}else conflict=true;
                p.putInt("repairCursor",cursor+1);p.putBoolean("repairConflict",conflict);return true;
            }
            p.putInt("repairCursor",cursor);p.putBoolean("repairConflict",conflict);
            if(cursor>=blueprint.size()){p.putString("condition",conflict?"REVIEW_REQUIRED":"INTACT");p.putInt("repairCursor",0);a.getNavigation().stop();}return true;
        }return false;
    }
}
