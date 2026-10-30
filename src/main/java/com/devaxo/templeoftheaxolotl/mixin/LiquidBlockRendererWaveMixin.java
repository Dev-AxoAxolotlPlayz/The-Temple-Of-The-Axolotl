package com.devaxo.templeoftheaxolotl.mixin;

import com.devaxo.templeoftheaxolotl.client.CelestialWaterMode;
import com.devaxo.templeoftheaxolotl.water.WaterSurfaceVisuals;
import com.devaxo.templeoftheaxolotl.water.WaterWaveMath;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Encodes a wave-amplitude marker into alpha of upper water vertices only.
 * The shader restores their output alpha. No baked offsets or chunk refreshes.
 * Experimental: marker is specific to vanilla translucent terrain format.
 */
@Mixin(LiquidBlockRenderer.class)
public abstract class LiquidBlockRendererWaveMixin {
    private static final String VERTEX =
        "Lnet/minecraft/client/renderer/block/LiquidBlockRenderer;vertex(Lcom/mojang/blaze3d/vertex/VertexConsumer;FFFFFFFFFI)V";

    @ModifyArgs(method = "tesselate", at = @At(value = "INVOKE", target = VERTEX), require = 0)
    private void tota$markWater(Args args, BlockAndTintGetter level, BlockPos pos,
                                VertexConsumer buffer, BlockState state, FluidState fluid) {
        if (!CelestialWaterMode.enabled() || !fluid.is(FluidTags.WATER)
                || !WaterSurfaceVisuals.isExposedWater(level, pos)) return;
        float localY = ((Float) args.get(2)) - (pos.getY() & 15);
        if (localY < 0.80f || localY > 1.02f) return;
        // Alpha is an 8-bit vertex channel. 230..250 encodes 21 depth bands.
        // This is not visible transparency: the fragment shader restores 1.0.
        // The shared-corner depth is approximated from the existing helper's
        // time-zero wave envelope; it is NOT an exact depth signal.
        float x = (Float) args.get(1), z = (Float) args.get(3);
        double wx = (pos.getX() & ~15) + x, wz = (pos.getZ() & ~15) + z;
        double a = WaterSurfaceVisuals.sharedCornerAmplitude(level, pos, wx, wz);
        int band = Math.max(0, Math.min(20, (int)Math.round(a * 20.0)));
        // LiquidBlockRenderer.vertex(buffer, x, y, z, r, g, b, a, u, v, light)
        // Args index 7 is alpha; index 9 is V texture coordinate.
        // Writing the marker to index 9 made water sample the wrong atlas UV!
        args.set(7, (230 + band) / 255.0f);
    }
}
