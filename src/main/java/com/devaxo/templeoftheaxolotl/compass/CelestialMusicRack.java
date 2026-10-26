package com.devaxo.templeoftheaxolotl.compass;

import com.devaxo.templeoftheaxolotl.item.ModItems;
import com.devaxo.templeoftheaxolotl.sound.CelestialMusicSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;

/** Server-owned nine-disc inventory and personal portable playback. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class CelestialMusicRack {
    private static final String KEY = "tota_celestial_music_rack_";
    private static final String PLAYING = "tota_celestial_music_playing";
    private static final String SHUFFLE = "tota_celestial_music_shuffle";
    private static final String REPEAT = "tota_celestial_music_repeat";
    private static final String PAUSED = "tota_celestial_music_paused";
    private static final String PAUSE_LEFT = "tota_celestial_music_pause_left";
    private static final String NEXT = "tota_celestial_music_next_tick";
    private CelestialMusicRack() {}
    public static ItemStack get(ServerPlayer p, int slot) {
        return ItemStack.parseOptional(p.registryAccess(), p.getPersistentData().getCompound(KEY + slot));
    }
    public static void set(ServerPlayer p, int slot, ItemStack stack) {
        if (slot < 0 || slot >= 9) return;
        if (stack.isEmpty()) p.getPersistentData().remove(KEY + slot);
        else p.getPersistentData().put(KEY + slot, stack.save(p.registryAccess()));
        if (p.getPersistentData().getInt(PLAYING) == slot + 1 && !isDisc(stack)) stop(p);
    }
    public static boolean isDisc(ItemStack s) {
        return s.is(ModItems.DISC_COSMIC_SEAS.get()) || s.is(ModItems.DISC_BENEATH_SURFACE.get()) || s.is(ModItems.DISC_OUT_OF_REACH.get());
    }
    private static SoundEvent sound(ItemStack s) {
        if (s.is(ModItems.DISC_COSMIC_SEAS.get())) return CelestialMusicSounds.COSMIC_SEAS.get();
        if (s.is(ModItems.DISC_BENEATH_SURFACE.get())) return CelestialMusicSounds.BENEATH_SURFACE.get();
        if (s.is(ModItems.DISC_OUT_OF_REACH.get())) return CelestialMusicSounds.OUT_OF_REACH.get();
        return null;
    }
    private static int duration(ItemStack s) {
        return s.is(ModItems.DISC_COSMIC_SEAS.get()) ? 5393 : s.is(ModItems.DISC_BENEATH_SURFACE.get()) ? 4640 : 4092;
    }
    public static int playing(ServerPlayer p) { return p.getPersistentData().getInt(PLAYING) - 1; }
    public static boolean shuffle(ServerPlayer p) { return p.getPersistentData().getBoolean(SHUFFLE); }
    public static int repeat(ServerPlayer p) { return Math.floorMod(p.getPersistentData().getInt(REPEAT), 3); }
    public static boolean paused(ServerPlayer p) { return p.getPersistentData().getBoolean(PAUSED); }
    private static void silence(ServerPlayer p) {
        for (String name : new String[]{"disc_cosmic_seas", "disc_beneath_surface", "disc_out_of_reach"})
            p.connection.send(new ClientboundStopSoundPacket(ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, name), SoundSource.RECORDS));
    }
    public static void pauseResume(ServerPlayer p) {
        if (!installed(p)) return;
        int active = playing(p);
        if (active < 0) { advance(p, true); return; }
        if (!paused(p)) {
            p.getPersistentData().putLong(PAUSE_LEFT, Math.max(1, p.getPersistentData().getLong(NEXT) - p.serverLevel().getGameTime()));
            p.getPersistentData().putBoolean(PAUSED, true);
            silence(p);
        } else {
            p.getPersistentData().putBoolean(PAUSED, false);
            // Minecraft's vanilla sound protocol cannot resume mid-sample; restart the selected disc.
            play(p, active);
        }
    }
    public static void cycleRepeat(ServerPlayer p) { if (installed(p)) p.getPersistentData().putInt(REPEAT, (repeat(p) + 1) % 3); }
    public static void toggleShuffle(ServerPlayer p) { if (installed(p)) p.getPersistentData().putBoolean(SHUFFLE, !shuffle(p)); }
    private static boolean installed(ServerPlayer p) { return TempleHudModules.getSlot(p, 8).is(ModItems.CELESTIAL_JUKEBOX.get()); }
    private static int nextSlot(ServerPlayer p, boolean manual) {
        int current = playing(p);
        if (!manual && repeat(p) == 2 && current >= 0 && isDisc(get(p, current))) return current;
        java.util.List<Integer> available = new java.util.ArrayList<>();
        for (int i = 0; i < 9; i++) if (isDisc(get(p, i))) available.add(i);
        if (available.isEmpty()) return -1;
        if (shuffle(p)) {
            if (available.size() > 1) available.remove(Integer.valueOf(current));
            return available.get(p.getRandom().nextInt(available.size()));
        }
        for (int i : available) if (i > current) return i;
        return (manual || repeat(p) == 1 || current < 0) ? available.get(0) : -1;
    }
    public static int nextUp(ServerPlayer p) {
        if (!installed(p)) return -1;
        if (shuffle(p)) return -2; // Random: no deterministic next-up display.
        return nextSlot(p, false);
    }
    public static void previous(ServerPlayer p) {
        if (!installed(p)) return;
        int current = playing(p);
        int previous = -1;
        for (int i = 8; i >= 0; i--) if (isDisc(get(p, i)) && i < current) { previous = i; break; }
        if (previous < 0) for (int i = 8; i >= 0; i--) if (isDisc(get(p, i))) { previous = i; break; }
        stop(p);
        if (previous >= 0) { p.getPersistentData().putInt(PLAYING, previous + 1); play(p, previous); }
    }
    public static void advance(ServerPlayer p, boolean manual) {
        if (!installed(p)) return;
        int next = nextSlot(p, manual);
        stop(p);
        if (next >= 0) { p.getPersistentData().putInt(PLAYING, next + 1); play(p, next); }
    }
    public static void stop(ServerPlayer p) {
        for (String name : new String[]{"disc_cosmic_seas", "disc_beneath_surface", "disc_out_of_reach"})
            p.connection.send(new ClientboundStopSoundPacket(ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, name), SoundSource.RECORDS));
        p.getPersistentData().remove(PLAYING);
        p.getPersistentData().remove(NEXT);
        p.getPersistentData().remove(PAUSED);
        p.getPersistentData().remove(PAUSE_LEFT);
    }
    public static void toggle(ServerPlayer p, int slot) {
        if (!TempleHudModules.getSlot(p, 8).is(ModItems.CELESTIAL_JUKEBOX.get())) return;
        if (p.getPersistentData().getInt(PLAYING) == slot + 1) { stop(p); return; }
        if (!isDisc(get(p, slot))) return;
        stop(p);
        p.getPersistentData().putInt(PLAYING, slot + 1);
        play(p, slot);
    }
    private static void play(ServerPlayer p, int slot) {
        ItemStack disc = get(p, slot);
        SoundEvent event = sound(disc);
        if (event == null) { stop(p); return; }
        p.playNotifySound(event, SoundSource.RECORDS, 1.0F, 1.0F);
        p.getPersistentData().putLong(NEXT, p.serverLevel().getGameTime() + duration(disc));
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 10 != 0) return;
        int active = p.getPersistentData().getInt(PLAYING) - 1;
        if (active < 0) return;
        if (!TempleHudModules.getSlot(p, 8).is(ModItems.CELESTIAL_JUKEBOX.get()) || !isDisc(get(p, active))) { stop(p); return; }
        if (paused(p)) return;
        if (p.serverLevel().getGameTime() >= p.getPersistentData().getLong(NEXT)) advance(p, false);
    }
    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        CompoundTag old = event.getOriginal().getPersistentData();
        for (int i = 0; i < 9; i++) if (old.contains(KEY + i)) p.getPersistentData().put(KEY + i, old.get(KEY + i).copy());
        // Playback stops on death/respawn, but the discs remain safely stored.
    }
}
