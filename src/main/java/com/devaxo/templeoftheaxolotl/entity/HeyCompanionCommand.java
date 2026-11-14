package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import java.util.*;

/** Owner-only, contextual shortcuts into the existing natural-language handlers. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class HeyCompanionCommand {
    private HeyCompanionCommand() {}
    private static final List<String> NOODLE_ACTIONS = List.of("follow", "sit", "stop", "protect", "fetch_logs");
    private static final List<String> AXO_ACTIONS = List.of("journal", "build");

    private static List<AbyssalAxolotlEntity> noodles(ServerPlayer p) {
        return p.serverLevel().getEntitiesOfClass(AbyssalAxolotlEntity.class,
                p.getBoundingBox().inflate(32), a -> a.isAlive() && a.isTame() && a.isOwnedBy(p) && a.hasPermanentName());
    }
    private static List<WaifuAxoluthuEntity> axoluthus(ServerPlayer p) {
        return p.serverLevel().getEntitiesOfClass(WaifuAxoluthuEntity.class,
                p.getBoundingBox().inflate(48), a -> a.isAlive() && p.getUUID().equals(a.getBondedPlayer()));
    }
    private static boolean isAxo(String name, WaifuAxoluthuEntity a) {
        return name.equalsIgnoreCase(a.getName().getString()) || name.equalsIgnoreCase("Axoluthu");
    }
    private static final SuggestionProvider<CommandSourceStack> NAMES = (ctx, builder) -> {
        if (ctx.getSource().getEntity() instanceof ServerPlayer p) {
            List<String> names = new ArrayList<>();
            for (var a : noodles(p)) names.add(a.permanentName());
            for (var a : axoluthus(p)) names.add(a.getName().getString());
            return SharedSuggestionProvider.suggest(names, builder);
        }
        return builder.buildFuture();
    };
    private static final SuggestionProvider<CommandSourceStack> ACTIONS = (ctx, builder) -> {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer p)) return builder.buildFuture();
        String name = StringArgumentType.getString(ctx, "companion_name");
        for (var a : noodles(p)) if (a.permanentName().equalsIgnoreCase(name))
            return SharedSuggestionProvider.suggest(NOODLE_ACTIONS, builder);
        for (var a : axoluthus(p)) if (isAxo(name,a))
            return SharedSuggestionProvider.suggest(AXO_ACTIONS, builder);
        return builder.buildFuture();
    };
    private static final SuggestionProvider<CommandSourceStack> TYPES = (ctx, builder) -> {
        String action = StringArgumentType.getString(ctx, "action");
        if (action.equalsIgnoreCase("build"))
            return SharedSuggestionProvider.suggest(WaifuAxoluthuConstructionRequests.TYPES, builder);
        return builder.buildFuture();
    };

    private static int execute(CommandSourceStack source, String name, String action, String args)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        for (var a : noodles(p)) {
            if (!a.permanentName().equalsIgnoreCase(name)) continue;
            if (!NOODLE_ACTIONS.contains(action)) break;
            String phrase = switch (action) {
                case "follow" -> "follow me";
                case "sit" -> "sit";
                case "stop" -> "stop";
                case "protect" -> "protect me";
                case "fetch_logs" -> "fetch " + (args.isBlank() ? "16" : args.trim()) + " logs";
                default -> "";
            };
            if (action.equals("fetch_logs") && !args.isBlank() && !args.matches("[0-9]{1,4}")) {
                source.sendFailure(Component.literal("Use a numeric log count, up to 4096.")); return 0;
            }
            BabyNoodleChat.handleText(p, "hey " + a.permanentName() + " " + phrase, a.getUUID());
            return 1;
        }
        for (var a : axoluthus(p)) {
            if (!isAxo(name,a)) continue;
            if (action.equals("journal") && args.isBlank()) {
                com.devaxo.templeoftheaxolotl.network.AxoluthuJournalPayload.send(p); return 1;
            }
            if (action.equals("build") && WaifuAxoluthuConstructionRequests.TYPES.contains(args.trim().toLowerCase(Locale.ROOT))) {
                WaifuAxoluthuConstructionRequests.submitChatRequest(p,
                        "hey axoluthu what if we build a " + args.trim() + " here");
                return 1;
            }
            source.sendFailure(Component.literal("Use /hey <companion_name> build <building_type> or journal."));
            return 0;
        }
        source.sendFailure(Component.literal("No eligible owned companion with that name is nearby."));
        return 0;
    }
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("hey")
                .then(Commands.argument("companion_name", StringArgumentType.string()).suggests(NAMES)
                        .then(Commands.argument("action", StringArgumentType.word()).suggests(ACTIONS)
                                .executes(ctx -> execute(ctx.getSource(), StringArgumentType.getString(ctx,"companion_name"),
                                        StringArgumentType.getString(ctx,"action"), ""))
                                .then(Commands.argument("arguments", StringArgumentType.greedyString()).suggests(TYPES)
                                        .executes(ctx -> execute(ctx.getSource(), StringArgumentType.getString(ctx,"companion_name"),
                                                StringArgumentType.getString(ctx,"action"), StringArgumentType.getString(ctx,"arguments")))))));
    }
}
