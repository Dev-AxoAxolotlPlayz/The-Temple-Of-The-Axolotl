package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class NoodleTraining {
    private NoodleTraining() {}
    private static CompoundTag data(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        return custom == null ? new CompoundTag() : custom.copyTag();
    }
    public static Map<String,List<String>> getCommands(ItemStack stack) {
        Map<String,List<String>> result = new LinkedHashMap<>();
        CompoundTag root = data(stack).getCompound("NoodleCommands");
        for (String key : root.getAllKeys()) {
            List<String> lines = new ArrayList<>();
            ListTag tasks = root.getList(key, Tag.TAG_STRING);
            for (int i=0;i<tasks.size();i++) lines.add(tasks.getString(i));
            result.put(key, lines);
        }
        return result;
    }
    public static void setCommands(ItemStack stack, Map<String,List<String>> commands) {
        CompoundTag root = data(stack);
        CompoundTag entries = new CompoundTag();
        commands.forEach((phrase, lines) -> {
            ListTag tasks = new ListTag();
            for (String line : lines) tasks.add(StringTag.valueOf(line));
            entries.put(phrase, tasks);
        });
        root.put("NoodleCommands", entries);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }
    public static void syncBooks(ServerPlayer player) {
        var library = NoodleCommandLibrary.load(player.getUUID());
        for (ItemStack stack : player.getInventory().items)
            if (stack.is(ModItems.NOODLE_COMMAND_BOOK.get())) setCommands(stack, library);
        for (ItemStack stack : player.getInventory().offhand)
            if (stack.is(ModItems.NOODLE_COMMAND_BOOK.get())) setCommands(stack, library);
        player.getInventory().setChanged();
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncBooks(player);
    }
    @SubscribeEvent public static void repaired(AnvilRepairEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack result = event.getOutput();
        if (!result.is(ModItems.NOODLE_COMMAND_BOOK.get())) return;
        // Only persist after the player ACTUALLY takes the anvil output.
        if (!event.getRight().is(ModItems.NOODLE_TRAINING_MEDAL.get())) return;
        CompoundTag medal = data(event.getRight());
        String phrase = medal.getString("Phrase");
        ListTag tasks = medal.getList("Tasks", Tag.TAG_STRING);
        if (phrase.isBlank() || tasks.isEmpty()) return;
        List<String> lines = new ArrayList<>();
        for (int i=0;i<tasks.size();i++) lines.add(tasks.getString(i));
        if (!NoodleCommandLibrary.save(player.getUUID(), phrase, lines))
            player.sendSystemMessage(Component.literal("Could not save the Abyssal Axolotl command JSON."));
    }
    @SubscribeEvent public static void anvil(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft(), right = event.getRight();
        if (left.is(ModItems.NOODLE_TRAINING_MEDAL.get()) && right.is(Items.WRITTEN_BOOK)) {
            String name = event.getName();
            if (name == null || !name.strip().matches("[\\p{L}\\p{N} _-]{1,48}")) return;
            var book = right.get(DataComponents.WRITTEN_BOOK_CONTENT);
            if (book == null) return;
            List<String> lines = new ArrayList<>();
            for (var page : book.pages()) {
                for (String line : page.raw().getString().split("\\R")) {
                    line = line.trim();
                    if (!line.isEmpty()) lines.add(line);
                }
            }
            if (lines.isEmpty() || lines.size() > 32) return;
            for (String line : lines) if (!BabyNoodleChat.isAllowedTask(line)) return;
            ItemStack result = left.copy();
            result.set(DataComponents.CUSTOM_NAME, Component.literal(name.strip()));
            CompoundTag root = data(result);
            root.putString("Phrase", name.strip().toLowerCase(Locale.ROOT));
            ListTag tasks = new ListTag();
            for (String line : lines) tasks.add(StringTag.valueOf(line));
            root.put("Tasks", tasks);
            result.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
            event.setOutput(result); event.setCost(1); event.setMaterialCost(1);
        } else if (left.is(ModItems.NOODLE_COMMAND_BOOK.get()) && right.is(ModItems.NOODLE_TRAINING_MEDAL.get())) {
            CompoundTag medal = data(right);
            String phrase = medal.getString("Phrase");
            ListTag tasks = medal.getList("Tasks", Tag.TAG_STRING);
            if (phrase.isBlank() || tasks.isEmpty()) return;
            ItemStack result = left.copy();
            CompoundTag root = data(result);
            CompoundTag commands = root.getCompound("NoodleCommands");
            commands.put(phrase, tasks.copy());
            root.put("NoodleCommands", commands);
            result.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
            event.setOutput(result); event.setCost(1); event.setMaterialCost(1);
        }
    }
}
