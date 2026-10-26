package com.devaxo.templeoftheaxolotl.eclipse;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Persistent raw-XP bank and shared offering slots. Ritual consumption comes in stage 2. */
public final class EclipseAltarEntity extends BlockEntity implements software.bernie.geckolib.animatable.GeoBlockEntity {
    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache animationCache = software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
    @Override public void registerControllers(software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) { }
    @Override public software.bernie.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() { return animationCache; }

    private int xpPoints;
    public final SimpleContainer offerings = new SimpleContainer(6) {
        @Override public void setChanged() {
            super.setChanged();
            EclipseAltarEntity.this.setChanged();
        }
    };

    public EclipseAltarEntity(BlockPos pos, BlockState state) {
        super(EclipseBlockEntities.ALTAR.get(), pos, state);
    }
    public int getXpPoints() { return xpPoints; }
    public void setXpPoints(int value) { xpPoints = Math.max(0, value); setChanged(); }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("RawXP", xpPoints);
        NonNullList<ItemStack> items = NonNullList.withSize(6, ItemStack.EMPTY);
        for (int i = 0; i < 6; i++) items.set(i, offerings.getItem(i));
        ContainerHelper.saveAllItems(tag, items, registries);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        xpPoints = Math.max(0, tag.getInt("RawXP"));
        NonNullList<ItemStack> items = NonNullList.withSize(6, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        for (int i = 0; i < 6; i++) offerings.setItem(i, items.get(i));
    }
}
