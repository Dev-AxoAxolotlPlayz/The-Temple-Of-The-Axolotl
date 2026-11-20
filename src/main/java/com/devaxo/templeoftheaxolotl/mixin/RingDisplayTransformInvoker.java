package com.devaxo.templeoftheaxolotl.mixin;

import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.class)
public interface RingDisplayTransformInvoker {
    @Invoker("setTransformation")
    void tota$setTransformation(Transformation transformation);
}
