package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class WaifuAxoluthuBrainCommands {
    private static WaifuAxoluthuEntity nearest(ServerPlayer player) {
        return player.level().getEntitiesOfClass(WaifuAxoluthuEntity.class,
                player.getBoundingBox().inflate(32), a -> a.isAlive()).stream()
                .min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
    }
    private static int status(net.minecraft.commands.CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        WaifuAxoluthuEntity a = nearest(player);
        if (a == null) { source.sendFailure(Component.literal("[Axoluthu/Brain] No Waifu Axoluthu within 32 blocks.")); return 0; }
        source.sendSuccess(() -> Component.literal("[Axoluthu/Brain] UUID=" + a.getUUID() + " mode=" + a.getMovementMode()
                + " goal=" + a.getCurrentGoal() + " bond=" + a.getBondedPlayer()
                + " anchor=" + a.getStayAnchor() + " home=" + a.getHomePosition()
                + " decisions=" + a.getDecisionsMade() + " completed=" + a.getGoalsCompleted()
                + " explorations=" + a.getExplorationCount() + " reflections=" + a.getReflectionCount()
                + " plannerCycles=" + a.getPlannerCycles() + " failedPaths=" + a.getFailedPaths()
                + " priorities=[" + a.getLastPriorityScores() + "]"
                + " desire=" + a.getSelectedDesire() + " planRevision=" + a.getTaskPlanRevisions()
                + " resourceScans=" + a.getResourceScanCount()
                + " lastResources=" + a.getLastResourceScan()
                + " acquisitions=" + a.getAcquisitions() + " lastAcquisition=" + a.getLastAcquisition()
                + " authorizedContainers=" + a.getAuthorizedContainers().size()
                + " autoAcquire=" + a.isAutoAcquireEnabled()
                + " acquisitionStatus=" + a.getAcquisitionStatus()
                + " autoBuild=" + a.isAutoBuildEnabled()
                + " workshopCrafts=" + a.getWorkshopCrafts()
                + " workshopPlacements=" + a.getWorkshopPlacements()
                + " workshopStatus=" + a.getWorkshopStatus()
                + " lastWorkshopPos=" + a.getLastWorkshopPlacement()
                + " navOwner=" + a.getNavigationOwner()
                + " constructionPhase=" + a.getConstructionPhase()
                + " stage4=[" + a.getStage4WorkshopDiagnostic() + "]"
                + " architectureScans=" + a.getBuildingScanCount()
                + " lastArchitecture=" + a.getLastBuildingScan()
                + " resourceRoute=" + a.getResourceRoute()
                + " pistonIdea=" + a.hasPistonIdea() + " pistonStatus=" + a.getPistonProjectStatus()
                + " taskPlan=" + a.getTaskPlanSummary()
                + " reason=" + a.getLastGoalReason() + " last=" + a.getLastDiagnostic()), false);
        return 1;
    }
    private static int mode(net.minecraft.commands.CommandSourceStack source, WaifuAxoluthuEntity.MovementMode mode)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        WaifuAxoluthuEntity a = nearest(player);
        if (a == null) { source.sendFailure(Component.literal("[Axoluthu/Brain] No Waifu Axoluthu within 32 blocks.")); return 0; }
        if (!a.setMovementMode(mode, player.getUUID())) {
            source.sendFailure(Component.literal("[Axoluthu/Bond] Only her bonded companion can change movement mode. Right-click an unbonded Axoluthu first."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("[Axoluthu/Brain] Movement mode: " + mode), false);
        return 1;
    }
    private static int plan(net.minecraft.commands.CommandSourceStack source,
            WaifuAxoluthuTaskPlanner.Desire desire)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        WaifuAxoluthuEntity a = nearest(player);
        if (a == null) {
            source.sendFailure(Component.literal("[Axoluthu/Planner] No Waifu Axoluthu within 32 blocks."));
            return 0;
        }
        if (!a.planDesire(desire, player.getUUID())) {
            source.sendFailure(Component.literal("[Axoluthu/Planner] Only the bonded companion can request plans."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("[Axoluthu/Planner] PLAN ONLY: "
                + a.getTaskPlanSummary() + " | No materials taken or blocks changed."), false);
        return 1;
    }
    private static int scan(net.minecraft.commands.CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        WaifuAxoluthuEntity a = nearest(player);
        if (a == null) {
            source.sendFailure(Component.literal("[Axoluthu/Storage] No Waifu Axoluthu within 32 blocks."));
            return 0;
        }
        if (!player.getUUID().equals(a.getBondedPlayer())) {
            source.sendFailure(Component.literal("[Axoluthu/Storage] Only her bonded companion can authorize a storage scan."));
            return 0;
        }
        var report = WaifuAxoluthuResourceScanner.scan(a, player, 12);
        a.recordResourceScan(report.summary());
        source.sendSuccess(() -> Component.literal("[Axoluthu/Storage] " + report.summary()), false);
        source.sendSuccess(() -> Component.literal("[Axoluthu/Storage] " + report.details()), false);
        return 1;
    }

    private static int take(net.minecraft.commands.CommandSourceStack source, String material, int quantity)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        WaifuAxoluthuEntity a = nearest(player);
        if (a == null) {
            source.sendFailure(Component.literal("[Axoluthu/Acquisition] No Waifu Axoluthu within 32 blocks."));
            return 0;
        }
        var result = WaifuAxoluthuResourceAcquisition.take(a, player, material, quantity);
        if (!result.success()) {
            source.sendFailure(Component.literal("[Axoluthu/Acquisition] " + result.detail()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("[Axoluthu/Acquisition] " + result.detail()), false);
        return 1;
    }
    private static int carried(net.minecraft.commands.CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        WaifuAxoluthuEntity a = nearest(player);
        if (a == null || !player.getUUID().equals(a.getBondedPlayer())) {
            source.sendFailure(Component.literal("[Axoluthu/Acquisition] No bonded Axoluthu nearby."));
            return 0;
        }
        java.util.List<String> stacks = new java.util.ArrayList<>();
        var inv = a.getAcquiredMaterials();
        for (int i=0; i<inv.getContainerSize(); i++)
            if (!inv.getItem(i).isEmpty())
                stacks.add(i + ":" + inv.getItem(i).getCount() + "x "
                        + inv.getItem(i).getHoverName().getString());
        source.sendSuccess(() -> Component.literal("[Axoluthu/Acquisition] Carried: "
                + (stacks.isEmpty() ? "empty" : String.join(", ", stacks))), false);
        return 1;
    }

    private static int permission(net.minecraft.commands.CommandSourceStack source, boolean allow)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player=source.getPlayerOrException();
        WaifuAxoluthuEntity axo=nearest(player);
        if(axo==null || !player.getUUID().equals(axo.getBondedPlayer())) {
            source.sendFailure(Component.literal("[Axoluthu/Permissions] No bonded Axoluthu nearby."));
            return 0;
        }
        var hit=player.pick(6,0,false);
        if(!(hit instanceof net.minecraft.world.phys.BlockHitResult blockHit)
                || hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK) {
            source.sendFailure(Component.literal("[Axoluthu/Permissions] Look at a container within 6 blocks."));
            return 0;
        }
        var pos=blockHit.getBlockPos();
        if(pos.distSqr(axo.blockPosition())>12*12
                || !(player.level().getBlockEntity(pos) instanceof net.minecraft.world.Container)) {
            source.sendFailure(Component.literal("[Axoluthu/Permissions] Target must be a nearby container."));
            return 0;
        }
        axo.authorizeContainer(pos,player.getUUID(),allow);
        source.sendSuccess(() -> Component.literal("[Axoluthu/Permissions] "
                +(allow?"Authorized ":"Revoked ")+pos.toShortString()),false);
        return 1;
    }
    private static int auto(net.minecraft.commands.CommandSourceStack source, boolean enabled)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player=source.getPlayerOrException();
        WaifuAxoluthuEntity axo=nearest(player);
        if(axo==null || !axo.setAutoAcquire(enabled,player.getUUID())) {
            source.sendFailure(Component.literal("[Axoluthu/Acquisition] Bonded Axoluthu required."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("[Axoluthu/Acquisition] Auto workshop sourcing "
                +(enabled?"ENABLED":"DISABLED")),false);
        return 1;
    }
    private static int acquire(net.minecraft.commands.CommandSourceStack source,String material,int quantity)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player=source.getPlayerOrException();
        WaifuAxoluthuEntity axo=nearest(player);
        if(axo==null) return 0;
        var result=WaifuAxoluthuResourceAcquisition.acquire(axo,player,material,quantity);
        if(!result.success()) {source.sendFailure(Component.literal("[Axoluthu/Acquisition] "+result.detail()));return 0;}
        source.sendSuccess(()->Component.literal("[Axoluthu/Acquisition] "+result.detail()),false);
        return 1;
    }
    private static int workshop(net.minecraft.commands.CommandSourceStack source, String action)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player=source.getPlayerOrException();
        WaifuAxoluthuEntity axo=nearest(player);
        if(axo==null || !player.getUUID().equals(axo.getBondedPlayer())) {
            source.sendFailure(Component.literal("[Axoluthu/Workshop] Bonded Axoluthu required within 32 blocks."));
            return 0;
        }
        if(action.equals("on") || action.equals("off")) {
            boolean enabled=action.equals("on");
            axo.setAutoBuild(enabled,player.getUUID());
            source.sendSuccess(()->Component.literal("[Axoluthu/Workshop] Auto craft+place "
                    +(enabled?"ENABLED":"DISABLED")),false);
            return 1;
        }
        WaifuAxoluthuWorkshopActions.Result result=action.equals("craft")
                ? WaifuAxoluthuWorkshopActions.craft(axo)
                : action.equals("craft_table") ? WaifuAxoluthuWorkshopActions.craftCraftingTable(axo)
                : action.equals("place_table") ? WaifuAxoluthuWorkshopActions.placeCraftingTable(axo)
                : WaifuAxoluthuWorkshopActions.place(axo);
        if(!result.success()) {
            axo.reportWorkshopFailure(result.detail());
            source.sendFailure(Component.literal("[Axoluthu/Workshop] "+result.detail()));
            return 0;
        }
        source.sendSuccess(()->Component.literal("[Axoluthu/Workshop] "+result.detail()),false);
        return 1;
    }
    private static int architecture(net.minecraft.commands.CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player=source.getPlayerOrException();
        WaifuAxoluthuEntity axo=nearest(player);
        if(axo==null || !player.getUUID().equals(axo.getBondedPlayer())) return 0;
        String report=WaifuAxoluthuArchitecture.scan(axo);
        axo.recordBuildingScan(report);
        source.sendSuccess(()->Component.literal("[Axoluthu/Architecture] "+report),false);
        return 1;
    }
    private static int route(net.minecraft.commands.CommandSourceStack source,
                             net.minecraft.world.item.Item item)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player=source.getPlayerOrException();
        WaifuAxoluthuEntity axo=nearest(player);
        if(axo==null || !player.getUUID().equals(axo.getBondedPlayer())) return 0;
        String route=WaifuAxoluthuResourceRoutes.route(item);
        axo.setResourceRoute(item+" -> "+route);
        source.sendSuccess(()->Component.literal("[Axoluthu/GatheringPlan] "+route+" (planning only)"),false);
        return 1;
    }
    private static int testComposter(net.minecraft.commands.CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player=source.getPlayerOrException();
        WaifuAxoluthuEntity axo=nearest(player);
        if(axo==null || !player.getUUID().equals(axo.getBondedPlayer()))return 0;
        String result=WaifuAxoluthuComposterHunt.hunt(axo,
                net.minecraft.world.entity.EntityType.SKELETON,0,160);
        source.sendSuccess(()->Component.literal("[Axoluthu/Composter] "+result),false);
        return 1;
    }
    private static int pistonIdea(net.minecraft.commands.CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player=source.getPlayerOrException();
        WaifuAxoluthuEntity a=nearest(player);
        if(a==null || !a.requestPistonIdea(player.getUUID())) {
            source.sendFailure(Component.literal("[Axoluthu] Bonded Axoluthu required within 32 blocks."));
            return 0;
        }
        source.sendSuccess(()->Component.literal("[Axoluthu] Piston idea queued. Set FREE mode to execute."),false);
        return 1;
    }
    private static WaifuAxoluthuEntity bondedLoaded(ServerPlayer player) {
        for(net.minecraft.server.level.ServerLevel level:player.server.getAllLevels()) {
            for(net.minecraft.world.entity.Entity entity:level.getAllEntities()) {
                if(entity instanceof WaifuAxoluthuEntity axo && axo.isAlive()
                        && player.getUUID().equals(axo.getBondedPlayer()))return axo;
            }
        }
        return null;
    }
    private static int home(net.minecraft.commands.CommandSourceStack source, String action)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        WaifuAxoluthuEntity a = nearest(player);
        if(a==null)a=bondedLoaded(player);
        if (a == null) {
            source.sendFailure(Component.literal("[Axoluthu/Home] No loaded bonded Axoluthu found."));
            return 0;
        }
        if (!player.getUUID().equals(a.getBondedPlayer())) {
            source.sendFailure(Component.literal("[Axoluthu/Home] Only her bonded owner may manage home."));
            return 0;
        }
        if ("ease".equals(action)) {
            if (!a.easeHome(player.getUUID())) {
                source.sendFailure(Component.literal("[Axoluthu/Home] Could not release home recall."));
                return 0;
            }
            source.sendSuccess(() -> Component.literal("[Axoluthu/Home] Home recall released; autonomous activity resumed. :D"), false);
            return 1;
        }
        if("go".equals(action)) {
            if(!a.requestGoHome(player.getUUID())) {
                source.sendFailure(Component.literal("[Axoluthu/Home] Cannot navigate to a home in another dimension."));
                return 0;
            }
            source.sendSuccess(()->Component.literal("[Axoluthu/Home] Returning home; autonomous project saved."),false);
            return 1;
        }
        if ("set".equals(action) && !a.setHomeFromPlayer(player)) return 0;
        if ("clear".equals(action) && !a.clearAssignedHome(player)) return 0;
        final String homeMessage =
                (a.hasManuallyAssignedHome() ? "Assigned" : "Automatic fallback")
                        + " home: " + a.getHomeDimension()
                        + " " + a.getHomePosition().toShortString();

        source.sendSuccess(() -> Component.literal(homeMessage), false);
        return 1;
    }
    private static int teach(net.minecraft.commands.CommandSourceStack source, String action)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player=source.getPlayerOrException();
        WaifuAxoluthuEntity axo=nearest(player);
        if(axo==null)axo=bondedLoaded(player);
        if(axo==null || !player.getUUID().equals(axo.getBondedPlayer())) {
            source.sendFailure(Component.literal("[Axoluthu/Architecture] Loaded bonded Axoluthu required."));
            return 0;
        }
        String result=switch(action) {
            case "baseline" -> WaifuAxoluthuBuildingLessons.baseline(axo, player.blockPosition());
            case "learn" -> WaifuAxoluthuBuildingLessons.learn(axo);
            case "envision" -> WaifuAxoluthuStage5Design.envision(axo);
            case "plan" -> WaifuAxoluthuStage5Journal.plan(axo);
            case "journal" -> WaifuAxoluthuStage5Journal.status(axo);
            case "materials" -> WaifuAxoluthuStage5Journal.materials(axo);
            case "blueprint" -> WaifuAxoluthuStage5Journal.blueprint(axo);
            case "survey" -> WaifuAxoluthuStage5Journal.survey(axo);
            case "propose" -> WaifuAxoluthuStage5SiteApproval.propose(axo,player);
            case "approve" -> WaifuAxoluthuStage5SiteApproval.decide(axo,player,"approve");
            case "reject" -> WaifuAxoluthuStage5SiteApproval.decide(axo,player,"reject");
            case "suggest" -> WaifuAxoluthuStage5SiteApproval.suggest(axo,player);
            case "audit" -> WaifuAxoluthuStage5Journal.inventoryAudit(axo);
            case "book" -> WaifuAxoluthuStage5Journal.giveBook(axo,player);
            default -> WaifuAxoluthuBuildingLessons.status(axo);
        };
        source.sendSuccess(()->Component.literal("[Axoluthu/Architecture] "+result),false);
        return 1;
    }

    /** Owner-only live view of the companion's actual 36-slot backpack. */
    private static int inventory(net.minecraft.commands.CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        WaifuAxoluthuEntity axo = nearest(player);
        if (axo == null || !player.getUUID().equals(axo.getBondedPlayer())) {
            source.sendFailure(Component.literal("[Axoluthu] Your bonded Axoluthu must be within 32 blocks."));
            return 0;
        }
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, playerInventory, ignored) -> new net.minecraft.world.inventory.ChestMenu(
                        net.minecraft.world.inventory.MenuType.GENERIC_9x4,
                        id, playerInventory, axo.getAcquiredMaterials(), 4),
                Component.literal("Axoluthu's Backpack")));
        return 1;
    }

    /** Deposits only the amount accepted by the owner's existing Repository. */
    private static int deposit(net.minecraft.commands.CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        WaifuAxoluthuEntity axo = nearest(player);
        if (axo == null || !player.getUUID().equals(axo.getBondedPlayer())) {
            source.sendFailure(Component.literal("[Axoluthu] Your bonded Axoluthu must be within 32 blocks."));
            return 0;
        }
        var bag = axo.getAcquiredMaterials();
        int deposited = 0;
        for (int slot = 0; slot < bag.getContainerSize(); slot++) {
            var stack = bag.getItem(slot);
            if (stack.isEmpty()) continue;
            int inserted = 0;
            for (int page = 0; page < com.devaxo.templeoftheaxolotl.repository.CelestialRepository.pages(player)
                    && inserted < stack.getCount(); page++) {
                var offered = stack.copy();
                offered.setCount(stack.getCount() - inserted);
                inserted += com.devaxo.templeoftheaxolotl.repository.CelestialRepository.insertPage(player, page, offered);
            }
            if (inserted > 0) {
                bag.removeItem(slot, inserted);
                deposited += inserted;
            }
        }
        bag.setChanged();
        final int total = deposited;
        source.sendSuccess(() -> Component.literal("[Axoluthu] Deposited " + total
                + " items into your shared Repository; any overflow remains in her backpack."), false);
        return 1;
    }

    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tota").then(Commands.literal("axoluthu")
                .then(Commands.literal("inventory").executes(ctx -> inventory(ctx.getSource())))
                .then(Commands.literal("deposit").executes(ctx -> deposit(ctx.getSource())))
                .then(Commands.literal("home")
                        .then(Commands.literal("go").executes(ctx -> home(ctx.getSource(), "go")))
                        .then(Commands.literal("ease").executes(ctx -> home(ctx.getSource(), "ease")))
                        .then(Commands.literal("set").executes(ctx -> home(ctx.getSource(), "set")))
                        .then(Commands.literal("clear").executes(ctx -> home(ctx.getSource(), "clear")))
                        .then(Commands.literal("status").executes(ctx -> home(ctx.getSource(), "status"))))
                .then(Commands.literal("dev")
                        .then(Commands.literal("hunt_skeleton").executes(ctx -> testComposter(ctx.getSource()))))
                .then(Commands.literal("dev").then(Commands.literal("idea")
                        .then(Commands.literal("piston").executes(ctx -> pistonIdea(ctx.getSource())))))
                .then(Commands.literal("debug").then(Commands.literal("status").executes(ctx -> status(ctx.getSource()))))
                .then(Commands.literal("architecture")
                        .then(Commands.literal("scan").executes(ctx -> architecture(ctx.getSource())))
                        .then(Commands.literal("baseline").executes(ctx -> teach(ctx.getSource(),"baseline")))
                        .then(Commands.literal("learn").executes(ctx -> teach(ctx.getSource(),"learn")))
                        .then(Commands.literal("envision").executes(ctx -> teach(ctx.getSource(),"envision")))
                        .then(Commands.literal("plan").executes(ctx -> teach(ctx.getSource(),"plan")))
                        .then(Commands.literal("journal").executes(ctx -> teach(ctx.getSource(),"journal")))
                        .then(Commands.literal("materials").executes(ctx -> teach(ctx.getSource(),"materials")))
                        .then(Commands.literal("blueprint").executes(ctx -> teach(ctx.getSource(),"blueprint")))
                        .then(Commands.literal("survey").executes(ctx -> teach(ctx.getSource(),"survey")))
                        .then(Commands.literal("propose").executes(ctx -> teach(ctx.getSource(),"propose")))
                        .then(Commands.literal("approve").executes(ctx -> teach(ctx.getSource(),"approve")))
                        .then(Commands.literal("reject").executes(ctx -> teach(ctx.getSource(),"reject")))
                        .then(Commands.literal("suggest").executes(ctx -> teach(ctx.getSource(),"suggest")))
                        .then(Commands.literal("audit").executes(ctx -> teach(ctx.getSource(),"audit")))
                        .then(Commands.literal("book").executes(ctx -> teach(ctx.getSource(),"book")))
                        .then(Commands.literal("lessons").executes(ctx -> teach(ctx.getSource(),"lessons"))))
                .then(Commands.literal("gather")
                        .then(Commands.literal("route")
                                .then(Commands.literal("cobblestone").executes(ctx -> route(ctx.getSource(),net.minecraft.world.item.Items.COBBLESTONE)))
                                .then(Commands.literal("oak_log").executes(ctx -> route(ctx.getSource(),net.minecraft.world.item.Items.OAK_LOG)))
                                .then(Commands.literal("bone").executes(ctx -> route(ctx.getSource(),net.minecraft.world.item.Items.BONE)))
                                .then(Commands.literal("sugar_cane").executes(ctx -> route(ctx.getSource(),net.minecraft.world.item.Items.SUGAR_CANE)))))
                .then(Commands.literal("resources")
                        .then(Commands.literal("allow").executes(ctx -> permission(ctx.getSource(),true)))
                        .then(Commands.literal("revoke").executes(ctx -> permission(ctx.getSource(),false)))
                        .then(Commands.literal("auto")
                                .then(Commands.literal("on").executes(ctx -> auto(ctx.getSource(),true)))
                                .then(Commands.literal("off").executes(ctx -> auto(ctx.getSource(),false))))
                        .then(Commands.literal("acquire")
                                .then(Commands.literal("iron")
                                        .then(Commands.argument("amount",IntegerArgumentType.integer(1,64))
                                                .executes(ctx -> acquire(ctx.getSource(),"iron",IntegerArgumentType.getInteger(ctx,"amount")))))
                                .then(Commands.literal("planks")
                                        .then(Commands.argument("amount",IntegerArgumentType.integer(1,64))
                                                .executes(ctx -> acquire(ctx.getSource(),"planks",IntegerArgumentType.getInteger(ctx,"amount"))))))
                        .then(Commands.literal("scan").executes(ctx -> scan(ctx.getSource())))
                        .then(Commands.literal("carried").executes(ctx -> carried(ctx.getSource())))
                        .then(Commands.literal("take")
                                .then(Commands.literal("iron")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1,64))
                                                .executes(ctx -> take(ctx.getSource(), "iron",
                                                        IntegerArgumentType.getInteger(ctx,"amount")))))
                                .then(Commands.literal("planks")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1,64))
                                                .executes(ctx -> take(ctx.getSource(), "planks",
                                                        IntegerArgumentType.getInteger(ctx,"amount")))))))
                .then(Commands.literal("workshop")
                        .then(Commands.literal("craft_table").executes(ctx -> workshop(ctx.getSource(),"craft_table")))
                        .then(Commands.literal("place_table").executes(ctx -> workshop(ctx.getSource(),"place_table")))
                        .then(Commands.literal("craft").executes(ctx -> workshop(ctx.getSource(),"craft")))
                        .then(Commands.literal("place").executes(ctx -> workshop(ctx.getSource(),"place")))
                        .then(Commands.literal("auto")
                                .then(Commands.literal("on").executes(ctx -> workshop(ctx.getSource(),"on")))
                                .then(Commands.literal("off").executes(ctx -> workshop(ctx.getSource(),"off")))))
                .then(Commands.literal("plan")
                        .then(Commands.literal("explore").executes(ctx -> plan(ctx.getSource(),
                                WaifuAxoluthuTaskPlanner.Desire.EXPLORE)))
                        .then(Commands.literal("workshop").executes(ctx -> plan(ctx.getSource(),
                                WaifuAxoluthuTaskPlanner.Desire.IMPROVE_WORKSHOP))))
                .then(Commands.literal("mode")
                        .then(Commands.literal("stay").executes(ctx -> mode(ctx.getSource(), WaifuAxoluthuEntity.MovementMode.STAY)))
                        .then(Commands.literal("follow").executes(ctx -> mode(ctx.getSource(), WaifuAxoluthuEntity.MovementMode.FOLLOW)))
                        .then(Commands.literal("free").executes(ctx -> mode(ctx.getSource(), WaifuAxoluthuEntity.MovementMode.FREE))))));
    }
}
