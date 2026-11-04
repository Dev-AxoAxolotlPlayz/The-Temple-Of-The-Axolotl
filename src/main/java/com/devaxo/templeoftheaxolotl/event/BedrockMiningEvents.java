
package com.devaxo.templeoftheaxolotl.event;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(
        modid = TempleOfTheAxolotl.MODID
)
public class BedrockMiningEvents {

    private static final int REQUIRED_TICKS = 600;

    // Allows minor client/server packet timing differences.
    private static final int PACKET_TIMEOUT = 2;

    // Use a separate ID from vanilla's player mining animation.
    private static final int SUPERNOVA_BREAKER_ID_BASE = -1000000;

    private static final Map<UUID, MiningProgress> PROGRESS =
            new HashMap<>();

    private static class MiningProgress {

        final BlockPos target;
        final ServerLevel level;
        final int breakerId;

        int ticks;
        int lastInputTick;
        int lastCrackStage = -1;

        MiningProgress(
                BlockPos target,
                ServerLevel level,
                int lastInputTick,
                int breakerId
        ) {
            this.target = target.immutable();
            this.level = level;
            this.lastInputTick = lastInputTick;
            this.breakerId = breakerId;
            this.ticks = 0;
        }
    }

    /**
     * Gives Supernova its own breaker ID, separate
     * from the player's normal block breaking ID.
     */
    private static int getBreakerId(ServerPlayer player) {
        return SUPERNOVA_BREAKER_ID_BASE - player.getId();
    }

    /**
     * Removes excavation progress and clears the
     * corresponding client-side block crack overlay.
     */
    private static void resetProgress(ServerPlayer player) {

        MiningProgress progress =
                PROGRESS.remove(player.getUUID());

        if (progress != null) {
            progress.level.destroyBlockProgress(
                    progress.breakerId,
                    progress.target,
                    -1
            );
        }
    }

    public static void receiveInput(
            ServerPlayer player,
            boolean mining,
            BlockPos pos
    ) {
        if (!mining || !SupernovaEvents.hasSupernova(player)) {
            resetProgress(player);
            return;
        }

        if (!(player.level() instanceof ServerLevel level)
                || !level.getBlockState(pos).is(com.devaxo.templeoftheaxolotl.registry.ModBlocks.STAR_BLASTED_BEDROCK.get())) {
            resetProgress(player);
            return;
        }

        int now = player.getServer().getTickCount();

        MiningProgress progress =
                PROGRESS.get(player.getUUID());

        // Switching blocks starts a completely new excavation.
        if (progress == null
                || !progress.target.equals(pos)
                || progress.level != level) {

            resetProgress(player);

            PROGRESS.put(
                    player.getUUID(),
                    new MiningProgress(
                            pos,
                            level,
                            now,
                            getBreakerId(player)
                    )
            );

            return;
        }

        progress.lastInputTick = now;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MiningProgress progress =
                PROGRESS.get(player.getUUID());

        if (progress == null) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)
                || !player.isAlive()
                || player.isSpectator()
                || !SupernovaEvents.hasSupernova(player)) {

            resetProgress(player);
            return;
        }

        int now = player.getServer().getTickCount();

        if (now - progress.lastInputTick > PACKET_TIMEOUT) {
            resetProgress(player);
            return;
        }

        BlockPos pos = progress.target;

        if (!level.getBlockState(pos).is(com.devaxo.templeoftheaxolotl.registry.ModBlocks.STAR_BLASTED_BEDROCK.get())
                || !player.mayInteract(level, pos)) {

            resetProgress(player);
            return;
        }

        // Confirm that the player is still aiming at
        // the exact same bedrock block.
        HitResult hit = player.pick(
                player.blockInteractionRange(),
                0.0F,
                false
        );

        if (!(hit instanceof BlockHitResult blockHit)
                || !blockHit.getBlockPos().equals(pos)) {

            resetProgress(player);
            return;
        }

        // Fixed countdown independent of mining effects.
        progress.ticks++;

        /*
         * Minecraft uses crack stages 0 through 9.
         *
         * 600 ticks / 10 stages = 60 ticks per stage.
         * Each stage lasts approximately 3 seconds
         * at the normal server tick rate.
         */
        int crackStage = Math.min(
                9,
                (progress.ticks * 10) / REQUIRED_TICKS
        );

        // Only send updates when the stage changes.
        if (crackStage != progress.lastCrackStage) {

            level.destroyBlockProgress(
                    progress.breakerId,
                    pos,
                    crackStage
            );

            progress.lastCrackStage = crackStage;
        }

        if (progress.ticks < REQUIRED_TICKS) {
            return;
        }

        // Remove the crack overlay and excavation state.
        resetProgress(player);

        // Server-owned completion. Only transform the exact verified custom block.
        // Never attempt vanilla bedrock's unbreakable destroyBlock path.
        if (!level.getBlockState(pos).is(com.devaxo.templeoftheaxolotl.registry.ModBlocks.STAR_BLASTED_BEDROCK.get())
                || !player.mayInteract(level, pos)
                || !SupernovaEvents.hasSupernova(player)) return;
        if (level.removeBlock(pos, false) && !player.isCreative()) {
            Block.popResource(level, pos, new ItemStack(Blocks.BEDROCK));
        }
    }

    @SubscribeEvent
    public static void onLogout(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            resetProgress(player);
        }
    }
}
