package com.devaxo.templeoftheaxolotl.network;
import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
public record RingFlightStatePayload(boolean active) implements CustomPacketPayload {
    public static final Type<RingFlightStatePayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID,"ring_flight_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf,RingFlightStatePayload> STREAM_CODEC=StreamCodec.composite(ByteBufCodecs.BOOL,RingFlightStatePayload::active,RingFlightStatePayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void handle(RingFlightStatePayload packet, IPayloadContext ctx){ctx.enqueueWork(()->com.devaxo.templeoftheaxolotl.client.RingAttackInput.flightState(packet.active()));}
}
