package com.devaxo.templeoftheaxolotl.client;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import vazkii.patchouli.client.book.text.BookTextParser;

/** Phase 1.4.11: diagnose the diary-only Patchouli font command. */
public final class DiaryFontRegistration {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation HANDWRITING =
            ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "lilomon");
    private static boolean registered;
    private static boolean loggedFirstUse;

    private DiaryFontRegistration() {}

    /** Called from the existing client-side initialization in TempleOfTheAxolotl. */
    public static synchronized void register() {
        if (registered) return;
        BookTextParser.register(state -> {
            state.modifyStyle(style -> style.withFont(HANDWRITING));
            if (!loggedFirstUse) {
                loggedFirstUse = true;
                LOGGER.info("[DiaryFont] Patchouli processed $(lilomon); requested font={}", HANDWRITING);
            }
            return "";
        }, "lilomon");
        registered = true;
        LOGGER.info("[DiaryFont] Registered Patchouli $(lilomon) formatting command");
    }
}
