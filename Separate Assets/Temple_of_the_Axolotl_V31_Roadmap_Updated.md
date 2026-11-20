# Temple of the Axolotl --- V31: Eldritch Miseries

**Platform:** Minecraft NeoForge 1.21.1\
**Baseline:** Checkpoint - Eldrich Miseries.zip (unchanged uploaded source checkpoint)\
**SHA-256:** 3674c6e6b5dc7729c141e84f8a316170cc87d0f5c880ba3f090441ab0d14a464\
**Archive validation:** ZIP CRC check passed; 2,010 entries, 491 Java, 901 JSON, 363 PNG. Compared to original V30 source audit: 45 new files, 28 modified files, no deleted files (CRC comparison). Contains src only; no Gradle build or gameplay run performed for this audit. V31 is a development baseline, not a release candidate.

## 0. Latest development continuation — Baby Noodle Stages I–IV

**Update:** 2026-11-19. This section records subsequent reported trials and overlay descriptions, not a new source audit. The original checkpoint hash and historical findings below remain unchanged. Earlier planned work is not implicitly closed by this update.

### Stage status

| Stage | Status | Next action |
|---|---|---|
| I — Baby Noodle foundation | PASS (reported) | Preserve existing behavior; regression test |
| II — Training Medal infrastructure | 9 PASS reported, 8 tests carried forward | Integrated validation in Stage III |
| III — Innate abilities and dungeon QoL | Experimental overlay delivered; compile fix delivered | Finish mechanics, add Registry display, compile and test |
| IV — Final polish | PLANNED | Documentation, remaining UI polish and multiplayer/release regression |

### Stage I — retained baseline

- Oxygen capacity 9,600 ticks; recharge 50 oxygen ticks per second.
- Abyssal Egg recipe: Nether Star / Heart of the Abyss / Nether Star; Glow Ink Sac / Dragon Egg / Glow Ink Sac; three Kraken Limbs. Use at fully powered active conduit in Ethereal Abyss.
- Noodle ranged attack: 9 HP, 25-tick cooldown, 8-block range, five-tick dark-blue/cyan pulse anchored at snoot. Guard within 10 blocks of owner; Follow trails 3–8 blocks in 3D; Stay sits; no random wandering in Follow.
- Universal `/hey <name> fetch <minecraft:item> <amount>`: raw vanilla resource whitelist shared with autocomplete, excludes crafted/processed/stonecut materials; suspend previous mode during Fetch and restore on delivery.

### Stage II — Training Medals (retain)

Single existing item ID with data components for base, specialized, Demi and fused medals. Keep programmable command-book behavior. Base crafting grid: lapis block / empty / lapis block; string / glowstone block / string; gold ingot / feather / gold ingot. One base medal in starter bundle; creative availability. Five innate slots per noodle, standard innate costs one, fused pair costs three. Owner-only application; reject duplicates, incompatible combinations and excess capacity before consumption; persistence across reload and Registry Recall/Summon/Revive.

| Variant | Smithing template | Addition (base is Training Medal) |
|---|---|---|
| Creeper Sense | TNT | Gunpowder |
| Blink | Eye of Ender | Ender Pearl |
| Impervious | Netherite Block | Enchanted Golden Apple |
| Devilish | Magma Block | Blaze Rod |
| Angelic | Glowstone Block | Phantom Membrane |
| Berserker | Blaze Rod | Iron Sword |
| Tank | Iron Block | Diamond Chestplate |
| Prospector | Diamond | Iron Pickaxe |
| Ghost | Ghast Tear | Soul Lantern |
| Demi | Nether Star | Nether Star |

Demi catalyst occupies the template slot; specialized medals occupy base and addition in either order. Six incompatible but fusable pairs: Blink + Impervious; Devilish + Impervious; Angelic + Impervious; Devilish + Angelic; Berserker + Impervious; Tank + Impervious. Berserker + Tank naturally compatible. Ghost incompatible with combat innates (Creeper Sense, Berserker, Tank, Devilish), including those contained in fused medals; no Ghost fusion. Utility combinations still obey their individual incompatibilities.

Field Guide: one Training Medals entry directly below Companion Commands in Celestial Companions; 14 internal pages in tested overlay, not separate chapters for variants. JEI R shows base crafting recipe; U refers players to An Axolotl's Field Guide. Hide specialized smithing listings without disabling actual recipes.

**Reported PASS (9):** compilation/launch of Stage II; medal visible in JEI; R base recipe; U referral; hidden specialized listings; correct chapter position; all 14 pages render; all nine specialized recipes; Demi catalyst recipe.

**Pending (8):** all six fusions; invalid fusion rejection; five-slot capacity; duplicates/incompatibilities; non-owner rejection; consumption only on success; world reload persistence; Registry Recall/Summon/Revive persistence. These are carried into Stage III integrated testing, not marked passed.

### Stage III — completion docket

Experimental overlay and Compile Fix 01 delivered. Initial compile failed because LivingEntity.getMaxHealth() is final. Fix reportedly moves penalty into MAX_HEALTH attributes; successful recompilation has not been explicitly reported. No fresh source audit or runtime verification of the fixes was performed for this roadmap update.

| Innate | Intended behavior / completion criterion |
|---|---|
| Creeper Sense | Guaranteed one-shot against creepers only; accelerated attacks; verify targeting and cooldown |
| Blink | Long-distance teleport; rescue owner from lava/void by exchanging positions; noodle remains revivable. Finish safe destination handling |
| Impervious | Total damage immunity; verify all relevant damage paths |
| Devilish | Offensive damage/speed buffs and fire immunity; verify exact agreed values against source/prior design before tuning |
| Angelic | Full flight AI, fall immunity, temporary yellow stars with light level 7; finish flight and temporary lighting |
| Berserker | +100% damage, +100% attack speed, −20 max HP; verify attributes |
| Tank | +50% damage, +100% max HP, 30 armor, 20 toughness, −10% movement and attack speed |
| Prospector | 15% chance to double entire completed Fetch order, one roll per order, no extra time |
| Ghost | Peaceful utility, autonomous soul lantern placement at dark spots, 20% slower Fetch; no attack or Guard. Exact light threshold and conjure/placement constraints remain to confirm |

Additive percentages relative to base, flat changes applied after percentage changes. Berserker + Tank: 22.5 HP per hit, +90% attack speed, max HP = base HP × 2 − 20 (minimum valid health). Test all six fused effects and ensure neither effects nor stat modifiers duplicate after reload or Registry operations.

#### Stage III Registry display — moved forward from Stage IV

Use the existing Companion Registry. Provide a readable representation of applied innates, used/available capacity (five slots), fused pairs and their three-slot cost, synchronized with authoritative companion data. Must permit visual confirmation during innate application and persistence tests. Exact visual layout and interaction design await the user's idea; do not invent a new screen or finalize slot layout yet. Preserve existing Registry categories, permissions, recall/summon/revive and typography.

#### Dungeon field-test additions

| ID | Requirement | Status |
|---|---|---|
| S3-Q01 | Spent spawn traps visually become vanilla Red Nether Bricks, including empty rolls and older exhausted traps; retain block identity, one-use mechanics, 60/25/15 distribution and loot ownership. Shallows, Decoy and Origin; reload/chunk/MP persistence. | Experimental implementation reported; test pending |
| S3-Q02 | Cosmic and the Seas disc never observed in loot despite plentiful other discs. Inspect registry ID, loot entries, weights, tier and custom treasure pool; restore comparable rarity if missing, do not blindly increase odds. | Investigate |
| S3-Q03 | RNGesus curse observed persisting after escape. Inspect intended lifetime and cleanup; remove on Temple exit/death return, clear timers, prevent out-of-dimension triggering, per-player isolation. Preserve 5% chance unless explicitly rebalanced. | Investigate/fix |
| S3-Q04 | Intermittent gong/bang in Fractured Origin. Trace sound event and trigger; do not remove or tune before identifying. | Investigate |

Origin expedition took roughly 45 minutes even with cheats; user eventually used Spectator to traverse floors. Ricochet bouncing through hordes trapped at one-block corners was enjoyable; retain unless infinite loops or performance issues are demonstrated. Reassess navigation after breadcrumbs rather than immediately nerfing the dungeon.

### Recommended next implementation order

1. Confirm Stage III Compile Fix 01 builds and launches; inspect existing experimental changes before modifying.
2. Obtain user's Registry display design; implement and test display against real innate data.
3. Finish Blink, Angelic and Ghost; verify remaining six innate behaviors, attribute stacking and fused effects.
4. Finish dungeon investigations and breadcrumb regression; consolidate changed/new files only into one overlay with relative source paths preserved.
5. Complete Stage III acceptance checks below. Stage IV then focuses on final presentation, Field Guide accuracy, multiplayer regressions and release gates.

### Stage III acceptance checklist (pending unless explicitly reported)

- [ ] Compile/launch after Compile Fix 01 and completion overlay
- [ ] Registry shows actual innate identity, capacity and fused cost; updates on application and lifecycle changes
- [ ] All six Demi recipes and effects; both ingredient orientations; invalid fusions rejected
- [ ] Five-slot cap; duplicates/incompatibilities; non-owner rejection; consume only on success
- [ ] World reload and Registry Recall/Summon/Revive persistence
- [ ] Creeper Sense, Blink, Impervious, Devilish, Angelic, Berserker, Tank, Prospector and Ghost individually
- [ ] Berserker + Tank stacking; no duplicate modifiers; Ghost restrictions including fused combat innates
- [ ] Spent trap texture for empty/single/double; old traps; reload/chunk persistence; multiplayer sync; no respawn or loot regression
- [ ] Cosmic and the Seas loot investigation and corrected distribution if necessary
- [ ] RNGesus exit/death cleanup and multiplayer isolation
- [ ] Identify Fractured Origin gong source
- [ ] Stage II JEI and Field Guide regression

### Stage IV — remaining work

Finalize Registry visuals as needed after Stage III functional display, revise Field Guide from planned to verified effects, test multiplayer ownership and synchronization, complete regressions and release gates. The earlier Promise, Eldritch Waters and V30 carry-forward sections below remain tracked; their historical statuses are not overwritten without new evidence.

---

## 1. Status conventions

-   PASS (reported): user observed behavior during today's iterative trials; not a fresh test of this final checkpoint.
-   PARTIAL / FAIL (reported): observed defects or incomplete tests; later overlays do not establish a pass.
-   SOURCE PRESENT: confirmed source/resource in archive, not runtime verification.
-   PLANNED: no claim of implementation. Earlier requirements remain unless explicitly superseded.

## 2. Today's progress and lessons

  ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  Workstream                          Result and remaining checks
  ----------------------------------- ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  Ring of the One                     Multitool netherrack mining reported PASS after correction; retain Oreminer and vegetation behavior; ricochet/return and multiplayer retest.

  Axolite pickaxe                     Fortune III, Oreminer reuse and Wrath's Special independent bonus discovery; reward behavior reported PASS, including Ancient Debris. Preserve exact prior pools and iron harvesting tier for T1.

  Cosmic Trident                      Static appearance restored, reported PASS. Do not reintroduce animated texture plan. Third-person Axolite charging correction reported PASS; Cosmic and other perspectives retest.

  Starfall                            ConcurrentModificationException in StarfallBurst corrected by detaching due bursts before executing callbacks; source confirms two-pass queue processing. Simultaneous proc trial reported PASS.

  Magnet and Celestial Anvil          Dynamic item attraction no longer orbiting, reported PASS. XP attraction, both modules' independent reach, upgrades and persistence still need regression checks.

  Promise modules                     Dashboard, input, sync, rig, controller, anchor planner, actions, upgrades, wearable model/texture/animation and first-person mixins present. Not a completed end-to-end gameplay system.

  Promise locomotion                  Floor/wall/ceiling/End underside, sprint, fall catch, altitude and anchor spacing reported PASS in earlier trial. Later first-person and visually mirrored arm issues; subsequent trial described as much closer, not final PASS.

  Promise role isolation              L1/R1 combat; L2/R2 mining; L3/R3/L4/R4 locomotion. Earlier role tests passed, but L4 participation later questioned. Enabled idle and disabled dormant poses reported PASS; retain independent state.

  Promise targeting/render            Mining target-to-arm correspondence failed; mirrored visuals affected mining and locomotion. Straight mining/combat beams and curved locomotion beams initially passed, later locomotion curvature artifacts observed. Follow-up transfer/pose/adhesion corrections present in source; retest.

  Promise first person                Combat visible; mining first-person not verified. Severe body/helmet clipping observed. Later armor/equipment/head mixins present, but latest fix not accepted by field test.

  Armor compatibility                 Axolite vanilla armor slot and enchantability tag changes present; verify actual enchanting, durability, helmet hiding and third-person rendering.

  Credits                             Field Guide Special Thanks entries for BillBodkin and tr7zw present; firstpersonmodel license resource present. Verify required attribution/redistribution notices against actual reused code.

  Eldritch Waters                     Portal entry reported working via setblock, but no Creative inventory portal. Boss and tentacles visible. Encounter fails expected mechanics; see tomorrow's docket.
  ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

### Final Promise design to retain

-   Eight physical tentacles: exclusive combat L1/R1, mining L2/R2, locomotion L3/R3/L4/R4. No cross-role borrowing. Dormant Eternal Embrace only when function disabled; enabled idle and working poses independent.
-   Shift+Space with fresh Space edge activates locomotion; ordinary walking/jumping before engagement, Space ascend, Shift descend, neutral altitude hold. Keep Off Ground defaults OFF. Preserve one-anchor support, 3.5-block spacing, acquire replacement before releasing previous anchor, forward preference about five blocks and rotating reserve.
-   Curved locomotion tethers with cyan spectral suction cups aligned to surface; straight mining/combat beams; correct transform and world endpoint in first/third person, no duplicate arms. Beam diameter three times earlier thin beam. Hide all dormant tentacles and all head/helmet geometry in first person, retain body, arms, held items and non-head armor; third person unchanged.
-   Combat baseline for nonweapon held: unenchanted Netherite sword damage/speed (8 HP, 1.6 attacks/s); recognized weapon inherits its own properties, not additive. Ally immunity, no durability consumption, no duplicated Starfall callbacks. Shared cooldown remains a code/design verification point.
-   Mining rolling cubic volume, exact broken-block targets, tool/enchantment/Oreminer behavior, no durability or Supernova/bedrock bypass. Base reach 5, max 13; base volume 3×4×2 to 6×6×5 over eight upgrades. Kraken Limb one per tier, +1 reach and +3% base visual scale; XP costs 2500/3125/3907/4883/6104/7630/9537/11921 (49,607 total). Magnet Stardust upgrades, +25% base strength/tier; item reach 10--18, XP only with Anvil equipped, independent base reach 10.
-   Gift Promise on first bond; owner-controlled sharing, replacement policy still open. Preserve equipped-only module wheel and dashboard toggles.

## 3. Baseline source findings for tomorrow

-   EldritchWatersPortalBlock uses a loaded-entity AABB lookup before spawning at (0.5, 2, 42.5), with arrival (0,12,0). This is not durable singleton state; unloaded bosses can be missed. User reports a new boss every portal entry; exact trigger requires runtime reproduction. Per-player return data exists.
-   EldritchArenaBuilder stores perimeter progress on each boss, writes four columns/tick, x/z -256..255, y 5..89. This can repeat per duplicate boss. Move progress to dimension-level persistence. Generator is infinite flat deep_ocean with bedrock/stone/water layers; finite terrain is not implemented. No Void Sea biome definition found in mod worldgen/biome directory.
-   Renderer explicitly scales 5× and expands frustum by 120 blocks. Scale target 3×; retain physical hitbox, recalculate visual alignment/culling. Measured old ring radius \~56 blocks, estimated new \~33.6.
-   AttackTentacleEntity has empty goal registration but custom server tick logic: 66-tick emergence, nearest player within 64, excludes Creative/Spectator, blink and strike timing, 120 HP, 24 damage. Damage filter accepts thrown vanilla or Axolite-family tridents only; melee not accepted. Investigate actual projectile identity, synced animation state, ownership and test mode rather than assuming no AI code exists.
-   AxoCthuluEntity has persistent per-entity stage, roster and timers, cosmic damage gate, shield strike after wave roster clear and final HP 120 / damage cap 24. Roster removes only loaded dead entities; disappeared dead entities may leave stale UUIDs. Verify robust death accounting. No final phase completion test yet.
-   Existing wave schedule in source (preserve unless explicitly changed):

  ---------------------------------------------------------------------------------------------
  Wave          Merolotl     Megolotl    Axokraken   Axoleviathan   Attack tentacles after mobs
  --------- ------------ ------------ ------------ -------------- -----------------------------
  1                   20            0            0              0                             1

  2                   15            5            0              0                             2

  3                   10            5            5              0                             3

  4                    8            5            5              2                             4
  ---------------------------------------------------------------------------------------------

Existing source requires Cosmic Trident shield strikes after each wave's mobs and tentacles, then crimson awakening and final battle. Preserve shield strikes as progression actions, not HP damage, while implementing the explicit rule that boss HP is invulnerable until all four waves are beaten and final vulnerability is Cosmic-only. Confirm exact intended transition with user if needed; do not silently remove prior shield mechanics.

## 4. Tomorrow morning: GL-B59--71 (all PLANNED)

  ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  ID                                  Requirement and acceptance criterion
  ----------------------------------- ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  B59                                 Portal accessible as item in Creative/search; verify BlockItem, translation and model, entry and individual return. Do not invent survival recipe.

  B60                                 Eldritch Waters only: disable Abyss pressure, damage/penalties and pressure HUD, remove underwater fog; retain water physics and Starry Seas.

  B61                                 Wave Merolotls auto-aggro to valid arena participants, reacquire after death/exit; preserve normal mobs outside arena; avoid costly global scans.

  B62                                 Distant boss spawn; proposed \~150 horizontal blocks from arrival, verify geometry, visibility and encounter trigger range.

  B63                                 No trident auto-homing on boss in any phase; shielded HP immune to all damage, only Cosmic Trident accepted after four waves cleared. Preserve legitimate shield progression.

  B64                                 Investigate slow charge (including vanilla) and projectile travel; duplicate cleanup first, measure TPS/MSPT and projectile motion before changing physics.

  B65                                 Server-authoritative shield cracking bar, wave number and live enemies remaining; same for all participants, cleanup on exit/completion. 25% per wave illustrative only.

  B66                                 Functional attack tentacle behavior and animation; vulnerable to vanilla, Axolite and Cosmic tridents, immune to other attacks; verify hitbox, projectile/melee interpretation, roster death notifications and phase cleanup.

  B67                                 Renderer 5× → 3×, not 15×; unchanged physical hitbox, collision, health, damage, movement. Recheck eye alignment and attack visual origins.

  B68                                 One active dimension encounter, persisted UUID/state; join/rejoin/death/restart do not duplicate or reset. Handle unloaded boss and duplicate roster cleanup without rewards. Completion allows legitimate new encounter per existing rules.

  B69                                 Actual 512×512 generated arena x/z -256..255, exterior void, no water/terrain/structures outside; containment and one-time dimension-level generation, avoid unnecessary chunk loads.

  B70                                 OP/debug-only dimension entity clear, provisional /tota_dev eldritchwaters clearentities; all non-player entities including unloaded, reset encounter state/HUD, no rewards, retain terrain/portals/players and other dimensions. Report removal count, no immediate auto-respawn.

  B71                                 Void Sea biome everywhere in Eldritch Waters, zero natural spawns; scripted encounter spawning remains, Celestial HUD shows Void Sea.
  ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

### Suggested implementation order and migration

B68/B70 → B69/B71/B60 → B59/B62/B67 → B61/B63/B65/B66 → B64. Back up test world before migration or entity clear. Existing generated exterior chunks and biome data need migration or developer arena reset; unloaded entity cleanup needs explicit storage/load strategy, not a loaded-entity query alone. Debug clear is intentionally destructive to non-player entities/items in that dimension. No changes applied today to this pending docket.

### Acceptance tests (all pending on V31)

-   [ ] Build and launch with dependencies
-   [ ] Creative portal search and return coordinates
-   [ ] Repeated entry, two/three players, death/rejoin and restart preserve one encounter
-   [ ] Unloaded boss never duplicated; existing duplicate cleanup preserves valid state
-   [ ] Debug clear removes unloaded entities, no rewards, other dimensions unaffected
-   [ ] 512×512 finite water/terrain and exterior void; existing save migration
-   [ ] Void Sea at every altitude; no natural aquatic mobs
-   [ ] No pressure or underwater fog; Ethereal Abyss unaffected
-   [ ] Distant boss spawn, 3× renderer, unchanged hitbox
-   [ ] Wave counts, arena-wide Merolotl targeting, tentacle behavior and all three trident vulnerabilities
-   [ ] Shield HUD, remaining enemy counts, four-wave progression and crimson transition
-   [ ] Boss HP immune until final phase; only Cosmic damage thereafter; no homing
-   [ ] Normal charge and underwater projectile movement; measure TPS/MSPT
-   [ ] Exactly one Eye reward and legitimate repeat encounter
-   [ ] Promise first-person all helmets hidden, dormant arms hidden, held items and body correct
-   [ ] Promise mining/locomotion aim, all four locomotion arms, MP and underwater movement
-   [ ] Armor enchanting/table/books, Starfall regression, module upgrades and magnet XP

## 5. Carry-forward V30 roadmap (historical source audit)

The original roadmap follows verbatim as a historical reference, not current status. V31 sections above supersede conflicting plans, particularly static Cosmic texture, axolite equipment and encounter work. All untested or unresolved V30 items remain tracked; Merolotl visible equipment, Axolite homing, broader armor pool and Eternal Void require explicit verification. Historical release gates remain applicable, with V31 baseline substituted.

------------------------------------------------------------------------

# Temple of the Axolotl --- V30 Source-Audited Roadmap

**Platform:** Minecraft NeoForge 1.21.1\
**Audit input:** `Checkpoint - v30 Source Audit.zip` (1,963 ZIP entries; 461 Java, 889 JSON, 361 PNG)\
**Audit status:** Static source inspection only. No Gradle wrapper/build files were present in this ZIP; no compilation or gameplay verification was performed.

## Status key

-   **SOURCE PRESENT:** Relevant source/resources confirmed in V30, not a gameplay PASS.
-   **PARTIAL / GAP:** Source exists but a requirement is demonstrably incomplete or narrower than specified.
-   **TEST:** Specific acceptance check needed before release.
-   **PLANNED:** Intended feature or redesign not verified as implemented in V30.

## 1. Source-confirmed V30 systems

  -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  Workstream                              V30 evidence                                                                                                                                                                                    Status / next action
  --------------------------------------- ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- ---------------------------------------------------------------------------------------------------------------------------------------
  Merolotl equipment                      `entity/MerolotlEntity.java` assigns weapons at 1% Netherite sword, 2% diamond axe, 3% vanilla trident, 1% Axolite trident; each armor slot has a 5% roll, choosing diamond or Axolite armor.   SOURCE PRESENT; TEST spawn distribution, visible models, persistence and drops.

  Merolotl targeting and ranged attacks   `MerolotlEntity.java` targets non-Creative/non-Spectator players and fires `ThrownTrident` projectiles for both trident types.                                                                  PARTIAL: Axolite throws currently use the same generic projectile path; targeted homing integration not demonstrated.

  Merolotl dust                           `data/temple_of_the_axolotl/loot_table/entities/merolotl.json`: independent 10% roll yielding 1--2 `raw_axolite_dust`.                                                                          SOURCE PRESENT; TEST loot table triggering, counts and independence from equipment.

  Merolotl renderer                       `client/renderer/MerolotlRenderer.java` is a basic `GeoEntityRenderer` with extended frustum bounds.                                                                                            PARTIAL: no armor/held-item render layers in this renderer; visible equipment is outstanding.

  Starfall                                `combat/StarfallBurst.java` executes one burst immediately and queues a second 5 ticks later; reads Sharpness, Smite, Bane, Impaling, Fire Aspect, Knockback from the weapon stack.             SOURCE PRESENT; TEST double damage events, hit invulnerability, entity immunity and multiplayer.

  Eternal Void                            `trident/EternalVoid.java` resolves enchantment level and radii 5/7/9/12/20 for I--V; enchantment JSON exists.                                                                                  SOURCE PRESENT; TEST attraction of items **and XP**, void recovery and return on all supported tridents.

  AxoCthulu                               `entity/AxoCthuluEntity.java` includes encounter/shield wave and final-phase routines, tentacle spawning, laser, and Eye award code.                                                            SOURCE PRESENT; TEST encounter from portal entry through legitimate final kill. Note `registerGoals()` says encounter AI comes later.

  Tentacles                               `entity/AttackTentacleEntity.java` exists.                                                                                                                                                      SOURCE PRESENT; TEST trident-only vulnerability and boss phase ownership.

  Cosmic/Axolite trident assets           Both `.geo.json`, textures, item models, and mesh binaries exist.                                                                                                                               SOURCE PRESENT; TEST first/third-person charging orientation, thrown and inventory visuals.

  Shallows                                `block/ShallowsPortalBlock.java`, `maze/ShallowsExpedition.java`, ruins structures and advancements exist.                                                                                      SOURCE PRESENT; TEST dimension access, generation, loot tiers, exit/return.

  Origin Temple                           Origin maze commands, portal registry, sanctum code/resources exist.                                                                                                                            SOURCE PRESENT; TEST 13×13 generation, 5 modules/tick, beam and portal persistence.

  Field Guide                             `guide/FieldGuideDelivery.java`, `guide/FieldGuideItem.java` and Patchouli book resources exist.                                                                                                SOURCE PRESENT; TEST first-entry delivery, recipe, chapter visibility.

  Axoluthu village AI                     Numerous `WaifuAxoluthu*` build, approval, navigation and learning classes exist.                                                                                                               SOURCE PRESENT; TEST actual full autonomous workflow; do not mark complete based on filenames.

  Companion Registry                      `entity/AbyssalCompanionRegistry.java` and registry item resources exist.                                                                                                                       SOURCE PRESENT; TEST registry UI, permissions, wheel and ownership.

  JEI/EMI Repository                      `compat/RepositoryJeiPlugin.java`, `RepositoryEmiPlugin.java`, `network/RepositoryRecipeFillPayload.java` exist.                                                                                SOURCE PRESENT; TEST server validation, bulk fill and crafting GUI.

  Second Fall                             `entity/SecondFallResurrection.java` exists.                                                                                                                                                    SOURCE PRESENT; TEST owner-only campfire ritual, ring return, duplicate prevention.
  -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

## 2. Highest-priority outstanding implementation

1.  **Merolotl Axolite homing:** Connect Merolotl-fired Axolite Tridents to the existing Axolite guided projectile logic; track the targeted player, not the Merolotl or another entity. Vanilla tridents must remain unguided.
2.  **Merolotl equipment visuals:** Add appropriate GeckoLib held-item and armor rendering. Current `MerolotlRenderer.java` does not expose the equipment in visible render layers.
3.  **Merolotl armor breadth:** Current natural spawn armor selection is only Diamond or Axolite, not the full vanilla/compatible armor pool previously discussed. Confirm intended pool and support pickup/equip behavior if desired.
4.  **Trident charging orientation:** Verify third-person-only pose for Axolite and Cosmic, preserving first-person/idle/inventory/thrown poses.
5.  **Cosmic Trident texture animation:** Preserve cyan accents while implementing planned moving effects in dark-purple UV regions, if still desired.
6.  **AxoCthulu encounter AI:** Validate boss navigation, wave transitions, tentacle targeting, damage caps, final-phase mechanics, Eye award, and multiplayer. `registerGoals()` still contains an explicit 'Encounter AI comes later' comment.
7.  **Eternal Void behavior:** Verify actual pickup of both item entities and XP orbs, voidproof recovery and correct Loyalty/innate return rather than treating the helper class as proof.

## 3. V30 testing matrix

  ------------------------------------------------------------------------------------------------------------------------------------------------------------
  Priority                Test                                 Pass criterion
  ----------------------- ------------------------------------ -----------------------------------------------------------------------------------------------
  P0                      Compile/build                        Full mod compiles and launches with required dependencies; no fatal errors.

  P0                      Merolotl player aggression           Targets Survival/Adventure; excludes Creative/Spectator; multiplayer selects correct player.

  P0                      Merolotl Axolite throw               Guided projectile homes on selected player; vanilla trident stays ballistic.

  P0                      Merolotl visuals                     Held sword/axe/tridents and equipped armor are visible, aligned, and animated.

  P0                      AxoCthulu full encounter             All waves, shield strikes, final phase, tentacles, and Eye reward operate end to end.

  P1                      Merolotl spawn rolls                 Weapon frequencies approximate 1/2/3/1%; armor 5% per slot; no unexpected equipment clearing.

  P1                      Merolotl dust loot                   10% success, 1--2 dust, independent of equipment; test large sample.

  P1                      Merolotl equipment drops             Validate vanilla-like drop rules and whether thrown tridents duplicate.

  P1                      Third-person trident charge          Correct both tridents; no regressions to other perspectives.

  P1                      Starfall double burst                Exactly two explosions 5 ticks apart, intended enchantment damage, allies protected.

  P1                      Eternal Void I--V                    Verify 5/7/9/12/20 radii, XP + item attraction, voidproof recovery and return.

  P1                      Origin/Shallows portal regressions   Correct access, generation, return coordinates, persistence and multiplayer.

  P1                      Companion ownership                  Registry permissions, Axoluthu owner-only approval, Wrath ritual ownership.

  P2                      Book/JEI/EMI/UI regressions          Patchouli grant, chapters, Repository autofill, Celestial Anvil, module wheel.
  ------------------------------------------------------------------------------------------------------------------------------------------------------------

## 4. Longer-term planned work

-   **Axoluthu autonomous settlement:** blueprint approval/rotation, plot clearance and owner safety, resource gathering and crafting, workstation learning, furnishing inspection, building catalog, and hologram cleanup. Source exists for multiple submodules, but complete behavior is not yet established by this audit.
-   **Celestial skies / Abyssal Lantern:** combined celestial overlay toggle, sky visuals and lantern breathing/dynamic lighting; verify against actual code before marking complete.
-   **Endgame rituals:** four Eyes of the Abyss, Tesseract summoning of the Allfather, Abyssal Egg/Conduit summoning of Axothulu, and father ritual details.
-   **Axolite gear redesign:** Pickaxe built-in Fortune III, Ring Oreminer reuse and Wrath's Special proc distribution; other equipment changes per latest design docket.
-   **Multiplayer stabilization:** portal restrictions and player-specific returns, allied immunity, spawn-trap ownership, combat balance, and third-player rendering regression checks.
-   **Documentation/polish:** localized item names, tooltip clarity, progression/advancements, UI icons, warning cleanup and final release notes.

## 5. Historical test results (not V30 retests)

Earlier field-test snapshot recorded PASS for launch/world load, Supernova transform, 30-second mining, bedrock drop, tool restriction, teleport, RNGesus 5%, spawn trap distribution, Sentinel damage, and lifesteal. Legacy resurrection failed and was superseded by the new ritual. These results **must not** be presented as V30 regression passes until repeated on V30.

## 6. Suggested release gates

1.  **V30-A --- Build and integration:** obtain full build project; compile; address Merolotl homing/visuals and any blocker failures.
2.  **V30-B --- Gameplay verification:** run P0/P1 tests, log actual results and screenshots/videos for visual bugs.
3.  **V30-C --- Multiplayer regression:** repeat 2--3-player portal, combat, companion and boss tests.
4.  **V30-RC --- Candidate release:** all P0 passing, no unresolved critical duplication/ownership issues, roadmap statuses updated from test evidence.

**Audit caution:** This is a static, selective source audit, not an exhaustive line-by-line review of all 1,963 ZIP entries. 'Source present' means code/assets were found, not that all intended mechanics are fully wired or correct.
