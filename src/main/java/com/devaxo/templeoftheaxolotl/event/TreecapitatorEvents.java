package com.devaxo.templeoftheaxolotl.event;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;

/** Progressive bottom-up Treecapitator harvest. One merged loot delivery per tree. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class TreecapitatorEvents {
    private static final ResourceKey<Enchantment> KEY = ResourceKey.create(Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "treecapitator"));
    private static final Map<UUID, Harvest> ACTIVE = new HashMap<>();
    private static final int[] LIMITS = {0,10,30,50,150};
    private static final int[] SPEEDS = {0,2,3,5,10};
    private record Harvest(UUID player, ResourceLocation dimension, List<BlockPos> positions,
                           List<ItemStack> drops, int speed, int index) {
        Harvest advance(int next) { return new Harvest(player,dimension,positions,drops,speed,next); }
    }
    private TreecapitatorEvents() {}

    private static boolean isLog(BlockState state) {
        return state.is(BlockTags.LOGS) && !state.isAir();
    }
    private static int level(ServerPlayer player) {
        ItemStack tool=player.getMainHandItem();
        if (!(tool.getItem() instanceof AxeItem) && !tool.is(ModItems.RING_OF_THE_ONE.get())) return 0;
        Holder<Enchantment> enchant = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(KEY).orElse(null);
        return enchant == null ? 0 : Math.min(4,tool.getEnchantmentLevel(enchant));
    }
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel world) || ACTIVE.containsKey(player.getUUID())) return;
        int enchantLevel=level(player);
        if (enchantLevel == 0 || !isLog(event.getState())) return;
        BlockPos origin=event.getPos();
        // Reject flat log structures: natural trees need leaves in their canopy.
        boolean canopy=false;
        for(int x=-4;x<=4&&!canopy;x++) for(int y=0;y<=12&&!canopy;y++) for(int z=-4;z<=4;z++) {
            BlockPos probe=origin.offset(x,y,z);
            if (world.hasChunk(probe.getX()>>4,probe.getZ()>>4)
                    && world.getBlockState(probe).is(BlockTags.LEAVES)) {canopy=true;break;}
        }
        if(!canopy)return;
        ArrayDeque<BlockPos> frontier=new ArrayDeque<>();
        Set<BlockPos> visited=new HashSet<>();
        List<BlockPos> logs=new ArrayList<>();
        frontier.add(origin.immutable());visited.add(origin.immutable());
        while(!frontier.isEmpty() && logs.size()<LIMITS[enchantLevel]) {
            BlockPos pos=frontier.removeFirst();
            if (!world.hasChunk(pos.getX()>>4,pos.getZ()>>4) || !isLog(world.getBlockState(pos))) continue;
            logs.add(pos);
            for(int dx=-1;dx<=1;dx++) for(int dy=-1;dy<=1;dy++) for(int dz=-1;dz<=1;dz++) {
                if(dx==0&&dy==0&&dz==0)continue;
                BlockPos next=pos.offset(dx,dy,dz);
                if(next.getY()<origin.getY() || next.getY()>origin.getY()+48
                        || Math.abs(next.getX()-origin.getX())>12 || Math.abs(next.getZ()-origin.getZ())>12)continue;
                if(visited.add(next.immutable()))frontier.add(next.immutable());
            }
        }
        logs.remove(origin);
        logs.sort(Comparator.comparingInt(BlockPos::getY));
        if(!logs.isEmpty())ACTIVE.put(player.getUUID(),new Harvest(player.getUUID(),
                world.dimension().location(),logs,new ArrayList<>(),SPEEDS[enchantLevel],0));
    }
    private static void merge(List<ItemStack> list, ItemStack incoming) {
        if(incoming.isEmpty())return;
        for(ItemStack stack:list) if(ItemStack.isSameItemSameComponents(stack,incoming)
                && stack.getCount()<stack.getMaxStackSize()) {
            int count=Math.min(incoming.getCount(),stack.getMaxStackSize()-stack.getCount());
            stack.grow(count);incoming.shrink(count);
            if(incoming.isEmpty())return;
        }
        while(!incoming.isEmpty()) {
            int count=Math.min(incoming.getCount(),incoming.getMaxStackSize());
            ItemStack part=incoming.copy();part.setCount(count);list.add(part);incoming.shrink(count);
        }
    }
    private static void finish(ServerPlayer player,Harvest harvest) {
        for(ItemStack stack:harvest.drops()) {
            ItemEntity entity=new ItemEntity(player.level(),player.getX(),player.getY()+0.2,player.getZ(),stack);
            entity.setPickUpDelay(0);
            player.level().addFreshEntity(entity);
        }
    }
    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        Iterator<Map.Entry<UUID,Harvest>> iterator=ACTIVE.entrySet().iterator();
        while(iterator.hasNext()) {
            var entry=iterator.next(); Harvest h=entry.getValue();
            ServerPlayer player=event.getServer().getPlayerList().getPlayer(h.player());
            if(player==null || !player.isAlive() || !player.level().dimension().location().equals(h.dimension())) {
                if(player!=null)finish(player,h);
                iterator.remove();continue;
            }
            ServerLevel world=(ServerLevel)player.level();
            int index=h.index();
            for(int i=0;i<h.speed() && index<h.positions().size();i++,index++) {
                BlockPos pos=h.positions().get(index);
                if(!world.hasChunk(pos.getX()>>4,pos.getZ()>>4))continue;
                BlockState state=world.getBlockState(pos);
                if(!isLog(state))continue;
                // Respect the player's current tool; never break logs in unloaded chunks.
                if(level(player)==0)break;
                for(ItemStack drop:Block.getDrops(state,world,pos,world.getBlockEntity(pos),player,player.getMainHandItem()))
                    merge(h.drops(),drop.copy());
                world.removeBlock(pos,false);
                world.sendParticles(ParticleTypes.END_ROD,pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5,
                        4,0.22,0.22,0.22,0.02);
                if(!player.getMainHandItem().is(ModItems.RING_OF_THE_ONE.get()))
                    player.getMainHandItem().hurtAndBreak(1,player,net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            }
            if(index>=h.positions().size() || level(player)==0) {finish(player,h);iterator.remove();}
            else entry.setValue(h.advance(index));
        }
    }
}
