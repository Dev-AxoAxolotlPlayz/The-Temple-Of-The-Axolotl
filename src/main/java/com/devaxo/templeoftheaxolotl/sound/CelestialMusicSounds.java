package com.devaxo.templeoftheaxolotl.sound;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CelestialMusicSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, TempleOfTheAxolotl.MODID);
    public static final DeferredHolder<SoundEvent, SoundEvent> MENU = song("celestial_menu");
    public static final DeferredHolder<SoundEvent, SoundEvent> COSMIC_SEAS = song("disc_cosmic_seas");
    public static final DeferredHolder<SoundEvent, SoundEvent> BENEATH_SURFACE = song("disc_beneath_surface");
    public static final DeferredHolder<SoundEvent, SoundEvent> OUT_OF_REACH = song("disc_out_of_reach");
    private static DeferredHolder<SoundEvent, SoundEvent> song(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, name)));
    }
    private CelestialMusicSounds() {}
}
