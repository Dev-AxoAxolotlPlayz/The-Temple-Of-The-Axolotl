package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public class ModEntityEvents {
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(
                ModEntities.AXOLOTL_ABSOLUTE.get(),
                Mob.createMobAttributes()
                        .add(Attributes.MAX_HEALTH, 100.0)
                        .add(Attributes.MOVEMENT_SPEED, 0.25)
                        .add(Attributes.FOLLOW_RANGE, 32.0)
                        .add(Attributes.STEP_HEIGHT, 1.0)
                        .add(Attributes.SCALE, 1.45)
                        .build()
        );
        event.put(ModEntities.CLUCKLES.get(), Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12).add(Attributes.MOVEMENT_SPEED, 0.32).build());
        event.put(ModEntities.BOSS_WRATH.get(), Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.ATTACK_DAMAGE, 10)
                .add(Attributes.MOVEMENT_SPEED, 0.23).add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.STEP_HEIGHT, 1.0).add(Attributes.SCALE, 2.5)
                .add(Attributes.ARMOR, 30.0).add(Attributes.ARMOR_TOUGHNESS, 20.0).build());
        event.put(ModEntities.TAMEABLE_WRATH.get(), Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60).add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 24).add(Attributes.STEP_HEIGHT, 1.0)
                .add(Attributes.SCALE, 1.45).build());
    }
}
