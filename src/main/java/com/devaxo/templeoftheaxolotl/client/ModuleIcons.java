package com.devaxo.templeoftheaxolotl.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import com.devaxo.templeoftheaxolotl.registry.ModBlocks;
final class ModuleIcons {
    static void item(GuiGraphics g,int x,int y,int id) {
        var item=switch(id) {
            case 0 -> Items.GOLDEN_CARROT;
            case 1 -> Items.CONDUIT;
            case 2 -> Items.DIAMOND_PICKAXE;
            case 3 -> ModItems.CELESTIAL_MAGNET.get();
            case 4 -> ModBlocks.DIARY_ITEM.get();
            case 5 -> ModBlocks.FIELD_GUIDE_ITEM.get();
            case 6 -> ModItems.NOODLE_COMMAND_BOOK.get();
            case 7 -> ModItems.ABYSSAL_COMPANION_REGISTRY.get();
            case 8 -> ModItems.ABYSSAL_LANTERN.get();
            case 9 -> Items.TRIDENT;
            case 10 -> Items.CARROT;
            case 12 -> ModItems.AXOLUTHUS_PROMISE.get();
            case 11 -> ModItems.ABYSSAL_GILLS.get();
            default -> Items.AIR;
        };
        g.renderFakeItem(new ItemStack(item),x,y);
    }
    static void glyph(GuiGraphics g,int x,int y,String name) {
        String[] rows=switch(name) {
            case "arrow" -> new String[]{"...#...","..##...",".######","#######",".######","..##...","...#..."};
            case "prev" -> new String[]{"#....#.","#...##.","#..###.","#.####.","#..###.","#...##.","#....#."};
            case "next" -> new String[]{".#....#",".##...#",".###..#",".####.#",".###..#",".##...#",".#....#"};
            case "play" -> new String[]{".#.....",".##....",".###...",".####..",".###...",".##....",".#....."};
            case "pause" -> new String[]{".##.##.",".##.##.",".##.##.",".##.##.",".##.##.",".##.##.",".##.##."};
            case "shuffle" -> new String[]{"##...##","..#.#.#","...#...","...#...","..#.#..","##...##",".....##"};
            case "repeat" -> new String[]{".#####.","#....##","#....#.",".......",".#....#","##....#",".#####."};
            default -> new String[]{"#.....#",".#...#.","..#.#..","...#...","..#.#..",".#...#.","#.....#"};
        };
        for(int r=0;r<7;r++) for(int c=0;c<7;c++) if(rows[r].charAt(c)=='#') g.fill(x+1+c*2,y+1+r*2,x+3+c*2,y+3+r*2,0xFFE0F4FF);
    }
}
