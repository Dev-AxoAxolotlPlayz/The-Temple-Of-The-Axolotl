package com.devaxo.templeoftheaxolotl.brewing;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public final class CosmicBrewingScreen extends AbstractContainerScreen<CosmicBrewingMenu>{
 public CosmicBrewingScreen(CosmicBrewingMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=320;imageHeight=296;inventoryLabelY=198;inventoryLabelX=77;}
 @Override protected void init(){super.init();addRenderableWidget(Button.builder(Component.literal("Withdraw 1,000 mB"),b->{if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);}).bounds(leftPos+190,topPos+179,112,20).build());}
 @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
  g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff0d1028);
  g.fill(leftPos+5,topPos+5,leftPos+315,topPos+192,0xff191433);
  g.fill(leftPos+8,topPos+8,leftPos+312,topPos+30,0xff302044);
  g.drawString(font,"Cosmic Brewing Stand",leftPos+81,topPos+14,0xffffd69b,false);
  for(int[] s:new int[][]{{94,41},{204,41},{94,98},{204,98},{204,153},{240,153}}){int x=leftPos+s[0]-2,y=topPos+s[1]-2;g.fill(x,y,x+20,y+20,0xffb99c67);g.fill(x+2,y+2,x+18,y+18,0xff29243c);}
  g.drawString(font,"Catalyst",leftPos+82,topPos+30,0xffe7cdaa,false);
  g.drawString(font,"Water Catalyst",leftPos+184,topPos+30,0xffe7cdaa,false);
  g.drawString(font,"Input",leftPos+87,topPos+86,0xffe7cdaa,false);
  g.drawString(font,"Output",leftPos+198,topPos+86,0xffe7cdaa,false);
  g.drawString(font,"Heat",leftPos+201,topPos+140,0xffe7cdaa,false);
  g.drawString(font,"Bucket",leftPos+239,topPos+140,0xffe7cdaa,false);
  g.drawString(font,"✦ "+menu.catalyst(),leftPos+31,topPos+46,0xffd9b5ff,false);
  g.drawString(font,"✦ "+menu.water(),leftPos+252,topPos+46,0xff8fddff,false);
  g.drawString(font,"Ichor Bank",leftPos+21,topPos+137,0xfff6dfbc,false);
  g.fill(leftPos+21,topPos+150,leftPos+179,topPos+161,0xff090b18);
  g.fill(leftPos+22,topPos+151,leftPos+22+(156*Math.min(16000,menu.ichor())/16000),topPos+160,0xff8749cc);
  g.drawString(font,menu.ichor()+" / 16000 mB",leftPos+39,topPos+164,0xffe9c6ff,false);
  g.drawString(font,"Heat Charges: "+menu.heat()+" / 100",leftPos+21,topPos+183,0xffffaf61,false);
  g.drawString(font,"Inventory",leftPos+77,topPos+200,0xffcab8d5,false);
  for(int r=0;r<4;r++)for(int c=0;c<9;c++){int x=leftPos+76+c*18,y=topPos+210+r*18+(r==3?4:0);g.fill(x,y,x+18,y+18,0xff70617e);g.fill(x+1,y+1,x+17,y+17,0xff28243b);}
 }
 @Override protected void renderLabels(GuiGraphics g,int mx,int my){}
 @Override public void render(GuiGraphics g,int mx,int my,float partial){renderBackground(g,mx,my,partial);super.render(g,mx,my,partial);renderTooltip(g,mx,my);}
}
