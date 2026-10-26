package com.devaxo.templeoftheaxolotl.pre07;
import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.registries.*;
public final class Pre07Items {
 public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TempleOfTheAxolotl.MODID);
 public static final DeferredItem<BlockItem> CELESTIAL_CACHE = ITEMS.registerSimpleBlockItem("celestial_cache", Pre07Blocks.CELESTIAL_CACHE);
 public static final DeferredItem<BlockItem> TRIAL_TRAP_SPAWNER = ITEMS.registerSimpleBlockItem("trial_trap_spawner", Pre07Blocks.TRIAL_TRAP_SPAWNER);
 public static final java.util.List<DeferredItem<Item>> STONES = new java.util.ArrayList<>();
 public static final java.util.List<DeferredItem<Item>> CRACKED = new java.util.ArrayList<>();
 static { for (int tier=1;tier<=4;tier++) {
  final int t=tier;
  STONES.add(ITEMS.register("tier_"+tier+"_ominous_trap_stone", () -> new Item(new Item.Properties().stacksTo(8).fireResistant())));
  CRACKED.add(ITEMS.register("tier_"+tier+"_cracked_ominous_trap_stone", () -> new CrackedTrapStoneItem(t, new Item.Properties().stacksTo(8).fireResistant())));
 }}
 private Pre07Items() {}
}
