package com.devaxo.templeoftheaxolotl.mixin;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ExperienceOrb.class)
public interface ExperienceOrbCountAccessor {
    @Accessor("count") int tota$count();
    @Accessor("count") void tota$count(int count);
}
