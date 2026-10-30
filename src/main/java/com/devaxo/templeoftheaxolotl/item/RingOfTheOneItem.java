
package com.devaxo.templeoftheaxolotl.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class RingOfTheOneItem extends SwordItem implements software.bernie.geckolib.animatable.GeoItem {

    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache animationCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.animation.AnimationController<>(this, "ring_blades", 0,
                state -> state.setAndContinue(software.bernie.geckolib.animation.RawAnimation.begin().thenLoop("Outer_Ring_Stars"))));
        controllers.add(new software.bernie.geckolib.animation.AnimationController<>(this, "ring_orbs", 0,
                state -> state.setAndContinue(software.bernie.geckolib.animation.RawAnimation.begin().thenLoop("Orbs"))));
        controllers.add(new software.bernie.geckolib.animation.AnimationController<>(this, "ring_star", 0,
                state -> state.setAndContinue(software.bernie.geckolib.animation.RawAnimation.begin().thenLoop("Central_Star"))));
    }

    @Override
    public software.bernie.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    public void createGeoRenderer(java.util.function.Consumer<software.bernie.geckolib.animatable.client.GeoRenderProvider> consumer) {
        consumer.accept(new software.bernie.geckolib.animatable.client.GeoRenderProvider() {
            private com.devaxo.templeoftheaxolotl.client.renderer.RingOfTheOneRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) renderer = new com.devaxo.templeoftheaxolotl.client.renderer.RingOfTheOneRenderer();
                return renderer;
            }
        });
    }


    private static final String MOD_ID =
            "temple_of_the_axolotl";

    // =========================================
    // CONSTRUCTOR
    // =========================================

    public RingOfTheOneItem(Properties properties) {
        super(Tiers.NETHERITE, properties);
    }

    // =========================================
    // BLOCK BREAKING PERMISSION
    // =========================================

    @Override
    public boolean canAttackBlock(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player
    ) {
        // Override SwordItem's Creative Mode
        // block-breaking restriction.
        // Server-authoritative protection runs before the initial block destruction.
        // Mining mode remains unchanged, including instant mining and Supernova.
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            return com.devaxo.templeoftheaxolotl.combat.RingMiningMode.isMining(serverPlayer);
        }
        // Client-side mining mode is not synchronized here; the server rejects
        // combat-mode destruction even if the client predicts a brief crack.
        return true;
    }

    // =========================================
    // EXCLUSIVE CELESTIAL ENCHANTMENTS
    // =========================================

    private static boolean isCelestialEnchantment(
            Holder<Enchantment> enchantment
    ) {
        return enchantment.unwrapKey().map(key -> {

            ResourceLocation id = key.location();

            if (!id.getNamespace().equals(MOD_ID)) {
                return false;
            }

            return switch (id.getPath()) {
                case "event_horizon",
                     "celestial_convergence",
                     "lucky_stars",
                     "supernova" -> true;

                default -> false;
            };

        }).orElse(false);
    }

    @Override
    public boolean supportsEnchantment(
            ItemStack stack,
            Holder<Enchantment> enchantment
    ) {
        return isCelestialEnchantment(enchantment);
    }

    @Override
    public boolean isPrimaryItemFor(
            ItemStack stack,
            Holder<Enchantment> enchantment
    ) {
        return false;
    }

    // =========================================
    // SUPERNOVA I - ENCHANTMENT DETECTION
    // =========================================

    private static boolean hasSupernova(
            ItemStack stack
    ) {
        for (var entry : stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet()) {

            if (entry.getIntValue() <= 0) {
                continue;
            }

            var key = entry.getKey().unwrapKey();

            if (key.isEmpty()) {
                continue;
            }

            ResourceLocation id =
                    key.get().location();

            if (id.getNamespace().equals(MOD_ID)
                    && id.getPath().equals("supernova")) {
                return true;
            }
        }

        return false;
    }

    // =========================================
    // SUPERNOVA I - UNIVERSAL MINING SPEED
    // =========================================

    @Override
    public float getDestroySpeed(
            ItemStack stack,
            BlockState state
    ) {

        // The Ring of the One can harvest the two celestial workstations.
        if (state.is(com.devaxo.templeoftheaxolotl.registry.ModBlocks.CELESTIAL_ANVIL_BLOCK.get())
                || state.is(com.devaxo.templeoftheaxolotl.brewing.CosmicBrewingRegistries.STAND.get())) {
            return 9.0F;
        }
        if (!hasSupernova(stack)) {
            return super.getDestroySpeed(stack, state);
        }

        // Bedrock will use the separate
        // 60-second excavation mechanic.
        if (state.is(Blocks.BEDROCK)) {
            return 1.0F;
        }

        // Preliminary universal mining speed:
        // Netherite tool (9) + Efficiency V (26)
        return 35.0F;
    }

    // =========================================
    // ITEM PROPERTIES
    // =========================================

    public static Item.Properties createProperties() {

        ResourceLocation damageId =
                ResourceLocation.fromNamespaceAndPath(
                        MOD_ID,
                        "ring_damage"
                );

        ResourceLocation speedId =
                ResourceLocation.fromNamespaceAndPath(
                        MOD_ID,
                        "ring_speed"
                );

        ResourceLocation reachId =
                ResourceLocation.fromNamespaceAndPath(
                        MOD_ID,
                        "ring_reach"
                );

        ItemAttributeModifiers modifiers =
                ItemAttributeModifiers.builder()

                        // ATTACK DAMAGE
                        // Existing value preserved.

                        .add(
                                Attributes.ATTACK_DAMAGE,
                                new AttributeModifier(
                                        damageId,
                                        12.0,
                                        AttributeModifier.Operation.ADD_VALUE
                                ),
                                EquipmentSlotGroup.MAINHAND
                        )

                        // ATTACK SPEED
                        // Existing value preserved.

                        .add(
                                Attributes.ATTACK_SPEED,
                                new AttributeModifier(
                                        speedId,
                                        1.6,
                                        AttributeModifier.Operation.ADD_VALUE
                                ),
                                EquipmentSlotGroup.MAINHAND
                        )

                        // ENTITY INTERACTION RANGE
                        // Existing value preserved.

                        .add(
                                Attributes.ENTITY_INTERACTION_RANGE,
                                new AttributeModifier(
                                        reachId,
                                        5.0,
                                        AttributeModifier.Operation.ADD_VALUE
                                ),
                                EquipmentSlotGroup.MAINHAND
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
                        DataComponents.UNBREAKABLE,
                        new Unbreakable(true)
                );
    }
}
