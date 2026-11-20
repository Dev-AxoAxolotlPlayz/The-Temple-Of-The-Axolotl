package com.devaxo.templeoftheaxolotl.client;
import com.devaxo.templeoftheaxolotl.network.*;
import com.devaxo.templeoftheaxolotl.promise.PromiseUpgrade;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
public final class PromiseDashboard extends Screen {
 private PromiseDashboardPayload state; private boolean excavation; private int x,y;
 private PromiseDashboard(PromiseDashboardPayload s){super(Component.literal("Axoluthu's Promise"));state=s;}
 public static void receive(PromiseDashboardPayload s){var mc=Minecraft.getInstance();if(s.open()&&mc.screen instanceof CelestialRadialWheel)mc.setScreen(new PromiseDashboard(s));else if(mc.screen instanceof PromiseDashboard d){d.state=s;d.rebuildWidgets();}}
 private void send(int i,int v){PacketDistributor.sendToServer(new PromiseSettingPayload(i,v));}
 private void button(String label,int dx,int dy,int w,Runnable action){addRenderableWidget(Button.builder(Component.literal(label),b->action.run()).bounds(x+dx,y+dy,w,20).build());}
 private int get(String k){return k.equals("depth")?Math.max(1,state.settings().getInt(k)):state.settings().getInt(k);}
 @Override protected void init(){x=(width-252)/2;y=(height-238)/2;var s=state.settings();if(!excavation){button("Mobility: "+(s.getBoolean("mobility")?"ON":"OFF"),16,52,220,()->send(0,s.getBoolean("mobility")?0:1));button("Keep Off Ground: "+(s.getBoolean("suspend")?"ON":"OFF"),16,78,220,()->send(8,s.getBoolean("suspend")?0:1));button("Mining: "+(s.getBoolean("mining")?"ON":"OFF"),16,102,220,()->send(1,s.getBoolean("mining")?0:1));String[] modes={"OFF","Player Combat Target","Auto-Combat Hostiles"};for(int i=0;i<3;i++){final int mode=i;button((s.getInt("combat")==i?"> ":"")+modes[i],16,128+i*22,220,()->send(2,mode));}button("Configure excavation",16,204,220,()->{excavation=true;rebuildWidgets();});}else{String[] keys={"left","right","above","below","depth"};for(int i=0;i<5;i++){final int f=i+3;final String k=keys[i];button("-",90,46+i*24,20,()->send(f,get(k)-1));button("+",138,46+i*24,20,()->send(f,get(k)+1));}button("Back",16,204,220,()->{excavation=false;rebuildWidgets();});}}
 @Override public boolean isPauseScreen(){return false;}
 @Override public void render(GuiGraphics g,int mx,int my,float partial){g.fill(0,0,width,height,0x88000818);g.fill(x,y,x+252,y+238,0xFF14253B);g.renderOutline(x,y,252,238,0xFF58DDE8);g.drawCenteredString(font,title,x+126,y+9,0xFF85F3FF);int n=state.upgrade();g.drawCenteredString(font,"+"+n+" | Reach: "+(5+n)+" blocks",x+126,y+24,0xFFFFFFFF);if(excavation){String[] keys={"left","right","above","below","depth"};for(int i=0;i<5;i++){g.drawString(font,keys[i],x+16,y+52+i*24,0xFFFFFFFF);g.drawCenteredString(font,""+get(keys[i]),x+124,y+52+i*24,0xFFFFFFFF);}g.drawString(font,"Max: "+PromiseUpgrade.width(n)+"x"+PromiseUpgrade.height(n)+"x"+PromiseUpgrade.depth(n),x+16,y+36,0xFF85F3FF);int l=Math.min(5,Math.max(0,get("left"))),r=Math.min(5,Math.max(0,get("right"))),u=Math.min(5,Math.max(0,get("above"))),d=Math.min(5,Math.max(0,get("below")));int scale=6;for(int a=-l;a<=r;a++)for(int b=-d;b<=u;b++){int px=x+204+a*scale,py=y+105-b*scale;g.fill(px,py,px+5,py+5,a==0&&b==0?0xFFFFD15C:0xFF50E9EF);}}super.render(g,mx,my,partial);}
}
