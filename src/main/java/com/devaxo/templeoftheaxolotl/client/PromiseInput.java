package com.devaxo.templeoftheaxolotl.client;
import com.devaxo.templeoftheaxolotl.network.PromiseInputPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
@EventBusSubscriber(modid="temple_of_the_axolotl",value=Dist.CLIENT)
public final class PromiseInput {
 @SubscribeEvent public static void tick(ClientTickEvent.Post e){var mc=Minecraft.getInstance();if(mc.player==null||!PromiseVisualState.get(mc.player.getUUID()).equipped())return;boolean enabled=mc.screen==null;int f=enabled?((mc.options.keyUp.isDown()?1:0)-(mc.options.keyDown.isDown()?1:0)):0;int s=enabled?((mc.options.keyLeft.isDown()?1:0)-(mc.options.keyRight.isDown()?1:0)):0;PacketDistributor.sendToServer(new PromiseInputPayload(enabled&&mc.options.keyJump.isDown(),f,s));}
}
