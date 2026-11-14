package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.server.level.ServerLevel;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Player suggestion intake only. Never approves, clears terrain, or starts building. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class WaifuAxoluthuConstructionRequests {
    private static final Pattern REQUEST = Pattern.compile(
        "^hey[ ,]+axoluthu[,! ]+what if we build (?:a|an|the) (.+?) here[?.! ]*$",
        Pattern.CASE_INSENSITIVE);
    public static final List<String> TYPES = List.of("workshop", "house", "storage hut", "library",
        "farm", "church", "palace", "smithy", "market", "watchtower");
    private static final String KEY = "phase35OwnerSuggestion";
    private WaifuAxoluthuConstructionRequests() {}

    /** Design handoff only. The owner chooses the actual site using the Celestial Blueprint. */
    @SubscribeEvent
    public static void onOwnerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        ServerLevel level = player.serverLevel();
        for (WaifuAxoluthuEntity axo : level.getEntitiesOfClass(
                WaifuAxoluthuEntity.class, player.getBoundingBox().inflate(48),
                a -> player.getUUID().equals(a.getBondedPlayer()))) {
            CompoundTag memory = axo.getArchitecturalLearning();
            CompoundTag request = memory.getCompound(KEY);
            if (!request.getBoolean("pending") || !"AWAITING_SURVEY".equals(request.getString("phase"))
                    || !request.hasUUID("owner") || !player.getUUID().equals(request.getUUID("owner"))) continue;
            String type = WaifuAxoluthuVillageStructures.normalize(request.getString("type"));
            if (type.isEmpty()) {
                request.putString("phase", "UNSUPPORTED_DESIGN");
                player.sendSystemMessage(Component.literal("[Axoluthu] I haven't learned that building type yet! QwQ"));
            } else {
                // Do not bind a design to a surveyed plot: placement belongs to the owner.
                request.remove("blueprintPreview");
                request.putString("phase", "PROPOSAL_READY");
                player.sendSystemMessage(Component.literal("[Axoluthu] My " + type
                        + " design is in the Architect's Journal! Claim the Celestial Blueprint,"
                        + " place its cyan projection, rotate it and lock the site! :D"));
            }
            request.putBoolean("constructionAllowed", false);
            memory.put(KEY, request);
            com.devaxo.templeoftheaxolotl.network.AxoluthuJournalPayload.send(player);
        }
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        submitChatRequest(event.getPlayer(), event.getRawText().trim());
    }

    /** Execute the exact same proposal intake from chat or /hey; never authorizes construction. */
    public static void submitChatRequest(ServerPlayer player, String text) {
        Matcher matcher = REQUEST.matcher(text);
        if (!matcher.matches()) return;
        String type = matcher.group(1).trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (!TYPES.contains(type)) {
            player.sendSystemMessage(Component.literal("[Axoluthu] I know these building types: "
                    + String.join(", ", TYPES) + ". :3"));
            return;
        }
        HitResult hit = player.pick(64.0, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            player.sendSystemMessage(Component.literal("[Axoluthu] Point at a block so I know where to survey! :3"));
            return;
        }
        BlockPos target = blockHit.getBlockPos();
        List<WaifuAxoluthuEntity> candidates = player.serverLevel().getEntitiesOfClass(
                WaifuAxoluthuEntity.class, player.getBoundingBox().inflate(48),
                axo -> player.getUUID().equals(axo.getBondedPlayer()));
        if (candidates.isEmpty()) {
            player.sendSystemMessage(Component.literal("[Axoluthu] I need to be nearby to hear your idea!"));
            return;
        }
        WaifuAxoluthuEntity axo = candidates.get(0);
        CompoundTag learning = axo.getArchitecturalLearning();
        CompoundTag existing = learning.getCompound(KEY);
        if (existing.getBoolean("pending")) {
            player.sendSystemMessage(Component.literal("[Axoluthu] I still have your previous suggestion to review! :3"));
            return;
        }
        CompoundTag request = new CompoundTag();
        request.putBoolean("pending", true);
        request.putString("type", type);
        request.putLong("target", target.asLong());
        request.putString("dimension", player.level().dimension().location().toString());
        request.putUUID("owner", player.getUUID());
        request.putLong("createdAt", player.serverLevel().getGameTime());
        request.putString("phase", "AWAITING_SURVEY");
        request.putBoolean("constructionAllowed", false);
        learning.put(KEY, request);
        player.sendSystemMessage(Component.literal("[Axoluthu] Ooo, a " + type + "! I'll consider "
                + target.toShortString() + " for our village! ^u^ (Survey beginning!)"));
    }
}
