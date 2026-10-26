package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Chicken;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class ClucklesRecruitmentEvents {
    @SubscribeEvent public static void recruit(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Chicken chicken) ||
                !event.getItemStack().is(ModItems.UNCOOKED_RICE.get())) return;
        if (!(event.getEntity() instanceof ServerPlayer player) || !(chicken.level() instanceof ServerLevel level)) return;
        ClucklesRegistry registry = ClucklesRegistry.get(level);
        if (registry.has(player.getUUID())) {
            player.displayClientMessage(Component.literal("You already have a Cluckles in your employment."), true);
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }
        ClucklesEntity cluckles = ModEntities.CLUCKLES.get().create(level);
        if (cluckles == null) return;
        cluckles.moveTo(chicken.getX(), chicken.getY(), chicken.getZ(), chicken.getYRot(), chicken.getXRot());
        cluckles.tame(player);
        cluckles.setOrderedToSit(false);
        if (!level.addFreshEntity(cluckles)) return;
        registry.register(player.getUUID(), cluckles.getUUID());
        chicken.discard();
        if (!player.getAbilities().instabuild) event.getItemStack().shrink(1);
        player.displayClientMessage(Component.literal("Cluckles has joined your workforce!"), true);
        award(player, "gainfully_employed");
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
    private static void award(ServerPlayer player, String name) {
        var advancement = player.server.getAdvancements().get(
                ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, name));
        if (advancement != null) player.getAdvancements().award(advancement, "completed");
    }
    @SubscribeEvent public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player &&
                ClucklesRegistry.get(player.serverLevel()).consumeLoss(player.getUUID())) {
            award(player, "gainfully_employed");
            award(player, "cluckles_lost");
        }
    }
    @SubscribeEvent public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ClucklesEntity cluckles && cluckles.level() instanceof ServerLevel level &&
                cluckles.getOwnerUUID() != null) {
            ClucklesRegistry.get(level).unregister(cluckles.getOwnerUUID(), cluckles.getUUID());
            if (level.getServer().getPlayerList().getPlayer(cluckles.getOwnerUUID()) instanceof ServerPlayer player) {
                award(player, "gainfully_employed");
                award(player, "cluckles_lost");
                ClucklesRegistry.get(level).consumeLoss(player.getUUID());
            }
        }
    }
}
