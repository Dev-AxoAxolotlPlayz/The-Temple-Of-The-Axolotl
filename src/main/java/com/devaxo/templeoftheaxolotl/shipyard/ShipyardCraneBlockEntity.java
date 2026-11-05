package com.devaxo.templeoftheaxolotl.shipyard;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.HolderLookup;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
public class ShipyardCraneBlockEntity extends BlockEntity implements GeoBlockEntity, MenuProvider {
 private final SimpleContainer inventory = new SimpleContainer(81){@Override public void setChanged(){super.setChanged();ShipyardCraneBlockEntity.this.setChanged();}};
 private boolean pendingCargo;
 private String pendingOutputId = "";
 public boolean hasPendingCargo() { return pendingCargo; }
 public boolean hasMiniatureSubmarineCargo() { return pendingCargo && "shorkolotl_100_item".equals(pendingOutputId); }
 private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
 public ShipyardCraneBlockEntity(BlockPos pos,BlockState state){super(ShipyardRegistries.CRANE.get(),pos,state);}
 public Container items(){return inventory;}

 // Commit a position-sensitive assembly and suspend its output on the hook.
 public boolean startCraft() {
   if (!(level instanceof ServerLevel server) || pendingCargo) return false;
   ShipyardRecipes.Assembly recipe = ShipyardRecipes.match(inventory);
   if (recipe == null) return false;
   for (int i=0;i<81;i++) if (!inventory.getItem(i).isEmpty()) inventory.removeItem(i, 1);
   pendingOutputId = recipe.id();
   pendingCargo = true;
   setChanged();
   server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
   syncCargo(server);
   return true;
 }

 private net.minecraft.world.item.Item cargoItem() {
   for (ShipyardRecipes.Assembly recipe : ShipyardRecipes.all())
     if (recipe.id().equals(pendingOutputId)) return recipe.output();
   return Items.AIR;
 }

 // Hook center in world coordinates. FACING points along the crane's forward axis.
 // Model pivot and hook share a Y plane; use block center as the anchor for the test.
 public Vec3 hookPosition() {
   Direction forward = getBlockState().getValue(ShipyardCraneBlock.FACING);
   Direction left = forward.getCounterClockWise();
   return Vec3.atCenterOf(worldPosition).add(
       -4.25 * left.getStepX() - forward.getStepX() * (2.0 / 3.0),
       5.0 / 3.0,
       -4.25 * left.getStepZ() - forward.getStepZ() * (2.0 / 3.0));
 }

 private static final String HOOK_TAG = "tota_shipyard_test_hook";
 private static final String DISPLAY_TAG = "tota_shipyard_test_diamond";
 private static final String OWNER_POS = "tota_shipyard_controller_pos";
 private static final int CHECK_INTERVAL = 10;

 private boolean ownedBy(net.minecraft.world.entity.Entity entity) {
   return entity.getPersistentData().getLong(OWNER_POS) == worldPosition.asLong();
 }

 private List<ArmorStand> hooks(ServerLevel level) {
   return level.getEntitiesOfClass(ArmorStand.class, new AABB(hookPosition(), hookPosition()).inflate(24.0),
       e -> e.getTags().contains(HOOK_TAG) && ownedBy(e));
 }
 private List<ItemEntity> displays(ServerLevel level) {
   return level.getEntitiesOfClass(ItemEntity.class, new AABB(hookPosition(), hookPosition()).inflate(24.0),
       e -> e.getTags().contains(DISPLAY_TAG) && ownedBy(e));
 }

 public static void serverTick(Level level, BlockPos pos, BlockState state, ShipyardCraneBlockEntity be) {
   if (!(level instanceof ServerLevel server) || server.getGameTime() % CHECK_INTERVAL != 0) return;
   be.syncCargo(server);
 }

 private void syncCargo(ServerLevel server) {
   List<ArmorStand> stands = hooks(server);
   List<ItemEntity> items = displays(server);
   if (!pendingCargo) {
     stands.forEach(e -> e.discard());
     items.forEach(e -> e.discard());
     return;
   }
   if (!stands.isEmpty() && !items.isEmpty()) {
     // Keep the visual item attached to the fixed hook anchor.
     Vec3 anchor = hookPosition().add(0, -0.35, 0);
     ItemEntity display = items.get(0);
     net.minecraft.world.item.Item expected = cargoItem();
     if (!display.getItem().is(expected)) display.setItem(new ItemStack(expected));
     display.setInvisible(hasMiniatureSubmarineCargo());
     display.setNoGravity(true);
     display.setDeltaMovement(Vec3.ZERO);
     if (display.position().distanceToSqr(anchor) > 0.0001) display.setPos(anchor.x, anchor.y, anchor.z);
     for (int i = 1; i < items.size(); i++) items.get(i).discard();
     for (int i = 1; i < stands.size(); i++) stands.get(i).discard();
     return;
   }
   stands.forEach(e -> e.discard());
   items.forEach(e -> e.discard());
   Vec3 hook = hookPosition();

   ArmorStand target = EntityType.ARMOR_STAND.create(server);
   ItemEntity diamond = new ItemEntity(server, hook.x, hook.y - 0.35, hook.z, new ItemStack(cargoItem()));
   if (target == null) return;
   target.moveTo(hook.x, hook.y - 0.95, hook.z, 0, 0);
   target.setInvisible(true);
   target.setNoGravity(true);
   target.setInvulnerable(false); // AttackEntityEvent needs a damageable target
   target.addTag(HOOK_TAG);
   target.getPersistentData().putLong(OWNER_POS, worldPosition.asLong());
   diamond.setInvisible(hasMiniatureSubmarineCargo());
   diamond.setNoGravity(true);
   diamond.setDeltaMovement(Vec3.ZERO); // prevent random initial item velocity
   diamond.setInvulnerable(true);
   diamond.setPickUpDelay(32767);
   diamond.addTag(DISPLAY_TAG);
   diamond.getPersistentData().putLong(OWNER_POS, worldPosition.asLong());
   server.addFreshEntity(target);
   server.addFreshEntity(diamond);
 }

 // Called by the server-side AttackEntityEvent. Exactly one ingredient consumed.
 public void releaseCargo(net.minecraft.world.entity.player.Player player, ArmorStand hit) {
   if (!(level instanceof ServerLevel server) || !pendingCargo || !ownedBy(hit)) return;
   List<ItemEntity> items = displays(server);
   if (items.isEmpty()) return;
   pendingCargo = false;
   pendingOutputId = "";
   setChanged();
   server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
   hooks(server).forEach(e -> e.discard());
   ItemEntity diamond = items.get(0);
   diamond.removeTag(DISPLAY_TAG);
   diamond.setInvisible(false);
   diamond.setInvulnerable(false);
   diamond.setNoGravity(false);
   diamond.setPickUpDelay(10);
   diamond.setDeltaMovement(0, -0.08, 0);
   for (int i = 1; i < items.size(); i++) items.get(i).discard();
 }

 public void clearCargo() {
   if (level instanceof ServerLevel server) {
     hooks(server).forEach(e -> e.discard());
     displays(server).forEach(e -> e.discard());
   }
 }

 @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
   return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
 }
 @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
   CompoundTag tag = new CompoundTag();
   tag.putBoolean("PendingCargo", pendingCargo);
   tag.putString("PendingOutputId", pendingOutputId);
   return tag;
 }
 @Override public Component getDisplayName(){return Component.translatable("container.temple_of_the_axolotl.shipyard_crane");}
 @Override public AbstractContainerMenu createMenu(int id,Inventory player,Player owner){return new ShipyardMenu(id,player,inventory,worldPosition);}
 @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);tag.put("Items",inventory.createTag(registries));tag.putBoolean("PendingCargo",pendingCargo);tag.putString("PendingOutputId",pendingOutputId);}
 @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);inventory.fromTag(tag.getList("Items",10),registries);pendingCargo=tag.getBoolean("PendingCargo");pendingOutputId=tag.getString("PendingOutputId");}
 @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){}
 @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
