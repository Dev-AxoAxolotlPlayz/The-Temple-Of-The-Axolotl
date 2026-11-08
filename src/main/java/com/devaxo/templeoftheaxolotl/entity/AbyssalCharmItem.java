package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** One-use charm: summons a genuinely wild, unnamed Abyssal Axolotl. */
public final class AbyssalCharmItem extends Item {
    public AbyssalCharmItem(Properties properties) { super(properties); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResultHolder.success(held);
        BlockPos base = player.blockPosition();
        for (int radius = 1; radius <= 4; radius++) {
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos pos = base.offset(dx, dy, dz);
                    var axolotl = ModEntities.ABYSSAL_AXOLOTL.get().create(server);
                    if (axolotl == null) return InteractionResultHolder.fail(held);
                    axolotl.moveTo(pos.getX()+0.5, pos.getY(), pos.getZ()+0.5, player.getYRot(), 0);
                    if (!server.noCollision(axolotl) || !server.getBlockState(pos.below()).isFaceSturdy(server, pos.below(), net.minecraft.core.Direction.UP)) continue;
                    // No tame(), owner assignment, or permanent naming here.
                    if (server.addFreshEntity(axolotl)) {
                        if (!player.getAbilities().instabuild) held.shrink(1);
                        return InteractionResultHolder.consume(held);
                    }
                }
            }
        }
        return InteractionResultHolder.fail(held);
    }
}
