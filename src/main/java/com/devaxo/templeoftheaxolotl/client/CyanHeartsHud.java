package com.devaxo.templeoftheaxolotl.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
@EventBusSubscriber(modid="temple_of_the_axolotl",value=Dist.CLIENT)
public final class CyanHeartsHud {
    private static float reserve;
    public static void receive(float v){reserve=Math.max(0f,v);}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){reserve=0;}
    private static final String[] HEART={"0110110","1111111","1111111","1111111","0111110","0011100","0001000"};
    @SubscribeEvent public static void render(RenderGuiEvent.Post e){
        var mc=Minecraft.getInstance();var p=mc.player;
        if(p==null || mc.options.hideGui || mc.gameMode==null || !mc.gameMode.canHurtPlayer() || reserve<=0 || !p.isAlive())return;
        var g=e.getGuiGraphics();
        int heartRows=Math.max(1,(int)Math.ceil((p.getMaxHealth()+p.getAbsorptionAmount())/20f));
        int spacing=Math.max(10-(heartRows-2),3); spacing=Math.min(10,spacing);
        int x=g.guiWidth()/2-91,y=g.guiHeight()-39-(heartRows-1)*spacing-(p.getArmorValue()>0?10:0)-11;
        float value=Math.min(reserve,p.getMaxHealth()*.5f);
        int count=(int)Math.ceil(value/2f);
        for(int i=0;i<count;i++){
            int xx=x+8*(i%10), yy=y-10*(i/10);
            float fill=value-i*2f;
            for(int r=0;r<7;r++)for(int c=0;c<7;c++)if(HEART[r].charAt(c)=='1'){
                int color=fill>=2f || c<4?0xff45e5ed:0xff214853;
                g.fill(xx+c,yy+r,xx+c+1,yy+r+1,color);
            }
        }
    }
}
