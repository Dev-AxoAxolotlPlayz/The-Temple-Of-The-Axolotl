package com.devaxo.templeoftheaxolotl.event;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Temporary, server-owned light block that follows held or module-equipped lanterns.
 *  Never replaces terrain or an existing light block (including another light system).
 */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class AbyssalLanternDynamicLighting {
    private record Placement(ServerLevel level, BlockPos pos, BlockState original, int brightness) { }
    private static final Map<UUID, Placement> PLACED = new HashMap<>();
    private AbyssalLanternDynamicLighting() { }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 4 != 0) return;
        UUID id = player.getUUID();
        boolean active = AbyssalLanternEvents.held(player) || AbyssalLanternEvents.moduleEnabled(player);
        ServerLevel level = player.serverLevel();
        BlockPos pos = BlockPos.containing(player.getX(), player.getEyeY() - 0.7, player.getZ());
        // Full 160-tick cycle, with 20-tick dwells at both extrema.
        int phase = (int)(level.getGameTime() % 160);
        double fraction = phase < 20 ? 0 : phase < 80 ? smooth((phase - 20) / 60.0)
                : phase < 100 ? 1 : smooth((160 - phase) / 60.0);
        boolean underwater = player.isUnderWater();
        int brightness = (underwater ? 13 : 10)
                + (int)Math.round(fraction * (underwater ? 2 : 3));
        Placement previous = PLACED.get(id);
        if (previous != null && (!active || previous.level != level || !previous.pos.equals(pos)
                || previous.brightness != brightness)) {
            restore(previous);
            PLACED.remove(id);
        }
        if (!active || PLACED.containsKey(id)) return;
        if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return;
        BlockState original = level.getBlockState(pos);
        if (!original.isAir() && !original.is(Blocks.WATER)) return;
        BlockState light = Blocks.LIGHT.defaultBlockState()
                .setValue(LightBlock.LEVEL, brightness)
                .setValue(LightBlock.WATERLOGGED, original.is(Blocks.WATER));
        if (level.setBlock(pos, light, 3)) PLACED.put(id, new Placement(level, pos, original, brightness));
    }

    private static double smooth(double x) { return x * x * (3 - 2 * x); }

    private static void restore(Placement placement) {
        ServerLevel level = placement.level;
        BlockPos pos = placement.pos;
        if (level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                && level.getBlockState(pos).is(Blocks.LIGHT)) {
            level.setBlock(pos, placement.original, 3);
        }
    }
}
