package com.devaxo.templeoftheaxolotl.mixin;

import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TridentItem.class)
public abstract class AxoliteTridentLockItemMixin {
    @Inject(method="releaseUsing",at=@At("HEAD"))
    private void tota$lockOnRelease(ItemStack stack, Level level, LivingEntity user, int remaining, CallbackInfo ci) {
        if(level.isClientSide || !(user instanceof Player player) || !com.devaxo.templeoftheaxolotl.item.AxoliteTridentMode.isAxoliteFamily(stack)) return;
        var data=player.getPersistentData();
        data.remove("totaAxoliteLockTarget");
        int charge=stack.getUseDuration(user)-remaining;
        var registry=level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var key=net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl","auto_target"));
        var holder=registry.getHolder(key).orElse(null);
        boolean auto=holder!=null && stack.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY).getLevel(holder)>0;
        if(!auto && (!player.isShiftKeyDown() || charge<40)) return;
        Vec3 origin=player.getEyePosition();
        Vec3 forward=player.getLookAngle();
        Mob best=null;
        double bestAlong=48;
        for(Mob mob:level.getEntitiesOfClass(Mob.class,player.getBoundingBox().inflate(48))) {
            if(mob instanceof com.devaxo.templeoftheaxolotl.entity.AxoCthuluEntity || !mob.isAlive() || !mob.isPickable() || mob.isAlliedTo(player) || player.isAlliedTo(mob)
               || (mob instanceof net.minecraft.world.entity.TamableAnimal tame && tame.isTame())) continue;
            Vec3 center=mob.getBoundingBox().getCenter();
            double along=center.subtract(origin).dot(forward);
            if(along<0 || along>bestAlong) continue;
            double radius=Math.max(0.65,mob.getBbWidth()*0.65);
            if(center.distanceToSqr(origin.add(forward.scale(along)))>radius*radius) continue;
            best=mob; bestAlong=along;
        }
        if(best!=null) data.putUUID("totaAxoliteLockTarget",best.getUUID());
    }
}
