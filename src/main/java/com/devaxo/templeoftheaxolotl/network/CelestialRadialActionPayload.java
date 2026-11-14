package com.devaxo.templeoftheaxolotl.network;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.compass.TempleHudModules;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import com.devaxo.templeoftheaxolotl.registry.ModBlocks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record CelestialRadialActionPayload(int index) implements CustomPacketPayload {
    public static final Type<CelestialRadialActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID,"celestial_radial_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf,CelestialRadialActionPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, CelestialRadialActionPayload::index, CelestialRadialActionPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, (packet,context) -> context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer p)) return;
            if (packet.index() == -1) { sendState(p); return; }
            switch (packet.index()) {
                case 0 -> { if (hasHelmet(p)) com.devaxo.templeoftheaxolotl.event.CelestialArmorEvents.toggleHelmetNightVision(p); }
                case 1 -> { if (hasHelmet(p)) com.devaxo.templeoftheaxolotl.event.CelestialArmorEvents.toggleHelmetConduit(p); }
                case 2 -> { if (hasRing(p)) com.devaxo.templeoftheaxolotl.combat.RingMiningMode.toggle(p); }
                case 3 -> { if (TempleHudModules.getSlot(p,4).is(ModItems.CELESTIAL_MAGNET.get()))
                    com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.cycleMagnet(p); }
                case 8 -> { if (TempleHudModules.getSlot(p,11).is(ModItems.ABYSSAL_LANTERN.get()))
                    com.devaxo.templeoftheaxolotl.event.AbyssalLanternEvents.toggle(p); }
                case 7 -> { if (TempleHudModules.getSlot(p,14).is(ModItems.ARCHITECTS_JOURNAL.get()))
                    com.devaxo.templeoftheaxolotl.network.AxoluthuJournalPayload.send(p); }
                case 4 -> { if (TempleHudModules.getSlot(p,15).is(ModBlocks.DIARY_ITEM.get()))
                    PacketDistributor.sendToPlayer(p,new CelestialRadialOpenBookPayload(0)); }
                case 5 -> { if (TempleHudModules.getSlot(p,16).is(ModBlocks.FIELD_GUIDE_ITEM.get()))
                    PacketDistributor.sendToPlayer(p,new CelestialRadialOpenBookPayload(1)); }
                case 6 -> { if ((TempleHudModules.getSlot(p,17).is(ModItems.NOODLE_COMMAND_BOOK.get()) || TempleHudModules.getSlot(p,17).is(ModItems.ABYSSAL_COMPANION_REGISTRY.get())))
                    PacketDistributor.sendToPlayer(p,new CelestialRadialOpenBookPayload(2)); }
                default -> { }
            }
            sendState(p);
        }));
    }
    private static boolean hasHelmet(ServerPlayer p) {
        return p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(ModItems.AXOLOTL_ABSOLUTE.get());
    }
    private static boolean hasRing(ServerPlayer p) {
        return p.getMainHandItem().is(ModItems.RING_OF_THE_ONE.get());
    }
    private static void sendState(ServerPlayer p) {
        int flags = 0;
        if (com.devaxo.templeoftheaxolotl.event.CelestialArmorEvents.helmetNightVisionEnabled(p)) flags |= 1;
        if (com.devaxo.templeoftheaxolotl.event.CelestialArmorEvents.helmetConduitEnabled(p)) flags |= 2;
        if (com.devaxo.templeoftheaxolotl.combat.RingMiningMode.isMining(p)) flags |= 4;
        if (com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.magnetMode(p) != 2) flags |= 8;
        if (TempleHudModules.getSlot(p,15).is(ModBlocks.DIARY_ITEM.get())) flags |= 16;
        if (TempleHudModules.getSlot(p,16).is(ModBlocks.FIELD_GUIDE_ITEM.get())) flags |= 32;
        if ((TempleHudModules.getSlot(p,17).is(ModItems.NOODLE_COMMAND_BOOK.get()) || TempleHudModules.getSlot(p,17).is(ModItems.ABYSSAL_COMPANION_REGISTRY.get()))) flags |= 64;
        if (TempleHudModules.getSlot(p,14).is(ModItems.ARCHITECTS_JOURNAL.get())) flags |= 128;
        if (TempleHudModules.getSlot(p,11).is(ModItems.ABYSSAL_LANTERN.get())) {
            flags |= 512; // equipped, independent of ON/OFF state
            if (com.devaxo.templeoftheaxolotl.event.AbyssalLanternEvents.moduleEnabled(p)) flags |= 256;
        }
        if (hasHelmet(p)) flags |= (1 << 10) | (1 << 11);
        if (hasRing(p)) flags |= (1 << 12);
        if (TempleHudModules.getSlot(p,4).is(ModItems.CELESTIAL_MAGNET.get())) flags |= (1 << 13);
        PacketDistributor.sendToPlayer(p, new CelestialRadialStatePayload(flags));
    }
}
