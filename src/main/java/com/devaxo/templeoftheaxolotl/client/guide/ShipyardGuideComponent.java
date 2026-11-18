package com.devaxo.templeoftheaxolotl.client.guide;

import com.devaxo.templeoftheaxolotl.shipyard.ShipyardRecipes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import vazkii.patchouli.api.ICustomComponent;
import vazkii.patchouli.api.IComponentRenderContext;
import vazkii.patchouli.api.IVariable;
import java.util.function.UnaryOperator;

public final class ShipyardGuideComponent implements ICustomComponent {
    public String assembly;
    private transient int x, y;
    private transient ShipyardRecipes.Assembly recipe;
    @Override public void onVariablesAvailable(UnaryOperator<IVariable> lookup, HolderLookup.Provider provider) {
        recipe = ShipyardRecipes.all().stream().filter(a -> a.id().equals(assembly)).findFirst().orElse(null);
    }
    @Override public void build(int x, int y, int page) { this.x=x; this.y=y; }
    @Override public void render(GuiGraphics g, IComponentRenderContext ctx, float tick, int mx, int my) {
        if (recipe == null) {
            g.drawString(Minecraft.getInstance().font, "Recipe unavailable", x, y, ctx.getTextColor(), false);
            return;
        }
        var font = Minecraft.getInstance().font;
        g.drawString(font, "Shipyard 9 x 9", x+12,y,ctx.getHeaderColor(),false);
        for (int row=0;row<9;row++) for(int col=0;col<9;col++) {
            int sx=x+col*12, sy=y+14+row*12;
            g.fill(sx,sy,sx+12,sy+12,0xFF244A50);
            g.fill(sx+1,sy+1,sx+11,sy+11,0xFF528187);
            var stack=new ItemStack(ShipyardRecipes.ingredient(recipe.pattern()[row].charAt(col)));
            if(stack.isEmpty()) continue;
            // Use page-space coordinates for Patchouli's recipe link, not local (0,0).
            // Only the occupied slot under the cursor may register an interactive link.
            boolean hovered = mx >= sx && mx < sx+12 && my >= sy && my < sy+12;
            int ix = sx+1, iy = sy+1;
            g.pose().pushPose();
            try {
                g.pose().translate(ix,iy,0);
                g.pose().scale(0.625f,0.625f,1f);
                g.pose().translate(-ix,-iy,0);
                if (hovered) {
                    // Clamp the one-pixel leading border into the native item bounds.
                    ctx.renderItemStack(g,ix,iy,Math.max(ix,mx),Math.max(iy,my),stack);
                } else {
                    g.renderItem(stack,ix,iy);
                }
            } finally {
                g.pose().popPose();
            }
        }
        g.drawString(font,"Output:",x+19,y+130,ctx.getTextColor(),false);
        ctx.renderItemStack(g,x+70,y+126,mx,my,new ItemStack(recipe.output()));
    }
}
