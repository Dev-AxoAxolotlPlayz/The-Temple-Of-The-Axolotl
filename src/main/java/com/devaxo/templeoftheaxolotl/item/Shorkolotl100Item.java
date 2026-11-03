package com.devaxo.templeoftheaxolotl.item;

import com.devaxo.templeoftheaxolotl.entity.ModEntities;
import com.devaxo.templeoftheaxolotl.entity.Shorkolotl100Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Boat-like water-only deployment item for the existing Shorkolotl-100 entity. */
public final class Shorkolotl100Item extends Item {
    public Shorkolotl100Item(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);
        Vec3 point = hit.getLocation();
        BlockPos waterPos = BlockPos.containing(point.subtract(0, 0.05, 0));
        if (!level.getFluidState(waterPos).is(FluidTags.WATER))
            return InteractionResultHolder.fail(stack);

        // Spawn slightly below the water surface, not on the ground.
        Shorkolotl100Entity submarine = ModEntities.SHORKOLOTL_100.get().create(level);
        if (submarine == null) return InteractionResultHolder.fail(stack);
        // Move the deployment one block horizontally in the player's facing direction.
        Vec3 forward = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (forward.lengthSqr() < 1.0E-6D) return InteractionResultHolder.fail(stack);
        Vec3 spawn = point.add(forward.normalize());
        BlockPos spawnWater = BlockPos.containing(spawn.subtract(0.0D, 0.05D, 0.0D));
        if (!level.getFluidState(spawnWater).is(FluidTags.WATER))
            return InteractionResultHolder.fail(stack);
        // Face away from the deploying player, like a placed boat.
        // The geometry's cockpit points toward its negative-Z end.
        float deploymentYaw = net.minecraft.util.Mth.wrapDegrees(player.getYRot() + 180.0F);
        submarine.moveTo(spawn.x, spawn.y - 0.6D, spawn.z, deploymentYaw, 0.0F);
        if (!level.noCollision(submarine) ||
                !level.getEntities(submarine, submarine.getBoundingBox(), e -> e.isPickable()).isEmpty())
            return InteractionResultHolder.fail(stack);

        if (!level.isClientSide) {
            if (!level.addFreshEntity(submarine)) return InteractionResultHolder.fail(stack);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
