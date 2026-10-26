
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
                .sized(1.3F, 1.875F).build("boss_wrath")
    );
    public static final DeferredHolder<EntityType<?>, EntityType<TameableWrathEntity>> TAMEABLE_WRATH = ENTITY_TYPES.register(
        "tameable_wrath", () -> EntityType.Builder.of(TameableWrathEntity::new, MobCategory.CREATURE).sized(1.3F, 1.25F).build("tameable_wrath")
    );
}

