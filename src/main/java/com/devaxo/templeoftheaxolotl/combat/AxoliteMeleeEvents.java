package com.devaxo.templeoftheaxolotl.combat;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import com.devaxo.templeoftheaxolotl.item.AxoliteTridentMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import java.util.HashSet;

@EventBusSubscriber(modid=TempleOfTheAxolotl.MODID)
public final class AxoliteMeleeEvents {
    @SubscribeEvent
    public static void onMelee(LivingDamageEvent.Post event) {
        if(!(event.getEntity().level() instanceof ServerLevel level)
                || !(event.getSource().getDirectEntity() instanceof Player player)
                || event.getSource().is(DamageTypes.MAGIC)
                || !AxoliteTridentMode.isAxoliteFamily(player.getMainHandItem())
                || AxoliteTridentMode.state(player)!=1
                || com.devaxo.templeoftheaxolotl.combat.InnateProtection.villager(event.getEntity())
                || event.getNewDamage()<=0) return;
        ShellShockedLogic.apply(event.getEntity(),player);
        if(event.getEntity() instanceof Mob mob) {
            var visited=new HashSet<java.util.UUID>();
            visited.add(mob.getUUID());
            AxoliteLightning.chain(level,player,player,mob,0,visited,null,
                player.getMainHandItem().is(ModItems.COSMIC_TRIDENT.get()), player.getMainHandItem());
        }
    }
}
