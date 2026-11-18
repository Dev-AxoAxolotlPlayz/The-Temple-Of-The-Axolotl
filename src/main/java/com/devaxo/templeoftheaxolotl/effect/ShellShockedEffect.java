package com.devaxo.templeoftheaxolotl.effect;

import com.devaxo.templeoftheaxolotl.combat.ShellShockedLogic;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Five-second, stacking celestial stun. Amplifier 0..4 means levels I..V. */
public final class ShellShockedEffect extends MobEffect {
    public ShellShockedEffect() { super(MobEffectCategory.HARMFUL, 0x67DFF5); }
    @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return true; }
    @Override public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        ShellShockedLogic.tick(entity, amplifier);
        return true;
    }
}
