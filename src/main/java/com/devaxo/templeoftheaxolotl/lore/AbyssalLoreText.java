package com.devaxo.templeoftheaxolotl.lore;

/** Initial editable lore draft for the five creatures, three structures and six registered biomes. */
public final class AbyssalLoreText {
    private AbyssalLoreText() {}
    private static final String[] TEXT = {
        "The Axoleviathan is a colossal legend of the deep. Sailors speak of a shadow vast enough to swallow the moonlight filtering through the sea.",
        "Tales of the Axokraken describe an elusive giant beneath the currents. Its true habits remain a mystery to the surface world.",
        "Axothulu, daughter of AxoCthulu, is an architect and companion. The Abyssal Egg represents a path to meeting her, rather than challenging her father. A bond with her is the beginning of a shared journey.",
        "Abyssal Axolotls are curious companions of the deep. Their loyalty can outlast even the darkest journey, especially when treated with care.",
        "Merolotls are the people of the Abyss. Their settlements and traditions suggest a civilization built in harmony with the ocean's mysteries.",
        "The Watergate joins distant waters. Its ancient frame marks a passage into the Abyss, where familiar rules give way to stranger tides.",
        "Scattered Merolotl ruins preserve traces of a once-busy world. Each stone invites questions about the lives and journeys of those who built it.",
        "Merolotl villages stand as living signs of community beneath the waves. Travelers may find shelter, trade and stories among their inhabitants.",
        "Abyssal Plateaus rise above the seafloor like drowned mesas. Their sheer sides conceal a landscape shaped by long ages of ocean currents.",
        "Coral shelves spread bright reefs across the dark waters. Delicate colonies thrive where the currents deliver light and nourishment.",
        "Kelp forests sway through the Abyssal Kelp Seas. Their tall fronds shelter small creatures and make winding corridors for explorers.",
        "Abyssal Ravines plunge toward the pressure-heavy depths. Their steep walls remind travelers that the ocean hides more than it reveals.",
        "The Abyssal Seafloor stretches between the great formations. Quiet sediment, ancient stone and scattered life record the ocean's patient work.",
        "Abyssal Sandbars rise in gentler shallows. Shifting sediment creates temporary paths and resting grounds between deeper waters.",
        "The Thermocline Layer stretches from Y=101 to Y=110: a ten-block cloud sea that feels solid to outsiders. Abyssal creatures evolved extra pairs of eyes and a Subthermal Trait to navigate its strange gradients. Axoleviathans, Axokrakens, Merolotls and Abyssal Axolotls swim through; the Shorkolotl-100 imitates this gift through engineering, carrying its passengers safely below.",
        "AxoCthulu is the father of Axothulu and the sovereign at the heart of the final Abyssal confrontation. Legends speak of the Cosmic Trident and the Eye of the Abyss, but the full truth of his arena remains to be uncovered.",
        "Axobloops inhabit the waters beneath the Thermocline.",
        "Megolotls are enormous ocean creatures, found in the Abyss and occasionally in deep surface oceans.",
        "Abyssal Anglers dwell in the deeper waters below the Thermocline.",
        "Abyssal Jellyfish drift in the upper Abyss and surface oceans.",
        "Ghostfish inhabit the waters below the Thermocline."
    };
    public static String text(int id) { return id>=0 && id<TEXT.length ? TEXT[id] : ""; }
}
