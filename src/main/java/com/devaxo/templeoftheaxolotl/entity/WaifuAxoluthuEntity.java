package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.devaxo.templeoftheaxolotl.registry.ModBlocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import java.util.UUID;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.world.phys.AABB;

/** Waifu Axoluthu: non-hostile integration foundation, separate from boss_axoluthu. */
public class WaifuAxoluthuEntity extends PathfinderMob implements GeoEntity {
    public enum MovementMode { STAY, FOLLOW, FREE }
    // Action state is synchronized so client GeckoLib animations match server-side effects.
    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(WaifuAxoluthuEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACTION_STARTED = SynchedEntityData.defineId(WaifuAxoluthuEntity.class, EntityDataSerializers.INT);
    public static final int ACTION_NONE=0, ACTION_FOCUSED=1, ACTION_WORKSTATION=2, ACTION_PLANT=3, ACTION_ATTACK=4, ACTION_CONSTRUCTION_POKE=5;
    private static final EntityDataAccessor<Integer> RAIL_TARGET = SynchedEntityData.defineId(WaifuAxoluthuEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RAIL_START = SynchedEntityData.defineId(WaifuAxoluthuEntity.class, EntityDataSerializers.INT);
    public void beginRailBeam(net.minecraft.world.entity.LivingEntity target) {
        if (level().isClientSide) return;
        entityData.set(RAIL_TARGET, target.getId());
        entityData.set(RAIL_START, tickCount);
    }
    public int railTargetId() { return entityData.get(RAIL_TARGET); }
    public int railBeamStart() { return entityData.get(RAIL_START); }
    private int actionUntil;
    private BlockPos actionTarget;
    private boolean manualGoHome;
    private BlockPos lastFootstep;
    private int lastFootstepTick;
    private BlockPos autonomousSleepingBed;
    private BlockPos registeredBed;
    private long nextBedHomeScan;

    /** Home is a walkable position beside the bed, never the bed block itself. */
    private void updateBedHome(ServerLevel level, BlockPos bed) {
        if (bed == null || !level.getBlockState(bed).getBlock().getClass().getSimpleName().contains("BedBlock")) return;
        if (manuallyAssignedHome) return; // explicit owner commands take precedence
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos feet = bed.relative(direction);
            if (!level.getChunkSource().hasChunk(feet.getX() >> 4, feet.getZ() >> 4)) continue;
            BlockPos below = feet.below();
            if (!level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)) continue;
            if (!level.noCollision(this, new AABB(feet.getX()+.1, feet.getY(), feet.getZ()+.1,
                    feet.getX()+.9, feet.getY()+getBbHeight(), feet.getZ()+.9))) continue;
            double distance = feet.distSqr(blockPosition());
            if (distance < bestDistance) { best = feet.immutable(); bestDistance = distance; }
        }
        if (best != null) {
            registeredBed = bed.immutable();
            homePosition = best;
        }
    }

    private void refreshBedHome(ServerLevel level) {
        if (manuallyAssignedHome || tickCount < nextBedHomeScan) return;
        nextBedHomeScan = tickCount + 200;
        BlockPos center = registeredBed != null ? registeredBed : homePosition;
        if (center == null || !level.getChunkSource().hasChunk(center.getX() >> 4, center.getZ() >> 4)) return;
        BlockPos best = null;
        double nearest = Double.MAX_VALUE;
        for (BlockPos candidate : BlockPos.betweenClosed(center.offset(-6,-2,-6), center.offset(6,2,6))) {
            if (!level.getChunkSource().hasChunk(candidate.getX() >> 4, candidate.getZ() >> 4)) continue;
            BlockState state = level.getBlockState(candidate);
            if (!(state.getBlock() instanceof net.minecraft.world.level.block.BedBlock)
                    || state.getValue(net.minecraft.world.level.block.BedBlock.PART)
                    != net.minecraft.world.level.block.state.properties.BedPart.HEAD) continue;
            double d = candidate.distSqr(center);
            if (d < nearest) { best = candidate.immutable(); nearest = d; }
        }
        if (best != null) updateBedHome(level, best);
        else if (registeredBed != null && registeredBed.distSqr(center) <= 64) registeredBed = null;
    }

    public void startAction(int action, int durationTicks, BlockPos target) {
        if(level().isClientSide)return;
        if(entityData.get(ACTION)==action && tickCount<actionUntil && java.util.Objects.equals(target,actionTarget))return;
        entityData.set(ACTION,action);
        entityData.set(ACTION_STARTED,tickCount);
        actionUntil=tickCount+durationTicks;
        actionTarget=target;
    }
    public boolean actionReady(int action, BlockPos target, int keyframeTicks, int totalTicks) {
        if(!java.util.Objects.equals(target,actionTarget) || entityData.get(ACTION)!=action || tickCount>=actionUntil) {
            startAction(action,totalTicks,target);
            return false;
        }
        return tickCount-entityData.get(ACTION_STARTED)>=keyframeTicks;
    }
    public void finishAction() { entityData.set(ACTION,ACTION_NONE); actionTarget=null; actionUntil=0; }
    public boolean isGoingHome() {return manualGoHome;}
    public boolean requestGoHome(UUID requester) {
        if(bondedPlayer==null || !bondedPlayer.equals(requester) || homePosition==null)return false;
        if(!getHomeDimension().equals(level().dimension().location().toString()))return false;
        manualGoHome=true;
        getNavigation().stop();
        finishAction();
        decisionCooldown=0;
        return true;
    }
    /** Release an owner's manual home recall without changing home or project progress. */
    public boolean easeHome(UUID requester) {
        if (bondedPlayer == null || !bondedPlayer.equals(requester)) return false;
        manualGoHome = false;
        getNavigation().stop();
        // The manual recall can leave WAIT_AT_HOME active indefinitely.
        if (currentGoal.equals("WAIT_AT_HOME") || currentGoal.equals("RETURN_HOME")) {
            chooseGoal("OBSERVE", "owner eased manual home recall");
        }
        decisionCooldown = 0;
        diagnostic("Home", "EASE manual recall canceled; autonomous activity resumed");
        return true;
    }
    public boolean navigateToSavedLocation(BlockPos destination, double speed) {
        if(!(level() instanceof ServerLevel server))return false;
        double dx=destination.getX()+.5-getX(), dz=destination.getZ()+.5-getZ();
        double length=Math.sqrt(dx*dx+dz*dz);
        if(length<1)return true;
        // Pathfinding to a chunk outside simulation range can fail immediately.
        // Use short, loaded waypoints; recalculate on each autonomous decision.
        double step=Math.min(length,12);
        BlockPos waypoint=BlockPos.containing(getX()+dx/length*step,getY(),getZ()+dz/length*step);
        if(!server.getChunkSource().hasChunk(waypoint.getX()>>4,waypoint.getZ()>>4)) {
            step=Math.min(length,5);
            waypoint=BlockPos.containing(getX()+dx/length*step,getY(),getZ()+dz/length*step);
        }
        if(!server.getChunkSource().hasChunk(waypoint.getX()>>4,waypoint.getZ()>>4))return false;
        return getNavigation().moveTo(waypoint.getX()+.5,waypoint.getY(),waypoint.getZ()+.5,speed);
    }
    public boolean autonomousWithinTerritory(BlockPos pos) {
        BlockPos center=stage4WorkshopOrigin!=null&&!stage4WorkshopComplete?stage4WorkshopOrigin:homePosition;
        return center==null || (Math.abs(pos.getX()-center.getX())<=256 && Math.abs(pos.getZ()-center.getZ())<=256);
    }
    /** Quantize actual movement, not head yaw, into eight 45-degree headings. */
    private int footstepHeading() {
        var v=getDeltaMovement();
        double x=v.x, z=v.z;
        if(x*x+z*z<0.0004) {
            float radians=getYRot()*(float)Math.PI/180F;
            x=-Math.sin(radians);z=Math.cos(radians);
        }
        // 0=north, 1=northeast, 2=east ... 7=northwest.
        return Math.floorMod((int)Math.round(Math.atan2(x,-z)*4.0/Math.PI),8);
    }

    /** Sleeping is purely personal; it never advances the world clock. */
    private boolean tickAutonomousBedtime(ServerLevel server) {
        if(movementMode!=MovementMode.FREE || manualGoHome || bondedPlayer==null
                || homePosition==null || !getHomeDimension().equals(server.dimension().location().toString())) {
            if(isSleeping()) stopSleeping();
            autonomousSleepingBed=null;
            return false;
        }
        if(server.isDay()) {
            if(isSleeping())stopSleeping();
            autonomousSleepingBed=null;
            return false;
        }
        if(isSleeping()) {
            getNavigation().stop();
            chooseGoal("SLEEPING_AT_HOME","resting until dawn");
            return true;
        }
        BlockPos bed=null;
        double best=Double.MAX_VALUE;
        for(BlockPos candidate:BlockPos.betweenClosed(homePosition.offset(-8,-2,-8),homePosition.offset(8,3,8))) {
            if(!server.getChunkSource().hasChunk(candidate.getX()>>4,candidate.getZ()>>4))continue;
            BlockState state=server.getBlockState(candidate);
            if(!(state.getBlock() instanceof net.minecraft.world.level.block.BedBlock)
                    || state.getValue(net.minecraft.world.level.block.BedBlock.PART)!=net.minecraft.world.level.block.state.properties.BedPart.HEAD
                    || state.getValue(net.minecraft.world.level.block.BedBlock.OCCUPIED))continue;
            double d=distanceToSqr(candidate.getX()+.5,candidate.getY(),candidate.getZ()+.5);
            // Prefer accessible cyan, then purple, then ordinary beds.
            net.minecraft.world.item.DyeColor dye=((net.minecraft.world.level.block.BedBlock)state.getBlock()).getColor();
            d+=dye==net.minecraft.world.item.DyeColor.CYAN?-1000.0:dye==net.minecraft.world.item.DyeColor.PURPLE?-500.0:0;
            if(d<best){best=d;bed=candidate.immutable();}
        }
        if(bed==null)return false; // Existing night return/wait fallback handles this.
        autonomousSleepingBed=bed;
        updateBedHome(server,bed);
        if(distanceToSqr(bed.getX()+.5,bed.getY(),bed.getZ()+.5)>5.0) {
            if(tickCount%20==0)navigateToSavedLocation(bed,1.1);
            chooseGoal("APPROACH_BED","heading to an available bed");
            return true;
        }
        getNavigation().stop();
        finishAction();
        startSleeping(bed);
        chooseGoal("SLEEPING_AT_HOME","using available bed until dawn");
        return true;
    }

    private void footprint(ServerLevel level) {
        if(tickCount-lastFootstepTick<10 || getDeltaMovement().horizontalDistanceSqr()<0.0025)return;
        BlockPos p=blockPosition();
        if(lastFootstep!=null && p.distSqr(lastFootstep)<9)return;
        if(!level.getChunkSource().hasChunk(p.getX()>>4,p.getZ()>>4))return;
        BlockState old=level.getBlockState(p);
        boolean grass=old.is(Blocks.SHORT_GRASS)||old.is(Blocks.TALL_GRASS)||old.is(Blocks.FERN)
                ||old.is(Blocks.LARGE_FERN)||old.is(Blocks.DEAD_BUSH)
                ||net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(old.getBlock()).getPath().contains("leaf_litter");
        // No solid terrain, carpets, saplings, flowers, crops or block entities are overwritten.
        if(!old.isAir()&&!grass)return;
        if(level.getBlockEntity(p)!=null)return;
        if(!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),net.minecraft.core.Direction.UP))return;
        if(old.is(Blocks.TALL_GRASS)||old.is(Blocks.LARGE_FERN)) {
            net.minecraft.core.BlockPos other=old.getValue(net.minecraft.world.level.block.DoublePlantBlock.HALF)==net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER?p.above():p.below();
            if(level.getBlockState(other).is(old.getBlock()))level.removeBlock(other,false);
        }
        if(level.setBlock(p,ModBlocks.CELESTIAL_FOOTSTEP.get().defaultBlockState()
                .setValue(com.devaxo.templeoftheaxolotl.block.CelestialFootstepBlock.HEADING,footstepHeading())
                .setValue(com.devaxo.templeoftheaxolotl.block.CelestialFootstepBlock.LEFT,nextFootLeft),3)) {
            level.scheduleTick(p,ModBlocks.CELESTIAL_FOOTSTEP.get(),600);
            lastFootstep=p.immutable();lastFootstepTick=tickCount;nextFootLeft=!nextFootLeft;
        }
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ACTION,ACTION_NONE);
        builder.define(ACTION_STARTED,0);
        builder.define(RAIL_TARGET,-1);
        builder.define(RAIL_START,-1000);
    }

    private MovementMode movementMode = MovementMode.FREE;
    private UUID bondedPlayer;
    private BlockPos stayAnchor;
    private String currentGoal = "EXPLORE";
    private String lastDiagnostic = "Brain initialized";
    private int decisionCooldown;
    private long decisionsMade;
    private long goalsCompleted;
    private int goalTicks;
    private String previousGoal = "NONE";
    private String lastGoalReason = "Initial state";
    private BlockPos homePosition;
    private String assignedHomeDimension = "";
    private boolean manuallyAssignedHome;
    private int explorationCount;
    private int reflectionCount;
    private int plannerCycles;
    private int failedPaths;
    private String lastPriorityScores = "not evaluated";
    private WaifuAxoluthuTaskPlanner.Desire selectedDesire = WaifuAxoluthuTaskPlanner.Desire.EXPLORE;
    private String taskPlanSummary = "not planned";
    private int taskPlanRevisions;

    private BlockPos settlementStorageOrigin;
    private int settlementStorageStep;
    private boolean settlementStorageComplete;
    private String settlementStatus="NOT_STARTED";
    public BlockPos getSettlementStorageOrigin() {return settlementStorageOrigin;}
    public void setSettlementStorageOrigin(BlockPos pos) {settlementStorageOrigin=pos;}
    public int getSettlementStorageStep() {return settlementStorageStep;}
    public void advanceSettlementStorageStep() {settlementStorageStep++;}
    public boolean isSettlementStorageComplete() {return settlementStorageComplete;}
    public void finishSettlementStorage() {settlementStorageComplete=true;}
    /** One-time, explicitly invoked dev rollback: keeps the completed workshop and world. */
    public void devResetToPostWorkshop() {
        settlementStorageOrigin = null;
        settlementStorageStep = 0;
        settlementStorageComplete = false;
        settlementStatus = "NOT_STARTED";
        settlementFurnished = false;
        // Preserve the workshop's actual completion, position, and physical blocks.
        // Clear only future village requests/proposals and non-workshop registry entries.
        architecturalLearning.remove("phase35OwnerSuggestion");
        architecturalLearning.remove("phase35VillageExpansionProposal");
        net.minecraft.nbt.ListTag entries = architecturalLearning.getList(
                "phase35VillageBuildings", net.minecraft.nbt.Tag.TAG_COMPOUND);
        net.minecraft.nbt.ListTag kept = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < entries.size(); i++) {
            net.minecraft.nbt.CompoundTag entry = entries.getCompound(i);
            if ("workshop".equalsIgnoreCase(entry.getString("type")))
                kept.add(entry.copy());
        }
        architecturalLearning.put("phase35VillageBuildings", kept);
    }

    private boolean settlementFurnished;
    private String furnishingStatus="NOT_STARTED";
    private boolean shyFollow;
    private long shyFollowUntil;
    private long nextShyFollowCheck;
    private boolean nextFootLeft;
    private boolean stage4WorkshopComplete;
    private BlockPos stage4WorkshopOrigin;
    private int stage4WorkshopStep;
    private String stage4WorkshopStatus="UNSTARTED";
    public boolean isStage4WorkshopComplete() { return stage4WorkshopComplete; }
    public BlockPos getStage4WorkshopOrigin() { return stage4WorkshopOrigin; }
    public int getStage4WorkshopStep() { return stage4WorkshopStep; }
    public String getStage4WorkshopStatus() { return stage4WorkshopStatus; }
    public String getStage4WorkshopDiagnostic() {
        if(stage4WorkshopComplete) return "COMPLETE 58/58";
        String state = movementMode != MovementMode.FREE ? "PAUSED_BY_"+movementMode
                : pendingProjectDeposit ? "PAUSED_FOR_PISTON_DELIVERY"
                : pistonIdea ? "PAUSED_FOR_PISTON_PROJECT"
                : stage4WorkshopStatus;
        return "ACTIVE "+stage4WorkshopStep+"/58 origin="
                +(stage4WorkshopOrigin==null ? "NOT_SELECTED" : stage4WorkshopOrigin.toShortString())
                +" state="+state;
    }
    public void setStage4WorkshopOrigin(BlockPos p) { stage4WorkshopOrigin=p; }
    public void advanceStage4Workshop() { stage4WorkshopStep++; }
    public void finishStage4Workshop() { stage4WorkshopComplete=true; }
    private boolean pistonIdea;
    private boolean pendingProjectDeposit;
    private long lastAutomaticArchitectureScan=-10000;
    private String pistonProjectStatus="IDLE";
    public boolean hasPistonIdea() { return pistonIdea; }
    public String getPistonProjectStatus() { return pistonProjectStatus; }
    public boolean requestPistonIdea(UUID player) {
        if(bondedPlayer==null || !bondedPlayer.equals(player))return false;
        pistonIdea=true;
        pendingProjectDeposit=false;
        pistonProjectStatus="QUEUED";
        speak("piston_idea", "Ooo! I have an idea! I'm gonna make a piston! :D");
        return true;
    }
    private int resourceScanCount;
    private String lastResourceScan = "not scanned";
    public int getResourceScanCount() { return resourceScanCount; }
    public String getLastResourceScan() { return lastResourceScan; }
    public void recordResourceScan(String result) {
        resourceScanCount++;
        lastResourceScan = result;
        diagnostic("Storage", "READ_ONLY scan=" + resourceScanCount + " " + result);
    }

    // Stage 2B: explicit, bonded-player-authorized resource retrieval.
    private final net.minecraft.world.SimpleContainer acquiredMaterials = new net.minecraft.world.SimpleContainer(36);
    private int acquisitions;
    private String lastAcquisition = "none";
    public net.minecraft.world.SimpleContainer getAcquiredMaterials() { return acquiredMaterials; }
    public int getAcquisitions() { return acquisitions; }
    public String getLastAcquisition() { return lastAcquisition; }
    public void recordAcquisition(String detail) {
        acquisitions++;
        lastAcquisition = detail;
        diagnostic("Acquisition", "transaction=" + acquisitions + " " + detail);
    }
    private final java.util.Set<Long> authorizedContainers = new java.util.HashSet<>();
    private boolean autoAcquireEnabled;
    private int acquisitionCooldown;
    private String acquisitionStatus = "idle";
    public boolean isAutoAcquireEnabled() { return autoAcquireEnabled; }
    public String getAcquisitionStatus() { return acquisitionStatus; }
    public java.util.Set<Long> getAuthorizedContainers() { return java.util.Collections.unmodifiableSet(authorizedContainers); }
    public boolean authorizeContainer(BlockPos pos, UUID player, boolean allow) {
        if (bondedPlayer == null || !bondedPlayer.equals(player)) return false;
        if (allow) authorizedContainers.add(pos.asLong());
        else authorizedContainers.remove(pos.asLong());
        diagnostic("Permissions", (allow ? "ALLOW " : "REVOKE ") + pos.toShortString());
        return true;
    }
    public boolean setAutoAcquire(boolean enabled, UUID player) {
        if (bondedPlayer == null || !bondedPlayer.equals(player)) return false;
        autoAcquireEnabled = enabled;
        acquisitionCooldown = 0;
        diagnostic("Acquisition", "auto=" + enabled + " desire=" + selectedDesire);
        return true;
    }
    public void updateAcquisitionStatus(String status) {
        if (!acquisitionStatus.equals(status)) diagnostic("Acquisition", status);
        acquisitionStatus = status;
    }
    // Stage 2E/F: crafting and placement are separate, auditable actions.
    private BlockPos pendingWorkshopPosition;
    private String pendingWorkshopType = "";
    public BlockPos getPendingWorkshopPosition() { return pendingWorkshopPosition; }
    public void setPendingWorkshopPosition(BlockPos pos,String type) {
        if(!pos.equals(pendingWorkshopPosition) || !type.equals(pendingWorkshopType))
            diagnostic("Navigation","approach-before-placement type="+type+" target="+pos.toShortString());
        pendingWorkshopPosition=pos.immutable(); pendingWorkshopType=type;
    }
    public void clearPendingWorkshopPosition() { pendingWorkshopPosition=null; pendingWorkshopType=""; }
    private boolean autoBuildEnabled;
    private int workshopActionCooldown;
    private int workstationPathWarnings;
    private int workshopCrafts;
    private int workshopPlacements;
    private String workshopStatus = "not started";
    private BlockPos lastWorkshopPlacement;
    public boolean isAutoBuildEnabled() { return autoBuildEnabled; }
    public int getWorkshopCrafts() { return workshopCrafts; }
    public int getWorkshopPlacements() { return workshopPlacements; }
    public String getWorkshopStatus() { return workshopStatus; }
    public BlockPos getLastWorkshopPlacement() { return lastWorkshopPlacement; }
    public boolean setAutoBuild(boolean enabled, UUID player) {
        if (bondedPlayer == null || !bondedPlayer.equals(player)) return false;
        autoBuildEnabled = enabled;
        workshopActionCooldown = 0;
        diagnostic("Workshop", "autoBuild=" + enabled + " (crafting and block placement)");
        return true;
    }
    public void recordWorkshopStep(String detail) {
        workshopStatus=detail;
        diagnostic("Workstation",detail);
        if(detail.startsWith("PLACED")) speak("placed_crafting","There we go! My crafting table is ready! :D");
    }
    public void recordWorkshopCraft(String detail) {
        workshopCrafts++;
        workshopStatus = detail;
        diagnostic("Craft", detail + " totalCrafts=" + workshopCrafts);
        if(detail.contains("crafting_table")) speak("craft_table","Yippee! I made a crafting table!");
        else if(detail.contains("smithing_table")) speak("craft_smithing","Ooo! Smithing table crafted! :D");
    }
    public void recordWorkshopPlacement(BlockPos pos) {
        workshopPlacements++;
        lastWorkshopPlacement = pos.immutable();
        workshopStatus = "COMPLETED smithing_table at " + pos.toShortString();
        diagnostic("Build", workshopStatus + " totalPlacements=" + workshopPlacements);
        speak("workshop_complete","My smithing table is ready! What should I make next? :D");
    }
    public void reportWorkshopFailure(String detail) {
        if (!workshopStatus.equals(detail)) {
            diagnostic("Workshop", detail);
            if(detail.startsWith("MISSING_")) speak("missing_"+detail,"Hmm... I'm missing some materials. I'll need to find those first! QwQ");
        }
        workshopStatus = detail;
    }
    // Stage 3A: exclusive task ownership. Player modes always take precedence.
    public enum NavigationOwner { NONE, WORKSHOP, STORAGE, PLAYER }
    private NavigationOwner navigationOwner = NavigationOwner.NONE;
    private String constructionPhase = "IDLE";
    private int buildingScanCount;
    private String lastBuildingScan = "not scanned";
    private String resourceRoute = "none";
    public NavigationOwner getNavigationOwner() { return navigationOwner; }
    public String getConstructionPhase() { return constructionPhase; }
    public int getBuildingScanCount() { return buildingScanCount; }
    public String getLastBuildingScan() { return lastBuildingScan; }
    public String getResourceRoute() { return resourceRoute; }
    public void recordBuildingScan(String result) {
        buildingScanCount++;
        lastBuildingScan = result;
        diagnostic("Architecture", "READ_ONLY scan="+buildingScanCount+" "+result);
    }
    public void setResourceRoute(String route) {
        if(!resourceRoute.equals(route)) diagnostic("GatheringPlan", route);
        resourceRoute = route;
    }
    void updateNavigationOwner(NavigationOwner owner, String phase) {
        if(navigationOwner != owner || !constructionPhase.equals(phase)) {
            diagnostic("NavigationOwner", navigationOwner+" -> "+owner+" phase="+phase);
            if(navigationOwner != owner) getNavigation().stop();
        }
        navigationOwner = owner;
        constructionPhase = phase;
    }
    private int navigationWarningCooldown;
    // Stage 3: dialogue is state-driven and throttled; casual remarks are rarer.
    private long lastSpeechTick = -10000;
    private long lastCasualTick = -10000;
    private String lastSpeechKey = "";
    private boolean autonomousStage3 = true;
    private boolean workshopAnnounced;
    private long lastMagicGatherTick = -10000;
    private String magicGatherStatus = "idle";
    public String getMagicGatherStatus() { return magicGatherStatus; }

    public boolean isAutonomousStage3() { return autonomousStage3; }
    public void speak(String key, String words) {
        if (!(level() instanceof ServerLevel server)) return;
        long now=server.getGameTime();
        if (key.equals(lastSpeechKey) && now-lastSpeechTick < 1200) return;
        if (now-lastSpeechTick < 100) return;
        lastSpeechKey=key;
        lastSpeechTick=now;
        Component line=Component.literal("<Axoluthu> ").withStyle(net.minecraft.ChatFormatting.AQUA)
                .append(Component.literal(words).withStyle(net.minecraft.ChatFormatting.WHITE));
        for(ServerPlayer player:server.players())
            if(player.distanceToSqr(this)<32*32 || player.getUUID().equals(bondedPlayer))
                player.sendSystemMessage(line);
        diagnostic("Speech",key+" "+words);
    }
    private void casualComment(ServerLevel server) {
        if(bondedPlayer==null || movementMode!=MovementMode.FREE) return;
        long now=server.getGameTime();
        if(now-lastCasualTick<7200 || random.nextInt(90)!=0) return;
        ServerPlayer player=server.getServer().getPlayerList().getPlayer(bondedPlayer);
        if(player==null || player.level()!=level() || player.distanceToSqr(this)>144) return;
        var hit=player.pick(6,0,false);
        if(!(hit instanceof net.minecraft.world.phys.BlockHitResult blockHit)) return;
        var pos=blockHit.getBlockPos();
        if(!server.getChunkSource().hasChunk(pos.getX()>>4,pos.getZ()>>4)) return;
        var state=server.getBlockState(pos);
        if(state.isAir()) return;
        lastCasualTick=now;
        speak("curious_"+(now/7200),"I wonder why "+player.getGameProfile().getName()
                +" is staring at this dum "+state.getBlock().getName().getString()+" instead of me :hmph:");
    }

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public WaifuAxoluthuEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override public boolean removeWhenFarAway(double distance) { return false; }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));

        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 12));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    public MovementMode getMovementMode() { return movementMode; }
    public UUID getBondedPlayer() { return bondedPlayer; }
    public String getCurrentGoal() { return currentGoal; }
    public String getLastDiagnostic() { return lastDiagnostic; }
    public BlockPos getStayAnchor() { return stayAnchor; }
    public BlockPos getHomePosition() { return homePosition; }
    public boolean hasManuallyAssignedHome() { return manuallyAssignedHome; }
    public String getHomeDimension() {
        return manuallyAssignedHome ? assignedHomeDimension : level().dimension().location().toString();
    }
    public boolean setHomeFromPlayer(ServerPlayer player) {
        if (bondedPlayer == null || !bondedPlayer.equals(player.getUUID())) return false;
        if (player.level() != level()) return false;
        homePosition = player.blockPosition().immutable();
        assignedHomeDimension = player.level().dimension().location().toString();
        manuallyAssignedHome = true;
        decisionCooldown = 0;
        diagnostic("Home", "Assigned " + assignedHomeDimension + " " + homePosition.toShortString());
        return true;
    }
    public boolean clearAssignedHome(ServerPlayer player) {
        if (bondedPlayer == null || !bondedPlayer.equals(player.getUUID())) return false;
        manuallyAssignedHome = false;
        assignedHomeDimension = "";
        homePosition = blockPosition().immutable(); // safe local fallback for autonomous routines
        decisionCooldown = 0;
        diagnostic("Home", "Manual home cleared; autonomous fallback " + homePosition.toShortString());
        return true;
    }
    public long getDecisionsMade() { return decisionsMade; }
    public long getGoalsCompleted() { return goalsCompleted; }
    public String getLastGoalReason() { return lastGoalReason; }
    public int getExplorationCount() { return explorationCount; }
    public int getReflectionCount() { return reflectionCount; }
    public int getPlannerCycles() { return plannerCycles; }
    public int getFailedPaths() { return failedPaths; }
    public String getLastPriorityScores() { return lastPriorityScores; }
    public String getSelectedDesire() { return selectedDesire.name(); }
    public String getTaskPlanSummary() { return taskPlanSummary; }
    public int getTaskPlanRevisions() { return taskPlanRevisions; }

    /** Only plans a project; it does not execute storage, crafting, or building steps. */
    public boolean planDesire(WaifuAxoluthuTaskPlanner.Desire desire, UUID requester) {
        if (bondedPlayer == null || !bondedPlayer.equals(requester)) {
            diagnostic("Planner", "REJECTED plan request: requester not bonded");
            return false;
        }
        selectedDesire = desire;
        taskPlanSummary = WaifuAxoluthuTaskPlanner.create(desire).summary();
        taskPlanRevisions++;
        diagnostic("Planner", "PLAN_ONLY desire=" + desire + " revision=" + taskPlanRevisions
                + " plan=" + taskPlanSummary + " worldChanges=NONE");
        return true;
    }


    private void chooseGoal(String goal, String reason) {
        decisionsMade++;
        if (currentGoal.equals(goal)) return;
        previousGoal = currentGoal;
        currentGoal = goal;
        lastGoalReason = reason;
        goalTicks = 0;
        diagnostic("Planner", "goal=" + goal + " previous=" + previousGoal
                + " reason=" + reason + " decisions=" + decisionsMade);
    }
    private void completeGoal(String detail) {
        goalsCompleted++;
        diagnostic("Reflection", "completed=" + currentGoal + " detail=" + detail
                + " totalCompleted=" + goalsCompleted);
    }

    private void diagnostic(String subsystem, String detail) {
        lastDiagnostic = subsystem + ": " + detail;
        TempleOfTheAxolotl.LOGGER.info("[Axoluthu/{}] entity={} dimension={} pos={} {}",
                subsystem, getUUID(), level().dimension().location(), blockPosition().toShortString(), detail);
    }

    public boolean setMovementMode(MovementMode mode, UUID requester) {
        if (bondedPlayer == null || !bondedPlayer.equals(requester)) return false;
        if (movementMode == mode) return true;
        movementMode = mode;
        getNavigation().stop();
        if (mode == MovementMode.STAY) stayAnchor = blockPosition();
        String nextGoal = switch (mode) {
            case STAY -> "WAIT_AT_ANCHOR";
            case FOLLOW -> "FOLLOW_COMPANION";
            case FREE -> "EXPLORE";
        };
        chooseGoal(nextGoal, "companion requested " + mode);
        decisionCooldown = 0;
        diagnostic("Brain", "Movement mode=" + mode + " goal=" + currentGoal
                + (stayAnchor == null ? "" : " anchor=" + stayAnchor.toShortString()));
        return true;
    }

    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide || hand != InteractionHand.MAIN_HAND) return InteractionResult.SUCCESS;
        if (bondedPlayer == null) {
            if (!player.getItemInHand(hand).is(com.devaxo.templeoftheaxolotl.item.ModItems.ABYSSAL_RING.get())) {
                player.displayClientMessage(Component.literal("Axoluthu looks curiously at you. Perhaps an Abyssal Ring would help..."), false);
                return InteractionResult.CONSUME;
            }
            if (!player.getAbilities().instabuild) player.getItemInHand(hand).shrink(1);
            bondedPlayer = player.getUUID();
            addTag("tamed");
            getPersistentData().putBoolean("tota_axoluthu_tamed", true);
            movementMode = MovementMode.FOLLOW;
            decisionCooldown = 0;
            if (homePosition == null) homePosition = blockPosition();
            diagnostic("Bond", "Bond established with player=" + bondedPlayer);
            player.sendSystemMessage(Component.literal("F-for me? You made this.. for me?"));
            player.sendSystemMessage(Component.literal("Hmph.. took you long enough.."));
            player.sendSystemMessage(Component.literal("i-i mean, not that i was waiting or anything"));
            player.displayClientMessage(Component.literal("Axoluthu recognizes you as her companion. Use /tota axoluthu mode <stay|follow|free>."), false);
        } else if (bondedPlayer.equals(player.getUUID())) {
            player.displayClientMessage(Component.literal("Axoluthu: " + movementMode + " | goal=" + currentGoal
                    + " | /tota axoluthu mode <stay|follow|free>"), false);
        } else {
            player.displayClientMessage(Component.literal("Axoluthu is already bonded to another companion."), false);
        }
        return InteractionResult.CONSUME;
    }

    /** Rare playful follow: uses real navigation and cover, never invisibility or teleportation. */
    private boolean tickShyFollow(ServerLevel level) {
        ServerPlayer owner=level.getServer().getPlayerList().getPlayer(bondedPlayer);
        if(owner==null || owner.level()!=level) {shyFollow=false;return false;}
        long now=level.getGameTime();
        if(!shyFollow && now>=nextShyFollowCheck) {
            nextShyFollowCheck=now+600;
            if(getRandom().nextInt(8)==0 && distanceToSqr(owner)>64 && distanceToSqr(owner)<4096) {
                shyFollow=true;shyFollowUntil=now+400;
            }
        }
        if(!shyFollow)return false;
        if(now>=shyFollowUntil || distanceToSqr(owner)>4096) {shyFollow=false;return false;}
        // Looking toward her? Stop approaching, and try to take cover behind nearby terrain.
        net.minecraft.world.phys.Vec3 toward=position().subtract(owner.position());
        boolean spotted=owner.getLookAngle().dot(toward.normalize())>.65 && distanceToSqr(owner)<256;
        if(spotted) {
            BlockPos cover=null;
            for(net.minecraft.core.Direction dir:net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos test=blockPosition().relative(dir,3);
                BlockPos wall=test.relative(dir.getOpposite());
                if(level.getBlockState(wall).isCollisionShapeFullBlock(level,wall) && level.getBlockState(test).isAir()
                        && level.getBlockState(test.above()).isAir()) {cover=test;break;}
            }
            if(cover!=null)navigateToSavedLocation(cover,1.1);
            else getNavigation().stop();
        } else if(distanceToSqr(owner)>100) {
            getNavigation().moveTo(owner,1.2);
        } else getNavigation().stop();
        chooseGoal("SHY_FOLLOW","pretending not to follow owner");
        return true;
    }
    // One refresh per departure/return cycle. Never clears ownership/protection data.
    private boolean forestryAwayFromHome = false;
    private long nextSaplingStorageAttempt = 0L;
    private void refreshForestryOnHomeReturn(ServerLevel level) {
        if(homePosition==null || !getHomeDimension().equals(level.dimension().location().toString()))return;
        double d=distanceToSqr(homePosition.getX()+.5,homePosition.getY(),homePosition.getZ()+.5);
        if(d>24*24)forestryAwayFromHome=true;
        else if(forestryAwayFromHome && d<12*12) {
            forestryAwayFromHome=false;
            WaifuAxoluthuForestry.resetNavigationMemory(this);
            diagnostic("Forestry","HOME_RETURN_REFRESH temporary tree paths cleared");
            nextSaplingStorageAttempt=0;
        }
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel server)) return;

        if(entityData.get(ACTION)!=ACTION_NONE && tickCount>=actionUntil)finishAction();
        if (WaifuAxoluthuCombat.tick(this, server)) return;
        refreshForestryOnHomeReturn(server);
        refreshBedHome(server);
        if(tickAutonomousBedtime(server))return;
        footprint(server);
        WaifuAxoluthuStorageManager.tickChestAnimation(this);
        setSprinting(!getNavigation().isDone() && (movementMode==MovementMode.FOLLOW
                || manualGoHome || currentGoal.startsWith("RETURN_HOME")
                || currentGoal.equals("BUILD_WORKSHOP")));
        // During a keyed action, evaluate the active task every tick rather than
        // the normal 40-tick planning cadence; this makes 41/24-tick cues exact.
        if(entityData.get(ACTION)!=ACTION_NONE)decisionCooldown=1;
        if(manualGoHome) {
            if(homePosition==null || !getHomeDimension().equals(server.dimension().location().toString())) {
                manualGoHome=false;
            } else if(distanceToSqr(homePosition.getX()+.5,homePosition.getY(),homePosition.getZ()+.5)<=4) {
                getNavigation().stop();
                if(--decisionCooldown<=0) {decisionCooldown=40;chooseGoal("WAIT_AT_HOME","manual recall completed");}
                return;
            } else {
                if(--decisionCooldown<=0) {
                    decisionCooldown=20;
                    chooseGoal("RETURN_HOME","manual owner recall");
                    navigateToSavedLocation(homePosition,1.35);
                }
                return;
            }
        }
        if (movementMode != MovementMode.FREE || manualGoHome) shyFollow=false;
        // COOPERATIVE CONSTRUCTION: a bonded Axoluthu never starts autonomous
        // workshop, resource, site-search, or village planning from this tick.
        // Navigation is cheap and only refreshed once per 20 ticks.
        if (bondedPlayer != null) {
            if (tickCount % 20 == 0) {
                ServerPlayer owner = server.getServer().getPlayerList().getPlayer(bondedPlayer);
                if (owner != null) AbyssalCompanionRegistry.checkAxoluthuLoaded(owner, this);
                if (isRemoved()) return;
            }
            if (movementMode == MovementMode.FREE && !getArchitecturalLearning().getCompound("phase35OwnerSuggestion").getBoolean("pending") && WaifuAxoluthuMaintenance.step(this,server)) return;
            if (movementMode == MovementMode.FOLLOW && tickCount % 20 == 0) {
                ServerPlayer companion = server.getServer().getPlayerList().getPlayer(bondedPlayer);
                if (companion != null && companion.level() == level()) {
                    if (distanceToSqr(companion) > 16.0)
                        getNavigation().moveTo(companion, distanceToSqr(companion) > 144 ? 1.35 : 1.1);
                    else getNavigation().stop();
                }
            }
            return;
        }
        // The exact-site builder is scheduled by the owner's tick. It owns
        // construction exclusively while an approved or reviewable job exists.
        // Do not run background architecture scans, resource hunts, or competing
        // settlement/workshop planners while she is reviewing/building/paused.
        WaifuAxoluthuTransmission.Gear transmissionGear = WaifuAxoluthuTransmission.gear(this);
        if (transmissionGear != WaifuAxoluthuTransmission.Gear.IDLE) {
            if (decisionCooldown > 20) decisionCooldown = 20;
            if (--decisionCooldown <= 0) {
                decisionCooldown = 20;
                // The owner project owns expensive planning, not the entity's
                // saved movement goal. Do not overwrite it on every tick.
            }
            return;
        }
        if (--decisionCooldown > 0) return;
        decisionCooldown = 40; // decisions at most every two seconds
        goalTicks += 40;
        casualComment(server);
        // Passive, read-only architecture recognition; never auto-authorizes storage.
        if(bondedPlayer!=null && movementMode==MovementMode.FREE
                && server.getGameTime()-lastAutomaticArchitectureScan>=600) {
            lastAutomaticArchitectureScan=server.getGameTime();
            recordBuildingScan(WaifuAxoluthuArchitecture.scan(this));
        }
        // Storage takes precedence over construction only while a viable deposit
        // interaction is in progress. Missing/full storage never deadlocks building.
        if(movementMode==MovementMode.FREE && bondedPlayer!=null
                && server.getGameTime()>=nextSaplingStorageAttempt) {
            boolean hasSaplings=false;
            var bag=getAcquiredMaterials();
            for(int i=0;i<bag.getContainerSize();i++)
                if(bag.getItem(i).is(net.minecraft.tags.ItemTags.SAPLINGS)) {hasSaplings=true;break;}
            if(hasSaplings && !getAuthorizedContainers().isEmpty()) {
                var result=WaifuAxoluthuStorageManager.depositSaplings(this);
                if(result.moved()) {
                    speak("saplings_stored","One tree replaced! I'll save the rest for later! :D");
                    nextSaplingStorageAttempt=server.getGameTime()+200;
                    return;
                }
                if(result.detail().startsWith("APPROACH_") || result.detail().startsWith("OPENING_")) {
                    nextSaplingStorageAttempt=server.getGameTime()+40;
                    return;
                }
            }
            nextSaplingStorageAttempt=server.getGameTime()+200;
        }
        if(pendingProjectDeposit && movementMode==MovementMode.FREE) {
            updateNavigationOwner(NavigationOwner.STORAGE,"DELIVER_FINISHED_PROJECT");
            var delivery=WaifuAxoluthuStorageManager.deposit(this,true);
            pistonProjectStatus="DELIVERING_"+delivery.detail();
            if(delivery.completed()) {
                pendingProjectDeposit=false;
                pistonProjectStatus="CRAFTED_PISTON_STORED";
                speak("piston_delivered","Tadaaa! I put my piston and all my leftover supplies in our chest! :D");
            } else if(delivery.detail().startsWith("AUTHORIZED_STORAGE_FULL")
                    || delivery.detail().startsWith("NO_AUTHORIZED_STORAGE")) {
                speak("piston_storage_wait","My piston is ready, but I need room in an approved chest! QwQ");
            }
            return;
        }
        if(pistonIdea && movementMode==MovementMode.FREE) {
            updateNavigationOwner(NavigationOwner.WORKSHOP,"PISTON_PROJECT");
            String status=WaifuAxoluthuPistonProject.step(this,lastMagicGatherTick);
            if(!status.equals(pistonProjectStatus)) {
                pistonProjectStatus=status;
                if(status.startsWith("NEED_") || status.startsWith("APPROACH_") || status.startsWith("SMELT"))
                    speak("piston_"+status,"For my piston: "+status.replace('_',' ').toLowerCase()+"!");
            }
            if(status.endsWith("GATHERED"))lastMagicGatherTick=server.getGameTime();
            if(status.equals("CRAFTED_PISTON")) {
                pistonIdea=false;
                pendingProjectDeposit=true;
                speak("piston_done","Yippee! I made my piston! Time to put everything away! :D");
            }
            return;
        }

        // Night interrupts autonomous projects without discarding their saved progress.
        if(movementMode==MovementMode.FREE && bondedPlayer!=null && !server.isDay()) {
            if(homePosition!=null && getHomeDimension().equals(server.dimension().location().toString())) {
                chooseGoal("RETURN_HOME_FOR_NIGHT","nighttime safety routine");
                if(distanceToSqr(homePosition.getX()+.5,homePosition.getY(),homePosition.getZ()+.5)>9) {
                    navigateToSavedLocation(homePosition,1.25);
                } else {
                    getNavigation().stop();
                    chooseGoal("WAITING_FOR_DAWN","at home; bed support follows after safe bed integration");
                }
                return;
            }
        }
        // Stage 4: one fully autonomous, persistent starter workshop per companion.
        // Piston delivery and explicit player movement modes always take precedence.
        if (bondedPlayer!=null && movementMode==MovementMode.FREE && !stage4WorkshopComplete) {
            updateNavigationOwner(NavigationOwner.WORKSHOP,"STAGE4_AUTONOMOUS_BUILD");
            // Keep the visible brain goal synchronized with the already-running
            // construction scheduler. Combat remains in the entity's normal AI.
            chooseGoal("BUILD_WORKSHOP", "unfinished Stage 4 workshop "
                    + stage4WorkshopStep + "/58");
            String result=WaifuAxoluthuConstruction.step(this,lastMagicGatherTick);
            if(!result.equals(stage4WorkshopStatus)) {
                stage4WorkshopStatus=result;
                diagnostic("Stage4",result+" step="+stage4WorkshopStep);
            }
            if(result.equals("COMPLETE")) {
                completeGoal("Stage 4 workshop finished");
                updateNavigationOwner(NavigationOwner.NONE,"IDLE");
                chooseGoal("OBSERVE", "Stage 4 workshop completed");
                speak("stage4_complete","I built us a little workshop! All by myself! :D");
            } else if(result.startsWith("SITE_FOUND")) {
                speak("stage4_site","Oooo! I found a spot for a workshop! Time to gather wood! :D");
            }
            return;
        }
        if (bondedPlayer!=null && movementMode==MovementMode.FREE
                && stage4WorkshopComplete && !settlementStorageComplete) {
            chooseGoal("BUILD_STORAGE_HUT", "settlement storage "+settlementStorageStep);
            String result=WaifuAxoluthuSettlement.step(this,lastMagicGatherTick);
            if(!result.equals(settlementStatus)) {
                settlementStatus=result;
                diagnostic("Settlement",result+" step="+settlementStorageStep);
            }
            if(result.equals("COMPLETE")) {
                speak("storage_hut_done","I built a storage hut! Hmph! It wasn't that hard! >:3");
            }
            return;
        }
        // Stage 5: the saved, owner-approved house project must be driven by the
        // autonomous scheduler. A prior regression left Stage5Build.step() orphaned,
        // so the normal explore/observe planner silently took over instead.
        // Phase 3.5: independent proposals, never independent authorization.
        if (!level().isClientSide && tickCount % 100 == 0)
            WaifuAxoluthuAutonomousProposals.tick(this);

        // Keep the saved blueprint, cursor, materials and authorization unchanged.
        if (bondedPlayer != null && movementMode == MovementMode.FREE
                && stage4WorkshopComplete && settlementStorageComplete
                && architecturalLearning.contains("stage5HousingProject", 10)) {
            net.minecraft.nbt.CompoundTag house = architecturalLearning.getCompound("stage5HousingProject");
            if (house.getBoolean("siteAuthorized")
                    && house.contains("blueprintPreview", 10)
                    && "BUILDABLE".equals(house.getCompound("blueprintPreview").getString("status"))
                    && !"HOUSE_COMPLETE".equals(house.getString("phase"))
                    && !"COMPLETE".equals(house.getString("constructionPhase"))) {
                updateNavigationOwner(NavigationOwner.WORKSHOP, "STAGE5_HOUSE");
                chooseGoal("BUILD_HOUSE", "resuming approved Stage 5 house");
                String houseResult = WaifuAxoluthuStage5Build.step(this, server, lastMagicGatherTick);
                // Log exceptional outcomes, not every individual poke or placement.
                if (houseResult.startsWith("BLOCKED_") || houseResult.equals("INVALID_BLUEPRINT")
                        || houseResult.equals("PLACEMENT_FAILED")) {
                    if (server.getGameTime() % 100 == 0)
                        diagnostic("Stage5", houseResult + " phase=" + house.getString("constructionPhase"));
                }
                if (houseResult.equals("COMPLETE")) {
                    completeGoal("Stage 5 house finished");
                    updateNavigationOwner(NavigationOwner.NONE, "IDLE");
                    speak("stage5_house_complete", "My house is finished! Come see! :D");
                }
                return;
            }
        }
        if (bondedPlayer!=null && movementMode==MovementMode.FREE
                && stage4WorkshopComplete && settlementStorageComplete
                && !settlementFurnished) {
            String result=WaifuAxoluthuFurnishing.step(this);
            if(result.equals("FURNISHED"))settlementFurnished=true;
            if(!result.equals(furnishingStatus)) {
                furnishingStatus=result;
                diagnostic("Furnishing",result);
            }
            // No bed available is not a reason to stop all autonomous behavior.
            if(!result.equals("WAITING_FOR_OWN_WHITE_BED") && !result.equals("BED_SPACE_OCCUPIED"))return;
        }
        if (bondedPlayer!=null && movementMode==MovementMode.FREE
                && stage4WorkshopComplete && settlementStorageComplete
                && tickShyFollow(server)) return;
        if(stage4WorkshopComplete && navigationOwner==NavigationOwner.WORKSHOP
                && constructionPhase.equals("STAGE4_AUTONOMOUS_BUILD"))
            updateNavigationOwner(NavigationOwner.NONE,"IDLE");
        // FREE mode needs no separate build/acquisition command. Legacy toggles
        // remain for backward compatibility but no longer gate independent work.
        if(autonomousStage3 && movementMode==MovementMode.FREE && bondedPlayer!=null) {
            autoBuildEnabled=true;
            autoAcquireEnabled=true;
            if(selectedDesire!=WaifuAxoluthuTaskPlanner.Desire.IMPROVE_WORKSHOP) {
                selectedDesire=WaifuAxoluthuTaskPlanner.Desire.IMPROVE_WORKSHOP;
                taskPlanSummary=WaifuAxoluthuTaskPlanner.create(selectedDesire).summary();
                taskPlanRevisions++;
            }
            if(!workshopAnnounced && WaifuAxoluthuWorkshopActions.findSmithingTable(this)==null) {
                speak("workshop_start","Hmm... I should get my workshop ready! :D");
                workshopAnnounced=true;
            }
        }
        // Demand-driven fallback: obtain raw iron only when the workshop needs
        // iron and authorized inventories cannot supply it. No idle farming.
        if(autonomousStage3 && movementMode==MovementMode.FREE && bondedPlayer!=null
                && WaifuAxoluthuWorkshopActions.findSmithingTable(this)==null
                && WaifuAxoluthuWorkshopActions.count(acquiredMaterials,"iron")<2
                && WaifuAxoluthuMagicGathering.count(acquiredMaterials,net.minecraft.world.item.Items.RAW_IRON)<2) {
            var station=WaifuAxoluthuMagicGathering.find(this,net.minecraft.world.level.block.Blocks.GRINDSTONE);
            if(station!=null) {
                var result=WaifuAxoluthuMagicGathering.gather(this,net.minecraft.world.item.Items.RAW_IRON,
                        lastMagicGatherTick,160);
                magicGatherStatus=result.status();
                if(result.completed()) {
                    lastMagicGatherTick=server.getGameTime();
                    speak("magic_iron_"+(lastMagicGatherTick/1200),"Pwoof! I've mined some raw iron! :D");
                }
            }
        }
        if(autonomousStage3 && movementMode==MovementMode.FREE && bondedPlayer!=null
                && WaifuAxoluthuWorkshopActions.findSmithingTable(this)==null
                && WaifuAxoluthuWorkshopActions.count(acquiredMaterials,"iron")<2
                && WaifuAxoluthuMagicGathering.count(acquiredMaterials,net.minecraft.world.item.Items.RAW_IRON)>0) {
            String smelt=WaifuAxoluthuFurnaceActions.process(this);
            if(smelt.equals("LOADED_RAW_IRON") || smelt.equals("FUELED_WITH_PLANK")
                    || smelt.equals("COLLECTED_INGOTS"))
                speak("furnace_"+smelt,"I'm working on the iron at the furnace! :D");
        }
        boolean activeWorkshop = autoBuildEnabled
                && selectedDesire == WaifuAxoluthuTaskPlanner.Desire.IMPROVE_WORKSHOP
                && workshopPlacements == 0
                && WaifuAxoluthuWorkshopActions.findSmithingTable(this)==null;
        // Storage gets exclusive navigation only when no unfinished workshop
        // needs control. Never touch unapproved containers.
        if (movementMode == MovementMode.FREE && !activeWorkshop
                && WaifuAxoluthuStorageManager.inventoryCrowded(this)
                && !authorizedContainers.isEmpty()) {
            updateNavigationOwner(NavigationOwner.STORAGE, "DEPOSIT_SURPLUS");
            var deposit = WaifuAxoluthuStorageManager.deposit(this);
            if (!deposit.completed()) {
                if (deposit.detail().startsWith("NO_AUTHORIZED") && server.getGameTime()%1200<40)
                    speak("storage_blocked", "My bag is full, but I can't find approved storage with room! QwQ");
                return;
            }
        }
        if (movementMode != MovementMode.FREE) {
            updateNavigationOwner(NavigationOwner.PLAYER,
                    movementMode == MovementMode.STAY ? "STAY_OVERRIDE" : "FOLLOW_OVERRIDE");
        } else if (activeWorkshop) {
            String phase = pendingWorkshopPosition != null
                    ? "APPROACH_PLACEMENT"
                    : WaifuAxoluthuWorkshopActions.findCraftingTable(this) == null
                        ? "PREPARE_CRAFTING_TABLE"
                        : WaifuAxoluthuWorkshopActions.isNearCraftingTable(this)
                            ? "CRAFT_OR_PLACE" : "APPROACH_CRAFTING_TABLE";
            updateNavigationOwner(NavigationOwner.WORKSHOP, phase);
        } else {
            updateNavigationOwner(NavigationOwner.NONE, "IDLE");
        }
        if (autoBuildEnabled && movementMode == MovementMode.FREE
                && selectedDesire == WaifuAxoluthuTaskPlanner.Desire.IMPROVE_WORKSHOP
                && bondedPlayer != null && --workshopActionCooldown <= 0) {
            workshopActionCooldown = 2;
            ServerPlayer companion = server.getServer().getPlayerList().getPlayer(bondedPlayer);
            if (companion == null || companion.level() != level()) {
                reportWorkshopFailure("PAUSED: bonded companion unavailable");
            } else if (workshopPlacements == 0
                    && WaifuAxoluthuWorkshopActions.findSmithingTable(this)==null) {
                var table = WaifuAxoluthuWorkshopActions.findCraftingTable(this);
                if (table == null) {
                    if (!WaifuAxoluthuWorkshopActions.hasCraftingTableItem(this)) {
                        var result = WaifuAxoluthuWorkshopActions.craftCraftingTable(this);
                        if (!result.success()) reportWorkshopFailure(result.detail());
                    }
                    if (WaifuAxoluthuWorkshopActions.hasCraftingTableItem(this)) {
                        var result = WaifuAxoluthuWorkshopActions.placeCraftingTable(this);
                        if (!result.success()) reportWorkshopFailure(result.detail());
                    }
                } else if (!WaifuAxoluthuWorkshopActions.isNearCraftingTable(this)) {
                    chooseGoal("APPROACH_CRAFTING_TABLE", "3x3 recipe requires physical workstation access");
                    if (!WaifuAxoluthuWorkshopActions.approachCraftingTable(this,table)
                            && ++workstationPathWarnings % 5 == 1)
                        reportWorkshopFailure("PATH_TO_CRAFTING_TABLE_FAILED at "+table.toShortString());
                } else {
                    if (!WaifuAxoluthuWorkshopActions.hasSmithingTableItem(this)) {
                        var result = WaifuAxoluthuWorkshopActions.craft(this);
                        if (!result.success()) reportWorkshopFailure(result.detail());
                    }
                    if (WaifuAxoluthuWorkshopActions.hasSmithingTableItem(this)) {
                        var result = WaifuAxoluthuWorkshopActions.place(this);
                        if (!result.success()) reportWorkshopFailure(result.detail());
                    }
                }
            }
        }
        if (autoAcquireEnabled && movementMode == MovementMode.FREE
                && selectedDesire == WaifuAxoluthuTaskPlanner.Desire.IMPROVE_WORKSHOP
                && bondedPlayer != null && --acquisitionCooldown <= 0) {
            acquisitionCooldown = 3;
            ServerPlayer companion = server.getServer().getPlayerList().getPlayer(bondedPlayer);
            if (companion == null || companion.level() != level()) {
                updateAcquisitionStatus("PAUSED: companion offline or in another dimension");
            } else {
                updateAcquisitionStatus(WaifuAxoluthuResourceAcquisition.fulfillWorkshop(this, companion));
            }
        }
        if (movementMode == MovementMode.STAY) {
            chooseGoal("WAIT_AT_ANCHOR", "stay mode active");
            if (stayAnchor == null) stayAnchor = blockPosition();
            if (homePosition == null) {
                homePosition = stayAnchor;
                diagnostic("Memory", "initialized missing home from Stay anchor "
                        + homePosition.toShortString());
            }
            if (distanceToSqr(stayAnchor.getX()+0.5, stayAnchor.getY(), stayAnchor.getZ()+0.5) > 9) {
                getNavigation().moveTo(stayAnchor.getX()+0.5, stayAnchor.getY(), stayAnchor.getZ()+0.5, 0.8);
            } else getNavigation().stop();
        } else if (movementMode == MovementMode.FOLLOW) {
            chooseGoal("FOLLOW_COMPANION", "follow mode active");
            ServerPlayer companion = bondedPlayer == null ? null : server.getServer().getPlayerList().getPlayer(bondedPlayer);
            if (companion == null || companion.level() != level()) {
                getNavigation().stop();
                if (--navigationWarningCooldown <= 0) {
                    diagnostic("Navigation", "FOLLOW paused: bonded player offline or in another dimension");
                    navigationWarningCooldown = 150;
                }
            } else if (distanceToSqr(companion) > 16) {
                if (!getNavigation().moveTo(companion, distanceToSqr(companion)>400?1.55:distanceToSqr(companion)>64?1.30:1.0) && --navigationWarningCooldown <= 0) {
                    diagnostic("Navigation", "Unable to path to companion; will retry");
                    navigationWarningCooldown = 150;
                }
            } else getNavigation().stop();
        } else {
            // Exclusive ownership: no exploration, observation or return-home
            // movement may replace a workshop navigation target.
            if (navigationOwner == NavigationOwner.WORKSHOP || navigationOwner == NavigationOwner.STORAGE) {
                chooseGoal("WORKSHOP_"+constructionPhase, "exclusive navigation ownership");
                return;
            }
            if (homePosition == null) homePosition = blockPosition();
            // Stage 1C: bounded, deterministic priority arbitration. No inventory or
            // block changes are performed by this planner.
            double homeDistance = Math.sqrt(distanceToSqr(homePosition.getX() + .5,
                    homePosition.getY(), homePosition.getZ() + .5));
            int homeScore = homeDistance > 24 ? 100 : 0;
            int observeScore = currentGoal.equals("EXPLORE") && getNavigation().isDone()
                    && goalTicks >= 40 ? 70 : 0;
            int exploreScore = currentGoal.equals("OBSERVE") && goalTicks < 120 ? 0 : 50;
            plannerCycles++;
            lastPriorityScores = "RETURN_HOME=" + homeScore + " OBSERVE=" + observeScore
                    + " EXPLORE=" + exploreScore + " distanceHome="
                    + String.format(java.util.Locale.ROOT, "%.1f", homeDistance);

            if (homeScore > 0) {
                chooseGoal("RETURN_HOME", "outside 24-block home radius; " + lastPriorityScores);
                if (getNavigation().isDone()) {
                    if (!getNavigation().moveTo(homePosition.getX() + .5, homePosition.getY(),
                            homePosition.getZ() + .5, .85)) {
                        failedPaths++;
                        if (--navigationWarningCooldown <= 0) {
                            diagnostic("Navigation", "return home path failed; failures=" + failedPaths);
                            navigationWarningCooldown = 10;
                        }
                    }
                }
            } else if (currentGoal.equals("RETURN_HOME")) {
                completeGoal("returned within home radius");
                chooseGoal("OBSERVE", "home reached; " + lastPriorityScores);
                getNavigation().stop();
            } else if (observeScore > exploreScore && observeScore > 0) {
                explorationCount++;
                completeGoal("exploration navigation finished");
                chooseGoal("OBSERVE", "observe after exploration; " + lastPriorityScores);
            } else if (currentGoal.equals("OBSERVE") && goalTicks < 120) {
                getNavigation().stop();
            } else {
                if (!currentGoal.equals("EXPLORE")) {
                    if (currentGoal.equals("OBSERVE")) {
                        reflectionCount++;
                        completeGoal("observation finished");
                        diagnostic("Memory", "reflection=" + reflectionCount
                                + " explorations=" + explorationCount);
                    }
                    chooseGoal("EXPLORE", "priority arbitration; " + lastPriorityScores);
                }
                if (getNavigation().isDone()) {
                    net.minecraft.world.phys.Vec3 target =
                            net.minecraft.world.entity.ai.util.DefaultRandomPos.getPos(this, 10, 5);
                    if (target != null) {
                        if (getNavigation().moveTo(target.x, target.y, target.z, .75))
                            diagnostic("Navigation", "explore target="
                                    + BlockPos.containing(target).toShortString());
                        else {
                            failedPaths++;
                            if (--navigationWarningCooldown <= 0) {
                                diagnostic("Navigation", "explore path failed; failures=" + failedPaths);
                                navigationWarningCooldown = 10;
                            }
                        }
                    } else if (--navigationWarningCooldown <= 0) {
                        diagnostic("Planner", "no suitable exploration target; waiting");
                        navigationWarningCooldown = 10;
                    }
                }
            }
        }
    }

    // Remember successfully crafted item types across world reloads.
    private CompoundTag learnedCraftingOutputs = new CompoundTag();
    public boolean hasCraftedBefore(net.minecraft.world.item.Item item) {
        return learnedCraftingOutputs.getBoolean(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString());
    }
    public void rememberCrafted(net.minecraft.world.item.Item item) {
        learnedCraftingOutputs.putBoolean(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString(), true);
    }

    private CompoundTag architecturalLearning = new CompoundTag();
    public CompoundTag getArchitecturalLearning() { return architecturalLearning; }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("WaifuAxoluthuArchitectureLessons", architecturalLearning.copy());
        tag.put("WaifuAxoluthuLearnedCrafts", learnedCraftingOutputs.copy());
        tag.putString("WaifuAxoluthuMode", movementMode.name());
        if(settlementStorageOrigin!=null)tag.putLong("WaifuSettlementStorageOrigin",settlementStorageOrigin.asLong());
        tag.putInt("WaifuSettlementStorageStep",settlementStorageStep);
        tag.putBoolean("WaifuSettlementStorageDone",settlementStorageComplete);
        tag.putBoolean("WaifuSettlementFurnished",settlementFurnished);
        tag.putString("WaifuFurnishingStatus",furnishingStatus);
        tag.putString("WaifuSettlementStatus",settlementStatus);
        tag.putBoolean("WaifuAxoluthuStage4Done",stage4WorkshopComplete);
        if(stage4WorkshopOrigin!=null) tag.putLong("WaifuAxoluthuStage4Origin",stage4WorkshopOrigin.asLong());
        tag.putInt("WaifuAxoluthuStage4Step",stage4WorkshopStep);
        tag.putString("WaifuAxoluthuStage4Status",stage4WorkshopStatus);
        tag.putBoolean("WaifuAxoluthuPistonIdea",pistonIdea);
        tag.putBoolean("WaifuAxoluthuPendingProjectDeposit",pendingProjectDeposit);
        tag.putString("WaifuAxoluthuPistonStatus",pistonProjectStatus);
        tag.putBoolean("WaifuAxoluthuAutonomous3",autonomousStage3);
        tag.putBoolean("WaifuAxoluthuWorkshopAnnounced",workshopAnnounced);
        tag.putLong("WaifuAxoluthuLastSpeech",lastSpeechTick);
        tag.putLong("WaifuAxoluthuLastCasual",lastCasualTick);
        tag.putString("WaifuAxoluthuLastSpeechKey",lastSpeechKey);
        tag.putString("WaifuAxoluthuNavOwner", navigationOwner.name());
        tag.putString("WaifuAxoluthuConstructionPhase", constructionPhase);
        tag.putInt("WaifuAxoluthuBuildingScans", buildingScanCount);
        tag.putLong("WaifuAxoluthuLastMagicGatherTick",lastMagicGatherTick);
        tag.putString("WaifuAxoluthuMagicGatherStatus",magicGatherStatus);
        tag.putString("WaifuAxoluthuLastBuildingScan", lastBuildingScan);
        tag.putString("WaifuAxoluthuResourceRoute", resourceRoute);
        tag.putString("WaifuAxoluthuGoal", currentGoal);
        tag.putLong("WaifuAxoluthuDecisions", decisionsMade);
        tag.putLong("WaifuAxoluthuCompleted", goalsCompleted);
        tag.putInt("WaifuAxoluthuExplorations", explorationCount);
        tag.putInt("WaifuAxoluthuReflections", reflectionCount);
        tag.putInt("WaifuAxoluthuResourceScans", resourceScanCount);
        tag.putInt("WaifuAxoluthuAcquisitions", acquisitions);
        tag.putBoolean("WaifuAxoluthuAutoAcquire", autoAcquireEnabled);
        tag.putBoolean("WaifuAxoluthuAutoBuild", autoBuildEnabled);
        if(pendingWorkshopPosition!=null) {
            tag.putLong("WaifuAxoluthuPendingBuildPos",pendingWorkshopPosition.asLong());
            tag.putString("WaifuAxoluthuPendingBuildType",pendingWorkshopType);
        }
        tag.putInt("WaifuAxoluthuWorkshopCrafts", workshopCrafts);
        tag.putInt("WaifuAxoluthuWorkshopPlacements", workshopPlacements);
        tag.putString("WaifuAxoluthuWorkshopStatus", workshopStatus);
        if (lastWorkshopPlacement != null)
            tag.putLong("WaifuAxoluthuLastWorkshopPos", lastWorkshopPlacement.asLong());
        tag.putString("WaifuAxoluthuAcquireStatus", acquisitionStatus);
        tag.putLongArray("WaifuAxoluthuAllowedContainers",
                authorizedContainers.stream().mapToLong(Long::longValue).toArray());
        tag.putString("WaifuAxoluthuLastAcquisition", lastAcquisition);
        net.minecraft.nbt.ListTag held = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < acquiredMaterials.getContainerSize(); i++) {
            var stack = acquiredMaterials.getItem(i);
            if (stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putByte("Slot", (byte)i);
            entry.put("Stack", stack.save(registryAccess()));
            held.add(entry);
        }
        tag.put("WaifuAxoluthuHeldResources", held);
        tag.putString("WaifuAxoluthuResourceResult", lastResourceScan);
        tag.putInt("WaifuAxoluthuPlannerCycles", plannerCycles);
        tag.putInt("WaifuAxoluthuFailedPaths", failedPaths);
        tag.putString("WaifuAxoluthuScores", lastPriorityScores);
        tag.putString("WaifuAxoluthuDesire", selectedDesire.name());
        tag.putString("WaifuAxoluthuTaskPlan", taskPlanSummary);
        tag.putInt("WaifuAxoluthuTaskPlanRevisions", taskPlanRevisions);
        tag.putInt("WaifuAxoluthuGoalTicks", goalTicks);
        tag.putString("WaifuAxoluthuPreviousGoal", previousGoal);
        tag.putString("WaifuAxoluthuGoalReason", lastGoalReason);
        tag.putBoolean("WaifuAxoluthuManualGoHome",manualGoHome);
        tag.putBoolean("WaifuAxoluthuManualHome", manuallyAssignedHome);
        tag.putString("WaifuAxoluthuHomeDimension", assignedHomeDimension);
        if (registeredBed != null) tag.putLong("WaifuAxoluthuRegisteredBed", registeredBed.asLong());
        if (homePosition != null) {
            tag.putLong("WaifuAxoluthuHome", homePosition.asLong());
        }
        if (bondedPlayer != null) tag.putUUID("WaifuAxoluthuBond", bondedPlayer);
        if (stayAnchor != null) {
            tag.putInt("WaifuAxoluthuAnchorX", stayAnchor.getX());
            tag.putInt("WaifuAxoluthuAnchorY", stayAnchor.getY());
            tag.putInt("WaifuAxoluthuAnchorZ", stayAnchor.getZ());
            tag.putBoolean("WaifuAxoluthuHasAnchor", true);
        }
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        architecturalLearning = tag.contains("WaifuAxoluthuArchitectureLessons",10)
                ? tag.getCompound("WaifuAxoluthuArchitectureLessons").copy() : new CompoundTag();
        learnedCraftingOutputs = tag.contains("WaifuAxoluthuLearnedCrafts", 10)
                ? tag.getCompound("WaifuAxoluthuLearnedCrafts").copy() : new CompoundTag();
        manualGoHome=tag.getBoolean("WaifuAxoluthuManualGoHome");
        settlementStorageOrigin=tag.contains("WaifuSettlementStorageOrigin")
                ?BlockPos.of(tag.getLong("WaifuSettlementStorageOrigin")):null;
        settlementStorageStep=Math.max(0,tag.getInt("WaifuSettlementStorageStep"));
        settlementStorageComplete=tag.getBoolean("WaifuSettlementStorageDone");
        settlementFurnished=tag.getBoolean("WaifuSettlementFurnished");
        furnishingStatus=tag.contains("WaifuFurnishingStatus")?tag.getString("WaifuFurnishingStatus"):"NOT_STARTED";
        settlementStatus=tag.contains("WaifuSettlementStatus")?tag.getString("WaifuSettlementStatus"):"NOT_STARTED";
        stage4WorkshopComplete=tag.getBoolean("WaifuAxoluthuStage4Done");
        stage4WorkshopOrigin=tag.contains("WaifuAxoluthuStage4Origin")
                ?BlockPos.of(tag.getLong("WaifuAxoluthuStage4Origin")):null;
        stage4WorkshopStep=tag.getInt("WaifuAxoluthuStage4Step");
        stage4WorkshopStatus=tag.contains("WaifuAxoluthuStage4Status")
                ?tag.getString("WaifuAxoluthuStage4Status"):"UNSTARTED";
        // Clamp corrupt values without discarding a legitimate partially built workshop.
        stage4WorkshopStep=Math.max(0,Math.min(58,stage4WorkshopStep));
        pistonIdea=tag.getBoolean("WaifuAxoluthuPistonIdea");
        pendingProjectDeposit=tag.getBoolean("WaifuAxoluthuPendingProjectDeposit");
        pistonProjectStatus=tag.contains("WaifuAxoluthuPistonStatus")?tag.getString("WaifuAxoluthuPistonStatus"):"IDLE";
        autonomousStage3=!tag.contains("WaifuAxoluthuAutonomous3") || tag.getBoolean("WaifuAxoluthuAutonomous3");
        workshopAnnounced=tag.getBoolean("WaifuAxoluthuWorkshopAnnounced");
        lastSpeechTick=tag.contains("WaifuAxoluthuLastSpeech") ? tag.getLong("WaifuAxoluthuLastSpeech") : -10000;
        lastCasualTick=tag.contains("WaifuAxoluthuLastCasual") ? tag.getLong("WaifuAxoluthuLastCasual") : -10000;
        lastSpeechKey=tag.getString("WaifuAxoluthuLastSpeechKey");
        try { navigationOwner = NavigationOwner.valueOf(tag.getString("WaifuAxoluthuNavOwner")); }
        catch (IllegalArgumentException ex) { navigationOwner = NavigationOwner.NONE; }
        constructionPhase = tag.contains("WaifuAxoluthuConstructionPhase")
                ? tag.getString("WaifuAxoluthuConstructionPhase") : "IDLE";
        buildingScanCount = tag.getInt("WaifuAxoluthuBuildingScans");
        lastMagicGatherTick=tag.getLong("WaifuAxoluthuLastMagicGatherTick");
        magicGatherStatus=tag.contains("WaifuAxoluthuMagicGatherStatus")
                ?tag.getString("WaifuAxoluthuMagicGatherStatus"):"idle";
        lastBuildingScan = tag.contains("WaifuAxoluthuLastBuildingScan")
                ? tag.getString("WaifuAxoluthuLastBuildingScan") : "not scanned";
        resourceRoute = tag.contains("WaifuAxoluthuResourceRoute")
                ? tag.getString("WaifuAxoluthuResourceRoute") : "none";
        if (tag.hasUUID("WaifuAxoluthuBond")) bondedPlayer = tag.getUUID("WaifuAxoluthuBond");
        // Upgrade previously bonded Axoluthus without resetting saved movement commands.
        if (bondedPlayer != null) {
            addTag("tamed");
            getPersistentData().putBoolean("tota_axoluthu_tamed", true);
        }
        try { movementMode = MovementMode.valueOf(tag.getString("WaifuAxoluthuMode")); }
        catch (IllegalArgumentException ignored) { movementMode = MovementMode.FREE; }
        // Preserve the saved movement mode. FOLLOW is initialized only on first bond in mobInteract.
        currentGoal = tag.contains("WaifuAxoluthuGoal") ? tag.getString("WaifuAxoluthuGoal") : "EXPLORE";
        decisionsMade = tag.getLong("WaifuAxoluthuDecisions");
        goalsCompleted = tag.getLong("WaifuAxoluthuCompleted");
        explorationCount = tag.getInt("WaifuAxoluthuExplorations");
        reflectionCount = tag.getInt("WaifuAxoluthuReflections");
        resourceScanCount = tag.getInt("WaifuAxoluthuResourceScans");
        acquisitions = tag.getInt("WaifuAxoluthuAcquisitions");
        autoAcquireEnabled = tag.getBoolean("WaifuAxoluthuAutoAcquire");
        autoBuildEnabled = tag.getBoolean("WaifuAxoluthuAutoBuild");
        pendingWorkshopPosition=tag.contains("WaifuAxoluthuPendingBuildPos")
                ? BlockPos.of(tag.getLong("WaifuAxoluthuPendingBuildPos")) : null;
        pendingWorkshopType=tag.getString("WaifuAxoluthuPendingBuildType");
        workshopCrafts = tag.getInt("WaifuAxoluthuWorkshopCrafts");
        workshopPlacements = tag.getInt("WaifuAxoluthuWorkshopPlacements");
        workshopStatus = tag.contains("WaifuAxoluthuWorkshopStatus")
                ? tag.getString("WaifuAxoluthuWorkshopStatus") : "not started";
        if (tag.contains("WaifuAxoluthuLastWorkshopPos"))
            lastWorkshopPlacement = BlockPos.of(tag.getLong("WaifuAxoluthuLastWorkshopPos"));
        acquisitionStatus = tag.contains("WaifuAxoluthuAcquireStatus")
                ? tag.getString("WaifuAxoluthuAcquireStatus") : "idle";
        authorizedContainers.clear();
        for (long key : tag.getLongArray("WaifuAxoluthuAllowedContainers"))
            authorizedContainers.add(key);
        // Repair earlier null-home saves, preferring the established Stay anchor.
        if (homePosition == null && tag.contains("WaifuAxoluthuHasAnchor")
                && tag.getBoolean("WaifuAxoluthuHasAnchor"))
            homePosition = new BlockPos(tag.getInt("WaifuAxoluthuAnchorX"),
                    tag.getInt("WaifuAxoluthuAnchorY"), tag.getInt("WaifuAxoluthuAnchorZ"));
        lastAcquisition = tag.contains("WaifuAxoluthuLastAcquisition")
                ? tag.getString("WaifuAxoluthuLastAcquisition") : "none";
        acquiredMaterials.clearContent();
        var held = tag.getList("WaifuAxoluthuHeldResources", 10);
        for (int i = 0; i < held.size(); i++) {
            CompoundTag entry = held.getCompound(i);
            int slot = entry.getByte("Slot") & 255;
            if (slot < acquiredMaterials.getContainerSize())
                acquiredMaterials.setItem(slot, net.minecraft.world.item.ItemStack.parseOptional(
                        registryAccess(), entry.getCompound("Stack")));
        }
        lastResourceScan = tag.contains("WaifuAxoluthuResourceResult")
                ? tag.getString("WaifuAxoluthuResourceResult") : "not scanned";
        plannerCycles = tag.getInt("WaifuAxoluthuPlannerCycles");
        failedPaths = tag.getInt("WaifuAxoluthuFailedPaths");
        lastPriorityScores = tag.contains("WaifuAxoluthuScores")
                ? tag.getString("WaifuAxoluthuScores") : "not evaluated";
        try {
            selectedDesire = WaifuAxoluthuTaskPlanner.Desire.valueOf(tag.getString("WaifuAxoluthuDesire"));
        } catch (IllegalArgumentException ignored) {
            selectedDesire = WaifuAxoluthuTaskPlanner.Desire.EXPLORE;
        }
        taskPlanSummary = tag.contains("WaifuAxoluthuTaskPlan")
                ? tag.getString("WaifuAxoluthuTaskPlan") : "not planned";
        taskPlanRevisions = tag.getInt("WaifuAxoluthuTaskPlanRevisions");
        goalTicks = tag.getInt("WaifuAxoluthuGoalTicks");
        previousGoal = tag.contains("WaifuAxoluthuPreviousGoal") ? tag.getString("WaifuAxoluthuPreviousGoal") : "NONE";
        lastGoalReason = tag.contains("WaifuAxoluthuGoalReason") ? tag.getString("WaifuAxoluthuGoalReason") : "Loaded";
        manuallyAssignedHome = tag.getBoolean("WaifuAxoluthuManualHome");
        assignedHomeDimension = tag.getString("WaifuAxoluthuHomeDimension");
        if (tag.contains("WaifuAxoluthuHome")) homePosition = BlockPos.of(tag.getLong("WaifuAxoluthuHome"));
        registeredBed = tag.contains("WaifuAxoluthuRegisteredBed") ? BlockPos.of(tag.getLong("WaifuAxoluthuRegisteredBed")) : null;
        if (manuallyAssignedHome && !assignedHomeDimension.equals(level().dimension().location().toString())) {
            // Avoid navigating to same-numbered coordinates in the wrong dimension.
            manuallyAssignedHome = false;
            assignedHomeDimension = "";
            homePosition = blockPosition();
        }
        if (homePosition == null) homePosition = stayAnchor != null ? stayAnchor : blockPosition();
        if (tag.getBoolean("WaifuAxoluthuHasAnchor"))
            stayAnchor = new BlockPos(tag.getInt("WaifuAxoluthuAnchorX"), tag.getInt("WaifuAxoluthuAnchorY"), tag.getInt("WaifuAxoluthuAnchorZ"));
        decisionCooldown = 0;
        if (!level().isClientSide) diagnostic("Memory", "Loaded bond=" + bondedPlayer + " mode=" + movementMode + " goal=" + currentGoal);
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "locomotion", 5, state -> {
            int action=entityData.get(ACTION);
            String name=(action==ACTION_FOCUSED || action==ACTION_CONSTRUCTION_POKE)?"animation.waifu_axoluthu.angry"
                    :action==ACTION_WORKSTATION?"animation.waifu_axoluthu.workstation"
                    :action==ACTION_PLANT?"animation.waifu_axoluthu.pickup"
                    :action==ACTION_ATTACK?"animation.waifu_axoluthu.trident_attack"
                    :state.isMoving()?"animation.waifu_axoluthu.walk":"animation.waifu_axoluthu.idle";
            state.getController().setAnimationSpeed((action==ACTION_CONSTRUCTION_POKE || action==ACTION_WORKSTATION || action==ACTION_FOCUSED) ? 2.0 : 1.0);
            state.setAnimation(RawAnimation.begin().thenLoop(name));
            return PlayState.CONTINUE;
        }));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
