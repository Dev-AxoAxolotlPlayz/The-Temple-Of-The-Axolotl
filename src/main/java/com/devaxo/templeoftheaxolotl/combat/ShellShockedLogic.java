package com.devaxo.templeoftheaxolotl.combat;

import com.devaxo.templeoftheaxolotl.effect.ModEffects;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ShellShockedLogic {
    public static final int DURATION = 100;
    public static final int STUN_TICKS = 50;
    private static final Map<UUID, UUID> OWNERS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_DOT_TICK = new ConcurrentHashMap<>();
    private ShellShockedLogic() {}

    public static boolean stunned(LivingEntity e) {
        var instance=e.getEffect(ModEffects.SHELL_SHOCKED);
        // A hit refreshes to 100 ticks; only the first 50 ticks are a hard freeze.
        return instance!=null && instance.getDuration()>DURATION-STUN_TICKS;
    }
    public static void apply(LivingEntity target, Player owner) {
        if(target.level().isClientSide || !target.isAlive() || target==owner
                || target.isAlliedTo(owner) || owner.isAlliedTo(target)) return;
        var previous=target.getEffect(ModEffects.SHELL_SHOCKED);
        int level=previous==null ? 0 : Math.min(4, previous.getAmplifier()+1);
        // Remove before re-adding to guarantee duration refresh even at level V.
        target.removeEffect(ModEffects.SHELL_SHOCKED);
        target.addEffect(new MobEffectInstance(ModEffects.SHELL_SHOCKED, DURATION, level, false, true, true));
        OWNERS.put(target.getUUID(),owner.getUUID());
    }
    public static void tick(LivingEntity target,int amplifier) {
        if(!(target.level() instanceof ServerLevel level)) return;
        if(stunned(target)) {
            target.setDeltaMovement(0,Math.min(0,target.getDeltaMovement().y),0);
            if(target instanceof net.minecraft.world.entity.Mob mob) mob.getNavigation().stop();
            if(target instanceof net.minecraft.world.entity.monster.Creeper creeper) creeper.setSwellDir(-1);
        }
        // 1 magic HP per stack per second, with attacker attribution.
        int now=target.tickCount;
        Integer last=LAST_DOT_TICK.get(target.getUUID());
        if(last!=null && now-last<20) return;
        LAST_DOT_TICK.put(target.getUUID(),now);
        var ownerId=OWNERS.get(target.getUUID());
        Player owner=ownerId==null?null:level.getServer().getPlayerList().getPlayer(ownerId);
        var type=level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MAGIC);
        DamageSource source=owner!=null?new DamageSource(type,owner):new DamageSource(type);
        target.hurt(source,Math.min(5,amplifier+1));
    }
}
