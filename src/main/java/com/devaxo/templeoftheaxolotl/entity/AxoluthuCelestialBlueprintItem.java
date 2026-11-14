package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.network.AxoluthuProjectionSyncPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** One-use, owner-bound blueprint: choose anchor, rotate, then lock.
 * This stage saves a job; the cooperative placement engine is intentionally not enabled yet.
 */
public final class AxoluthuCelestialBlueprintItem extends Item {
    public AxoluthuCelestialBlueprintItem(Properties properties) { super(properties); }

    @Override public InteractionResult useOn(UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer player))
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
        ItemStack stack = context.getItemInHand();
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) return InteractionResult.FAIL;
        CompoundTag data = custom.copyTag();
        if (!data.hasUUID("owner") || !player.getUUID().equals(data.getUUID("owner"))
                || !data.hasUUID("axo") || !data.hasUUID("blueprintId")
                || !data.contains("manifest", 10)) {
            say(player, "That blueprint doesn't belong to you or has invalid data.");
            return InteractionResult.FAIL;
        }
        CompoundTag source = data.getCompound("manifest");
        if (!source.contains("blocks", 9) || source.getList("blocks", 10).isEmpty()) return InteractionResult.FAIL;

        BlockPos anchor = context.getClickedPos().relative(context.getClickedFace());
        if (player.isShiftKeyDown()) {
            data.putInt("turns", Math.floorMod(data.getInt("turns") + 1, 4));
            data.remove("previewAnchor");
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            sendPreview(player, data, anchor);
            say(player, "Rotated 90 degrees. Click the desired site to preview.");
            return InteractionResult.SUCCESS;
        }

        if (!data.contains("previewAnchor", 4) || data.getLong("previewAnchor") != anchor.asLong()) {
            data.putLong("previewAnchor", anchor.asLong());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            sendPreview(player, data, anchor);
            say(player, "Cyan preview placed. Click this same anchor again to lock; sneak-click to rotate.");
            return InteractionResult.SUCCESS;
        }

        // A second deliberate click locks the preview, only after server validation.
        WaifuAxoluthuEntity axo = null;
        for (WaifuAxoluthuEntity candidate : player.serverLevel().getEntitiesOfClass(
                WaifuAxoluthuEntity.class, player.getBoundingBox().inflate(128),
                a -> a.getUUID().equals(data.getUUID("axo"))
                        && player.getUUID().equals(a.getBondedPlayer()))) {
            axo = candidate;
            break;
        }
        if (axo == null) { say(player, "Axoluthu needs to be nearby to lock this design."); return InteractionResult.FAIL; }
        CompoundTag request = axo.getArchitecturalLearning().getCompound("phase35OwnerSuggestion");
        if (!request.getBoolean("pending") || !"BLUEPRINT_CLAIMED".equals(request.getString("phase"))
                || !request.hasUUID("blueprintId")
                || !request.getUUID("blueprintId").equals(data.getUUID("blueprintId"))) {
            say(player, "This proposal is no longer available for locking.");
            return InteractionResult.FAIL;
        }
        CompoundTag placed = transform(source, anchor, data.getInt("turns"));
        String problem = validate(player, placed);
        if (problem != null) {
            say(player, "Cannot lock here: " + problem + ". Blueprint was NOT consumed.");
            return InteractionResult.FAIL;
        }
        // Commit the immutable, positioned manifest before consuming the item.
        request.put("blueprintPreview", placed);
        request.putLong("target", anchor.asLong());
        request.putInt("quarterTurns", data.getInt("turns"));
        request.putString("dimension", player.level().dimension().location().toString());
        request.putString("phase", "COOP_BUILDING");
        request.putBoolean("designApproved", true);
        request.putBoolean("constructionAllowed", false); // legacy builder remains disabled
        request.putBoolean("siteLocked", true);
        request.putInt("buildCursor", 0);
        request.putInt("coopClearCursor", 0);
        request.putInt("coopPlaceCursor", 0);
        request.putString("coopStage", "CLEAR");
        stack.shrink(1);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.9f, 1f);

        // Leave the locked projection active, owned by the saved construction plan.
        PacketDistributor.sendToPlayer(player, new AxoluthuProjectionSyncPayload(
                data.getUUID("blueprintId"), player.level().dimension().location().toString(), placed));
        say(player, "Blueprint locked! Axoluthu will clear and build with you! :3");
        return InteractionResult.SUCCESS;
    }

    private static void say(ServerPlayer player, String message) {
        player.displayClientMessage(Component.literal("[Axoluthu] " + message), true);
    }

    /** Transform the original immutable design from origin zero to the player's anchor. */
    private static CompoundTag transform(CompoundTag source, BlockPos anchor, int turns) {
        CompoundTag result = source.copy();
        BlockPos origin = BlockPos.of(source.getLong("origin"));
        ListTag transformed = new ListTag();
        ListTag original = source.getList("blocks", 10);
        for (int i = 0; i < original.size(); i++) {
            CompoundTag block = original.getCompound(i).copy();
            block.putLong("pos", rotate(BlockPos.of(block.getLong("pos")), origin, anchor, turns).asLong());
            transformed.add(block);
        }
        result.put("blocks", transformed);
        result.putLong("origin", anchor.asLong());
        if (source.contains("entrance", 4))
            result.putLong("entrance", rotate(BlockPos.of(source.getLong("entrance")), origin, anchor, turns).asLong());
        result.putInt("quarterTurns", Math.floorMod(turns, 4));
        return result;
    }

    private static BlockPos rotate(BlockPos pos, BlockPos origin, BlockPos anchor, int turns) {
        int x = pos.getX() - origin.getX(), y = pos.getY() - origin.getY(), z = pos.getZ() - origin.getZ();
        return switch (Math.floorMod(turns, 4)) {
            case 1 -> anchor.offset(-z, y, x);
            case 2 -> anchor.offset(-x, y, -z);
            case 3 -> anchor.offset(z, y, -x);
            default -> anchor.offset(x, y, z);
        };
    }

    private static void sendPreview(ServerPlayer player, CompoundTag data, BlockPos anchor) {
        CompoundTag placed = transform(data.getCompound("manifest"), anchor, data.getInt("turns"));
        PacketDistributor.sendToPlayer(player, new AxoluthuProjectionSyncPayload(
                data.getUUID("blueprintId"), player.level().dimension().location().toString(), placed));
    }

    /** Conservative, one-time validation. No chunk loads and no block edits. */
    private static String validate(ServerPlayer player, CompoundTag preview) {
        ListTag blocks = preview.getList("blocks", 10);
        if (blocks.size() < 1 || blocks.size() > 4000) return "Invalid blueprint size";
        Set<Long> positions = new HashSet<>();
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag entry = blocks.getCompound(i);
            if (!entry.contains("pos", 4) || !entry.contains("block", 8)) return "Malformed block entry";
            BlockPos pos = BlockPos.of(entry.getLong("pos"));
            if (!positions.add(pos.asLong())) return "Duplicate block position";
            if (pos.getY() < player.level().getMinBuildHeight()
                    || pos.getY() >= player.level().getMaxBuildHeight()) return "Outside world height";
            if (!player.serverLevel().getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4))
                return "Unloaded chunk";
            BlockState desired = WaifuAxoluthuPalettePlacement.state(entry, preview.getInt("quarterTurns"));
            if (desired == null) return "Unsupported block";
            // The player explicitly authorizes replacement within the locked footprint.
            // Hardness and ordinary terrain collisions are not placement blockers.
            // The builder separately handles block-entity inventory safety.
        }
        // Ensure the whole clearing volume is loaded; no forced chunk loads.
        int minX=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,minY=Integer.MAX_VALUE,maxY=Integer.MIN_VALUE,minZ=Integer.MAX_VALUE,maxZ=Integer.MIN_VALUE;
        for (int i=0;i<blocks.size();i++) {
            BlockPos p=BlockPos.of(blocks.getCompound(i).getLong("pos"));
            minX=Math.min(minX,p.getX()); maxX=Math.max(maxX,p.getX());
            minY=Math.min(minY,p.getY()); maxY=Math.max(maxY,p.getY());
            minZ=Math.min(minZ,p.getZ()); maxZ=Math.max(maxZ,p.getZ());
        }
        long volume=(long)(maxX-minX+1)*(maxY-minY+1)*(maxZ-minZ+1);
        if (volume>12000) return "Building volume exceeds safety limit";
        for(int x=minX;x<=maxX;x+=16) for(int z=minZ;z<=maxZ;z+=16)
            if (!player.serverLevel().getChunkSource().hasChunk(x>>4,z>>4)) return "Unloaded construction area";
        if (!player.serverLevel().getChunkSource().hasChunk(maxX>>4,maxZ>>4)) return "Unloaded construction edge";
        return null;
    }
}
