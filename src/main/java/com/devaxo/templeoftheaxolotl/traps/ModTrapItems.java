package com.devaxo.templeoftheaxolotl.traps;

import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModTrapItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("temple_of_the_axolotl");
    public static final DeferredItem<BlockItem> RNGESUS_TRAP = ITEMS.registerSimpleBlockItem("rngesus_trap", ModTrapBlocks.RNGESUS_TRAP);
    public static final DeferredItem<BlockItem> SPIKE_TRAP = ITEMS.registerSimpleBlockItem("spike_trap", ModTrapBlocks.SPIKE_TRAP);
    public static final DeferredItem<BlockItem> HONEYBRICK_TRAP = ITEMS.registerSimpleBlockItem("honeybrick_trap", ModTrapBlocks.HONEYBRICK_TRAP);
    public static final DeferredItem<BlockItem> SNEAK_TRAP = ITEMS.registerSimpleBlockItem("sneak_trap", ModTrapBlocks.SNEAK_TRAP);
    private ModTrapItems() {}
}
