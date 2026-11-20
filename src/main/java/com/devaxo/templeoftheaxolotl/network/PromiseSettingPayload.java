package com.devaxo.templeoftheaxolotl.network;
import com.devaxo.templeoftheaxolotl.promise.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
@EventBusSubscriber(modid="temple_of_the_axolotl")
public record PromiseSettingPayload(int field,int value) implements CustomPacketPayload {
 public static final Type<PromiseSettingPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl","promise_setting"));
 public static final StreamCodec<RegistryFriendlyByteBuf,PromiseSettingPayload> CODEC=StreamCodec.of((b,v)->{b.writeVarInt(v.field);b.writeVarInt(v.value);},b->new PromiseSettingPayload(b.readVarInt(),b.readVarInt()));
 public Type<? extends CustomPacketPayload> type(){return TYPE;}
 @SubscribeEvent public static void register(RegisterPayloadHandlersEvent e){e.registrar("1").playToServer(TYPE,CODEC,(v,c)->c.enqueueWork(()->{if(!(c.player() instanceof ServerPlayer p)||!PromiseState.installed(p))return;var s=PromiseState.settings(p);int n=PromiseUpgrade.level(p),i=v.field(),a=v.value();String[] keys={"mobility","mining","combat","left","right","above","below","depth","suspend"};if(i<0||i>=keys.length)return;String k=keys[i];if(i<2||i==8){if(a==0||a==1)s.putBoolean(k,a==1);}else{int max=switch(i){case 2->2;case 3->PromiseUpgrade.width(n)-1-s.getInt("right");case 4->PromiseUpgrade.width(n)-1-s.getInt("left");case 5->PromiseUpgrade.height(n)-1-s.getInt("below");case 6->PromiseUpgrade.height(n)-1-s.getInt("above");default->PromiseUpgrade.depth(n);};if(a>=(i==7?1:0)&&a<=max)s.putInt(k,a);}PromiseState.set(p,s);PromiseDashboardPayload.send(p,false);}));}
}
