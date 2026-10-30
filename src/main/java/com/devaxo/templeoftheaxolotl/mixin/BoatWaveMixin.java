package com.devaxo.templeoftheaxolotl.mixin;

import com.devaxo.templeoftheaxolotl.water.BoatWaveState;
import com.devaxo.templeoftheaxolotl.water.WaterDepthCache;
import com.devaxo.templeoftheaxolotl.water.WaterSurfacePhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hull-based spring buoyancy. Vanilla controlBoat and collision movement remain unchanged. */
@Mixin(Boat.class)
public abstract class BoatWaveMixin {
    @Inject(method = "floatBoat", at = @At("TAIL"))
    private void temple$applyWaveBuoyancy(CallbackInfo ci) {
        Boat boat = (Boat)(Object)this;
        if (!boat.isControlledByLocalInstance() || boat.isUnderWater() || boat.onGround()) {
            BoatWaveState.clear(boat);
            return;
        }
        int x = (int)Math.floor(boat.getX());
        int z = (int)Math.floor(boat.getZ());
        if (!boat.level().hasChunk(x >> 4, z >> 4)) return;
        int baseY = (int)Math.floor(boat.getBoundingBox().minY);
        int surfaceY = Integer.MIN_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = 1; dy >= -2; dy--) {
            cursor.set(x, baseY + dy, z);
            if (boat.level().getFluidState(cursor).is(FluidTags.WATER)
                    && !boat.level().getFluidState(cursor.above()).is(FluidTags.WATER)) {
                surfaceY = cursor.getY();
                break;
            }
        }
        if (surfaceY == Integer.MIN_VALUE) {
            BoatWaveState.clear(boat);
            return;
        }
        double depth = WaterDepthCache.sample(boat.level(), x, surfaceY, z);
        if (depth <= 2) {
            BoatWaveState.clear(boat);
            return;
        }
        double time = boat.level().getGameTime();
        WaterSurfacePhysics.HullSample hull = WaterSurfacePhysics.sample(
            boat.getX(), boat.getZ(), boat.getYRot(), time, depth, 1.6, 1.1, surfaceY + 0.9);
        double desired = (2 * hull.centerHeight() + hull.frontHeight()
            + hull.rearHeight() + hull.leftHeight() + hull.rightHeight()) / 6.0;
        Vec3 velocity = boat.getDeltaMovement();
        double acceleration = WaterSurfacePhysics.buoyancyAcceleration(
            boat.getY(), desired, velocity.y, 0.035, 0.12, 0.065);
        boat.setDeltaMovement(velocity.x,
            Math.max(-0.3, Math.min(0.3, velocity.y + acceleration)), velocity.z);
        BoatWaveState.update(boat, hull.pitchDegrees(), hull.rollDegrees());
    }
}
