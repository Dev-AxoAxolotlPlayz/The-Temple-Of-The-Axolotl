package com.devaxo.templeoftheaxolotl.network;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.loot.TempleRewardMenus;
import com.devaxo.templeoftheaxolotl.compass.TempleHudModules;
import com.devaxo.templeoftheaxolotl.repository.CelestialRepositoryMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Server-authoritative inventory shortcuts. 0 = Repository; 1 = dungeon reward vault. */
public record InventoryStorageOpenPayload(int destination) implements CustomPacketPayload {
    public static final Type<InventoryStorageOpenPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "inventory_storage_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InventoryStorageOpenPayload> STREAM_CODEC =
            StreamCodec.of((buf, packet) -> buf.writeVarInt(packet.destination()),
                    buf -> new InventoryStorageOpenPayload(buf.readVarInt()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static final class Registration {
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToServer(TYPE, STREAM_CODEC, (packet, context) ->
                    context.enqueueWork(() -> {
                        if (!(context.player() instanceof ServerPlayer player)) return;
                        if (packet.destination() == 0) {
                            player.openMenu(new SimpleMenuProvider((id, inv, who) ->
                                    new CelestialRepositoryMenu(id, inv), Component.literal("Celestial Repository")));
                        } else if (packet.destination() == 1) {
                            TempleRewardMenus.open(player);
                        } else if (packet.destination() == 2) {
                            TempleHudModules.open(player);
                        } else if (packet.destination() == 4 &&
                                com.devaxo.templeoftheaxolotl.entity.ClucklesRegistry.get(player.serverLevel()).has(player.getUUID())) {
                            player.openMenu(new SimpleMenuProvider((id, inv, who) ->
                                new com.devaxo.templeoftheaxolotl.compass.CelestialModulesMenu(id, inv), Component.literal("Cluckles Controls")));
                        }
                    }));
        }
    }
}
