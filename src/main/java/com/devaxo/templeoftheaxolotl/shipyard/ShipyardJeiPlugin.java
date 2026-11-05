package com.devaxo.templeoftheaxolotl.shipyard;

import com.devaxo.templeoftheaxolotl.registry.ModBlocks;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@JeiPlugin
public final class ShipyardJeiPlugin implements IModPlugin {
 private static final String MODID = "temple_of_the_axolotl";
 public static final RecipeType<ShipyardRecipes.Assembly> TYPE = RecipeType.create(MODID, "shipyard_crafting", ShipyardRecipes.Assembly.class);
 @Override public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath(MODID,"shipyard_jei"); }
 @Override public void registerCategories(IRecipeCategoryRegistration reg) { reg.addRecipeCategories(new Category(reg.getJeiHelpers().getGuiHelper())); }
 @Override public void registerRecipes(IRecipeRegistration reg) { reg.addRecipes(TYPE, ShipyardRecipes.all()); }
 @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) { reg.addRecipeCatalyst(new ItemStack(ModBlocks.SHIPYARD_CRANE_ITEM.get()), TYPE); }
 // JEI's built-in menu transfer handles ingredient matching, player inventory,
 // and server-authoritative slot clicks. The crane has 81 input slots, then 36 player slots.
 @Override public void registerRecipeTransferHandlers(IRecipeTransferRegistration reg) {
  reg.addRecipeTransferHandler(ShipyardMenu.class, ShipyardRegistries.CRANE_MENU.get(),
      TYPE, 0, 81, 81, 36);
 }
 private static final class Category implements IRecipeCategory<ShipyardRecipes.Assembly> {
  private final mezz.jei.api.gui.drawable.IDrawable icon;
  Category(IGuiHelper helper) { icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.SHIPYARD_CRANE_ITEM.get())); }
  @Override public RecipeType<ShipyardRecipes.Assembly> getRecipeType() { return TYPE; }
  @Override public Component getTitle() { return Component.literal("Shipyard Crane — 9×9 Crafting"); }
  @Override public mezz.jei.api.gui.drawable.IDrawable getIcon() { return icon; }
  @Override public int getWidth() { return 200; }
  @Override public int getHeight() { return 172; }
  @Override public void setRecipe(IRecipeLayoutBuilder builder, ShipyardRecipes.Assembly recipe, IFocusGroup focuses) {
   for (int y=0;y<9;y++) for (int x=0;x<9;x++) {
    var item = ShipyardRecipes.ingredient(recipe.pattern()[y].charAt(x));
    // Register every cell, including empty cells. JEI's recipe transfer maps
    // INPUT slots to menu slots in registration order; skipping empty cells
    // packs the recipe into the top-left corner of the crane's 9x9 inventory.
    var slot = builder.addSlot(RecipeIngredientRole.INPUT, x*18+1, y*18+1);
    if (item != Items.AIR) slot.addItemStack(new ItemStack(item));
   }
   builder.addSlot(RecipeIngredientRole.OUTPUT, 178, 73).addItemStack(new ItemStack(recipe.output()));
  }
  @Override public void draw(ShipyardRecipes.Assembly recipe, IRecipeSlotsView slots, GuiGraphics gui, double mouseX, double mouseY) {
   for (int y=0;y<9;y++) for (int x=0;x<9;x++) {
    int sx=x*18, sy=y*18;
    gui.fill(sx,sy,sx+18,sy+18,0xFF14373C);
    gui.fill(sx+1,sy+1,sx+17,sy+17,0xFF47797D);
   }
   gui.drawString(net.minecraft.client.Minecraft.getInstance().font, "→", 178, 53, 0xFFFFFFFF);
  }
 }
}
