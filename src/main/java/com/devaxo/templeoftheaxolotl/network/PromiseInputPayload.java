package com.devaxo.templeoftheaxolotl.network;
import com.devaxo.templeoftheaxolotl.promise.PromiseInputState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
@EventBusSubscriber(modid="temple_of_the_axolotl")
public record PromiseInputPayload(boolean up, int forward, int strafe) implements CustomPacketPayload {
 public static final Type<PromiseInputPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl","promise_input"));
 public static final StreamCodec<RegistryFriendlyByteBuf,PromiseInputPayload> CODEC=StreamCodec.of((b,v)->{b.writeBoolean(v.up());b.writeByte(v.forward());b.writeByte(v.strafe());},b->new PromiseInputPayload(b.readBoolean(),b.readByte(),b.readByte()));
 public Type<? extends CustomPacketPayload> type(){return TYPE;}
 @SubscribeEvent public static void register(RegisterPayloadHandlersEvent e){e.registrar("1").playToServer(TYPE,CODEC,(v,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer p)PromiseInputState.receive(p,v.up(),v.forward(),v.strafe());}));}
}
