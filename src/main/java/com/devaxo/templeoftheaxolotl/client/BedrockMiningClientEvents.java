
package com.devaxo.templeoftheaxolotl.client;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.event.SupernovaEvents;
import com.devaxo.templeoftheaxolotl.network.BedrockMiningPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(
        modid = TempleOfTheAxolotl.MODID,

        value = Dist.CLIENT
)
public class BedrockMiningClientEvents {

    private static boolean wasMining = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.level == null) {
            wasMining = false;
            return;
        }

        boolean mining = false;
        BlockPos target = BlockPos.ZERO;

        if (minecraft.screen == null
                && minecraft.isWindowActive()
                && RingAttackInput.physicalMiningHeld(minecraft)
                && SupernovaEvents.hasSupernova(minecraft.player)) {

            HitResult hit = minecraft.player.pick(
                    minecraft.player.blockInteractionRange(),
                    0.0F,
                    false
            );

            if (hit instanceof BlockHitResult blockHit) {

                BlockPos pos = blockHit.getBlockPos();

                if (minecraft.level.getBlockState(pos)
                        .is(Blocks.BEDROCK)) {

                    mining = true;
                    target = pos;
                }
            }
        }

        if (mining) {
            // Send continuously so the server knows the key
            // is still held and the target hasn't changed.
            PacketDistributor.sendToServer(
                    new BedrockMiningPayload(true, target)
            );

        } else if (wasMining) {
            PacketDistributor.sendToServer(
                    new BedrockMiningPayload(false, BlockPos.ZERO)
            );
        }

        wasMining = mining;
    }
}
