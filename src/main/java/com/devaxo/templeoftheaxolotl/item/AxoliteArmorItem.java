package com.devaxo.templeoftheaxolotl.item;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Diamond durability; Netherite protection, toughness, and knockback resistance. */
public final class AxoliteArmorItem extends ArmorItem {
    public AxoliteArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        double armor = switch (getType()) {
            case HELMET -> 3;
            case CHESTPLATE -> 8;
            case LEGGINGS -> 6;
            case BOOTS -> 3;
            default -> 0;
        };
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "axolite_" + getType().getName());
        EquipmentSlotGroup slot = EquipmentSlotGroup.bySlot(getType().getSlot());
        return ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR, new AttributeModifier(id, armor, AttributeModifier.Operation.ADD_VALUE), slot)
                .add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id, 3.0, AttributeModifier.Operation.ADD_VALUE), slot)
                .add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(id, 0.1, AttributeModifier.Operation.ADD_VALUE), slot)
                .build();
    }
    private static final ResourceLocation OUTER = ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID,
            "textures/models/armor/axolite_celestial_layer_1.png");
    private static final ResourceLocation INNER = ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID,
            "textures/models/armor/axolite_celestial_layer_2.png");

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot,
                                            ArmorMaterial.Layer layer, boolean innerModel) {
        return slot == EquipmentSlot.LEGS ? INNER : OUTER;
    }
}
