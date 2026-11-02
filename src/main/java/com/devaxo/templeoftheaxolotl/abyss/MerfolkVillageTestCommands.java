package com.devaxo.templeoftheaxolotl.abyss;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Dev-only village preview. Run in an empty area, as it overwrites blocks. */
public final class MerfolkVillageTestCommands {
    private MerfolkVillageTestCommands() {}
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher=event.getDispatcher();
        dispatcher.register(Commands.literal("tota")
            .then(Commands.literal("merfolk")
                .then(Commands.literal("village")
                    .then(Commands.literal("test")
                        .requires(source->source.hasPermission(2))
                        .executes(context->{
                            CommandSourceStack source=context.getSource();
                            ServerLevel level=source.getLevel();
                            if(!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
                                source.sendFailure(Component.literal("Run this preview in the Overworld."));
                                return 0;
                            }
                            BlockPos center=BlockPos.containing(source.getPosition()).above(10);
                            long seed=level.getRandom().nextLong();
                            int count=AbyssMerfolkVillages.preview(level,center,seed);
                            source.sendSuccess(()->Component.literal("Generated "+count+" floating merfolk homes near "
                                +center.toShortString()+". Preview overwrites blocks; use an empty area."),true);
                            return count;
                        })))));
    }
}
