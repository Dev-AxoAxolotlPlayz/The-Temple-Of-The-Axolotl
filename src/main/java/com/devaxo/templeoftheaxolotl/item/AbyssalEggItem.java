package com.devaxo.templeoftheaxolotl.item;

import com.devaxo.templeoftheaxolotl.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;

/** Survival summoning item; deliberately not a vanilla spawn egg. */
public final class AbyssalEggItem extends Item {
    public AbyssalEggItem(Properties properties) { super(properties); }

    @Override public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(Blocks.CONDUIT)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel server) || context.getPlayer() == null) return InteractionResult.FAIL;
        var player = context.getPlayer();
        if (!server.dimension().location().toString().equals("temple_of_the_axolotl:ethereal_abyss")
                || !(server.getBlockEntity(pos) instanceof ConduitBlockEntity conduit)
                || !conduit.isActive() || !fullFrame(server, pos)) {
            player.displayClientMessage(Component.literal("The Abyssal Egg requires an active, fully powered conduit in the Ethereal Abyss."), true);
            return InteractionResult.FAIL;
        }
        var axo = ModEntities.WAIFU_AXOLUTHU.get().create(server);
        if (axo == null) return InteractionResult.FAIL;
        // Prefer clear space near the player, outside the conduit frame.
        boolean placed = false;
        BlockPos base = player.blockPosition();
        for (int dy = 0; dy <= 2 && !placed; dy++) {
            for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                var candidate = base.relative(direction, 2).above(dy);
                axo.moveTo(candidate.getX() + .5, candidate.getY(), candidate.getZ() + .5, player.getYRot(), 0);
                if (server.noCollision(axo)) { placed = true; break; }
            }
        }
        if (!placed || !server.addFreshEntity(axo)) {
            player.displayClientMessage(Component.literal("There is not enough room for Axoluthu to appear."), true);
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    private static boolean fullFrame(ServerLevel level, BlockPos center) {
        // Vanilla conduit frame: union of the three 5x5 perimeter rings (42 positions).
        for (int x = -2; x <= 2; x++) for (int y = -2; y <= 2; y++) for (int z = -2; z <= 2; z++) {
            int ax = Math.abs(x), ay = Math.abs(y), az = Math.abs(z);
            if (ax <= 1 && ay <= 1 && az <= 1) {
                if (!level.isWaterAt(center.offset(x, y, z))) return false;
            }
            boolean frame = (x == 0 && (ay == 2 || az == 2))
                    || (y == 0 && (ax == 2 || az == 2))
                    || (z == 0 && (ax == 2 || ay == 2));
            if (!frame) continue;
            var state = level.getBlockState(center.offset(x, y, z));
            if (!state.is(Blocks.PRISMARINE) && !state.is(Blocks.PRISMARINE_BRICKS)
                    && !state.is(Blocks.DARK_PRISMARINE) && !state.is(Blocks.SEA_LANTERN)) return false;
        }
        return true;
    }
}
