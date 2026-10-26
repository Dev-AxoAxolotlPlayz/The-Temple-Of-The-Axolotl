
package com.devaxo.templeoftheaxolotl.item;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.client.renderer.CelestialArmorRenderer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class CelestialArmorItem extends ArmorItem implements GeoItem {

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation CHESTPLATE_ANIMATION =
            RawAnimation.begin()
                    .thenLoop("passive_chestplate_animation");

    private static final RawAnimation LEGGINGS_ANIMATION =
            RawAnimation.begin()
                    .thenLoop("tail_animation_passive");

    public CelestialArmorItem(
            Holder<ArmorMaterial> material,
            Type type,
            Properties properties
    ) {
        super(material, type, properties);
    }

    // =========================================
    // CELESTIAL ARMOR ATTRIBUTES
    // =========================================

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {

        // Helmet is handled separately by AxolotlAbsoluteItem.
        // Each piece gets its own modifier ID and equipment slot.

        double armorValue;

        switch (getType()) {
            case CHESTPLATE -> armorValue = 80.0 / 3.0;
            case LEGGINGS -> armorValue = 20.0;
            case BOOTS -> armorValue = 10.0;
            default -> {
                return super.getDefaultAttributeModifiers();
            }
        }

        EquipmentSlotGroup slot =
                EquipmentSlotGroup.bySlot(getType().getSlot());

        ResourceLocation modifierId =
                ResourceLocation.fromNamespaceAndPath(
                        TempleOfTheAxolotl.MODID,
                        "celestial_" + getType().getName()
                );

        return ItemAttributeModifiers.builder()

                // Slot-scaled armor
                .add(
                        Attributes.ARMOR,
                        new AttributeModifier(
                                modifierId,
                                armorValue,
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        slot
                )

                // Armor Toughness: +8 per piece
                .add(
                        Attributes.ARMOR_TOUGHNESS,
                        new AttributeModifier(
                                modifierId,
                                8.0,
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        slot
                )

                // Knockback Resistance: +40% per piece
                .add(
                        Attributes.KNOCKBACK_RESISTANCE,
                        new AttributeModifier(
                                modifierId,
                                0.40,
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        slot
                )

                // Maximum Health: +10 HP per piece
                .add(
                        Attributes.MAX_HEALTH,
                        new AttributeModifier(
                                modifierId,
                                10.0,
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        slot
                )

                .build();
    }

    // =========================================
    // GECKOLIB ANIMATION CONTROLLERS
    // =========================================

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers
    ) {

        if (getType() == Type.CHESTPLATE) {
            controllers.add(
                    new AnimationController<>(
                            this,
                            0,
                            state -> state.setAndContinue(
                                    CHESTPLATE_ANIMATION
                            )
                    )
            );
        }

        if (getType() == Type.LEGGINGS) {
            controllers.add(
                    new AnimationController<>(
                            this,
                            0,
                            state -> state.setAndContinue(
                                    LEGGINGS_ANIMATION
                            )
                    )
            );
        }

        // Boots have no custom animation.
    }

    // =========================================
    // GECKOLIB INSTANCE CACHE
    // =========================================

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // =========================================
    // GECKOLIB ARMOR RENDERER
    // =========================================

    @Override
    public void createGeoRenderer(
            Consumer<GeoRenderProvider> consumer
    ) {

        consumer.accept(new GeoRenderProvider() {

            private CelestialArmorRenderer renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(
                    T livingEntity,
                    ItemStack itemStack,
                    EquipmentSlot equipmentSlot,
                    HumanoidModel<T> original
            ) {

                if (renderer == null) {
                    renderer = new CelestialArmorRenderer();
                }

                return renderer;
            }
        });
    }
}
