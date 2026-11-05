package com.devaxo.templeoftheaxolotl.mixin;

import com.devaxo.templeoftheaxolotl.client.CelestialWaterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Vanilla moon phase order: full=0, waning=1..3, new=4, waxing=5..7. */
@Mixin(LevelRenderer.class)
public abstract class CelestialMoonSizeMixin {
    @ModifyConstant(method = "renderSky", constant = @Constant(floatValue = 20.0F), require = 0)
    private float tota$phaseSize(float original) {
        if (!true) return original;
        var level = Minecraft.getInstance().level;
        if (level == null) return original;
        int phase = level.getMoonPhase();
        int stepsFromNew = Math.abs(4 - phase);
        return original * (0.95F + stepsFromNew * 0.05F);
    }
}
