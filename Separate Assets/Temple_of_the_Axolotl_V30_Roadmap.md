# Temple of the Axolotl — V30 Source-Audited Roadmap

**Platform:** Minecraft NeoForge 1.21.1  
**Audit input:** `Checkpoint - v30 Source Audit.zip` (1,963 ZIP entries; 461 Java, 889 JSON, 361 PNG)  
**Audit status:** Static source inspection only. No Gradle wrapper/build files were present in this ZIP; no compilation or gameplay verification was performed.

## Status key
- **SOURCE PRESENT:** Relevant source/resources confirmed in V30, not a gameplay PASS.
- **PARTIAL / GAP:** Source exists but a requirement is demonstrably incomplete or narrower than specified.
- **TEST:** Specific acceptance check needed before release.
- **PLANNED:** Intended feature or redesign not verified as implemented in V30.

## 1. Source-confirmed V30 systems

| Workstream | V30 evidence | Status / next action |
|---|---|---|
| Merolotl equipment | `entity/MerolotlEntity.java` assigns weapons at 1% Netherite sword, 2% diamond axe, 3% vanilla trident, 1% Axolite trident; each armor slot has a 5% roll, choosing diamond or Axolite armor. | SOURCE PRESENT; TEST spawn distribution, visible models, persistence and drops. |
| Merolotl targeting and ranged attacks | `MerolotlEntity.java` targets non-Creative/non-Spectator players and fires `ThrownTrident` projectiles for both trident types. | PARTIAL: Axolite throws currently use the same generic projectile path; targeted homing integration not demonstrated. |
| Merolotl dust | `data/temple_of_the_axolotl/loot_table/entities/merolotl.json`: independent 10% roll yielding 1–2 `raw_axolite_dust`. | SOURCE PRESENT; TEST loot table triggering, counts and independence from equipment. |
| Merolotl renderer | `client/renderer/MerolotlRenderer.java` is a basic `GeoEntityRenderer` with extended frustum bounds. | PARTIAL: no armor/held-item render layers in this renderer; visible equipment is outstanding. |
| Starfall | `combat/StarfallBurst.java` executes one burst immediately and queues a second 5 ticks later; reads Sharpness, Smite, Bane, Impaling, Fire Aspect, Knockback from the weapon stack. | SOURCE PRESENT; TEST double damage events, hit invulnerability, entity immunity and multiplayer. |
| Eternal Void | `trident/EternalVoid.java` resolves enchantment level and radii 5/7/9/12/20 for I–V; enchantment JSON exists. | SOURCE PRESENT; TEST attraction of items **and XP**, void recovery and return on all supported tridents. |
| AxoCthulu | `entity/AxoCthuluEntity.java` includes encounter/shield wave and final-phase routines, tentacle spawning, laser, and Eye award code. | SOURCE PRESENT; TEST encounter from portal entry through legitimate final kill. Note `registerGoals()` says encounter AI comes later. |
| Tentacles | `entity/AttackTentacleEntity.java` exists. | SOURCE PRESENT; TEST trident-only vulnerability and boss phase ownership. |
| Cosmic/Axolite trident assets | Both `.geo.json`, textures, item models, and mesh binaries exist. | SOURCE PRESENT; TEST first/third-person charging orientation, thrown and inventory visuals. |
| Shallows | `block/ShallowsPortalBlock.java`, `maze/ShallowsExpedition.java`, ruins structures and advancements exist. | SOURCE PRESENT; TEST dimension access, generation, loot tiers, exit/return. |
| Origin Temple | Origin maze commands, portal registry, sanctum code/resources exist. | SOURCE PRESENT; TEST 13×13 generation, 5 modules/tick, beam and portal persistence. |
| Field Guide | `guide/FieldGuideDelivery.java`, `guide/FieldGuideItem.java` and Patchouli book resources exist. | SOURCE PRESENT; TEST first-entry delivery, recipe, chapter visibility. |
| Axoluthu village AI | Numerous `WaifuAxoluthu*` build, approval, navigation and learning classes exist. | SOURCE PRESENT; TEST actual full autonomous workflow; do not mark complete based on filenames. |
| Companion Registry | `entity/AbyssalCompanionRegistry.java` and registry item resources exist. | SOURCE PRESENT; TEST registry UI, permissions, wheel and ownership. |
| JEI/EMI Repository | `compat/RepositoryJeiPlugin.java`, `RepositoryEmiPlugin.java`, `network/RepositoryRecipeFillPayload.java` exist. | SOURCE PRESENT; TEST server validation, bulk fill and crafting GUI. |
| Second Fall | `entity/SecondFallResurrection.java` exists. | SOURCE PRESENT; TEST owner-only campfire ritual, ring return, duplicate prevention. |

## 2. Highest-priority outstanding implementation

1. **Merolotl Axolite homing:** Connect Merolotl-fired Axolite Tridents to the existing Axolite guided projectile logic; track the targeted player, not the Merolotl or another entity. Vanilla tridents must remain unguided.
2. **Merolotl equipment visuals:** Add appropriate GeckoLib held-item and armor rendering. Current `MerolotlRenderer.java` does not expose the equipment in visible render layers.
3. **Merolotl armor breadth:** Current natural spawn armor selection is only Diamond or Axolite, not the full vanilla/compatible armor pool previously discussed. Confirm intended pool and support pickup/equip behavior if desired.
4. **Trident charging orientation:** Verify third-person-only pose for Axolite and Cosmic, preserving first-person/idle/inventory/thrown poses.
5. **Cosmic Trident texture animation:** Preserve cyan accents while implementing planned moving effects in dark-purple UV regions, if still desired.
6. **AxoCthulu encounter AI:** Validate boss navigation, wave transitions, tentacle targeting, damage caps, final-phase mechanics, Eye award, and multiplayer. `registerGoals()` still contains an explicit 'Encounter AI comes later' comment.
7. **Eternal Void behavior:** Verify actual pickup of both item entities and XP orbs, voidproof recovery and correct Loyalty/innate return rather than treating the helper class as proof.

## 3. V30 testing matrix

| Priority | Test | Pass criterion |
|---|---|---|
| P0 | Compile/build | Full mod compiles and launches with required dependencies; no fatal errors. |
| P0 | Merolotl player aggression | Targets Survival/Adventure; excludes Creative/Spectator; multiplayer selects correct player. |
| P0 | Merolotl Axolite throw | Guided projectile homes on selected player; vanilla trident stays ballistic. |
| P0 | Merolotl visuals | Held sword/axe/tridents and equipped armor are visible, aligned, and animated. |
| P0 | AxoCthulu full encounter | All waves, shield strikes, final phase, tentacles, and Eye reward operate end to end. |
| P1 | Merolotl spawn rolls | Weapon frequencies approximate 1/2/3/1%; armor 5% per slot; no unexpected equipment clearing. |
| P1 | Merolotl dust loot | 10% success, 1–2 dust, independent of equipment; test large sample. |
| P1 | Merolotl equipment drops | Validate vanilla-like drop rules and whether thrown tridents duplicate. |
| P1 | Third-person trident charge | Correct both tridents; no regressions to other perspectives. |
| P1 | Starfall double burst | Exactly two explosions 5 ticks apart, intended enchantment damage, allies protected. |
| P1 | Eternal Void I–V | Verify 5/7/9/12/20 radii, XP + item attraction, voidproof recovery and return. |
| P1 | Origin/Shallows portal regressions | Correct access, generation, return coordinates, persistence and multiplayer. |
| P1 | Companion ownership | Registry permissions, Axoluthu owner-only approval, Wrath ritual ownership. |
| P2 | Book/JEI/EMI/UI regressions | Patchouli grant, chapters, Repository autofill, Celestial Anvil, module wheel. |

## 4. Longer-term planned work

- **Axoluthu autonomous settlement:** blueprint approval/rotation, plot clearance and owner safety, resource gathering and crafting, workstation learning, furnishing inspection, building catalog, and hologram cleanup. Source exists for multiple submodules, but complete behavior is not yet established by this audit.
- **Celestial skies / Abyssal Lantern:** combined celestial overlay toggle, sky visuals and lantern breathing/dynamic lighting; verify against actual code before marking complete.
- **Endgame rituals:** four Eyes of the Abyss, Tesseract summoning of the Allfather, Abyssal Egg/Conduit summoning of Axothulu, and father ritual details.
- **Axolite gear redesign:** Pickaxe built-in Fortune III, Ring Oreminer reuse and Wrath's Special proc distribution; other equipment changes per latest design docket.
- **Multiplayer stabilization:** portal restrictions and player-specific returns, allied immunity, spawn-trap ownership, combat balance, and third-player rendering regression checks.
- **Documentation/polish:** localized item names, tooltip clarity, progression/advancements, UI icons, warning cleanup and final release notes.

## 5. Historical test results (not V30 retests)

Earlier field-test snapshot recorded PASS for launch/world load, Supernova transform, 30-second mining, bedrock drop, tool restriction, teleport, RNGesus 5%, spawn trap distribution, Sentinel damage, and lifesteal. Legacy resurrection failed and was superseded by the new ritual. These results **must not** be presented as V30 regression passes until repeated on V30.

## 6. Suggested release gates

1. **V30-A — Build and integration:** obtain full build project; compile; address Merolotl homing/visuals and any blocker failures.
2. **V30-B — Gameplay verification:** run P0/P1 tests, log actual results and screenshots/videos for visual bugs.
3. **V30-C — Multiplayer regression:** repeat 2–3-player portal, combat, companion and boss tests.
4. **V30-RC — Candidate release:** all P0 passing, no unresolved critical duplication/ownership issues, roadmap statuses updated from test evidence.

**Audit caution:** This is a static, selective source audit, not an exhaustive line-by-line review of all 1,963 ZIP entries. 'Source present' means code/assets were found, not that all intended mechanics are fully wired or correct.
