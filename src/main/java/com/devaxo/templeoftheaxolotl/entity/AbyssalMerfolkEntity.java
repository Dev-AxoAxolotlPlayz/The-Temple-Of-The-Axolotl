package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Temporary passive drowned-shaped merfolk; never alters vanilla drowned. */
public class AbyssalMerfolkEntity extends Drowned {
    public AbyssalMerfolkEntity(EntityType<? extends Drowned> type, Level level) {
        super(type, level);
        setCustomName(Component.literal("Testifixolotl"));
        setCustomNameVisible(true);
        setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        // Deliberately omit ALL zombie/drowned attack and target goals.
        this.goalSelector.addGoal(1, new RandomSwimmingGoal(this, 1.0D, 20));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            // Drowned vanilla equipment initialization can otherwise give tridents.
            if (!getMainHandItem().isEmpty()) setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            if (!getOffhandItem().isEmpty()) setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            // Temporary recognizable Allfather item. This is not a wearable 3D helmet model.
            if (!getItemBySlot(EquipmentSlot.HEAD).is(ModItems.AXOLOTL_ABSOLUTE.get())) {
                setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.AXOLOTL_ABSOLUTE.get()));
                setDropChance(EquipmentSlot.HEAD, 0.0F);
            }
        }
    }



    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        return false;
    }
}
