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
    public static void registerWaterSpawnPlacements(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.ABYSSAL_AXOLOTL.get(), net.minecraft.world.entity.SpawnPlacementTypes.IN_WATER,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getLevel().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)
                        && pos.getY() <= 45 && level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER),
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.MEGOLOTL.get(), net.minecraft.world.entity.SpawnPlacementTypes.IN_WATER,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)
                        && (!level.getLevel().dimension().location().getPath().equals("ethereal_abyss") || pos.getY() <= 120),
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.ABYSSAL_JELLYFISH.get(), net.minecraft.world.entity.SpawnPlacementTypes.IN_WATER,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)
                        && (!level.getLevel().dimension().location().getPath().equals("ethereal_abyss") || pos.getY() >= 111),
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.GHOSTFISH.get(), net.minecraft.world.entity.SpawnPlacementTypes.IN_WATER,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)
                        && level.getLevel().dimension().location().equals(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "ethereal_abyss"))
                        && pos.getY() <= 100,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.ABYSSAL_ANGLER.get(), net.minecraft.world.entity.SpawnPlacementTypes.IN_WATER,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)
                        && level.getLevel().dimension().location().equals(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "ethereal_abyss"))
                        && pos.getY() <= 100,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.AXOBLOOP.get(), net.minecraft.world.entity.SpawnPlacementTypes.IN_WATER,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)
                        && level.getLevel().dimension().location().equals(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "ethereal_abyss"))
                        && pos.getY() <= 100,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ATTACK_TENTACLE.get(), AttackTentacleEntity.attributes().build());
        event.put(ModEntities.AXOCTHULU.get(), AxoCthuluEntity.attributes().build());
        event.put(ModEntities.WAIFU_AXOLUTHU.get(), Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 24)
                .add(Attributes.STEP_HEIGHT, 1.0).build());
        event.put(ModEntities.MEGOLOTL.get(), Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80).add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.FOLLOW_RANGE, 32).add(Attributes.ATTACK_DAMAGE, 8).build());
        event.put(ModEntities.ABYSSAL_JELLYFISH.get(), Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10).add(Attributes.MOVEMENT_SPEED, 0.12)
                .add(Attributes.FOLLOW_RANGE, 16).build());
        event.put(ModEntities.AXOBLOOP.get(), Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.MOVEMENT_SPEED, 0.14)
                .add(Attributes.FOLLOW_RANGE, 24).build());
        event.put(ModEntities.GHOSTFISH.get(), Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.FOLLOW_RANGE, 24).build());
        event.put(ModEntities.ABYSSAL_ANGLER.get(), Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12)
                .add(Attributes.MOVEMENT_SPEED, 0.19)
                .add(Attributes.FOLLOW_RANGE, 24).build());
        event.put(ModEntities.ABYSSAL_AXOLOTL.get(), Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24).add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 24).add(Attributes.STEP_HEIGHT, 1.0)
                .add(Attributes.ATTACK_DAMAGE, 13.5D).build());
        event.put(ModEntities.AXOLEVIATHAN.get(),AxoleviathanEntity.attributes().build());
        event.put(ModEntities.AXOKRAKEN.get(), net.minecraft.world.entity.Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 180).add(Attributes.ATTACK_DAMAGE, 16.0D).add(Attributes.FOLLOW_RANGE, 32).add(Attributes.MOVEMENT_SPEED, .34).build());
        event.put(ModEntities.MEROLOTL.get(), net.minecraft.world.entity.Mob.createMobAttributes().add(Attributes.ATTACK_DAMAGE, 8.0D).build());
        event.put(
                ModEntities.AXOLOTL_ABSOLUTE.get(),
                Mob.createMobAttributes()
                        .add(Attributes.MAX_HEALTH, 100.0)
                        .add(Attributes.MOVEMENT_SPEED, 0.25)
                        .add(Attributes.FOLLOW_RANGE, 32.0)
                        .add(Attributes.STEP_HEIGHT, 1.0)
                        .add(Attributes.SCALE, 1.45).add(Attributes.ATTACK_DAMAGE, 4.0)
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
