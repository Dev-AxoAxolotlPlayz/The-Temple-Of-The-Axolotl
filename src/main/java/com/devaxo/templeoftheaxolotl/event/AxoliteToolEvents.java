package com.devaxo.templeoftheaxolotl.event;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;

/** Axolite innate tool effects. All world mutations are server authoritative. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class AxoliteToolEvents {
    private AxoliteToolEvents() {}
    private record Replant(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos pos, BlockState crop, UUID player) {}
    private static final List<Replant> REPLANTS = new ArrayList<>();
    private static final Map<UUID, Integer> BLEED = new HashMap<>();
    @SubscribeEvent public static void damage(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || !player.getMainHandItem().is(ModItems.AXOLITE_SWORD.get()) || event.getNewDamage() <= 0) return;
        player.heal(event.getNewDamage() * 0.05F);
        if (player.getRandom().nextFloat() < 0.10F) BLEED.put(event.getEntity().getUUID(), 100);
    }
    @SubscribeEvent public static void bonemeal(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown()
                || !event.getItemStack().is(ModItems.AXOLITE_HOE.get()) || !(player.level() instanceof ServerLevel level)) return;
        BlockPos pos=event.getPos();
        ItemStack simulated = new ItemStack(Items.BONE_MEAL);
        if (BoneMealItem.applyBonemeal(simulated, level, pos, player)) {
            if (!player.getAbilities().instabuild) event.getItemStack().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            event.setCanceled(true);
        }
    }
    @SubscribeEvent public static void breakBlock(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) return;
        ItemStack tool=player.getMainHandItem(); BlockState state=event.getState(); BlockPos pos=event.getPos();
        if (tool.is(ModItems.AXOLITE_HOE.get()) && state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
            // Replant only when the player possesses planting material; no free seed duplication.
            ItemStack seed=new ItemStack(state.is(net.minecraft.world.level.block.Blocks.WHEAT) ? Items.WHEAT_SEEDS :
                    state.is(net.minecraft.world.level.block.Blocks.CARROTS) ? Items.CARROT :
                    state.is(net.minecraft.world.level.block.Blocks.POTATOES) ? Items.POTATO :
                    state.is(net.minecraft.world.level.block.Blocks.BEETROOTS) ? Items.BEETROOT_SEEDS : Items.AIR);
            if(seed.isEmpty()) return;
            for (int i=0;i<player.getInventory().getContainerSize();i++) {
                ItemStack slot=player.getInventory().getItem(i);
                if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot,seed)) {
                    if (!player.getAbilities().instabuild) slot.shrink(1);
                    REPLANTS.add(new Replant(level.dimension(),pos.immutable(),crop.defaultBlockState(),player.getUUID()));
                    break;
                }
            }
        }
        if (ModItems.isAxolitePickaxe(tool) && level.random.nextFloat() <
                (tool.is(ModItems.AXOLITE_PICKAXE_TIER_3.get()) ? 0.15F :
                 tool.is(ModItems.AXOLITE_PICKAXE_TIER_2.get()) ? 0.10F : 0.05F)) {
            boolean nether=state.is(net.minecraft.world.level.block.Blocks.NETHERRACK);
            boolean stone=state.is(net.minecraft.world.level.block.Blocks.STONE)
                    || state.is(net.minecraft.world.level.block.Blocks.DEEPSLATE);
            if (nether || stone) {
                net.minecraft.world.item.Item reward;
                if (nether) {
                    float roll=level.random.nextFloat();
                    reward=roll<0.75F?Items.GOLD_NUGGET:roll<0.85F?Items.QUARTZ:roll<0.97F?Items.GOLD_INGOT:Items.ANCIENT_DEBRIS;
                } else {
                    float roll=level.random.nextFloat();
                    if (roll<0.01F) reward=ModItems.STARDUST.get();
                    else if (roll<0.06F) {
                        net.minecraft.world.item.Item[] choices={Items.DIAMOND,Items.EMERALD,Items.IRON_INGOT,Items.GOLD_INGOT};
                        reward=choices[level.random.nextInt(choices.length)];
                    } else if (roll<0.21F) {
                        reward=level.random.nextBoolean()?Items.REDSTONE:Items.LAPIS_LAZULI;
                    } else if (roll<0.46F) {
                        reward=level.random.nextBoolean()?Items.GLOWSTONE_DUST:Items.COPPER_INGOT;
                    } else {
                        reward=level.random.nextBoolean()?Items.GOLD_NUGGET:Items.IRON_NUGGET;
                    }
                }
                int count=!nether && (reward==Items.GOLD_NUGGET || reward==Items.IRON_NUGGET)
                        ? 1+level.random.nextInt(3) : 1;
                Block.popResource(level,pos,new ItemStack(reward,count));
            }
        }
    }
    /** Sneak-breaking shovel-native blocks smelts their actual drops, using the world's recipe manager. */
    @SubscribeEvent public static void shovelAutosmelt(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player) || !player.isShiftKeyDown()
                || !(player.level() instanceof ServerLevel level)
                || !event.getTool().is(ModItems.AXOLITE_SHOVEL.get())
                || !event.getState().is(BlockTags.MINEABLE_WITH_SHOVEL)) return;
        for (var dropEntity : event.getDrops()) {
            ItemStack original=dropEntity.getItem();
            if (original.isEmpty()) continue;
            var recipe=level.getRecipeManager().getRecipeFor(
                    RecipeType.SMELTING, new SingleRecipeInput(original.copyWithCount(1)), level);
            if (recipe.isEmpty()) continue;
            ItemStack output=recipe.get().value().getResultItem(level.registryAccess()).copy();
            if (output.isEmpty()) continue;
            output.setCount(output.getCount()*original.getCount());
            dropEntity.setItem(output);
        }
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        var server=event.getServer();
        if (!REPLANTS.isEmpty()) {
            List<Replant> pending=new ArrayList<>(REPLANTS); REPLANTS.clear();
            for (Replant r:pending) {
                ServerLevel level=server.getLevel(r.dimension());
                if(level!=null && level.hasChunk(r.pos().getX()>>4,r.pos().getZ()>>4) && level.isEmptyBlock(r.pos()))
                    level.setBlock(r.pos(),r.crop(),3);
            }
        }
        Iterator<Map.Entry<UUID,Integer>> it=BLEED.entrySet().iterator();
        while(it.hasNext()) {
            var entry=it.next(); LivingEntity target=null;
            for(ServerLevel level:server.getAllLevels()) {
                var found=level.getEntity(entry.getKey());
                if(found instanceof LivingEntity living) {target=living;break;}
            }
            int remaining=entry.getValue()-1;
            if(target==null || !target.isAlive() || remaining<=0) {it.remove();continue;}
            if(remaining%20==0) target.hurt(target.damageSources().magic(),1.0F);
            entry.setValue(remaining);
        }
    }
}
