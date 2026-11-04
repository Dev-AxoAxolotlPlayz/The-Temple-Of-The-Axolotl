package com.devaxo.templeoftheaxolotl.pre07;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import java.util.*;
/** Each trap fires once; every spawned Sentinel carries its own death reward. */
public class TrialTrapSpawnerEntity extends BlockEntity {
 private int tier=1, state=0, age=0;
 private static final String[] NAMES={"Vorthal","Nerath","Dreadfin","Thalassus","Sir Bubbles","Murkveil","Abyssion","Oceiros"};
 private static final String[] TITLES={"the Tidebreaker","the Unforgiven","of the Deep","the Silent","Harbinger of Ruin","the Forgotten"};
 public TrialTrapSpawnerEntity(BlockPos pos,BlockState block){super(Pre07BlockEntities.TRAP.get(),pos,block);}
 public void setTier(int tier){this.tier=Math.max(1,Math.min(4,tier));setChanged();}
 public static void tick(Level world,BlockPos pos,BlockState block,TrialTrapSpawnerEntity trap){
  if(!(world instanceof ServerLevel level)||++trap.age%10!=0||trap.state!=0)return;
  var players=level.getEntitiesOfClass(ServerPlayer.class,new AABB(pos).inflate(3),p->!p.isSpectator()&&!p.isCreative()&&p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=9);
  if(players.isEmpty())return;
  ServerPlayer player=players.get(0);
  int floor=Math.max(1,Math.min(10,com.devaxo.templeoftheaxolotl.traps.TempleProgress.floor(player)));
  // Mark consumed BEFORE spawning; reloads cannot restart the encounter.
  trap.state=2;trap.setChanged();
  // Universal one-time outcome: 25% single, 15% double, 60% blank.
  int outcome = level.random.nextInt(100);
  int encounters = outcome < 25 ? 1 : outcome < 40 ? 2 : 0;
  for (int i = 0; i < encounters; i++) spawnEncounter(level,pos,floor,trap.tier);
 }
 private static void spawnEncounter(ServerLevel level,BlockPos pos,int floor,int layer){
  int roll=level.random.nextInt(100);
  if(roll<30){spawnSentinel(level,pos,floor,layer);return;}
  spawnRegularHostile(level,pos);
 }
 /** One weighted regular hostile per regular encounter; no water or terrain modifications. */
 private static void spawnRegularHostile(ServerLevel level,BlockPos pos){
  // Skeleton variants are selected together; Wither Skeletons are 14% of this category.
  int choice=level.random.nextInt(20);
  EntityType<? extends Mob> type=switch(choice){
   case 0 -> EntityType.ZOMBIE;
   case 1 -> EntityType.HUSK;
   case 2 -> EntityType.DROWNED;
   case 3 -> EntityType.ZOMBIE_VILLAGER;
   case 4 -> skeletonType(level);
   case 5 -> EntityType.SPIDER;
   case 6 -> EntityType.CAVE_SPIDER;
   case 7 -> EntityType.VINDICATOR;
   case 8 -> EntityType.RAVAGER;
   case 9 -> EntityType.ZOGLIN;
   case 10 -> EntityType.WITCH;
   case 11 -> EntityType.SLIME;
   case 12 -> EntityType.VEX;
   case 13 -> EntityType.ZOGLIN;
   case 14 -> EntityType.RAVAGER;
   case 15 -> EntityType.BREEZE;
   case 16 -> EntityType.ZOMBIE;
   case 17 -> EntityType.CAVE_SPIDER;
   case 18 -> EntityType.VINDICATOR;
   default -> EntityType.SPIDER;
  };
  Mob mob=type.create(level);
  if(mob==null)return;
  mob.moveTo(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,level.random.nextFloat()*360,0);
  mob.setPersistenceRequired();
  if(mob instanceof AbstractSkeleton){
   mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(level.random.nextBoolean()?Items.NETHERITE_SWORD:Items.NETHERITE_AXE));
   mob.setDropChance(EquipmentSlot.MAINHAND,0);
  } else if(mob instanceof Vindicator){
   mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.NETHERITE_AXE));
   mob.setDropChance(EquipmentSlot.MAINHAND,0);
  }
  TempleCrew.prepare(level, mob);
  level.addFreshEntity(mob);
 }
 /** Trap-only: creative-enchanted axe and full netherite, none of it farmable. */
 private static void equipBrute(ServerLevel level,Mob brute){
  ItemStack axe=new ItemStack(Items.NETHERITE_AXE);
  var enchants=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
  axe.enchant(enchants.getOrThrow(Enchantments.SHARPNESS),5);
  axe.enchant(enchants.getOrThrow(Enchantments.SMITE),5);
  axe.enchant(enchants.getOrThrow(Enchantments.BANE_OF_ARTHROPODS),5);
  axe.enchant(enchants.getOrThrow(Enchantments.FIRE_ASPECT),2);
  axe.enchant(enchants.getOrThrow(Enchantments.KNOCKBACK),2);
  axe.enchant(enchants.getOrThrow(Enchantments.LOOTING),3);
  axe.enchant(enchants.getOrThrow(Enchantments.UNBREAKING),3);
  axe.enchant(enchants.getOrThrow(Enchantments.MENDING),1);
  axe.enchant(enchants.getOrThrow(Enchantments.EFFICIENCY),5);
  axe.enchant(enchants.getOrThrow(Enchantments.FORTUNE),3);
  axe.enchant(enchants.getOrThrow(Enchantments.SILK_TOUCH),1);
  brute.setItemSlot(EquipmentSlot.MAINHAND,axe);
  brute.setDropChance(EquipmentSlot.MAINHAND,0);
  EquipmentSlot[] slots={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
  Item[] pieces={Items.NETHERITE_HELMET,Items.NETHERITE_CHESTPLATE,Items.NETHERITE_LEGGINGS,Items.NETHERITE_BOOTS};
  for(int i=0;i<slots.length;i++){
   ItemStack armor=new ItemStack(pieces[i]);
   armor.enchant(enchants.getOrThrow(Enchantments.PROTECTION),4);
   armor.enchant(enchants.getOrThrow(Enchantments.UNBREAKING),3);
   armor.enchant(enchants.getOrThrow(Enchantments.MENDING),1);
   brute.setItemSlot(slots[i],armor);
   brute.setDropChance(slots[i],0);
  }
 }
 private static EntityType<? extends Mob> skeletonType(ServerLevel level){
  int roll=level.random.nextInt(100);
  if(roll<29)return EntityType.SKELETON;
  if(roll<58)return EntityType.STRAY;
  if(roll<86)return EntityType.BOGGED;
  return EntityType.WITHER_SKELETON;
 }
 /** Original Sentinel encounter and reward tags remain unchanged. */
 private static void spawnSentinel(ServerLevel level,BlockPos pos,int floor,int layer){
  int roll=level.random.nextInt(100);
  if(roll<20){
   PiglinBrute brute=EntityType.PIGLIN_BRUTE.create(level);
   if(brute!=null){
    configure(level,brute,pos,floor,layer,false);
    // Prevent Temple Sentinel piglin brutes from zombifying outside the Nether.
    brute.setImmuneToZombification(true);
    equipBrute(level,brute);
    TempleCrew.prepare(level, brute);
    level.addFreshEntity(brute);
   }
  }else if(roll<28){
   Drowned drowned=EntityType.DROWNED.create(level);
   Guardian guardian=EntityType.GUARDIAN.create(level);
   if(drowned==null||guardian==null)return;
   configure(level,drowned,pos,floor,layer,true);
   configure(level,guardian,pos,floor,layer,false);
   TempleCrew.prepare(level,drowned); TempleCrew.prepare(level,guardian);
   if(level.addFreshEntity(drowned)&&level.addFreshEntity(guardian))guardian.startRiding(drowned,true);
  }else if(roll<52){
   Guardian guardian=EntityType.GUARDIAN.create(level);
   if(guardian!=null){configure(level,guardian,pos,floor,layer,false);guardian.setNoGravity(true);TempleCrew.prepare(level,guardian);level.addFreshEntity(guardian);}
  }else if(roll<76){
   Skeleton skeleton=EntityType.SKELETON.create(level);
   if(skeleton!=null){configure(level,skeleton,pos,floor,layer,true);skeleton.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(level.random.nextBoolean()?Items.NETHERITE_SWORD:Items.NETHERITE_AXE));skeleton.setDropChance(EquipmentSlot.MAINHAND,0);TempleCrew.prepare(level,skeleton);level.addFreshEntity(skeleton);}
  }else{
   Drowned drowned=EntityType.DROWNED.create(level);
   if(drowned!=null){configure(level,drowned,pos,floor,layer,true);TempleCrew.prepare(level,drowned);level.addFreshEntity(drowned);}
  }
 }
 private static void configure(ServerLevel level,Monster mob,BlockPos pos,int floor,int layer,boolean armor){
  mob.moveTo(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,level.random.nextFloat()*360,0);
  mob.getPersistentData().putInt("tota_sentinel_floor",floor);
  mob.getPersistentData().putInt("tota_sentinel_layer",layer);
  mob.getPersistentData().putBoolean("tota_sentinel_reward",true);
  mob.setCustomName(Component.literal(NAMES[level.random.nextInt(NAMES.length)]+", "+TITLES[level.random.nextInt(TITLES.length)]));
  mob.setCustomNameVisible(true);
  var hp=mob.getAttribute(Attributes.MAX_HEALTH);
  if(hp!=null)hp.setBaseValue(floor*50.0);
  mob.setHealth(floor*50.0f);
  var attack=mob.getAttribute(Attributes.ATTACK_DAMAGE);
  if(attack!=null)attack.setBaseValue(attack.getBaseValue()+1.0*floor);
  mob.setPersistenceRequired();
  if(armor){
   EquipmentSlot[] slots={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
   Item[] items={Items.DIAMOND_HELMET,Items.DIAMOND_CHESTPLATE,Items.DIAMOND_LEGGINGS,Items.DIAMOND_BOOTS};
   var trimMaterial=level.registryAccess().registryOrThrow(Registries.TRIM_MATERIAL).getHolderOrThrow(TrimMaterials.NETHERITE);
   var trimPattern=level.registryAccess().registryOrThrow(Registries.TRIM_PATTERN).getHolderOrThrow(TrimPatterns.SILENCE);
   for(int i=0;i<slots.length;i++){
    ItemStack piece=new ItemStack(items[i]);
    piece.set(DataComponents.TRIM,new ArmorTrim(trimMaterial,trimPattern));
    mob.setItemSlot(slots[i],piece);
    mob.setDropChance(slots[i],0);
   }
   if(mob instanceof Drowned){mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.TRIDENT));mob.setDropChance(EquipmentSlot.MAINHAND,0);}
  }
 }
 @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider provider){super.saveAdditional(tag,provider);tag.putInt("Tier",tier);tag.putInt("State",state);}
 @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider provider){super.loadAdditional(tag,provider);tier=Math.max(1,tag.getInt("Tier"));state=tag.getInt("State");}
}
