package com.devaxo.templeoftheaxolotl.compat;

import com.devaxo.templeoftheaxolotl.repository.CelestialRepositoryMenu;
import com.devaxo.templeoftheaxolotl.repository.CelestialRepositoryMenuTypes;
import com.devaxo.templeoftheaxolotl.network.RepositoryRecipeFillPayload;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.Optional;

/** Recipe transfer is performed by the server against the player's persistent bulk storage. */
@JeiPlugin
public final class RepositoryJeiPlugin implements IModPlugin {
    @Override public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "repository_jei");
    }
    @Override public void registerRecipes(mezz.jei.api.registration.IRecipeRegistration registration) {
        registration.addIngredientInfo(new net.minecraft.world.item.ItemStack(com.devaxo.templeoftheaxolotl.item.ModItems.NOODLE_TRAINING_MEDAL.get()),
                mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
                net.minecraft.network.chat.Component.literal("Look for Recipes in An Axolotl's Field Guide."));
    }
    @Override public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime) {
        var manager = runtime.getRecipeManager();
        var recipes = manager.createRecipeLookup(RecipeTypes.SMITHING).includeHidden().get()
                .filter(recipe -> recipe.id().getNamespace().equals("temple_of_the_axolotl")
                        && recipe.id().getPath().startsWith("training_medal_")
                        && recipe.id().getPath().endsWith("_smithing")).toList();
        manager.hideRecipes(RecipeTypes.SMITHING, recipes);
    }
    @Override public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new IRecipeTransferHandler<CelestialRepositoryMenu, RecipeHolder<CraftingRecipe>>() {
            @Override public Class<? extends CelestialRepositoryMenu> getContainerClass() {
                return CelestialRepositoryMenu.class;
            }
            @Override public Optional<MenuType<CelestialRepositoryMenu>> getMenuType() {
                return Optional.of(CelestialRepositoryMenuTypes.REPOSITORY.get());
            }
            @Override public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
                return RecipeTypes.CRAFTING;
            }
            @Override public IRecipeTransferError transferRecipe(CelestialRepositoryMenu menu,
                    RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView recipeSlots,
                    Player player, boolean maxTransfer, boolean doTransfer) {
                if (!menu.hasCrafting()) return null;
                if (doTransfer) PacketDistributor.sendToServer(new RepositoryRecipeFillPayload(recipe.id()));
                return null;
            }
        }, RecipeTypes.CRAFTING);
    }
}
