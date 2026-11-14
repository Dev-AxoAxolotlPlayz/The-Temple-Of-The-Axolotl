package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Owner-led, non-destructive structural and furnishing inspections. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class WaifuAxoluthuBuildPhaseReview {
    private WaifuAxoluthuBuildPhaseReview() {}
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("tota_buildphase")
                .then(Commands.literal("attention").then(Commands.argument("plot",com.mojang.brigadier.arguments.IntegerArgumentType.integer(1)).executes(ctx -> {
                    ServerPlayer player=ctx.getSource().getPlayerOrException();
                    for(WaifuAxoluthuEntity axo:player.serverLevel().getEntitiesOfClass(WaifuAxoluthuEntity.class,player.getBoundingBox().inflate(48),a->player.getUUID().equals(a.getBondedPlayer()))) {
                        if(WaifuAxoluthuVillageRegistry.requestAttention(axo,com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx,"plot")))return 1;
                    }
                    return 0;
                })))
                .then(Commands.literal("boundary").then(Commands.argument("plot",com.mojang.brigadier.arguments.IntegerArgumentType.integer(1)).executes(ctx -> {
                    ServerPlayer player=ctx.getSource().getPlayerOrException();
                    return WaifuAxoluthuMaintenance.toggle(player,com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx,"plot"));
                })))
                .then(Commands.literal("review").executes(ctx -> action(ctx.getSource(), false)))
                .then(Commands.literal("approve").executes(ctx -> action(ctx.getSource(), true))));
    }
    public static void showControls(ServerPlayer player, String phase) {
        player.sendSystemMessage(Component.literal("[Axoluthu] Check the " + phase
                + "! Change anything you want; I'll learn from your edits! :3"));
        player.sendSystemMessage(Component.literal("[Approve " + phase + "]")
                .withStyle(Style.EMPTY.withColor(0x55FFAA)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tota_buildphase approve")))
                .append(Component.literal("   /tota_buildphase review")));
    }
    private static int action(CommandSourceStack source, boolean approve)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        for (WaifuAxoluthuEntity axo : player.serverLevel().getEntitiesOfClass(
                WaifuAxoluthuEntity.class, player.getBoundingBox().inflate(48),
                a -> player.getUUID().equals(a.getBondedPlayer()))) {
            CompoundTag learning = axo.getArchitecturalLearning();
            CompoundTag request = learning.getCompound("phase35OwnerSuggestion");
            if (!request.getBoolean("pending") || !request.hasUUID("owner")
                    || !player.getUUID().equals(request.getUUID("owner"))
                    || !player.level().dimension().location().toString().equals(request.getString("dimension"))) continue;
            String phase = request.getString("phase");
            boolean structure = "AWAITING_STRUCTURE_INSPECTION".equals(phase);
            boolean furnishing = "AWAITING_FURNISHING_INSPECTION".equals(phase);
            if (!structure && !furnishing) continue;
            if (!approve) { showControls(player, structure ? "structure" : "furnishing"); return 1; }
            if (furnishing && !WaifuAxoluthuWorkstationHandoff.allPlaced(player.serverLevel(), request)) {
                player.sendSystemMessage(Component.literal("[Axoluthu] Please place the workstations I gave you before finishing! :3"));
                return 0;
            }
            int changes = WaifuAxoluthuPlayerLessons.accept(player, player.serverLevel(), request, furnishing);
            if (structure) {
                request.putString("phase", "INSTALLING_FURNISHINGS");
                request.putBoolean("constructionAllowed", true);
                request.putBoolean("axoRepairPass", false);
                player.sendSystemMessage(Component.literal("[Axoluthu] I learned " + changes
                        + " structural changes! Now I'll make your workstations! :D"));
            } else {
                String type = WaifuAxoluthuVillageStructures.normalize(request.getString("type"));
                net.minecraft.core.BlockPos origin = net.minecraft.core.BlockPos.of(request.getLong("target"));
                int width = WaifuAxoluthuVillageStructures.width(type);
                if (!WaifuAxoluthuVillageRegistry.finalizeFurnished(axo, player.serverLevel(), request, type, origin, width)) {
                    player.sendSystemMessage(Component.literal("[Axoluthu] I need the entire building loaded before finishing!"));
                    return 0;
                }
                if ("workshop".equals(type) && !axo.isStage4WorkshopComplete()) {
                    axo.setStage4WorkshopOrigin(origin);
                    axo.finishStage4Workshop();
                }
                if ("storage".equals(type) && !axo.isSettlementStorageComplete()) {
                    axo.setSettlementStorageOrigin(origin);
                    axo.finishSettlementStorage();
                }
                request.putBoolean("pending", false);
                request.putBoolean("constructionAllowed", false);
                request.putString("phase", "COMPLETE");
                player.sendSystemMessage(Component.literal("[Axoluthu] I learned " + changes
                        + " furnishing changes! We finished the building together! ^u^"));
            }
            learning.put("phase35OwnerSuggestion", request);
            return 1;
        }
        player.sendSystemMessage(Component.literal("[Axoluthu] No building is awaiting player inspection. :3"));
        return 0;
    }
}
