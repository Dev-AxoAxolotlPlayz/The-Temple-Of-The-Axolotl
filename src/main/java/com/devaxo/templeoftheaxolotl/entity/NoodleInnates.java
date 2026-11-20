package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import java.util.*;

/** Stage II medal metadata and server-side innate compatibility rules. */
public final class NoodleInnates {
    private NoodleInnates() {}
    private static final String MEDAL = "NoodleInnateMedal";
    private static final String[] NAMES = {"creeper_sense","blink","impervious","devilish","angelic","berserker","tank","prospector","ghost"};
    private static final Set<String> TYPES = Set.of(NAMES);
    private static final Set<String> FUSIONS = Set.of("blink:impervious","devilish:impervious","angelic:impervious","angelic:devilish","berserker:impervious","impervious:tank");
    private static final Set<String> GHOST_EXCLUDES = Set.of("creeper_sense","berserker","tank","devilish");
    private static CompoundTag root(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }
    private static String key(String a, String b) { return a.compareTo(b) < 0 ? a+":"+b : b+":"+a; }
    public static String kind(ItemStack stack) {
        if (!stack.is(ModItems.NOODLE_TRAINING_MEDAL.get())) return "";
        return root(stack).getCompound(MEDAL).getString("Kind");
    }
    public static List<String> traits(ItemStack stack) {
        CompoundTag medal = root(stack).getCompound(MEDAL);
        List<String> out = new ArrayList<>();
        ListTag list = medal.getList("Traits", Tag.TAG_STRING);
        for (int i=0;i<list.size();i++) if (TYPES.contains(list.getString(i)) && !out.contains(list.getString(i))) out.add(list.getString(i));
        return out;
    }
    public static boolean fused(ItemStack stack) { return root(stack).getCompound(MEDAL).getBoolean("Fused"); }
    public static int cost(ItemStack stack) { return fused(stack) ? 3 : traits(stack).size(); }
    public static boolean validPair(String a, String b) { return FUSIONS.contains(key(a,b)); }
    public static boolean incompatible(String a, String b) {
        if (a.equals(b)) return true;
        if ((a.equals("ghost") && GHOST_EXCLUDES.contains(b)) || (b.equals("ghost") && GHOST_EXCLUDES.contains(a))) return true;
        return validPair(a,b);
    }
    public static boolean canApply(List<String> existing, ItemStack medal) {
        List<String> incoming = traits(medal);
        if (incoming.isEmpty() || incoming.size() > 2 || (incoming.size() == 2 && (!fused(medal) || !validPair(incoming.get(0),incoming.get(1))))) return false;
        if (incoming.contains("ghost") && incoming.size()>1) return false;
        if (cost(medal) > 5) return false;
        for (String t : incoming) for (String old : existing) if (incompatible(t,old)) return false;
        return true;
    }
    public static ItemStack create(String kind, List<String> traits, boolean fused) {
        ItemStack out = new ItemStack(ModItems.NOODLE_TRAINING_MEDAL.get());
        CompoundTag root = root(out), medal = new CompoundTag();
        medal.putString("Kind",kind);
        ListTag list = new ListTag();
        for (String trait : traits) list.add(StringTag.valueOf(trait));
        medal.put("Traits",list); medal.putBoolean("Fused",fused);
        root.put(MEDAL,medal); out.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        String name = kind.equals("demi") ? "Training Medal - Demi" : fused ? "Demi Training Medal - "+String.join(" & ",traits) : "Training Medal - "+kind.replace('_',' ');
        out.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return out;
    }
    public static String recipeKind(SmithingRecipeInput input) {
        if (!input.base().is(ModItems.NOODLE_TRAINING_MEDAL.get())) return "";
        Item template = input.template().getItem(), addition = input.addition().getItem();
        if (template == Items.NETHER_STAR && addition == Items.NETHER_STAR) return "demi";
        if (!kind(input.base()).isEmpty()) return "";
        if (template == Items.TNT && addition == Items.GUNPOWDER) return "creeper_sense";
        if (template == Items.ENDER_EYE && addition == Items.ENDER_PEARL) return "blink";
        if (template == Items.NETHERITE_BLOCK && addition == Items.ENCHANTED_GOLDEN_APPLE) return "impervious";
        if (template == Items.MAGMA_BLOCK && addition == Items.BLAZE_ROD) return "devilish";
        if (template == Items.GLOWSTONE && addition == Items.PHANTOM_MEMBRANE) return "angelic";
        if (template == Items.BLAZE_ROD && addition == Items.IRON_SWORD) return "berserker";
        if (template == Items.IRON_BLOCK && addition == Items.DIAMOND_CHESTPLATE) return "tank";
        if (template == Items.DIAMOND && addition == Items.IRON_PICKAXE) return "prospector";
        if (template == Items.GHAST_TEAR && addition == Items.SOUL_LANTERN) return "ghost";
        return "";
    }
    public static boolean fusionInput(SmithingRecipeInput input) {
        if (!"demi".equals(kind(input.template()))) return false;
        if (!input.base().is(ModItems.NOODLE_TRAINING_MEDAL.get()) || !input.addition().is(ModItems.NOODLE_TRAINING_MEDAL.get())) return false;
        List<String> a=traits(input.base()), b=traits(input.addition());
        return a.size()==1 && b.size()==1 && validPair(a.get(0),b.get(0)) && !a.contains("ghost") && !b.contains("ghost");
    }
    public static ItemStack smith(SmithingRecipeInput input) {
        if (fusionInput(input)) return create("fusion",List.of(traits(input.base()).get(0),traits(input.addition()).get(0)),true);
        String kind=recipeKind(input);
        return kind.isEmpty() ? ItemStack.EMPTY : create(kind,kind.equals("demi")?List.of():List.of(kind),false);
    }
    public static List<String> readTraits(CompoundTag tag) {
        List<String> result=new ArrayList<>();
        ListTag list=tag.getList("NoodleInnates",Tag.TAG_STRING);
        for(int i=0;i<list.size();i++) if(TYPES.contains(list.getString(i)) && !result.contains(list.getString(i))) result.add(list.getString(i));
        return result;
    }
    public static void writeTraits(CompoundTag tag, Collection<String> traits) {
        ListTag list=new ListTag(); for(String t:traits) if(TYPES.contains(t)) list.add(StringTag.valueOf(t)); tag.put("NoodleInnates",list);
    }
}
