
package com.devaxo.templeoftheaxolotl.client.model;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.CelestialArmorItem;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;

import software.bernie.geckolib.model.GeoModel;

public class CelestialArmorModel extends GeoModel<CelestialArmorItem> {

    private ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(
                TempleOfTheAxolotl.MODID, path
        );
    }

    @Override
    public ResourceLocation getModelResource(CelestialArmorItem item) {
        return switch (item.getType()) {
            case CHESTPLATE -> resource("geo/axolotl_chestplate.geo.json");
            case LEGGINGS -> resource("geo/axolotl_leggings.geo.json");
            case BOOTS -> resource("geo/axolotl_boots.geo.json");
            default -> resource("geo/axolotl_chestplate.geo.json");
        };
    }

    @Override
    public ResourceLocation getTextureResource(CelestialArmorItem item) {
        return switch (item.getType()) {
            case CHESTPLATE -> resource("textures/item/axolotl_chestplate.png");
            case LEGGINGS -> resource("textures/item/axolotl_leggings.png");
            case BOOTS -> resource("textures/item/axolotl_boots.png");
            default -> resource("textures/item/axolotl_chestplate.png");
        };
    }

    @Override
    public ResourceLocation getAnimationResource(CelestialArmorItem item) {
        return switch (item.getType()) {
            case CHESTPLATE -> resource(
                    "animations/axolotl_chestplate_passives.animation.json"
            );
            case LEGGINGS -> resource(
                    "animations/axolotl_leggings_passive_tail.animation.json"
            );
            default -> resource(
                    "animations/axolotl_chestplate_passives.animation.json"
            );
        };
    }
}
