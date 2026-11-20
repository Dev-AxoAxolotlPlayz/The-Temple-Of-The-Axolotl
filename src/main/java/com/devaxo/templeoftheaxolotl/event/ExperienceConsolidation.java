package com.devaxo.templeoftheaxolotl.event;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import com.devaxo.templeoftheaxolotl.mixin.ExperienceOrbCountAccessor;
@EventBusSubscriber(modid="temple_of_the_axolotl")
public final class ExperienceConsolidation {
    @SubscribeEvent public static void merge(EntityTickEvent.Post event){
        if(!(event.getEntity() instanceof ExperienceOrb orb)||!(orb.level() instanceof ServerLevel level)||!orb.isAlive()||level.getGameTime()%10!=0)return;
        var a=(ExperienceOrbCountAccessor)orb;
        long total=(long)orb.value*a.tota$count();
        for(var other:level.getEntitiesOfClass(ExperienceOrb.class,orb.getBoundingBox().inflate(5),o->o!=orb&&o.isAlive()&&o.getId()>orb.getId()&&o.distanceToSqr(orb)<=25)){
            long next=(long)other.value*((ExperienceOrbCountAccessor)other).tota$count();
            if(total+next>32767)continue; // vanilla persists Value as a short
            total+=next;orb.value=(int)total;a.tota$count(1);other.discard();
        }
    }
}
