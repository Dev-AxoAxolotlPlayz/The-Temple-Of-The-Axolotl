package com.devaxo.templeoftheaxolotl.shipyard;

import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.List;
import java.util.ArrayList;

/** Fixed, position-sensitive 9x9 shipyard assemblies; single item per occupied slot. */
public final class ShipyardRecipes {
 private ShipyardRecipes() {}
 public record Assembly(String id, String[] pattern, Item output) {}
 public static List<Assembly> all() {
   return List.of(
     new Assembly("cockpit_glass", new String[]{".........", ".LLLLLLL.", "PRRRRRRRP", "PMAAAAAMP", "PMAAAAAMP", "PMAAAAAMP", "PRRRRRRRP", ".LLLLLLL.", "........."}, ModItems.COCKPIT_GLASS.get()),
     new Assembly("air_jet_thruster", new String[]{"..B......", ".BW......", "PWMMMMMR.", "PPMGRGRLL", "PPMRGRGLL", "PPMGRGRLL", "PWMMMMMR.", ".BW......", "..B......"}, ModItems.AIR_JET_THRUSTER.get()),
     new Assembly("tail_rudder", new String[]{"..LLLL...", ".BPPPPL..", ".WPPPPL..", ".SPPPL...", ".SPPPL...", ".SPPPPL..", ".WPPPPL..", ".BLLLLL..", "........."}, ModItems.TAIL_RUDDER.get()),
     new Assembly("shorkolotl_cockpit", new String[]{".........", "....LLL..", "...QQQQQL", "..QPPPPQL", "..CAMMPQL", ".LQPPPPQL", ".LLQQQQQL", "....LLL..", "........."}, ModItems.SHORKOLOTL_COCKPIT.get()),
     new Assembly("flipper_stabilizer", new String[]{".........", ".........", ".........", "..BWWWB..", ".BWSSSWB.", ".BPPPPPB.", ".PQQQQQP.", ".PQLLLQP.", "...LLL..."}, ModItems.FLIPPER_STABILIZER.get()),
     new Assembly("shorkolotl_hull", new String[]{"FLLLLLLLF", "QQQQQQQQQ", "QBPMMMPBQ", "QWPMAAPWQ", "QSPMASPSQ", "QWPMAAPWQ", "QBPMMMPBQ", "QQQQQQQQQ", "FLLLLLLLF"}, ModItems.SHORKOLOTL_HULL.get()),
     new Assembly("shorkolotl_tail", new String[]{".........", ".........", "LLQQQQLL.", "QQQPPPQQ.", "GGGPPPSD.", "TBTPPPWQ.", "QQQPPPBQ.", "LLQQQQQQ.", "........."}, ModItems.SHORKOLOTL_TAIL.get()),
     new Assembly("shorkolotl_100_item", new String[]{".........", ".LLLLLLL.", "PPPMMMMMM", "PPPPPPPPM", "PKLLHLLYM", "PPPPPPPPM", "PPPMMMMMM", ".LLLLLLL.", "........."}, ModItems.SHORKOLOTL_100_ITEM.get())
   );
 }
 public static Item ingredient(char c) {
   return switch(c) {
     case 'P' -> ModItems.REINFORCED_IRON_PLATING.get();
     case 'M' -> ModItems.MALLEABLE_SHEETMETAL.get();
     case 'A' -> ModItems.PRESSURE_TREATED_ACRYLIC_PANEL.get();
     case 'L' -> ModItems.GLOW_STRIP.get();
     case 'R' -> ModItems.METAL_ROD.get();
     case 'G' -> ModItems.METAL_GEAR.get();
     case 'V' -> ModItems.RIVET.get();
     case 'B' -> ModItems.BOLT.get();
     case 'W' -> ModItems.WASHER.get();
     case 'S' -> ModItems.SPRING.get();
     case 'Q' -> net.minecraft.world.item.Items.QUARTZ_BLOCK;
     case 'C' -> ModItems.COCKPIT_GLASS.get();
     case 'T' -> ModItems.AIR_JET_THRUSTER.get();
     case 'D' -> ModItems.TAIL_RUDDER.get();
     case 'F' -> ModItems.FLIPPER_STABILIZER.get();
     case 'K' -> ModItems.SHORKOLOTL_COCKPIT.get();
     case 'H' -> ModItems.SHORKOLOTL_HULL.get();
     case 'Y' -> ModItems.SHORKOLOTL_TAIL.get();
     default -> Items.AIR;
   };
 }
 public static Assembly match(Container contents) {
   for (Assembly recipe : all()) {
     boolean matches = true;
     for (int i=0;i<81;i++) {
       ItemStack stack = contents.getItem(i);
       Item expected = ingredient(recipe.pattern()[i/9].charAt(i%9));
       if (expected == Items.AIR ? !stack.isEmpty() : !stack.is(expected) || stack.getCount()<1) { matches=false; break; }
     }
     if (matches) return recipe;
   }
   return null;
 }
}
