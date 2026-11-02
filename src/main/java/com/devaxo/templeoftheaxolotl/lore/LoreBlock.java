package com.devaxo.templeoftheaxolotl.lore;

import com.devaxo.templeoftheaxolotl.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** One-use, globally consumed lore source; Patchouli advancement is player-specific. */
public final class LoreBlock extends BaseEntityBlock {
    public static final MapCodec<LoreBlock> CODEC = simpleCodec(LoreBlock::new);
    // Chronological diary sequence. IDs are unchanged to preserve saved advancement progress.
    private static final String[] MEMORY_IDS = {
            "first_fragment",
            "before_the_beginning_a_will_before_two",
            "before_the_beginning_the_two_brothers",
            "the_ring_of_the_one_opposing_powers",
            "the_ring_of_the_one_an_impossible_crown",
            "the_first_fall_envy",
            "the_second_fall_temptation",
            "the_second_fall_a_lesson_learned",
            "the_first_fall_broken_wings",
            "the_first_fall_the_ring_cast_away",
            "the_forgotten_prison_temple_00001",
            "the_forgotten_prison_false_temples",
            "the_forgotten_prison_the_shattered_key",
            "echoes_of_divinity_shards_and_orbs",
            "echoes_of_divinity_the_celestial_key",
            "the_original_sin_the_fractured_origin",
            "echoes_of_divinity_the_decoy_sanctums",
            "the_brothers_fate_freedom",
            "the_original_sin_the_final_confrontation",
            "the_brothers_fate_forgiveness",
            "the_brothers_fate_reunion",
            "the_second_fall_the_world_darkens"
    };
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public LoreBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }
    @Override public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LoreBlockEntity(pos, state);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        // Award the first undiscovered memory, not a random one.
        // Previously unlocked advancements are skipped, including on existing worlds.
        net.minecraft.advancements.AdvancementHolder chosen = null;
        for (String id : MEMORY_IDS) {
            var memoryId = ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "lore/" + id);
            var holder = serverPlayer.server.getAdvancements().get(memoryId);
            if (holder != null && !serverPlayer.getAdvancements().getOrStartProgress(holder).isDone()) {
                chosen = holder;
                break;
            }
        }
        if (chosen == null) {
            serverPlayer.sendSystemMessage(Component.literal("There are no undiscovered memories left here."));
            return InteractionResult.SUCCESS;
        }
        boolean awarded = serverPlayer.getAdvancements().award(chosen, "discovered");
        if (!awarded || !serverPlayer.getAdvancements().getOrStartProgress(chosen).isDone()) {
            serverPlayer.sendSystemMessage(Component.literal("The memory could not be recorded. The block remains intact."));
            return InteractionResult.SUCCESS;
        }
        serverPlayer.sendSystemMessage(Component.literal("A forgotten memory has been added to the diary."));
        level.setBlock(pos, ModBlocks.BROKEN_LORE_BLOCK.get().defaultBlockState()
                .setValue(BrokenLoreBlock.FACING, state.getValue(FACING)), 3);
        return InteractionResult.SUCCESS;
    }
}
