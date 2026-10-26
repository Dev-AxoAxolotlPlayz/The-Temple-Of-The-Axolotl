package com.devaxo.templeoftheaxolotl.maze;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.traps.TempleProgress;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.core.Vec3i;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import net.minecraft.world.phys.Vec3;

import java.util.*;

/** NeoForge 1.21.1: development-only schematic maze test command. */
public final class AxoMazeCommands {
    private AxoMazeCommands() {}
    private static final int CELL_XZ = 15, CELL_Y = 16;
    /** Fixed generation anchor; NOT the player's spawn point. */
    public static final BlockPos TEMPLE_ORIGIN = new BlockPos(0, 200, 0);
    /** Unrotated entrance template occupies grid cell (1, 0, 1). */
    public static final BlockPos ENTRANCE_MIN = TEMPLE_ORIGIN.offset(15, 0, 15);

    /** True only after all scheduled modules were placed successfully. */
    public static boolean isBuildComplete(ServerLevel level) {
        return fullReset == null && activeJob == null && activeUndo == null && lastGeneration != null
                && lastGeneration.level() == level
                && lastGeneration.snapshots().size() > 0
                && buildCompleted;
    }

    /** An active build or an undo snapshot means this dimension is not yet free. */
    public static boolean isMazeOccupiedByBuild(ServerLevel level) {
        return isFullResetting(level) || (activeUndo != null && activeUndo.level == level)
                || (activeJob != null && activeJob.level == level)
                || (lastGeneration != null && lastGeneration.level() == level);
    }

    // One-time legacy-dungeon cleanup. Not persistent: finish before restarting.
    private static FullResetJob fullReset;
    private static final int RESET_XZ_CELLS = 9;
    private static final int RESET_VERTICAL_PASSES = 14;
    private static final int RESET_TOTAL_CELLS = RESET_XZ_CELLS * RESET_XZ_CELLS * RESET_VERTICAL_PASSES;
    private static final class FullResetJob {
        final ServerLevel level;
        int nextCell;
        FullResetJob(ServerLevel level) { this.level = level; }
    }
    private static boolean isFullResetting(ServerLevel level) {
        return fullReset != null && fullReset.level == level;
    }
    private static boolean hasPlayersInside(ServerLevel level) {
        return !level.players().isEmpty();
    }
    /** Automatic cleanup uses the SAME deep-clean worker as /templefullreset. */
    public static boolean beginAutomaticFullReset(ServerLevel level) {
        if (level == null || hasPlayersInside(level)) return false;
        // Static workers can survive integrated-server shutdown in the JVM.
        // Their ServerLevel belongs to a dead server and must not block recovery.
        discardStaleWorkers(level.getServer());
        if (fullReset != null || activeJob != null || activeUndo != null) return false;
        fullReset = new FullResetJob(level);
        return true;
    }

    /** Clear only worker references owned by an earlier server instance. */
    private static void discardStaleWorkers(net.minecraft.server.MinecraftServer current) {
        if (fullReset != null && fullReset.level.getServer() != current) fullReset = null;
        if (activeJob != null && activeJob.level.getServer() != current) activeJob = null;
        if (activeUndo != null && activeUndo.level.getServer() != current) activeUndo = null;
        if (lastGeneration != null && lastGeneration.level().getServer() != current) {
            lastGeneration = null;
            activeTempleId = null;
            buildCompleted = false;
        }
    }

    private static int startFullReset(CommandSourceStack source) {
        ServerLevel level = source.getServer().getLevel(TempleDimensionManager.TEMPLE_DIMENSION);
        if (level == null) { source.sendFailure(Component.literal("Temple dimension unavailable.")); return 0; }
        if (fullReset != null || activeJob != null || activeUndo != null) {
            source.sendFailure(Component.literal("Temple is already building or resetting. Wait for completion.")); return 0;
        }
        if (hasPlayersInside(level)) {
            source.sendFailure(Component.literal("Full reset refused: a player is inside the temple dimension.")); return 0;
        }
        fullReset = new FullResetJob(level);
        source.sendSuccess(() -> Component.literal("Temple deep clean started: 1134 cells, Y=215 down to 0. Do not restart until complete."), true);
        return 1;
    }
    private static void tickFullReset(FullResetJob job) {
        // One 15x16x15 region per tick; snake back and forth along successive Z rows.
        int index = job.nextCell;
        int pass = index / 81;
        int inLayer = index % 81;
        int row = inLayer / 9;
        int column = inLayer % 9;
        int xCell = (row % 2 == 0) ? column : 8 - column;
        int minX = xCell * 15, minZ = row * 15;
        int maxY = 215 - pass * 16;
        int minY = Math.max(0, maxY - 15);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = maxY; y >= minY; y--) {
            for (int z = minZ; z < minZ + 15; z++) {
                for (int x = minX; x < minX + 15; x++) {
                    pos.set(x, y, z);
                    if (!job.level.getBlockState(pos).isAir())
                        job.level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        job.nextCell++;
        if (job.nextCell == RESET_TOTAL_CELLS) {
            // Snapshots from an earlier maze are invalid after a destructive reset.
            retireRewards(job.level);
            lastGeneration = null;
            buildCompleted = false;
            fullReset = null;
            TempleOfTheAxolotl.LOGGER.info("Temple full reset complete (0..134 X/Z, 0..215 Y).");
            job.level.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("Temple deep clean complete. The dungeon is ready for a fresh generation."), false);
        }
    }

    private static boolean buildCompleted = false;
    private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final int GRID = 9;
    // Fixed northwest complex: 4x4 cells, starting at X=0, Z=2.
    // Its eastmost north-wall doorway faces the fixed elevator at (3,-10,1).
    // 48 blocks high: Y cells -12, -11, -10 (loot entrance at -10).
    private static final Cell SANCTUM_MIN = new Cell(0, -12, 2);
    private static final Cell FINAL_ELEVATOR_UPPER = new Cell(3, -9, 1);
    private static final Cell FINAL_ELEVATOR_LOWER = new Cell(3, -10, 1);
    private static final Vec3i SANCTUM_SIZE = new Vec3i(60, 48, 60);
    private static final int[] TOP_Y = {0, -1, -3, -6};
    private static final int[] FLOORS = {1, 2, 3, 4};

    // Development undo: one snapshot per running server. Not persistent across restarts.
    private static UndoRecord lastGeneration;
    // One identity per generated maze. Shared by every visitor of that maze.
    private static UUID activeTempleId;
    public static UUID activeTempleId(ServerLevel level) {
        return lastGeneration != null && lastGeneration.level() == level && buildCompleted
                ? activeTempleId : null;
    }
    public static long activeTempleSeed(ServerLevel level) {
        return lastGeneration != null && lastGeneration.level() == level && buildCompleted
                ? lastGeneration.seed() : 0L;
    }
    private static void retireRewards(ServerLevel level) {
        if (activeTempleId != null) {
            com.devaxo.templeoftheaxolotl.loot.DungeonRewardInventory.invalidateTemple(
                    level.getServer(), activeTempleId);
            activeTempleId = null;
        }
    }
    private static BuildJob activeJob;
    private static UndoJob activeUndo;
    private static final int UNDO_MODULES_PER_TICK = 3;

    private static final class UndoJob {
        final ServerLevel level;
        final UndoRecord record;
        int next;
        UndoJob(ServerLevel level, UndoRecord record) {
            this.level = level;
            this.record = record;
            this.next = record.snapshots().size() - 1;
        }
    }

    public static boolean isDeconstructing(ServerLevel level) {
        return isFullResetting(level) || (activeUndo != null && activeUndo.level == level);
    }

    /** Queues reverse-order snapshot restoration; returns false if unavailable or already running. */
    public static boolean requestDeconstruction(ServerLevel level) {
        if (fullReset != null || activeJob != null || activeUndo != null || lastGeneration == null
                || lastGeneration.level() != level || !buildCompleted) return false;
        retireRewards(level);
        activeUndo = new UndoJob(level, lastGeneration);
        buildCompleted = false;
        return true;
    }

    private static boolean tickListenerRegistered;
    private static final int MODULES_PER_TICK = 3;
    private record Placement(BlockPos pos, Vec3i size, StructureTemplate template,
                             Rotation rotation, boolean entities, int treasureLayer, int trapLayer) {
        Placement(BlockPos pos, Vec3i size, StructureTemplate template,
                  Rotation rotation, boolean entities, int treasureLayer) {
            this(pos, size, template, rotation, entities, treasureLayer, treasureLayer);
        }
        Placement(BlockPos pos, Vec3i size, StructureTemplate template,
                  Rotation rotation, boolean entities) {
            this(pos, size, template, rotation, entities, 0, 0);
        }
    }
    private static final class BuildJob {
        final ServerLevel level;
        final CommandSourceStack source;
        final long seed;
        final List<Placement> placements;
        final List<Placement> lootPlacements;
        int next;
        int nextLoot;
        final Set<BlockPos> initializedLootContainers = new HashSet<>();
        BuildJob(ServerLevel level, CommandSourceStack source, long seed, List<Placement> placements) {
            this.level = level; this.source = source; this.seed = seed; this.placements = placements;
            // Separate construction from inventory tagging. Scan the finished
            // dungeon from its highest treasure floor to its deepest.
            this.lootPlacements = placements.stream()
                    .filter(p -> p.treasureLayer() > 0)
                    .sorted(java.util.Comparator.comparingInt(
                            (Placement p) -> p.pos().getY()).reversed())
                    .toList();
        }
    }
    private static void onServerTick(ServerTickEvent.Post event) {
        FullResetJob reset = fullReset;
        if (reset != null && reset.level.getServer() == event.getServer()) {
            if (hasPlayersInside(reset.level)) return; // Never erase blocks beneath players.
            try { tickFullReset(reset); }
            catch (Exception ex) {
                fullReset = null;
                TempleOfTheAxolotl.LOGGER.error("Temple full reset stopped at cell " + reset.nextCell, ex);
            }
            return;
        }
        UndoJob undoJob = activeUndo;
        if (undoJob != null && undoJob.level.getServer() == event.getServer()) {
            try {
                for (int i = 0; i < UNDO_MODULES_PER_TICK && undoJob.next >= 0; i++) {
                    Snapshot snapshot = undoJob.record.snapshots().get(undoJob.next);
                    StructurePlaceSettings settings = new StructurePlaceSettings()
                            .setMirror(Mirror.NONE).setRotation(Rotation.NONE).setIgnoreEntities(true);
                    BlockPos pos = snapshot.origin();
                    if (!snapshot.original().placeInWorld(undoJob.level, pos, pos,
                            settings, undoJob.level.random, 2)) {
                        throw new IllegalStateException("Could not restore snapshot at " + pos);
                    }
                    undoJob.next--;
                }
                if (undoJob.next < 0) {
                    for (UUID id : undoJob.record.generatedEntities()) {
                        Entity entity = undoJob.level.getEntity(id);
                        if (entity != null && !(entity instanceof Player)) entity.discard();
                    }
                    lastGeneration = null;
                    activeUndo = null;
                    TempleOfTheAxolotl.LOGGER.info("Temple dungeon deconstruction complete; dimension restored.");
                }
            } catch (Exception ex) {
                // Keep the snapshot and the current index for investigation/retry.
                activeUndo = null;
                TempleOfTheAxolotl.LOGGER.error("Temple deconstruction failed; snapshot retained", ex);
            }
            return;
        }
        BuildJob job = activeJob;
        if (job == null || job.level.getServer() != event.getServer()) return;
        try {
            for (int i = 0; i < MODULES_PER_TICK && job.next < job.placements.size(); i++) {
                Placement task = job.placements.get(job.next);
                StructureTemplate original = new StructureTemplate();
                original.fillFromWorld(job.level, task.pos(), task.size(), true, null);
                lastGeneration.snapshots().add(new Snapshot(task.pos(), original));
                AABB bounds = new AABB(Vec3.atLowerCornerOf(task.pos()),
                        Vec3.atLowerCornerOf(task.pos().offset(task.size())));
                Set<UUID> before = new HashSet<>();
                if (task.entities()) for (Entity entity : job.level.getEntitiesOfClass(Entity.class,
                        bounds, e -> !(e instanceof Player))) before.add(entity.getUUID());
                try {
                    placeTemplate(job.level, task);
                } finally {
                    if (task.entities()) for (Entity entity : job.level.getEntitiesOfClass(Entity.class,
                            bounds, e -> !(e instanceof Player))) {
                        if (!before.contains(entity.getUUID()))
                            lastGeneration.generatedEntities().add(entity.getUUID());
                    }
                }
                job.next++;
            }
            // The build phase must be COMPLETELY finished before any inventory
            // receives a pending loot table. No structure is placed during this pass.
            if (job.next == job.placements.size()) {
                for (int i = 0; i < MODULES_PER_TICK
                        && job.nextLoot < job.lootPlacements.size(); i++) {
                    Placement lootTask = job.lootPlacements.get(job.nextLoot++);
                    assignTreasureLoot(job.level, lootTask, job.initializedLootContainers);
                }
            }
            if (job.next == job.placements.size()
                    && job.nextLoot == job.lootPlacements.size()) {
                activeJob = null;
                buildCompleted = true;
                job.source.sendSuccess(() -> Component.literal("The Axolotl Absolute Watches As You Descend"), true);
                TempleOfTheAxolotl.LOGGER.info("Temple generation complete: {} modules, seed {}", job.placements.size(), job.seed);
            }
        } catch (Exception ex) {
            activeJob = null;
            buildCompleted = false;
            TempleOfTheAxolotl.LOGGER.error("Gradual maze construction failed", ex);
            job.source.sendFailure(Component.literal("Maze construction stopped at module "
                    + (job.next + 1) + ": " + ex.getMessage()
                    + ". Use /templefullreset for an emergency deep clean (destructive)."));
        }
    }
    /** Validate scheduled world-space cuboids before any block is modified. */
    private static void validatePlacementFootprints(List<Placement> tasks) {
        for (int i = 0; i < tasks.size(); i++) {
            Placement a = tasks.get(i);
            for (int j = i + 1; j < tasks.size(); j++) {
                Placement b = tasks.get(j);
                if (!footprintsOverlap(a, b)) continue;
                String details = "Temple module footprint overlap: task " + i + " at " + a.pos()
                        + " size " + a.size() + " and task " + j + " at " + b.pos()
                        + " size " + b.size();
                TempleOfTheAxolotl.LOGGER.error(details);
                throw new IllegalStateException(details);
            }
        }
        TempleOfTheAxolotl.LOGGER.info("Temple footprint audit passed: {} nonoverlapping placements", tasks.size());
    }

    private static boolean footprintsOverlap(Placement a, Placement b) {
        return axisOverlaps(a.pos().getX(), a.size().getX(), b.pos().getX(), b.size().getX())
                && axisOverlaps(a.pos().getY(), a.size().getY(), b.pos().getY(), b.size().getY())
                && axisOverlaps(a.pos().getZ(), a.size().getZ(), b.pos().getZ(), b.size().getZ());
    }

    private static boolean axisOverlaps(int a, int aSize, int b, int bSize) {
        return (long) a < (long) b + bSize && (long) b < (long) a + aSize;
    }

    private static void placeTemplate(ServerLevel level, Placement task) {
        BlockPos offset = switch (task.rotation()) {
            case NONE -> BlockPos.ZERO;
            case CLOCKWISE_90 -> new BlockPos(14, 0, 0);
            case CLOCKWISE_180 -> new BlockPos(14, 0, 14);
            case COUNTERCLOCKWISE_90 -> new BlockPos(0, 0, 14);
        };
        BlockPos placePos = task.pos().offset(offset);
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setMirror(Mirror.NONE).setRotation(task.rotation())
                .setIgnoreEntities(!task.entities());
        if (!task.template().placeInWorld(level, placePos, placePos, settings, level.random, 2))
            throw new IllegalStateException("Could not place structure at " + task.pos());
        // Trap blocks are embedded in the user's NBT templates. Preserve their
        // authored positions and tag their tier from the maze floor, not from
        // the default (Tier I) block-entity value.
        if (task.trapLayer() > 0) {
            int tagged = 0;
            for (BlockPos cursor : BlockPos.betweenClosed(task.pos(),
                    task.pos().offset(task.size().getX() - 1,
                            task.size().getY() - 1, task.size().getZ() - 1))) {
                if (level.getBlockEntity(cursor) instanceof
                        com.devaxo.templeoftheaxolotl.pre07.TrialTrapSpawnerEntity trap) {
                    trap.setTier(task.trapLayer());
                    tagged++;
                }
            }
            if (tagged > 0)
                TempleOfTheAxolotl.LOGGER.debug("Tagged {} trial traps as Tier {} at {}",
                        tagged, task.trapLayer(), task.pos());
        }
    }

    private static void assignTreasureLoot(ServerLevel level, Placement task, Set<BlockPos> initialized) {
        BlockPos start = task.pos();
        // The transition template is 32 high and the Sanctum spans 60x48x60.
        for (BlockPos cursor : BlockPos.betweenClosed(start,
                start.offset(task.size().getX() - 1, task.size().getY() - 1, task.size().getZ() - 1))) {
            BlockPos pos = cursor.immutable();
            var state = level.getBlockState(pos);
            BlockEntity entity = level.getBlockEntity(pos);
            // Pre-07: convert empty treasure chests into inventory-free Celestial Caches
            // only after every structure has been placed. Never touch sushi barrels.
            if (state.is(Blocks.CHEST) && initialized.add(pos)) {
                level.setBlock(pos, com.devaxo.templeoftheaxolotl.pre07.Pre07Blocks.CELESTIAL_CACHE.get().defaultBlockState(), 2);
                if (level.getBlockEntity(pos) instanceof com.devaxo.templeoftheaxolotl.pre07.CelestialCacheEntity cache)
                    cache.setTier(task.treasureLayer());
                continue;
            }
            if (!(entity instanceof RandomizableContainerBlockEntity container)) continue;
            // A chest in an overlapping module must be initialized at most once.
            if (!initialized.add(pos)) continue;
            if (state.is(Blocks.TRAPPED_CHEST)) {
                // Trapped chests are intentionally empty in the structure NBT.
                // Do not touch their inventory during generation.
                continue;
            } else if (state.is(Blocks.CHEST) || state.is(Blocks.BARREL)) {
                String table = state.is(Blocks.BARREL) ? "sushi_barrel_layer_" + task.treasureLayer()
                        : "treasure_floor_" + task.treasureLayer();
                ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE,
                        ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "chests/" + table));
                // Do not inspect or clear the inventory here: ordinary dungeon loot
                // must remain an unopened, lazy loot table until first player access.
                // The NBT templates contain empty treasure containers.
                container.setLootTable(key, level.random.nextLong());
                TempleOfTheAxolotl.LOGGER.debug("Temple lazy loot assigned: {} at {} (module {} size {})",
                        table, pos, task.pos(), task.size());
            }
            container.setChanged();
        }
    }

    private record Snapshot(BlockPos origin, StructureTemplate original) {}
    private record UndoRecord(ServerLevel level, long seed, List<Snapshot> snapshots, Set<UUID> generatedEntities) {}

    private static int previewUndo(CommandSourceStack source) {
        UndoRecord undo = lastGeneration;
        if (undo == null) {
            source.sendFailure(Component.literal("No undo snapshot available (snapshots reset on restart)."));
            return 0;
        }
        if (source.getLevel() != undo.level()) {
            source.sendFailure(Component.literal("Run undo in the dimension where the maze was generated."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Undo available: seed " + undo.seed()
                + " | " + undo.snapshots().size() + " module regions. "
                + "WARNING: changes made inside the maze since generation will be overwritten. "
                + "Use /templefullreset for a destructive dimension reset if necessary."), false);
        return 1;
    }

    private static int confirmUndo(CommandSourceStack source) {
        if (activeJob != null || activeUndo != null) {
            source.sendFailure(Component.literal("Maze still building or deconstructing; wait for completion before undo."));
            return 0;
        }
        UndoRecord undo = lastGeneration;
        if (undo == null) {
            source.sendFailure(Component.literal("No undo snapshot available."));
            return 0;
        }
        if (source.getLevel() != undo.level()) {
            source.sendFailure(Component.literal("Run undo in the dimension where the maze was generated."));
            return 0;
        }
        try {
            // Remove only entities created by this generation (not players or preexisting mobs).
            for (UUID id : undo.generatedEntities()) {
                Entity generated = undo.level().getEntity(id);
                if (generated != null && !(generated instanceof Player)) generated.discard();
            }
            // Snapshots include the full sanctum volume and do not overlap.
            List<Snapshot> reverse = new ArrayList<>(undo.snapshots());
            Collections.reverse(reverse);
            for (Snapshot snapshot : reverse) {
                StructurePlaceSettings settings = new StructurePlaceSettings()
                        .setMirror(Mirror.NONE).setRotation(Rotation.NONE).setIgnoreEntities(true);
                BlockPos pos = snapshot.origin();
                if (!snapshot.original().placeInWorld(undo.level(), pos, pos,
                        settings, undo.level().random, 2)) {
                    throw new IllegalStateException("Failed to restore " + pos);
                }
            }
            retireRewards(undo.level());
            lastGeneration = null;
            buildCompleted = false;
            source.sendSuccess(() -> Component.literal("Maze undone! Restored "
                    + undo.snapshots().size() + " original module regions."), true);
            return 1;
        } catch (Exception ex) {
            TempleOfTheAxolotl.LOGGER.error("Maze undo failed", ex);
            source.sendFailure(Component.literal("Undo failed: " + ex.getMessage()
                    + ". Snapshot retained; retry after resolving the problem."));
            return 0;
        }
    }


    public static void register(RegisterCommandsEvent event) {
        if (!tickListenerRegistered) {
            NeoForge.EVENT_BUS.addListener(AxoMazeCommands::onServerTick);
            tickListenerRegistered = true;
        }
        // Keep the emergency dimension-wide deep clean; retire the legacy /axo_maze tree.
        event.getDispatcher().register(Commands.literal("templefullreset")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> startFullReset(ctx.getSource())));
    }

    /** Dev command and future session controller both use the fixed pocket dimension. */
    public static int generateInTemple(long seed, CommandSourceStack source) {
        ServerLevel temple = source.getServer().getLevel(TempleDimensionManager.TEMPLE_DIMENSION);
        if (temple == null) {
            source.sendFailure(Component.literal("Temple dimension is unavailable."));
            return 0;
        }
        return generate(temple, TEMPLE_ORIGIN, seed, source);
    }

    private static int generate(ServerLevel level, BlockPos origin, long seed,
                                net.minecraft.commands.CommandSourceStack source) {
        if (fullReset != null || lastGeneration != null || activeJob != null || activeUndo != null) {
            source.sendFailure(Component.literal("A maze undo snapshot is still active. "
                    + "Wait for automatic demolition, or use /templefullreset for an emergency deep clean."));
            return 0;
        }
        // The combined sanctum reaches 12 cells below origin (192 blocks).
        if (origin.getY() - 12 * CELL_Y < level.getMinBuildHeight() + 2
                || origin.getY() + CELL_Y >= level.getMaxBuildHeight()) {
            source.sendFailure(Component.literal("Not enough vertical world space. Fly higher and retry."));
            return 0;
        }
        try {
            Layout layout = null;
            for (int attempt = 0; attempt < 24; attempt++) {
                Layout candidate = build(seed + attempt * 104729L);
                if (candidate != null) { layout = candidate; break; }
            }
            if (layout == null) {
                source.sendFailure(Component.literal("Could not create a valid layout; try another seed."));
                return 0;
            }
            // Validate every resource BEFORE changing the world.
            Map<String, StructureTemplate> templates = new HashMap<>();
            for (String name : List.of("tota_straight", "tota_corner", "tota_tjunction",
                    "tota_fourway", "tota_deadend", "tota_bubbleelevator",
                    "tota_bait", "tota_decoy", "tota_lavastraight", "tota_lava_corner",
                    "tota_traptjunction", "tota_loot_to_next_layer", "tota_chamber_entrance",
                    "tota_treasure_hallway_straight")) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                        TempleOfTheAxolotl.MODID, "maze/" + name);
                StructureTemplate template = level.getStructureManager().get(id)
                        .orElseThrow(() -> new IllegalStateException("Missing template: " + id));
                int expectedY = (name.equals("tota_bubbleelevator") || name.equals("tota_loot_to_next_layer")) ? 32 : 16;
                Vec3i size = template.getSize();
                if (size.getX() != 15 || size.getY() != expectedY || size.getZ() != 15)
                    throw new IllegalStateException("Wrong dimensions for " + id + ": " + size);
                templates.put(name, template);
            }
            ResourceLocation sanctumId = ResourceLocation.fromNamespaceAndPath(
                    TempleOfTheAxolotl.MODID, "maze/tota_loot_sanctum");
            StructureTemplate sanctumTemplate = level.getStructureManager().get(sanctumId)
                    .orElseThrow(() -> new IllegalStateException("Missing template: " + sanctumId));
            if (!sanctumTemplate.getSize().equals(SANCTUM_SIZE))
                throw new IllegalStateException("Wrong sanctum dimensions: " + sanctumTemplate.getSize()
                        + "; expected " + SANCTUM_SIZE);

            // Plan only. Snapshots and placement occur gradually on server ticks.
            List<Placement> tasks = new ArrayList<>();
            for (Node n : layout.nodes.values()) {
                String name = templateFor(n);
                tasks.add(new Placement(world(origin, n.cell), new Vec3i(15, 16, 15),
                        templates.get(name), rotationFor(n, name), false,
                        "tota_treasure_hallway_straight".equals(name) ? layerFor(n.cell.y()) : 0,
                        layerFor(n.cell.y())));
            }
            for (Elevator e : layout.elevators) {
                tasks.add(new Placement(world(origin, new Cell(e.x(), e.lowerY(), e.z())),
                        new Vec3i(15, 32, 15), templates.get("tota_bubbleelevator"), Rotation.NONE, false));
            }
            for (Dropper d : layout.droppers) {
                tasks.add(new Placement(world(origin, new Cell(d.x(), d.lowerY(), d.z())),
                        new Vec3i(15, 32, 15), templates.get("tota_loot_to_next_layer"), d.rotation(), false,
                        Math.max(1, layerFor(d.lowerY()) - 1)));
            }
            tasks.add(new Placement(world(origin, SANCTUM_MIN), SANCTUM_SIZE,
                    sanctumTemplate, Rotation.NONE, true, 4));
            // Abort before placing anything if two modules claim the same blocks.
            validatePlacementFootprints(tasks);
            buildCompleted = false;
            lastGeneration = new UndoRecord(level, seed, new ArrayList<>(), new HashSet<>());
            activeTempleId = UUID.randomUUID();
            activeJob = new BuildJob(level, source, seed, tasks);
            int total = tasks.size();
            source.sendSuccess(() -> Component.literal("The Temple Is Arising: " + total
                    + " modules at " + MODULES_PER_TICK + " per server tick"), true);
            TempleOfTheAxolotl.LOGGER.info("Temple generation queued: {} modules, seed {}", total, seed);
            return 1;
        } catch (Exception ex) {
            TempleOfTheAxolotl.LOGGER.error("Axo maze generation failed", ex);
            source.sendFailure(Component.literal("Maze generation failed: " + ex.getMessage()));
            return 0;
        }
    }

    private static int layerFor(int proceduralY) {
        if (proceduralY == 0) return 1;
        if (proceduralY >= -2) return 2;
        if (proceduralY >= -5) return 3;
        return 4;
    }

    private record Cell(int x, int y, int z) {
        Cell step(Direction d) { return new Cell(x + d.getStepX(), y, z + d.getStepZ()); }
    }
    private static final class Node {
        final Cell cell;
        final EnumSet<Direction> exits = EnumSet.noneOf(Direction.class);
        String special;
        String variant; // Deterministic optional room choice for this node.
        Node(Cell cell) { this.cell = cell; }
    }
    private record Elevator(int x, int lowerY, int z) {}
    private record Dropper(int x, int lowerY, int z, Rotation rotation) {}
    private static final class Layout {
        final LinkedHashMap<Cell, Node> nodes = new LinkedHashMap<>();
        final HashSet<Cell> reserved = new HashSet<>();
        final List<Elevator> elevators = new ArrayList<>();
        final List<Dropper> droppers = new ArrayList<>();
        Node add(Cell c) {
            if (!reserved.add(c)) throw new IllegalStateException("Collision: " + c);
            Node n = new Node(c);
            nodes.put(c, n);
            return n;
        }
        Node requireNode(Cell cell, String context) {
            Node node = nodes.get(cell);
            if (node == null) throw new IllegalStateException("Missing maze node " + cell + " during " + context);
            return node;
        }
        void connect(Cell a, Cell b) {
            Direction d = direction(a, b);
            requireNode(a, "connect source").exits.add(d);
            requireNode(b, "connect destination").exits.add(d.getOpposite());
        }
    }

    private static Layout build(long seed) {
        Random rng = new Random(seed);
        Layout layout = new Layout();

        // Fixed dropper locations preserve the already playtested stage progression.
        for (int stage = 0; stage < 3; stage++) {
            int bottom = TOP_Y[stage] - FLOORS[stage] + 1;
            reserve(layout, new Cell(7, bottom, 7));
            reserve(layout, new Cell(7, bottom - 1, 7));
            layout.droppers.add(new Dropper(7, bottom - 1, 7, Rotation.NONE));
        }
        // The final connector is immovable and remains unrotated.
        reserve(layout, FINAL_ELEVATOR_UPPER);
        reserve(layout, FINAL_ELEVATOR_LOWER);
        layout.elevators.add(new Elevator(FINAL_ELEVATOR_LOWER.x(),
                FINAL_ELEVATOR_LOWER.y(), FINAL_ELEVATOR_LOWER.z()));

        // Place intermediate elevators using seeded randomized backtracking.
        // Two modules need clearance only when their vertical ranges overlap or
        // touch; otherwise distant floors may safely reuse the X/Z grid.
        List<int[]> transitions = new ArrayList<>();
        for (int stage = 0; stage < 4; stage++) {
            for (int f = 0; f < FLOORS[stage] - 1; f++)
                transitions.add(new int[]{TOP_Y[stage] - f - 1, stage});
        }
        if (!placeElevators(layout, transitions, 0, rng)) return null;

        // Reserve the dedicated entrance while carving; add it afterward so
        // the spanning tree cannot rely on its forbidden EAST/WEST doors.
        Cell first = new Cell(1, 0, 1);
        reserve(layout, first);

        // Ordinary nodes on the ten procedural floors only (Y 0 through -9).
        for (int y = 0; y >= -9; y--) {
            List<Cell> floor = new ArrayList<>();
            for (int x = 0; x < GRID; x++) {
                for (int z = 0; z < GRID; z++) {
                    Cell c = new Cell(x, y, z);
                    if (!layout.reserved.contains(c)) floor.add(layout.add(c).cell);
                }
            }

            // A randomized spanning tree connects every ordinary cell.
            // Special modules are interior islands, not obstacles spanning the floor.
            Cell start = floor.get(rng.nextInt(floor.size()));
            Set<Cell> seen = new HashSet<>();
            ArrayDeque<Cell> stack = new ArrayDeque<>();
            seen.add(start);
            stack.push(start);
            while (!stack.isEmpty()) {
                Cell at = stack.peek();
                List<Cell> options = new ArrayList<>();
                for (Direction d : HORIZONTAL) {
                    Cell next = at.step(d);
                    if (layout.nodes.containsKey(next) && !seen.contains(next)) options.add(next);
                }
                if (options.isEmpty()) stack.pop();
                else {
                    Cell next = options.get(rng.nextInt(options.size()));
                    layout.connect(at, next);
                    seen.add(next);
                    stack.push(next);
                }
            }
            if (seen.size() != floor.size()) return null;

            // Optional loops; both ends are always opened together.
            for (Cell at : floor) {
                for (Direction d : List.of(Direction.EAST, Direction.SOUTH)) {
                    Cell next = at.step(d);
                    if (layout.nodes.containsKey(next)
                            && !layout.nodes.get(at).exits.contains(d)
                            && rng.nextDouble() < 0.30) layout.connect(at, next);
                }
            }
        }

        // Dedicated entrance is the only ordinary room omitted from the DFS.
        layout.reserved.remove(first);
        Node entrance = layout.add(first);
        entrance.special = "entrance";
        for (Direction d : List.of(Direction.NORTH, Direction.SOUTH))
            layout.connect(first, first.step(d));

        // Connect each fixed progression module to its physical template doors.
        for (Elevator e : layout.elevators) {
            Cell upper = new Cell(e.x(), e.lowerY() + 1, e.z());
            Cell lower = new Cell(e.x(), e.lowerY(), e.z());
            Cell north = upper.step(Direction.NORTH);
            layout.requireNode(north, "elevator upper NORTH doorway")
                    .exits.add(Direction.SOUTH);
            if (!lower.equals(FINAL_ELEVATOR_LOWER)) {
                Cell south = lower.step(Direction.SOUTH);
                layout.requireNode(south, "elevator lower SOUTH doorway")
                        .exits.add(Direction.NORTH);
            }
        }
        for (Dropper d : layout.droppers) {
            Cell upper = new Cell(d.x(), d.lowerY() + 1, d.z());
            Cell lower = new Cell(d.x(), d.lowerY(), d.z());
            layout.requireNode(upper.step(Direction.NORTH), "dropper upper NORTH doorway")
                    .exits.add(Direction.SOUTH);
            for (Direction dir : HORIZONTAL) {
                layout.requireNode(lower.step(dir), "dropper lower arrival doorway")
                        .exits.add(dir.getOpposite());
            }
        }

        // Sanctum footprint is fixed and occupies Y -12..-10, outside the
        // ten procedural floors. Its only external connection is the fixed
        // elevator's lower SOUTH doorway.
        for (int x = SANCTUM_MIN.x(); x < SANCTUM_MIN.x() + 4; x++) {
            for (int z = SANCTUM_MIN.z(); z < SANCTUM_MIN.z() + 4; z++) {
                for (int y = SANCTUM_MIN.y(); y < SANCTUM_MIN.y() + 3; y++) {
                    if (layout.reserved.contains(new Cell(x, y, z))) return null;
                }
            }
        }
        if (!validateProgression(layout)) return null;

        // Preserve the user's current schematic variants.
        for (Node n : layout.nodes.values()) {
            if (n.special != null) continue;
            String base = templateFor(n);
            double roll = rng.nextDouble();
            n.variant = switch (base) {
                case "tota_straight" -> roll < 0.15 ? "tota_lavastraight" : null;
                case "tota_corner" -> roll < 0.15 ? "tota_lava_corner" : null;
                case "tota_tjunction" -> roll < 0.20 ? "tota_traptjunction" : null;
                case "tota_deadend" -> roll < 0.15 ? "tota_bait" : null;
                default -> null;
            };
        }
        // Decoys need a connected dead-end/bait neighbor and cannot touch other decoys.
        List<Node> decoyCandidates = new ArrayList<>();
        for (Node n : layout.nodes.values()) {
            if (n.special != null || n.variant != null || !"tota_straight".equals(templateFor(n))) continue;
            boolean hasDeadEnd = false;
            for (Direction d : n.exits) {
                Node neighbor = layout.nodes.get(n.cell.step(d));
                if (neighbor != null && neighbor.exits.contains(d.getOpposite())
                        && neighbor.exits.size() == 1) hasDeadEnd = true;
            }
            if (hasDeadEnd) decoyCandidates.add(n);
        }
        Collections.shuffle(decoyCandidates, rng);
        for (Node n : decoyCandidates) {
            if (rng.nextDouble() >= 0.10) continue;
            boolean touchesDecoy = false;
            for (Direction d : HORIZONTAL) {
                Node neighbor = layout.nodes.get(n.cell.step(d));
                if (neighbor != null && "tota_decoy".equals(neighbor.variant)) touchesDecoy = true;
            }
            if (!touchesDecoy) n.variant = "tota_decoy";
        }
        // Replace 1-4 existing straight corridors, no more than one per major layer.
        // A fallback guarantees at least one placement whenever the maze has a straight.
        List<Integer> layers = new ArrayList<>(List.of(1, 2, 3, 4));
        Collections.shuffle(layers, rng);
        int chosen = 0;
        for (int layer : layers) {
            List<Node> candidates = new ArrayList<>();
            for (Node n : layout.nodes.values()) {
                if (n.special == null && layerFor(n.cell.y()) == layer
                        && n.exits.size() == 2 && isOpposite(n.exits)) candidates.add(n);
            }
            if (candidates.isEmpty()) continue;
            if (chosen > 0 && rng.nextDouble() >= 0.40) continue;
            Node pick = candidates.get(rng.nextInt(candidates.size()));
            pick.variant = "tota_treasure_hallway_straight";
            chosen++;
        }
        for (Node n : layout.nodes.values()) {
            if (n.exits.isEmpty() || n.exits.size() > 4) return null;
            rotationFor(n, templateFor(n));
        }
        return layout;
    }

    /**
     * Validate the complete ten-floor traversal graph, including the actual
     * entrances on both halves of every two-cell elevator and loot dropper.
     *
     * A corridor opening into a reserved module is legal only when that
     * module's template has a matching doorway on that exact side/floor.
     * Every corridor must be reachable from the first entrance, and the
     * final sanctum doorway must be reachable too.
     */
    private static boolean validateProgression(Layout layout) {
        Map<Cell, EnumSet<Direction>> ports = new HashMap<>();
        Map<Cell, Cell> verticalLinks = new HashMap<>();

        for (Node node : layout.nodes.values()) {
            ports.put(node.cell, EnumSet.copyOf(node.exits));
        }

        for (Elevator e : layout.elevators) {
            Cell upper = new Cell(e.x, e.lowerY + 1, e.z);
            Cell lower = new Cell(e.x, e.lowerY, e.z);
            // Elevator template: upper NORTH, lower SOUTH.
            if (ports.putIfAbsent(upper, EnumSet.of(Direction.NORTH)) != null
                    || ports.putIfAbsent(lower, EnumSet.of(Direction.SOUTH)) != null) return false;
            verticalLinks.put(upper, lower);
            verticalLinks.put(lower, upper);
        }

        for (Dropper d : layout.droppers) {
            Cell upper = new Cell(d.x, d.lowerY + 1, d.z);
            Cell lower = new Cell(d.x, d.lowerY, d.z);
            // Loot dropper: one rotated upper port, four lower ports.
            if (ports.putIfAbsent(upper, EnumSet.of(rotate(Direction.NORTH, d.rotation))) != null
                    || ports.putIfAbsent(lower, EnumSet.allOf(Direction.class)) != null) return false;
            verticalLinks.put(upper, lower);
            verticalLinks.put(lower, upper);
        }

        // Every horizontal doorway must have a matching doorway in its neighbor.
        // The final sanctum is an intentional external exit, not a corridor cell.
        for (Map.Entry<Cell, EnumSet<Direction>> entry : ports.entrySet()) {
            Cell cell = entry.getKey();
            for (Direction direction : entry.getValue()) {
                if (cell.equals(FINAL_ELEVATOR_LOWER) && direction == Direction.SOUTH) continue;
                Cell next = cell.step(direction);
                EnumSet<Direction> neighborPorts = ports.get(next);
                if (neighborPorts == null || !neighborPorts.contains(direction.getOpposite())) {
                    return false;
                }
            }
        }

        // Traverse both horizontal doors and the built-in vertical transitions.
        Cell entrance = new Cell(1, 0, 1);
        if (!ports.containsKey(entrance) || !ports.containsKey(FINAL_ELEVATOR_LOWER)) return false;
        Set<Cell> visited = new HashSet<>();
        ArrayDeque<Cell> queue = new ArrayDeque<>();
        visited.add(entrance);
        queue.add(entrance);
        while (!queue.isEmpty()) {
            Cell at = queue.removeFirst();
            for (Direction direction : ports.get(at)) {
                if (at.equals(FINAL_ELEVATOR_LOWER) && direction == Direction.SOUTH) continue;
                Cell next = at.step(direction);
                if (visited.add(next)) queue.addLast(next);
            }
            Cell otherFloor = verticalLinks.get(at);
            if (otherFloor != null && visited.add(otherFloor)) queue.addLast(otherFloor);
        }

        // Ensures all ten floors and their progression modules are connected.
        return visited.size() == ports.size()
                && visited.contains(FINAL_ELEVATOR_LOWER);
    }

    private static boolean placeElevators(Layout layout, List<int[]> transitions,
                                          int index, Random rng) {
        if (index == transitions.size()) return true;
        int lowerY = transitions.get(index)[0];
        List<int[]> candidates = new ArrayList<>();
        for (int x = 1; x < GRID - 1; x++) for (int z = 1; z < GRID - 1; z++) {
            Cell upper = new Cell(x, lowerY + 1, z);
            Cell lower = new Cell(x, lowerY, z);
            if (layout.reserved.contains(upper) || layout.reserved.contains(lower)) continue;
            if (layout.reserved.contains(upper.step(Direction.NORTH))
                    || layout.reserved.contains(lower.step(Direction.SOUTH))) continue;
            if (!moduleClearance(layout, x, z, lowerY)) continue;
            if (blocksModuleDoor(layout, upper) || blocksModuleDoor(layout, lower)) continue;
            candidates.add(new int[]{x, z});
        }
        Collections.shuffle(candidates, rng);
        for (int[] pos : candidates) {
            Cell upper = new Cell(pos[0], lowerY + 1, pos[1]);
            Cell lower = new Cell(pos[0], lowerY, pos[1]);
            layout.reserved.add(upper);
            layout.reserved.add(lower);
            Elevator elevator = new Elevator(pos[0], lowerY, pos[1]);
            layout.elevators.add(elevator);
            if (placeElevators(layout, transitions, index + 1, rng)) return true;
            layout.elevators.remove(layout.elevators.size() - 1);
            layout.reserved.remove(upper);
            layout.reserved.remove(lower);
        }
        return false;
    }
    private static boolean blocksModuleDoor(Layout layout, Cell candidate) {
        for (Elevator e : layout.elevators) {
            Cell upperDoor = new Cell(e.x(), e.lowerY() + 1, e.z() - 1);
            Cell lowerDoor = new Cell(e.x(), e.lowerY(), e.z() + 1);
            if (candidate.equals(upperDoor) || (!lowerDoor.equals(FINAL_ELEVATOR_LOWER.step(Direction.SOUTH))
                    && candidate.equals(lowerDoor))) return true;
        }
        for (Dropper d : layout.droppers) {
            Cell upper = new Cell(d.x(), d.lowerY() + 1, d.z());
            Cell lower = new Cell(d.x(), d.lowerY(), d.z());
            if (candidate.equals(upper.step(Direction.NORTH))) return true;
            for (Direction direction : HORIZONTAL)
                if (candidate.equals(lower.step(direction))) return true;
        }
        return false;
    }
    private static boolean moduleClearance(Layout layout, int x, int z, int lowerY) {
        for (Elevator e : layout.elevators) {
            if (verticalNear(lowerY, e.lowerY())
                    && Math.max(Math.abs(x - e.x()), Math.abs(z - e.z())) < 3) return false;
        }
        for (Dropper d : layout.droppers) {
            if (verticalNear(lowerY, d.lowerY())
                    && Math.max(Math.abs(x - d.x()), Math.abs(z - d.z())) < 3) return false;
        }
        return true;
    }
    private static boolean verticalNear(int a, int b) {
        // Each module occupies two Y cells. Include one additional floor of clearance.
        return Math.abs(a - b) <= 2;
    }

    private static void reserve(Layout layout, Cell c) {
        if (!layout.reserved.add(c)) throw new IllegalStateException("Overlapping module: " + c);
    }

    private static Direction rotate(Direction direction, Rotation rotation) {
        return switch (rotation) {
            case NONE -> direction;
            case CLOCKWISE_90 -> direction.getClockWise();
            case CLOCKWISE_180 -> direction.getOpposite();
            case COUNTERCLOCKWISE_90 -> direction.getCounterClockWise();
        };
    }

    private static boolean isOpposite(Set<Direction> dirs) {
        if (dirs.size() != 2) return false;
        Direction first = dirs.iterator().next();
        return dirs.contains(first.getOpposite());
    }
    private static Direction direction(Cell a, Cell b) {
        for (Direction d : HORIZONTAL) if (a.step(d).equals(b)) return d;
        throw new IllegalArgumentException("Not horizontally adjacent: " + a + " " + b);
    }
    private static String templateFor(Node n) {
        if ("entrance".equals(n.special)) return "tota_chamber_entrance";
        if (n.variant != null) return n.variant;
        return switch (n.exits.size()) {
            case 1 -> "tota_deadend";
            case 2 -> isOpposite(n.exits) ? "tota_straight" : "tota_corner";
            case 3 -> "tota_tjunction";
            case 4 -> "tota_fourway";
            default -> throw new IllegalStateException("Invalid exit count: " + n.cell);
        };
    }
    private static Rotation rotationFor(Node n, String name) {
        EnumSet<Direction> base = switch (name) {
            case "tota_straight", "tota_lavastraight", "tota_decoy", "tota_treasure_hallway_straight", "tota_chamber_entrance" ->
                    EnumSet.of(Direction.NORTH, Direction.SOUTH);
            case "tota_corner", "tota_lava_corner" -> EnumSet.of(Direction.EAST, Direction.SOUTH);
            case "tota_tjunction", "tota_traptjunction" -> EnumSet.of(Direction.WEST, Direction.EAST, Direction.SOUTH);
            case "tota_fourway" -> EnumSet.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
            case "tota_deadend", "tota_bait" -> EnumSet.of(Direction.SOUTH);
            default -> throw new IllegalStateException("Unknown template " + name);
        };
        for (int turns = 0; turns < 4; turns++) {
            EnumSet<Direction> rotated = EnumSet.noneOf(Direction.class);
            for (Direction d : base) {
                Direction current = d;
                for (int j = 0; j < turns; j++) current = current.getClockWise();
                rotated.add(current);
            }
            if (rotated.equals(n.exits)) return switch (turns) {
                case 0 -> Rotation.NONE;
                case 1 -> Rotation.CLOCKWISE_90;
                case 2 -> Rotation.CLOCKWISE_180;
                default -> Rotation.COUNTERCLOCKWISE_90;
            };
        }
        throw new IllegalStateException("No matching rotation for " + name + " at " + n.cell
                + " exits=" + n.exits);
    }
    private static BlockPos world(BlockPos origin, Cell c) {
        return origin.offset(c.x * CELL_XZ, c.y * CELL_Y, c.z * CELL_XZ);
    }
    private static void place(ServerLevel level, BlockPos origin, Cell cell,
                              StructureTemplate template, Rotation rotation) {
        BlockPos min = world(origin, cell);
        // Minecraft rotates structure blocks around the template's local (0,0,0).
        // Translate its rotated footprint back into the positive 15x15 grid cell.
        BlockPos offset = switch (rotation) {
            case NONE -> BlockPos.ZERO;
            case CLOCKWISE_90 -> new BlockPos(14, 0, 0);
            case CLOCKWISE_180 -> new BlockPos(14, 0, 14);
            case COUNTERCLOCKWISE_90 -> new BlockPos(0, 0, 14);
        };
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setMirror(Mirror.NONE).setRotation(rotation).setIgnoreEntities(true);
        boolean placed = template.placeInWorld(level, min.offset(offset), min.offset(offset),
                settings, level.random, 2);
        if (!placed) throw new IllegalStateException("Could not place template at " + min);
    }
}
