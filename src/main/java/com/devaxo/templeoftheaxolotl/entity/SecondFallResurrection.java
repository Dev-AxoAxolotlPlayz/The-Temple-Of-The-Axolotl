package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.item.ModItems;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

/** Owner-only, server-authoritative Second Fall redemption ritual. */
public final class SecondFallResurrection {
    private enum Stage { QUESTION, TRAVEL }
    private static final class Session {
        final UUID aboveId, ownerId;
        final ServerLevel level;
        final ItemStack ring;
        final boolean previouslySitting;
        Stage stage = Stage.QUESTION;
        BlockPos campfire;
        long deadline;
        Session(AxolotlAbsoluteEntity above, UUID owner, ItemStack stack, long deadline) {
            aboveId = above.getUUID(); ownerId = owner; level = (ServerLevel) above.level();
            ring = stack; previouslySitting = above.isOrderedToSit(); this.deadline = deadline;
        }
    }
    private final Map<UUID, Session> sessions = new HashMap<>();
    private MinecraftServer activeServer;
    private final Map<UUID, Long> retryAfter = new HashMap<>();
    // The player who actually tossed each ring, tracked without ItemEntity.setOwner().
    private final Map<UUID, UUID> ringDroppers = new HashMap<>();
    private static final String OFFERER_TAG = "tota_resurrection_offerer";


    @SubscribeEvent
    public void onToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player
                && event.getEntity().getItem().is(ModItems.RING_OF_THE_ONE.get())) {
            ringDroppers.put(event.getEntity().getUUID(), player.getUUID());
            // Persist the dropper identity on the actual item entity; UUID-only
            // maps can lose the association across entity reloads.
            event.getEntity().getPersistentData().putUUID(OFFERER_TAG, player.getUUID());
        }
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tota_resurrect_debug")
            .executes(ctx -> diagnose(ctx.getSource().getPlayerOrException())));
        event.getDispatcher().register(Commands.literal("tota_resurrect")
            .then(Commands.argument("answer", StringArgumentType.word())
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    String answer = StringArgumentType.getString(ctx, "answer");
                    return respond(player, answer);
                })));
    }

    private UUID offeringOwner(ItemEntity item) {
        var data = item.getPersistentData();
        if (data.hasUUID(OFFERER_TAG)) return data.getUUID(OFFERER_TAG);
        UUID cached = ringDroppers.get(item.getUUID());
        if (cached != null) return cached;
        var owner = item.getOwner();
        return owner != null ? owner.getUUID() : null;
    }

    /** On-demand diagnostics; no spam and no bypass of ownership/progression. */
    private int diagnose(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        player.sendSystemMessage(Component.literal("[TOTA Resurrection] Global Origin progression gate: disabled (owner-only ritual)."));
        player.sendSystemMessage(Component.literal("[TOTA Resurrection] Existing loaded owned Wrath: "
                + hasTamedWrath(player.getUUID(), server)));
        ServerLevel level = player.serverLevel();
        var aboves = level.getEntitiesOfClass(AxolotlAbsoluteEntity.class,
                player.getBoundingBox().inflate(16), e -> e.isAlive());
        player.sendSystemMessage(Component.literal("[TOTA Resurrection] Nearby Above All: " + aboves.size()));
        for (AxolotlAbsoluteEntity above : aboves) {
            player.sendSystemMessage(Component.literal("[TOTA Resurrection] Above All " + above.getUUID()
                    + " tame=" + above.isTame() + " ownerMatches="
                    + player.getUUID().equals(above.getOwnerUUID())
                    + " distance=" + String.format(java.util.Locale.ROOT, "%.1f", player.distanceTo(above))));
            var items = level.getEntitiesOfClass(ItemEntity.class,
                    above.getBoundingBox().inflate(3), e -> e.isAlive()
                            && e.getItem().is(ModItems.RING_OF_THE_ONE.get()));
            player.sendSystemMessage(Component.literal("[TOTA Resurrection] Nearby rings: " + items.size()));
            for (ItemEntity item : items) {
                player.sendSystemMessage(Component.literal("[TOTA Resurrection] Ring " + item.getUUID()
                        + " dropperMatches=" + player.getUUID().equals(offeringOwner(item))
                        + " tracked=" + (offeringOwner(item) != null)));
            }
        }
        return 1;
    }

    private int respond(ServerPlayer player, String answer) {
        Session session = sessions.get(player.getUUID());
        if (session == null || session.stage != Stage.QUESTION) return 0;
        AxolotlAbsoluteEntity above = findAbove(session);
        if (above == null || !player.getUUID().equals(above.getOwnerUUID())
                || player.serverLevel() != session.level) {
            abort(session, "The offering was returned.");
            return 0;
        }
        if ("no".equalsIgnoreCase(answer)) {
            player.sendSystemMessage(Component.literal("Above All: Owkay ^u^"));
            abort(session, null);
            return 1;
        }
        if (!"yes".equalsIgnoreCase(answer)) return 0;
        if (hasTamedWrath(player.getUUID(), player.getServer())) {
            abort(session, "You already have a tamed Wrath.");
            return 0;
        }
        BlockPos target = findCampfire(above);
        if (target == null) {
            abort(session, "Above All cannot find a lit soul campfire within 5 blocks.");
            return 0;
        }
        session.campfire = target;
        session.stage = Stage.TRAVEL;
        session.deadline = session.level.getGameTime() + 300;
        above.setOrderedToSit(false);
        above.setInSittingPose(false);
        above.getNavigation().moveTo(target.getX() + .5, target.getY(), target.getZ() + .5, 1.0);
        return 1;
    }

    @SubscribeEvent
    public void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) { activeServer = server; sessions.clear(); retryAfter.clear(); ringDroppers.clear(); }
        if (server.getTickCount() % 5 != 0) return;
        for (Session session : List.copyOf(sessions.values())) {
            AxolotlAbsoluteEntity above = findAbove(session);
            if (above == null || !above.isTame() || !session.ownerId.equals(above.getOwnerUUID())
                    || session.level.getGameTime() > session.deadline) {
                abort(session, "The resurrection ritual was interrupted.");
                continue;
            }
            if (session.stage == Stage.TRAVEL) {
                if (!session.level.getBlockState(session.campfire).is(Blocks.SOUL_CAMPFIRE)
                        || !session.level.getBlockState(session.campfire).getValue(net.minecraft.world.level.block.CampfireBlock.LIT)) {
                    abort(session, "The soul campfire is no longer lit.");
                    continue;
                }
                if (above.distanceToSqr(session.campfire.getX()+.5, session.campfire.getY()+.5, session.campfire.getZ()+.5) <= 4.0) {
                    complete(session, above, server);
                } else if (server.getTickCount() % 20 == 0) {
                    above.getNavigation().moveTo(session.campfire.getX()+.5, session.campfire.getY(), session.campfire.getZ()+.5, 1.0);
                }
            }
        }
        // The dropped item's owner UUID must match the tame owner's UUID.
        // Never guess ownership from proximity: multiplayer guests must not initiate.
        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                if (sessions.containsKey(player.getUUID()) || hasTamedWrath(player.getUUID(), server)) continue;
                for (AxolotlAbsoluteEntity above : level.getEntitiesOfClass(AxolotlAbsoluteEntity.class,
                        player.getBoundingBox().inflate(8), e -> e.isAlive() && e.isTame()
                            && player.getUUID().equals(e.getOwnerUUID()))) {
                    if (sessions.values().stream().anyMatch(s -> s.aboveId.equals(above.getUUID()))) continue;
                    for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class,
                            above.getBoundingBox().inflate(2), e -> e.isAlive()
                                && e.getItem().is(ModItems.RING_OF_THE_ONE.get())
                                && player.getUUID().equals(offeringOwner(e))
                                && level.getGameTime() >= retryAfter.getOrDefault(e.getUUID(), 0L))) {
                        ringDroppers.remove(item.getUUID());
                        ItemStack ring = item.getItem().copyWithCount(1);
                        ItemStack rest = item.getItem();
                        rest.shrink(1);
                        if (rest.isEmpty()) item.discard();
                        Session session = new Session(above, player.getUUID(), ring, level.getGameTime()+600);
                        sessions.put(player.getUUID(), session);
                        above.getNavigation().stop();
                        above.setOrderedToSit(true);
                        above.setInSittingPose(true);
                        above.triggerAnim("curiosity_controller", "tamed_curiosity");
                        player.sendSystemMessage(Component.literal("Above All: Can i bowwow this pwease"));
                        player.sendSystemMessage(Component.literal("[Yes]").withStyle(s -> s
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tota_resurrect yes")))
                            .append(Component.literal("  "))
                            .append(Component.literal("[No]").withStyle(s -> s
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tota_resurrect no")))));
                        break;
                    }
                }
            }
        }
    }

    private void complete(Session session, AxolotlAbsoluteEntity above, MinecraftServer server) {
        ServerPlayer owner = server.getPlayerList().getPlayer(session.ownerId);
        if (owner == null || hasTamedWrath(session.ownerId, server)) {
            abort(session, "Wrath is already tamed or the owner is offline.");
            return;
        }
        TameableWrathEntity wrath = ModEntities.TAMEABLE_WRATH.get().create(session.level);
        if (wrath == null) { abort(session, "Wrath could not be summoned."); return; }
        BlockPos fire = session.campfire;
        wrath.moveTo(fire.getX()+.5, fire.getY()+1, fire.getZ()+.5, 0, 0);
        wrath.tame(owner);
        wrath.setOrderedToSit(true);
        wrath.setInSittingPose(true);
        wrath.addTag("origin_redeemed_wrath");
        if (!session.level.addFreshEntity(wrath)) {
            abort(session, "Wrath could not be summoned.");
            return;
        }
        session.level.removeBlock(fire, false);
        session.level.broadcastEntityEvent(wrath, (byte) 7);
        wrath.triggerAnim("curiosity_controller", "tamed_curiosity");
        above.setOrderedToSit(session.previouslySitting);
        above.setInSittingPose(session.previouslySitting);
        sessions.remove(session.ownerId);
        owner.sendSystemMessage(Component.literal("Above All has brought Wrath home."));
    }

    private static BlockPos findCampfire(AxolotlAbsoluteEntity above) {
        BlockPos best = null;
        double distance = 25.01;
        for (BlockPos pos : BlockPos.betweenClosed(above.blockPosition().offset(-5,-5,-5),
                above.blockPosition().offset(5,5,5))) {
            var state = above.level().getBlockState(pos);
            if (!state.is(Blocks.SOUL_CAMPFIRE) || !state.getValue(net.minecraft.world.level.block.CampfireBlock.LIT)) continue;
            double d = pos.distSqr(above.blockPosition());
            if (d <= 25 && d < distance) { distance = d; best = pos.immutable(); }
        }
        return best;
    }

    private static boolean hasTamedWrath(UUID owner, MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            // Check all currently loaded entities across dimensions.
            for (var entity : level.getAllEntities()) {
                if (entity instanceof TameableWrathEntity wrath && wrath.isAlive()
                        && wrath.isTame() && owner.equals(wrath.getOwnerUUID())) return true;
            }
        }
        return false;
    }

    private AxolotlAbsoluteEntity findAbove(Session session) {
        var entity = session.level.getEntity(session.aboveId);
        return entity instanceof AxolotlAbsoluteEntity above && above.isAlive() ? above : null;
    }

    private void abort(Session session, String reason) {
        sessions.remove(session.ownerId);
        AxolotlAbsoluteEntity above = findAbove(session);
        if (above != null) {
            above.getNavigation().stop();
            above.setOrderedToSit(session.previouslySitting);
            above.setInSittingPose(session.previouslySitting);
        }
        ServerPlayer player = session.level.getServer().getPlayerList().getPlayer(session.ownerId);
        if (player != null && reason != null) player.sendSystemMessage(Component.literal(reason));
        if (!session.ring.isEmpty()) {
            var pos = player != null && player.serverLevel() == session.level ? player.position()
                    : above != null ? above.position() : session.level.getSharedSpawnPos().getCenter();
            ItemEntity returned = new ItemEntity(session.level, pos.x, pos.y, pos.z, session.ring.copy());
            // Returned rings are not new offerings; suppress re-triggering briefly.
            retryAfter.put(returned.getUUID(), session.level.getGameTime() + 100);
            session.level.addFreshEntity(returned);
        }
    }
}

