package com.devaxo.templeoftheaxolotl.network;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
public record TridentFovPayload(int degrees) implements CustomPacketPayload {
    public static final Type<TridentFovPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl","trident_fov"));
    public static final StreamCodec<RegistryFriendlyByteBuf,TridentFovPayload> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT,TridentFovPayload::degrees,TridentFovPayload::new);
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE,CODEC,(packet,ctx)->ctx.enqueueWork(()->{
            if(ctx.player() instanceof ServerPlayer p) p.getPersistentData().putInt("tota_trident_fov",Math.max(30,Math.min(110,packet.degrees())));
        }));
    }
    public static boolean withinView(Entity owner, Entity target) {
        if (!(owner instanceof Player p)) return true;
        int fov=p.getPersistentData().getInt("tota_trident_fov");
        if(fov==0) fov=70;
        var delta=target.getBoundingBox().getCenter().subtract(p.getEyePosition());
        return delta.lengthSqr()<1e-8 || p.getLookAngle().dot(delta.normalize())>=Math.cos(Math.toRadians(fov*0.5));
    }
}
