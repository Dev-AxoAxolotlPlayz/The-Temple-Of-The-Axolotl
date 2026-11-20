package com.devaxo.templeoftheaxolotl.entity;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.commands.Commands;
@EventBusSubscriber(modid="temple_of_the_axolotl")
public final class EldritchHudCleanup {
    private static final Map<UUID,Set<ServerBossEvent>> BARS=new HashMap<>();
    static void remember(ServerPlayer p,ServerBossEvent b){ BARS.computeIfAbsent(p.getUUID(),k->new HashSet<>()).add(b); }
    private static void clear(ServerPlayer p){var bars=BARS.remove(p.getUUID());if(bars==null)return;for(var b:bars){for(var old:List.copyOf(b.getPlayers()))if(old.getUUID().equals(p.getUUID()))b.removePlayer(old);p.connection.send(ClientboundBossEventPacket.createRemovePacket(b.getId()));}}
    @SubscribeEvent public static void death(LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("tota_dev").requires(s->s.hasPermission(2)).then(Commands.literal("eldritchwaters").then(Commands.literal("clearbars").executes(c->{clear(c.getSource().getPlayerOrException());c.getSource().getPlayerOrException().connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(net.minecraft.network.chat.Component.literal("Boss bars cleared")));return 1;}))));}
}
