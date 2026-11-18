package com.devaxo.templeoftheaxolotl.compat;

import com.devaxo.templeoftheaxolotl.item.ModItems;
import com.devaxo.templeoftheaxolotl.registry.ModBlocks;
import com.devaxo.templeoftheaxolotl.brewing.CosmicBrewingRegistries;
import com.devaxo.templeoftheaxolotl.pre07.Pre07Fluids;
import mezz.jei.api.*;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.*;

@JeiPlugin
public final class ProcessingJeiPlugin implements IModPlugin {
    private static final String NS = "temple_of_the_axolotl";
    public record Display(List<ItemStack> inputs, ItemStack output, String line1, String line2) {}
    private static final RecipeType<Display> KILN = RecipeType.create(NS,"annealing",Display.class);
    private static final RecipeType<Display> BREW = RecipeType.create(NS,"cosmic_brewing",Display.class);
    @Override public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath(NS,"processing_jei"); }
    @Override public void registerCategories(IRecipeCategoryRegistration r) {
        var h = r.getJeiHelpers().getGuiHelper();
        r.addRecipeCategories(new Category(KILN,"Annealing Kiln",h.createDrawableItemStack(new ItemStack(ModBlocks.ANNEALING_KILN_ITEM.get()))),
                new Category(BREW,"Cosmic Brewing",h.createDrawableItemStack(new ItemStack(CosmicBrewingRegistries.STAND_ITEM.get()))));
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration r) {
        r.addRecipeCatalyst(new ItemStack(ModBlocks.ANNEALING_KILN_ITEM.get()),KILN);
        r.addRecipeCatalyst(new ItemStack(CosmicBrewingRegistries.STAND_ITEM.get()),BREW);
    }
    @Override public void registerRecipes(IRecipeRegistration r) {
        List<Display> kiln = new ArrayList<>();
        for(int n=1;n<=9;n++) { int cost=n*(n+7); kiln.add(new Display(List.of(new ItemStack(ModItems.MOLTEN_GLASS_IN_A_MOLD.get(),n),new ItemStack(Items.BLUE_ICE,(cost+23)/24)),new ItemStack(ModItems.PRESSURE_TREATED_ACRYLIC_PANEL.get(),n),cost+" coolant units / 10 seconds","Blue ice: 24 units; bank retained")); }
        r.addRecipes(KILN,kiln);
        List<Display> brew=new ArrayList<>();
        for (ItemStack heat : List.of(new ItemStack(Items.LAVA_BUCKET), new ItemStack(Items.MAGMA_BLOCK,4), new ItemStack(Items.SOUL_CAMPFIRE), new ItemStack(Items.CAMPFIRE,2))) {
            for (ItemStack water : List.of(new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.BLUE_ICE,2))) {
                brew.add(new Display(List.of(new ItemStack(ModItems.SHARD_OF_DIVINITY.get()),water,heat,new ItemStack(Items.BUCKET)),new ItemStack(Pre07Fluids.DILUTED_BUCKET.get()),"2 heat + 1 water + 1 catalyst","Shard: 10 charges; lava: 10 heat"));
            }
        }
        for(Item item: List.of(ModItems.CELESTIAL_MAGNET.get(),ModItems.CELESTIAL_REPOSITORY.get(),ModItems.CELESTIAL_JUKEBOX.get(),ModItems.LOGISTICS_MAESTRO.get(),ModItems.DISC_COSMIC_SEAS.get(),ModItems.DISC_BENEATH_SURFACE.get(),ModItems.DISC_OUT_OF_REACH.get(),Items.ENCHANTED_BOOK)) {
            brew.add(new Display(List.of(new ItemStack(item)),new ItemStack(ModItems.SHARD_OF_DIVINITY.get()),"10% shard chance","Consumes 1000 stored Ichor"));
        }
        r.addRecipes(BREW,brew);
    }
    private record Category(RecipeType<Display> type,String name,IDrawable icon) implements IRecipeCategory<Display> {
        @Override public RecipeType<Display> getRecipeType(){return type;}
        @Override public Component getTitle(){return Component.literal(name);}
        @Override public IDrawable getIcon(){return icon;}
        @Override public int getWidth(){return 184;}
        @Override public int getHeight(){return 65;}
        @Override public void setRecipe(IRecipeLayoutBuilder b,Display d,IFocusGroup f){
            for(int i=0;i<d.inputs().size();i++) b.addSlot(RecipeIngredientRole.INPUT,5+i*22,8).addItemStack(d.inputs().get(i));
            b.addSlot(RecipeIngredientRole.OUTPUT,160,8).addItemStack(d.output());
        }
        @Override public void draw(Display d,IRecipeSlotsView s,GuiGraphics g,double mx,double my){
            var font=net.minecraft.client.Minecraft.getInstance().font;
            g.drawString(font,"→",124,12,0xFF555555,false);
            g.drawString(font,d.line1(),3,34,0xFF333333,false);
            g.drawString(font,d.line2(),3,47,0xFF333333,false);
        }
    }
}
