package com.devaxo.templeoftheaxolotl.abyss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class WatergateEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int awakeningStage;
    private int chunkProgress;

    public int getAwakeningStage() { return awakeningStage; }
    public int getChunkProgress() { return chunkProgress; }

    public void setProgress(int stage, int progress) {
        stage = Math.max(0, Math.min(14, stage));
        progress &= 255;
        if (awakeningStage == stage && chunkProgress == progress) return;
        awakeningStage = stage;
        chunkProgress = progress;
        setChanged();
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("AwakeningStage", awakeningStage);
        tag.putInt("ChunkProgress", chunkProgress);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        awakeningStage = Math.max(0, Math.min(14, tag.getInt("AwakeningStage")));
        chunkProgress = tag.getInt("ChunkProgress") & 255;
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public WatergateEntity(BlockPos pos, BlockState state) { super(WatergateEntities.WATERGATE.get(), pos, state); }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) { }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
