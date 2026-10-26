package com.devaxo.templeoftheaxolotl.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Cosmetic dungeon marker. Authoritative state is stored on the player. */
public final class TempleMarkerEffect extends MobEffect {
    public TempleMarkerEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
