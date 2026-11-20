package com.devaxo.templeoftheaxolotl.network;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
@EventBusSubscriber(modid="temple_of_the_axolotl")
public record CyanHeartsPayload(float health) implements CustomPacketPayload {
    public static final Type<CyanHeartsPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl","cyan_hearts"));
    public static final StreamCodec<RegistryFriendlyByteBuf,CyanHeartsPayload> CODEC=StreamCodec.of((b,v)->b.writeFloat(v.health()),b->new CyanHeartsPayload(b.readFloat()));
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void send(ServerPlayer p,float value){PacketDistributor.sendToPlayer(p,new CyanHeartsPayload(value));}
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent e){e.registrar("1").playToClient(TYPE,CODEC,(v,c)->c.enqueueWork(()->com.devaxo.templeoftheaxolotl.client.CyanHeartsHud.receive(v.health())));}
}
