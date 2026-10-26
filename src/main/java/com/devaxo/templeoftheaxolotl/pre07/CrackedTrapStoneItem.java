package com.devaxo.templeoftheaxolotl.pre07;
import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import java.util.*;
public class CrackedTrapStoneItem extends Item {
 private final int tier;
 public CrackedTrapStoneItem(int tier, Properties props) { super(props); this.tier=tier; }
 @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
  ItemStack held=player.getItemInHand(hand);
  if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.sidedSuccess(held, level.isClientSide());
  int rolls=sp.isShiftKeyDown()?held.getCount():1;
  ResourceKey<LootTable> key=ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID,"chests/treasure_floor_"+tier));
  LootTable table=server.getServer().reloadableRegistries().getLootTable(key);
  for(int i=0;i<rolls;i++) {
   var params=new LootParams.Builder(server).withParameter(LootContextParams.ORIGIN,sp.position()).withLuck(sp.getLuck()).create(LootContextParamSets.CHEST);
   List<ItemStack> generated=table.getRandomItems(params);
   List<ItemStack> choices=new ArrayList<>();
   for(ItemStack item:generated) if(!item.isEmpty()) choices.add(item);
   // Independent celestial jackpot: normal tier loot is never replaced.
   if(server.random.nextInt(1000)==0) {
    ItemStack bonus=new ItemStack(com.devaxo.templeoftheaxolotl.item.ModItems.SHARD_OF_DIVINITY.get());
    if(!sp.getInventory().add(bonus))sp.drop(bonus,false);
   }
   if(!choices.isEmpty()) {
    ItemStack prize=choices.get(server.getRandom().nextInt(choices.size())).copy();
    prize.setCount(1); // Exactly one item per cracked stone.
    if(!sp.getInventory().add(prize)) sp.drop(prize,false);
   }
  }
  if(!sp.getAbilities().instabuild) held.shrink(rolls);
  sp.sendSystemMessage(Component.literal("Redeemed " + rolls + " Tier " + tier + " cracked trap stone(s)."));
  return InteractionResultHolder.success(held);
 }
}
