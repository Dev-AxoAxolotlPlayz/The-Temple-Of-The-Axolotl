package com.devaxo.templeoftheaxolotl.pre07;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class TempleCrew {
    private static final String TEAM = "Temple Crew";
    private TempleCrew() {}
    public static void prepare(ServerLevel level, Mob mob) {
        PlayerTeam team = level.getScoreboard().getPlayerTeam(TEAM);
        if (team == null) team = level.getScoreboard().addPlayerTeam(TEAM);
        team.setAllowFriendlyFire(false);
        level.getScoreboard().addPlayerToTeam(mob.getStringUUID(), team);
        mob.getPersistentData().putBoolean("tota_temple_crew", true);
        mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, -1, 0, false, false));
    }
    @SubscribeEvent public static void incoming(LivingIncomingDamageEvent event) {
        var victim = event.getEntity();
        var attacker = event.getSource().getEntity();
        if (attacker != null && victim.getPersistentData().getBoolean("tota_temple_crew")
                && attacker.getPersistentData().getBoolean("tota_temple_crew")) event.setCanceled(true);
    }
    @SubscribeEvent public static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide || !mob.getPersistentData().getBoolean("tota_temple_crew")) return;
        if (mob.getTarget() != null && mob.getTarget().getPersistentData().getBoolean("tota_temple_crew")) mob.setTarget(null);
        if (mob.tickCount % 200 == 0 && !mob.hasEffect(MobEffects.FIRE_RESISTANCE))
            mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, -1, 0, false, false));
    }
}
