package com.devaxo.templeoftheaxolotl.combat;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import com.devaxo.templeoftheaxolotl.mixin.AxoliteArrowStackAccessor;

@EventBusSubscriber(modid="temple_of_the_axolotl")
public final class SiphonEvents {
    private record Strike(ServerPlayer owner, ItemStack weapon) {}
    private static final ThreadLocal<Strike> SECONDARY = new ThreadLocal<>();
    public static boolean secondary(net.minecraft.world.entity.player.Player owner, ItemStack weapon, java.util.function.BooleanSupplier hit) {
        if (!(owner instanceof ServerPlayer p)) return hit.getAsBoolean();
        Strike old=SECONDARY.get();
        SECONDARY.set(new Strike(p,weapon));
        try { return hit.getAsBoolean(); }
        finally { if(old==null) SECONDARY.remove(); else SECONDARY.set(old); }
    }
    @SubscribeEvent
    public static void hit(LivingDamageEvent.Post event) {
        if (event.getNewDamage()<=0 || !(event.getSource().getEntity() instanceof ServerPlayer p)
            || !p.isAlive() || event.getEntity()==p) return;
        ItemStack weapon;
        Strike strike=SECONDARY.get();
        if(strike!=null && strike.owner()==p) weapon=strike.weapon();
        else if(event.getSource().getDirectEntity() instanceof ThrownTrident t && event.getSource().is(DamageTypes.TRIDENT))
            weapon=((AxoliteArrowStackAccessor)t).tota$pickupStack();
        else if(event.getSource().getDirectEntity()==p && event.getSource().is(DamageTypes.PLAYER_ATTACK)) weapon=p.getMainHandItem();
        else return;
        var key=ResourceKey.create(Registries.ENCHANTMENT,ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl","siphon"));
        var holder=p.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(key).orElse(null);
        if(holder==null || weapon.isEmpty()) return;
        int level=Math.min(4,weapon.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY).getLevel(holder));
        if(level<=0 || p.getRandom().nextFloat()>=.05f*level) return;
        if(p.getRandom().nextBoolean()) {
            if(p.getHealth()<p.getMaxHealth()) p.heal(2f);
            else CyanHearts.add(p,1f);
        } else {
            var food=p.getFoodData();
            if(food.getFoodLevel()<20) food.setFoodLevel(Math.min(20,food.getFoodLevel()+2));
            else food.setSaturation(Math.min(food.getFoodLevel(),food.getSaturationLevel()+2f));
        }
    }
}
