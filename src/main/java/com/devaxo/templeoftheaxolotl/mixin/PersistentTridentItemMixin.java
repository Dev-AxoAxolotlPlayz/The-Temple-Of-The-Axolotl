package com.devaxo.templeoftheaxolotl.mixin;
import com.devaxo.templeoftheaxolotl.item.AxoliteTridentMode;
import com.devaxo.templeoftheaxolotl.trident.PersistentTridents;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.sounds.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(TridentItem.class)
public abstract class PersistentTridentItemMixin {
    @Inject(method="use",at=@At("HEAD"),cancellable=true)
    private void tota$recall(Level level, Player p, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir){
        ItemStack stack=p.getItemInHand(hand);
        if(!AxoliteTridentMode.isAxoliteFamily(stack))return;
        var data=p.getPersistentData(); String key=PersistentTridents.key(stack);
        if(p.isShiftKeyDown()) {
            if(level instanceof ServerLevel s && data.hasUUID(key)) {
                ThrownTrident found=null;
                for(var world:s.getServer().getAllLevels()) if(world.getEntity(data.getUUID(key)) instanceof ThrownTrident t && PersistentTridents.marked(t)){found=t;break;}
                if(found!=null && found.level()==level)found.getPersistentData().putBoolean(PersistentTridents.RECALL,true);
                else {if(found!=null)found.discard();data.remove(key);}
            }
            cir.setReturnValue(InteractionResultHolder.sidedSuccess(stack,level.isClientSide));
        } else if(!level.isClientSide && data.hasUUID(key)) cir.setReturnValue(InteractionResultHolder.fail(stack));
    }
    @Inject(method="releaseUsing",at=@At("HEAD"),cancellable=true)
    private void tota$retain(ItemStack stack,Level level,LivingEntity user,int remaining,CallbackInfo ci){
        if(!AxoliteTridentMode.isAxoliteFamily(stack)||!(user instanceof Player p))return;
        var reg=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        if(stack.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS,net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY).getLevel(reg.getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.RIPTIDE))>0)return;
        ci.cancel();
        if(!(level instanceof ServerLevel s)||stack.getUseDuration(user)-remaining<10||stack.getDamageValue()>=stack.getMaxDamage()-1||p.isShiftKeyDown())return;
        var data=p.getPersistentData();String key=PersistentTridents.key(stack);if(data.hasUUID(key))return;
        if(!p.getAbilities().instabuild)stack.hurtAndBreak(1,p,p.getUsedItemHand()==InteractionHand.MAIN_HAND?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND);
        ThrownTrident t=new ThrownTrident(s,p,stack.copy());
        t.getPersistentData().putBoolean(PersistentTridents.MARK,true);
        t.pickup=AbstractArrow.Pickup.DISALLOWED;
        t.shootFromRotation(p,p.getXRot(),p.getYRot(),0,2.5f,1);
        if(s.addFreshEntity(t)){data.putUUID(key,t.getUUID());s.playSound(null,t,SoundEvents.TRIDENT_THROW.value(),SoundSource.PLAYERS,1,1);p.awardStat(Stats.ITEM_USED.get(stack.getItem()));}
    }
}
