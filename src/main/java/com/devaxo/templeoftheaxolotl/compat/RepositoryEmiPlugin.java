package com.devaxo.templeoftheaxolotl.compat;

import com.devaxo.templeoftheaxolotl.repository.CelestialRepositoryMenu;
import com.devaxo.templeoftheaxolotl.repository.CelestialRepositoryMenuTypes;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import net.minecraft.world.inventory.Slot;
import java.util.List;

/** Exposes actual embedded crafting grid and player inventory to EMI. */
public final class RepositoryEmiPlugin implements EmiPlugin {
    @Override public void register(EmiRegistry registry) {
        registry.addRecipeHandler(CelestialRepositoryMenuTypes.REPOSITORY.get(),
            new StandardRecipeHandler<CelestialRepositoryMenu>() {
                @Override public List<Slot> getInputSources(CelestialRepositoryMenu menu) {
                    return menu.slots.subList(68, 104);
                }
                @Override public List<Slot> getCraftingSlots(CelestialRepositoryMenu menu) {
                    return menu.slots.subList(56, 65);
                }
                @Override public boolean supportsRecipe(dev.emi.emi.api.recipe.EmiRecipe recipe) {
                    return recipe.getCategory() == dev.emi.emi.api.recipe.VanillaEmiRecipeCategories.CRAFTING;
                }
            });
    }
}
