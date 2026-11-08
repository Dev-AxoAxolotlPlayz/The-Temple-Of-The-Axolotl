package com.devaxo.templeoftheaxolotl.network;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record CelestialRadialOpenBookPayload(int index) implements CustomPacketPayload {
    public static final Type<CelestialRadialOpenBookPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID,"celestial_radial_open_book"));
    public static final StreamCodec<RegistryFriendlyByteBuf,CelestialRadialOpenBookPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, CelestialRadialOpenBookPayload::index, CelestialRadialOpenBookPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC, (packet,context) -> context.enqueueWork(() -> {
            if (packet.index() == 0) com.devaxo.templeoftheaxolotl.client.DiaryOpeningScreen.open();
            if (packet.index() == 1) vazkii.patchouli.api.PatchouliAPI.get().openBookGUI(ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID,"axolotls_field_guide"));
            if (packet.index() == 2) com.devaxo.templeoftheaxolotl.client.AbyssalCommandBookScreen.open(new net.minecraft.world.item.ItemStack(com.devaxo.templeoftheaxolotl.item.ModItems.NOODLE_COMMAND_BOOK.get()));
        }));
    }
}
