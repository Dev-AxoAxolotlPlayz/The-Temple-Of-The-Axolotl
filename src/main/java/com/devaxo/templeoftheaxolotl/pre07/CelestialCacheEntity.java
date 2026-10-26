package com.devaxo.templeoftheaxolotl.pre07;
import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.loot.DungeonRewardInventory;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.network.chat.Component;
import java.util.*;
public class CelestialCacheEntity extends BlockEntity {
 private int tier=1, vanish=-1;
 public CelestialCacheEntity(BlockPos pos,BlockState state){super(Pre07BlockEntities.CACHE.get(),pos,state);}
 public void setTier(int tier){this.tier=Math.max(1,Math.min(4,tier));setChanged();}
 public void claim(ServerPlayer player){
  if(vanish>=0 || !(level instanceof ServerLevel server))return;
  var key=ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID,"chests/treasure_floor_"+tier));
  LootTable table=server.getServer().reloadableRegistries().getLootTable(key);
  var params=new LootParams.Builder(server).withParameter(LootContextParams.ORIGIN,net.minecraft.world.phys.Vec3.atCenterOf(worldPosition)).withLuck(player.getLuck()).create(LootContextParamSets.CHEST);
  List<ItemStack> generated=table.getRandomItems(params);
  // Preflight all slots before committing; never lose a cache on overflow.
  var snapshot=DungeonRewardInventory.read(player);
  for(ItemStack item:generated) if(!DungeonRewardInventory.add(player,item)) {
   DungeonRewardInventory.write(player,snapshot);
   player.sendSystemMessage(Component.literal("Dungeon reward storage full; cache remains available."));return;
  }
  vanish=60;setChanged();
  player.sendSystemMessage(Component.literal("Celestial Cache claimed: rewards transferred to dungeon storage."));
 }
 public static void tick(Level level,BlockPos pos,BlockState state,CelestialCacheEntity entity){
  if(entity.vanish>0){entity.vanish--;entity.setChanged();}
  else if(entity.vanish==0){entity.vanish=-1;level.removeBlock(pos,false);}
 }
 @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);tag.putInt("Tier",tier);tag.putInt("Vanish",vanish);}
 @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);tier=tag.getInt("Tier");vanish=tag.getInt("Vanish");}
}
