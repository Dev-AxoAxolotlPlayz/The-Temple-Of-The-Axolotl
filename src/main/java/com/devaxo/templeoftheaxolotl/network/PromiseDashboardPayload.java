package com.devaxo.templeoftheaxolotl.network;
import com.devaxo.templeoftheaxolotl.promise.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
@EventBusSubscriber(modid="temple_of_the_axolotl")
public record PromiseDashboardPayload(boolean open, int upgrade, CompoundTag settings) implements CustomPacketPayload {
 public static final Type<PromiseDashboardPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl","promise_dashboard"));
 public static final StreamCodec<RegistryFriendlyByteBuf,PromiseDashboardPayload> CODEC=StreamCodec.of((b,v)->{b.writeBoolean(v.open);b.writeVarInt(v.upgrade);b.writeNbt(v.settings);},b->new PromiseDashboardPayload(b.readBoolean(),b.readVarInt(),b.readNbt()));
 public Type<? extends CustomPacketPayload> type(){return TYPE;}
 public static void send(ServerPlayer p,boolean open){if(PromiseState.installed(p))PacketDistributor.sendToPlayer(p,new PromiseDashboardPayload(open,PromiseUpgrade.level(p),PromiseState.settings(p)));}
 @SubscribeEvent public static void register(RegisterPayloadHandlersEvent e){e.registrar("1").playToClient(TYPE,CODEC,(v,c)->c.enqueueWork(()->com.devaxo.templeoftheaxolotl.client.PromiseDashboard.receive(v)));}
}
