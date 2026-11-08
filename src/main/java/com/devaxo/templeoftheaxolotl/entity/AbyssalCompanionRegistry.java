package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import java.util.*;

/** Server-authoritative companion state, stored with the owner's player data. */
public final class AbyssalCompanionRegistry {
    public static final String KEY = "tota_abyssal_companion_registry_v1";
    private AbyssalCompanionRegistry() {}
    private static CompoundTag all(ServerPlayer player) { return player.getPersistentData().getCompound(KEY); }
    private static void commit(ServerPlayer player, CompoundTag all) { player.getPersistentData().put(KEY, all); }
    private static String key(UUID id) { return id.toString(); }
    public static void observe(ServerPlayer player, AbyssalAxolotlEntity axo) {
        if (!player.getUUID().equals(axo.getOwnerUUID()) || !axo.isTame()) return;
        CompoundTag all = all(player), tag = all.getCompound(key(axo.getUUID()));
        if (tag.getString("State").equals("stored") || tag.getString("State").equals("revive") || tag.getString("State").equals("pending")) return;
        tag.putString("Name", axo.getName().getString()); tag.putString("State", "active");
        tag.putBoolean("CustomNamed", axo.hasCustomName());
        tag.putString("Category", "Abyssal");
        tag.putUUID("UUID", axo.getUUID()); all.put(key(axo.getUUID()), tag); commit(player, all);
    }
    /** Finish a deferred recall once the original entity is loaded again.
     * Also recover a companion left behind when its owner dies. Never create
     * a second copy of an entity while its old chunk is unloaded.
     */
    public static void checkLoaded(ServerPlayer player, AbyssalAxolotlEntity axo) {
        if (!player.getUUID().equals(axo.getOwnerUUID()) || !axo.isTame()) return;
        CompoundTag all = all(player), tag = all.getCompound(key(axo.getUUID()));
        String state = tag.getString("State");
        if (state.equals("revive")) {
            // A defeated companion must not reappear from an old saved chunk.
            axo.remove(Entity.RemovalReason.DISCARDED);
            return;
        }
        if (state.equals("stored") && tag.contains("Entity")) {
            // Stale loaded entity: registry is authoritative.
            axo.remove(Entity.RemovalReason.DISCARDED);
            return;
        }
        if (state.equals("pending") || (!player.isAlive() && !state.equals("stored"))) {
            tag.putUUID("UUID", axo.getUUID());
            tag.putString("Name", axo.getName().getString());
            tag.put("Entity", axo.saveWithoutId(new CompoundTag()));
            tag.putString("State", "stored");
            all.put(key(axo.getUUID()), tag);
            commit(player, all);
            axo.remove(Entity.RemovalReason.DISCARDED);
            sync(player);
        }
    }
    /** Store the exact loaded companion before it can become stranded. */
    public static void recallLoaded(ServerPlayer player, AbyssalAxolotlEntity axo) {
        if (!axo.isTame() || !player.getUUID().equals(axo.getOwnerUUID())) return;
        CompoundTag all = all(player), tag = all.getCompound(key(axo.getUUID()));
        if ("stored".equals(tag.getString("State")) || "revive".equals(tag.getString("State"))) return;
        tag.putUUID("UUID", axo.getUUID());
        tag.putString("Name", axo.getName().getString());
        tag.put("Entity", axo.saveWithoutId(new CompoundTag()));
        tag.putString("State", "stored");
        all.put(key(axo.getUUID()), tag);
        commit(player, all);
        axo.remove(Entity.RemovalReason.DISCARDED);
    }
    public static void defeated(ServerPlayer player, AbyssalAxolotlEntity axo) {
        if (!player.getUUID().equals(axo.getOwnerUUID())) return;
        CompoundTag all = all(player), tag = all.getCompound(key(axo.getUUID()));
        tag.putUUID("UUID", axo.getUUID()); tag.putString("Name", axo.getName().getString());
        tag.put("Entity", axo.saveWithoutId(new CompoundTag())); tag.putString("State", "revive");
        all.put(key(axo.getUUID()), tag); commit(player, all);
        sync(player);
    }
    private static String companionCategory(net.minecraft.world.entity.TamableAnimal pet) {
        if (pet instanceof AbyssalAxolotlEntity) return "Abyssal";
        if (pet instanceof TameableWrathEntity || pet instanceof AxolotlAbsoluteEntity ||
                pet.getType().toString().toLowerCase(java.util.Locale.ROOT).contains("above_all")) return "Celestial";
        return "Normal";
    }

    private static void observeNamedPet(ServerPlayer owner, net.minecraft.world.entity.TamableAnimal pet) {
        if (!pet.isTame() || !pet.hasCustomName() || !owner.getUUID().equals(pet.getOwnerUUID())) return;
        if (pet instanceof AbyssalAxolotlEntity axo) { observe(owner, axo); return; }
        CompoundTag all = all(owner);
        String id = key(pet.getUUID());
        CompoundTag entry = all.getCompound(id);
        entry.putUUID("UUID", pet.getUUID());
        entry.putString("Name", pet.getCustomName().getString());
        entry.putString("Category", companionCategory(pet));
        entry.putBoolean("CustomNamed", true);
        entry.putString("Dimension", pet.level().dimension().location().toString());
        entry.putInt("X", pet.blockPosition().getX());
        entry.putInt("Y", pet.blockPosition().getY());
        entry.putInt("Z", pet.blockPosition().getZ());
        entry.putString("EntityType", net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(pet.getType()).toString());
        if ("pending".equals(entry.getString("State"))) {
            // Existing entity has finally loaded: safely teleport that same UUID.
            if (teleportNamedPet(owner, pet)) entry.putString("State", "active");
        } else entry.putString("State", "active");
        all.put(id, entry);
        commit(owner, all);
    }

    private static boolean teleportNamedPet(ServerPlayer owner, net.minecraft.world.entity.TamableAnimal pet) {
        if (!pet.isTame() || !pet.hasCustomName() || !owner.getUUID().equals(pet.getOwnerUUID())) return false;
        ServerLevel destination = owner.serverLevel();
        BlockPos base = owner.blockPosition();
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            BlockPos p = base.offset(dx, 0, dz);
            if (!destination.getWorldBorder().isWithinBounds(p)) continue;
            if (!destination.getBlockState(p).getCollisionShape(destination,p).isEmpty()) continue;
            if (!destination.getBlockState(p.above()).getCollisionShape(destination,p.above()).isEmpty()) continue;
            // Water-only companions must not be teleported into air.
            if (pet instanceof AbyssalAxolotlEntity && destination.getFluidState(p).isEmpty()) continue;
            if (pet.level() == destination) {
                pet.teleportTo(p.getX()+0.5, p.getY(), p.getZ()+0.5);
                return true;
            }
            // Cross-dimensional transfer through the vanilla entity mechanism.
            Entity transferred = pet.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(
                    destination, new net.minecraft.world.phys.Vec3(p.getX()+0.5,p.getY(),p.getZ()+0.5),
                    net.minecraft.world.phys.Vec3.ZERO, pet.getYRot(), pet.getXRot(),
                    net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING));
            return transferred != null;
        }
        return false;
    }

    public static void sync(ServerPlayer player) {
        // Discover owned loaded companions before sending the roster.
        for (ServerLevel level : player.server.getAllLevels())
            for (Entity entity : level.getAllEntities())
                if (entity instanceof net.minecraft.world.entity.TamableAnimal pet) observeNamedPet(player, pet);
        CompoundTag all = all(player);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                new com.devaxo.templeoftheaxolotl.network.AbyssalRosterPayload(publicRoster(all).toString()));
    }
    private static CompoundTag publicRoster(CompoundTag all) {
        CompoundTag view = new CompoundTag();
        for (String key : all.getAllKeys()) {
            CompoundTag original = all.getCompound(key);
            // Legacy stored noodles predate CustomNamed. Their saved custom-name
            // NBT (or a non-default historical display name) is authoritative.
            boolean named = original.getBoolean("CustomNamed");
            if (!named && ("stored".equals(original.getString("State")) ||
                    "revive".equals(original.getString("State")))) {
                CompoundTag saved = original.getCompound("Entity");
                named = saved.contains("CustomName", net.minecraft.nbt.Tag.TAG_STRING)
                        && !saved.getString("CustomName").isBlank();
                if (!named && original.getString("Category").isEmpty()) {
                    String legacyName = original.getString("Name");
                    named = !legacyName.isBlank() &&
                            !legacyName.equalsIgnoreCase("Abyssal Axolotl");
                }
            }
            if (!named) continue;
            CompoundTag entry = new CompoundTag();
            entry.putString("Name", original.getString("Name"));
            entry.putString("State", original.getString("State"));
            entry.putString("Category", original.getString("Category").isEmpty() ? "Abyssal" : original.getString("Category"));
            view.put(key, entry);
        }
        return view;
    }
    public static void action(ServerPlayer player, UUID id) {
        CompoundTag all = all(player);
        if (!all.contains(key(id), net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            player.sendSystemMessage(Component.literal("Companion entry missing on server. The book may be showing an outdated roster; reopen it after reconnecting."));
            sync(player);
            return;
        }
        CompoundTag tag = all.getCompound(key(id));
        if (!tag.hasUUID("UUID")) {
            // The registry key is the original UUID. Restore missing redundant metadata only;
            // never manufacture an entity or overwrite a conflicting UUID.
            tag.putUUID("UUID", id);
            all.put(key(id), tag);
            commit(player, all);
            player.sendSystemMessage(Component.literal("Repaired missing companion UUID metadata. Checking saved state..."));
        } else if (!id.equals(tag.getUUID("UUID"))) {
            player.sendSystemMessage(Component.literal("Companion registry UUID mismatch. Entry preserved; no entity spawned. Back up your world for recovery."));
            return;
        }
        // Universal pet storage uses the same owner-bound UUID as the Abyssal registry.
        // We never manufacture a replacement for an unloaded active pet.
        if (!tag.getString("Category").isEmpty() && !"Abyssal".equals(tag.getString("Category"))) {
            actionNamedPet(player, id, all, tag);
            sync(player);
            return;
        }
        String state = tag.getString("State");
        if (!state.equals("active") && !state.equals("pending") && !state.equals("stored") && !state.equals("revive")) {
            player.sendSystemMessage(Component.literal("Companion has unknown registry state: '" + state + "'. Entry preserved for recovery."));
            return;
        }
        if (state.equals("pending")) {
            // A queued recall can only finish once the original entity is loaded.
            // Never spawn a replacement with the same UUID while the original chunk is unloaded.
            for (ServerLevel level : player.server.getAllLevels()) {
                Entity found = level.getEntity(id);
                if (found instanceof AbyssalAxolotlEntity axo && axo.isTame()
                        && player.getUUID().equals(axo.getOwnerUUID())) {
                    recallLoaded(player, axo);
                    sync(player);
                    player.sendSystemMessage(Component.literal(axo.getName().getString() + " safely stored. Press Summon again."));
                    return;
                }
            }
            player.sendSystemMessage(Component.literal("Companion recall is queued. Its original chunk must load before summoning; no duplicate will be created."));
            return;
        }
        if (state.equals("active")) {
            for (ServerLevel level : player.server.getAllLevels()) {
                Entity e = level.getEntity(id);
                if (e instanceof AbyssalAxolotlEntity axo && axo.isTame() && player.getUUID().equals(axo.getOwnerUUID())) {
                    tag.put("Entity", axo.saveWithoutId(new CompoundTag()));
                    tag.putString("State", "stored"); all.put(key(id), tag); commit(player, all);
                    axo.remove(Entity.RemovalReason.DISCARDED);
                    player.sendSystemMessage(Component.literal(axo.getName().getString() + " recalled."));
                    sync(player); return;
                }
            }
            // Mark pending rather than treating an unloaded entity as absent.
            // The original will be saved and removed on its next server tick.
            tag.putString("State", "pending"); all.put(key(id), tag); commit(player, all);
            player.sendSystemMessage(Component.literal("Recall queued. Companion will be stored when its chunk loads."));
            sync(player);
        } else if (state.equals("stored") || state.equals("revive")) {
            if (state.equals("revive") && !validShrine(player)) {
                player.sendSystemMessage(Component.literal("Need Revive: use the Book of Axoluthu at an Abyssal Moai lectern.")); return;
            }
            if (!tag.contains("Entity", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                player.sendSystemMessage(Component.literal("Cannot summon: this companion's saved entity data is missing. Keep the world backup and registry entry for recovery."));
                return;
            }
            ServerLevel level = player.serverLevel();
            // Check for a duplicate across every dimension before restoring the saved UUID.
            for (ServerLevel other : player.server.getAllLevels()) if (other.getEntity(id) != null) {
                player.sendSystemMessage(Component.literal("Cannot summon: an entity with this UUID is already loaded. No duplicate was created."));
                return;
            }
            BlockPos spot = player.blockPosition().offset(1,0,1);
            if (!level.getBlockState(spot).getCollisionShape(level,spot).isEmpty()) spot = player.blockPosition().above();
            if (!level.getBlockState(spot).getCollisionShape(level,spot).isEmpty()) {
                player.sendSystemMessage(Component.literal("Cannot summon: no free space beside or above you."));
                return;
            }
            AbyssalAxolotlEntity axo = com.devaxo.templeoftheaxolotl.entity.ModEntities.ABYSSAL_AXOLOTL.get().create(level);
            if (axo == null) {
                player.sendSystemMessage(Component.literal("Cannot summon: Abyssal Axolotl entity type could not be created."));
                return;
            }
            CompoundTag saved = tag.getCompound("Entity").copy();
            // Keep original snapshot intact if deserialization fails. A saved
            // entity without its type ID may still be loaded into the known type.
            try {
                axo.load(saved);
            } catch (RuntimeException ex) {
                player.sendSystemMessage(Component.literal("[Noodle Rescue] Saved companion data could not be loaded: "
                        + ex.getClass().getSimpleName() + ". See latest.log; original data preserved."));
                com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl.LOGGER.error(
                        "Saved companion deserialization failed for {} (state={})", id, state, ex);
                return;
            }
            axo.setUUID(id); axo.moveTo(spot.getX()+0.5, spot.getY(), spot.getZ()+0.5, player.getYRot(), 0);
            if (state.equals("revive")) { axo.setHealth(axo.getMaxHealth()); axo.setGuarding(false); }
            if (!player.getUUID().equals(axo.getOwnerUUID()) || !axo.isTame()) {
                player.sendSystemMessage(Component.literal("Cannot summon: saved companion owner/taming data does not match. Entry preserved for recovery."));
                return;
            }
            if (!level.addFreshEntity(axo)) {
                player.sendSystemMessage(Component.literal("Cannot summon: world rejected the entity. Entry preserved for recovery."));
                return;
            }
            tag.putString("State", "active"); all.put(key(id), tag); commit(player, all);
            player.sendSystemMessage(Component.literal(axo.getName().getString() + " summoned."));
        }
        sync(player);
    }
    /** Find the original pet, loading only its last known chunk on explicit owner request. */
    private static net.minecraft.world.entity.TamableAnimal findOriginal(ServerPlayer player, UUID id, CompoundTag entry) {
        for (ServerLevel level : player.server.getAllLevels()) {
            Entity loaded = level.getEntity(id);
            if (loaded instanceof net.minecraft.world.entity.TamableAnimal pet) return pet;
            if (loaded != null) return null;
        }
        if (!entry.contains("Dimension", net.minecraft.nbt.Tag.TAG_STRING) ||
                !entry.contains("X", net.minecraft.nbt.Tag.TAG_INT) ||
                !entry.contains("Y", net.minecraft.nbt.Tag.TAG_INT) ||
                !entry.contains("Z", net.minecraft.nbt.Tag.TAG_INT)) return null;
        try {
            var dimension = net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    net.minecraft.resources.ResourceLocation.parse(entry.getString("Dimension")));
            ServerLevel level = player.server.getLevel(dimension);
            if (level == null) return null;
            BlockPos last = new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z"));
            if (!level.getWorldBorder().isWithinBounds(last)) return null;
            // Explicit user action: load only the last registered chunk, never scan the world.
            level.getChunkAt(last);
            Entity found = level.getEntity(id);
            return found instanceof net.minecraft.world.entity.TamableAnimal pet ? pet : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static void actionNamedPet(ServerPlayer player, UUID id, CompoundTag all, CompoundTag tag) {
        String state = tag.getString("State");
        if (state.equals("stored")) {
            if (!tag.contains("Entity", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                player.sendSystemMessage(Component.literal("Cannot summon: saved companion snapshot missing. Registry entry preserved."));
                return;
            }
            for (ServerLevel world : player.server.getAllLevels()) if (world.getEntity(id) != null) {
                player.sendSystemMessage(Component.literal("Cannot summon: original UUID already exists. No duplicate created."));
                return;
            }
            CompoundTag saved = tag.getCompound("Entity").copy();
            String typeName = tag.getString("EntityType");
            if (typeName.isBlank()) typeName = saved.getString("id");
            var typeKey = net.minecraft.resources.ResourceLocation.tryParse(typeName);
            if (typeKey == null || !net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(typeKey)) {
                player.sendSystemMessage(Component.literal("Cannot summon: unknown saved entity type. Snapshot preserved."));
                return;
            }
            var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(typeKey);
            if (type == null) return;
            ServerLevel level = player.serverLevel();
            net.minecraft.world.entity.TamableAnimal restored = null;
            Entity candidate = type.create(level);
            if (candidate instanceof net.minecraft.world.entity.TamableAnimal pet) restored = pet;
            if (restored == null) {
                player.sendSystemMessage(Component.literal("Cannot summon: saved entity is not a tameable pet."));
                return;
            }
            try { restored.load(saved); }
            catch (RuntimeException ex) {
                com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl.LOGGER.error("Companion restoration failed for {}", id, ex);
                player.sendSystemMessage(Component.literal("Companion restoration failed; snapshot preserved. See latest.log."));
                return;
            }
            restored.setUUID(id);
            if (!restored.isTame() || !player.getUUID().equals(restored.getOwnerUUID())) {
                player.sendSystemMessage(Component.literal("Saved companion ownership mismatch; original snapshot preserved."));
                return;
            }
            BlockPos destination = safePetSpot(player, restored);
            if (destination == null) {
                player.sendSystemMessage(Component.literal("No safe space to summon companion nearby."));
                return;
            }
            restored.moveTo(destination.getX()+0.5, destination.getY(), destination.getZ()+0.5, player.getYRot(), 0);
            if (!level.addFreshEntity(restored)) {
                player.sendSystemMessage(Component.literal("World rejected companion; saved snapshot preserved."));
                return;
            }
            tag.putString("State", "active");
            tag.remove("Entity");
            tag.putString("Dimension", level.dimension().location().toString());
            tag.putInt("X", destination.getX()); tag.putInt("Y", destination.getY()); tag.putInt("Z", destination.getZ());
            all.put(key(id), tag); commit(player, all);
            player.sendSystemMessage(Component.literal(restored.getName().getString() + " summoned."));
            return;
        }
        net.minecraft.world.entity.TamableAnimal pet = findOriginal(player, id, tag);
        if (pet == null) {
            tag.putString("State", "pending"); all.put(key(id), tag); commit(player, all);
            player.sendSystemMessage(Component.literal("Original companion not found in its last registered chunk. Recall queued; no duplicate created."));
            return;
        }
        if (!pet.isTame() || !player.getUUID().equals(pet.getOwnerUUID()) || !pet.hasCustomName()) {
            player.sendSystemMessage(Component.literal("Companion ownership/name validation failed. Entry preserved."));
            return;
        }
        // Preserve the exact original entity, including UUID and entity-specific NBT.
        CompoundTag snapshot = pet.saveWithoutId(new CompoundTag());
        tag.put("Entity", snapshot);
        tag.putString("EntityType", net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(pet.getType()).toString());
        tag.putString("Name", pet.getCustomName().getString());
        tag.putString("State", "stored");
        all.put(key(id), tag); commit(player, all);
        pet.remove(Entity.RemovalReason.DISCARDED);
        player.sendSystemMessage(Component.literal(tag.getString("Name") + " safely stored. Press Summon to bring them back."));
    }

    private static BlockPos safePetSpot(ServerPlayer owner, net.minecraft.world.entity.TamableAnimal pet) {
        ServerLevel level = owner.serverLevel();
        BlockPos origin = owner.blockPosition();
        for (int dy = 0; dy <= 2; dy++) for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            BlockPos pos = origin.offset(dx, dy, dz);
            if (!level.getWorldBorder().isWithinBounds(pos)) continue;
            if (!level.getBlockState(pos).getCollisionShape(level,pos).isEmpty()) continue;
            if (!level.getBlockState(pos.above()).getCollisionShape(level,pos.above()).isEmpty()) continue;
            if (pet instanceof AbyssalAxolotlEntity && level.getFluidState(pos).isEmpty()) continue;
            return pos;
        }
        return null;
    }

    private static boolean validShrine(ServerPlayer player) {
        // Require a nearby lectern and Moai (Abyssal Torch), facing the opposite direction.
        BlockPos origin = player.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-4,-2,-4), origin.offset(4,2,4))) {
            var state = player.level().getBlockState(p);
            if (!state.is(Blocks.LECTERN)) continue;
            var facing = state.getValue(net.minecraft.world.level.block.LecternBlock.FACING);
            if (!player.level().getBlockState(p.relative(facing.getOpposite())).is(com.devaxo.templeoftheaxolotl.registry.ModBlocks.ABYSSAL_TORCH.get())) continue;
            if (player.level().getBlockEntity(p) instanceof net.minecraft.world.level.block.entity.LecternBlockEntity lectern
                    && lectern.getBook().is(com.devaxo.templeoftheaxolotl.item.ModItems.NOODLE_COMMAND_BOOK.get())) return true;
        }
        return false;
    }
}
