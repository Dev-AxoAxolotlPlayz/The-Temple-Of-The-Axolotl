package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Stage 5.4f: visual-only site proposal and owner-confirmed permission.
 * Permission never bypasses claim protection or enables construction.
 */
public final class WaifuAxoluthuStage5SiteApproval {
    private static final String KEY = "stage5HousingProject";
    private WaifuAxoluthuStage5SiteApproval() {}

    private static CompoundTag project(WaifuAxoluthuEntity axo) {
        CompoundTag data = axo.getArchitecturalLearning();
        return data.contains(KEY, 10) ? data.getCompound(KEY) : null;
    }

    public static String propose(WaifuAxoluthuEntity axo, ServerPlayer owner) {
        CompoundTag p = project(axo);
        if (p == null || !p.contains("blueprintPreview",10)) return "Create a project and blueprint first.";
        if (!(axo.level() instanceof ServerLevel level) || level != owner.serverLevel()
                || !level.dimension().location().toString().equals(p.getString("dimension")))
            return "The player and proposed site must be in the same dimension.";
        int w=p.getInt("width"), d=p.getInt("depth");
        if(w<5 || d<5 || w>31 || d>31) return "Invalid site dimensions.";
        BlockPos origin=BlockPos.of(p.getLong("origin"));
        int occupied=0, unloaded=0;
        long fingerprint=1469598103934665603L;
        for(int x=0;x<w;x++) for(int z=0;z<d;z++) for(int y=0;y<=9;y++) {
            BlockPos at=origin.offset(x,y,z);
            if(!level.isLoaded(at)) { unloaded++; continue; }
            var state=level.getBlockState(at);
            fingerprint=(fingerprint ^ state.hashCode()) * 1099511628211L;
            if(y>0 && !state.isAir() && !state.canBeReplaced()) occupied++;
        }
        if(unloaded>0) return "Site has "+unloaded+" unloaded positions; cannot request approval.";
        p.putLong("approvalFingerprint",fingerprint);
        p.putInt("approvalObstructions",occupied);
        p.putBoolean("siteAuthorized",false);
        p.putBoolean("constructionAllowed",false);
        p.putString("phase","AWAITING_SITE_APPROVAL");
        p.putString("notes", occupied>0
                ? "This site contains "+occupied+" occupied positions. Terrain may be player-modified; permission requires an extra confirmation. No blocks will be altered."
                : "Proposed site outlined with visual markers. Awaiting player permission; no blocks changed.");
        markers(level,origin,w,d);
        owner.sendSystemMessage(Component.literal("[Axoluthu] I found a place for my house! >:3 Can I build here? Stand inside the glowing boundary to decide."));
        owner.sendSystemMessage(Component.literal("[Yes, build here]").withStyle(Style.EMPTY.withColor(0x55FFAA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/tota axoluthu architecture approve")))
                .append(Component.literal("   "))
                .append(Component.literal("[No, find another spot]").withStyle(Style.EMPTY.withColor(0xFF7777)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/tota axoluthu architecture reject"))))
                .append(Component.literal("   "))
                .append(Component.literal("[Suggest another area]").withStyle(Style.EMPTY.withColor(0x88CCFF)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/tota axoluthu architecture suggest")))));
        return "Proposed "+w+"x"+d+" boundary at "+origin.toShortString()+"; "+occupied+" occupied positions. Ghost markers are particles, not blocks.";
    }


    /** Player-guided alternative: use the player's current position as the center of
     * the same design. This only updates the saved proposal; it never edits blocks.
     * The player must explicitly approve the new site afterwards.
     */
    public static String suggest(WaifuAxoluthuEntity axo, ServerPlayer owner) {
        CompoundTag p = project(axo);
        if (p == null || !p.contains("blueprintPreview", 10)) return "Create a project and blueprint first.";
        String phase = p.getString("phase");
        if (!"AWAITING_SITE_APPROVAL".equals(phase) && !"CONFIRM_TERRAIN_CHANGES".equals(phase) && !"SITE_RECHECK_REQUIRED".equals(phase))
            return "No pending site proposal to relocate.";
        if (!(axo.level() instanceof ServerLevel level) || level != owner.serverLevel()
                || !level.dimension().location().toString().equals(p.getString("dimension")))
            return "Please visit the desired location in the same dimension as Axoluthu.";
        int w = p.getInt("width"), d = p.getInt("depth");
        if (w < 5 || d < 5 || w > 31 || d > 31) return "Invalid design dimensions.";
        BlockPos at = owner.blockPosition();
        BlockPos proposed = new BlockPos(at.getX() - w / 2, at.getY(), at.getZ() - d / 2);
        BlockPos old = BlockPos.of(p.getLong("origin"));
        if (old.equals(proposed)) return "Move to a different area and select Suggest another area again.";
        // Check the full proposed footprint and height before changing the project.
        for (int x = 0; x < w; x++) for (int z = 0; z < d; z++)
            for (int y = -1; y <= 12; y++) {
                BlockPos check = proposed.offset(x, y, z);
                if (check.getY() < level.getMinBuildHeight() || check.getY() >= level.getMaxBuildHeight()
                        || !level.isLoaded(check)) return "The suggested area is not fully loaded or exceeds build height.";
            }
        CompoundTag candidate = p.copy();
        candidate.putLong("origin", proposed.asLong());
        CompoundTag preview;
        try { preview = WaifuAxoluthuStage5Blueprint.create(candidate); }
        catch (IllegalArgumentException ex) { return "Suggested area rejected: " + ex.getMessage(); }
        // Invalidate old approval and audit data; retain her chosen architecture.
        p.putLong("origin", proposed.asLong());
        p.put("blueprintPreview", preview);
        p.putBoolean("siteAuthorized", false);
        p.putBoolean("constructionAllowed", false);
        p.remove("approvalFingerprint");
        p.remove("approvalObstructions");
        p.remove("inventoryAudit");
        p.putString("phase", "BLUEPRINT_REVIEW");
        p.putString("notes", "Player suggested a different site. Design retained; blueprint preview relocated. New terrain survey and explicit approval required.");
        // Relocation is a new proposal, not a request for the player to manually survey again.
        return "Got it! >:3 Suggested area at " + proposed.toShortString() + ". " + propose(axo, owner);
    }

    public static String decide(WaifuAxoluthuEntity axo, ServerPlayer owner, String choice) {
        CompoundTag p=project(axo);
        if(p==null || !"AWAITING_SITE_APPROVAL".equals(p.getString("phase"))
                && !"CONFIRM_TERRAIN_CHANGES".equals(p.getString("phase"))) return "No pending site approval.";
        if(!(axo.level() instanceof ServerLevel level) || level!=owner.serverLevel()
                || !level.dimension().location().toString().equals(p.getString("dimension"))) return "Wrong dimension.";
        BlockPos o=BlockPos.of(p.getLong("origin"));
        int w=p.getInt("width"),d=p.getInt("depth");
        BlockPos player=owner.blockPosition();
        if(player.getX()<o.getX() || player.getX()>=o.getX()+w || player.getZ()<o.getZ() || player.getZ()>=o.getZ()+d)
            return "Please stand inside the marked X-Z area to answer.";
        if("reject".equals(choice)) {
            p.putString("phase","SITE_REJECTED");
            p.putBoolean("siteAuthorized",false);
            p.putBoolean("constructionAllowed",false);
            p.putString("notes","Player rejected this location. A new location must be surveyed and proposed; no blocks changed.");
            return "Okay! I'll look for somewhere else. QwQ (Use architecture plan after starting a new project; automatic relocation is not implemented yet.)";
        }
        // Live validation: block-state hashes change with harmless growth, fluid updates,
        // and other normal world ticks. Recount actual obstructions instead of locking
        // the proposal behind an exact historical snapshot.
        int occupied = 0;
        for (int x=0; x<w; x++) for (int z=0; z<d; z++) for (int y=0; y<=9; y++) {
            BlockPos at=o.offset(x,y,z);
            if (!level.isLoaded(at)) return "Site chunks are not loaded yet; try again when nearby.";
            var state=level.getBlockState(at);
            if (y>0 && !state.isAir() && !state.canBeReplaced()) occupied++;
        }
        int surveyedOccupied=p.getInt("approvalObstructions");
        if (occupied>surveyedOccupied && "CONFIRM_TERRAIN_CHANGES".equals(p.getString("phase"))) {
            // New obstructions since the first confirmation: require a fresh first
            // confirmation rather than silently accepting new occupied positions.
            p.putString("phase","AWAITING_SITE_APPROVAL");
        }
        p.putInt("approvalObstructions",occupied);
        if(p.getInt("approvalObstructions")>0 && !"CONFIRM_TERRAIN_CHANGES".equals(p.getString("phase"))) {
            p.putString("phase","CONFIRM_TERRAIN_CHANGES");
            owner.sendSystemMessage(Component.literal("[Axoluthu] This area has occupied terrain and might include someone's landscaping! Are you REALLY sure? QwQ"));
            owner.sendSystemMessage(Component.literal("[Yes, I understand]").withStyle(Style.EMPTY.withColor(0xFFCC55)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/tota axoluthu architecture approve")))
                    .append(Component.literal("   "))
                    .append(Component.literal("[Find another spot]").withStyle(Style.EMPTY.withColor(0xFF7777)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/tota axoluthu architecture reject"))))
                    .append(Component.literal("   "))
                    .append(Component.literal("[Suggest another area]").withStyle(Style.EMPTY.withColor(0x88CCFF)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/tota axoluthu architecture suggest")))));
            return "Extra confirmation required because terrain could be player-modified.";
        }
        p.put("blueprintPreview",WaifuAxoluthuStage5Blueprint.create(p));
        p.putBoolean("siteAuthorized",true);
        p.putBoolean("constructionAllowed",false);
        p.putString("phase","SITE_APPROVED");
        p.putString("notes","Bonded player approved this site. Construction is still locked pending a final blueprint, protection checks and material preparation.");
        return "YAY! Site approved! >:3 Journal updated. Construction remains locked.";
    }

    /** Bounded, read-only relocation after rejection. Keeps her chosen architecture.
     * No new chunks are loaded and no world blocks are touched. */
    public static void tickAutonomousSurvey(WaifuAxoluthuEntity axo, ServerLevel level) {
        if (axo.tickCount % 100 != 0 || axo.getBondedPlayer() == null) return;
        CompoundTag p = project(axo);
        if (p == null || !"SITE_REJECTED".equals(p.getString("phase"))) return;
        if (!level.dimension().location().toString().equals(p.getString("dimension"))) return;
        int w = p.getInt("width"), d = p.getInt("depth");
        if (w < 5 || d < 5 || w > 31 || d > 31) return;
        BlockPos previous = BlockPos.of(p.getLong("origin"));
        BlockPos center = axo.blockPosition();
        long cycle = level.getGameTime() / 100;
        for (int attempt = 0; attempt < 24; attempt++) {
            long seed = (cycle * 1103515245L + attempt * 214013L + axo.getUUID().getLeastSignificantBits());
            int dx = (int) Math.floorMod(seed, 65) - 32;
            int dz = (int) Math.floorMod(seed >>> 16, 65) - 32;
            int x = center.getX() + dx, z = center.getZ() + dz;
            if (Math.abs(x - previous.getX()) < w + 4 && Math.abs(z - previous.getZ()) < d + 4) continue;
            if (!level.getChunkSource().hasChunk(x >> 4, z >> 4)
                    || !level.getChunkSource().hasChunk((x+w-1) >> 4, (z+d-1) >> 4)) continue;
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos origin = new BlockPos(x,y,z);
            boolean safe = true;
            for (int a = 0; a < w && safe; a++) for (int b = 0; b < d && safe; b++) {
                BlockPos ground = origin.offset(a,-1,b);
                if (!level.isLoaded(ground) || level.getBlockState(ground).isAir()
                        || !level.getFluidState(ground).isEmpty()) { safe=false; break; }
                for (int h=0; h<=11; h++) {
                    BlockPos test=origin.offset(a,h,b);
                    if (!level.isLoaded(test) || !level.getBlockState(test).isAir()
                            || !level.getFluidState(test).isEmpty()) { safe=false; break; }
                }
            }
            if (!safe) continue;
            CompoundTag candidate = p.copy();
            candidate.putLong("origin", origin.asLong());
            CompoundTag preview;
            try { preview = WaifuAxoluthuStage5Blueprint.create(candidate); }
            catch (IllegalArgumentException ignored) { continue; }
            p.putLong("origin", origin.asLong());
            p.put("blueprintPreview", preview);
            p.putBoolean("siteAuthorized",false);
            p.putBoolean("constructionAllowed",false);
            p.remove("approvalFingerprint");
            p.remove("approvalObstructions");
            p.remove("inventoryAuditAvailable");
            p.remove("inventoryAuditMissing");
            p.putString("phase","BLUEPRINT_REVIEW");
            p.putString("notes","I found a new candidate after the last one was rejected! It still needs player approval. No terrain changed.");
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(axo.getBondedPlayer());
            if (owner != null && owner.serverLevel() == level)
                owner.sendSystemMessage(Component.literal("[Axoluthu] I found another possible spot! >:3 Come see it and use /tota axoluthu architecture propose when you're ready."));
            return;
        }
    }

    /** Re-emit the purely visual boundary until a decision is recorded. No world blocks change. */
    public static void tickMarkers(WaifuAxoluthuEntity axo, ServerLevel level) {
        if (axo.tickCount % 8 != 0) return;
        CompoundTag p = project(axo);
        if (p == null) return;
        String phase = p.getString("phase");
        if (!"AWAITING_SITE_APPROVAL".equals(phase)
                && !"CONFIRM_TERRAIN_CHANGES".equals(phase)) return;
        if (!level.dimension().location().toString().equals(p.getString("dimension"))) return;
        int w = p.getInt("width"), d = p.getInt("depth");
        if (w < 5 || d < 5 || w > 31 || d > 31 || !p.contains("origin")) return;
        BlockPos origin = BlockPos.of(p.getLong("origin"));
        if (!level.isLoaded(origin) || !level.isLoaded(origin.offset(w - 1, 0, d - 1))) return;
        markers(level, origin, w, d);
    }

    private static void markers(ServerLevel level, BlockPos o,int w,int d) {
        for(int x=0;x<w;x++) for(int z=0;z<d;z++) {
            if(x!=0 && z!=0 && x!=w-1 && z!=d-1) continue;
            if(x%3!=0 && z%3!=0 && x!=w-1 && z!=d-1) continue;
            BlockPos at=o.offset(x,1,z);
            level.sendParticles(ParticleTypes.END_ROD,at.getX()+0.5,at.getY()+0.5,at.getZ()+0.5,3,0.15,0.3,0.15,0.005);
        }
    }
}
