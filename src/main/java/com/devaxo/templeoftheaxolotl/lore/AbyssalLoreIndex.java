package com.devaxo.templeoftheaxolotl.lore;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import java.util.*;

/** Independent Abyss discoveries; never touches Diary advancements. */
public final class AbyssalLoreIndex {
    private AbyssalLoreIndex() {}
    public static final String[] CREATURES = {"Axoleviathan", "Axokraken", "Axothulu", "Abyssal Axolotl", "Merolotls"};
    public static final String[] STRUCTURES = {"Watergate", "Merolotl Ruins", "Merolotl Villages"};
    public static final String[] BIOMES = {"Abyssal Plateaus", "Abyssal Coral Shelves", "Abyssal Kelp Seas", "Abyssal Ravines", "Abyssal Seafloor", "Abyssal Sandbars"};
    public static final String[] ENTRIES = java.util.stream.Stream.of(CREATURES, STRUCTURES, BIOMES, new String[]{"The Thermocline Layer", "AxoCthulu", "Axobloops", "Megolotls", "Abyssal Anglers", "Abyssal Jellyfish", "Ghostfish"}).flatMap(Arrays::stream).toArray(String[]::new);
    public static final int RETIRED = 14;
    public static int visibleCount() { return ENTRIES.length - 1; }
    public static int visibleId(int index) { return index >= RETIRED ? index + 1 : index; }
    private static final String KEY = "tota_abyssal_lore_discoveries";
    public static boolean has(ServerPlayer player, int id) { return player.getPersistentData().getCompound(KEY).getBoolean("entry_" + id); }
    public static void sync(ServerPlayer player) {
        int mask=0;
        for (int i=0;i<ENTRIES.length;i++) if (has(player,i)) mask |= (1<<i);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                new com.devaxo.templeoftheaxolotl.network.AbyssalLorePayload(mask));
    }
    public static int unlockNext(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData().getCompound(KEY);
        for (int i = 0; i < ENTRIES.length; i++) if (i != RETIRED && !tag.getBoolean("entry_" + i)) {
            tag.putBoolean("entry_" + i, true); player.getPersistentData().put(KEY, tag); sync(player); return i;
        }
        return -1;
    }
}
