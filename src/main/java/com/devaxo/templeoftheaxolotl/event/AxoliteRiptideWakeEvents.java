package com.devaxo.templeoftheaxolotl.event;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.AxoliteTridentMode;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.joml.Vector3f;

@EventBusSubscriber(modid=TempleOfTheAxolotl.MODID)
public final class AxoliteRiptideWakeEvents {
    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !p.isAutoSpinAttack()
                || AxoliteTridentMode.state(p)!=2 || p.getDeltaMovement().lengthSqr()<0.04) return;
        var v=p.getDeltaMovement();
        var center=p.position().add(0,0.65,0).subtract(v.scale(0.35));
        for(int i=0;i<3;i++) {
            var color=switch(i) {
                case 0 -> new Vector3f(0.20f,0.95f,1.0f);
                case 1 -> new Vector3f(1.0f,0.30f,0.76f);
                default -> new Vector3f(0.68f,0.45f,1.0f);
            };
            var pos=center.subtract(v.scale(i*0.20));
            p.serverLevel().sendParticles(new DustParticleOptions(color,0.9f),pos.x,pos.y,pos.z,3,0.25,0.25,0.25,0.0);
        }
    }
}
