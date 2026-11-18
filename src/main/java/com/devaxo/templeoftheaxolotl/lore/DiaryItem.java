package com.devaxo.templeoftheaxolotl.lore;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import vazkii.patchouli.api.PatchouliAPI;

/** Craftable physical diary. Discovery progress is stored per player by advancements. */
public final class DiaryItem extends Item {
    private static final ResourceLocation BOOK = ResourceLocation.fromNamespaceAndPath(
            TempleOfTheAxolotl.MODID, "diary_of_an_axolotl");

    public DiaryItem(Properties properties) { super(properties); }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (!(entity instanceof ServerPlayer player)) return;
        var advancement = player.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "origin/above_all"));
        if (advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone()
                && !stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Diary of an Axolotl"));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            com.devaxo.templeoftheaxolotl.client.DiaryOpeningScreen.open();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
