package com.devaxo.templeoftheaxolotl.effect;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, TempleOfTheAxolotl.MODID);
    public static final DeferredHolder<MobEffect, MobEffect> FREEZING_IMMUNITY =
            EFFECTS.register("freezing_immunity", () ->
                    new FreezingImmunityEffect(MobEffectCategory.BENEFICIAL, 0x88EFFF));
    public static final DeferredHolder<MobEffect, MobEffect> RNGESUS_CURSE =
            EFFECTS.register("rngesus_curse", () ->
                    new RngesusCurseEffect(MobEffectCategory.HARMFUL, 0x292038));
    public static final DeferredHolder<MobEffect, MobEffect> HE_SAYS_LEAVE =
            EFFECTS.register("he_says_leave", () ->
                    new TempleMarkerEffect(MobEffectCategory.NEUTRAL, 0x9D829E));
    public static final DeferredHolder<MobEffect, MobEffect> YOU_SHOULDNT_HAVE =
            EFFECTS.register("you_shouldnt_have", () ->
                    new TempleMarkerEffect(MobEffectCategory.HARMFUL, 0xB54141));
    public static final DeferredHolder<MobEffect, MobEffect> SAVE_ME =
            EFFECTS.register("save_me", () ->
                    new TempleMarkerEffect(MobEffectCategory.BENEFICIAL, 0xE7C96B));
    private ModEffects() {}
}
