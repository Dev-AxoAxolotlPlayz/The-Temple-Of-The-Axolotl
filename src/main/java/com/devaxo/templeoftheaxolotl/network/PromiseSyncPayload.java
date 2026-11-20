package com.devaxo.templeoftheaxolotl.network;
import com.devaxo.templeoftheaxolotl.promise.PromiseSnapshot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import java.util.*;
@EventBusSubscriber(modid="temple_of_the_axolotl")
public record PromiseSyncPayload(UUID player, PromiseSnapshot snapshot) implements CustomPacketPayload {
    public static final Type<PromiseSyncPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl","promise_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PromiseSyncPayload> CODEC=StreamCodec.of((buf,v)->{
        buf.writeUUID(v.player()); buf.writeBoolean(v.snapshot().equipped()); buf.writeBoolean(v.snapshot().mobility()); buf.writeBoolean(v.snapshot().mining()); buf.writeBoolean(v.snapshot().combat()); buf.writeVarInt(v.snapshot().upgrade());
        buf.writeVarInt(v.snapshot().contacts().size()); for(var c:v.snapshot().contacts()){buf.writeVarInt(c.arm());buf.writeDouble(c.point().x);buf.writeDouble(c.point().y);buf.writeDouble(c.point().z);buf.writeByte(c.face()); }
    },buf->{UUID id=buf.readUUID();boolean equipped=buf.readBoolean(),mobility=buf.readBoolean();boolean mining=buf.readBoolean(),combat=buf.readBoolean();int upgrade=buf.readVarInt();int n=Math.min(8,Math.max(0,buf.readVarInt()));List<PromiseSnapshot.Contact> points=new ArrayList<>();for(int i=0;i<n;i++)points.add(new PromiseSnapshot.Contact(buf.readVarInt(),new Vec3(buf.readDouble(),buf.readDouble(),buf.readDouble()),buf.readByte()));return new PromiseSyncPayload(id,new PromiseSnapshot(equipped,mobility,mining,combat,upgrade,points));});
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent e){e.registrar("1").playToClient(TYPE,CODEC,(packet,ctx)->ctx.enqueueWork(()->com.devaxo.templeoftheaxolotl.client.PromiseVisualState.receive(packet.player(),packet.snapshot())));}
}
