package com.devaxo.templeoftheaxolotl.trident;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.devaxo.templeoftheaxolotl.item.ModItems;
public final class PersistentTridents {
    public static final String MARK="TotaPersistentTrident";
    public static final String RECALL="TotaTridentRecall";
    public static String key(ItemStack stack) {return stack.is(ModItems.COSMIC_TRIDENT.get())?"TotaActiveCosmic":"TotaActiveAxolite";}
    public static boolean marked(ThrownTrident e){return e.getPersistentData().getBoolean(MARK);}
    public static boolean valid(ThrownTrident e, Player p, ItemStack stack){var d=p.getPersistentData();return d.hasUUID(key(stack))&&d.getUUID(key(stack)).equals(e.getUUID());}
    public static void clear(ThrownTrident e, Player p, ItemStack stack){if(valid(e,p,stack))p.getPersistentData().remove(key(stack));}
}
