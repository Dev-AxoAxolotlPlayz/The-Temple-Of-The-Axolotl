package com.devaxo.templeoftheaxolotl.client;

import com.devaxo.templeoftheaxolotl.entity.BossWrathEntity;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Dark crimson fog around the Second Fall; does not change the world's clock or weather. */
public final class SecondFallSky {
    @SubscribeEvent
    public void onFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        boolean nearby = !mc.level.getEntitiesOfClass(BossWrathEntity.class,
                mc.player.getBoundingBox().inflate(96), BossWrathEntity::isSecondFall).isEmpty();
        if (!nearby) return;
        event.setRed(event.getRed() * 0.12F);
        event.setGreen(event.getGreen() * 0.06F);
        event.setBlue(event.getBlue() * 0.09F);
    }
}
