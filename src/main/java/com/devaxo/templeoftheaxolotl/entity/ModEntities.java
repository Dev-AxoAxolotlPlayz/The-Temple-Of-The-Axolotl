
package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(
                    Registries.ENTITY_TYPE,
                    TempleOfTheAxolotl.MODID
            );

    public static final DeferredHolder<EntityType<?>, EntityType<WaifuAxoluthuEntity>> WAIFU_AXOLUTHU =
            ENTITY_TYPES.register("waifu_axoluthu", () -> EntityType.Builder
                    .of(WaifuAxoluthuEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).clientTrackingRange(16).updateInterval(2)
                    .build("waifu_axoluthu"));

    public static final DeferredHolder<EntityType<?>, EntityType<MegolotlEntity>> MEGOLOTL =
            ENTITY_TYPES.register("megolotl", () -> EntityType.Builder.of(MegolotlEntity::new, MobCategory.WATER_CREATURE)
                    .sized(19.8F, 11.3F).clientTrackingRange(32).build("megolotl"));
    public static final DeferredHolder<EntityType<?>, EntityType<AbyssalJellyfishEntity>> ABYSSAL_JELLYFISH =
            ENTITY_TYPES.register("abyssal_jellyfish", () -> EntityType.Builder.of(AbyssalJellyfishEntity::new, MobCategory.WATER_AMBIENT)
                    .sized(4.7F, 5.5F).clientTrackingRange(20).build("abyssal_jellyfish"));

    public static final DeferredHolder<EntityType<?>, EntityType<AxobloopEntity>> AXOBLOOP =
            ENTITY_TYPES.register("axobloop", () -> EntityType.Builder.of(AxobloopEntity::new, MobCategory.WATER_AMBIENT)
                    .sized(55.0F, 19.0F).clientTrackingRange(32).build("axobloop"));

    public static final DeferredHolder<EntityType<?>, EntityType<GhostfishEntity>> GHOSTFISH =
            ENTITY_TYPES.register("ghostfish", () -> EntityType.Builder.of(GhostfishEntity::new, MobCategory.WATER_AMBIENT)
                    .sized(10.5F, 1.8F).clientTrackingRange(20).build("ghostfish"));

    public static final DeferredHolder<EntityType<?>, EntityType<AbyssalAnglerEntity>> ABYSSAL_ANGLER =
            ENTITY_TYPES.register("abyssal_angler", () -> EntityType.Builder.of(AbyssalAnglerEntity::new, MobCategory.WATER_AMBIENT)
                    .sized(16.0F, 8.0F).clientTrackingRange(24).build("abyssal_angler"));

    public static final DeferredHolder<EntityType<?>, EntityType<AbyssalAxolotlEntity>> ABYSSAL_AXOLOTL =
            ENTITY_TYPES.register("abyssal_axolotl", () -> EntityType.Builder.of(AbyssalAxolotlEntity::new, MobCategory.CREATURE)
                    .sized(1.0f, 0.8f).clientTrackingRange(10).build("abyssal_axolotl"));

    public static final DeferredHolder<EntityType<?>,EntityType<AxoleviathanEntity>> AXOLEVIATHAN=ENTITY_TYPES.register("axoleviathan",()->EntityType.Builder.of(AxoleviathanEntity::new,MobCategory.MONSTER).sized(3f,3f).clientTrackingRange(20).build("axoleviathan"));
    public static final DeferredHolder<EntityType<?>, EntityType<Shorkolotl100Entity>> SHORKOLOTL_100 =
            ENTITY_TYPES.register("shorkolotl_100", () -> EntityType.Builder
                    .of(Shorkolotl100Entity::new, MobCategory.MISC)
                    .sized(3.3F, 2.8F).clientTrackingRange(10).updateInterval(1)
                    .build("shorkolotl_100"));

    public static final DeferredHolder<EntityType<?>, EntityType<AxokrakenEntity>> AXOKRAKEN = ENTITY_TYPES.register("axokraken", () -> EntityType.Builder.of(AxokrakenEntity::new, MobCategory.MONSTER).sized(3.0F, 2.5F).clientTrackingRange(20).build("axokraken"));

    public static final DeferredHolder<EntityType<?>, EntityType<MerolotlEntity>> MEROLOTL = ENTITY_TYPES.register(
            "merolotl", () -> EntityType.Builder.of(MerolotlEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).build("merolotl")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<AxolotlAbsoluteEntity>>
            AXOLOTL_ABSOLUTE = ENTITY_TYPES.register(
            "axolotl_above_all",
            () -> EntityType.Builder.of(
                    AxolotlAbsoluteEntity::new,
                    MobCategory.CREATURE
            ).sized(1.3F, 1.25F).build("axolotl_above_all")
    );
    public static final DeferredHolder<EntityType<?>, EntityType<ClucklesEntity>> CLUCKLES = ENTITY_TYPES.register(
        "cluckles", () -> EntityType.Builder.of(ClucklesEntity::new, MobCategory.CREATURE).sized(0.65F, 0.95F).build("cluckles")
    );
    public static final DeferredHolder<EntityType<?>, EntityType<BossWrathEntity>> BOSS_WRATH = ENTITY_TYPES.register(
        "boss_wrath", () -> EntityType.Builder.of(BossWrathEntity::new, MobCategory.MONSTER)
                .sized(3.0F, 1.5F).fireImmune().build("boss_wrath")
    );
    public static final DeferredHolder<EntityType<?>, EntityType<WrathHeadHitbox>> WRATH_HEAD_HITBOX = ENTITY_TYPES.register(
        "wrath_head_hitbox", () -> EntityType.Builder.of(WrathHeadHitbox::new, MobCategory.MISC)
                .sized(3.0F, 2.5F).clientTrackingRange(8).updateInterval(1).build("wrath_head_hitbox")
    );
    public static final DeferredHolder<EntityType<?>, EntityType<TameableWrathEntity>> TAMEABLE_WRATH = ENTITY_TYPES.register(
        "tameable_wrath", () -> EntityType.Builder.of(TameableWrathEntity::new, MobCategory.CREATURE).sized(1.3F, 1.25F).build("tameable_wrath")
    );
}

