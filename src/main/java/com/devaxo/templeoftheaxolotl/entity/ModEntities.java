
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

    public static final DeferredHolder<EntityType<?>, EntityType<AbyssalMerfolkEntity>> ABYSSAL_MERFOLK = ENTITY_TYPES.register(
            "abyssal_merfolk", () -> EntityType.Builder.of(AbyssalMerfolkEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).build("abyssal_merfolk")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<AxolotlAbsoluteEntity>>
            AXOLOTL_ABSOLUTE = ENTITY_TYPES.register(
            "axolotl_absolute",
            () -> EntityType.Builder.of(
                    AxolotlAbsoluteEntity::new,
                    MobCategory.CREATURE
            ).sized(1.3F, 1.25F).build("axolotl_absolute")
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

