package com.devaxo.templeoftheaxolotl.axolite;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Server-side drop restriction is enforced by the block's loot table tool predicate. */
public class AxoliteOreBlock extends Block {
    public AxoliteOreBlock(BlockBehaviour.Properties properties) { super(properties); }
}
