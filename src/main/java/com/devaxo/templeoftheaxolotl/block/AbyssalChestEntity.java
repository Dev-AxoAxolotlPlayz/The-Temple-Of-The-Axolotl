package com.devaxo.templeoftheaxolotl.block;

import com.devaxo.templeoftheaxolotl.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.UUID;

public final class AbyssalChestEntity extends BlockEntity {
    private UUID owner;
    public AbyssalChestEntity(BlockPos pos,BlockState state) { super(com.devaxo.templeoftheaxolotl.block.AbyssalChestEntities.CHEST.get(),pos,state); }
    public UUID owner(){return owner;}
    public void setOwner(UUID id){owner=id;setChanged();}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);if(owner!=null)tag.putUUID("RepositoryOwner",owner);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);owner=tag.hasUUID("RepositoryOwner")?tag.getUUID("RepositoryOwner"):null;}
}
