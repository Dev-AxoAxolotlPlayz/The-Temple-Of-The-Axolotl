package com.devaxo.templeoftheaxolotl.pre07;

import com.devaxo.templeoftheaxolotl.event.BendsEvents;
import com.devaxo.templeoftheaxolotl.effect.ModEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/** Drink to cure existing Bends; otherwise behave as a normal placeable fluid bucket. */
public final class DrinkableDilutedIchorBucket extends BucketItem {
    public DrinkableDilutedIchorBucket(Fluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (BendsEvents.hasBends(player) || player.hasEffect(ModEffects.THE_BENDS)) {
            return ItemUtils.startUsingInstantly(level, player, hand);
        }
        return super.use(level, player, hand);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player) || !BendsEvents.hasBends(player)) return stack;
        if (!level.isClientSide()) BendsEvents.cure(player);
        return ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET), false);
    }
}
