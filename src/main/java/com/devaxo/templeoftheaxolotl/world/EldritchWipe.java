package com.devaxo.templeoftheaxolotl.world;
import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.block.EldritchWatersPortalBlock;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import java.nio.file.*;
import java.util.*;
import java.io.IOException;

/** Temporary developer reset. Never removes files while the level is open. */
@EventBusSubscriber(modid="temple_of_the_axolotl")
public final class EldritchWipe {
    private static Path marker(MinecraftServer s){return s.getWorldPath(LevelResource.ROOT).resolve("tota_eldritch_wipe.pending");}
    public static boolean pending(MinecraftServer s){return Files.isRegularFile(marker(s));}
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("tota_dev").requires(s->s.hasPermission(2)).then(Commands.literal("eldritchwaters").then(Commands.literal("wipe")
            .executes(c->{c.getSource().sendFailure(Component.literal("Back up the world. Use /tota_dev eldritchwaters wipe confirm, then close the world or stop the server."));return 0;})
            .then(Commands.literal("confirm").executes(c->{
                var src=c.getSource();var server=src.getServer();var level=server.getLevel(EldritchWatersPortalBlock.DIMENSION);
                if(level==null)return 0;
                for(ServerPlayer p:List.copyOf(level.players())){
                    var d=p.getPersistentData();var id=net.minecraft.resources.ResourceLocation.tryParse(d.getString("TotaEldritchReturnDimension"));
                    var dest=id==null?null:server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,id));
                    if(dest==null || EldritchEncounter.arena(dest)) { dest=server.overworld();var pos=dest.getSharedSpawnPos();if(!p.teleportTo(dest,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,Set.of(),p.getYRot(),p.getXRot()))return 0; }
                    else if(!p.teleportTo(dest,d.getDouble("TotaEldritchReturnX"),d.getDouble("TotaEldritchReturnY"),d.getDouble("TotaEldritchReturnZ"),Set.of(),p.getYRot(),p.getXRot()))return 0;
                }
                if(!level.players().isEmpty())return 0;
                try{Files.writeString(marker(server),"Eldritch Waters only\n");}catch(IOException ex){src.sendFailure(Component.literal("Cannot queue wipe: "+ex.getMessage()));return 0;}
                src.sendSuccess(()->Component.literal("Eldritch Waters wipe queued. Stop server / close world, then reopen. Portal entry blocked until reset."),true);return 1;
            })))));
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){
        var s=e.getServer();if(!pending(s))return;
        Path root=s.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        Path target=root.resolve("dimensions/temple_of_the_axolotl/eldritch_waters");
        try{
            for(Path p=target;!p.equals(root);p=p.getParent())if(Files.isSymbolicLink(p))throw new IOException("Refusing symbolic link: "+p);
            if(Files.exists(target))try(var walk=Files.walk(target)){for(Path p:walk.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}
            Files.deleteIfExists(marker(s));
            TempleOfTheAxolotl.LOGGER.warn("Eldritch Waters wiped after server stop: {}",target);
        }catch(IOException ex){TempleOfTheAxolotl.LOGGER.error("Eldritch Waters wipe failed; marker retained",ex);}
    }
}
