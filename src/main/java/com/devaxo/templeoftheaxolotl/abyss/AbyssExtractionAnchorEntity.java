package com.devaxo.templeoftheaxolotl.abyss;

import com.devaxo.templeoftheaxolotl.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import java.util.UUID;

public final class AbyssExtractionAnchorEntity extends BlockEntity {
    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(
            net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, TempleOfTheAxolotl.MODID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AbyssExtractionAnchorEntity>> TYPE =
            TYPES.register("abyss_extraction_anchor", () -> BlockEntityType.Builder.of(
                    AbyssExtractionAnchorEntity::new, ModBlocks.ABYSS_EXTRACTION_ANCHOR.get()).build(null));
    private UUID owner;
    public AbyssExtractionAnchorEntity(BlockPos pos, BlockState state) { super(TYPE.get(), pos, state); }
    public void setOwner(UUID uuid) { owner = uuid; setChanged(); }
    public boolean isOwner(UUID uuid) { return owner != null && owner.equals(uuid); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) tag.putUUID("Owner", owner);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
