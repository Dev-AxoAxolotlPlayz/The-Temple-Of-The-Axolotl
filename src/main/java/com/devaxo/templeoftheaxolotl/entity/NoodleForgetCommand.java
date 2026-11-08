package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class NoodleForgetCommand {
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tota_noodle_forget")
            .then(Commands.argument("phrase", StringArgumentType.greedyString())
                .executes(ctx -> {
                    ServerPlayer player=ctx.getSource().getPlayerOrException();
                    String phrase=StringArgumentType.getString(ctx,"phrase");
                    if (!NoodleCommandLibrary.forget(player.getUUID(),phrase)) {
                        player.sendSystemMessage(Component.literal("That command was not in your saved library."));
                        return 0;
                    }
                    NoodleTraining.syncBooks(player);
                    player.sendSystemMessage(Component.literal("Forgot Abyssal Axolotl command: "+phrase));
                    return 1;
                })));
    }
}
