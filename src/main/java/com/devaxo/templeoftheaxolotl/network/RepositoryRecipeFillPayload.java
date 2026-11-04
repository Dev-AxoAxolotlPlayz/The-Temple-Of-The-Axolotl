package com.devaxo.templeoftheaxolotl.network;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.repository.CelestialRepositoryMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Client sends only a recipe identifier. The server verifies all ingredient stock. */
public record RepositoryRecipeFillPayload(ResourceLocation recipeId) implements CustomPacketPayload {
    public static final Type<RepositoryRecipeFillPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "repository_recipe_fill"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RepositoryRecipeFillPayload> STREAM_CODEC =
        StreamCodec.of((buf, packet) -> buf.writeResourceLocation(packet.recipeId()),
                       buf -> new RepositoryRecipeFillPayload(buf.readResourceLocation()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, (packet, context) ->
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player
                    && player.containerMenu instanceof CelestialRepositoryMenu menu) {
                    if (!menu.fillFromBulk(player, packet.recipeId())) {
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            "Repository: cannot fill recipe (check ingredients and empty crafting grid)."), true);
                    }
                }
            }));
    }
}
