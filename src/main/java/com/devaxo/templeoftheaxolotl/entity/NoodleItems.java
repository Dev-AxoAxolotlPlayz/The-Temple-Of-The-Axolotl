package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Persist a complete tamed noodle in a bucket, including owner, name, and queued supplies. */
public final class NoodleItems {
    private NoodleItems() {}
    public static ItemStack makeFilledBucket(AbyssalAxolotlEntity noodle) {
        CompoundTag entity = new CompoundTag();
        noodle.saveWithoutId(entity);
        ItemStack stack = new ItemStack(ModItems.NOODLE_BUCKET.get());
        CompoundTag data = new CompoundTag();
        data.put("Noodle", entity);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return stack;
    }
    public static CompoundTag bucketData(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        return custom == null ? new CompoundTag() : custom.copyTag().getCompound("Noodle");
    }
}
