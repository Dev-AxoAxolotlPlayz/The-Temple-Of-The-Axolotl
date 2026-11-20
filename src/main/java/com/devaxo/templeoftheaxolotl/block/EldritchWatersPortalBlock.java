package com.devaxo.templeoftheaxolotl.block;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import java.util.Set;

/** Server-side entry and return for the Eldritch Waters encounter. */
public final class EldritchWatersPortalBlock extends Block {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "eldritch_waters"));
    private static final String RETURN_DIM = "TotaEldritchReturnDimension";
    private static final String RETURN_X = "TotaEldritchReturnX";
    private static final String RETURN_Y = "TotaEldritchReturnY";
    private static final String RETURN_Z = "TotaEldritchReturnZ";
    private static final BlockPos ARRIVAL = new BlockPos(0, 12, 0);

    public EldritchWatersPortalBlock(BlockBehaviour.Properties properties) { super(properties); }

    private void activate(Level level, Player player) {
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return;
        if (level.dimension().equals(DIMENSION)) {
            var data = sp.getPersistentData();
            if (!data.contains(RETURN_DIM)) {
                sp.displayClientMessage(Component.literal("No saved Eldritch Waters return location."), true);
                return;
            }
            ResourceLocation id = ResourceLocation.tryParse(data.getString(RETURN_DIM));
            if (id == null) return;
            ServerLevel target = sp.server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
            if (target == null) return;
            double x = data.getDouble(RETURN_X), y = data.getDouble(RETURN_Y), z = data.getDouble(RETURN_Z);
            if (sp.teleportTo(target, x, y, z, Set.of(), sp.getYRot(), sp.getXRot())) {
                data.remove(RETURN_DIM); data.remove(RETURN_X); data.remove(RETURN_Y); data.remove(RETURN_Z);
            }
            return;
        }
        ServerLevel destination = sp.server.getLevel(DIMENSION);
        if (destination == null) {
            sp.displayClientMessage(Component.literal("Eldritch Waters dimension is unavailable."), true);
            return;
        }
        if (com.devaxo.templeoftheaxolotl.world.EldritchWipe.pending(sp.server)) { sp.displayClientMessage(Component.literal("Eldritch Waters wipe pending; restart the world."),true); return; }
        // Flat worldgen supplies the water column and seabed; no giant synchronous block-fill.
        destination.getChunk(0, 0);
        BlockPos exitPos = ARRIVAL.below(2);
        if (!destination.getBlockState(exitPos).is(com.devaxo.templeoftheaxolotl.registry.ModBlocks.ELDRITCH_WATERS_PORTAL.get())) {
            destination.setBlock(exitPos, com.devaxo.templeoftheaxolotl.registry.ModBlocks.ELDRITCH_WATERS_PORTAL.get().defaultBlockState(), 3);
        }
        var data = sp.getPersistentData();
        data.putString(RETURN_DIM, level.dimension().location().toString());
        data.putDouble(RETURN_X, sp.getX()); data.putDouble(RETURN_Y, sp.getY()); data.putDouble(RETURN_Z, sp.getZ());
        if (!sp.teleportTo(destination, ARRIVAL.getX()+0.5, ARRIVAL.getY()+0.5, ARRIVAL.getZ()+0.5,
                Set.of(), sp.getYRot(), sp.getXRot())) return;
        com.devaxo.templeoftheaxolotl.world.EldritchEncounter.ensure(destination);
        sp.displayClientMessage(Component.literal("Eldritch Waters: defeat the summoned waves to expose each shield."), false);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        activate(level, player); return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        activate(level, player); return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
