package com.devaxo.templeoftheaxolotl.maze;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Stage 3.2 foundation: emerald-block registration, saturation, occupancy,
 * and logout return markers. Does not generate or dismantle the maze.
 *
 * Register once during common mod setup:
 *     NeoForge.EVENT_BUS.register(new TempleDimensionManager());
 *
 * Session integration MUST set a safe Overworld return position before teleporting
 * a player into the temple. See TODO in onLogin for return teleport integration.
 */
public final class TempleDimensionManager {
    public static final String IN_TEMPLE_TAG = "in_temple";
    public static final String RETURN_PENDING_TAG = "tota_return_pending";

    public static final ResourceKey<Level> TEMPLE_DIMENSION = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "temple_dimension"));

    private static final int SATURATION_INTERVAL_TICKS = 600;

    /** Server-only ephemeral membership. Cleared when a player logs out. */
    private final Set<UUID> activePlayers = new HashSet<>();

    /**
     * Called after the player physically stands on the emerald marker.
     * This is the ONLY place the in_temple tag is added.
     */
    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.serverLevel().dimension().equals(TEMPLE_DIMENSION)) return;

        BlockPos floor = player.blockPosition().below();
        if (!player.getTags().contains(IN_TEMPLE_TAG)
                && player.serverLevel().getBlockState(floor).is(Blocks.EMERALD_BLOCK)) {
            player.addTag(IN_TEMPLE_TAG);
            activePlayers.add(player.getUUID());
            // Saturation is an instant effect. Apply immediately on first check-in.
            giveSaturation(player);
        }

        if (!player.getTags().contains(IN_TEMPLE_TAG)) return;
        activePlayers.add(player.getUUID());

        if (player.tickCount % SATURATION_INTERVAL_TICKS == 0) {
            giveSaturation(player);
        }
    }

    private static void giveSaturation(ServerPlayer player) {
        // Amplifier 2 means Saturation III. Duration 1 tick for instant effect.
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 1, 2, false, false));
    }

    /**
     * Call on successful extraction or intercepted lethal-damage ejection,
     * before or during the safe teleport out of the temple.
     */
    public void finishVisit(ServerPlayer player) {
        player.removeTag(IN_TEMPLE_TAG);
        player.removeTag(RETURN_PENDING_TAG);
        activePlayers.remove(player.getUUID());
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.serverLevel().dimension().equals(TEMPLE_DIMENSION)
                && !player.getTags().contains(IN_TEMPLE_TAG)) return;

        player.removeTag(IN_TEMPLE_TAG);
        activePlayers.remove(player.getUUID());
        // Persistent command tag survives logout; used by onLogin.
        player.addTag(RETURN_PENDING_TAG);
        // A session controller should now check occupancy and begin cleanup
        // ONLY if generation/arrivals are settled and no active players remain.
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.getTags().contains(RETURN_PENDING_TAG)) return;

        // IMPORTANT: Do NOT clear RETURN_PENDING_TAG here until the session
        // controller has actually teleported the player to their saved
        // Overworld portal location. This manager does not know that location.
        // Integrate the safe return teleport in the next stage.
    }

    /** Number of online, tagged players physically in the temple dimension. */
    public int activeCount(MinecraftServer server) {
        int count = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.serverLevel().dimension().equals(TEMPLE_DIMENSION)
                    && player.getTags().contains(IN_TEMPLE_TAG)) {
                count++;
            }
        }
        return count;
    }

    public boolean hasActivePlayers(MinecraftServer server) {
        return activeCount(server) > 0;
    }
}
