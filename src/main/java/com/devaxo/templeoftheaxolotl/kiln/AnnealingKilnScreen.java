package com.devaxo.templeoftheaxolotl.kiln;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public class AnnealingKilnScreen extends AbstractContainerScreen<AnnealingKilnMenu> {
 private Button start;
 public AnnealingKilnScreen(AnnealingKilnMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=176;imageHeight=185;inventoryLabelY=91;}
 @Override protected void init(){super.init();start=addRenderableWidget(Button.builder(Component.literal("Start Cooling"),b->{if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);}).bounds(leftPos+77,topPos+70,94,20).build());}
 @Override protected void containerTick(){super.containerTick();start.active=!menu.isCooling();}
 @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){int x=leftPos,y=topPos;g.fill(x,y,x+176,y+185,0xFF102A31);g.fill(x+4,y+4,x+172,y+181,0xFF24505B);for(int i=0;i<9;i++)slot(g,x+18+i%3*18,y+19+i/3*18);slot(g,x+89,y+37);slot(g,x+144,y+37);for(int j=0;j<3;j++)for(int i=0;i<9;i++)slot(g,x+8+i*18,y+103+j*18);for(int i=0;i<9;i++)slot(g,x+8+i*18,y+161);g.drawString(font,"Coolant: "+menu.coolant()+" units",78+x,17+y,0xB8EFFF,false);g.drawString(font,"Cooling: "+menu.progress()+"/200",78+x,27+y,0xB8EFFF,false);}
 private void slot(GuiGraphics g,int x,int y){g.fill(x-1,y-1,x+17,y+17,0xFF091A1E);g.fill(x,y,x+16,y+16,0xFF467784);}
 @Override public void render(GuiGraphics g,int mx,int my,float partial){super.render(g,mx,my,partial);renderTooltip(g,mx,my);}
}
