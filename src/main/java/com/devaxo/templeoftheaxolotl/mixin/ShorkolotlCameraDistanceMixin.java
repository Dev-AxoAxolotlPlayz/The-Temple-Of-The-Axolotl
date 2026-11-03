package com.devaxo.templeoftheaxolotl.mixin;

import com.devaxo.templeoftheaxolotl.entity.Shorkolotl100Entity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Request ten blocks of third-person camera distance for the submarine. */
@Mixin(Camera.class)
public abstract class ShorkolotlCameraDistanceMixin {
    @ModifyConstant(method = "setup", constant = @Constant(doubleValue = 4.0D), require = 0)
    private double tota$shorkolotlCameraDistance(double original) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getVehicle() instanceof Shorkolotl100Entity)
            return 10.0D;
        return original;
    }
}
