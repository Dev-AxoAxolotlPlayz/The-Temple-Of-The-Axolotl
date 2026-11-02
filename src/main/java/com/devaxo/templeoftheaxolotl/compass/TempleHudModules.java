
package com.devaxo.templeoftheaxolotl.compass;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import com.devaxo.templeoftheaxolotl.repository.CelestialRepository;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Persistent server-owned HUD module storage.
 *
 * Slot 0 accepts the Temple Seeker.
 * Slots 1-8 are reserved for future HUD modules.
 */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class TempleHudModules {

    private static final String KEY = "tota_navigation_module";
    private static final String COMPASS_KEY = "tota_position_module";
    private static final String CLOCK_KEY = "tota_clock_module";

    private TempleHudModules() {}

    public static ItemStack get(ServerPlayer player) {
        return getSlot(player, 0);
    }

    private static String key(int slot) {
        return slot == 0 ? KEY : slot == 1 ? COMPASS_KEY : slot == 2 ? CLOCK_KEY : "tota_hud_module_" + slot;
    }

    public static ItemStack getSlot(ServerPlayer player, int slot) {
        CompoundTag data = player.getPersistentData();

        if (!data.contains(key(slot))) {
            return ItemStack.EMPTY;
        }

        return ItemStack.parseOptional(
                player.registryAccess(),
                data.getCompound(key(slot))
        );
    }

    private static void save(ServerPlayer player, int slot, ItemStack stack) {
        String storageKey = key(slot);
        if (stack.isEmpty()) {
            player.getPersistentData().remove(storageKey);
        } else {
            player.getPersistentData().put(
                    storageKey,
                    stack.save(player.registryAccess())
            );
        }
    }

    public static void open(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (id, playerInventory, who) -> new CelestialModulesMenu(id, playerInventory),
                Component.literal("Celestial Modules")
        ));
    }

    /** The first three slots are live; later slots are reserved until their modules exist. */
    public static boolean accepts(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> stack.is(ModItems.TEMPLE_SEEKER.get());
            case 1 -> stack.is(Items.COMPASS);
            case 2 -> stack.is(Items.CLOCK);
            case 3 -> stack.is(ModItems.CELESTIAL_REPOSITORY.get());
            case 4 -> stack.is(ModItems.CELESTIAL_MAGNET.get());
            case 5 -> stack.is(Items.CRAFTING_TABLE);
            case 6 -> stack.is(ModItems.CELESTIAL_ANVIL.get());
            case 7 -> stack.is(ModItems.LOGISTICS_MAESTRO.get());
            case 8 -> stack.is(ModItems.CELESTIAL_JUKEBOX.get());
            case 9 -> stack.is(ModItems.ABYSS_PRESSURE_GAUGE.get());
            case 10 -> stack.is(ModItems.ABYSSAL_GILLS.get());
            default -> false;
        };
    }

    public static void storeSlot(ServerPlayer player, int slot, ItemStack stack) {
        if (slot >= 0 && slot < 18) save(player, slot, stack);
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {

        CommandDispatcher<CommandSourceStack> dispatcher =
                event.getDispatcher();

        dispatcher.register(
                Commands.literal("tota")
                        .then(
                                Commands.literal("modules")
                                        .executes(context -> {
                                            ServerPlayer player =
                                                    context.getSource()
                                                            .getPlayerOrException();

                                            open(player);
                                            return 1;
                                        })
                        )
        );
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        for (int slot = 0; slot < 18; slot++) {
            String storageKey = key(slot);
            if (event.getOriginal().getPersistentData().contains(storageKey)) {
                event.getEntity().getPersistentData().put(storageKey,
                        event.getOriginal().getPersistentData().get(storageKey).copy());
            }
        }
        // Respawn creates a new Player instance. Copy settings as well as installed modules.
        // Preserve the exact NBT values (including explicit OFF states and magnet mode).
        for (String settingKey : new String[]{
                "temple_of_the_axolotl.ring_mining_mode",
                "tota_helmet_night_vision_disabled",
                "tota_helmet_conduit_disabled",
                "tota_abyssal_gills_disabled",
                "tota_cluckles_in",
                "tota_cluckles_out",
                "tota_cluckles_radius",
                "tota_cluckles_magnet"
        }) {
            if (event.getOriginal().getPersistentData().contains(settingKey)) {
                event.getEntity().getPersistentData().put(settingKey,
                        event.getOriginal().getPersistentData().get(settingKey).copy());
            }
        }
        for (String trackingKey : new String[]{"tota_seeker_target", "tota_seeker_state"}) {
            if (event.getOriginal().getPersistentData().contains(trackingKey))
                event.getEntity().getPersistentData().put(trackingKey,
                        event.getOriginal().getPersistentData().get(trackingKey).copy());
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.tickCount % 5 != 0) {
            return;
        }

        // Celestial Anvil: a 10-block XP-orb magnet (does not mint XP).
        if (getSlot(player, 6).is(ModItems.CELESTIAL_ANVIL.get())) {
            for (ExperienceOrb orb : player.serverLevel().getEntitiesOfClass(
                    ExperienceOrb.class, new AABB(player.blockPosition()).inflate(10.0))) {
                if (orb.isAlive()) orb.setPos(player.getX(), player.getY() + 0.5, player.getZ());
            }
        }

        // Celestial Magnet: store ordinary item entities; keep overflow in the world.
        // Dungeon-specific reward routing will be added after lootbag API integration.
        if (getSlot(player, 4).is(ModItems.CELESTIAL_MAGNET.get()) &&
                com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.magnetMode(player) != 2) {
            for (ItemEntity entity : player.serverLevel().getEntitiesOfClass(
                    ItemEntity.class, new AABB(player.blockPosition()).inflate(10.0))) {
                if (!entity.isAlive() || entity.hasPickUpDelay()) continue;
                ItemStack stack = entity.getItem();
                int accepted = 0;
                int mode = com.devaxo.templeoftheaxolotl.entity.ClucklesLogistics.magnetMode(player);
                if (mode == 0 && getSlot(player, 3).is(ModItems.CELESTIAL_REPOSITORY.get())) {
                    accepted += CelestialRepository.insert(player, stack.copyWithCount(stack.getCount() - accepted));
                    if (accepted < stack.getCount())
                        accepted += CelestialRepository.insertSecond(player, stack.copyWithCount(stack.getCount() - accepted));
                }
                if (accepted < stack.getCount()) {
                    ItemStack remainder = stack.copyWithCount(stack.getCount() - accepted);
                    player.getInventory().add(remainder);
                    accepted += stack.getCount() - accepted - remainder.getCount();
                }
                if (accepted == 0) {
                    // Preserve the original item entity; no repeated respawning or item duplication.
                    entity.setDeltaMovement(player.position().subtract(entity.position()).scale(0.08).add(0,0.03,0));
                }
                if (accepted <= 0) continue;
                stack.shrink(accepted);
                if (stack.isEmpty()) entity.discard();
                else {
                    entity.setItem(stack);
                    // Unstored remainder stays as ONE real item entity following the owner.
                    entity.setDeltaMovement(player.position().subtract(entity.position()).scale(0.08).add(0,0.03,0));
                }
            }
        }

        ItemStack module = get(player);

        boolean active =
                module.is(ModItems.TEMPLE_SEEKER.get());

        // Refresh the compass target while equipped.
        if (active) {
            TempleSeekerEvents.updateModuleStack(
                    player,
                    module
            );

            save(player, 0, module);
        }

        LodestoneTracker tracker =
                module.get(DataComponents.LODESTONE_TRACKER);

        GlobalPos pos =
                tracker == null
                        ? null
                        : tracker.target().orElse(null);

        boolean valid =
                active
                        && pos != null
                        && pos.dimension().equals(
                        player.level().dimension()
                )
                        && player.level()
                        .dimension()
                        .equals(Level.OVERWORLD);

        PacketDistributor.sendToPlayer(
                player,
                new TempleNavigationPayload(
                        active,
                        valid,
                        valid ? pos.pos().getX() : 0,
                        valid ? pos.pos().getZ() : 0,
                        TempleSeekerEvents.arrived(player),
                        TempleSeekerEvents.unavailable(player),
                        getSlot(player, 1).is(Items.COMPASS),
                        getSlot(player, 2).is(Items.CLOCK),
                        getSlot(player, 9).is(ModItems.ABYSS_PRESSURE_GAUGE.get())
                )
        );
    }
}
