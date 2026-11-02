package com.devaxo.templeoftheaxolotl.maze;

import com.devaxo.templeoftheaxolotl.entity.BossWrathEntity;
import com.devaxo.templeoftheaxolotl.entity.ModEntities;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

/** Origin-only, persistent two-ending rescue. Does not call the regular Temple victory handler. */
public final class OriginSanctumRescue {
    private static final String FORGIVENESS = "Sorry I hurt you, but i also forgive you";
    private static final Map<UUID, Long> EXTRACT_AT = new HashMap<>();
    private static MinecraftServer lastServer;
    private static final long PREPARATION_AND_EXTRACTION = 200 + 600;

    private static boolean isCopperRewardChest(ServerLevel level, BlockPos pos) {
        String id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getPath();
        return id.contains("waxed_weathered_copper_chest") && level.getBlockEntity(pos) instanceof Container;
    }

    /** Search only the authored 60x48x60 Sanctum template, never nearby ordinary chests. */
    private static BlockPos findChest(ServerLevel level, BlockPos origin) {
        BlockPos found = null;
        for (BlockPos p : BlockPos.betweenClosed(origin, origin.offset(59, 47, 59))) {
            if (isCopperRewardChest(level, p)) {
                if (found != null) return null; // Ambiguous reward chest: do not populate the wrong inventory.
                found = p.immutable();
            }
        }
        return found;
    }

    /** Idempotent, server-side, applies exactly one set of rewards per generated Origin. */
    public static boolean resolve(ServerLevel level, BossWrathEntity boss, boolean forgiven) {
        if (!boss.isOriginWrath() || !level.dimension().equals(OriginTempleState.DIMENSION)) return false;
        OriginTempleState state = OriginTempleState.get(level);
        if (state.originResolved()) return false;
        BlockPos chestPos = findChest(level, boss.getSanctumTemplateOrigin());
        if (chestPos == null || !(level.getBlockEntity(chestPos) instanceof Container chest)) {
            return false;
        }
        int needed = forgiven ? 3 : 2;
        int free = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) if (chest.getItem(i).isEmpty()) free++;
        if (free < needed) return false;
        ItemStack[] rewards = forgiven
                ? new ItemStack[]{new ItemStack(ModItems.CELESTIAL_KEY.get()), new ItemStack(Items.GOLDEN_CARROT), new ItemStack(Items.SOUL_CAMPFIRE)}
                : new ItemStack[]{new ItemStack(ModItems.CELESTIAL_KEY.get()), new ItemStack(Items.GOLDEN_CARROT)};
        int index = 0;
        for (int i = 0; i < chest.getContainerSize() && index < rewards.length; i++)
            if (chest.getItem(i).isEmpty()) chest.setItem(i, rewards[index++]);
        chest.setChanged();
        state.resolve(forgiven, chestPos);
        for (ServerPlayer player : level.players())
            player.sendSystemMessage(Component.literal(forgiven
                    ? "Origin Wrath has accepted forgiveness. The prison chest now holds the rescue offerings."
                    : "Origin Wrath has fallen. The prison chest now holds the Celestial Key and Golden Carrot."));
        return true;
    }

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        if (!FORGIVENESS.equals(event.getRawText())) return;
        ServerPlayer player = event.getPlayer();
        if (!player.serverLevel().dimension().equals(OriginTempleState.DIMENSION)) return;
        ServerLevel level = player.serverLevel();
        OriginTempleState state = OriginTempleState.get(level);
        if (state.originResolved()) return;
        if (state.triggeringPlayer() == null || !state.triggeringPlayer().equals(player.getUUID())) return;
        if (state.lockedWrathDefeats() > 15) {
            player.sendSystemMessage(Component.literal("Origin Wrath refuses forgiveness."));
            return;
        }
        List<BossWrathEntity> bosses = level.getEntitiesOfClass(BossWrathEntity.class,
                player.getBoundingBox().inflate(96), b -> b.isOriginWrath() && b.isAlive() && !b.isOriginResolved());
        if (bosses.size() != 1) return;
        BossWrathEntity boss = bosses.get(0);
        var redeemed = ModEntities.TAMEABLE_WRATH.get().create(level);
        if (redeemed == null) return;
        if (!resolve(level, boss, true)) return;
        redeemed.moveTo(boss.getX(), boss.getY(), boss.getZ(), boss.getYRot(), 0);
        redeemed.addTag("origin_redeemed_wrath");
        level.addFreshEntity(redeemed);
        boss.resolveOriginByForgiveness();
        boss.discard();
        player.sendSystemMessage(Component.literal("Origin Wrath has been redeemed. Use the Soul Campfire to earn its trust."));
    }

    private static BlockPos doorBottom(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.IRON_DOOR)) return null;
        return state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    @SubscribeEvent
    public void onDoor(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        if (!level.dimension().equals(OriginTempleState.DIMENSION)) return;
        if (!event.getItemStack().is(ModItems.CELESTIAL_KEY.get())) return;
        OriginTempleState state = OriginTempleState.get(level);
        if (!state.originResolved() || state.rescueStarted() || state.rewardChest() == null) return;
        BlockPos bottom = doorBottom(level, event.getPos());
        if (bottom == null || bottom.distSqr(state.rewardChest()) > 30 * 30) return;
        BlockState first = level.getBlockState(bottom);
        Direction facing = first.getValue(DoorBlock.FACING);
        BlockPos other = null;
        for (Direction side : new Direction[]{facing.getClockWise(), facing.getCounterClockWise()}) {
            BlockPos candidate = bottom.relative(side);
            BlockState adjacent = level.getBlockState(candidate);
            if (adjacent.is(Blocks.IRON_DOOR) && adjacent.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                    && adjacent.getValue(DoorBlock.FACING) == facing) { other = candidate; break; }
        }
        if (other == null || !level.getBlockState(bottom.above()).is(Blocks.IRON_DOOR)
                || !level.getBlockState(other.above()).is(Blocks.IRON_DOOR)) return;
        event.setCanceled(true);
        if (!player.getAbilities().instabuild) event.getItemStack().shrink(1);
        level.removeBlock(bottom.above(), false);
        level.removeBlock(bottom, false);
        level.removeBlock(other.above(), false);
        level.removeBlock(other, false);
        state.beginRescue();
        long finish = level.getServer().overworld().getGameTime() + PREPARATION_AND_EXTRACTION;
        for (ServerPlayer participant : level.players()) {
            if (!participant.isSpectator()) {
                EXTRACT_AT.put(participant.getUUID(), finish);
                participant.sendSystemMessage(Component.literal("Origin Temple SUCCESS! Preparation: 10 seconds, extraction: 30 seconds. Tamed companions will accompany you."));
            }
        }
    }

    @SubscribeEvent
    public void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (lastServer != server) { lastServer = server; EXTRACT_AT.clear(); }
        long now = server.overworld().getGameTime();
        for (var entry : new HashMap<>(EXTRACT_AT).entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !player.serverLevel().dimension().equals(OriginTempleState.DIMENSION)) {
                EXTRACT_AT.remove(entry.getKey()); continue;
            }
            long remaining = entry.getValue() - now;
            if (remaining > 0) {
                player.displayClientMessage(Component.literal(remaining > 600
                        ? "Origin rescue preparation: " + ((remaining - 600 + 19) / 20) + "s"
                        : "Origin extraction: " + ((remaining + 19) / 20) + "s"), true);
                continue;
            }
            if (extractWithCompanions(player)) EXTRACT_AT.remove(entry.getKey());
        }
    }

    private static boolean extractWithCompanions(ServerPlayer player) {
        ServerLevel origin = player.serverLevel();
        List<TamableAnimal> companions = origin.getEntitiesOfClass(TamableAnimal.class,
                player.getBoundingBox().inflate(160), pet -> pet.isTame() && player.getUUID().equals(pet.getOwnerUUID()));
        if (!OriginTempleTravel.devExit(player)) return false;
        ServerLevel destination = player.serverLevel();
        int i = 0;
        for (TamableAnimal pet : companions) {
            double angle = i++ * 2.39996;
            double x = player.getX() + Math.cos(angle) * (2 + (i / 8));
            double z = player.getZ() + Math.sin(angle) * (2 + (i / 8));
            BlockPos surface = destination.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    BlockPos.containing(x, player.getY(), z));
            pet.teleportTo(destination, surface.getX() + .5, surface.getY(), surface.getZ() + .5,
                    Set.of(), pet.getYRot(), pet.getXRot());
        }
        player.sendSystemMessage(Component.literal("Origin rescue complete. Your tamed companions have been brought home."));
        return true;
    }
}
