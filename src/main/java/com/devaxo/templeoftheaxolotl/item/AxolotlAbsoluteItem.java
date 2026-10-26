
package com.devaxo.templeoftheaxolotl.item;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;

import com.devaxo.templeoftheaxolotl.client.renderer.AxolotlAbsoluteArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;

public class AxolotlAbsoluteItem extends ArmorItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final ResourceLocation CELESTIAL_GRACE_ID =
            ResourceLocation.fromNamespaceAndPath(
                    TempleOfTheAxolotl.MODID,
                    "celestial_grace"
            );

    public AxolotlAbsoluteItem(Properties properties) {
        super(ArmorMaterials.NETHERITE, ArmorItem.Type.HELMET, properties);
    }

    public static Item.Properties createProperties() {

        ResourceLocation armorId =
                ResourceLocation.fromNamespaceAndPath(
                        TempleOfTheAxolotl.MODID,
                        "axolotl_absolute"
                );

        ItemAttributeModifiers modifiers =
                ItemAttributeModifiers.builder()
                        .add(
                                Attributes.ARMOR,
                                new AttributeModifier(
                                        armorId,
                                        10.0,
                                        AttributeModifier.Operation.ADD_VALUE
                                ),
                                EquipmentSlotGroup.HEAD
                        )
                        .add(
                                Attributes.ARMOR_TOUGHNESS,
                                new AttributeModifier(
                                        armorId,
                                        8.0,
                                        AttributeModifier.Operation.ADD_VALUE
                                ),
                                EquipmentSlotGroup.HEAD
                        )
                        .add(
                                Attributes.KNOCKBACK_RESISTANCE,
                                new AttributeModifier(
                                        armorId,
                                        0.40,
                                        AttributeModifier.Operation.ADD_VALUE
                                ),
                                EquipmentSlotGroup.HEAD
                        )
                        .add(
                                Attributes.MAX_HEALTH,
                                new AttributeModifier(
                                        armorId,
                                        10.0,
                                        AttributeModifier.Operation.ADD_VALUE
                                ),
                                EquipmentSlotGroup.HEAD
                        )
                        .build();

        return new Item.Properties()
                .stacksTo(1)
                .fireResistant()
                .component(
                        DataComponents.ATTRIBUTE_MODIFIERS,
                        modifiers
                )
                .component(
                        DataComponents.ENCHANTMENT_GLINT_OVERRIDE,
                        true
                );
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "absolute_gills", 0,
                state -> state.setAndContinue(RawAnimation.begin().thenLoop("axolotl_absolute_passive_Gills"))));
        controllers.add(new AnimationController<>(this, "absolute_wings", 0,
                state -> state.setAndContinue(RawAnimation.begin().thenLoop("axolotl_absolute_passive_Wings"))));
        controllers.add(new AnimationController<>(this, "absolute_crown", 0,
                state -> state.setAndContinue(RawAnimation.begin().thenLoop("axolotl_absolute_passive_Crown"))));
        controllers.add(new AnimationController<>(this, "absolute_tesseract", 0,
                state -> state.setAndContinue(RawAnimation.begin().thenLoop("axolotl_absolute_passive_Tesseract"))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private AxolotlAbsoluteArmorRenderer renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(
                    T livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot,
                    HumanoidModel<T> original) {
                if (renderer == null) renderer = new AxolotlAbsoluteArmorRenderer();
                return renderer;
            }
        });
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    // Explicitly allow Celestial Grace on the
    // custom Axolotl Absolute helmet.
    @Override
    public boolean supportsEnchantment(
            ItemStack stack,
            Holder<Enchantment> enchantment
    ) {
        if (isCelestialGrace(enchantment)) {
            return true;
        }

        return super.supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean isPrimaryItemFor(
            ItemStack stack,
            Holder<Enchantment> enchantment
    ) {
        if (isCelestialGrace(enchantment)) {
            return true;
        }

        return super.isPrimaryItemFor(stack, enchantment);
    }

    private static boolean isCelestialGrace(
            Holder<Enchantment> enchantment
    ) {
        return enchantment.unwrapKey()
                .map(key -> key.location().equals(CELESTIAL_GRACE_ID))
                .orElse(false);
    }
}
