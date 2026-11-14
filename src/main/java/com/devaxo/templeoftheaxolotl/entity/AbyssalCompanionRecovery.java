package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.core.registries.BuiltInRegistries;

/** Preserve the owner registry across respawn and recover loaded pets at logout/death. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class AbyssalCompanionRecovery {
    private AbyssalCompanionRecovery() {}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) recallLoaded(player);
    }
    /** Player death replaces the ServerPlayer instance. Copy the authoritative
     * roster into the new player's persistent NBT before the old instance is lost.
     * Never reconstruct companions here: saved UUIDs and states remain unchanged.
     */
    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer replacement)) return;
        CompoundTag original = event.getOriginal().getPersistentData();
        String key = AbyssalCompanionRegistry.KEY;
        if (!original.contains(key, net.minecraft.nbt.Tag.TAG_COMPOUND)) return;
        // A copied registry is safer than retaining the old player's mutable tag.
        replacement.getPersistentData().put(key, original.getCompound(key).copy());
    }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer owner)
            OfflineCompanionDeaths.get(owner.serverLevel()).reconcile(owner);
    }

    /** Pending death receipts are authoritative: a stale chunk must not resurrect
     * a dead companion while the owner is offline. */
    @SubscribeEvent public static void entityJoined(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Entity entity = event.getEntity();
        if (entity instanceof WaifuAxoluthuEntity axo && axo.getBondedPlayer() != null
                && OfflineCompanionDeaths.get(level).isPending(axo.getUUID()))
            axo.remove(Entity.RemovalReason.DISCARDED);
        else if (entity instanceof TamableAnimal pet && pet.isTame() && pet.getOwnerUUID() != null
                && OfflineCompanionDeaths.get(level).isPending(pet.getUUID()))
            pet.remove(Entity.RemovalReason.DISCARDED);
    }

    private static void recordOffline(Entity entity, java.util.UUID owner, String category, boolean axoluthu) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        CompoundTag death = new CompoundTag();
        death.putUUID("UUID", entity.getUUID());
        death.putUUID("Owner", owner);
        death.putString("Name", entity.getName().getString());
        death.putString("Category", category);
        death.putBoolean("CustomNamed", entity.hasCustomName() || axoluthu);
        death.putBoolean("AxoluthuBond", axoluthu);
        death.putString("EntityType", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        death.put("Entity", entity.saveWithoutId(new CompoundTag()));
        OfflineCompanionDeaths.get(level).record(death);
    }

    @SubscribeEvent public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            recallLoaded(player);
        } else if (event.getEntity() instanceof WaifuAxoluthuEntity axo && axo.getBondedPlayer() != null) {
            if (axo.getServer() != null) {
                ServerPlayer owner = axo.getServer().getPlayerList().getPlayer(axo.getBondedPlayer());
                if (owner != null) AbyssalCompanionRegistry.defeatedAxoluthu(owner, axo);
                else recordOffline(axo, axo.getBondedPlayer(), "Celestial", true);
            }
        } else if (event.getEntity() instanceof net.minecraft.world.entity.TamableAnimal pet
                && pet.isTame() && pet.getOwnerUUID() != null && !pet.level().isClientSide()) {
            if (pet.getServer() != null) {
                ServerPlayer owner = pet.getServer().getPlayerList().getPlayer(pet.getOwnerUUID());
                if (owner != null) AbyssalCompanionRegistry.defeatedPet(owner, pet);
                else if (pet.hasCustomName() || pet instanceof AbyssalAxolotlEntity)
                    recordOffline(pet, pet.getOwnerUUID(),
                            pet instanceof AbyssalAxolotlEntity ? "Abyssal" : "Normal", false);
            }
        }
    }
    private static void recallLoaded(ServerPlayer owner) {
        // Snapshot matches before removal: do not mutate the live entity collection
        // while iterating over it. Include Axoluthu as well as Abyssal Axolotls.
        java.util.List<Entity> companions = new java.util.ArrayList<>();
        for (var level : owner.server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof AbyssalAxolotlEntity axo && axo.isTame()
                        && owner.getUUID().equals(axo.getOwnerUUID())) {
                    companions.add(entity);
                } else if (entity instanceof WaifuAxoluthuEntity axo
                        && owner.getUUID().equals(axo.getBondedPlayer())) {
                    companions.add(entity);
                }
            }
        }
        for (Entity entity : companions) {
            if (entity.isRemoved()) continue;
            if (entity instanceof AbyssalAxolotlEntity axo)
                AbyssalCompanionRegistry.recallLoaded(owner, axo);
            else if (entity instanceof WaifuAxoluthuEntity axo)
                AbyssalCompanionRegistry.recallAxoluthuOnLogout(owner, axo);
        }
    }
}
