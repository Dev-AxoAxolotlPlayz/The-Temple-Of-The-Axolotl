package com.devaxo.templeoftheaxolotl.promise;
import java.util.*;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import com.devaxo.templeoftheaxolotl.entity.AxokrakenEntity;
import com.devaxo.templeoftheaxolotl.entity.WaifuAxoluthuEntity;
import com.devaxo.templeoftheaxolotl.registry.ModBlocks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
@EventBusSubscriber(modid="temple_of_the_axolotl")
public final class PromiseActions {
 private static final Set<ServerPlayer> ACTIVE=Collections.newSetFromMap(new IdentityHashMap<>());
 private record Job(Object level, ItemStack tool, ArrayDeque<BlockPos> blocks){}
 private record Pulse(Vec3 point,int expires,int arm){}
 private static final Map<ServerPlayer,List<Pulse>> PULSES=new WeakHashMap<>();
 private static final Map<ServerPlayer,int[]> NEXT=new WeakHashMap<>();
 static void pulse(ServerPlayer p,Vec3 point,boolean mining){var list=PULSES.computeIfAbsent(p,k->new ArrayList<>());int role=mining?1:0;int arm=role+4*(NEXT.computeIfAbsent(p,k->new int[2])[role]++ & 1); list.removeIf(a->a.arm()==arm); list.add(new Pulse(point,p.tickCount+3,arm));while(list.size()>8)list.remove(0);}
 static List<PromiseSnapshot.Contact> contacts(ServerPlayer p,int max){var list=PULSES.get(p);if(list==null)return List.of();list.removeIf(a->a.expires()<p.tickCount || (a.arm()%4==1?!PromiseState.settings(p).getBoolean("mining"):PromiseState.settings(p).getInt("combat")==0));return list.stream().skip(Math.max(0,list.size()-max)).limit(max).map(a->new PromiseSnapshot.Contact(a.arm(),a.point())).toList();}
 private static final Map<ServerPlayer,Integer> LAST_COMBAT=new WeakHashMap<>();
 private static boolean meleeWeapon(ItemStack stack){
  var item=stack.getItem();
  return item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem
      || item instanceof MaceItem || stack.is(ModItems.RING_OF_THE_ONE.get());
 }
 private static void innateAttack(ServerPlayer p,LivingEntity target){
  // Do not let unrelated held-item damage hooks or enchantments affect innate damage.
  int slot=p.getInventory().selected;
  ItemStack original=p.getInventory().getItem(slot);
  ACTIVE.add(p);
  try{
   p.getInventory().setItem(slot,ItemStack.EMPTY);
   target.hurt(p.damageSources().playerAttack(p),8.0F);
  }finally{
   p.getInventory().setItem(slot,original);
   ACTIVE.remove(p);
  }
 }
 private static final Map<ServerPlayer,Job> JOBS=new WeakHashMap<>();
 private static boolean enabled(ServerPlayer p){return PromiseState.installed(p)&&!p.isSpectator()&&p.isAlive();}
 @SubscribeEvent public static void loot(LivingDropsEvent e){if(e.getEntity() instanceof AxokrakenEntity a)e.getDrops().add(new ItemEntity(a.level(),a.getX(),a.getY(),a.getZ(),new ItemStack(ModItems.KRAKEN_LIMB.get())));}
 @SubscribeEvent public static void commands(RegisterCommandsEvent e){
 var root=Commands.literal("promise");
 root.then(Commands.literal("mining").executes(c->{var p=c.getSource().getPlayerOrException();var s=PromiseState.settings(p);s.putBoolean("mining",!s.getBoolean("mining"));PromiseState.set(p,s);p.sendSystemMessage(Component.literal("Mining: "+s.getBoolean("mining")));return 1;}));
 root.then(Commands.literal("combat").then(Commands.argument("mode",IntegerArgumentType.integer(0,2)).executes(c->{var p=c.getSource().getPlayerOrException();var s=PromiseState.settings(p);s.putInt("combat",IntegerArgumentType.getInteger(c,"mode"));PromiseState.set(p,s);return 1;})));
 for(String key:List.of("left","right","above","below","depth"))root.then(Commands.literal(key).then(Commands.argument("value",IntegerArgumentType.integer(key.equals("depth")?1:0,5)).executes(c->{var p=c.getSource().getPlayerOrException();var s=PromiseState.settings(p);int value=IntegerArgumentType.getInteger(c,"value"),n=PromiseUpgrade.level(p);int max=key.equals("depth")?PromiseUpgrade.depth(n):key.equals("left")||key.equals("right")?PromiseUpgrade.width(n)-1:PromiseUpgrade.height(n)-1;String other=switch(key){case "left"->"right";case "right"->"left";case "above"->"below";case "below"->"above";default->"";};if(value+(other.isEmpty()?0:s.getInt(other))>max)return 0;s.putInt(key,value);PromiseState.set(p,s);return 1;})));
 e.getDispatcher().register(Commands.literal("tota").then(root));}
 @SubscribeEvent public static void mining(BlockEvent.BreakEvent e){if(e.isCanceled()||!(e.getPlayer() instanceof ServerPlayer p)||ACTIVE.contains(p)||!enabled(p)||!PromiseState.settings(p).getBoolean("mining")||p.isCreative())return;
 var s=PromiseState.settings(p);int n=PromiseUpgrade.level(p),l=Math.min(s.getInt("left"),PromiseUpgrade.width(n)-1),r=Math.min(s.getInt("right"),PromiseUpgrade.width(n)-1-l),u=Math.min(s.getInt("above"),PromiseUpgrade.height(n)-1),d=Math.min(s.getInt("below"),PromiseUpgrade.height(n)-1-u),depth=Math.max(1,Math.min(s.getInt("depth"),PromiseUpgrade.depth(n)));
 Direction forward=p.getDirection(),right=forward.getClockWise();ArrayDeque<BlockPos> q=new ArrayDeque<>();for(int z=0;z<depth;z++)for(int y=-d;y<=u;y++)for(int x=-l;x<=r;x++)if(x!=0||y!=0||z!=0)q.add(e.getPos().relative(forward,z).relative(right,x).above(y));JOBS.put(p,new Job(p.level(),p.getMainHandItem().copy(),q));}
 private static boolean reach(ServerPlayer p,Vec3 target){return PromiseAnchorPlanner.inCube(p.position().add(0,1,0),target,5+PromiseUpgrade.level(p));}
 private static void preserve(ServerPlayer p,Runnable action){ItemStack tool=p.getMainHandItem();int slot=p.getInventory().selected;ACTIVE.add(p);try{p.getInventory().setItem(slot,tool.copy());action.run();}finally{p.getInventory().setItem(slot,tool);ACTIVE.remove(p);}}
 private static boolean valid(ServerPlayer p,LivingEntity t){return t!=null&&t!=p&&t.isAlive()&&t.level()==p.level()&&!(t instanceof Player)&&!(t instanceof TamableAnimal a&&a.isTame())&&!(t instanceof WaifuAxoluthuEntity)&&!p.isAlliedTo(t)&&reach(p,t.position());}
 @SubscribeEvent public static void tick(PlayerTickEvent.Post e){if(!(e.getEntity() instanceof ServerPlayer p))return;if(!enabled(p)){JOBS.remove(p);PULSES.remove(p);NEXT.remove(p);LAST_COMBAT.remove(p);return;}var s=PromiseState.settings(p);Job j=JOBS.get(p);if(j!=null){if(j.level()!=p.level()||!s.getBoolean("mining")||j.tool().getItem()!=p.getMainHandItem().getItem())JOBS.remove(p);else{for(int i=0;i<2&&!j.blocks().isEmpty();i++){BlockPos b=j.blocks().removeFirst();var level=p.serverLevel();if(!level.hasChunk(b.getX()>>4,b.getZ()>>4)||!reach(p,Vec3.atCenterOf(b))||!level.mayInteract(p,b))continue;var state=level.getBlockState(b);String id=BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();if(state.isAir()||state.getDestroySpeed(level,b)<0||id.contains("bedrock")||!(!state.requiresCorrectToolForDrops() || p.getMainHandItem().isCorrectToolForDrops(state) || p.getMainHandItem().is(ModItems.RING_OF_THE_ONE.get())))continue;boolean[] broken={false};preserve(p,()->broken[0]=p.gameMode.destroyBlock(b));if(broken[0])pulse(p,Vec3.atCenterOf(b),true);}if(j.blocks().isEmpty())JOBS.remove(p);}}
 int mode=s.getInt("combat");if(mode==0){LAST_COMBAT.remove(p);return;}
 boolean weapon=meleeWeapon(p.getMainHandItem());
 double speed=weapon?p.getAttributeValue(Attributes.ATTACK_SPEED):1.6D;
 int interval=(int)Math.ceil(20.0D/Math.max(0.01D,speed));
 if(p.tickCount-LAST_COMBAT.getOrDefault(p,-10000)<interval || (weapon&&p.getAttackStrengthScale(0)<1.0F))return;LivingEntity t=p.tickCount-p.getLastHurtMobTimestamp()<100?p.getLastHurtMob():null;if(!valid(p,t)&&mode==2){t=p.tickCount-p.getLastHurtByMobTimestamp()<100?p.getLastHurtByMob():null;if(!valid(p,t))t=p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(5+PromiseUpgrade.level(p)),a->a instanceof Enemy&&valid(p,a)).stream().min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);}if(valid(p,t)){LivingEntity target=t;LAST_COMBAT.put(p,p.tickCount);if(weapon)preserve(p,()->p.attack(target));else innateAttack(p,target);pulse(p,target.getBoundingBox().getCenter(),false);}}
}
