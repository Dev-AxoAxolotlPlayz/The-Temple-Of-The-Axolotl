package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.Entity;

public class NoodleBucketItem extends Item {
    public NoodleBucketItem(Properties properties) { super(properties); }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) return InteractionResult.FAIL;
        ItemStack stack = context.getItemInHand();
        var noodle = ModEntities.ABYSSAL_AXOLOTL.get().create(level);
        if (noodle == null) return InteractionResult.FAIL;
        noodle.load(NoodleItems.bucketData(stack));
        noodle.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, context.getRotation(), 0);
        if (!level.addFreshEntity(noodle)) return InteractionResult.FAIL;
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            stack.shrink(1);
            if (context.getPlayer() != null) {
                ItemStack empty = new ItemStack(Items.BUCKET);
                if (!context.getPlayer().getInventory().add(empty)) context.getPlayer().drop(empty, false);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
