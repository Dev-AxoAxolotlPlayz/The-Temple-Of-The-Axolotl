# Temple of the Axolotl — Master Development Roadmap V17

**Updated:** October 31, 2026 — Origin endgame completion checkpoint; next active docket is lore integration.  
**Platform:** Minecraft 1.21.1 · NeoForge 21.1.259 · Java 21 · GeckoLib 4.7.1  
**Mod ID:** `temple_of_the_axolotl` · **Package:** `com.devaxo.templeoftheaxolotl`

> **V17 precedence:** This opening docket supersedes *older active-status labels* and the old diary-title/reveal wording. All V16 and earlier roadmap text is preserved verbatim below as historical reference. **USER-CONFIRMED COMPLETE** means the developer reports the feature finished; this is not an independent source audit. **NEXT / PLANNED** means implementation remains ahead. The new lore-block architecture is a design plan, not a claim that code already exists.

## 0. NEXT ACTIVE DOCKET — Origin lore, Diary of an Axolotl, and scattered lore structures

**Status: NEXT / PLANNED.** This is the immediate follow-up to the completed Origin endgame mechanics, ahead of the deferred Merfolk, Cosmic Trident, and Cosmo-Bow modeling session.

### 0A. Custom lore blocks and consumption

- [ ] Register **two distinct custom blocks**: **Intact Lore Block** (interactive, magical) and **Broken Lore Block** (spent, inactive). Use them as reusable lore components embedded in monuments and other structures; exact visible model/texture versus concealed presentation is not yet chosen.
- [ ] Right-clicking an **intact** Lore Block in a lore site/Obelisk selects **one random chapter the interacting player has not yet discovered** from the eligible chapter pool. Never award duplicates, including on repeated interactions or rejoining.
- [ ] **First successful activation consumes the monolith globally in multiplayer**: transform the intact Lore Block to the broken/depleted variant and preserve that state. Other players cannot harvest a new chapter from the same already-consumed monolith. This is the preserved historical multiplayer rule; player-specific chapter unlocks are tracked separately.
- [ ] Do not consume a Lore Block or award a duplicate when the player has **no eligible undiscovered chapter**; decide whether to show the fading-magic message or a distinct message in this case. Preserve the global one-use behavior for a successful activation.
- [ ] On interacting with a **broken/depleted** Lore Block, display the **exact action-bar message**: `The Story Seeps as the Magic Fades...`.
- [ ] **No physical lore scrolls or loose duplicate pages**: discoveries unlock chapters directly in the diary. Maintain server-authoritative per-player persistent unlock flags, and world-persistent consumed-block state. Verify restart, relog, simultaneous multiplayer use, and structure reloads.
- [ ] Decide how the generic Lore Block carries a chapter pool/site identity (block entity, structure data, or equivalent); preserve future extensibility for different structures and Abyss lore without silently changing Origin behavior.

### 0B. Scattered peaceful lore structures and Obelisks

- [ ] Create/finish **peaceful ponds, ruined statues, and carved monolith/Obelisk lore sites** scattered across the world. These are the three historical categories; exact NBT designs, biome restrictions, frequency, rarity, placement rules and total number are not specified in the roadmap.
- [ ] Integrate an **Intact Lore Block** into the authored structure templates; preserve the site/Obelisk's decorative geometry and allow the active block to become a **Broken Lore Block** after the first successful activation.
- [ ] Implement lore-site world generation and test discoverability, block persistence, non-duplication, and reload behavior. Ensure structure templates do not accidentally replenish already-consumed sites.
- [ ] Preserve clues linking the **Origin Temple**, **Shards of Divinity → Orbs of Envy → Celestial Key**, the prison, and the ending branches. The precise inscription text is to be written, not already finalized.

### 0C. Patchouli book — Diary of an Axolotl

- [ ] Integrate **Patchouli** and create the lore book. **Final confirmed title: `Diary of an Axolotl`** (restores the original historical title, replacing the temporary `Above All’s Diary` proposal).
- [ ] Book begins with an **obfuscated title**; retain the **same cover before and after revelation**: central yin-yang symbol, halo above, flaming horns on either side. The book's contents and acquired chapters remain unchanged when its title reveals.
- [ ] Write entries as **Axolotl Above All's first-person memories**. Unlock chapters from lore-site discoveries and, where appropriate, item acquisition, recipes, and hidden advancements/flags. Use persistent, nonduplicate progression rather than inventory scrolls.
- [ ] Cover the brothers' contrasting powers; their creator **The Axolotl Absolute** (subtly foreshadowed, not prematurely explained); the Ring of the One; Wrath's failed attempt to wield it and torn wings; Above All casting it into the Minecraft-verse; imprisonment in Origin Temple `00001`; shattered key/fragments and decoy temples; Origin confrontation; forgiveness, resurrection and Second Fall. Preserve later corrections to the lore chronology.
- [ ] Keep Axolotl Absolute's precise motives, appearance and relationship to the original `axolotl_absolute` item unresolved; do not invent canon. Draft the actual chapter text and decide the chapter-to-site/unlock mapping during implementation.

### 0D. Origin Temple iron-door title revelation — latest trigger

- [ ] **Latest confirmed trigger:** when the Origin Temple prison's **iron double doors break/open through the Celestial Key rescue interaction**, permanently reveal the same book's previously obfuscated title as **`Diary of an Axolotl`**. This specifies the precise iron-door event behind the older phrase “upon opening the prison cell,” while retaining the original diary title.
- [ ] Hook the reveal to the **actual successful double-door rescue event**, not to Origin Wrath's death, mere proximity, an arbitrary block break, or an unrelated iron door.
- [ ] Keep the existing rescue mechanics intact: one Celestial Key consumed; both doors open/break together; 10-second preparation plus 30-second extraction sequence; companion handling and Origin persistence remain as already completed.
- [ ] Save the title-reveal flag persistently for the appropriate player/campaign, including relog/restart; previously discovered chapters remain available and no second book is spawned. Determine any precise multiplayer ownership behavior during implementation.

### 0E. Lore acceptance checklist

- [ ] New player sees the obfuscated book title and unchanged cover.
- [ ] First successful intact Obelisk interaction unlocks exactly one eligible undiscovered chapter and changes the block to Broken.
- [ ] Repeat interactions do not grant duplicate chapters; depleted block shows `The Story Seeps as the Magic Fades...`.
- [ ] Other players cannot obtain additional lore from a globally consumed monolith, while their own chapter progress stays separate.
- [ ] Discovered chapters, consumed blocks, and diary title flags persist across relog, server restart and chunk reload.
- [ ] A genuine successful Celestial Key double-door rescue reveals **Diary of an Axolotl** without replacing the book or losing progress; unrelated iron doors and boss defeat alone do not reveal it.
- [ ] Existing Origin mechanics, portal lifecycle, rescue/extraction and regular Temple remain unaffected.

## 1. OCTOBER 31 — Origin endgame work completed today (developer-confirmed)

**Overall Origin endgame mechanics: USER-CONFIRMED COMPLETE.** The Origin docket was revisited specifically to finish previously forgotten endgame mechanics; the developer reports that the mechanical work is now finished. This section supersedes V16's older “planned / partial / unverified” statuses for the features below, but is not a new independent code audit.

| Completed workstream | October 31 completion checkpoint |
|---|---|
| Origin dungeon and portal lifecycle | Persistent Fractured Origin, 10 floors of 13×13 maze modules, 5 placements/tick, Sanctum placement, red converted portal, Celestial Key reentry, regular Temple isolation and developer reset/save cleanup. |
| Origin difficulty and combat | Player-specific regular Wrath defeat counts, locked Origin difficulty tiers, Divine Suppression, crown interaction, Rampage, gravity/laser/pounce and final boss balance/collision. |
| Origin Sanctum and rescue | Origin Wrath defeat/forgiveness, copper chest rewards, prison iron double-door Celestial Key rescue, 10+30-second extraction and companion handling. |
| Companion mechanics | Above All riding, protective knockback headbutt; redeemed Wrath's combat headbutt, flight and weapon equipment; dungeon companion restrictions. |
| Soul-fire resurrection | Fully enchanted Ring of the One offering, Above All proximity requirement, failed-offering Ring protection and repeat resurrection route. |
| The Second Fall | Ring temptation, warning/confirmation, fallen Wrath transformation, midnight sky, permanent safe blackstone conversion and subsequent recovery/resurrection behavior. |
| Boss Ring attachment — final visual polish | **Final in-game Ring positioning completed:** normal 100% scale; `weapon_anchor` offset **X = −0.125**, **Y = 0**, **Z = −0.1875** (−2 / −3 Blockbench units); **90° Z rotation**, applied unconditionally in `BossWrathRenderer.renderStackForBone`. Ring lies horizontally and is centered in Fallen Origin Wrath's mouth. Do not restore earlier 25% scaling or conditional `isSecondFall()` transform. |
| Testing and cleanup | Developer reports Origin endgame tests and compiler-warning cleanup finished. Do not reclassify historical open tasks as outstanding solely because old V16/V15 snapshots still say “planned.” |

**Still not completed as of this checkpoint:** the **lore implementation** in §0: custom intact/broken Lore Blocks, Obelisk/scattered lore sites, Patchouli chapters and the iron-door-triggered **Diary of an Axolotl** title revelation. These are tomorrow's work, not part of today's completed Origin mechanics.

**Next after lore:** Merfolk character designs, Cosmic Trident design, Cosmo-Bow design; preserve the V13/V12 weapon mechanics and recipes as historical specifications.

---

# Preserved V16 roadmap and all earlier historical appendices (unchanged)

# Temple of the Axolotl — Master Development Roadmap V16

**Updated:** October 30, 2026 (late-night development continuing into October 31)  
**Platform:** Minecraft 1.21.1 · NeoForge 21.1.259 · Java 21 · GeckoLib 4.7.1  
**Mod ID:** `temple_of_the_axolotl` · **Package:** `com.devaxo.templeoftheaxolotl`  
**Scope:** Origin Temple generation, portal persistence, Origin Fallen Wrath spawning, difficulty persistence, developer resets and evacuation; preserves V15 water/pressure status and every earlier roadmap appendix verbatim.

> **V16 status rules:** **USER-TESTED** = player confirmed in-game; **USER-COMPILED** = player confirmed compilation; **PATCH DELIVERED / UNVERIFIED** = source patch supplied but not confirmed compiled/tested; **DESIGN LOCKED** = agreed future behavior; **PLANNED** = implementation still outstanding; **REGRESSION** = known issue. This opening docket supersedes older *status* claims about Origin Temple while leaving historical text intact. The roadmap is not an independent code audit.

## 0. Executive checkpoint — Rise of the Origin Temple

| Workstream | Current status | Evidence / next boundary |
|---|---|---|
| Fractured Origin dimension | **USER-TESTED** | Dedicated `temple_of_the_axolotl:fractured_origin` void dimension, purple/End-like sky, independent of regular Temple. |
| Origin dungeon generator | **USER-TESTED** | Ten-floor, 13×13 grid per floor; 5 module placements per tick; game reported **1,682 placed modules**, then generation complete. Maintain special-module exclusions and connectivity. |
| Origin Sanctum and boss placements | **USER-TESTED** | Above All correctly appears in prison cage; Origin Fallen Wrath spawns at intended Sanctum platform; Wrath defeat does **not** invoke regular Temple completion/extraction. |
| Portal conversion / reentry | **USER-TESTED** | Celestial Key converts regular Temple portal into permanent red Origin portal; further entries require another Key; generated Origin persists without normal Temple cleanup. Current red-block appearance is a placeholder. |
| Regular Temple regression | **USER-TESTED** | User's nine-test pass includes unchanged regular Temple behavior. Do not change the regular 9×9, 3/tick lifecycle while updating Origin. |
| `/temple fullresetorigin` original implementation | **USER-TESTED, THEN REGRESSION** | Initial incremental Origin reset and Celestial Key reentry passed. Later tests produced repeated misleading 'maze undo snapshot' messages and unsafe testing workflow. |
| `/templeexit` / reset evacuation | **PATCH DELIVERED / PARTIALLY OBSERVED** | Developer evacuation patch provided; subsequent log indicated `/templeexit` and reset activity, but full safety/regression certification pending. |
| Origin Wrath difficulty tracking | **PATCH DELIVERED / UNVERIFIED** | Personal decoy-Wrath kill counter, owner UUID, locked Tier I/II/III, developer test command proposed/patch supplied; no completed tier-behavior game test reported. |
| Automatic dimension-save wipe | **PATCH DELIVERED / UNVERIFIED — HIGH RISK** | Proposed `/temple fullresetorigin` queues a Fractured Origin-only save deletion on shutdown/restart; no PowerShell intended. **Must validate actual save lifecycle, persistence and clean restart before relying on it.** |
| Origin campaign completion | **PLANNED / PARTIAL** | Divine Suppression, forgiveness, copper chest, iron doors, companion extraction, permanent beacon beam, soul-fire resurrection and Second Fall not yet certified. |

### 0A. Origin dungeon — current generation specification

- **Dimension:** `temple_of_the_axolotl:fractured_origin`; purple void sky, no ordinary terrain; entirely separate from the normal Temple dimension.
- **Generation:** 13×13 cells per floor, ten floors; 5 placements per server tick. The observed completion announcement was **1,682 modules** (not 1,690; reserved/special placements and exclusions affect the total).
- **Dungeon bounding coordinates supplied by developer:** **(0, 0, 0) through (194, 215, 194)** inclusive (X/Y/Z); this is the planned reset region, not proof every structure block fills it. The Origin Sanctum is a larger **60×48×60** NBT structure; vertical transport modules can be **15×32×15** versus ordinary **15×16×15** modules.
- **Palette:** Origin corruption NBTs are **one-to-one substitutions for their matching normal modules**, not a separate reduced pool. Keep ordinary lava hallways, lava corridors, treasure hallways, bait rooms, trap T-junctions, and all original frequency, placement, connectivity and exclusion rules. Use corrupted variants only where the matching variant exists (corner, straight, T, four-way, decoy, bubble elevator, loot sanctum); retain the fixed regular entrance `tota_chamber_entrance` and its emerald landing block.
- **Lifecycle:** A converted Origin portal remains red and requires one Celestial Key per entry. Origin structure is normally generated only once and persists indefinitely. The developer reset is the **only intentional regeneration exception**. Never run regular Temple snapshot cleanup automatically against Origin.
- **Cost:** 4 Shards of Divinity → 1 Orb of Envy; 9 Orbs → 1 Celestial Key (**36 Shards**). Maintain existing normal Temple/portal interactions unless specifically converted.

### 0B. Nine-test checkpoint — ALL PASSED by user

1. Compile Origin patch (**after user corrected a `BlockPos`→`AABB` constructor error** in `OriginMazeCommands.java`).
2. Generate the Origin Temple.
3. Spawn Origin Fallen Wrath on the Sanctum platform.
4. Spawn Above All in the prison cage.
5. Defeat Origin Wrath without triggering regular Temple extraction.
6. Exit Fractured Origin.
7. Execute `/temple fullresetorigin`.
8. Reenter with a Celestial Key and regenerate.
9. Confirm regular Temple still works.

**Scope caveat:** These nine passes apply to the earlier working reset/spawn patch, **not automatically** to later difficulty-tier, evacuation, or automatic dimension-wipe patches.

### 0C. Origin Fallen Wrath — difficulty, ownership, suppression

**Triggering player rule:** The player who **initiates Origin Sanctum/dungeon generation** supplies the personal decoy-Wrath victory count. Save the UUID and the selected tier permanently with the generated Sanctum and boss. Other players entering later cannot change the tier. Counters are personal, persistent and increment once per qualifying regular Wrath clear; Origin Wrath does not count toward the regular-decoy total.

| Personal regular/decoy Wrath clears | Origin tier | Curse | Established combat rule | In-fight forgiveness |
|---|---|---|---|---|
| 0–10 | I / Easy | YOU SHOULDN'T HAVE I | **≥750 HP**; regular Hard baseline | Yes |
| 11–15 | II / Medium | YOU SHOULDN'T HAVE II | Divine Suppression; projectile/explosion immunity except optional Create Potato Cannon exception | Yes |
| 16–20 | II / Medium | YOU SHOULDN'T HAVE II | Same as Medium | **No** |
| 21+ | III / Hard | YOU SHOULDN'T HAVE III | Medium rules plus Totem/Eternal Guard lethal-save suppression | **No** |

**Do not conflate tier and forgiveness boundaries:** Medium starts at 11; forgiveness is allowed through **15** kills, not only through 10. The old roadmap specifies the exact forgiveness chat phrase: `Sorry I hurt you, but i also forgive you`.

**Divine Suppression:** Medium and Hard are intended to suppress relevant equipment/potion blessings during the fight; historical specification includes disabling Ambrosia blessings, armor-set effects and potion effects. Audit exact intended exemptions and interaction with innate armor powers, Ring effects and multiplayer before implementing. Hard additionally suppresses Totem and Eternal Guard lethal saves. These combat mechanics are **not yet certified implemented**.

**Developer tier test:** `/temple setdecoywrathdefeats 0`, `11` or `21` was included in the delivered difficulty foundation patch. **Command availability and counter persistence require testing in the user's current merged source.** Reset Origin between tier tests and confirm owner UUID/tier remain fixed across relog/restart.

**Boss placement:** Origin Wrath appears at the Sanctum 2×2 platform (original template horizontal center X=85, Z=72, relative to the actual placed/rotated NBT; target Y around −54 after platform offset). Above All's cage placement is already working; preserve its independent spawn. Origin Wrath must be persistently marked **bound to Origin Temple** so defeat or forgiveness never runs the regular Sanctum chest/extraction logic. Preserve hostile `BossWrathEntity` versus redeemed `TameableWrathEntity` distinction.

### 0D. Origin Sanctum endings and extraction — DESIGN LOCKED / NOT COMPLETED

- **Defeat:** A waxed weathered copper chest near Above All's prison contains **one Celestial Key** (for the double iron doors) and **one golden carrot** (to free/tame Above All).
- **Forgiveness:** If eligible, the exact phrase stops combat and converts Origin Wrath to small, winged Tameable Wrath. The chest additionally contains **one soul campfire** for taming redeemed Wrath.
- **Rescue trigger:** Using the Celestial Key on the prison's iron double doors consumes one Key and opens/breaks both doors together; *that* starts the existing **10-second preparation + 30-second extraction** sequence. Defeating Wrath alone must not extract the player.
- **Extraction:** Return the player **and all tamed companions**; never destroy or reset the Origin dimension during ordinary extraction. Implement safe handling for cross-dimensional companion teleport, offline owners, passengers and loaded chunks.
- **Visuals:** Converted portal gets a **permanent cosmetic red beacon-style beam without vanilla pyramid requirements**. Current red portal block is a temporary visual placeholder.
- **Further endings:** Post-victory **soul-fire resurrection** of Wrath requires a maximally enchanted Ring of the One sacrificed into soul fire with freed Above All within 4 blocks; invalid offering must preserve Ring and show `The Soul of the Damned refuses the sacrifice`. Optional **Second Fall** remains as specified in the V15/V14 historical material: deliberate Ring offering, warning/confirmation, Hard-tier fallen boss in Overworld, midnight and safe terrain blackstone conversion. Neither is certified implemented.
- **Solo restriction:** Historical Origin design requires keyholder-only session isolation from initial entry, not merely from boss spawn. **Needs source and gameplay validation** in the new persistent-dimension implementation.

### 0E. Developer reset: regression and intended replacement

**Observed regression:** After the safe-evacuation changes, the developer reported repeated **'A maze undo snapshot is still active'** messages. The shared game log showed generation/reset activity and separate invalid block-attached entity warnings near Y=−57; the user clarified that the spam of concern was the **undo-snapshot text**, not the decorative-entity warnings. The exact repeated caller and lifecycle interaction are **not yet conclusively diagnosed**. Avoid deleting safety checks or assuming that the message alone proves an active snapshot.

**Requested final workflow:**

```mcfunction
/temple fullresetorigin
```

1. Evacuate every player from Fractured Origin to a safe Overworld position, preserving/restoring appropriate game modes.
2. Stop or safely invalidate Origin generation, pending entry teleports and extraction jobs; block reentry while reset pending.
3. Queue a **Fractured Origin dimension-save-only wipe**, with no block-by-block demolition and no regular Temple undo snapshots.
4. **Exit/restart Minecraft once**; the mod automatically performs the wipe at a verified safe point in save shutdown/startup lifecycle. **No PowerShell, manual directory deletion or separate script**.
5. Reset Origin-only saved completion/progress state; preserve regular Temple, Overworld converted portal, Celestial Key rules and personal defeat counters. Next key entry builds a new Origin Temple.

**Safety gate:** The latest delivered automatic-restart wipe patch was **not yet user-compiled or runtime-tested** at roadmap time. Before calling it safe, verify exact folder scope `dimensions/temple_of_the_axolotl/fractured_origin`, startup/shutdown ordering (no live open region files), failed-deletion handling, state reset, backup/recovery, reentry blocking, repeat reset, and no accidental overwrites from older patch versions. Do not represent 'queued' as 'wiped' until restart and world inspection confirm it.

### 0F. Immediate next engineering order

1. **Validate latest automatic restart wipe patch**: compile, run `/temple fullresetorigin`, confirm evacuation and no spam, close/restart, verify fresh dimension regeneration and normal Temple non-regression. Keep a world backup.
2. **Validate difficulty foundation**: personal counters, developer `setdecoywrathdefeats`, generation trigger UUID, Tier I/II/III locking and persistence; do not mistake a spawned boss for completed combat tier mechanics.
3. **Implement Origin combat**: Easy/Medium/Hard health and immunities, Divine Suppression, projectile/explosion exceptions, Hard lethal-save prevention; regression-test normal Wrath and Ring.
4. **Implement forgiveness and Sanctum rescue**: exact phrase and ≤15 threshold, chest reward variants, double-door Celestial Key consumption, companion-inclusive 10+30-second extraction.
5. **Finish portal visuals**: persistent red beam and red portal polish; test across relogs and unloaded chunks.
6. **Future Origin narrative**: solo ownership safeguards, Diary of an Axolotl reveal, soul-fire resurrection and optional Second Fall.
7. **Continue previously active Phase 6.5/6.6 docket**: hurt-camera/knockback fix; Crushing Pressure, Bends, Diluted Ichor cure, Diving Helmet and Sub-for-One. **NO TOUCHIE BOAT**; retain V15's status boundaries.
8. **Other V15/V14 commitments remain**: Abyss Watergate/ruins, Axoleviathan/Axokraken/Axoluthu, Moai, Ring and Celestial Anvil regressions, all preserved historical features. Do not resurrect cut Repository search/sort or separate Ambrosia fluid.

---

# Preserved V15 master roadmap and V1–V14 historical appendices (unchanged)

# Temple of the Axolotl — Master Development Roadmap V15

**Updated:** October 29, 2026 (development session spanning October 29–30 local time) — Phase 6.4 water completion, Phase 6.5 pressure/HUD/death-message work, Phase 6.6 decompression design.  
**Platform:** Minecraft 1.21.1 · NeoForge 21.1.259 · Java 21 · GeckoLib 4.7.1  
**Mod ID:** `temple_of_the_axolotl` · **Package:** `com.devaxo.templeoftheaxolotl`  
**Source basis:** User's V14 master roadmap, conversation-confirmed gameplay and decisions, plus spot inspection of the supplied **Major Checkpoint — 6.5 — Pressures Under the Sea COMPLETED** ZIP. No new Gradle build or full source audit was run for this roadmap. The entire V14 document, including V1–V13 history, follows unchanged.

> **V15 precedence:** This opening update supersedes earlier active status and design wording where later decisions differ. Historical text is retained for traceability. **USER-TESTED** means user observed in game; **SOURCE-OBSERVED** means a source file was inspected in the supplied checkpoint; **CANDIDATE/NEEDS TEST** means a patch or behavior is not runtime-certified; **DESIGN LOCKED** is an agreed future specification; **PLANNED** is not yet implemented. Never conflate these statuses.

## 0. Executive docket — October 29 achievements and immediate status

| Workstream | V15 status | Outcome / next boundary |
|---|---|---|
| **6.4 Dreamlike Starry Seas** | **USER-APPROVED COMPLETE** | Dark blue-teal translucent water, cyan/lavender star sparkle, bioluminescent crests, depth-aware underwater color/clarity, and `/tota water enable/disable`; user approved nighttime visuals. **NO TOUCHIE BOAT**: preserve functioning buoyancy and boat physics. |
| **6.5 Abyss Pressure HUD** | **USER-TESTED** | Analog pressure dial at right of Position/Chronometer, reads player's eye height, scales to 100 at Overworld Y=-30. New second 9-slot module column with Abyss Pressure Gauge item enabling the display; HUD layout and pixel silhouettes approved. |
| **6.5 Helmet/module GUI** | **USER-TESTED VISUAL FIX** | Crescent-moon Night Vision, fish Conduit Power and horseshoe Magnet silhouettes draw above buttons. NV and Conduit are independent toggles. Prior blank gray icon squares fixed. |
| **6.5 Pressure damage** | **USER-TESTED / SOURCE-OBSERVED** | Underwater-head-only pressure damage, warning and lethal tiers; player confirmed rapid damage and death. Actual source uses `PlayerTickEvent.Post`, custom `abyss_pressure` damage source and death message subclass. Verify armor/iframe bypass claims independently; damage-type JSON in checkpoint contains only message ID, scaling and exhaustion. |
| **6.5 Randomized death messages** | **USER-TESTED** | User shared in-game chat screenshot showing multiple distinct Axoluthu messages. Source observed: five helmet-tier messages and eight crush-tier messages selected randomly on death. |
| **6.5 Hurt-camera shake** | **CANDIDATE / REGRESSION** | Initial `GameRenderer.bobHurt` mixin caused startup `MixinTransformerError`; likely bad injection signature. A corrected-signature patch was supplied, **not yet confirmed to launch or suppress shake**. Treat checkpoint's mixin as suspect until replaced and tested. |
| **6.5 Physical knockback** | **OPEN BUG / DESIGN DISCUSSION** | Frequent pressure hits produce excessive ping-pong movement. Do not confuse physical motion with camera tilt. User is considering controlled movement restriction instead of allowing uncontrolled repeated knockback. |
| **6.6 Crushing Pressure** | **DESIGN LOCKED / NOT IMPLEMENTED** | Progressive underwater movement impairment as pressure grows; full effect thresholds, swimming physics and protection interactions to be implemented/tested. |
| **6.6 The Bends** | **DESIGN LOCKED / NOT IMPLEMENTED** | Accumulating decompression consequence when Crushing Pressure level decreases during ascent; blobfish PNG status icon; not removed by milk, helmet or submarine; protective equipment only stops further accumulation. Diluted Ichor bucket cures existing Bends. |

## 1. Phase 6.4 — Abyssal water and boat preservation

- **Visuals:** Dreamlike Starry Seas aesthetic, translucent navy/teal water, sparkling cyan and lavender star motifs, glowing bioluminescent wake/crest effects. Underwater tint darkens with depth while retaining readable, clear visibility; water visuals can be toggled using `/tota water enable` and `/tota water disable`.
- **User result:** Water aesthetic approved; user used a screenshot as PC lock screen. Water physics and boat buoyancy declared complete by user. **Do not modify boat physics in Phase 6.5/6.6 fixes** without explicit approval and regression tests.
- **Reference ranges:** Overworld surface **Y=62** to deepest **Y=-30** (92 blocks); Ethereal Abyss intended surface **Y=200** to deepest **Y=0** (200 blocks). These are the pressure normalization references, not proof that the planned Abyss worldgen is complete.

## 2. Phase 6.5 — Pressure and interface implementation

### 2A. Pressure formula and damage tiers

Current source uses eye height and `clamp((seaLevel - player.getEyeY()) * 100 / depthRange, 0, 100)` with `(seaLevel, depthRange)=(62,92)` for Overworld and `(200,200)` for **every non-Overworld dimension**. **TODO:** restrict Abyss-specific behavior to the actual Abyss dimension instead of treating Nether/End as Abyss by default, if desired.

| Pressure reading | Classification | Current effect / intended protection |
|---|---|---|
| **0–25** | Safe | No pressure damage. |
| **>25–50** | Warning | Warning only; no pressure damage. |
| **>50–75** | Helmet zone | Without protection, **0.5 HP every 4 ticks**. Diving Helmet protection planned, not certified. |
| **>75–100** | Crush / submarine zone | Without protection, **1.0 HP every 2 ticks**. Sub-for-One protection planned, not certified. |

- Damage is server-side and only when **head underwater** (`player.isUnderWater()`), alive, non-Creative and non-Spectator. Dry caves and partial foot immersion do not qualify.
- Damage source is `temple_of_the_axolotl:abyss_pressure`; checkpoint includes `data/temple_of_the_axolotl/damage_type/abyss_pressure.json` and `AbyssPressureEvents.java`. **Important audit item:** a dedicated bypass tag/other mechanisms are needed to substantiate prior 'true damage bypasses armor, resistance, invulnerability frames' claims; the inspected JSON by itself does not prove those bypasses. Preserve observed lethal cadence while validating exact protection interactions.
- Existing pressure damage and death-message logic should not be weakened or rewritten to solve visual shake or physical knockback.

### 2B. HUD, modules, and screen fixes

- Pressure gauge is an analog circular dial to the right of the Position/Chronometer area; player confirmed 100 at head Y=-30 and continuous eye-level tracking.
- Gauge requires **Abyss Pressure Gauge** item installed in first slot of a **second 9×1 vertical module column**, adjacent to the original nine slots (18 total).
- GUI uses the existing Ethereal Abyss background and shows Magnet, Mining, Night Vision and Conduit Power controls. Crescent moon, fish and magnet pixel silhouettes now render in `render()` **after** `super.render`, not underneath widgets in `renderBg`; user confirmed fixed.
- Retain separate Night Vision and Conduit toggles and existing Ring Mining toggle. HUD background was trimmed to remove excess empty area; verify actual source layout before further resizing.

### 2C. Axoluthu's Cemetery — randomized death messages

- **USER-TESTED:** in-game chat showed different death lines including 'sent to Axoluthu's Cemetery', 'saw something shiny in the dark', 'received a one-way invitation from Axoluthu', 'went deeper than destiny permitted' and 'thought to go for a swim'. Repeats are normal random selection.
- **SOURCE-OBSERVED:** `RandomizedPressureDamageSource` selects **5 helmet** keys `death.attack.abyss_pressure.helmet.0`–`.4` and **8 crush** keys `death.attack.abyss_pressure.crush.0`–`.7`; translation text is in `en_us.json`. The source's actual grouping overrides the earlier brainstorming-only 11-message count.
- Death text uses the victim's actual display name dynamically (`%1$s`), **not a hardcoded real name**. Preserve this privacy and localization behavior.
- Maintain mix of ominous and comic lines: cemetery, weight of the ocean, Abyss unforgiving, Axoluthu's records/invitation, submarines, destiny, sardine, forgotten helmet, tasting the sea, swimming, shiny object. Existing 13 strings remain source authority; don't silently delete any.
- **Future Sub-for-One:** move 'thought to go for a swim' and 'saw something shiny in the dark' into a dedicated submarine-incident pool when a robust event/context exists. Current appearance in crush pool is acceptable until then.
- Translation follow-ups: validate `death.attack.abyss_pressure.player` fallback and the Celestial Anvil's `block.temple_of_the_axolotl.celestial_anvil` localization, if still missing in actual build.

### 2D. Outstanding camera and knockback bugs

1. **Startup regression:** original `AbyssPressureNoHurtCameraMixin.java` in checkpoint targets `GameRenderer.bobHurt` with `(Camera camera, float partialTick, CallbackInfo)`; the injection signature is suspect for Minecraft 1.21.1. User supplied crash report showing `MixinTransformerError` during class initialization and downstream `RenderType` initialization failure. A replacement signature patch was delivered after the checkpoint, but **no user-confirmed successful run** has been recorded. Verify against exact mappings and `latest.log` before certifying.
2. **Camera requirement:** suppress only pressure-induced hurt-camera tilt. Retain ordinary combat hurt-camera effects and all pressure damage.
3. **Motion requirement:** pressure damage currently causes extreme ping-pong motion under rapid repeated hits. Investigate knockback and velocity pathways independently of hurt-camera mixin. Avoid per-tick velocity reset that would break natural swimming/buoyancy. User's newer design suggests controlled immobilization via Crushing Pressure; exact physics remain pending.
4. **Non-regression:** NO TOUCHIE BOAT; no unrequested changes to damage cadence, death text, HUD, or other mods.

## 3. NEW Phase 6.6 — Crushing Pressure and The Bends

**Overall status: DESIGN LOCKED / NOT IMPLEMENTED.** This is the next active docket. Phase 6.6 introduces movement impairment and decompression consequences while preserving Phase 6.5's pressure-damage foundation.

### 3A. Crushing Pressure — descent effect

- Custom underwater status effect **Crushing Pressure**. Conceptual intensity increases with current pressure; reduces ability to swim/move, becoming very restrictive at extreme depth. Pressure should feel oppressive rather than randomly launching players.
- Candidate tiers: pressure **>50–75 = Crushing Pressure I**; **>75–100 = Crushing Pressure II**. Earlier discussion considered mild movement slowdown in warning zone **>25–50**; this is an **optional balance proposal, not a finalized threshold**.
- Restrict player-controlled swimming/acceleration, not ordinary gravity, currents, or protected vehicle handling. Preserve normal movement where pressure is safe. Ensure only head-submerged context triggers underwater pressure mechanics.
- Diving Helmet and Sub-for-One are intended to protect appropriate tiers; exact protection flags, helmet behavior, submarine pressure seal and how effect clears while protected require implementation decisions. Do not imply equipment already exists.
- Visual indicator/status-effect icon for Crushing Pressure is open; cracked helmet is a **suggested** icon, not final approved art.

### 3B. The Bends — ascent consequence (LOCKED)

**Trigger rule:** When a player's active **Crushing Pressure level decreases by one due to ascending across a pressure boundary**, add **The Bends I** (one accumulated severity level). Multiple unprotected level decreases accumulate additional Bends levels. This is **not** a generic timer that starts merely upon reaching the surface; the *level drop during ascent* is the event. Do not trigger when the effect changes solely due to equipment changes or unrelated clearing.

- **The Bends persists** after the player surfaces; entering the Sub-for-One or putting on the Diving Helmet **does not remove or reduce existing Bends**.
- **Protected equipment prevents further Bends accumulation** but does not cure existing levels. Distinguish current Crushing Pressure level from persistent Bends severity.
- **Milk does not cure it**; no bucket-of-milk bypass. A durable per-player severity/exposure state may be needed so vanilla status-effect clearing does not erase the condition. Administrator/debug commands should remain practical unless later specified.
- **The Bends icon:** a **blobfish PNG** in the status-effect UI. User approved the blobfish idea and illustrated presentation; production sprite/art and in-game registration remain to be created.
- **Effect gameplay:** intermittent Slowness, Mining Fatigue and mild damage were *suggested* as potential symptoms; actual severity effects, durations, level cap, persistence across death/relog, exact detection on rapid versus gradual ascent, and cure interaction need explicit implementation/balance decisions. **Do not present suggestions as locked.**
- This is an intentionally game-simplified decompression mechanic, not a real-world medical model.

### 3C. Diluted Ichor — new cure (LOCKED)

- Existing **Diluted Ichor Bucket** gains an additional consumable use: **drink one bucket to fully cure all accumulated Bends levels**, then return an empty bucket.
- Milk remains ineffective; Diving Helmet and Sub-for-One only prevent further accumulation. **Diluted Ichor does not cancel active Crushing Pressure or provide lasting immunity**; a new unsafe ascent can produce new Bends.
- Preserve the existing Diluted/Pure Ichor distinction and Cosmic Brewing Stand production; **do not resurrect the CUT Ambrosia fluid** or obsolete water-cauldron recipe.
- Drinking duration/animation, inventory edge cases, creative behavior and item use interactions remain implementation details. Longer-than-potion consumption was proposed but not locked.

### 3D. Phase 6.6 acceptance tests / implementation order

1. Establish explicit pressure-tier and previous-tier state, with server authority, eye-level depth calculation, and exact Abyss dimension selection.
2. Add Crushing Pressure status effect and deterministic swimming resistance; confirm smooth motion and no camera shake/knockback regressions.
3. Detect **ascent-driven** tier drops; grant one additional Bends level per qualifying tier crossed, including multi-tier rapid ascent. Avoid double-counting on ticks, relog or equipment transitions.
4. Implement persistence, milk immunity, equipment-based prevention of **new** Bends, and status-effect display with blobfish PNG.
5. Implement Diluted Ichor drink-to-cure, bucket return and no permanent immunity; verify current brewing/crafting uses are unchanged.
6. Verify protected vs unprotected ascent, surface transition, Creative/Spectator, death/relog, vanilla effect clearing, HUD pressure values, and Sub-for-One future integration. Set explicit decisions for severity cap, effects, safe ascent and death reset before final certification.
7. Regression: water shader toggles, boat buoyancy, module GUI, death messages, Eternal Guard, ordinary combat camera and natural swimming.

## 4. Updated overall roadmap — active order and preserved commitments

| Priority | Docket | Status / next milestone |
|---|---|---|
| **Immediate repair** | 6.5 camera shake and excessive pressure knockback | Correct/test mixin, isolate motion effects; preserve all existing pressure logic. |
| **Next major phase** | **6.6 Crushing Pressure / The Bends / Diluted Ichor cure** | Implement and game-test new movement/decompression system, blobfish icon, anti-milk rules. |
| **Next equipment** | Diving Helmet | Provide tier-appropriate protection; prevent further Bends without erasing existing severity. Exact crafting and mechanics TBD. |
| **Next vehicle** | Sub-for-One | One-seat submersible, safe crush-zone exploration, exactly one Axoleviathan bite survived when pristine, second bite catastrophic unless repaired; future submarine death-message pool. |
| **Abyss world/portal** | Ethereal Abyss Y=200 sea, ocean strata, trenches, vents, Mer-Axolotl villages; Abyss Watergate | Preserve naturally generated Deep Ocean / Deep Frozen Ocean ruins, **7 houses, 4 bubble-glass trail modules, 1 central Watergate, 1 jukebox, several Moais**, disc playback activation. Worldgen and portal are not completed by water-shader work. |
| **Abyss fauna/boss** | Axokraken, Axoleviathan, worshippers, Axoluthu | Preserve alliance, 1,024 HP leviathan, 30 armor/20 toughness, guaranteed Heart, replacement Axokraken, Cosmic Trident-only boss weapon, five leviathans after boss death. |
| **Other existing work** | Ring, Fallen Boss Wrath, Sanctum, Origin campaign, companion AI, Repository/Modules, workstations, audio, dungeon economy, lore | Retain V14/V13 detailed status and unresolved regression checks; new water work does not automatically certify or cancel these. |
| **Deferred/CUT** | Create Easter eggs (later); separate Ambrosia, Repository search/sort, standalone multiplayer regression docket (cut) | No reactivation without explicit new user decision. |

### Release and continuity guardrails

- **NO TOUCHIE BOAT** — user-approved Phase 6.4 boat physics and water visuals must remain intact.
- Keep existing randomized pressure death messages and their dynamic player-name formatting.
- No global cancellation of camera shake or damage knockback for unrelated damage sources.
- Do not confuse a screenshot, supplied ZIP, successful compilation, user approval or design agreement with an exhaustive implementation test.
- Preserve original V14 and all historical appendices below, unchanged; newest V15 active decisions take precedence over old prospective wording (especially V13's outdated 75/100-block pressure specification).

---

# Preserved V14 master roadmap and V1–V13 historical appendices (unchanged)

# Temple of the Axolotl — Master Development Roadmap V14

**Updated:** October 28, 2026 — Phase 6.3.x Ring and Abyssal Moai progress, warning cleanup, Abyss Watergate and Sunken Merfolk Village design.  
**Platform:** Minecraft 1.21.1 · NeoForge 21.1.259 · Java 21 · GeckoLib 4.7.1  
**Mod ID:** `temple_of_the_axolotl`  
**Authority:** User-reported tests, screenshots, patch discussion, and design decisions through October 28. **This is a planning and progress docket, not an independent audit of the latest source ZIP.** V13 and all its earlier historical appendices are preserved in full below. Latest explicit decisions supersede older status descriptions.

> **READ FIRST — V14 STATUS CONVENTION.** **USER-TESTED** = behavior observed and confirmed by user; **COMPILED** = user-reported build passed, not gameplay certification; **CANDIDATE** = patch or implementation discussed/delivered but not independently validated; **DESIGN LOCKED** = agreed specification, not implemented; **PLANNED** = work remaining. Never treat a design note as source implementation or a compilation pass as runtime verification. **No warning suppressions** are allowed as a substitute for real API migrations.

## 0. V14 executive progress checkpoint

| Workstream | Latest status | October 28 outcome / remaining boundary |
|---|---|---|
| **Ring of the One — 3D visuals** | **USER-TESTED / refined** | Elaborate Blockbench ring completed with animation and hand-painted high-resolution texture. User reported **318 bones / 1,176 cubes**, **256×256 minimum UV atlas** and **2048×2048 rendered texture** for acceptable smooth gradients. Ring item/first-person/flight render orientations iterated; user confirmed flight working. Inventory icon continued needing orientation adjustments; do not infer final axis/angle from an earlier request. |
| **Ring — arcing chakram attacks** | **USER-TESTED / refined** | Elliptical flight and return improved; outward zenith and arc width must derive from the **player's actual entity-interaction/reach attribute**, not a fixed 8-block constant. Longer/wider arcs should proportionally extend return time. User reported path fix working. Preserve attack-speed scaling and swept-perimeter hit detection. |
| **Ring — enchantments** | **USER-REPORTED WORKING FOR LATEST PATCH; regression still appropriate** | Arc attacks must carry applicable Ring enchantments, including sword enchantments and **Breach** and **Density** when placed via the Celestial Anvil's intentional nonstandard enchantment rules. Lucky Stars proc-on-kill fireworks: roll 1 **purple/lavender**, roll 2 **white**, roll 3 **cyan**; if multiple rolls occur, show the **highest roll's color**. User said the integrated patch worked; test each enchant and combination individually before blanket certification. |
| **Abyssal Moai** | **USER-SHOWN MODEL / USER-TESTED PROTOTYPE** | Replaces the prior Abyssal Torch concept while preserving the `abyssal_torch` registry identity. Two-block-high, floor-mounted, facing-aware Moai/gargoyle hybrid with dark navy/sculk stone and cyan details; glow/light emission retained **underwater**, comparable to a sea lantern. Inventory representation should show the **full upright model** facing the viewer, not just its base or rear. User demonstrated a multi-Moai perimeter and reported satisfactory prototype behavior. |
| **Moai mob repulsion** | **USER-TESTED / targeted exceptions** | Intended five-block radius, hostile and actively aggressive neutral mobs; exclude players, passive mobs, pets, Temple Crew **including Boss Fallen Wrath**, and all Abyssal faction members. **Zombie horses explicitly added as a special repulsion target** despite the normal peaceful/neutral exclusions. Validate zombie horse/rider interactions, field overlap, removal, and full measured radius in current build. |
| **Java warning cleanup** | **USER-REPORTED CLEAN COMPILE** | Series of Phase 6.3.2 warning fixes migrated deprecated enchantment calls, NeoForge event-bus registrations, client fluid extensions, chunk checks and other reported warning sites **without suppressions**. Latest user-shared `compileTestJava` result: **BUILD SUCCESSFUL**, `compileJava` ran with **no reported warnings**, `compileTestJava NO-SOURCE`. IntelliJ restart/inspections and in-game regressions remain separate verification steps. |
| **Abyss Watergate / lost merfolk hamlet** | **DESIGN LOCKED / NOT YET IMPLEMENTED** | Rare naturally generated seafloor ruin in **Deep Ocean** and **Deep Frozen Ocean**, with a central giant Watergate, a jukebox shrine, Moai guardians, **seven ruined house modules**, **four bubble-like glass pathways**, and supporting decoration. Details in §2 below. |

## 1. Ring of the One — October 28 implementation notes

### 1A. Blockbench and renderer

- Ring's high-complexity model and animations were user-reported finished. Record model scale and texture resolution **separately**: 256×256 UV layout and 2048×2048 pixel texture are not interchangeable measurements. Avoid reintroducing tiny 16×16 or 32×32 art for this asset.
- Three distinct display contexts require independent inspection: **GUI icon**, **first-person held item**, and **flying chakram entity**. Rotations that fix one can break another. Previous experimentation included 90° X-axis flattening and Z-axis icon rotation; use the latest model and renderer source to confirm final transforms rather than blindly applying an old angle.
- Flight animation and arcing attack geometry are separate from the decorative spinning/animated geometry. Avoid an effect where the center appears stationary while only some blades spin.

### 1B. Attack range and enchantments

- Read the current player's effective reach/entity-interaction attribute at throw time. Ellipse zenith, width, and return duration must respond dynamically; don't hard-code a presumed eight-block reach. Preserve player-relative return homing, pitch/yaw alignment, attack-speed scaling, and narrow swept-path damage.
- Apply Ring enchantments to projectile/arc combat with the correct hit attribution, damage calculations, and on-kill events. The Celestial Anvil intentionally allows unusual sword enchantments plus Breach and Density on the Ring; Fortune and Silk Touch remain mutually exclusive under the existing Anvil specification.
- Lucky Stars: on a successful enchanted kill, firework-star visual uses **lavender/purple** for roll 1, **white** for roll 2, **cyan** for roll 3. If rolls overlap, **highest successful roll determines the color**. Keep server-side proc logic distinct from client particle effects; avoid duplicate visual triggers.

## 2. Abyssal Docket — Watergate and Sunken Merfolk Village

### 2A. Watergate: worldgen, structure and activation — DESIGN LOCKED

- The **Abyss Watergate is NOT a player-crafted, freely placeable portal entrance** in normal progression. It is the central monument of a naturally generated ancient merfolk ruin on the ocean floor, restricted to **Minecraft's Deep Ocean and Deep Frozen Ocean biomes**.
- The Watergate's visible frame is one **oversized Blockbench/GeckoLib model anchored to a single controller block**. The surrounding ruins are Minecraft structure-template content. Its visual dimensions can exceed the one-block controller, but generation must clear/reserve the model footprint and the portal collision area; do not assume one block of physical collision covers the full gateway.
- **Musical key:** A **jukebox included in the structure NBT** plays **The Cosmic and the Seas** music disc to awaken the Watergate. Activation requires **actual playback**, not merely disc storage. Whether ordinary jukeboxes, Celestial Jukeboxes or both qualify, the precise detection radius, and whether activation persists after playback stops remain **OPEN**. Do not automatically place a preplaying disc or assume permanent activation.
- **Portal effect:** A custom animated **cyan/turquoise Nether Portal-inspired surface** inside the Watergate opening, with a deep-navy center, luminous cyan streaks, and cyan portal-style particles. Vanilla `PORTAL` particles are not simply tintable; use custom particles or a suitable tintable particle type. Server controls actual teleportation/active state; client controls visuals and particles. This is a **planned renderer**, not a working portal yet.
- Possible activation presentation: dormant → musical awakening → cyan rune/particle buildup → expanding portal surface → active swirling gate → shutdown. This is an **art-direction proposal**; exact timing and shutdown mechanics are not locked.

### 2B. Sunken Merfolk Village — DESIGN LOCKED

**Scale:** A compact, ruined merfolk **hamlet**, **not** an Ancient City-sized settlement. The Ancient City's central monumental frame is a *layout analogy*, not a request to copy its scale.

| Structure element | Count | Requirement |
|---|---:|---|
| **Abyss Watergate centerpiece** | **1** | Giant central gateway; controller model integrated into NBT/worldgen structure |
| **Ruined merfolk house modules** | **7** | Compact, visibly damaged underwater homes around the monument |
| **Bubble-glass pathway modules** | **4** | Scattered individual glass blocks/clusters suggesting bubble trails and paths through the water; **NOT** sealed glass tunnels, domes, or air-filled roads |
| **Jukebox** | **1** | Pre-positioned in the NBT as the Watergate's musical activation shrine |
| **Abyssal Moai sentinels** | **Several; exact count TBD** | Decor and hostile-mob repulsion around the site; keep all faction exceptions |
| **Other decor** | **As designed in NBT** | Ruined stone/prismarine details, seabed embellishments, ancient architectural remains |

- Bubble pathways should look **organic and watery**: irregular individual transparent or pale-cyan glass blocks, staggered heights, gaps, and small floating clusters, visually reminiscent of bubbles. **They are not glass-enclosed corridors.** This explicitly supersedes any earlier suggestion of intact air-filled tunnels.
- House arrangement, Moai positions and decorations are authored by the user in Minecraft and saved as NBT templates. One complete structure template is acceptable if it fits; **modular templates/jigsaw generation are an option**, not a mandatory design decision. Seven houses + four paths + one gate = **12 principal layout modules**, with jukebox and Moais embedded in the site rather than incorrectly counted as extra houses.
- Placement should resemble vanilla underwater ruins: some partial burial and terrain integration are appropriate, but the main gate and its activation point must remain reachable. Ensure reliable ocean-floor anchoring, waterlogging, unobstructed portal opening, and compatibility with uneven terrain. Worldgen spacing/rarity, precise bounding box and whether houses vary by seed are **OPEN**.
- Distinguish this **ruined Overworld village** from the **living Abyss Upper Ocean mer-axolotl settlements** in V13 §2F. The latter are floating bubble-shaped **prismarine architectural clusters without enclosing glass domes**; the Overworld ruins use **stray glass bubble trails** as pathways. Do not conflate these two designs.

### 2C. Moai presentation and behavior — updated from V13 §2D

- Model style: hybrid **Moai + sentinel gargoyle**, not an ordinary Easter Island statue or generic torch. Dark deep-ocean stone, cyan details, luminous accents. User iterated Blockbench geometry/UVs, fixed z-fighting, and refined the finished appearance.
- Retain **soul-torch-like light output underwater**, roughly sea-lantern-level emission; validate actual block light and submerged rendering rather than only emissive-looking texture.
- **Full-model inventory icon**: show the entire upright statue, not only the pedestal, and orient its **face toward the GUI camera**. The user specifically rejected a rear-facing inventory pose.
- Repulsion must exclude all Temple Crew (including **hostile Boss Fallen Wrath**) and Abyssal faction mobs, while **zombie horses are a deliberate special-case target**. Test hostile jockey mounts as well as rider behavior.
- Maintain the existing registry identity (`abyssal_torch`) where necessary for compatibility, but present the new Moai model/behavior. Distinguish tested current code from planned final refinements.

## 3. Immediate next steps and regression boundaries

1. **Checkpoint safety:** retain the latest project source/asset ZIP as the authority for any code patch. Do not claim that this roadmap itself modified the mod.
2. **Ring:** verify GUI/front orientation and full enchantment interactions in combat; preserve the now-working flight visuals and dynamic reach-based ellipsoid.
3. **Moai:** verify full icon pose, underwater light emission, five-block radius, faction exceptions, zombie horse push, overlapping fields and two-block placement/breaking.
4. **Compiler/IDE:** IntelliJ restart and Problems panel inspection; distinguish compiler warnings from IDE inspections. Run a game client regression for affected network/event-bus/fluid/trap paths after the warning cleanup. **Never hide warnings with `@SuppressWarnings`.**
5. **Watergate planning:** build the central gate, seven ruined houses, four bubble-glass trail modules, jukebox and Moais in a test world; save structure NBT and choose whole-template versus jigsaw assembly after measuring footprint.
6. **Watergate implementation (future):** worldgen in the two Deep Ocean biomes; music-disc playback detection; animated cyan portal renderer and particles; server-side activation/teleportation; underwater structure collision/placement tests.
7. **Preserve V13 Abyss commitments:** Y=200 sea surface, 75/100-block depth limits, three ocean strata, Sub-for-One one-chomp survival, Axoleviathan/Axokraken alliance, Axoluthu worshippers and boss progression, Cosmo-Bow and Cosmic Trident. None are implicitly implemented by today's planning.

---

# Preserved V13 master roadmap and complete V1–V12 historical appendices (unchanged)

# Temple of the Axolotl — Master Development Roadmap V13

**Updated:** October 27, 2026 — V13 Abyssal Moai, Watergate, ocean strata, pressure, Mer-Axolotl villages and Sub-for-One planning; preserves V12 combat and cosmic weapons  
**Platform:** Minecraft 1.21.1 · NeoForge 21.1.259 · Java 21 · GeckoLib 4.7.1  
**Mod ID:** `temple_of_the_axolotl`  
**Authority:** User-confirmed plans and screenshots through October 27, including the V12 baseline and later Abyss brainstorming. **No new source ZIP, build, or complete in-game test was supplied for this V12 update.** V11 and its historical V1–V10 appendices are preserved verbatim below. Latest explicit decisions supersede older active plans, but do not erase history.

> **READ FIRST — V12 STATUS INDEX.** **USER-TESTED** = user observed/approved behavior; **DELIVERED/CANDIDATE** = patch discussed or supplied, not necessarily compiled or verified; **DESIGN LOCKED** = agreed specification, not implemented; **PLANNED** = future work; **CUT** = removed from active scope. Do not claim a source patch is complete from roadmap text alone.

## 0. V13 consolidated docket index — V1 through V12

| Classification | Dockets / features | Current evidence and next boundary |
|---|---|---|
| **Completed / user-confirmed** | V1–V6 core Temple dimension, natural Temple generation, 10-floor dungeon with 802 modules at 3/tick, portal lifecycle and reset paths | Retain V11/V10 test boundaries; no fresh dedicated-server certification. |
| **Completed / user-confirmed** | V4–V5 Celestial armor model/visuals and innate abilities; V5 pounce movement; V6 Sentinel names/drops, Trap Stone opening/stonecutting/fire immunity, Aegis-trident exceptions | Preserve known-working paths; broader regression still open. |
| **Completed / user-confirmed** | V6–V9 Repository backend (56 distinct variants, 32 native stacks each, deposit/withdraw/rejoin), left-side R/L/H shortcut layout | Existing features stay; search/sort remains cut. |
| **Completed / user-confirmed** | V8–V9 Brewing Stand and Anvil visual/model checkpoints; V10 Fallen Wrath model shown in game; V11 Ring of the One chakram combat declared complete | Visual approval and Ring combat approval do not certify every mechanic or asset link. |
| **Completed / user-reported** | V11 Above All / tameable Wrath animation freeze and friendly-fire fixes; Boss Wrath Phase 3 pursuit/pounce/shockwave candidate approved | Keep hostile BossWrathEntity separate from TameableWrathEntity. |
| **In progress** | V10–V12 Fallen Boss Wrath AI, crown interactions, hitbox, laser origin/envelope, animations, balance, Sanctum | Experimental head hitbox was abandoned; current direction is one enlarged regular entity hitbox, now fitted closer to the body. See §1. |
| **In progress / needs verification** | RNGesus Curse apparition overlay, Celestial Modules/Cluckles/Magnet, Jukebox pause/audio routing, workstation harvest/recipes, dungeon loot/extraction, Sentinel polish, asset wiring | Eye-overlay design supplied; confirm actual transparent PNG, 50% render opacity and curse-only trigger in game. |
| **Planned** | Shards → Orbs → Key; Origin Temple/Origin Wrath/Above All rescue; companions; Patchouli diary/monoliths; Second Fall; compact HUD; compiler cleanup | Retain all existing active scope and earlier acceptance criteria. |
| **Planned — V12/V13** | Abyss dimension, Abyssal faction, Axoleviathan, Axokraken, Axoluthu and worshippers, Cosmo-Bow and Cosmic Trident | **Concept/design only**; no new Java implementation or worldgen confirmed. See §2. |
| **CUT — V9** | Separate Ambrosia fluid; Repository search/filter/sort; standalone multiplayer/rejoin/restart regression docket | Do not reactivate. Existing persistence/multiplayer support remains. |
| **LATER — V9** | Optional Create compatibility Easter eggs | Not a near-term dependency. |

## 1. Fallen Boss Wrath — October 27 follow-up and supersession

### 1A. Main collision box — latest chosen direction

- **ABANDONED:** Experimental secondary head hitbox and animated head-corner tracking. The user supplied a `Head Hitbox` pivot, then eight corner locator bones under `Head Hitbox Corners...` in the `Head` hierarchy; multiple attempted Java hitbox positions did not align in game. These bones are historical references, **not** an active requirement to spawn a second hitbox.
- **CURRENT:** Use **one ordinary entity collision box**, resized to approximate the entire main body. The user first requested roughly **3×** the previous hitbox, then began tuning actual dimensions. The latest explicitly pasted entity registration was `.sized(3.0F, 1.5F)`; later screenshots showed a larger bounding box over the body, but **no final numeric dimensions were confirmed**. Do not infer final measurements from screenshots or silently restore a tall 3× box.
- Hitbox remains Minecraft's entity-aligned bounding box; a standard AABB does **not** pitch/roll with GeckoLib animation. Prioritize playable reach and body coverage over unverified bone math. Screenshot-based fit is an iterative test, not a final collision certification.
- Preserve the **separate crown weak-point concept** as a distinct *planned/verification* item only if still desired; dropping the secondary **head** hitbox does not by itself confirm that the crown hitbox was implemented or removed.

### 1B. Laser and combat

- V11's Phase 3 and Phase 4 descriptions remain historical baselines. Phase 5.3–5.5 and subsequent hitbox experiments were discussed; **do not assert successful compilation or completed bone-following hitboxes** from the discussion.
- **Requested laser outcome:** visible Warden-style sonic envelope around the main laser, actual damage aligned with beam, **stacked damage and no knockback**. This is a later design request that supersedes V11's earlier “once per target per firing” intention **for the requested new laser behavior**; exact stack cadence/damage and game-tested implementation remain unresolved.
- Preserve the established charge/attack timing and boss movement unless a newer verified code change explicitly supersedes it: 80-tick charge sequence, pounce-only 9-block ripple, normal pursuit rather than pounce spam, hit-based Rampage transitions. Laser animation root scale and bone rotations must not be confused with collision dimensions.
- Existing boss stats target **30 armor / 20 toughness** and lead/boat immunity; user reported manually correcting an `AbstractBoat` class-reference compile error. Treat source state as user-reported, not freshly audited.

### 1C. RNGesus Curse overlay

- Replace the previous curse apparition with **Fallen Boss Wrath's red eyes**. User supplied a revised red glowing eyes image; black surrounding pixels should be **transparent**, with the overlay displayed at **50% opacity**, reminiscent of the Elder Guardian apparition.
- This overlay belongs to the **successful RNGesus Curse trap** sequence, not ordinary dungeon entry. Preserve V10's successful trap → teleport → persistent effect → apparition after 20 ticks condition.
- Art was supplied and overlay patch discussed; **final source integration, transparency and in-game visual timing not independently verified** in this roadmap.

## 2. Abyssal Dimension Docket (V12 baseline + V13 additions, planned)

**Theme:** A deep-ocean Abyss realm ruled by the axolotl-Cthulhu sea king **Axoluthu**. Its aquatic enemies belong to the common **Abyssal faction/classification**, analogous to the Illager classification for raids. Faction affiliation is a design rule; faction-specific AI, tags and combat targeting are not yet implemented.

### 2A. Abyssal creatures and faction

| Creature / group | Locked concept | Status |
|---|---|---|
| **Axoleviathan** | Enormous axolotl-inspired ocean predator; detects and hunts players and player boats even from around **80 blocks underwater**. During boat-destroying chomp, **head and part of neck breach** the water, megalodon-style. **1,024 HP, 30 armor, 20 armor toughness**; intentionally small/hard-to-hit collision target (exact dimensions TBD). Killable. | **Planned / design locked**, no AI or hitbox implementation confirmed. |
| **Axokraken** | Axolotl head and upper torso, with **tentacles instead of lower torso/tail**. Allied with Axoleviathan; both target players rather than hunting each other. When one Axokraken dies, **another immediately spawns within 30 blocks** of its death location. | **Planned**; spawning guardrails and dimension limits TBD. |
| **Axoluthu worshippers** | Mermaid–axolotl hybrids who worship Axoluthu and share the **Abyssal** faction. | **Planned**; detailed AI/stats/drops TBD. |
| **Axoluthu** | Sea king, an axolotl-Cthulhu-style major boss. **Cannot be damaged by any attacking weapon except the Cosmic Trident.** Killing an Axoleviathan provokes him and unlocks his fight. On his death, **five Axoleviathans spawn and actively hunt the player** as his final wrath. | **Planned**; boss HP, arena, other environmental damage rules and implementation TBD. |

**Axoleviathan ↔ Axokraken alliance:** They cooperate in hunting players; the leviathan does **not** consume or attack allied Axokrakens as an ordinary mechanic, despite the leviathan's imposing size. No boat is required for leviathan targeting: swimmers are valid targets too.

**Important spawning design note:** “One Axokraken immediately replaces a dead Axokraken within 30 blocks” is the intended gameplay rule. Define safe placement, chunk loading and anti-runaway spawn safeguards when implementing, without silently weakening the user's intended persistent threat.

### 2B. Abyssal celestial weapons

| Weapon | Design | Status |
|---|---|---|
| **Cosmo-Bow** | Draw to fire a **small Fallen Wrath-style laser** without consuming or requiring arrows. One laser **impact** modeled on a **fully drawn Power X bow shot plus Instant Damage II tipped-arrow effect**; not a continuous multi-tick damage stack. Beam may remain visible briefly after its one damaging hit. Exact armor/magic/undead interaction and unified damage implementation need decisions. | **Planned / design locked**. |
| **Cosmic Trident** | The **only attacking weapon that can damage Axoluthu**. Crafted from **four vanilla Tridents, four Orbs of Envy and one Heart of the Abyss**. Thrown versus melee use and further powers remain unspecified. | **Planned / design locked**. |

**Cosmic Trident crafting grid (3×3):**

| | | |
|:---:|:---:|:---:|
| Vanilla Trident | Orb of Envy | Vanilla Trident |
| Orb of Envy | **Heart of the Abyss** | Orb of Envy |
| Vanilla Trident | Orb of Envy | Vanilla Trident |

**Heart of the Abyss:** **Guaranteed drop from every killed Axoleviathan**, including any Axoleviathans spawned by Axoluthu's wrath unless a later decision explicitly changes it. The Heart is the central crafting ingredient for the Cosmic Trident. No additional drop rate or recipe ingredients should be invented.

### 2C. Abyssal progression and encounter order

1. Explore/survive the Abyss and its hostile Abyssal faction.
2. Defeat an Axoleviathan, receive **one guaranteed Heart of the Abyss**, and **provoke Axoluthu**, enabling the boss confrontation.
3. Gather four vanilla Tridents and four Orbs of Envy; craft the **Cosmic Trident** using the grid above.
4. Fight Axoluthu using the **Cosmic Trident**, the sole authorized attacking weapon.
5. When Axoluthu dies, **five Axoleviathans spawn** and actively pursue the player.

**Open design details, not secretly settled:** Abyss access/portal and worldgen, exact faction registry/tag structure, Axoluthu health/arena, how the five post-boss leviathans spawn safely, Axokraken replacement persistence, Cosmo-Bow cooldown/damage integration, Cosmic Trident combat abilities beyond boss vulnerability, recipes for the Cosmo-Bow, and whether any nonweapon environmental damage bypasses Axoluthu's protection.

## 2D. NEW V13 — Abyssal Moai and Watergate groundwork (October 27, 2026)

**Status: DESIGN LOCKED / PLANNED, except specifically reported Phase 6.2.9 test outcomes.** The Abyssal Moai begins the **Phase 6.3.x** series, distinct from the Temple dungeon's own architecture. It is early standalone infrastructure intended to decorate the **future Abyss Watergate portal** once the Abyss dimension and physics-based water are implemented. The exact Watergate activation/traversal mechanic is not decided.

- **Abyssal Torch → Abyssal Moai:** Reimagine the interdiction-torch-style block as a **two-block-high, one-block-footprint**, floor-placed Moai-head guardian, with sculk-like dark texture, cyan veins, glowing purple eyes and a purple ethereal flame. Decorative architecture rather than torch spam. Face with placement direction; place/remove both halves as one structure and drop only one item. The bottom half alone runs repulsion to avoid duplicate forces.
- **Repulsion:** Intended **5-block radius** (not five-block diameter), pushing hostile and currently aggressive neutral-hostile mobs away while leaving peaceful mobs, players and pets unaffected. Avoid vertical launches, wall clipping and stacked repulsion force when fields overlap. Effect stops upon removal.
- **Phase 6.2.9 test handoff:** Basic placement, peaceful/player exclusions and movement stability were reported PASS. Range was FAIL (approximately 3-block range / 5-block diameter), aggressive Enderman repulsion was FAIL (insufficient force), removal behavior was NOT TESTED. These are Phase 6.3.x fixes, **not** completed work.
- **Implementation notes for future validation:** Two-block synchronized placement/breaking, correct orientation, item-drop uniqueness, collision, repulsion strength and actual measured radius, neutral hostility detection, server performance, and field-overlap behavior.

## 2E. NEW V13 — Abyss ocean world generation and pressure

**Design direction:** An almost entirely aquatic, vertically layered Abyss with **ocean surface at world Y=200**. Include underwater valleys, deep trenches, underwater volcanic/hydrothermal vents, cliffs and bioluminescent features. Exact world-height bounds, seafloor profile, biome boundaries, terrain generation and presence/absence of islands are **not yet locked**. Water physics is an explicit next major workstream after the Moai, and should be designed before submarine implementation; no water-simulation architecture has been chosen.

**Pressure thresholds are depths measured BELOW the Y=200 water surface, NOT negative world Y coordinates:**

| Exploration equipment | Depth below sea level | Equivalent world coordinate | Intended limit |
|---|---:|---:|---|
| Unprotected player | 75 blocks | Y=125 | Cannot safely descend beyond this depth without protection |
| Protected diver / diving gear | 100 blocks | Y=100 | Gear extends safe range; deeper exploration needs a submersible |
| Sub-for-One | Deeper than 100 blocks | Below Y=100 | Required for crush-zone exploration; ultimate hull depth rating TBD |

The exact onset/damage progression of pressure crushing, protection item stats and pressure-simulation model remain **undecided**. Ocean strata are conceptually **upper ocean, deep ocean and crush zone**; their exact ecological boundaries need not coincide with the 75/100-block equipment thresholds. Do not invent biome boundary coordinates.

## 2F. NEW V13 — Three-layer ecosystem and settlements

| Region | Inhabitants and encounters | Design direction |
|---|---|---|
| **Upper Ocean** | **Mer-Axolotl Axoluthu worshippers** | Underwater **prismarine villages**, visually resembling small floating bubble-shaped clusters of architecture, suspended in open water. **No actual enclosing glass bubble or air dome** (explicitly not Sandy's dome). Keep worshippers in the existing Abyssal faction. Village population, trading, aggression, architecture and exact altitude TBD. |
| **Deep Ocean** | **Axokraken** | Principal domain of the Axokraken, among underwater valleys and more dangerous waters. Preserve existing Axokraken–Axoleviathan alliance and its on-death replacement rule from §2A. |
| **Crush Zone** | **Axoleviathan** | Primary habitat/hunting grounds of the Axoleviathan, though it **can traverse all three ocean layers** and hunt outside its home depth. Retain existing ~80-block underwater detection, stats, boat/player targeting, Heart drop and Axoluthu progression from §2A–2C. |

The three-layer ecology adds habitat preferences; it **does not replace** the prior creature AI, boss relationships, faction membership, drop tables or weapon progression.

## 2G. NEW V13 — Sub-for-One solo exploration submarine

**Planned, not implemented.** A **single-player exploration submersible**, not a walkable multi-room ship, developed after water physics and diving/pressure systems. Its purpose is to make the crush zone explorable while preserving the danger of abyssal predators.

- **One-chomp survivability:** A pristine Sub-for-One withstands **exactly one direct Axoleviathan bite**. A second direct bite before repair causes critical hull failure / loss of operability; precise failure, escape, recovery and repair rules TBD. Implement as a deliberate per-bite survivability rule rather than relying on unspecified generic hitpoint numbers.
- **Predator behavior:** Axoleviathan is **both territorial and an active/opportunistic submarine hunter**. It can patrol, detect and pursue the sub outside its home zone. Precise pursuit duration, detection cues and bite timing remain open.
- **Proposed vehicle capabilities, not yet locked:** Depth gauge using `depth = max(0, 200 - worldY)`, propulsion, buoyancy/ballast, headlights, sonar, breathing supply, hull-status warnings, and emergency ascent. These are candidate design ideas, **not** confirmed implementation requirements or fixed stats.
- **Development dependencies:** Water physics and stable fluid/vehicle interaction → pressure and diving gear → single-seat submersible → predator/sub combat balance. Validate buoyancy, collisions, underwater control, depth limits and performance before promising full physical-fluid simulation.

## 2H. NEW V13 — Abyss expansion sequencing and unresolved decisions

1. **Phase 6.3.x:** Finish Abyssal Moai two-block model, placement and repulsion fixes; keep it usable before the Abyss dimension exists.
2. **Next:** Research/prototype physics-based water and interactions with worldgen, players, mobs and moving vehicles.
3. **Next:** Build diving equipment and below-sea-level pressure/crush mechanics, with 75/100-block protection thresholds.
4. **Next:** Develop Sub-for-One and balance it for exactly one Axoleviathan chomp.
5. **Later:** Build the Abyss dimension's Y=200 ocean, trenches, vents, floating prismarine villages, three-layer creature distribution, Abyss Watergate, and existing Axoluthu/Cosmic Trident campaign. The ordering among these later subprojects is not final.

**Still open:** Watergate operation, water physics feasibility and performance, custom world height, terrain and biome depth boundaries, pressure damage timing, diving equipment recipes, vehicle controls and materials, hull repair/recovery, village generation and worshipper AI, and final creature spawn balance. Preserve V12 §2A–2C as the source of truth for already specified creature stats, alliance, drops and Cosmic weapons.

## 3. V13 priorities and preservation rules

1. **Immediate Fallen Wrath:** test the single resized entity hitbox, preserve verified pounce/pursuit behavior, finish/verify laser visual envelope and stacked hit behavior, and verify curse-eye overlay alpha/opacity/trigger.
2. **Existing V11 open work:** boss arena/crown/progression, workstation harvesting, brewing bank, Jukebox routing and true Pause/Resume, Modules/Cluckles/Magnet/Repository, dungeon loot and extraction, companion regressions.
3. **New Abyssal design backlog:** Phase 6.3 Abyssal Moai, physics water, diving pressure, Sub-for-One, Y=200 ocean layers, Mer-Axolotl prismarine villages, Watergate, creature worldgen and spawning, Axoleviathan/Axokraken/Axoluthu, Cosmo-Bow and Cosmic Trident.
4. **Longer term:** Origin campaign, lore monoliths, optional Second Fall, HUD, warning cleanup, survival playthrough. **Do not reactivate V9 CUT features** or elevate deferred Create Easter eggs.
5. **Source of truth:** Latest user decisions take precedence; preserve original V11 text below unchanged for traceability. V13 documents requirements and reported observations; it is **not** a build report.

---

# Preserved V11 master roadmap (including complete V1–V10 historical appendices; unchanged)

# Temple of the Axolotl — Master Development Roadmap V11

**Updated:** October 26, 2026 — Ring of the One chakram combat completion, companion animation fixes, Fallen Boss Wrath Phases 1–4 and laser-locator checkpoint  
**Platform:** Minecraft 1.21.1 · NeoForge 21.1.259 · Java 21 · GeckoLib 4.7.1  
**Mod ID:** `temple_of_the_axolotl`  
**Status authority:** User-reported gameplay results and decisions through October 26, 2026. This document is **not** an independent audit of the latest Java sources, a Gradle build certificate, or a comprehensive in-game regression report. The original V10 document, including its full V1–V9 history, follows unchanged below.

> **READ THIS FIRST — V11 SUMMARY DOCKET.** The V11 sections below supersede earlier *status labels and active plans* when they conflict with the latest user-confirmed results. Preserve the V9 CUT decisions: no separate Ambrosia, no Repository search/sort, no standalone multiplayer/rejoin/restart regression docket; optional Create Easter eggs remain LATER. **Delivered ≠ compiled ≠ user-tested.** Do not treat proposed features or prior assistant patch descriptions as proof of functioning source code.

## 0. V11 executive summary docket — current project state

### 0A. Newly completed / user-tested since V10

| Workstream | Current status | Evidence, behavior and boundary |
|---|---|---|
| **Ring of the One — chakram combat** | **USER-TESTED: combat behavior declared complete** | Holding Attack repeatedly throws a visible Ring on a yaw/pitch-aligned elliptical boomerang trajectory; damage is restricted to the swept perimeter rather than a broad area; hand mirroring, outbound/inbound motion, player-relative homing return during strafing/running, and attack-speed scaling were iterated and user-confirmed. Haste was explicitly used to verify attack-speed variation. User declared the chakram implementation complete after Phase 6. |
| **Ring of the One — Combat/Mining toggle** | **User-approved integrated design / Phase 5 candidate** | Pickaxe-icon Mining toggle sits below Magnet in Celestial Modules GUI; combat-versus-mining control was bundled with the Phase 4.1 speed fix. Preserve the GUI placement and ensure holding Attack only initiates chakram combat when combat mode is active. The final user-tested Ring behavior is authoritative; independently inspect source before claiming every toggle edge case passes. |
| **Companion animation / friendly-fire fixes** | **USER-REPORTED FIXED** | Above All and tameable Wrath animation freeze caused by inappropriate hold-on-last-frame settings was corrected; standing/landing transitions and intended friendly-fire protections were user-reported working. Above All is protected from player-attributed friendly fire; Wrath's corresponding protection applies once tamed. Keep companion code and animation JSON separate from boss-Wrath edits. |
| **Boss Wrath Phase 3 combat test** | **USER-APPROVED checkpoint ('perfect')** | User approved the delivered pursuit/pounce/shockwave/laser patch. Walking to targets instead of pounce-spamming and restricting the 9-block shockwave to the pounce were the principal fixes. This approval does not independently certify every Rampage damage formula, renderer path, or long-run encounter edge case. |
| **Boss Wrath horn-locator geometry** | **USER-SUPPLIED and locator names confirmed in prior review** | Four empty horn-tip locator bones plus the focus locator were added to the Blockbench geometry after correcting an accidental cube-instead-of-bone mistake. Exact locator names: `Left Horn Tip A`, `Left Horn Tip B`, `Right Horn Tip A`, `Right Horn Tip B`, `Laser Focus Point`. |

### 0B. Boss Fallen Wrath — combat specification and Phase 4 checkpoint

**Separate entities:** `BossWrathEntity` is the hostile Fallen Boss Wrath; `TameableWrathEntity` is the redeemed companion. Do not conflate their animations, targeting, immunities or behavior.

| System | Latest locked behavior | Current verification boundary |
|---|---|---|
| Target selection / pursuit | Boss walks/pathfinds toward the next target when out of melee range. Cows are intentional natural enemies for V.B.T.S. combat testing. Pounce is an occasional heavy attack, **not** the default travel method. | Phase 3 delivered; user called it perfect. |
| Ordinary contact / headbutt | Close-range normal melee/nibble may occur on contact; headbutt damage should land at the **end** of its animation. **Neither** normal collision nor headbutt spawns a shockwave. | Phase 3 delivered; preserve independent attack states and animation-end guards. |
| Heavy pounce | Distinct heavy pounce with separate cooldown; at the animation's end, trigger **one** horizontal, terrain-following **9-block-radius** ripple. Shockwave has **1.25-block-tall** collision ring, damages targets but adds **no scripted knockback**; wall obstruction does not block it. | Phase 3 delivered; inspect actual animation synchronization and block-display renderer in source before claiming exact geometry. |
| Normal → Rampage | **10 successful hits exchanged**: a successful boss attack **or** a successful hit received by the boss advances the counter. | Design locked; test hit-event accounting and crown interaction. |
| Rampage → laser | **3 successful hits exchanged**, including hits **received** by the boss; boosted attack damage and shorter cooldowns. A multi-target shockwave should count as one successful outgoing attack action, not once per cow. | Design locked; actual boosted ordinary-melee damage was a known earlier gap—verify in current code. |
| Laser charge | **80 ticks / 4 seconds**. Ticks 0–39: four horn-tip energy spheres form and tractor pull is active. Ticks 40–59: four narrow horn beams converge at a focus point in front of central crystal; focus sphere grows; pull continues. | Phase 3 visual candidate delivered; Phase 4 addresses origin placement. |
| Dodge window | Ticks **60–69**: tractor pull **stops**, central sphere condenses and brightens, target has exactly **0.5 s** to evade. Firing direction is locked rather than retargeting a dodging player. | Design locked; verify animation alignment and target lock in source. |
| Main laser | Ticks **70–79**: **one thick damaging beam** fires from central focus point along locked direction, penetrating walls. Four feeder beams are visual-only; damage applies to eligible targets intersecting main beam, intended once per target per firing. At tick 80, reset to Normal. | Phase 3 delivered a denser **particle-based** laser, not a particle-independent beam renderer. Visual/damage alignment needs game test. |
| Crown vulnerability / boss scaling | V10's preferred separate crown hitbox (+2 Rampage counter on crown hit), 250/500/750 HP by difficulty, decoy 2.5×→3.0× scale, 30 armor/20 toughness and boat/lead immunity remain active specifications. | **Do not** mark crown hitbox or anti-execution protections implemented without source/test evidence. Preserve the older V10 caveats. |

**Laser origin geometry, Phase 4:**

| Locator bone | Parent bone (from previous geometry review) | Purpose |
|---|---|---|
| `Left Horn Tip A` | `Center` | Left horn emitter A |
| `Left Horn Tip B` | `Center4` | Left horn emitter B |
| `Right Horn Tip A` | `Center3` | Right horn emitter A |
| `Right Horn Tip B` | `Center2` | Right horn emitter B |
| `Laser Focus Point` | `Center5` | Convergence sphere and thick beam origin |

**Phase 4 status:** A two-file `BossWrathEntity.java` + `boss_fallen_wrath.geo.json` horn-locator savepoint patch was **reported delivered** and positively received. It uses locator positions transformed from the model's **rest pose** and Wrath's entity position/facing; it does **not** yet track animated GeckoLib bone positions every frame. Final live-bone attachment, animation-induced drift, actual beam start positions, and source compilation remain unverified. Do not describe this as full live GeckoLib tracking or a fully validated renderer.

### 0C. Current completed foundation — retained from V10

| Workstream | Retained status / caveat |
|---|---|
| Temple world generation and lifecycle | Natural Overworld Temple, shared Temple dimension, 10-floor dungeon with 802 modules placed at three per server tick, demolition/reset/extraction paths were user-tested in prior checkpoints. |
| Celestial armor and visuals | Four-piece set, innate/passive powers, chibi helmet, robe panels and segmented tail were user-reported complete; interaction and edge-case regression remain. |
| Repository | 56 data-component-distinct variants, 32 native stacks per variant, server-authoritative deposit/withdrawal and user-tested rejoin persistence. |
| Sentinel / Trap Stones | Sentinel names and drops, intact→cracked Stonecutter conversion, bulk opening, fire immunity and trident/Aegis exceptions were user-tested; eight textures and Enchantment Book lottery remain verification tasks. |
| GUI / Modules / Jukebox / Magnet | Approved Modules layout, nine-disc vertical rack to left, R/L/H left-of-armor shortcuts, Magnet controls and conditional Cluckles tab workstream; individual playback, routing and sync edge cases remain. |
| Cosmetic workstations | Cosmic Brewing Stand and redesigned Celestial Anvil visuals shown/approved; harvest/drop duplication, bank mechanics, icon wiring and animation still need tests. |

### 0D. OPEN / NEXT ACTIONS — prioritized

1. **Commit/save Phase 4 checkpoint** with matching geometry and Java sources; compile latest build and run a controlled V.B.T.S. test for four horn emitters, convergence focus, beam origin, and damage path. Verify that no horn effect remains centered on the entity's X/Z position.
2. **Optional Phase 4.1 laser fidelity:** replace rest-pose locator calculations with live GeckoLib animated-bone world-space transforms if horn/crown animation visibly drifts; a dedicated beam renderer is still optional/future rather than already implemented.
3. **Boss combat regression:** verify 10-hit and 3-hit counters (incoming/outgoing), crown +2, animation-end headbutt, one-shot pounce-only 9-block ripple, no knockback, pathfinding between targets, boosted melee damage, wall-penetrating laser, exact 0–40–60–70–80 tick sequence and reset.
4. **Boss campaign integration:** Sanctum arena and scaling/clearance at 2.5×–3.0×, 250/500/750 HP difficulty, armor/boat/lead enforcement, anti-execution blacklist, loot, cleanup and Origin Wrath progression.
5. **Other near-term regression:** workstation one-drop harvesting, Cosmic Brewing Stand bank/recipes, updated item sprites, title-menu Music/Master routing versus jukebox Records/Master, Jukebox true Pause/Resume, Modules/Repository/Cluckles/Magnet sync and dungeon loot provenance.
6. **Longer-term campaign and polish:** Shards→Orbs→Key, Origin Temple/Above All rescue, companion mounts and behavior, Patchouli diary/monoliths, Second Fall optional ending, compact HUD, compiler warnings and final survival playthrough.

### 0E. Preservation, supersession and known cautions

- **Ring of the One combat is complete by user report.** Older V10 and historical 'Ring model/boomerang combat planned' entries are superseded. Exclusive Ring enchantments, ability interactions and broader regression remain open.
- **Boss Wrath Phase 4 is a savepoint, not final live bone-tracking certification.** A positive response to the patch is not a recorded full in-game laser validation.
- **Companion Wrath and Above All are distinct from Fallen Boss Wrath.** Preserve the user-confirmed standing animation fixes and friendly-fire behavior.
- **Do not reactivate V9 cuts:** separate Ambrosia fluid, Repository search/sort, or dedicated multiplayer regression docket. Do not remove existing persistence or multiplayer support. Optional Create Easter eggs remain deferred.
- **Newer decisions override older appendix wording** for pounce radius (9), Rampage hit accounting (hits both dealt and received), laser sequence (80 ticks), Ring combat completion, and locator-based beam origins. The preserved historical text is for traceability, not the active instruction set.
- **No code audit in V11.** The current user-provided artifact is the V10 Markdown roadmap, not a fresh complete mod source or build log; source-specific statements about patch contents are based on the preceding patch discussion and should be verified against the actual latest project.

---

# Preserved V10 master roadmap and V1–V9 historical appendices (original text follows unchanged)

# Temple of the Axolotl — Master Development Roadmap V10

**Updated:** October 25, 2026 — V1–V9 unified docket index + Fallen Wrath visual/registration/armor checkpoint  
**Platform:** Minecraft 1.21.1 · NeoForge 21.1.259 · Java 21 · GeckoLib 4.7.1  
**Mod ID:** `temple_of_the_axolotl`  
**Baseline:** V9's reported MegaCheckpoint `08-D1.5.7`; subsequent Fallen Wrath screenshots and patch discussion. **This is a planning record, not a new source audit or compile certification.**

> **Read this first.** This new opening index is the current status guide across **V1–V9**. The complete original V9 (including preserved V8, V7, V6, V5, V4, V3 and earlier history) is reproduced *unchanged* after the V10 update. Later explicit user decisions take precedence over historical plans. **CUT** means removed from active scope, not delete working code. **Delivered** does not mean compiled; **compiled** does not mean tested in-game; **visual pass** does not mean combat logic complete.

## 0. V1–V9 unified docket status — the quick-reference master index

### 0A. COMPLETED / user-tested or user-confirmed (with stated boundaries)

| Docket / era | Feature | Confirmed completion boundary |
|---|---|---|
| V1–V4 core foundation | NeoForge mod, Axo/GeckoLib registration, natural Overworld Temple, shared Temple dimension, portal lifecycle | Working foundation; not full dedicated-server certification. |
| V3–V5 world generation | Ten-floor procedural dungeon, **802 modules at three per server tick**, maze generation announcements | User-tested generation; not full trap/loot/campaign coverage. |
| V3–V6 portal lifecycle | Automatic Temple demolition/deep cleanup, `/templefullreset`, `/tota_devportal`, combat-logout extraction trigger | User-tested specific paths; old `/axo_maze` retired. |
| V4–V5 celestial armor | Chestplate, leggings, boots, chibi Axolotl Absolute helmet; textures, models, passive/innate abilities | Armor set reported complete; final edge-case regression remains. |
| V4–V5 armor visuals | Visible segmented leggings tail, walking robe panels, corrected helmet Blockbench scale/breathing | Visual passes reported; verify final export/renderer across poses. |
| V5 combat movement | Wrath's Pounce horizontal momentum and removal of jump-counter actionbar spam | User-tested. |
| V5 extraction effects | Temple-only persistent effects removed on exit | User-tested; other effects preserved. |
| V5–V6 reward staging | Server-owned reward staging and run binding foundation | Tested foundation, **not** entire extraction/reward economy. |
| V6 Sentinel/traps | Random Sentinel names, Trap Stone drops, individual/bulk Trap Stone opening, Stonecutter intact→cracked, fire immunity | User-tested listed actions; weights, AI, and eight final textures remain checks. |
| V6 Aegis/tridents | Tridents bypass Aegis; Sentinel tridents cannot be picked up | User-tested. |
| V6–V9 Repository | 56 distinct data-component-aware item variants, 32 native stacks per variant, deposits, withdrawals, rejoin persistence | Backend user-tested; broader restart/multiplayer not certified. |
| V6–V9 inventory shortcuts | R/L/H buttons positioned to left of armor slots | User-approved visible layout; individual actions/recipe-book hiding still need checks. |
| V7–V9 menu artwork | Cinematic cosmic/undersea title menu and separate clean child-screen art | Visual result shown; full screen-transition/audio coverage not certified. |
| V7–V9 GUI integration | Modules, Repository, Lootbag, conditional Cluckles tab candidate, left-side vertical 9-disc rack, Magnet controls | User reported successful compile after registration fix; verify control behavior and audio in game. |
| V8–V9 Cosmic Brewing Stand | Custom modeled/textured stand, tesseract, corrected placed geometry | In-game visuals shown; brewing mechanics/drop-count verification outstanding. |
| V8–V9 Celestial Anvil | Custom purple/gold starfield model, hand-painted texture, orientation/scale hotfix | User explicitly approved final positioning; harvest and icon tests remain. |
| V8–V9 item artwork | Cosmic Essence bottle, Ichor buckets, Anvil, Magnet, Jukebox and Brewing Stand art direction/assets | Art prepared/approved, **not** blanket confirmation of final source wiring. |
| V9 review | V1–V8 planning audit and 19 KEEP / 3 CUT / 1 LATER decisions | Planning decisions confirmed, not implementations. |
| V10 (Oct 25) Fallen Wrath visuals | Hand-painted **128×128** Fallen Wrath texture, four wings, glowing red eyes/horns/toes, crown, expressive brows/mouth; in-game spawn/model shown | **Model/texture visibly in Minecraft**. AI/attacks, hitboxes, boss arena and final balance are **not** thereby certified. |

### 0B. IN PROGRESS / implemented candidates needing tests or finishing

| Docket / era | Feature | What remains / acceptance criterion |
|---|---|---|
| V5–V9 dungeon reward economy | 20/4/3 restricted chest slots, rarity weights, Sanctum loot, separate Dungeon Lootbag, exactly-once extraction and failure confiscation | Validate full loot provenance, tier odds, no loss/duplication and success/failure paths. |
| V6–V9 Ominous Trap Stones | Eight intact/cracked orb sprites, 0.1% per-stone Celestial Lottery | Verify eight production textures; finish/verify Celestial Enchantment Book jackpot. |
| V6–V9 Sentinel polish | Guardian corridor flight, armor Silence/netherite trims, ignition and difficulty scaling | Verify all encounter variants and floor/layer formulas. |
| V7–V9 portable Jukebox | Nine vertical disc slots to LEFT, three playable discs, queue/Shuffle/Repeat/Play/Pause | True pause position, transitions, disc removal, source routing and no overlap. |
| V7–V9 menu audio | *The Cosmic and the Seas* menu theme; *Out of Reach* dungeon ambience | Fix Music/Master vs Records/Master sliders; avoid duplicate tracks, stop at correct transitions. |
| V7–V9 Modules/Repository/Cluckles | Conditional employed-Cluckles tab, Magnet ON/OFF, R/L/H and Repository | Confirm state sync, inventory/GUI scaling, employment detection and storage behavior. |
| V7–V9 Magnet | 10-block dropped-item collection to Repository or Dungeon Lootbag | Verify pickup delay, ownership, reward provenance, full destination/overflow. |
| V8–V9 workstations | Cosmic Brewing Stand charge/bank/recycling and both workstation harvest rules | Test 16,000 mB bank, catalysts/heat, single item drop, Ring or diamond-tier pickaxe and contents preservation. |
| V8–V9 assets | Item PNGs, Cache textures, spawn eggs, localization | Verify actual model/definition links, alpha, item names and creative entries. |
| V7–V10 Fallen Boss Wrath | Spawn egg/entity registration and rendered model/animation assets | **In-game visual spawn shown.** Finish AI, attacks, arena, crown mechanics, collision and game testing. |
| V10 armor/transport micro-patch | Fixed **30 armor / 20 armor toughness**; reject leads and boats | Patch was reported packaged/delivered; **compile and in-game enforcement still unverified**. Keep source behavior authoritative until tested. |
| V5–V9 armor/Ring abilities | Eternal Guard, Aegis, Ring lifesteal, exclusive enchantments and Divine Suppression | Run interactions/regression, resolve source-specific attribute discrepancies. |
| V7–V9 Cluckles/Axo | Cluckles controls, employment, item fetch, feather shedding, Axo companion features | Verify every ability and conditional GUI behavior; do not infer from animation screenshots. |

### 0C. PLANNED / active future dockets (retained from V1–V9)

| Workstream | Active scope |
|---|---|
| **Regular Fallen Wrath** | Sanctum encounter; Easy/Medium/Hard **250/500/750 HP**; torn-wing dive/pounce, gravity/Celestial Rupture, Rampage, rewards, blacklist immunities and robust boss collision. |
| **Wrath escalation** | Decoy starts at **2.5×**, +**0.1×** per defeat, caps at **3.0×**; pounce **AOE and damage** scale; other damage/cooldown formulas must be finalized against code. |
| **Wrath collision/behavior** | Main body hitbox and **secondary crown hitbox** (latest favored design); crown hit **+2 Rampage counter**; close-proximity repeated melee damage; prevent boat/lead exploits; protect boss from armor-reduction innate and execution mechanic. |
| **Origin Wrath campaign** | Origin Temple seed `00001`, solo keyholder lock, Divine Suppression, difficulty by personal decoy kills, Origin Sanctum/prison, Above All rescue, forgiveness and ending branches. |
| **Progression** | Four Shards → one Orb of Envy; nine Orbs → one Celestial Key; Ring of the One 3D item and exclusive enchantments; Sanctum clues and loot. |
| **Companions** | Rideable Above All with Gentle Boop™, redeemed tameable Wrath combat/flying, Cluckles/Axo behavior and companion dungeon boundary rules (wolves excepted). |
| **Lore** | Patchouli *Diary of an Axolotl*; peaceful ponds/ruined statues/monoliths, random undiscovered chapter per monolith, depleted/broken state, no scrolls, exact actionbar “The Story Seeps as the Magic Fades...”. |
| **Optional ending** | *The Second Fall* after forgiveness/resurrection: deliberate Ring offering, empowered Origin Wrath in Overworld, midnight and protected blackstone conversion; later than core campaign. |
| **Compact HUD** | `/tota hud` and `/tota config`, configurable anchors/themes/opacity, Pounce/Eternal Guard/effect displays. |
| **Polish** | Wrath curse apparition on successful trap only, Night Vision flicker, boss spawn-egg art, warnings/deprecations, localization, credits, end-to-end survival and performance checks. |

### 0D. CUT, SUPERSEDED or LATER — do not silently reactivate

| Status | Docket / feature | Current decision |
|---|---|---|
| **CUT (V9)** | Dedicated multiplayer/rejoin/restart **regression-testing docket** | Remove as standalone planned task; **do not** disable multiplayer or remove persistence, and still test normal functionality when necessary. |
| **CUT (V9)** | **Ambrosia** as a separate magical fluid | No separate Ambrosia feature. Retain Diluted/Pure Ichor and Cosmic Brewing Stand. |
| **CUT (V9)** | **Repository search/filter and sorting** | Preserve existing Repository and storage; no search/sort feature work. Historical V6/V8 plans are superseded. |
| **LATER (V9)** | Optional **Create** compatibility Easter eggs | Deferred, not near-term. |
| **SUPERSEDED** | Water-cauldron Diluted Ichor crafting | Custom Cosmic Brewing Stand design takes precedence. |
| **SUPERSEDED** | 27 identical dungeon chest rolls | Use restricted **20/4/3** rarity slot groups. |
| **SUPERSEDED** | 0.5% Trap Stone bonus | **0.1%** independent per-stone lottery target. |
| **SUPERSEDED** | Forgiveness cap of ten decoy kills | Latest cap is **15**. |
| **SUPERSEDED** | Old Anvil geometry/icon, old R/L/H location, old title-disc 3×3 crafting | Preserve approved new 3D Anvil, left-of-armor shortcuts, boss-loot title disc. |
| **REJECTED BUG** | Origin Wrath eye apparition on normal dungeon entry | Trigger only after successful curse trap, successful teleport, persistent effect, then 20 ticks. |

### 0E. V1–V9 status precedence and release cautions

1. **User-tested ≠ every edge case passed.** Preserve green-flagged systems and do not downgrade them because an older historical appendix says “planned.”
2. **Art ready ≠ asset wired.** Check `geo`, animation JSON, item textures, renderer, creative entry and localization separately.
3. **Patch delivered ≠ built.** The latest Fallen Wrath armor/transport ZIP has not been confirmed compiled or behavior-tested in the evidence supplied here.
4. **V9 cuts govern active scope.** The historical sections below intentionally retain old tasks, including now-cut search/sort and Ambrosia; they are **not** instructions to revive them.
5. **Current boss visual baseline:** ~**30 Blockbench units tall**, ~**57 units wingspan**, ~**44 units long** at model design scale; user reports the in-game boss appears roughly **2.5 blocks taller** than an earlier expectation. The Sanctum arena is **64×32×64**; test head/crown/wing clearance at **3.0×** rather than assuming it fits from the base screenshot.

## 1. October 25 Fallen Wrath checkpoint — new since V9

**Visuals — user-shown in Blockbench and Minecraft:** The Fallen Wrath boss is a huge dark-crimson four-winged celestial axolotl, with hand-painted mottled gradients (128×128 atlas rather than the usual 64×64), red-hot horn/toe highlights, bright lower-eye gradients that suggest emissive light without requiring emissive rendering, a hovering violet/blue crown, and the deliberately expressive angry eyebrows and `:3` mouth. User showed spawn and model at player scale in Minecraft; that is **strong visual evidence**, not proof of final AI or hitbox behavior.

**Hitbox design:** Latest preferred direction is a **main body hitbox** with a **separate vulnerable crown hitbox** (rather than automatically treating every visible wing cube as collidable). Hitting crown adds **+2 to the Rampage counter**. Verify collision reach, damage routing, boss health synchronization, anti-cheese, and crown motion. This is **design intent**, not confirmed implementation.

**Combat behavior:** Close-contact repeated melee damage, akin to a hostile proximity hazard, is desired. Preserve pounce AOE and damage scaling with decoy scale. Exclude Boss Wrath from armor-reduction innate effects and execution. The exact base attack rate, AOE formula and final Rampage balance still need code-level decisions/testing.

**Armor and transport:** User chose fixed maximum-style armor instead of health scaling. The most recent patch response states **30 armor / 20 armor toughness**, no leashing, no boat riding. Treat this as a **delivered candidate** until the actual files are built and tested. Do not confuse the *base armor* setting with earlier proposed 50 armor / 20 toughness or a health-scaling system; those were superseded.

**Immediate Fallen Wrath checklist:**
- [x] Hand-painted final-direction boss texture and four-wing visual model shown.
- [x] Boss model shown spawned in Minecraft.
- [ ] Confirm armor/transport patch compiles and survives actual lead/boat tests.
- [ ] Implement/verify separate crown hitbox, +2 Rampage counter, repeated contact damage.
- [ ] Confirm blacklist entries for armor reduction and execution.
- [ ] Verify GeckoLib animations, wing/crown movements, target tracking and attack telegraphs.
- [ ] Test boss arena geometry and scaled pounce AOE at 2.5× through 3.0×.
- [ ] Verify spawn egg, creative registration, boss encounter, loot and despawn/cleanup rules.

## 2. V10 prioritized active order

1. **Fallen Wrath immediate:** build and test armor/boat/lead patch, crown hitbox and anti-execution protections; complete combat AI.
2. **Near-term regression:** workstation single-drop harvesting, Brewing Stand economy, item icons, Jukebox sound routing, Modules/Repository/Cluckles/Magnet sync and dungeon loot provenance.
3. **Campaign:** Regular Sanctum → Origin Temple and Origin Wrath → Above All rescue → diary, key progression and reward finalization.
4. **Companions and powers:** Above All ride/boop, redeemed Wrath, Cluckles/Axo, Ring/armor interactions.
5. **World narrative and polish:** monoliths, Second Fall, compact HUD, visuals, credits, performance and compiler warnings; optional Create Easter eggs later.

---

# Preserved V9 roadmap and historical appendices (original text follows unchanged)

# Temple of the Axolotl — Master Development Roadmap V9

**Updated:** October 24, 2026 — V1–V8 planning review decisions
**Baseline:** MegaCheckpoint 08-D1.5.7 completed.zip (user-supplied checkpoint; no new code changes in this document)
**Scope:** Planning decisions only. This document does not certify compilation, game tests, or implementation.

> **Decision precedence:** This V9 decision record supersedes conflicting *active-planning labels* in V8 and its historical appendices. All original V8 text and the V1–V8 planning audit are preserved below for reference. A CUT removes a task/feature from the active roadmap, **not** from existing game code or the historical record. Unmentioned plans remain unchanged.

## 1. Decision summary

- **KEEP:** 19 items
- **CUT:** 3 items
- **LATER:** 1 item
- **Unspecified:** Preserve without reclassification

## 2. Confirmed decisions by workstream

### Core campaign

- [x] **KEEP** — Regular Wrath boss and Sanctum encounter
- [x] **KEEP** — Origin Wrath, Origin Temple and Above All rescue
- [x] **KEEP** — Dungeon loot, extraction and failure consequences
- [x] **KEEP** — Ring enchantments and Shard → Orb → Key progression
- [x] **KEEP** — Patchouli diary and final story reveal

### Companions and powers

- [x] **KEEP** — Finish Above All's rideable companion and Gentle Boop
- [x] **KEEP** — Finish redeemed tameable Wrath and combat AI
- [x] **KEEP** — Verify Cluckles, Axo and companion ownership/behavior
- [x] **KEEP** — Complete armor and Ring ability regression tests

### Existing features to verify

- [x] **KEEP** — Cosmic Brewing Stand charge, bank and recycling mechanics
- [x] **KEEP** — Both workstation harvesting rules and exactly-one drops
- [x] **KEEP** — New item icons and inventory rendering
- [x] **KEEP** — Jukebox sound volume routing and playback edge cases
- [x] **KEEP** — Repository, Magnet and GUI synchronization
- [x] **KEEP** — Dungeon rarity tables and reward provenance
- [ ] **CUT** — Multiplayer, rejoin and restart persistence

### Optional or later additions

- [x] **KEEP** — Scattered lore structures and unlockable monoliths
- [x] **KEEP** — The Second Fall alternate ending
- [ ] **CUT** — Ambrosia as a separate magical fluid
- [x] **KEEP** — Configurable compact Celestial HUD
- [ ] **CUT** — Repository search and sorting
- [ ] **LATER** — Optional Create compatibility Easter eggs
- [x] **KEEP** — Extra cosmetic polish, armor trims and compiler-warning cleanup

## 3. Interpretation and non-regression rules

- **Multiplayer, rejoin and restart persistence:** Remove the dedicated regression-testing item from the active planning docket. Do **not** disable multiplayer support, delete saved data, or reverse persistence that already works. Other kept features can still require normal functional testing when implemented.
- **Ambrosia:** Do not plan a separate Ambrosia magical-fluid feature. This does not alter the existing Diluted Ichor/Pure Ichor distinction or Cosmic Brewing Stand rules.
- **Repository search/sorting:** No longer an active feature request. Preserve the current Repository interface and storage behavior.
- **Create compatibility Easter eggs:** Defer; do not schedule as a near-term requirement.
- **The Second Fall:** Remains a planned optional alternate ending; keep its established story and mechanic details in historical roadmap until implementation decisions change.
- **Lore monoliths:** Keep right-click chapter unlocks, nonduplicate discoveries, depleted/broken state, and “The Story Seeps as the Magic Fades...” message as previously specified; no lore scrolls.
- **Completed versus verified:** KEEP does not imply complete. The approved Celestial Anvil scale/orientation fix is user-confirmed, but workstation drop counts and new icon integration remain on the verification list.
- **Historical preservation:** Entries not expressly named above retain their previous status and details. If older text conflicts with the latest confirmed design (e.g., water-cauldron Diluted Ichor), the later design prevails.

## 4. Current prioritized active docket (suggested order, not a new user decision)

1. **Near-term regression and finishing:** Workstation one-drop/harvest tests, item icons, Cosmic Brewing charge/bank/recycling, jukebox audio, Modules/Repository/Magnet synchronization, dungeon loot weights and provenance.
2. **Campaign completion:** Regular Wrath Sanctum → Origin Temple/Origin Wrath → Above All rescue → diary reveal, progression and extraction/failure consequences.
3. **Companions and powers:** Above All mount/boop, redeemed Wrath combat, Cluckles/Axo behavior, armor and Ring tests.
4. **World and narrative expansion:** Lore structures and monoliths, Second Fall ending, compact HUD, cosmetic polish and warnings.
5. **Deferred:** Create compatibility Easter eggs.

## 5. Historical master roadmap — V8 preserved in full

# Temple of the Axolotl — Master Development Roadmap V8

**Updated:** October 24, 2026 — Cosmic workstations, GeckoLib models, texture overhaul, and in-game hotfix checkpoint  
**Platform:** Minecraft 1.21.1 · NeoForge 21.1.259 · Java 21 · GeckoLib 4.7.1  
**Mod ID:** `temple_of_the_axolotl`  
**Java package:** `com.devaxo.templeoftheaxolotl`

> **Status authority:** V8 supersedes older progress labels where explicitly updated. **User-confirmed in game** means the player demonstrated or explicitly approved the result; **compiled** means a user-reported successful Java build, not a gameplay pass; **asset prepared** means artwork/model files were produced or approved, not necessarily installed in the latest source; **candidate integrated** means a patch was delivered/discussed but source and end-to-end testing are not independently audited; **open** means an issue remains. All V7/V6/V5/V4/V3 history is preserved below, verbatim. The roadmap is not a substitute for the latest full source ZIP.

## 1. October 24 executive checkpoint

| Workstream | V8 status | Latest observation / boundary |
|---|---|---|
| Celestial Modules, Jukebox and Cluckles GUI reorganization | **Candidate integrated; compilation confirmed** | Integrated 08-D1.4.5.2 candidate followed by screen-registration generic type-inference fix. User reported **BUILD SUCCESSFUL** with zero errors and 40 compiler warnings; this alone does not certify every UI control in game. |
| Menu soundtrack volume routing | **Open bug / diagnostic** | With Music OFF and Ambient/Environment OFF, the custom title theme *The Cosmic and the Seas* reportedly continued. Master Volume OFF behavior has not been confirmed; investigate actual playback path and sound category before changing it. |
| Cosmic Brewing Stand | **Custom 3D model/texture integrated and shown in game; final placement correction user-confirmed** | GeckoLib/Blockbench cosmic apparatus with golden central tesseract, dark-purple framework, luminous highlights. Initial base had an apparent ground-visibility/geometry issue; user corrected geometry using an offset tapered cube and later corrected a lowercase asset-name mistake. Final screenshot showed stand placed correctly. |
| Celestial Anvil | **Custom model and textured result user-shown; orientation/scale hotfix user-confirmed** | User hand-built and hand-painted the new purple-and-gold 3D model with a starry cosmic work surface. Initial game placement was short and rotated relative to selection outline; subsequent microfix was explicitly called **perfect**. |
| Workstation mining and drops | **Candidate patched; regression testing required** | Both Cosmic Brewing Stand and Celestial Anvil require the **Ring of the One OR a diamond-tier-or-higher pickaxe** for intended breaking/collection. Brewing Stand initially dropped **two** on break; hotfix work followed, but final single-drop regression is not independently established here. |
| Cosmic Brewing Stand, Essence, Ichor buckets | **Item icon artwork produced / discussed** | Cosmic Essence must depict **liquid cosmos in a bottle**, not a free-floating orb or generic space icon. Brewing Stand and Diluted/Pure Ichor bucket icon designs were approved; four-icon PNG bundle was requested/delivered in the art workflow. Verify that latest files are installed in source. |
| Celestial Anvil item icon | **3D-inspired icon requested; production-file verification needed** | New inventory icon must depict the redesigned physical anvil, not the old icon or a presentation overlay. Confirm a standalone transparent PNG is in the current resource pack and correctly referenced. |
| Celestial Magnet and Celestial Jukebox icons | **Approved designs; standalone PNGs prepared** | Magnet must read as an actual **black hole**, not an eyeball; Jukebox uses approved celestial-music styling. Standalone PNGs and combined ZIP were provided; verify transparency, inventory legibility and project integration. |
| Wrath bosses, lore monoliths, dungeon carryovers | **Open / not superseded** | Preserve all previously agreed combat, companion, loot, progression and lore specifications. Do not mark completed because workstation visuals progressed. |

## 2. Workstation modeling and rendering — completed work and remaining checks

### 2.1 Cosmic Brewing Stand

- Reference direction: square open-frame magical brewing apparatus, dark-purple/pearl/gold construction, illuminated purple/cyan highlights and a **golden rotating tesseract** suspended above the central brewing element.
- The tesseract concept uses **six cubes total**, including **three nested central cubes**, one coherent gold palette, and its own passive animation. Model/animations must remain **GeckoLib-compatible** for export and in-game playback; avoid an ordinary non-animated Blockbench export when animation is required.
- Earlier prototype textures at **16×16** were inadequate. User requested **64×64**, separately painted UV faces rather than arbitrary face placement on a premade picture, with **128×128 permitted if needed** for a model. Correct UVs, avoid z-fighting and maintain a deliberate emissive layer for glowing components.
- User showed the stand in Blockbench and in Minecraft. The first in-world version visually exposed an odd ground gap/visibility effect. User solved it by adding another tapered, offset cube and fixed a separate asset naming/case mismatch. Final in-world screenshot showed corrected placement.
- Preserve brewing behavior, recipe, inventory and animation hooks during future model adjustments. Verify lighting/emissive effects and animation loops in a normal gameplay environment.

### 2.2 Celestial Anvil — new definitive art direction

- The redesigned anvil is **not** the earlier overdecorated automatic reconstruction. The user modeled the replacement in Blockbench: tiered **deep-violet/purple base**, luminous **gold corner protectors**, compact central pedestal with gold cross-like banding, and a broad square anvil head.
- Head has a **dark cosmic-violet work surface**, a **golden perimeter frame**, and raised gold corner blocks. The user hand-painted smooth low-opacity/soft-brush gradients (reported working approach: at most **60% brush opacity, 100% softness** after base coat), then added subtle stars and celestial speckles to the work surface. Keep this nuanced shading, not hard-edged random patches.
- The user clarified that a previously supplied geometry file had merely been **misnamed**: it was the actual **anvil** model. Do not replace it with an invented alternate geometry; use the latest user-supplied geometry and texture as source of truth.
- Initial in-game render was **rotated and too short** compared with the block outline. A later microfix was explicitly approved as **perfect**. Preserve corrected facing, footprint, pivot and scale; regression-test rotation in all four horizontal directions and hitbox/outline alignment.
- Inventory icon must match the new 3D design, with a usable **actual PNG**, not a mockup or overlay. Verify item definitions/models use the updated icon rather than legacy resources.

### 2.3 Mining/collection and anti-duplication acceptance rules

- For **both** Cosmic Brewing Stand and Celestial Anvil, require **Ring of the One** or **diamond/netherite pickaxe** to obtain the block as an item. Other tools should not yield collectible workstation items. Check the game's actual harvest logic and block tags, not just hardness or tooltips.
- **Exactly one workstation item per successful survival break** (unless Silk Touch or other explicitly approved mechanics later change the rule). The Brewing Stand was observed dropping **two**; inspect competing `playerWillDestroy` / `onRemove` / loot-table / block-entity-drop pathways and ensure the item is awarded only once. Do not delete stored contents or duplicate them.
- Verify the same rule for the Celestial Anvil, all orientations, creative breaking, Silk Touch/Fortune, explosions where relevant, and server/multiplayer authority. Preserve the corrected 3D item/world visuals after pickup and re-placement.

## 3. Item icons and texture pipeline

| Item | Final design requirement | Next verification |
|---|---|---|
| Cosmic Brewing Stand | Approved recognizable 3D workstation icon | Check standalone PNG and correct inventory mapping |
| Cosmic Essence | **Bottle containing viscous black liquid cosmos**, speckled with stars; emphatically **not** a black-hole orb | Check latest actual sprite and item model |
| Diluted Ichor Bucket | Approved bucket design, visually distinct from Pure Ichor | Check PNG and inventory mapping |
| Pure Ichor Bucket | Approved bucket design, distinct liquid presentation | Check PNG and inventory mapping |
| Celestial Anvil | New hand-built purple/gold anvil with cosmic starfield head | Replace any old icon; ensure standalone transparent PNG |
| Celestial Magnet | Approved **black-hole** motif, no eyeball appearance | Standalone PNG prepared; confirm transparency and in-game resolution |
| Celestial Jukebox | Approved celestial jukebox/music motif | Standalone PNG prepared; confirm transparency and in-game resolution |

- Treat concept sheets, generated art previews, overlay mockups and extracted icon files as **different deliverables**. The mod requires correctly named actual PNG textures at the expected `assets/temple_of_the_axolotl/textures/item/` paths, with matching Minecraft 1.21.1 item definition/model references.
- For 3D blocks, preserve the separate geometry, texture atlas, GeckoLib animations and block renderer. An inventory icon may be a dedicated PNG or a properly configured 3D item renderer, but must display the **current** model rather than the previous icon.
- Recheck alpha/transparency, icon centering, readability at small inventory sizes, and consistency with the cosmic violet/gold/cyan palette.

## 4. Celestial audiovisual and GUI workstream — current state

- The integrated **08-D1.4.5.2** GUI candidate reorganized the Celestial Modules panel, put **Celestial Magnet ON/OFF** in Modules, and moved **Cluckles controls into a conditional separate side-tab** beside Modules/Repository/Lootbag/X. The Cluckles tab should only appear for a player with an employed, living Cluckles; the source/in-game behavior still requires validation.
- Celestial Jukebox's disc rack is **nine rows by one column on the LEFT** of module slots, opening only when equipped. Preserve **Play/Pause, Shuffle ON/OFF, Repeat OFF/ALL/ONE, Now Playing, Next Up**. In the delivered candidate, Pause/Resume was noted as effectively **restarting the current track**, not true position-preserving pause; improve before declaring feature complete.
- User compiled after a `TempleRewardScreenRegistration.java` generic type-inference repair and reported **BUILD SUCCESSFUL in 4 seconds**, **zero errors**, **40 warnings**. The warning count is a new observed checkpoint, not proof that the original older ~18-warning cleanup list was resolved.
- **Sound bug:** title/menu score *The Cosmic and the Seas* continued playing with **Music OFF** and **Ambient/Environment OFF** while Jukebox/Note Blocks was at 100%. Diagnose whether custom audio uses the wrong `SoundSource`, bypasses Minecraft's SoundManager, or fails to update active instances on slider changes. The title theme should obey **Music + Master**; portable jukebox/discs should obey **Records/Jukebox + Master**. Do not infer the exact root cause until code is inspected.
- Verify custom theme continuity across title and child screens, suppress vanilla menu music only while appropriate, avoid overlap/restart, and stop cleanly on world entry. Check all three discs, loot placements, localization, multiplayer and playback persistence as documented in V7.

## 5. Next session — priority and regression checklist

1. **Verify workstation harvest and anti-duplication**: correct Ring/diamond-or-higher harvest conditions, exactly one drop, no double award, contents preserved, all block facings.
2. **Validate latest asset wiring**: Celestial Anvil icon must match 3D model; Cosmic Essence is a bottle of liquid cosmos; Brewing Stand, two Ichor buckets, Magnet and Jukebox all use approved standalone icons.
3. **Inspect soundtrack routing** and fix Music/Master volume behavior without breaking the Records volume category for jukeboxes; check menu transitions and duplicate playback.
4. **Run integrated GUI tests**: Jukebox 9×1 left rack, queue/controls, conditional Cluckles tab, Magnet toggle, Repository persistence and inventory shortcut behavior; address true Pause/Resume semantics.
5. **Confirm workstation GeckoLib animations/emissive rendering**: golden tesseract animation, UV faces, texture resolution, z-fighting, and anvil orientation/scale across block states.
6. **Resume V7 carryovers**: loot tables and recycling, Cluckles and Wrath spawn eggs, Cache, boss Wrath, lore monoliths, dungeon extraction, Sentinel/Orb/Repository/Magnet remaining tasks and multiplayer regression.

---

# Appendix — Preserved V7 master roadmap (October 23, 2026)

The complete V7 document, including its V6/V5/V4/V3 history, follows **verbatim**. Use V8 above for October 24 updates and current status.

# Temple of the Axolotl — Master Development Roadmap V7

**Updated:** October 23, 2026 — Celestial audiovisual systems / 08-D1.4.5.1–08-D1.5.5.1 planning checkpoint  
**Platform:** Minecraft 1.21.1 · NeoForge 21.1.259 · Java 21 · GeckoLib 4.7.1  
**Mod ID:** `temple_of_the_axolotl`  
**Java package:** `com.devaxo.templeoftheaxolotl`

> **Status convention:** **User-shown** = a screenshot or report demonstrates the visible result; **candidate delivered / integration reported** = code or assets were discussed as patched, but this roadmap does not independently audit the source; **design locked** = agreed behavior/layout, not proof of implementation; **planned** = remaining work. V7 supersedes older *status* labels where explicitly updated, while retaining V6 and its V5/V4/V3 historical specifications verbatim below. Where docket numbers varied in conversation (`08-D1.4.5.1`, `08-D1.5.5.1`, `08-D1-1.5.5.1`), treat the music/UI/loot work as one coordinated active workstream until source/patch naming is reconciled; do not imply separate completed patches.

## 1. Executive checkpoint — what advanced this session

| Workstream | Latest status | Evidence / boundary |
|---|---|---|
| Cinematic main menu | **User-shown visual result** | Custom cosmic/undersea Temple background, deity overhead, themed logo and buttons shown in screenshot. The background-only artwork without logo/buttons was also produced for secondary screens. |
| Kelp/background animation | **Reported restored / visual direction approved** | Keep animated luminous kelp, stars, bubbles and celestial atmosphere; verify animation is active on all supported screens rather than assuming every screen is complete. |
| Secondary screen theme | **Design locked; integration verification needed** | Extend clean background and music through Singleplayer, Multiplayer, Mods, Create World, world preparation/generation and loading transitions. Avoid hiding important progress/error text. |
| Vanilla menu music suppression | **Requested; verify implementation** | Prevent vanilla menu music from overlapping custom soundtrack, without indiscriminately muting gameplay, sound effects or user music settings. |
| Celestial Jukebox module | **Candidate integration discussed; controls remain** | Portable, player-following music; 9-row × 1-column disc storage opens to LEFT of module slots only while module equipped. Playback/GUI exists as an active patch target; end-to-end in-game audio, persistence and multiplayer tests remain. |
| Celestial music discs | **Three tracks and credit/loot specifications locked; candidate disc playback discussed** | `The Cosmic and the Seas`, `Beneath the Surface`, `Out of Reach`. Verify all three discs are registered, textured, audible, and loop/stop cleanly. |
| Repository GUI | **User-shown screenshot** | Celestial Repository screen with item grid, storage counts and Cluckles-related UI visible. Preserve existing tested backend behavior. |
| Celestial Modules GUI | **User-shown screenshot; control positions designed** | Vertical module slots, adjacent jukebox column and open right-hand control area. User annotated desired jukebox controls and magnet toggle; not all drawn controls are confirmed functional. |
| Cluckles own tab | **Design locked** | Separate conditional **Cluckles** side-tab beside Modules/Repository/Lootbag and close X. Show tab only when player employs a living Cluckles; move ALL Cluckles settings into this tab. |
| Cosmic Essence | **Item/recipe integration workstream; retexture planned** | Appearance must be **starry black goo inside a bottle**, akin to an ominous bottle. Confirm registration, localization and creative tab in source/in-game. |
| Boss Fallen Wrath / tameable Wrath | **Still on docket** | Model, animations, combat and Sanctum incorporation remain planned; do not mark completed due to audiovisual progress. |

## 2. Today's deliverables and agreed design

### 2.1 Cinematic celestial menu and continuity

- Main menu visual: upper **starry cosmos** and lower **undersea Temple** with cyan/gold light, luminous kelp, particles, bubbles and floating crystals. **Axolotl Absolute** is the actual pearl/cyan-and-crimson deity with floating tesseract crown, **not** the Dark Absolute airship. Cluckles makes a fishbowl-helmet swimming cameo.
- Main menu shows custom title `The Temple of The Axolotl` and subtitle `The Cosmic and the Seas`. Keep the **background-only clean art** for child screens; do not bake the title/buttons into those screens.
- Child screens to cover: Singleplayer world selection, Multiplayer, Mods, world creation and settings, world generation/preparation, loading/joining world, and transitions back to title. Use appropriate rendering hooks and fallbacks for compatibility; some Minecraft loading stages may not support the same live animation path as ordinary GUI screens.
- Music should carry between related menus **without restarting on every navigation**, then hand off cleanly to in-world sound. Mute/suppress overlapping vanilla menu music only while the custom menu score owns playback. Honor Music volume and mute settings.
- Preserve clear foreground readability, accessible navigation, UI scaling, loading progress, and escape/back controls. Provide graceful fallback when custom renderer is unavailable.

### 2.2 Celestial Jukebox module — portable speaker

- A real **Celestial Jukebox** equippable module with proper item registration, texture, English name and creative-menu entry.
- Smithing recipe: **Cosmic Essence + Jukebox + Nether Star** in the same smithing-style family as the Celestial Anvil; verify exact template/base/addition slot mapping with the implemented recipe and game rules.
- On equip, extend the Celestial Modules interface with a **9 rows × 1 column** music-disc inventory **to the LEFT of the existing module column**; collapse the expansion on unequip. Do not confuse 9×1 vertical with 1×9 horizontal.
- The jukebox is a **player-following portable speaker**, not a stationary world block. The music follows its owner; no unintended global/dimension-wide playback. Persist disc contents and playback state safely, and stop/transition when unequipped or player leaves.
- Controls planned for right-side free space in Modules GUI: **Pause/Play**, **Shuffle ON/OFF**, **Repeat OFF/ALL/ONE**, **Now Playing**, **Next Up**. Separate **Magnet ON/OFF** toggle also belongs in this Modules GUI area, not in Cluckles' tab.
- Clarify queue progression, disc removal while playing, empty slots, end-of-track behavior, pause/resume position, rejoin and multiplayer proximity; avoid two copies of one soundtrack overlapping.

### 2.3 Music library and credits

| Disc display name | Track credit / lore text (preserve as specified) | Placement / usage |
|---|---|---|
| **The Cosmic and the Seas** | `Oceanic Trench - ONECinematicStudios` | Main title/menu theme; **boss shulker/chest loot** rather than crafted disc |
| **Beneath the Surface** | `The Lost City of Atlantis - ONECinematicStudios` | Rare treasure in ordinary Temple dungeon chests |
| **Out of Reach** | `Cosmic Starfighter-Psychronic` | Temple dungeon ambient soundtrack when a player is in the generated dungeon dimension; rare treasure in ordinary Temple dungeon chests |

- The user provided MP3s in a **separate music folder** during the patch workflow. Verify actual source assets are included in the project; the roadmap alone does not embed the audio.
- Music comes from Pixabay tracks attributed to **ONECinematicStudio** and **Psychronic**, under the reported **Pixabay Content License**. Preserve in-game lore credits **exactly as requested** and add clear attribution in a bundled credits/attributions file, including original links and the license applicable at download time. “Copyright-free” is not the same as public domain; preserve license conditions and proof of provenance.
- All three should be real playable music discs, registered with appropriate sounds, durations, models/textures, jukebox behavior, creative entries and localization. Do not reuse the superseded 3×3 crafting recipe; **the title disc is boss loot**.
- Dungeon ambient audio must start only in the appropriate active Temple dimension/session and stop on exit, extraction, disconnect or menu transitions; avoid clashing with jukebox music.

### 2.4 Repository, Modules, Lootbag and Cluckles interface

- User screenshots show a **Celestial Repository** item-storage screen and **Celestial Modules** screen; preserve functional storage, original modules and R/L/H inventory shortcuts.
- Modules screen: keep the vertical installed-module inventory; extend the **jukebox 9×1 column to its LEFT** when equipped. Use the large open area to the right for **Now Playing**, **Next Up**, Pause/Play, Shuffle, Repeat, and Magnet toggle. The magnet toggle belongs here even if the magnet's actual pickup routing is handled elsewhere.
- **Cluckles must have a separate side-tab button** alongside Repository, Modules and Lootbag (plus X close). Show it **only while that player has an employed, living Cluckles**; hide it if no Cluckles is employed. All Cluckles settings, including Follow/Stay, item-retrieval settings and related behavior toggles, go **inside Cluckles' own tab**, not in the Modules page.
- Preserve the distinction between always-accessible Repository, separate dungeon reward Lootbag, equippable modules and Cluckles employment controls. Tabs must work with server-authoritative ownership and survive resizing/reopening.

### 2.5 Items, textures, localization and creative inventory

- Make/register and put into creative inventory: **Celestial Jukebox** and **Cosmic Essence**; ensure display names in `assets/temple_of_the_axolotl/lang/en_us.json` (the usual Minecraft English localization file).
- Add/check proper localized names for **Celestial Cache**, **Celestial Magnet**, **Celestial Anvil**, **Celestial Repository Module**, **Temple Lootbag** and rename the helmet's *display name* to **The Axolotl Allfather** without casually changing registry IDs such as `axolotl_absolute`.
- Cosmic Essence texture: **bottled starry black viscous goo**, ominous-bottle silhouette, cosmic speckles/glints.
- Complete Celestial Cache item and placed-block textures and verify correct inventory/world rendering.
- Finish **Tameable Wrath Spawn Egg** and **Cluckles Spawn Egg** textures; preserve their entity registration and expected spawn behavior.

### 2.6 Loot economy, ichor and progression

- **Boss reward chest/shulker:** `The Cosmic and the Seas` music disc.
- **Normal Temple dungeon chests:** rare `Beneath the Surface` and `Out of Reach` discs; **Cosmic Essence** as an **Epic** option; **Celestial Magnet** and **Celestial Repository Module** as **Epic** options.
- **Vanilla mineshaft chests:** add Cosmic Essence to loot pool. Final weights/chances are not yet specified; avoid inventing probabilities.
- **Diluted Ichor:** implement its cauldron-making method (exact ingredient/interaction recipe to verify/decide), then allow recycling **duplicate eligible celestial loot** in a diluted-ichor cauldron. Each attempt consumes the extra module/item **and the diluted ichor**, with a **10% chance** to recover **one Shard of Divinity**; the other 90% consumes the inputs without a shard. Define eligible celestial items explicitly; prevent exploiting bound/persistent equipment or protected reward provenance.
- Preserve established 20/4/3 restricted dungeon chest slots, tier unlocks, existing Shard lottery odds, separate Lootbag routing, and successful/failed extraction rules. Integrate new loot into compatible rarity pools instead of replacing old tables wholesale.

### 2.7 Wrath and companions — open, not silently completed

- **Fallen Boss Wrath** remains an explicit patch objective: integrate actual Blockbench model, textures, animation controllers, attacks and Sanctum encounter. Include articulated four-wing celestial design, crown/laser movement, pounce and existing Regular/Origin difficulty canon. Verify current source before claiming integration.
- **Tameable Wrath** remains a separate companion model/entity and spawn-egg texture task.
- **Cluckles** recruitment canon: convert one vanilla chicken using one **uncooked rice grain**; one living Cluckles per owner, persistent ownership, named `Cluckles` by default; follow/stay, safe item pickup-and-return, seated feather shedding, special hidden challenge advancements and conditional own GUI tab. Existing implementation status must be verified feature by feature.
- **Dungeon companion restrictions:** companions wait at the prismarine portal boundary, including Cluckles and tameable Wrath; **tamed wolves are the sole allowed exception**. Resume normal following after dungeon exit; preserve sit states, ownership, and unloaded/rejoin behavior.
- Preserve latest agreed decoy scaling: size starts 2.5×, +0.1× per defeated decoy capped at 3.0×; +0.2× base damage per defeat (exact additive/compound interpretation to confirm); attack cooldown reduces 2 percentage points of base per defeat to 20% normal floor, with Rampage capable of 10% floor; Origin damage doubles the **last defeated decoy's** damage before its own modifiers. Resolve the precise Rampage multiplier and scaling formula before implementation.

## 3. Next development session — prioritized implementation and validation

1. **Finish jukebox interaction:** Pause/Play, Repeat OFF/ALL/ONE, Shuffle ON/OFF; Now Playing/Next Up; nine vertical disc slots to left; test track switching, persistent contents, owner-following audio, GUI reopening, and clean playback stop.
2. **Complete interface navigation:** add Cluckles conditional side-tab and put all his toggles there; put Magnet ON/OFF in the free Modules panel area; verify Repository/Modules/Lootbag/Cluckles/X tabs, inventory R/L/H shortcuts and recipe-book behavior.
3. **Finish cosmetic assets and naming:** bottled black-starfield Cosmic Essence texture, Cache inventory/placed textures, Cluckles and tameable Wrath egg textures, proper `en_us.json` display names, **The Axolotl Allfather** helmet display name.
4. **Finish all three discs and music integration:** registration, OGG conversion/assets as needed, duration/loop/credit tooltips, creative entries, title-menu continuity and dungeon ambience. Verify original source files and attribution metadata.
5. **Implement loot integrations:** boss title disc, two rare Temple treasure discs, Cosmic Essence in mineshafts and Epic Temple pool, Magnet and Repository Module Epic pool; confirm rarity weighting and reward provenance.
6. **Implement diluted ichor creation and recycling:** settle cauldron recipe, consume duplicate celestial loot and diluted ichor, independently roll **10%** Shard return, test anti-duplication and multiplayer.
7. **Fallen Boss Wrath:** import model/texture/animations and attack animations, register behavior and integrate Sanctum encounter. Maintain separate Regular/Origin boss rules and companion restrictions; verify all gameplay in a test dungeon.
8. **Carryovers/regression:** Repository search/sort/polish (older 08-D1.5), full Celestial Magnet 10-block routing (older 08-D2), eight orb textures, Celestial Enchantment Book lottery, Guardian navigation/armor trims, boss lootbag extraction, curse apparition and Night Vision flicker; ensure no regressions.

## 4. Validation checklist for next checkpoint

- [ ] All custom menu screens and world-loading paths show correct art without breaking progress UI; soundtrack persists appropriately and vanilla music does not overlap.
- [ ] Celestial Jukebox module equips, exposes exactly **9 vertical disc slots to the LEFT**, and plays three discs without duplication; Pause/Play, Repeat and Shuffle work.
- [ ] Cluckles tab appears only for employed owners; all toggles are in that tab; Magnet toggle is on Modules screen.
- [ ] Names, creative entries, spawn eggs, Cosmic Essence and Cache textures render correctly.
- [ ] Boss and ordinary dungeon loot tables plus mineshaft integration produce the intended rewards at intended rarity.
- [ ] Diluted ichor creation and 10% recycling consume inputs atomically with no exploit.
- [ ] Boss Wrath model, animation and Sanctum behavior pass an actual in-game encounter.
- [ ] Multiplayer, rejoin, restart, GUI scale, inventory overflow and existing dungeon reward rules regressions pass.

---

# Appendix — Preserved V6 master roadmap (October 21, 2026)

The complete V6 document, including its V5/V4/V3 history, follows **verbatim**. Consult V7 above for newer decisions and current progress status.

# Temple of the Axolotl — Master Development Roadmap V6

**Updated:** October 21, 2026 — Patch 08 / 08-D1.4 end-of-day checkpoint  
**Platform:** Minecraft 1.21.1 · NeoForge 21.1.259 · Java 21 · GeckoLib 4.7.1  
**Mod ID:** `temple_of_the_axolotl`  
**Java package:** `com.devaxo.templeoftheaxolotl`

> **Authority and status convention:** This V6 checkpoint overrides older *progress labels* where newer user tests or decisions are recorded, but retains V5/V4/V3 design canon and historical records below. **PASS (user-tested)** means the user reported seeing the feature work in Minecraft; it is not a source audit, multiplayer certification or comprehensive regression test. **Delivered / unverified** means a candidate was provided but the specific feature was not confirmed. **Planned** means no completed implementation is claimed. Distinguish individual verified behavior from completion of a whole subsystem.

## 1. Executive summary — Patch 08 development

Patch 08 advanced the dungeon Sentinel encounters, Ominous Trap Stone rewards, Celestial Lottery, trident behavior, and storage infrastructure. Work then branched into **08-C** (orb texture stage) and **08-D1** (Celestial Repository backend and frontend). **08-D1.4** is the latest player-confirmed inventory shortcut layout. **08-D1.5 is next, scheduled by agreement for the next development session**, not implemented as of this checkpoint. **08-D2** (Celestial Magnet) remains planned.

| Area | V6 status | Confirmed scope / remaining boundary |
|---|---|---|
| Sentinel encounters | **PASS — user-tested for spawning/names and stone drops** | Randomized named Sentinels observed, including Guardian and armored undead; stone drops observed. Exact encounter weights, every floor/layer scaling edge case, trim appearance, and Guardian dry-land navigation require further verification. |
| Trap Stone opening | **PASS — user-tested** | Individual right-click and shift-right-click bulk cracking/loot rolls work. Preserve both. |
| Trap Stone stonecutting | **PASS — user-tested** | Intact → cracked via Stonecutter now works. Preserve tier-matched 1:1 conversions. |
| Trap Stone fire immunity | **PASS — user-tested** | Lava/fire-resistant item behavior confirmed. Preserve. |
| Celestial Lottery | **PASS for Shard reward at old 0.5%; 0.1% delivered, odds not statistically verified** | Tier I was observed awarding Shards; user requested **0.1% flat per stone** thereafter, same odds for every tier and every bulk roll. The alternative Celestial Enchantment Book jackpot remains **pending**. |
| Celestial Aegis / tridents | **PASS — user-tested** | Thrown tridents bypass Aegis reflection and Sentinel-thrown tridents are uncollectible. Preserve reflection of other supported projectiles. |
| Trap Stone visual assets | **Design approved; eight-PNG integration requires follow-up confirmation** | Abyssal-oceanic orb designs (I–IV intact, I–IV cracked) approved. An 08-C orb-texture test ZIP was delivered; no explicit in-game verification of final textures yet. Earlier missing-texture squares were confirmed. |
| Axo Agriculture logger | **Delivered / not independently rechecked** | Noisy Rice Grain/Koji drop-roll logging targeted for removal; preserve drop odds. |
| Dungeon extraction effects | **PASS — user-tested in prior V5 checkpoint** | Temple-only persistent effects removed on exit; preserve non-temple effects. Boss reward lootbag delivery still needs explicit success-path regression. |
| Celestial Repository backend | **PASS — user-tested** | Deposits, quantity listing, withdrawals, and leave/rejoin persistence reported successful. |
| Repository 7×8 GUI | **Delivered / in-game opening and contents need explicit confirmation** | GUI Alpha exists, using server-authoritative storage. Search, sorting, polish remain pending. |
| Inventory shortcut buttons | **PASS — user-tested for positioning/display** | **R / L / H** vertical column shown correctly to the **left of armor slots** in vanilla survival inventory, clear of active potion sidebar. Actual click-through behavior for all three, and recipe-book-open hiding, should be regression-tested separately. |
| Celestial Magnet | **Planned — not implemented** | Equippable 10-block pickup module and destination routing reserved for 08-D2. |

## 2. Patch 08-A/B — Sentinel and trap hotfix record

### 2.1 Sentinel design to preserve

- Ominous Trap Spawner: **5-block spherical detection**, one-time activation saved across reloads; several traps per maze NBT template (design target 3–7). No global cooldown.
- Provisional encounter distribution: **30% Guardian, 30% Skeleton, 30% Drowned, 10% Drowned Jockey**. Validate actual source weights before labeling these verified.
- Individual Sentinel stats scale by **dungeon floor** (1–10), not maze layer: **50 × floor HP** and **+2 × floor attack damage**. Double-spawn chance scales separately by **maze layer**: layer 1 **0%**, layer 2 **25%**, layer 3 **50%**, layer 4 **75%**. Independently roll each spawned encounter.
- Skeleton and Drowned should wear full diamond armor with **Silence trim pattern / netherite trim material** and have no equipment drops; Skeleton netherite sword, Drowned trident. Exact trim visuals still require verification.
- A rare Drowned Jockey has a Guardian passenger; **both entities independently receive full scaled stats** and **each drops its own Trap Stone**. Preserve one stone per Sentinel kill.
- Guardian dry-corridor hovering/fly-swimming and unintended Sentinel ignition are **polish / verification items**, not marked fixed.
- Randomized Apotheosis-inspired Sentinel names are **user-tested**; preserve.

### 2.2 Ominous Trap Stone behavior

- Eight item variants: **Tier I, II, III, IV intact**, plus corresponding **four cracked** variants. Item maximum stack size **8**; Repository capacity for each variant is therefore **8 × 32 = 256**.
- **Stonecutter 1 intact → 1 matching cracked** works in-game; all eight are **fire/lava-resistant**.
- Normal cracked-stone use rolls loot once; shift-use bulk opening rolls independently for each stone. Both methods are **user-tested**.
- **Celestial Lottery:** an independent **0.1% (1 in 1,000) bonus chance per opened stone**, across all tiers. The bonus is separate from ordinary tier loot. Target reward: **one Shard of Divinity OR one Celestial Enchantment Book** (50/50 split provisional unless balance changes). No pity timer. **Only the Shard reward was confirmed at the earlier 0.5% setting**; book reward implementation remains outstanding.
- **Approved artwork:** abyssal/oceanic magical orbs, marine blue → deep violet/magenta tier progression, ornate coral/metallic accents, prominent glowing Roman numeral, and a **red-orange molten fissure through the middle on cracked variants**, while preserving the numeral. Deliver **eight separate transparent-background PNGs** and correct Minecraft 1.21.1 item definitions/models. Latest approved concept image is the art reference; do not treat the composite concept sheet itself as eight production sprites.

### 2.3 Aegis and agriculture

- **Thrown tridents are the Aegis projectile vulnerability**: do not reflect them. Sentinel tridents must have pickup disabled so they cannot be farmed. Both were explicitly confirmed in-game.
- Retain Aegis reflection for other supported projectile types and other armor defenses.
- Remove noisy Axo Agriculture Rice Grain/Koji roll logging while retaining probabilities; no new drop-rate balancing was requested.

### 2.4 Extraction / prior Pass B carryovers

- Preserve temple-only persistent effect cleanup on exit, previously confirmed by user. Preserve unrelated persistent powers, including Eternal Guard.
- **Still verify:** successful boss extraction grants the intended dungeon reward lootbag **exactly once**, no duplicate deliveries, and no rewards lost on exit.
- **Still verify/finish:** Silence/netherite armor trim presentation, Guardian corridor movement, and Celestial Enchantment Book lottery reward. These were previously listed as Pass B carryovers and are **not silently marked completed** by 08-C or 08-D1.

## 3. Patch 08-C — approved orb art / delivered texture candidate

- Eight separate transparent sprites required for production: `I`, `II`, `III`, `IV`, and cracked counterparts.
- User approved the later **abyssal-oceanic** concept with luminous numerals and red lava-like cracks, superseding the earlier generic ominous-orb art direction.
- An **08-C Orb Texture Stage** source candidate was delivered. In-game visual success has **not yet been explicitly reported**. Verify all eight in inventory, Stonecutter and item drops; confirm 1.21.1 `assets/<modid>/items/` definitions, `models/item/` models and `textures/item/` PNG paths.
- Do not alter confirmed stonecutting, lava immunity, item stack sizes or loot-roll behavior while correcting art.

## 4. Patch 08-D1 — Celestial Repository

### 4.1 Backend — user-tested

| Rule | Specification / field-test status |
|---|---|
| Distinct entries | **56 unique item variants**, including data-component differences; same item ID with different enchantments/components must not merge. |
| Capacity per variant | **32 × native maximum stack size**: 64-stack item → 2,048; 16-stack → 512; 8-stack Trap Stone → 256; nonstackable → 32. |
| Overall maximum | **114,688 items** only if all 56 variants normally stack to 64; not a fixed cap across mixed items. |
| Persistence | Server-owned per-player data, retained across saves and respawns; **leave/rejoin confirmed by user**. Death, server restart and multiplayer isolation need extended testing. |
| Deposit | Safe partial/full-stack deposit; overflow must remain with player, never deleted. **User-tested.** |
| Withdrawal | Preserves original item data/components; **user-tested**, including quantity changes. |
| Example field test | 4/56 variants: Tier I cracked stone **8/256**, Tier I intact **89/256**, Prismarine Crystals **2/2048**, Emeralds **109/2048**. |

Development commands from backend alpha (retain for diagnostics while appropriate):

```text
/tota repository
/tota repository deposit_hand
/tota repository withdraw 1 64
/tota repository open
```

### 4.2 GUI alpha and inventory shortcuts

- **08-D1 GUI Alpha:** initial **7 columns × 8 rows** displaying up to 56 variants, item icons and stored counts; server-authoritative interactions. Initial controls: left-click entry withdraws up to one native stack, right-click Repository slot deposits main-hand stack, Esc closes. **Further UI verification pending**; commands remain a fallback.
- **08-D1.3:** first survival-inventory R/L shortcuts placed near the right-side active potion effects and collided visually with them; **superseded**.
- **08-D1.4 — user-approved visual layout:** vertical **R / L / H** shortcut buttons **immediately left of the survival inventory's armor slots**, outside the panel. Screenshot confirms their placement clear of the five active potion-effect panels.
- **R** → Celestial Repository; **L** → Dungeon Lootbag / reward vault; **H** → existing Celestial HUD Modules menu. Three separate destinations; **do not duplicate the HUD interface or merge Repository and Lootbag storage**.
- Buttons should **hide whenever the recipe book is expanded**, reappear when closed, follow inventory position and GUI scale, and not consume equipment/module slots. The recipe-book behavior and three click-through routes need explicit field tests even though layout was approved.
- Repository is **always accessible**, without an equipped module; **Magnet** is a separate equippable module.

### 4.3 Next session: **08-D1.5 — Search, Sorting & GUI Polish** (PLANNED)

1. Add search/filter across item names while preserving stable backing entry identity.
2. Add sort modes (e.g. name, count, item ID) without breaking withdrawals/deposits or causing slot-index desynchronization.
3. Polish the abyssal-blue 7×8 interface, count display, tooltips, empty states and interaction clarity.
4. Validate mouse actions, full storage, data-component-distinct items, server synchronization, reopening, recipe-book hiding, and three shortcut destinations.
5. Keep safe partial-deposit and withdrawal logic authoritative on the server. Do not rely on vanilla `ItemStack` to hold oversized counts.

## 5. Patch 08-D2 — Celestial Magnet (PLANNED, after D1.5)

- Equip as a **Celestial HUD module**, occupying a module slot; show equipped status on HUD.
- Collect eligible dropped item entities within **10 blocks**; respect pickup delay, ownership and other-player protections.
- **Normal world drops → Celestial Repository.** **Eligible dungeon-specific rewards → separate Dungeon Lootbag** according to provenance/routing rules; do not merge storage systems.
- When destination is full, preserve remaining items in the world. No deletion, duplication, unauthorized pickups or bypassing dungeon failure-confiscation rules.
- Test multiplayer, chunk boundaries, dungeon extraction, logout/rejoin and module unequip behavior.

## 6. Other open roadmap items / preservation rules

- The V5 **Origin Wrath curse apparition** trap-only eligibility/timing and artwork correction remains open unless separately user-tested. Do not trigger on ordinary dungeon entry.
- V5 **Night Vision expiration flicker** fix remains unverified; do not silently refresh potion duration.
- Existing **Celestial HUD module interface** is now reachable through planned/implemented inventory shortcut integration, but the older V5 *configurable compact on-screen HUD* specification is a distinct feature; do not conflate them.
- Continue dungeon chest restricted rarity **20/4/3**, Sanctum rules, boss rewards, treasure hallway, Wrath bosses, Ring model, companion progression, Origin Temple, diary and release hardening. The older V5 Stage 01 “inventory submenu TBD” label is superseded **only for the new R/L/H shortcut placement**, not for the entire reward lifecycle.
- Preserve the earlier confirmed temple demolition, developer portal, Wrath's Pounce momentum, dungeon logout ejection, generation announcements and persistent-effect cleanup.
- **Deferred maintenance:** ~18 Java deprecation/compiler warnings (NeoForge event bus and enchantment helper calls) recorded for later cleanup; not an 08-D1.5 blocker.
- **Release regression:** multiplayer ownership, server restart persistence, recipe book interactions, duplicate-prevention, successful/failed extraction reward rules, and fresh-world temple distribution remain necessary.

## 7. Next development session — actionable order

1. **08-D1.5:** Repository search, sorting and UI polish; verify R/L/H click behavior and recipe-book hiding.
2. **08-D2:** Celestial Magnet equipment, 10-block routing, overflow/ownership safeguards and HUD indicator.
3. **Patch 08 carryovers:** Verify final eight orb PNGs render; finish Celestial Enchantment Book jackpot option; confirm Sentinel armor trim/Guardian flight and successful boss lootbag delivery.
4. Continue the larger V5 milestones and full release regression tests afterward.

---

# Appendix — Preserved V5 roadmap (October 20, 2026)

The following V5 document, including its complete V4/V3 historical appendices, is preserved **verbatim**. For progress statuses and decisions made after October 20, consult the V6 sections above first. Older 'planned' labels are historical and do not override newer user-tested results.

# Temple of the Axolotl — Master Development Roadmap V5

**Updated:** October 20, 2026 — V5 field-test and hotfix checkpoint  
**Platform:** Minecraft 1.21.1 · NeoForge 21.1.259 · Java 21 · GeckoLib 4.7.1  
**Mod ID:** `temple_of_the_axolotl`  
**Java package:** `com.devaxo.templeoftheaxolotl`

> **Status authority:** This V5 checkpoint supersedes V4's outdated progress labels where expressly updated below. Completed means the user reports the work finished; it does **not** claim an independent code audit or exhaustive multiplayer/restart testing. All earlier design rules, numerical specifications, lore, and unresolved questions are retained in the full V4 appendix. More recent V4 decisions override historical V3/V2 wording.

## 1. V5 executive checkpoint — completed

| System | V5 status | Notes |
|---|---|---|
| Celestial armor set | **Complete — user reported** | Helmet, chestplate, leggings, boots; finished models/textures and animations, including the Axolotl Absolute helmet |
| Armor innate abilities | **Complete — user reported** | Implementation checkpoint cleared; edge-case compatibility and full regression testing are separate release tasks |
| Armor potion effects | **Complete — user reported** | Preserve distinctions between potion passives and innates under Origin Divine Suppression |
| Armor attribute values | **Complete — user reported** | Final code is authoritative for exact values; V4 recorded a +5 vs +10 max-HP-per-piece discrepancy, which must not be silently resolved without checking source |
| Natural temple spawning | **Complete — user reported** | Preserve exclusion radius and portal lifecycle rules |
| Temple structure generation | **Complete — user reported** | Natural Overworld temple placement and template generation |
| Ten-floor dungeon generation | **Complete — user reported** | Procedural maze construction; loot population and treasure hallway remain separate tasks |
| Helmet scale and idle breathing | **Completed in Blockbench; integration to verify** | `Helmet` child of `bipedHead` scales 1.80× → 1.82× → 1.80× over two seconds, smooth interpolation; minor Z adjustment seats chin at forehead; scale keyed in an existing passive animation; Java renderer stays at default 1× |

### 1.1 Final Axolotl Absolute helmet presentation

- Helmet item remains registered as `axolotl_absolute`, distinct from the Axolotl Absolute entity's assets.
- Chibi two-sided pearl/cyan and crimson axolotl with gills, wings, crown and gyroscopic tesseract. Four established passive animation controllers cover gills, wings, crown and tesseract; a subtle breathing scale cycle is carried in an existing animation, not necessarily a fifth controller.
- Blockbench `bipedHead` is the head-tracking anchor. Its child `Helmet` contains the visual geometry and carries the 1.8× scale. The player head and outer hat/skin layer must remain visible.
- Java `AxolotlAbsoluteArmorRenderer` should **not** use `withScale(1.5F)` or an ad hoc `preRender` translation now that Blockbench controls the helmet size. The user prepared the revised animation; final in-game animation export and head-following regression should be checked, not assumed.
- Preserve the old voxel item model asset for possible future Totem of Undying presentation, but do not re-enable its duplicate equipped render layer.

## 1.2 V5 field-test checkpoint — latest player reports

**Status convention:** **PASS (user-tested)** means the player explicitly confirmed the behavior in-game. **Implemented / test pending** means delivered in a source hotfix but not yet verified in gameplay or by a successful compilation report. **Pending fix** means the behavior is known to need adjustment or was omitted. **Planned** means design agreed, not implemented. Do not treat a partial pass as completion of an entire feature.

| System / behavior | Latest V5 status | Field-test evidence and boundaries |
|---|---|---|
| Automatic temple demolition | **PASS — user-tested (checklist set 1)** | Last-crawler cleanup with five-second warning and deep-clean/reset flow; retain `/templefullreset` emergency command and avoid reviving `/axo_maze` commands. Multiplayer and restart edge cases still need release regression testing. |
| Developer portal | **PASS — user-tested (checklist set 2)** | `/tota_devportal` placement, raised position, dark-prismarine foundation and portal operation passed the latest checklist. Earlier missing-ring issue was specific to an older developer portal. |
| Wrath's Pounce movement | **PASS — user-tested (checklist set 3)** | Horizontal momentum preservation on successive air jumps passed; preserve current controls, vertical impulse, modifiers, particles and fall protection. |
| Developer tools | **PASS — user-tested** | Developer utilities reported functional. `/templefullreset` must remain available; `/axo_maze` namespace stays retired. |
| Reward staging and run binding | **PASS — user-tested for foundation only** | Server-owned staging, reward handling and association with the active run passed; **does not** mean physical chest loot, restricted rarity pools, player inventory submenu or full production reward lifecycle is complete. |
| Combat-logout ejection detection | **PASS — user-tested** | Logging out during a dungeon triggered extraction on rejoin. Earlier rejected teleport was caused by the then-missing developer-portal dark-prismarine ring, not a failed logout trigger. |
| Dungeon generation announcements | **PASS — user-tested** | `The Temple Is Arising: 802 modules at 3 per server tick` and `The Axolotl Absolute Watches As You Descend` display correctly. |
| Wrath's Pounce action-bar cleanup | **PASS — user-tested** | Repeating jump-counter spam is gone; do not reintroduce it. |
| Temple-specific persistent-effect cleanup | **PASS — user-tested** | All temple-related persistent effects disappear when exiting the temple; preserve this lifecycle behavior. |
| Ten-floor generation | **PASS — previously user-tested** | 802 modules placed at three per server tick. Does not validate newly changed Overworld temple distribution. |
| Natural temple distribution adjustment | **Implemented / fresh-world test pending** | Requested minimum spawn exclusion **1,000 blocks** (formerly 3,000); structure-set **spacing 66 chunks, separation 52 chunks** (formerly 80/62). The user's desired search density was approximately 60–72 chunks; 66 is the selected value. Test in newly generated terrain/a new world. These settings do not guarantee exact temple-to-temple distance. |
| Wrath curse icon and apparition | **Partially implemented / correction pending** | An initial crimson-eye icon/overlay was included in the combined source, but its overlay was tied to **initial dungeon arrival**, which is incorrect. Final Origin Wrath eye artwork and translucency remain future work. |
| Night Vision expiration flicker | **Pending fix** | Suppression was requested but explicitly **not included** in the last combined ZIP; keep natural potion expiration and duration. |
| Celestial HUD | **Planned** | Agreed player-configurable compact HUD, not yet implemented. |

### 1.3 Immediate V5 hotfix docket — fix without regressing green flags

1. **Correct curse-trap overlay trigger.** The event is **not** ordinary temple entry. Only after the RNG curse trap's chance succeeds, **He Is Upset With You** is actually added to that player's **dungeon-persistent effects**, **and** the curse trap's teleport successfully completes, schedule the overlay **20 server ticks after that teleport**. Never display it on an unsuccessful roll, failed teleport, or ordinary dungeon arrival. Keep the existing one-second Blindness and one-second Darkness effects. Avoid duplicate overlay scheduling for one successful trap activation; prevent stale overlays after exit/session reset.
2. **Suppress Night Vision expiration flicker.** Client-side rendering fix only; do not extend or silently reapply Night Vision, alter other potion timers, or interfere with effect cleanup. Validate with the armor helmet's Night Vision and normal vanilla Night Vision.
3. **Regression-protect the three green-flagged sets.** Preserve the five-second automatic deep-clean lifecycle, valid raised dark-prismarine developer portal, and Wrath's Pounce horizontal-velocity behavior. Preserve combat-logout extraction, generation messages, reward/run-binding foundation, and effect removal on exit.
4. **Fresh-world generation validation (test, not a new code fix unless needed).** Verify the 1,000-block minimum spawn exclusion and 66/52-chunk structure settings on a new world. Record actual found positions and biome restrictions before deciding further spacing changes.

### 1.4 Future V5 visual and interface docket

**Origin Wrath curse apparition — art and polish planned.** The curse is **Wrath's** anger at intruders entering his decoy temples to challenge him and free his imprisoned brother, Above All. The full-screen apparition must use **Origin Wrath's actual angry eyes** from the established character design, **not a generic celestial eye**. Prepare a transparent-background PNG and render the apparition **translucently** so the dungeon remains visible behind it. Use a brief Elder Guardian–inspired appearance/fade rather than an opaque screen takeover. The compact status-effect icon and the large apparition may be separate assets to maintain legibility. The timing and trap-only eligibility rule in §1.3 is mandatory for the eventual final artwork.

**Celestial HUD — designed, implementation pending.** Add non-OP `/tota hud` toggle and `/tota config` GUI with client-persistent visibility, six corner/side anchors (top-left, top-right, center-left, center-right, bottom-left, bottom-right), scale, opacity and four themes (Celestial Gold, Crimson, Pearl, Minimal). Use a single compact rectangle: dynamic remaining/max Wrath's Pounce jumps at upper left; a slim **120-second** Eternal Guard cooldown/READY line beneath; persistent custom curse/cosmetic effect icons in a vertical stack at upper right with Roman-numeral strength and temporary click-to-reveal names (without stealing normal gameplay clicks); regular potion-effect dropdown beneath, same overall width, showing up to five effects with scrolling, duration, name and amplifier, soonest-expiring first, infinite last. Expanded bottom-anchored HUD moves upward to stay onscreen. Optional vanilla potion-HUD icon hiding must not remove the effects themselves. Synchronize server-authoritative cooldown/effect strength; do not hardcode jump maximums.

**Additional later polish.** Continue helmet integration checks, actual dungeon chest loot and treasure hallway, Regular/Origin Wrath assets and combat, Ring presentation, redeemed Wrath, Origin Temple, and broader survival/multiplayer/restart compatibility testing according to the main milestones below.

---

## 2. V5 main development milestones — after current hotfix

1. **Dungeon loot table integrations.** Implement the revised 27-slot restricted rarity design (20 Common/Rare/Empty, 4 Rare/Epic/Empty, 3 Epic/Legendary/Empty); level unlocks, Sanctum rules, Legendary enchanted-book selection, 3% conditional Shard replacement, Regular Wrath's 40% Shard bonus, dedicated Dungeon Loot Inventory and inventory-screen submenu, lootbag drop/collection mechanics, and success/failure extraction rules with `/temple success` and `/temple failed` development commands. Resolve remaining probability normalization and reward-GUI rules before claiming completion.
2. **Treasure hallway NBT template.** Build and export the treasure hallway structure file, then connect it to the generator with correct dimensions, orientation, placement, loot-container identification and undo/cleanup behavior. Exact filename, dimensions and connection logic are not yet specified.
3. **Boss Wrath.** Model, texture, rig and animate the large fallen Wrath; implement Regular Wrath encounter logic and difficulty tiers (Easy 250 HP, Medium 500 HP, Hard 750 HP), torn-wing dive, Celestial Rupture and Rampage, plus reward hooks. Origin combat extensions remain later implementation work.
4. **Ring of the One passive 3D presentation.** Make its 3D model/texture and passive animation, then integrate item rendering without changing established innate lifesteal and four exclusive enchantment design. Animation specifics are still TBD.
5. **Tameable/redeemed Wrath assets.** Build the smaller restored-wing companion's model, textures and animations, preserving its onyx/ruby appearance, molten fins/gills, orbiting gemstones and combat-companion role. AI, taming and story triggers require integration beyond asset creation.
6. **Origin Temple.** Build the original temple and Origin Sanctum/prison route for seed `00001`; preserve the solo keyholder lock from entry, final confrontation, Above All prison rescue and diary reveal. Exact architectural layout remains TBD.

### 2.1 Stage 01 breakdown — loot tables, inventory, lootbags, and testing

| Substage | Task | Acceptance criteria |
|---|---|---|
| **01-A** | **Dungeon Loot Inventory** | Maintain a dedicated, player-owned inventory for dungeon-earned Rare, Epic, and Legendary rewards; distinguish reward provenance from identical items brought into the dungeon. Common loot goes to normal inventory. |
| **01-B** | **Inventory-screen submenu** | Provide a clickable entry from the vanilla player inventory screen into a Dungeon Loot Inventory submenu, visually/functionally inspired by Doggy Talents Next's dog inventory interface. No new keybind required. Ensure server-side access checks, correct synchronization, and multiplayer ownership. Exact button position/layout TBD. |
| **01-C** | **Lootbag drop mechanic** | Implement lootbag creation, rarity-appropriate contents, collection/opening, and routing to normal versus dedicated inventory. Exact drop trigger, item appearance, and opening interaction are **TBD**; do not silently assume bags replace all chest rewards. Prevent duplication on collection/reconnection. |
| **01-D** | **Success and failure finalization** | On successful completion/extraction, preserve earned dungeon rewards. On lethal-damage dungeon failure, remove only Rare/Epic/Legendary rewards earned in that run; preserve Common loot and the player's pre-existing equipment, inventory and XP. Design withdrawal/claim behavior to prevent bypassing confiscation. |
| **01-E** | **Development test commands** | Add operator/dev-only `/temple success` and `/temple failed` commands that invoke the same finalization paths as genuine dungeon success and failure. Test with a player-specific active session and avoid unintended effects outside a test session. Command capitalization shown in discussion is normalized to lowercase for Minecraft command syntax. |
| **01-F** | **Integration/regression tests** | Verify rarity restrictions (20/4/3), level unlocks, Legendary books and Shard odds, Sanctum chest rules, lootbag routing, GUI reopening, logout/restart persistence, multiple players, successful extraction, failed-run confiscation, and anti-duplication. |

**Implementation boundary:** The developer commands are intended to simulate the *outcome* of a dungeon run; whether they also teleport/extract the player, retire a portal, or tear down a live maze should be decided before implementation. Do not accidentally reset shared dungeon state during a reward-only test. The precise lootbag mechanics and inventory submenu layout are design tasks, not completed features.

## 3. Later work and release hardening — not silently marked complete

- Portal/session persistence across restarts, unloaded chunks and multiplayer; anti-duplication and teardown safety.
- Fully test loot ownership, restricted-slot probabilities, rewards, failure confiscation and final chest extraction.
- Origin Wrath encounter tiers and Divine Suppression; forgiveness eligibility through 15 Regular Wrath kills, resurrection and Second Fall.
- Ring-exclusive enchantment effects and levels; Ambrosia; Above All's companion behavior and Gentle Boop; Patchouli diary progression.
- End-to-end survival progression and compatibility testing.

## 4. Preserved balance and story rules

- **Key crafting:** 4 Shards of Divinity → 1 Orb of Envy; 9 Orbs → 1 Celestial Key (36 Shards total).
- **Loot:** 20/4/3 restricted slot groups; four rarity tiers; Legendary reward = 97% enchanted-book category or 3% replacement Shard; boss bonus = 40% Shard of Divinity. Do not reuse the obsolete 27-identical-roll model.
- **Origin:** seed `00001`, solo lock beginning at entry; forgiveness permitted at **15 or fewer** personal Regular Wrath kills. Preserve established tier boundaries and resurrection/Second Fall conditions.
- **Cosmology:** Axolotl Absolute created Above All and Wrath as two distinct independent celestial beings, not pieces split from itself.

---

# Appendix — Complete V4 roadmap (October 20, 2026; preserved)

The original V4 roadmap follows intact for full technical detail, historical checkpoints, earlier appendices and design canon. Where its progress statuses conflict with the V5 checkpoint above, V5 takes precedence. V4's later rules continue to supersede earlier historical appendices.

Temple of the Axolotl — Master Development Roadmap V4
Updated: October 20, 2026  
Platform: Minecraft 1.21.1 · NeoForge 21.1.259 · Java 21 · GeckoLib 4.7.1  
Mod ID: `temple_of_the_axolotl`  
Java package: `com.devaxo.templeoftheaxolotl`  
Scope: Latest progress, implementation priorities, design canon, and complete preserved V3 roadmap.
> **Authority and status rules.** V4 updates override outdated V3 snapshots **only where specifically noted**. **Tested** = user reported seeing it work in-game; **Implemented** = user reported integration/code work, without implying exhaustive testing; **In progress** = partially completed; **Designed** = agreed concept, not necessarily coded; **TBD** = not established. This is a development record based on discussion and screenshots, **not a fresh source-code audit**.
1. October 20 executive snapshot
Area	V4 status	Evidence / remaining work
Natural temples, shared dimension, portal lifecycle	Tested (single-player)	V3 records temple spawning, entry/exit, maze teardown, compass and retired portals. Restart/multiplayer hardening remains.
Ten-floor procedural maze	Tested	802 modules, 9×9×10 arrangement, 3 placements per tick; traps, loot and complete campaign flow still require work.
Celestial armor — chestplate	Modeled, animated, textured, and shown in-game	Royal robe with pearlescent and crimson duality, decorative jewels and shoulder accents; animated dorsal fin is separate from leggings tail. Gameplay abilities need separate verification.
Celestial armor — leggings	Modeled, textured, rigged, and tested visually in-game	Segmented tail visible; robe panels attached to correct leg bones and follow walking motion. Small hip-feather accents present; flutter is not visibly apparent in-game and is not a blocker.
Celestial armor — boots	Modeled, textured, and shown in-game	Two axolotl-themed slippers, one per celestial brother; visual design considered sufficiently complete, no extra trim required. Gameplay abilities not freshly verified.
Celestial armor — helmet	Geometry in progress	New chibi, dual-sided axolotl sitting on player's head, with four small feathered wings and developing gills. Current colors are construction markers, not final textures.
Armor rendering / skin overlays	Implemented and visually debugged in part	Shared GeckoLib armor model/renderer, plus sequential skin-overlay handling. Helmet must preserve the player's head and hat/outer skin layer.
Ring of the One 3D model and enchantments	Pending / earlier prototypes	Preserve V3 progression, enchantments and endings.
Regular/Origin Wrath, companions, Patchouli	Design established; implementation still pending/partial	No new boss combat or diary completion claimed.
Cosmology	New lore canon	The Axolotl Absolute is the hidden creator of the two distinct celestial brothers.
2. Recent implementation log — celestial armor
2.1 Shared armor architecture
Existing armor uses shared `CelestialArmorModel.java` and `CelestialArmorRenderer.java` rather than one renderer/model class per piece. `CelestialArmorModel` selects the leggings geometry (`geo/axolotl_leggings.geo.json`) and passive tail animation (`animations/axolotl_leggings_passive_tail.animation.json`) for the leggings slot.
GeckoLib armor bones are mapped to vanilla player body parts. The project uses `bipedHead`, `bipedBody`, `bipedLeftLeg`, and `bipedRightLeg` conventions as appropriate. The helmet's head-bone mapping and final integration should be verified against the current renderer before changing code.
Important resolved bug: the leggings' segmented tail was initially parented under `bipedBody` and did not appear when leggings were equipped, even though leg geometry rendered. Reparenting it temporarily under `bipedLeftLeg` made it visible, isolating the issue to slot/bone visibility. Subsequent renderer/bone work restored the visible tail and hip region. Do not reintroduce the old missing-tail regression.
Earlier duplicate tail bones in an old export were cleaned up. The duplicate-bone cleanup alone was not the final fix; slot-specific rendering was the relevant diagnosis.
Robe panels were moved to the correct leg parent groups. Tested: panels now follow the legs during movement; no mismatched panel attachments remain in the reported model.
An existing sequential skin-overlay handler (`CelestialArmorSkinOverlayHandler`) adjusts outer player skin layers according to equipped armor. Helmet-specific behavior must be revised or configured so both the vanilla head and the hat/outer head skin layer remain visible, since the new helmet sits above rather than enclosing the head. Preserve existing chestplate/leggings behavior.
2.2 Chestplate — royal celestial robe
Visual status: shown in-game. Purple royal outer robe, luminous-looking pearlescent/white and crimson interior split, ornate blue/red gems, decorative trims, shoulder accents, and a distinct animated dorsal fin on the back.
The chestplate's dorsal fin is not the leggings' segmented tail. Both features are independently modeled.
Keep existing gameplay design for Haste, Celestial Aegis, Eternal Guard and set bonuses; confirm actual effects in code/testing rather than treating appearance screenshots as ability tests.
2.3 Leggings — pantalonez and segmented tail
Visual status: shown in-game. Rebuilt UV/texture work from scratch after geometry changes. Purple fabric shading intentionally coordinates with chestplate; pearl/crimson cloth paneling, trims and ornaments completed.
Model construction uses individual cubes for trim instead of intersecting/impaling trim cubes; construction colors were temporary and replaced by finished fabric textures.
The small hip-feather accents were modeled; passive animation was created/imported but flutter is too subtle to notice reliably in-game. No further work required unless the user decides otherwise.
Tested: tail visible behind the player; corrected robe panels follow their assigned legs.
2.4 Boots — celestial slippers
Visual status: shown in-game. One small Above All-inspired slipper and one Wrath-inspired slipper. The user judged the existing detail sufficient and opted against further accenting.
Keep the existing `axolotl_absolute` registry/item identifier where required to avoid unnecessary callback/reference renames. A visual redesign does not imply renaming item IDs.
2.5 Helmet — the chibi Axolotl Absolute
New creative direction (in progress): A tiny, comically well-fed, two-sided axolotl sits on top of the player's head. One half represents Axolotl Above All (pearl/light); the other represents Wrath (crimson/dark). The split runs through the head, body and tail, emphasizing their shared origin. It has small flippers, a chunky tail, developing side gills, and four chibi feathered wings (two on each side).
The Blockbench hierarchy uses a `bipedHead` anchor, `Left Axo` and `Right Axo` groups with separate eyes, flippers and tail halves, plus wing groups such as `Wings → Wing Unit A → Wing Feather Unit A`.
The wings were custom modeled, not merely duplicated from the leggings' hip feathers. A layered, stepped-feather geometry is visible. Multiple cubes may keep the default name `waist` because the whole group animates as one bone; renaming every cube is unnecessary.
The lavender wing limbs, cyan feathers, pink/white body halves, green gills and yellow eyes in Blockbench previews are temporary construction markers. Do not assume these are final colors or materials.
The head reference cube is for positioning. Check whether it is exported/rendered and prevent it from covering the vanilla player's head.
Critical visual requirement: helmet must not remove or hide the player's face, head model, or outer hat/skin layer. Revisit the existing sequential skin-overlay handler's helmet rule. The chibi axolotl should move with head rotations.
Remaining: finish gill geometry, finalize all geometry and bone pivots, animate wings/gills as desired, paint final pearl/crimson textures, export GeckoLib assets, integrate/test helmet rendering and skin-layer preservation in-game, then inspect idle/walk/sneak/head-turn poses.
Humor/design shorthand: “a chibi axolotl that had one too many Oasis Sushis and never skips gill day.” This is visual flavor, not a new food mechanic.
3. New story canon — the hidden third deity
3.1 The Axolotl Absolute: the Celestial's Celestial
Confirmed new lore premise: The Axolotl Absolute is a higher, mysterious creator deity, hinted to oversee the Axolotl-verse. Axolotl Above All and Axolotl's Wrath originated as two distinct beings created by the Absolute's willpower. They are not halves broken off from the Absolute, and the Absolute is not necessarily diminished by their creation.
Creator analogy (user-established): The Axolotl Absolute is the baker. Above All and Wrath are two separate cupcakes. The Axolotl-verse is the oven. This is an analogy for creation, not literal baking or an assertion that the universe itself is the Absolute.
Continuity with V3: The brothers still embody contrasting creation/preservation and destruction/jealousy; their opposing powers still form the Ring of the One. Wrath's failed attempt to wield the Ring, his torn wings, Above All's imprisonment, the scattered key, the original temple, forgiveness, resurrection and Second Fall remain unchanged. The Absolute is upstream of this established narrative, not a replacement for it.
Mystery remains intentional: The Absolute's appearance, consciousness, whereabouts, motive, ability to intervene, relationship to the Ring, and whether it ever appears as an entity/boss are TBD. No claim that it is omnipotent or that Above All cannot understand it has been canonized.
3.2 Subtle lore delivery (proposed, not yet approved)
Treat the dual-sided helmet as visual foreshadowing of a single creator who made two distinct celestial brothers; it does not mean the brothers literally fused into one organism.
Consider an optional cryptic diary line or temple inscription suggesting a will that existed before either brother. Do not reveal the full creator identity prematurely without a pacing decision.
Preserve the Patchouli diary's first-person Above All voice and existing title reveal, Diary of an Axolotl, upon opening his prison cell.
Keep the Absolute's exact relationship to the original `axolotl_absolute` item name open until lore and item descriptions are finalized.
3.3 Updated creation chronology
Before the brothers: the Axolotl Absolute exists as a higher creator (details deliberately mysterious).
Creation: by its will, the Absolute creates Above All and Wrath as independent celestial beings within the Axolotl-verse.
The brothers and Ring: the brothers develop their distinct powers; their opposed powers create the Ring of the One.
The First Fall: Wrath covets the Ring, tries to wield it alone, and loses the integrity of his wings.
The imprisonment: Above All casts the Ring into the Minecraft-verse; Wrath imprisons his brother and scatters the key through decoy temples.
The player's story: temples, shards, the Celestial Key, seed `00001`, Origin Wrath, the diary reveal, and established alternate endings follow V3's full chronology.
4. Current priority order — V4
Immediate: finish the celestial armor visual pass
[x] Chestplate model/texture visual pass and in-game appearance.
[x] Leggings' missing-tail renderer issue resolved; tail visible in-game.
[x] Leggings robe panels reparented correctly and tested with leg motion.
[x] Leggings pearl/crimson fabric textures and hip/leg trims completed.
[x] Dual-themed boots' visual appearance completed to user's satisfaction.
[ ] Finish helmet geometry: gills, chibi wings, split tail, facial details and head positioning.
[ ] Finalize helmet textures after geometry (current colors are markers).
[ ] Verify `bipedHead` rigging and avoid exporting an obstructive reference head cube.
[ ] Update/test helmet rule in sequential skin-overlay handler so vanilla head and hat layer remain visible.
[ ] Export helmet model/texture/animation; equip in-game and inspect multiple poses.
[ ] Independently verify all armor innate abilities, potion passives, set bonuses and enchantment hooks in gameplay.
Resume broader mod development afterward
[ ] Harden portal/session state across restarts, unloaded chunks and multiple players.
[ ] Finish trap/loot/extraction rules, restricted-slot rarity math, and separate dungeon-reward inventory.
[ ] Implement/finish Ring of the One 3D presentation and four exclusive enchantments.
[ ] Complete Regular Sanctum statues and Origin Sanctum/prison route.
[ ] Implement Regular/Origin Wrath entities, animations, attacks and difficulty rules.
[ ] Implement Ambrosia, Above All companion/Gentle Boop, and redeemed Wrath.
[ ] Implement the forgiveness, soul-fire resurrection, and Second Fall branches with protection/recovery logic.
[ ] Write Patchouli lore, including subtle hints of the Absolute, and implement the diary title reveal.
[ ] Full survival, multiplayer, restart, compatibility and anti-duplication tests.
5. Carry-forward rules and discrepancies to verify
Do not downgrade tested work: V3's October 16 status that armor was merely a non-equippable placeholder is superseded by the user's subsequent in-game chestplate, leggings and boots demonstrations.
Armor health bonus conflict: V3 states +5 HP per piece (+20 HP full set), while more recent armor discussions describe +10 HP per piece. Do not silently resolve this discrepancy; inspect the current item/attribute code and confirm intended balance before publishing final stats.
Armor visuals supersede old designs: V3's tentative onyx/sapphire leggings and old chestplate plan no longer describe the finished pearl/crimson/purple robe and leggings. Keep the V3 descriptions only as history.
Celestial Key progression remains 4 Shards → 1 Orb; 9 Orbs → 1 Key (36 Shards).
Forgiveness remains available through 15 Regular Wrath kills, with Origin tier boundaries unchanged. This V3 update supersedes the archived October 15 cap of 10.
Chest loot uses the revised 20/4/3 restricted-slot pools, not 27 identical Legendary-eligible rolls. Legendary conditional bonus Shard chance is 3%, not older superseded calculations.
Boss bonus drop is a Shard of Divinity at 40%, not the older “Wrath fragment” terminology.
Post-victory resurrection requires a fully maxed Ring offered to soul fire with freed Above All within four blocks; preserve the Ring on invalid ritual.
Do not confuse the chestplate's dorsal fin with the leggings' tail or the helmet's four wings/gills.
Implementation vs design: screenshots validate visual appearance, not potion effects, combat mechanics, multiplayer behavior or persistence.
---
Appendix — Full V3 roadmap (October 16, 2026; preserved)
The original V3 document follows below without dropping its historical specifications, earlier updates, and October 15 archive. Where V3 conflicts with the V4 sections above, V4 takes precedence.
---
Temple of the Axolotl — Master Development Roadmap
Updated: October 16, 2026 — Version 3 (end-of-day update)  
Platform: Minecraft 1.21.1 · NeoForge · Java 21 · GeckoLib 4.7.1  
Mod ID: `temple\_of\_the\_axolotl`  
Package: `com.devaxo.templeoftheaxolotl`  
Purpose: Consolidated development specification, story canon, and implementation backlog.
> \*\*Source-of-truth convention:\*\* \*\*Confirmed\*\* means explicitly established in design discussions, not necessarily implemented. \*\*Tested\*\* means the user reported testing it. \*\*Proposed\*\* means an implementation suggestion awaiting approval. \*\*TBD\*\* means not decided. This October 16 update takes precedence over conflicting October 15 snapshot statements reproduced in the archival appendix.
October 16 — End-of-day progress update (read first)
> This section supersedes older implementation-status descriptions below while preserving the complete earlier design specification and original roadmap appendix. “Implemented” is based on our reported development progress; “tested” means explicitly confirmed in-game. Multiplayer and crash-recovery testing are not complete.
Implemented and tested — temple exploration loop
[x] Natural Overworld temple pyramid (`tota\_thetemple.nbt`, approximately 49×58×49), in coastal/ocean biomes; partially submerged temples intentional.
[x] Spawn exclusion zone: ±3,000 blocks from world spawn.
[x] Emerald Temple Portal enters the single shared `temple\_of\_the\_axolotl:temple\_dimension`; original Overworld portal is saved for the return trip.
[x] `/templeexit` returns players to their entrance portal.
[x] Ten-floor maze generation: 802 modules, 9×9×10 layout, three module placements per tick; entry arrival approximately (22.5, 202, 26.5).
[x] Automatic maze teardown and snapshot restoration after the final player leaves.
[x] `/templefullreset confirm` recovery scanner, covering 1,134 sections, to clear ghost dungeon remnants.
[x] Session records all portals that successfully admitted a player; used portals are retired in saved world data when dungeon deconstruction begins.
[x] Portal blocks are removed when their chunks are loaded; retirement data prevents reusing retired temples.
[x] Temple Seeker Compass with 32 needle frames, right-click horizontal-distance action bar, Shift + right-click refresh, and nearest-unused-temple selection.
[x] Compass direction corrected and immediate retargeting after retirement confirmed in-game.
Follow-up: Test multiple players/portals, restarts during an active dungeon, session-portal persistence, and unloaded-chunk portal removal. The active-session portal set is not yet guaranteed to survive server restarts.
Implemented/prepared — celestial items and recipes
[x] Registered `shard\_of\_divinity`, `orb\_of\_envy`, `celestial\_key` and added creative-tab entries.
[x] Prepared three separate transparent PNG item textures with the original pink, purple, cyan and gold Axolotl-verse flair.
[x] Prepared `minecraft:item/generated` item-model JSONs for the three textures.
[x] Recipes for Temple Seeker Compass, chestplate, leggings, boots, Orb of Envy and Celestial Key.
[x] 4 Shards of Divinity → 1 Orb of Envy (2×2 crafting).
[x] 9 Orbs of Envy → 1 Celestial Key (3×3 crafting); 36 shards per key.
[x] Armor recipes intentionally require bedrock, intended to be obtainable through the Ring of the One's Supernova.
Not yet verified: Final item PNGs rendering in-game. Armor items are craftable placeholders, not yet functional equippable animated armor.
Texture location: `src/main/resources/assets/temple\_of\_the\_axolotl/textures/item/` with `shard\_of\_divinity.png`, `orb\_of\_envy.png`, `celestial\_key.png`.
Model location: `src/main/resources/assets/temple\_of\_the\_axolotl/models/item/` with the matching `.json` filenames; each `layer0` points to `temple\_of\_the\_axolotl:item/<item\_id>`.
Next: Blockbench Day — finalized celestial armor design
Chestplate — Above All pearlescence
[ ] Pearl-white iridescent texture matching Axolotl Above All's body.
[ ] Mirrored dorsal/tail-fin silhouettes on the front and back faces of both arms.
[ ] Matching animated gills on the outer side faces of both arms.
[ ] Gentle, subtly staggered passive fin/gill movement; proper player arm and torso rigging.
Leggings — celestial duality
[ ] One leg onyx-black; the other sapphire-blue.
[ ] Matching material detail, player leg rigging and animation compatibility.
Boots — celestial slippers
[ ] One axolotl slipper inspired by Wrath, the other by Axolotl Above All.
[ ] Distinct tiny passive animations for each slipper, such as gill movement or blinking.
[ ] Test idle, walking, sprinting, sneaking and third-person poses.
Implementation after Blockbench: Export geometry, textures and animations; implement equippable GeckoLib armor and verify attachment to the player's moving body parts.
Later modeling and gameplay milestones
[ ] Create a true 3D Ring of the One model and effects; complete its abilities and enchantments.
[ ] Build Boss Wrath as a new large custom model and animation rig; implement Regular/Origin Wrath combat.
[ ] Build Tameable Wrath, adapting/recoloring the Axolotl Above All model where suitable.
[ ] Integrate Origin seed `00001`, solo ten-floor challenge, Origin Sanctum and story dialogue.
[ ] Implement Gentle Boop, Ring sacrifice and Wrath resurrection, plus forgiveness/redeemed-Wrath paths as specified below.
[ ] Finish dungeon traps, loot tables, perfect-chest rewards, extraction and failure handling.
[ ] Implement Patchouli Diary progression and the Diary of an Axolotl reveal.
[ ] Reconcile old “Wrath fragment” loot wording with the finalized Shard → Orb → Key progression before implementing drop tables.
[ ] Test server restart recovery, multiplayer, unloaded chunks, anti-duplication and complete survival progression.
Updated stage snapshot
Stage	Current status
1 — Core Axo companion	Foundation implemented; further behavior/polish pending
2 — Items, Ring, armor	Three celestial items/recipes/textures prepared; animated wearable armor and Ring 3D model pending
3.1 — Procedural maze	802-module ten-floor maze generated/tested; traps and loot progression pending
3.2 — Temple dimension	Implemented and single-player tested; session/restart/multiplayer hardening pending
3.3 — Wrath and sanctums	Arena and story design; models/AI/encounters pending
3.4 — Patchouli and lore	Story designed; implementation pending
4+ — Polish/release	Planned
---
Current development snapshot
Stage	Scope	Status
1	Core mod, Axo companion, models, GeckoLib	Working foundation; companion gameplay incomplete
2	Axolotl Absolute, Ring, armor, key progression	Partially implemented / detailed design
3.1	Ten-floor procedural maze	Generation/connectivity tested successfully; broader loot and dimension integration remain
3.2	Dedicated Temple Dimension and portal/session lifecycle	Implemented and single-player tested; hardening pending
3.3	Sanctums, Regular/Origin Wrath, Gentle Boop	Regular arena built; combat planned
3.4	Patchouli diary and reveal	Fully conceptualized; not implemented
4+	Expansion, balancing, compatibility, release	TBD
Maze progress since the October 15 roadmap
Tested: ten-floor procedural labyrinth; approximately 802 placed modules, paced at three modules per server tick. Connectivity validated and generation rules passed without reported exclusion errors.
The player has walked the generated maze and found it genuinely disorienting. Uniform stone-brick corridors, curved corners, evenly spaced sea lanterns, and deceptive routes are intentional.
Existing commands: `/axo\_maze generate \[seed]` and `/axo\_maze undo \[confirm]` (exact argument syntax and undo implementation must be verified in source before integration).
Traps are static, disguised blocks with step-triggered damage/effects/curses; no trap animation or block-state transformation required. Includes damage, burn, honey/movement impairment, and teleportation curse. Teleport curses can send a player one floor upward, briefly obscuring vision.
Still to implement/verify: production loot tables, chest reward tracking, temple-dimension integration, final sanctum entry and completion flow. The original roadmap's older “5×5 prototype / 9×9 expansion pending” status is superseded by the user's newer successful ten-floor testing; exact current grid dimensions should be checked against source.
Stage 3.2 — Dedicated Temple Dimension (original design specification; see latest update above)
2.1 Environment and player rules — confirmed
Setting	Specification
Dimension architecture	One reusable pocket dimension, not one dimension per temple
World	Void; dungeon structure is placed and later explicitly removed
Proposed registry ID	`temple\_of\_the\_axolotl:temple\_dimension` (proposed, not yet locked)
Maze template origin	Y = 200
Entrance arrival	Unrotated entrance-room template origin + (7.5, 3, -3), e.g. `(Xe+7.5, 203, Ze-3)`
Game mode	Adventure within the dimension; restore player's prior mode on extraction
Inventory and XP	Normal inventory and XP persist across dimensions; dungeon loot has its own on-screen clickable inventory GUI (no keybind). Common rewards go directly to the normal player inventory.
Hunger support	Saturation III, applied every 30 seconds / 600 ticks, requested effect duration 5 seconds / 100 ticks; refresh stops outside dimension
Trap behavior	Existing static blocks activate upon player contact/step; no animations or block changes
Sky/fog visuals	Starry void; maze is enclosed. Ambient light and day/night technical behavior remain implementation details, not finalized.
Simultaneous sessions	One active dungeon seed/session at a time
Implementation note: Java Saturation is an instant status effect, so a nominal five-second effect duration does not produce five seconds of continuous Saturation. Apply on entry and at the chosen interval, then verify hunger behavior in-game. No manual removal is needed after leaving if it is only periodically granted in the Temple.
2.2 Portal and session lifecycle — confirmed design
DORMANT: No active dungeon; a fresh Overworld temple portal may claim the next session.
GENERATING: The first eligible temple portal activation claims a seed and begins paced `/axo\_maze generate`. Portal charging/entry must wait until generation is ready; approximate generation-only time at 802 modules / 3 per tick / 20 TPS is 13.4 seconds, excluding lag and other work.
ACTIVE: Players entering an ordinary decoy temple join the same current maze seed and starting entrance, not independent instances. Track each player's UUID and originating Overworld portal/return position.
EXTRACTING: Players leave via failure, completion, or disconnect handling; others may remain and continue.
RESETTING: After the last player leaves, sequentially undo the maze, mirroring placement; Overworld temple portal(s) vanish when dismantling of the generated seed begins. No new generation may overlap cleanup.
DORMANT again: Verify all placed blocks/structure data were removed; a different available temple portal can begin a newly seeded run.
Persistent data and safety: Keep session seed, phase, originating portal, player UUID/return points, generation/undo progress, and pending-login-return flags across server restarts. Unloading chunks does not erase saved blocks. Cleanup must explicitly undo placed structures, including blocks changed during gameplay. A disconnecting player is returned to the Overworld temple on next login rather than being teleported while offline. Confirm what happens if multiple Overworld temples are used during one active session; individual return positions should be retained regardless.
Special original temple — seed `00001`: Requires the Celestial Key. From the moment the keyholder enters, lock the entire session against all other players, for all ten floors and Origin Sanctum; do not wait until Origin Wrath spawns. Clean the solo lock safely after exit, disconnect, death, and server restart. Ordinary decoy sessions remain multiplayer-capable.
2.3 Exit and failure behavior — confirmed
Trigger	Outcome
Lethal damage in the Temple	Prevent normal death, immediately extract to the player's originating Overworld temple portal; leave player at 1 HP / half a heart. No countdown or death screen. Preserve brought equipment, inventory, and XP except the specified dungeon-loot penalty.
Final Sanctum chest opened	Identify the opening player's UUID and start that player's action-bar countdown; allow reward collection, then extract to the originating Overworld temple portal. No rare-loot confiscation on success.
Logout / disconnect	Persist return intent; on next login, safely return to the originating Overworld temple rather than leaving player stranded.
Last player gone	End session, retire portal, and run sequential undo.
Countdown duration: confirmed 30 seconds, with an on-screen clickable Extract Now control for immediate voluntary extraction. The player has time to collect chest rewards but need not wait out the timer. A chest-opening event must be protected against duplicate timers and UUID spoofing. Failed-run confiscation applies only on lethal-damage failure, not on successful chest extraction; whether other exit types count as failures requires explicit design.
Safe return: Avoid retriggering portal travel or immediate damage; restore prior game mode. Intercept damage before vanilla death handling and ensure other modded death-prevention systems interact predictably.
Stage 3.1 / 3.2 — Dungeon loot economy (new confirmed design)
3.1 Chest composition and rarity pools — revised canon
27 fixed chest positions, generated as a single chest-population operation, not the earlier model of 27 identical independent rarity rolls.
Positions 1–20 (20 slots): Common / Rare / Empty only.
Positions 21–24 (4 slots): Rare / Epic / Empty only.
Positions 25–27 (3 slots): Epic / Legendary / Empty only.
If a result is not yet unlocked at the chest's level, that position is empty rather than rerolled into another rarity. Exact per-position probability normalization remains to be implemented; the earlier global percentages cannot simply be applied unchanged as complete distributions to each restricted pool.
Four rarity tiers only: Common, Rare, Epic, Legendary; no Uncommon. Golden Tropical Fish Sushi is among the best Common drops.
Level 1: Common unlocked. Level 2: Rare unlocked. Level 3: Epic unlocked. Level 4 and Sanctum: Legendary unlocked.
Prior global baseline weights were Common 65%, Rare 12%, Epic 8%, Legendary 3%, Empty 12%; these are historical reference weights, not yet a mathematically complete algorithm for the newly restricted slot pools.
Sanctum rarity weighting is confirmed as Rare 65%, Epic 25%, Legendary 10%, and no Common. Because each position also has an allowed-rarity pool and Empty is possible, the exact algorithm mapping these percentages onto the 20/4/3 position groups must be specified in code/design; do not silently assign impossible outcomes or invent an Empty percentage.
The final Sanctum chest begins its 30-second extraction timer on UUID-attributed opening; Extract Now can end it early.
3.2 Legendary reward subtable — revised canon
A Legendary outcome offers two top-level reward categories:
Legendary reward category	Conditional chance within Legendary	Notes
Enchanted-book category	97%	Pool includes four Ring of the One-exclusive books AND stronghold-library-style enchanted books; ordinary books dilute the chance of a Ring-exclusive book. The relative selection weight between the two book families is TBD.
Bonus Shard of Divinity	3%	Replaces the book; not an additional drop.
Ring-exclusive enchantments: Supernova I (maximum I), Event Horizon I (maximum I), Lucky Stars I (maximum I), Celestial Convergence (maximum VI). The four Ring enchantment types are equally likely to be selected once a Ring-exclusive book is chosen; the resulting level distribution for Celestial Convergence remains TBD. The stronghold-library enchanted-book pool is an added dilution source, not a fifth Ring enchantment.
Probability note: The former 2.40% bonus-shard-per-chest calculation and 136-shard maximum assumed all 27 slots could roll Legendary. That assumption is superseded. Under the new fixed-position design, only three slots per chest can contain Legendary rewards. The absolute theoretical maximum, assuming four Level 4 chests plus one Sanctum chest and one Wrath bonus drop, is 4×3 + 3 + 1 = 16 Shards of Divinity from these sources in one run. This is an upper bound; exact chances depend on the finalized slot-pool algorithm.
Boss reward: The Regular Wrath 40% drop is a Shard of Divinity, not a Wrath fragment. Keep any separate guaranteed key-fragment rewards distinct from this drop.
3.3 Dedicated dungeon loot inventory and failure confiscation
Add a separate dungeon-reward inventory GUI, opened by an on-screen clickable control, not a keybind.
Common loot enters the player's normal inventory. Rare, Epic, and Legendary rewards are tracked in the separate dungeon inventory, not mixed with ordinary brought-in items.
On lethal-damage failure only, wipe Rare, Epic, and Legendary dungeon rewards in that dedicated inventory; Common rewards already transferred to the player's main inventory remain.
On successful final-chest extraction, preserve all earned rewards. Specify the exact withdrawal/claiming behavior of the separate GUI during implementation so the failure penalty cannot be bypassed by moving rewards to external storage.
Items brought into the dungeon and normal inventory/XP are preserved; the failure penalty applies to dungeon-generated rewards, not matching item types from outside.
Updated progression, Origin difficulty, and endings
4.1 Celestial progression and consequences
4 Shards of Divinity → 1 Orb of Envy.
9 Orbs of Envy → 1 Celestial Key = 36 Shards.
Shards also compete with Axolotl Absolute armor crafting.
Each Regular Wrath kill / decoy temple clear increments a persistent personal kill count, once per clear. Higher counts raise Origin Wrath difficulty.
Lucky bonus-shard finds can reduce the number of clears needed, preserving more opportunities for reconciliation.
4.2 Origin Wrath tiers (unchanged)
Personal Regular Wrath kills	Curse	Origin tier	Established mechanics	Forgiveness allowed? (updated)
0–10	YOU SHOULDN'T HAVE I	Easy	≥750 HP; Regular Hard baseline	Yes
11–15	YOU SHOULDN'T HAVE II	Medium	Divine Suppression; projectile/explosion immunity except optional Create Potato Cannon exception	Yes
16–20	YOU SHOULDN'T HAVE II	Medium	Same Medium rules	No
21+	YOU SHOULDN'T HAVE III	Hard	Medium rules plus Totem/Eternal Guard lethal-save suppression	No
New canon: The forgiveness eligibility cap is 15 or fewer kills, not the old 0–10 cap. This does not change the tier boundaries. The exact phrase remains `Sorry I hurt you, but i also forgive you`, typed during the solo Origin encounter. A successful forgiveness event stops the fight, restores Wrath to his small winged redeemable form, and he can be tamed with a Soul Campfire.
4.3 Fourth route: post-victory soul-fire resurrection (new canon)
This is an alternative path for players who missed the in-fight forgiveness opportunity and have already killed Origin Wrath and freed Axolotl Above All.
Required conditions:
Origin Wrath has been defeated and Above All freed.
Sacrifice a maxed-out Ring of the One into a block of soul fire; all four Ring-exclusive enchantments must be present at their required maximum levels (verify exact enchantment-level rules when designed).
Axolotl Above All must be within a four-block radius of the sacrificial soul fire, as if he is the one calling his brother back.
Any additional duplicate-resurrection or ownership restrictions remain TBD.
On success: Consume the offered Ring and resurrect the tameable redeemed Wrath (the established onyx/ruby, restored-wing, flying companion form). The resurrected Wrath is not automatically bonded; the player must still tame him using a Soul Campfire.
On invalid offering: Preserve the Ring and display this exact action-bar message:
> The Soul of the Damned refuses the sacrifice
No Ring should be lost if the ritual conditions fail. Implementation safeguard: Soul fire normally damages dropped items, so detect and validate the offering before vanilla item destruction; do not destroy a failed offering. Consider clear anti-duplication and one-time redemption state.
Narrative meaning: The brother who was imprisoned becomes the one calling his fallen brother back. This route has no kill-count cap established, but demands a completed Origin fight and the sacrifice of a fully empowered Ring.
4.4 Existing endings remain canon
Ordinary victory: Defeat Origin Wrath, free Above All, unlock the diary's real title, and obtain his companion progression.
Path of Forgiveness: At ≤15 Regular Wrath kills, use the exact phrase during the Origin fight; redeem and tame Wrath.
The Brother's Requiem (working name, not confirmed title): After ordinary victory, sacrifice a maxed Ring in soul fire with Above All within four blocks; resurrect tameable Wrath.
The Second Fall: After redeeming Wrath, deliberately offer him the Ring with a confirmation warning. Wrath learns to distribute the Ring's power across all its enchantments, ruins his wings again, transforms into Hard-tier Origin Wrath in the Overworld, darkens the sky to new-moon midnight, and converts nearby safe ordinary terrain to blackstone. He cannot be tamed again after this fall. Exact end-of-event recovery and Ring return are TBD.
Second Fall after resurrection is possible only if the player crafts another Ring of the One (the resurrection sacrifice consumes the prior Ring). Second Fall blackstone conversion is permanent, excluding protected block entities and inventory blocks; defeating berserk Origin Wrath does not restore the altered terrain. A sufficiently determined player can craft another fully maxed Ring and repeat the same soul-fire resurrection ritual to resurrect the berserked Wrath again. This costs the newly crafted Ring; the revived tameable Wrath still requires Soul Campfire taming. Exact defeat-state / targeting logic for the repeat ritual is an implementation detail.
Other established systems (unchanged from October 15)
5.1 Regular Wrath
Tier determined by whichever is higher: party size or curse. Easy: 1–2 players / curse 0–2, 250 HP. Medium: 3–4 / curse 3–6, 500 HP. Hard: 5+ / curse 7+, 750 HP.
Easy: Torn-wing Dive Bomb, igniting impact.
Medium: Celestial Rupture, 15-block gravity pull during charge, facing locked before beam, three seconds of once-per-second damage. For curse 10+, the priority target remains pulled through firing.
Hard: After ten successful hits, 15 seconds of Rampage: 50% shorter cooldowns, +25% damage; successful Rampage hits contribute to the next activation.
Regular Sanctum: built 60×32×60 deepslate cathedral with celestial yin-yang arena; small statues and protected recipe-clue daises require final installation/testing.
5.2 Above All and equipment
Gentle Boop™: peaceful/protective Above All, wolf-like damage, face-forward `owo` animation, custom knockback strength 20. Above All is the rideable companion; redeemed Wrath is a flying combat companion wielding vanilla netherite sword or axe.
Axolotl Absolute armor: +5 innate max HP per piece (+20 HP full set), with potion passives and innate abilities differentiated for Origin suppression. Eternal Guard prevents lethal damage on a two-minute cooldown unless suppressed at Origin Hard. Helmet prevents Ambrosia freezing but not its accelerated drowning.
Ambrosia: planned luminous gold fluid, light 8, flows a little slower than lava, dissipates after ~30 seconds, cannot be recollected, doubles drowning pressure, causes freezing without Absolute Helmet, and grants helmet-wearer Strength II / Regeneration IV / Absorption V / Resistance I during immersion and for 30 seconds after; blessings suppressed in Origin Medium/Hard.
Ring of the One: innate lifesteal; four exclusive dungeon-loot enchantments with individual weights TBD. Its Yang Singularity connects to celestial enchantment mechanics.
5.3 Patchouli and story
Diary initially has an obfuscated title; same cover (yin-yang, halo, flaming horns) and first-person Above All memories throughout.
On opening Above All's prison cell, the title reveals Diary of an Axolotl.
Celestial brothers embody creation/preservation and destruction/jealousy. Their opposed powers formed the Ring; Wrath's failed attempt to wield it ruined his wings. Above All sent the Ring to the Minecraft-verse; Wrath imprisoned him in original temple seed `00001`, shattered the key twice, and scattered fragments through decoy temples.
Forgiveness, sacrifice, and temptation are distinct thematic choices. The player's repeated enchantment use unwittingly teaches Wrath how to master the Ring in the Second Fall.
5.4 Ambrosia acquisition and handling — new confirmed rules
Drop one Shard of Divinity into a full water cauldron to transform its contents into Ambrosia. The Shard is not consumed and can be recovered/reused for further conversions.
Ambrosia can be bucketed out of the Ambrosia cauldron; each filled bucket is non-stackable, like ordinary water/lava buckets.
Once Ambrosia is placed on the floor as world fluid, it cannot be bucketed back up. Preserve the earlier planned 30-second world-fluid expiry, glow/light level 8, slow flow, accelerated drowning, freezing without the Absolute Helmet, and temporary helmet blessings, subject to testing.
Create pipes are incompatible with Ambrosia, including if optional Create integration is added. Create's Potato Cannon firing golden carrots remains the separate optional Easter-egg projectile exception against Origin Wrath; do not conflate these two integration rules.
Implement cauldron item interaction and returned Shard carefully to avoid item loss or duplication; a full water cauldron is required.
Implementation backlog — next actionable order
[x] Test full ten-floor maze generation and room connectivity (user-reported).
[ ] Inspect current maze source and confirm exact template grid, snapshots, undo behavior, and performance.
[ ] Register reusable void dimension and dimension type; finalize sky, light, bed/respawn, coordinate, and ambient settings.
[ ] Create persistent dungeon-session manager with DORMANT/GENERATING/ACTIVE/RESETTING phases.
[ ] Integrate portal charge, session seed assignment, first-portal ownership, and shared multiplayer entrance.
[ ] Enforce seed `00001` keyholder-only lock from entry.
[ ] Apply Adventure mode and timed Saturation III only in Temple; restore game mode on exit.
[ ] Implement safe origin-portal return, pending login extraction, and restart resilience.
[ ] Implement pre-death interception: immediate return at 1 HP, inventory provenance-based confiscation.
[ ] Implement final-chest UUID action-bar countdown and successful extraction; decide duration.
[ ] Integrate sequential `/axo\_maze undo` on final departure; verify void reset and retire portal.
[ ] Implement four-rarity 27-roll loot tables and dungeon-session provenance tagging.
[ ] Implement Ring-book weighted subtable and 0.09%-per-eligible-roll bonus Shard chance; resolve Sanctum Legendary premium and boss drop naming.
[ ] Complete regular sanctum statue placement and build Origin Sanctum/prison approach.
[ ] Implement Regular and Origin Wrath entities, animations, combat tiers, and loot rewards.
[ ] Implement Absolute armor, Ring enchantments, Shard/Orb/Key progression, and Ambrosia.
[ ] Implement Above All's rideable companion and Gentle Boop™.
[ ] Implement revised ≤15-kill forgiveness path and redeemed Wrath companion.
[ ] Implement maxed-Ring soul-fire sacrifice, 4-block Above All proximity, rejection action bar, and resurrection.
[ ] Implement Second Fall and protection against Overworld griefing/duplicate events.
[ ] Integrate Patchouli diary unlocks and final title reveal.
[ ] Test multiplayer, server restarts, death-prevention compatibility, item persistence, duplicate rewards, and optional Create integration.
Remaining design / implementation questions
Exact restricted-slot probability algorithm for Level 1–4 and Sanctum chests (20 Common/Rare/Empty, 4 Rare/Epic/Empty, 3 Epic/Legendary/Empty); specifically, how to reconcile earlier global weights with per-group eligibility and the Sanctum 65/25/10 distribution without inventing rates.
How strongly stronghold-library enchanted books dilute the Ring-exclusive book selection, and the book-level selection behavior for Celestial Convergence I–VI.
Dungeon reward GUI: claim/withdrawal rules, multiplayer ownership, capacity, and exploit-resistant confiscation; action-bar Extract Now UI implementation.
Dimension ambient light and time mechanics beyond the confirmed starry-void sky; portal entry details across multiple Overworld temples, with portals retiring when dismantling begins.
Origin Sanctum layout, Origin-exclusive offensive moves and additional HP: TBD.
Ambrosia cauldron conversion, expiry, client visuals and optional-mod compatibility implementation specifics.
Exact ritual state checks and how to identify the post-Second-Fall Wrath eligible for repeat resurrection; blackstone conversion radius and protected-support-block policy remain TBD.
---
Appendix A — Original October 15 master roadmap (preserved source)
The following is the original roadmap supplied by the user, retained verbatim as a historical reference. Some entries are superseded by the October 16 decisions above; consult the updated sections first when they conflict.
```text
Temple of the Axolotl — Master Development Roadmap
Development snapshot • October 15, 2026 • Minecraft 1.21.1 / NeoForge
1. Project overview
Field	Details
Mod name	Temple of the Axolotl
Mod ID	`temple\\\_of\\\_the\\\_axolotl`
Java package	`com.devaxo.templeoftheaxolotl`
Minecraft	1.21.1
Mod loader	NeoForge
Java	21
Animation library	GeckoLib 4.7.1
Original concept	Custom axolotl companion
Expanded concept	Companion, celestial powers, procedural temples, bosses, and story-driven progression
Status legend
Implemented / tested
In progress
Planned
Concept / TBD
Statuses reflect our previous development discussions, not a fresh inspection of the source code.
2. Full development stages
1
Core Mod \& Axo Companion
Foundation built
Entity, model, animations, registrations, and basic companion groundwork.
2
Items, Powers \& Progression
In progress
Axolotl Absolute, special effects, enchantments, trophies, and progression.
3.1
Procedural Temple Maze
In progress
Ten floors, room variants, traps, loot, seeded generation, and transitions.
3.2
Temple Dimension
Dimension registration, temple access, multiplayer handling, and original-temple isolation.
3.3
Sanctums \& Celestial Combat
Regular Wrath, Origin Wrath, two sanctums, and Above All's Gentle Boop.
3.4
Patchouli Diary \& Lore
Progressively unlocked lore and the final Diary of an Axolotl revelation.
4+
Future Expansion \& Polish
Further content, balancing, compatibility, and release preparation. Not yet fully scoped.
3. Stage 1 — Companion and mod foundation
Working foundation
Standalone NeoForge mod project.
Custom Axo entity, model, and GeckoLib animation groundwork.
Existing Axo animations established and protected from unnecessary changes.
Initial custom item and creative-tab registrations.
Early texture and model integration.
Axo Cuisine registration and recipe groundwork.
Finish remaining companion interactions and behaviors.
Verify the complete companion gameplay loop.
Final polish, sounds, and animations.
Core principle: The original Axo companion remains part of the project, even as the mod expands into a full campaign.
4. Stage 2 — Items, enchantments, and progression
Partially implemented
Axolotl Absolute item and early ability prototypes.
Blocking/offhand behavior and cooldown groundwork previously tested.
Early enchantment and Rebirth integration.
Finish Eternal Guard, including its two-minute recharge and lethal-damage protection.
Finalize interactions with Totems and Revive Me.
Finish the Absolute's custom visual design.
Complete Black and White Axolotl Trophy progression.
Finalize progression requirements and recipe unlocks.
Connect the Ring of the One's Yang Singularity to its intended enchantment mechanics.
The Axolotl Absolute's planned visual identity includes a pale axolotl, golden frills, a halo, and yin-yang imagery.
5. Stage 3.1 — Procedural maze
Current development focus
Generator and floor architecture
`/axo\\\_maze generate \\\[seed]` command prototype.
`/axo\\\_maze undo \\\[confirm]` and snapshot groundwork.
Seed-driven generation and room rotation.
Prototype multi-floor maze.
Structure-template placement.
Expand the prototype's 5×5 room grid toward the intended 9×9.
Finish all ten floors.
Complete the 1 + 2 + 3 + 4 floor-stage arrangement.
Finish variant selection and weights.
Test room connectivity, dead ends, and seed reproducibility.
Verify undo and snapshots for larger structures.
NBT room catalog
Room	Purpose
`tota\\\_straight`	Standard corridor
`tota\\\_lavastraight`	Lava parkour corridor
`tota\\\_decoy`	Deceptive trap-filled chamber
`tota\\\_corner`	Standard corner
`tota\\\_lava\\\_corner`	Lava parkour corner
`tota\\\_tjunction`	Standard T-junction
`tota\\\_traptjunction`	Trap-filled T-junction
`tota\\\_fourway`	Four-way junction
`tota\\\_deadend`	Standard dead end
`tota\\\_bait`	Empty trapped-chest bait room
`tota\\\_chamber\\\_entrance`	Entrance-room template
`tota\\\_bubbleelevator`	Vertical floor connection
`tota\\\_loot\\\_to\\\_next\\\_layer`	Multi-floor stage transition
`tota\\\_entrance\\\_to\\\_sanctum`	Special final descent, planned
Dimensions: Ordinary room cells are 15×16×15; existing two-level transition structures are 15×32×15. The proposed sanctum descent is 15×48×15.
Planned room-selection weights
Connection type	Variants
Straight	60% normal, 25% lava, 15% decoy
Corner	70% normal, 30% lava
T-junction	65% normal, 35% trapped
Dead end	80% normal, 20% bait
These are proposed balancing weights, not yet confirmed in the generator.
Trap system
Trap	Intended behavior
RNGesus	Curse, darkness/blindness, possible teleport
Spike	Three hearts of damage
Honeybrick	Movement and jump impairment
Burn	Ignites players standing on it
Four custom trap block types and runtime behavior prototyped.
RNGesus teleport marker approach using chiseled stone bricks.
Twenty-percent teleport chance designed into the trap behavior.
Test traps thoroughly across rotated and generated rooms.
Verify that trapped bait chests are excluded from ordinary loot filling.
Finish loot placement, vertical connections, and the final sanctum entrance.
6. Stage 3.2 — Temple Dimension
Planned
Register the dedicated Temple Dimension.
Implement temple entry and return behavior.
Connect generated maze instances to their seeds.
Allow multiplayer participation in ordinary decoy temples.
Reserve seed `00001` for the original Temple of the Axolotl.
Require the original temple's special key.
Upon entry, lock the dimension against every player except the keyholder.
Keep all ten floors and the Origin Sanctum solo.
Ensure safe lock cleanup after disconnects, deaths, and server restarts.
Important canonical rule: The original temple's isolation begins when the keyholder enters seed `00001`, not when Origin Wrath spawns.
7. Stage 3.3 — Two sanctums and celestial combat
Mechanics designed • Implementation pending
The two sanctums
Feature	Regular Sanctum	Origin Sanctum
Location	Ordinary decoy temples	Original temple `00001`
Participants	Multiplayer allowed	Exactly one keyholder
Boss	Axolotl's Wrath	Origin Wrath
Difficulty scaling	Party size + curse	YOU SHOULDN'T HAVE
Arena	Built: 60×32×60	Unique layout TBD
Story purpose	Guard key fragments	Guard Above All's prison
Regular sanctum built: 60×32×60 deepslate cathedral, floating sea-lantern cosmos, 30-block starry yin-yang arena, flooring completed.
Small axolotl statues built: planned for installation in the regular sanctum. Two original statue daises now display protected Yin/Yang Singularity visual recipe clues using glow item frames behind stained glass (`Fixed`/`Invulnerable` NBT).
Origin sanctum remains to be built: unique architecture, dimensions, prison connection, and solo encounter staging TBD.
Finish the dedicated final descent.
Add boss spawning, health displays, animations, and attack AI.
Implement boss defeat and reward handling.
Connect the Origin encounter to Above All's prison.
Regular Axolotl's Wrath
Difficulty	Party size	Curse level	HP
Easy	1–2	0–2	250
Medium	3–4	3–6	500
Hard	5+	7+	750
Difficulty uses whichever factor produces the higher tier.
Wrath's combat abilities
Easy — Dive Bomb
Wrath struggles to fly with his torn wings, targets a player, and crashes downward with an igniting impact. His damaged wings are a consequence of his fall from grace.
Medium — Celestial Rupture
A red horn beam with a 15-block gravitational pull during charging. Wrath locks his facing before firing, and the stationary beam deals damage once per second for three seconds. Exact damage tuning remains TBD.
Hard — Rampage
Ten successful hits trigger 15 seconds of Rampage: attack cooldowns −50%, damage +25%. Successful hits during Rampage contribute toward future Rampages, allowing chaining.
Curse level 10+
Wrath prioritizes the highly cursed player. During Celestial Rupture, the gravitational pull continues through beam firing for that primary target.
Other established details include Wrath attempting to stretch his damaged wings during idle animations before folding them back in frustration.
Origin Wrath
Origin Easy begins at Regular Hard's baseline (750 HP and its existing attacks). Origin Medium/Hard add confirmed defensive rules; their additional HP and Origin-exclusive offensive attacks remain undecided.
Kill count	YOU SHOULDN'T HAVE	Origin difficulty
0–10	I	Easy
11–20	II	Medium
21+	III	Hard
Implement the three-tier kill-based scaling.
Kill-count rule decided: every Regular Wrath kill / decoy temple defeated by the player contributes to the persistent personal count. Prevent double-counting a single clear; exact event wiring TBD.
Establish Origin Medium and Hard HP above the 750-HP baseline; not yet decided.
Design Origin-exclusive attacks or enhanced versions of existing abilities.
Remove party-size scaling and multiplayer target priorities.
Finish the encounter's connection to the prison rescue.
Origin Wrath is always a solo fight, because temple `00001` has already locked out everyone except its keyholder.
NEW — Axolotl Above All: Gentle Boop™ 🐟
Gentle Boop
Above All's signature defensive attack
Design decided
Damage
Wolf bite
Knockback
20
Attack type
Face boop
Aggression
Wolf-style
Attack behavior
Normally peaceful and nonaggressive.
Defends himself when attacked.
Protects his bonded player using tamed-wolf-style targeting behavior.
Can assist when his player attacks a valid enemy.
Performs a tiny forward `owo` face-boop animation.
Deals ordinary wolf-bite-level damage.
Applies knockback strength 20 through a custom knockback implementation.
Returns to his peaceful behavior once the threat is resolved.
owo
Target has been politely relocated.
Design contrast: Wrath pulls enemies inward to destroy them; Above All pushes enemies away to protect others.
And yes, buddi, the boop uses his FACE, not his flippers. XDDDD
8. Stage 3.4 — Patchouli: Diary of an Axolotl
Fully conceptualized • Not implemented
This stage comes after the maze, dimension, and sanctums.
Integrate Patchouli's guidebook system.
Create a permanent cover with a central yin-yang, halo above, and flaming horns on either side.
Give the book an initially obfuscated title.
Write lore entries from Above All's first-person perspective.
Unlock entries through item acquisition, recipes, and advancements.
Use hidden advancement flags for progression gating where appropriate.
Explain the celestial brothers, the Ring of the One, Wrath's fall, the scattered key, and the temples.
Connect entries to discoveries made during gameplay.
Trigger the book's title reveal when Above All's prison cell opens.
Before rescue
Obfuscated title
Prison cell opened
Diary of an Axolotl
Same book. Same cover. Same contents. Only the title changes.
The reveal establishes that the player has been carrying Axolotl Above All's personal memories throughout the adventure.
9. Established story canon
Two celestial brothers embody creation (Axolotl Above All) and destruction (Axolotl's Wrath).
The collision of their powers creates the Ring of the One, the center of the Axolotl-verse.
Wrath attempts to claim the Ring's power alone, even though neither brother can wield it independently.
The Ring burns and tears Wrath's wings, causing his fall from grace and leaving him unable to fly normally.
Above All casts the Ring into the Minecraft-verse, hoping a worthy wielder will eventually awaken it.
Wrath imprisons Above All in the first temple, shatters the key twice, and creates countless decoy temples to conceal its fragments.
The player collects fragments, obtains access to temple `00001`, and enters alone.
After ten floors and Origin Wrath, the player reaches Above All's prison, opening the way to the diary's true identity.
The Ring's Yang Singularity is tied to the celestial enchantment system. The brothers' personalities are also reflected in combat: destructive gravity versus protective knockback.
10. Future ideas and unfinished decisions
Feature or question	Status
Black/White Axolotl Trophy progression	Planned
Axolotl Absolute's finished abilities	In progress
Key-fragment acquisition and crafting	Planned
Invulnerable Axo throne encounter	Early concept
Temple-specific durable blocks	Planned
Server-side progression and access locks	Planned
Temple relocation and dead-coral marker	Early concept
Optional Cold Sweat integration	Earlier idea
Axo Cuisine expansion	Partially implemented
Origin Wrath Medium/Hard abilities	TBD
Origin Sanctum's exact dimensions	TBD
YOU SHOULDN'T HAVE kill categories	TBD
Gentle Boop sound and animation timing	TBD
Stage 4's main direction	Not decided
11. Our next development checklist
Development queue
0 / 12
Finish maze room variants
NBT templates, rotations, seeded selection
Expand and finish the maze
Ten floors, connectivity, loot, transitions
Finish maze testing
Traps, snapshots, undo, seed reproducibility
Create Temple Dimension
Dimension registration and normal temple access
Implement seed 00001 lockdown
Keyholder-only access from entry
Build regular sanctum — COMPLETE (arena build)
60×32×60 arena and cosmos built; statue models ready, installation pending
Build Origin Sanctum
Unique original-temple arena and prison connection
Implement Regular Wrath
AI, difficulty, attacks, rewards
Implement Origin Wrath
Solo scaling, unique mechanics, defeat flow
Implement Gentle Boop
Wolf-style defense, face animation, knockback 20
Integrate Patchouli
Lore pages and advancement gating
Implement diary reveal
Rename upon opening Above All's prison cell
 Copy checklist
12. Current overall status
System	Status
Core mod and Axo entity	Working foundation
Axo companion gameplay	Partial
Custom items and enchantments	Partial
Trap runtime	Working prototype
Procedural maze	Working prototype
Temple Dimension	Not yet implemented
Regular Sanctum	Arena built; statues pending placement
Origin Sanctum	Not yet built
Regular Wrath	Combat design established
Origin Wrath	Core rules established
Above All's Gentle Boop	Attack design established
Celestial lore	Major storyline established
Patchouli diary	Designed, pending implementation
Temple of the Axolotl
One companion. Two celestial brothers. Countless temples. One original.
CURRENT PRIORITY: STAGE 3.1 — FINISH THE MAZE; ALSO BUILD ORIGIN SANCTUM, IMPLEMENT AMBROSIA AND WRATH
The full updated roadmap, including our newest addition:
Axolotl Above All — Gentle Boop™
Damage: wolf bite.
Knockback: 20.
Animation: `owo`.
Wrath spent eternity mastering gravity just to get outperformed by his brother's forehead. XDDDD 😭
---
13. October 15 expansion — origin difficulty and combat rules
Status: design confirmed; code/field testing pending. The regular boss retains its cumulative kit: Easy (250 HP) uses the torn-wing dive-bomb slam and ignition; Medium (500 HP) adds the stationary horn beam, Celestial Rupture (working name), with 15-block pull during charging, pull ending one tick before firing, and one damage event per second for three seconds; Hard (750 HP) adds Rampage after 10 successful hits (15 seconds, 50% shorter cooldowns, +25% damage, hits during Rampage count toward subsequent triggers). Curse 10+ extends the pull on the priority target throughout the beam. Regular difficulty is the higher tier determined by party size or curse.
Origin Wrath — solo-only, personal kill-count scaling
Personal Regular Wrath kills	YOU SHOULDN'T HAVE	Tier	Confirmed properties
0–10	I	Easy	At least 750 HP, all Regular Hard abilities; ordinary potion buffs, totems, Eternal Guard, ranged and explosion damage still work.
11–20	II	Medium	All Easy abilities; Divine Suppression disables all potion effects, including armor potion passives, Ambrosia blessings, and totem-provided recovery effects. Totems/Eternal Guard can still prevent a lethal hit, but grant no potion-based recovery. Immune to ranged/projectile and explosion damage except Create Potato Cannon ammunition. Melee remains valid.
21+	III	Hard	All Medium rules, plus death-prevention suppression: Totems of Undying and Eternal Guard cannot avert a lethal hit. Innate powers (e.g., Ring lifesteal, Celestial Aegis, Divine Leap, armor attribute bonuses) remain unless independently countered.
Origin-only extra HP and new offensive moves: TBD. Second Fall exception: immediately forces Origin Hard regardless of personal kill count and grants Ring mastery (Section 17).
Create easter egg: If Create is installed, its Potato Cannon is the one projectile exception to Medium/Hard immunity. Golden carrots are particularly potent ammunition (user's example: \~10 hearts / 20 HP per hit), but their steep projectile arc makes hits difficult. Lore explanation: golden carrots remind Wrath of his brother, who is tamed with golden carrots. Treat exact ammo damage as a balance/test target, not a verified Create stat. Integration must be optional when Create is absent.
Combat implementation checklist
\[ ] Regular Wrath model, boss textures, GeckoLib animations, torn-wing idle attempt, dive/landing and fire impact.
\[ ] Medium horn-beam charge, 15-block pull, lock facing, three timed beam hits; curse-10 priority behavior.
\[ ] Hard Rampage successful-hit tracking, 15-second duration, cooldown/damage modifiers, carryover hit counts.
\[ ] Regular tier selection, party/curse detection, reward chest with guaranteed key fragment and 40% Wrath fragment drop.
\[ ] Origin model/animations and solo targeting; persistent personal kill counts and tier selection.
\[ ] Origin potion suppression, projectile/explosion immunity, optional Potato Cannon exception, lethal-save suppression on Hard.
\[ ] Verify innates vs potion passives and interaction with Totems, Eternal Guard, Ring lifesteal and modded attacks.
\[ ] Finalize Origin Medium/Hard HP, new attacks, exact beam damage, boss defeat and prison unlocking.
14. Axolotl Absolute armor, Ring and key economy
Design confirmed; implementation status requires code inspection. Every armor piece provides an innate +5 max HP; a full set adds +20 HP (10 hearts), bringing normal 20 HP to 40 HP (20 hearts). This innate attribute survives Divine Suppression.
Piece	Potion-based passive (suppressed on Origin Medium/Hard)	Innate properties (generally retained)
Helmet	Strength I, Night Vision I, Conduit Power I	+5 max HP; protects against Ambrosia freezing but not faster drowning
Chestplate	Haste I	+5 max HP; Celestial Aegis deflects incoming projectiles along wearer's look vector; Eternal Guard prevents an otherwise lethal hit, then has a 2-minute recharge (disabled in Origin Hard)
Leggings	None	+5 max HP; +10% swim speed, +15% underwater mining efficiency, immunity to Elder Guardian Mining Fatigue
Boots	None	+5 max HP; double jump via second midair Jump press, wind-charge-like upward boost; reset on landing
Helmet crafting: top row three Shards of Divinity; middle row White Axolotl Trophy / Ring of the One / Black Axolotl Trophy; bottom row three Beacons. The Ring is returned, not consumed, but reset to its default unenchanted state (rare enchantments lost). Recipes for the other three pieces remain TBD, though Shards of Divinity will be required. No offhand-dependent armor abilities.
Progression economy: 4 Shards of Divinity → 1 Orb of Envy; 9 Orbs of Envy → 1 Celestial Key (36 shards total). Armor and key crafting compete for shards; farming more decoy temples also increases the personal Wrath kill count, making the final encounter harder. Ring of the One has innate lifesteal (direct healing on successful attack, not a potion) and rare Ring-specific enchantments such as Yang Singularity. Level 4 temple loot rooms and sanctum loot rooms can yield Ring enchantments, with sanctum rooms offering the highest chance. Exact enchantment effects and rates TBD.
15. Ambrosia — Elixir of the Gods
New custom fluid: not implemented. Golden, luminous fluid with light level 8, flowing a little slower than Overworld lava. Both source and flowing Ambrosia disappear 30 seconds after placement by permeating into ordinary mortal blocks; this is a one-way bucket (cannot be collected again). Define timing carefully for new flow blocks so the effect does not create permanent sources.
Situation	Intended effect
Any player submerged	Drowning/air depletion approximately twice as fast as in water
Without Absolute Helmet	Powder-snow-like freezing effect
With Absolute Helmet	Freezing prevented, accelerated drowning still applies
Wearing Helmet while immersed	Strength II, Regeneration IV, Absorption V, Resistance I
After leaving fluid	Above blessings persist for 30 seconds
Origin Medium or Hard fight	Divine Suppression nullifies Ambrosia potion blessings
Implementation checklist: register fluid/source/flowing variants, block and bucket; render glow/color and light; implement non-recoverable bucket behavior; persist source/flow expiry safely; manage 30-second expiration; custom freezing/drowning; helmet detection and blessing duration; prevent Ambrosia from bypassing boss suppression; test fluid spread and chunk unload/reload. Optional Create fluid integration can be evaluated later.
16. Secret endings and companion outcomes
A. The Path of Forgiveness — peaceful Origin ending
Trigger: during Origin Wrath's encounter, the solo keyholder types exactly `Sorry I hurt you, but i also forgive you` in chat. Available only with YOU SHOULDN'T HAVE I (0–10 Regular Wrath kills). When successful, Origin Wrath stops fighting, shrinks to the size/model family of Axolotl Above All, and becomes tameable with a Soul Campfire. This is a nonviolent alternate resolution; the final prison-rescue flow must be integrated so the story remains completable. Define handling for capitalization/whitespace and multiplayer chat interception during implementation; the quoted string is the canonical phrase.
Redeemed Wrath visual: Above All-sized replica geometry and curiosity animation, but onyx body, ruby eyes (instead of sapphire), same amethyst central crown crystal, magma/lava-like fins and gills, and orbiting red Reality Stone and yellow Mind Stone gemstones. Restored wings allow flight like Above All. Wrath retains his pounce because he became accustomed to it during his long flightless exile.
Companion role contrast: Above All remains the rideable mount and protective Gentle Boop™ user (wolf-like damage, knockback 20, face `owo` boop). Redeemed Wrath is not rideable; he is a flying combat companion, physically gripping a netherite sword or netherite axe in his flipper. No modded weapons or other items are equipable in the weapon slot, except the Ring's deliberate lore-trap interaction below.
Redeemed Wrath's pounce: base 8 attack damage (4 hearts), plus the equipped weapon's damage; e.g., netherite axe +9 → 17 damage (8.5 hearts) before armor. Pounce ignites the enemy and applies 3 seconds of Slowness, Blindness, and a custom mob AI disable preventing movement/attacks; plain vanilla effects alone are insufficient to disable hostile mob AI. Wrath then retreats, waits for the actual sword/axe attack-recharge timing, and pounces again, like a player timing fully charged attacks. Crit-like rhythm is confirmed; any extra critical-hit multiplier is not established. This sets up Above All's knockback-20 boop after the enemy regains vision.
B. The Second Fall — secret catastrophic ending after redemption
Trigger: player deliberately offers the Ring of the One to an already-redeemed, tamed Wrath. It is not an allowed weapon equip; it is a unique interaction. Show an ominous confirmation prompt to avoid accidental irreversible activation, e.g., “He recognizes the Ring. His eyes betray a familiar hunger. Offer it anyway?” (suggested wording).
Confirmed consequences:
Wrath accepts and takes the Ring; his restored wings are ruined again, and he reverts from small companion form to the enormous fallen Origin Wrath boss.
He uses his celestial powers to instantly apply every Ring enchantment to the Ring, regardless of which enchantments the player's copy had. This partitions the Ring's powers before its original combined force can destroy him; he can now use the Ring.
The Overworld sky blackens; time/moon presentation is forced to midnight under a new moon. Define restoration of world time/weather/moon after the event as an implementation task.
Nearby ground and ordinary solid blocks convert to blackstone. Exclude block entities (chests, machines, inventories, etc.) and protect their support/dependent blocks where necessary to prevent griefing. Define safe conversion radius and restoration policy; neither is established yet.
He is now Hard-tier Origin Wrath regardless of the player's kill count, with all Hard immunities and restrictions plus his newly mastered, fully enchanted Ring. This takes place in the Overworld, outside the sanctum's containment.
Redeemed Wrath is lost as a tameable companion; he cannot be tamed again after the Second Fall. Whether the Ring can be recovered on defeating this boss, whether he roams, and the precise encounter termination are TBD; do not assume permanent item deletion beyond his taking it.
Why he knows how: across the player's victories over Wrath in decoy temples and the original temple, Wrath observed the mortal enchanting items to enhance them and bypass limitations. The player unknowingly taught him the solution to the Ring's restriction: split its combined power across separate enchantments. The Second Fall is the horrifying payoff to that lesson. This is not a new Ring ability invented from nothing, and it does not require the player to pre-enchant the offered Ring.
17. Revised immediate development queue
\[x] Build regular sanctum's 60×32×60 arena, cosmos, yin-yang floor, and recipe-clue daises.
\[x] Create the small axolotl statue models for later placement.
\[ ] Place/finalize small statues in regular sanctum; verify protected item-frame recipe clues.
\[ ] Finish remaining maze templates, 9×9 expansion, ten floors, traps, loot, seeded testing, and final descent.
\[ ] Implement Temple Dimension and seed `00001` single-keyholder lock.
\[ ] Build Origin Sanctum and Above All prison-cell approach.
\[ ] Implement Ambrosia fluid with 30-second lifetime, drowning/freezing and helmet blessings.
\[ ] Finish Absolute armor, Ring enchantments, shards/orbs/key progression.
\[ ] Create Regular Wrath boss entity, model/animations and Easy/Medium/Hard skill sets.
\[ ] Create Origin Wrath model, three difficulty tiers, suppression/immunities and optional Potato Cannon exception.
\[ ] Implement Above All's mount/companion AI and Gentle Boop™.
\[ ] Implement Path of Forgiveness, Redeemed Wrath's visuals/flight/weapon+pounce AI.
\[ ] Implement Second Fall confirmation, full-Ring enchanting, world transformation and Overworld Hard boss.
\[ ] Integrate Patchouli diary, discovery flags and final title reveal.
\[ ] Test edge cases: world grief protection, restart persistence, solo lock, effect suppression, companion ownership and optional-mod compatibility.
18. Current project status — October 15, 2026
Area	Current status
NeoForge/GeckoLib foundation, Axo	Working foundation; gameplay incomplete
Items, armor, Ring, key progression	Partial prototypes / detailed design; further implementation needed
Procedural maze, trap blocks	Working prototypes; ten-floor 9×9 version pending
Temple Dimension / original lock	Planned
Regular Sanctum	Arena built; small statue models made, placement pending
Origin Sanctum	Not built
Ambrosia	Fully specified concept; not implemented
Regular Wrath / Origin Wrath	Difficulty skill sets designed; boss entities pending
Above All Gentle Boop	Design established; implementation pending
Forgiveness / Redeemed Wrath	Detailed design; implementation pending
Second Fall	Detailed story and mechanics; implementation pending
Patchouli diary	Story/reveal designed; implementation pending
---
19. Full chronological lore — The Two Celestial Brothers and the Ring of the One
This final section records the agreed narrative, including both optional endings. Gameplay values and unresolved implementation choices remain in the preceding sections.
Before the Minecraft-verse
At the heart of the Axolotl-verse were two celestial brothers: Axolotl Above All, associated with creation, protection and preservation, and Axolotl's Wrath, associated with destruction and an ever-growing jealousy of his brother. Their opposed powers collided and formed the Ring of the One, the heart of their universe. Neither brother could command the Ring's entire power independently; it was born of both.
Wrath coveted that power for himself. He attempted to claim the Ring alone, but its power burned and tore his wings. His fall from grace caused his inability to fly, not the other way around. Above All cast the Ring into the Minecraft-verse, hoping a worthy wielder might one day awaken its potential.
Imprisonment, fragments and false temples
Wrath, unwilling to let anyone else possess what he could not, imprisoned Above All in the deepest chamber of the original Temple of the Axolotl, identified by seed `00001`. He shattered the temple's key twice and created countless decoy temples, scattering the fragments among them. His motivation: if he could not have the Ring, no one could.
Players explore decoy temples, survive the increasingly complex ten-floor mazes, and confront Regular Wrath in their sanctums. They acquire shards, fragments and celestial equipment. Each victory, however, adds to their personal YOU SHOULDN'T HAVE count, increasing Origin Wrath's hostility. Wrath appears to be losing repeatedly, but he is also watching: the player uses enchantments to make ordinary items stronger and circumvent their limits.
The original temple and the diary
The player eventually crafts the Celestial Key and enters the original temple. The moment the keyholder enters seed `00001`, the temple locks out everyone else. The ten floors and Origin encounter must be completed alone. Wrath's torn wings, gravity attacks and anger reflect his ancient fall; the higher the player's prior kill count, the more ruthlessly Origin Wrath suppresses the powers on which they rely.
Throughout the journey, the player carries an initially obfuscated Patchouli diary. Its cover is constant: a yin-yang symbol, halo and flaming horns. Entries unlock as discoveries are made and are told in Above All's first person. Upon opening his prison cell, the same book quietly changes its title to “Diary of an Axolotl”. The cover and contents remain; the player realizes whose memories they have been carrying.
The ordinary victory
In the standard path, the player defeats Origin Wrath, reaches Above All's prison and frees him. Above All can become a loyal, rideable companion. His tiny face-boop sends enemies away, in direct contrast to Wrath's pull toward destruction. Golden carrots are used to tame Above All; the same food evokes Wrath's memories of his brother, explaining why golden-carrot Potato Cannon shots can pierce his otherwise formidable projectile defense.
Secret path: forgiveness and reunion
If the player has defeated Regular Wrath no more than ten times, they can choose mercy during the Origin encounter by typing “Sorry I hurt you, but i also forgive you” in chat. Wrath stops fighting and shrinks to a companion form matching his brother's size and curious mannerisms. He can be tamed with a Soul Campfire. His wings are restored, and he can fly again, but he retains the pounce slam he learned during his long flightless existence.
He is unmistakably himself: onyx skin, ruby eyes, molten fins and gills, an amethyst crown center, and orbiting red Reality and yellow Mind Stones. Above All remains the mount and protective guardian; redeemed Wrath becomes a flying, flipper-armed companion wielding a netherite sword or axe. His pounce disables enemies briefly, allowing Above All's Gentle Boop™ to finish the brothers' unlikely tag-team defense. Forgiveness reunites the brothers without erasing Wrath's personality.
Secret path within the secret path: the Second Fall
After redemption, the player can choose to offer Wrath the Ring of the One. An ominous warning asks for deliberate confirmation. The Ring was the source of Wrath's first fall, and he recognizes the temptation immediately.
This time, however, the player has unknowingly taught him something crucial. Across the decoy temples and the original battle, he observed the player enchanting items to separate and amplify powers. Upon receiving the Ring, Wrath uses his celestial abilities to place every Ring enchantment on it at once, distributing the powers before the artifact can destroy him. He has learned to wield what once rejected him.
His restored wings are ruined again, his companion form becomes the enormous Hard-tier Origin Wrath, and the Overworld is engulfed by an unnatural new-moon midnight. Nearby ordinary terrain turns to blackstone, while storage blocks and block entities are protected from conversion. He takes the Ring and cannot be tamed again. Unlike the first fall, he is not trapped in a sanctum: the player has brought a fully Ring-empowered celestial threat into their own world.
The cruelest irony is that the player already had a peaceful ending. They forgave Wrath, reunited the brothers and earned his trust — then gave him the artifact that had consumed him. The player did not merely hand him the Ring; through every earlier victory, they taught him how to use it.
The final thematic choice: mastery through conquest, reconciliation through mercy, or catastrophe through temptation. The brothers can find peace together — but the player can still choose to repeat the tragedy, this time with consequences beyond the temple walls.
```

## 6. V1–V8 planning audit — preserved in full

# Temple of the Axolotl — V1–V8 Master Planning Audit

**Review basis:** Master Roadmap V8 (October 24, 2026), including preserved V7/V6/V5/V4/V3 and earlier historical material, plus decisions in the current conversation through MegaCheckpoint 08-D1.5.7. **This is a planning audit, not a fresh Java code audit.**

**Legend:** `[x]` = previously reported complete/working; `[ ]` = candidate to keep/defer/cut. **KEEP / DECIDE / VERIFY / DEFER** are initial sorting suggestions, not final decisions. An old 'planned' label does not override newer user-reported success. Some V1/V2 details survive only as historical text inside later appendices.

## A. Core world and campaign — KEEP
- [x] NeoForge 1.21.1 core mod, custom Axo entity, GeckoLib infrastructure and registrations.
- [ ] Finish the original Axo companion's full interaction, sound, animation and gameplay loop. **VERIFY**
- [x] Natural Overworld temple structure generation and temple portal foundation.
- [x] Ten-floor procedural dungeon generation (802 modules, three placements per server tick, as previously tested).
- [x] Shared Temple dimension and basic session/portal lifecycle (single-player).
- [x] Automatic temple teardown and deep-clean/reset; `/templefullreset` fallback.
- [x] Developer portal and retired `/axo_maze` commands.
- [x] Dungeon generation messages and logout-triggered extraction detection.
- [ ] Fresh-world temple distribution: 1,000-block spawn exclusion, 66-chunk spacing/52 separation. **VERIFY**
- [ ] Server-restart resilience, multi-temple concurrency, multiplayer ownership, portal retirement and recovery. **KEEP**
- [ ] Confirm adventure-mode, timed Saturation III, game-mode restoration and dimension restrictions. **VERIFY**
- [ ] Confirm Origin Temple seed `00001` and solo keyholder lock from entry. **KEEP**
- [ ] Finalize Origin Sanctum, prison corridor and Above All rescue sequence. **KEEP**
- [ ] Finish regular Sanctum statues, protected recipe-clue daises and arena encounter integration. **KEEP**
- [ ] Finish all traps, room-specific loot, treasure hallways and campaign-level progression. **KEEP**

## B. Dungeon loot, extraction and economy — KEEP
- [x] Reward staging and per-run binding foundation (previously tested).
- [x] Separate Celestial Repository backend: 56 unique data-component-aware variants, 32 native stacks each, safe partial deposits and withdrawals; leave/rejoin persistence tested.
- [x] Dedicated Dungeon Lootbag/Reward Vault exists as separate storage interface; recent screenshot showed restricted treasure content.
- [ ] Confirm chest slots grouped **20 / 4 / 3** by eligible rarity, not 27 identical rolls. **KEEP**
- [ ] Finalize per-tier rarity probabilities and Sanctum 65% Rare / 25% Epic / 10% Legendary interpretation within eligible slots. **DECIDE**
- [ ] Preserve four rarities: Common, Rare, Epic, Legendary; tier unlocks at Levels 1/2/3/4. **KEEP**
- [ ] Legendary reward: 97% enchanted-book category vs 3% replacement Shard; stronghold-style books dilute Ring-exclusive books. **KEEP**
- [ ] Decide stronghold-book dilution weights and Celestial Convergence level distribution. **DECIDE**
- [ ] Preserve 40% Regular Wrath Shard of Divinity bonus, separate from guaranteed key fragments. **KEEP**
- [ ] Verify UUID-owned Sanctum final chest, 30-second countdown, Extract Now, and exactly-once successful lootbag delivery. **VERIFY**
- [ ] Verify lethal-damage dungeon failure removes only dungeon-generated Rare/Epic/Legendary rewards, retaining brought-in inventory, XP and Common loot. **VERIFY**
- [ ] Define and enforce exploit-proof reward claims, provenance and transfer to/from external storage. **KEEP**
- [x] Boss/title music disc, rare dungeon discs, Cosmic Essence and modules in loot tables were reported working in the latest tests.
- [ ] Regression-test actual weights, mineshaft Cosmic Essence, chest rarity restrictions and multiplayer loot ownership. **VERIFY**
- [ ] Preserve 4 Shards → Orb of Envy; 9 Orbs → Celestial Key (36 Shards). **KEEP**

## C. Celestial workstations, items and interface — mostly VERIFY
- [x] Cosmic Brewing Stand custom placed GeckoLib model, golden nested-cube tesseract, animation and corrected ground geometry.
- [x] Celestial Anvil new hand-painted purple/gold model, starry surface, and user-approved scale/orientation fix.
- [ ] Confirm both workstations harvest with diamond/netherite pickaxe **or** Ring of the One, and drop exactly one workstation. **VERIFY**
- [ ] Confirm both workstations retain inventories, saved state, item returns and correct block outlines/collision. **VERIFY**
- [x] Six-slot Cosmic Brewing Stand design: catalyst, water catalyst, input, output, heat and bucket.
- [ ] Validate 16,000 mB Diluted Ichor bank, heat buffer and three counters in the GUI. **VERIFY**
- [ ] Validate exact heat conversion: lava bucket 10; two magma blocks 1; campfire 1; soul campfire 2. **VERIFY**
- [ ] Validate water conversion: water bucket 1 or two Blue Ice 1; Shard of Divinity 10 catalyst charges. **VERIFY**
- [ ] Validate automatic brewing: 2 heat + 1 catalyst + 1 water charge → 1,000 mB Diluted Ichor; withdraw 1,000 mB into bucket. **VERIFY**
- [ ] Validate eligible duplicate module/disc/celestial-enchantment-book recycling, 10% Shard recovery and consumption/anti-duplication rules. **VERIFY / DECIDE exact trigger**
- [ ] Preserve Pure Ichor as treasure-hallway-only, never survival bucketable or placeable. **KEEP**
- [ ] Preserve Diluted Ichor world-placement non-recollection and 15-second source-removal decay if world fluid is retained. **DECIDE / VERIFY**
- [x] Cosmic Essence visual direction locked: **liquid cosmos inside a bottle**.
- [x] Celestial Anvil, Brewing Stand, Diluted/Pure Ichor buckets, Magnet and Jukebox icon artwork prepared/approved.
- [ ] Verify final icons installed in `textures/item` and item-model definitions; ensure 3D Anvil icon matches actual model. **VERIFY**
- [ ] Confirm Celestial Cache inventory/world textures, creative tab entries, spawn eggs and all `en_us.json` names. **VERIFY**
- [x] Celestial Jukebox full nine-slot disc inventory was user-accepted after patch.
- [ ] Verify pause/resume position, shuffle/repeat modes, queue, disc removal, persistence and playback on multiplayer/rejoin. **VERIFY**
- [ ] Fix custom title-theme routing to Music/Master volume and Jukebox to Records/Master; prevent overlap. **KEEP**
- [ ] Verify cinematic main menu and secondary screens, animated kelp/stars/bubbles, loading screen continuity and credits. **VERIFY**
- [ ] Preserve track attribution and provenance for ONECinematicStudio and Psychronic / Pixabay. **KEEP**
- [x] Celestial Modules / Repository / Lootbag / Cluckles GUI integration and compiler fix reported successful.
- [ ] Verify Cluckles employed-only side tab, Magnet toggle, R/L/H shortcuts, recipe-book hiding, GUI scaling and server synchronization. **VERIFY**
- [ ] Repository search/filter and sorting with stable entry identity, plus polish of 7×8 view. **DECIDE / VERIFY**
- [ ] Magnet 10-block pickup radius, ownership/pickup-delay checks, Repository vs Dungeon Lootbag routing, and safe overflow. **VERIFY**
- [ ] Optional configurable compact Celestial HUD (`/tota hud`, `/tota config`, themes/anchors, Pounce/Eternal Guard/effect displays). **DECIDE / DEFER**

## D. Companions and creature systems — KEEP
- [x] Cluckles custom model and animation patch reported successful; dungeon restrictions demonstrated with Cluckles waiting at prismarine portal.
- [ ] Verify rice-grain recruitment, one Cluckles per player, persistent ownership and default naming. **VERIFY**
- [ ] Verify Follow/Stay, item retrieval and delivery, radius, feather shedding, and conditional employment controls. **VERIFY**
- [ ] Finish/check Cluckles hidden challenge advancements, including “Gainfully Employed” and “I BAWKIN DID SO MUCH. HOW COULD YOU”. **DECIDE / VERIFY**
- [ ] Verify all non-wolf companions wait outside dungeon, resume after exit and survive unload/rejoin; wolves alone may enter. **VERIFY**
- [ ] Finish Axolotl Above All: rideable companion, protective AI and Gentle Boop™ knockback. **KEEP**
- [ ] Finish redeemed tameable Wrath: separate flying companion model, combat behavior, equipment and Soul Campfire taming. **KEEP**
- [ ] Finish separate spawn eggs and visual polish for tameable Wrath and Cluckles. **VERIFY**
- [ ] Sentinel/Guardian dry-corridor hovering, unwanted ignition and AI navigation fixes. **VERIFY**
- [x] Sentinel randomized names, trident vulnerability of Aegis and non-pickup Sentinel tridents previously tested.

## E. Celestial armor, Ring and combat powers — KEEP / VERIFY
- [x] Axolotl Absolute armor set models/textures/innates/passives reported complete, including chibi Allfather helmet and four passive controllers.
- [ ] Final helmet head tracking, vanilla skin/hat visibility, idle breathing and all armor animation regression. **VERIFY**
- [ ] Confirm actual armor attributes (+5 vs +10 max HP historical discrepancy) against latest source. **VERIFY**
- [ ] Confirm Eternal Guard 120-second lethal-save behavior and Totem/Revive Me interactions. **VERIFY**
- [ ] Confirm Celestial Aegis reflection exceptions, Pounce momentum and armor powers under Divine Suppression. **VERIFY**
- [ ] Finish the Ring of the One 3D presentation, item rendering and special effect interactions. **DECIDE / KEEP**
- [ ] Complete four Ring-exclusive enchantments: Supernova I, Event Horizon I, Lucky Stars I, Celestial Convergence I–VI. **KEEP**
- [ ] Validate Ring lifesteal, Yang Singularity, enchantment levels and progression. **VERIFY**
- [ ] Finish/verify eight Ominous Trap Stone sprites (I–IV intact/cracked), lava immunity, stonecutting and independent bulk loot rolls. **VERIFY**
- [ ] Confirm 0.1% per-stone Celestial Lottery and missing Celestial Enchantment Book prize (alongside Shard). **KEEP**
- [ ] Finish Silence/netherite trim presentation and suppress rice/koji debug spam. **DEFER / VERIFY**
- [ ] Correct trap-only Origin Wrath apparition: successful curse roll + teleport + persistent effect, then 20 ticks, translucent true Wrath eyes. **KEEP**
- [ ] Fix Night Vision expiration flicker without extending/reapplying effect duration. **DEFER**

## F. Regular Wrath and Origin Wrath — central campaign KEEP
- [ ] Import and animate final four-wing Fallen Boss Wrath model, crown/horns, gravity laser and articulated combat. **KEEP**
- [ ] Integrate Regular Wrath Sanctum encounter, AI and kill rewards. **KEEP**
- [ ] Regular Wrath difficulty based on greater of party size or curse: Easy 250 HP, Medium 500, Hard 750. **KEEP**
- [ ] Implement/verify Torn-wing Dive Bomb, Celestial Rupture gravity beam and 15-second Rampage after ten hits. **KEEP**
- [ ] Implement decoy escalation: 2.5×→3.0× scale cap, per-clear damage increase, shorter cooldowns, Rampage floor. **KEEP / DECIDE precise math**
- [ ] Implement Origin Wrath difficulty based on personal decoy kill count; solo lock, Divine Suppression and higher-tier immunities. **KEEP**
- [ ] Origin tiers: 0–10 Easy, 11–20 Medium, 21+ Hard; forgiveness only through 15 kills. **KEEP**
- [ ] Implement Origin boss unique attacks, final arena/prison encounter, defeat and Above All release. **KEEP / DECIDE moves and HP**
- [ ] Implement in-fight forgiveness exact phrase and restored tameable Wrath form. **KEEP**
- [ ] Implement post-victory maxed Ring soul-fire resurrection with Above All within four blocks, rejection message and safe invalid offerings. **KEEP**
- [ ] Implement The Second Fall: intentional Ring offering, confirmation, Overworld Hard Origin Wrath, midnight sky and protected blackstone conversion. **DECIDE / DEFER until main ending works**
- [ ] Decide duplicate resurrection/ownership safeguards, blackstone radius, repeat ritual and aftermath rules. **DECIDE**

## G. Lore, structures and environmental magic — KEEP / DECIDE
- [ ] Finish Patchouli diary with first-person Above All memories and final reveal as **Diary of an Axolotl**. **KEEP**
- [ ] Preserve creator cosmology: Axolotl Absolute created Above All and Wrath as separate beings. **KEEP**
- [ ] Implement scattered peaceful lore sites: ponds, ruined statues and carved monoliths. **DECIDE timing**
- [ ] Implement monolith right-click to unlock random **undiscovered** Lorebook chapter, then become depleted/broken; no scrolls, no duplicates. **KEEP if lore sites remain**
- [ ] Display exact actionbar **“The Story Seeps as the Magic Fades...”**; multiplayer first activator consumes monolith. **KEEP if implemented**
- [ ] Complete Origin Temple lore clues, Shard/Orb/Key hints, prisoner reveal and endings. **KEEP**
- [ ] Evaluate Ambrosia fluid (luminous gold, level 8 light, ~30s decay, freezing/drowning/helmet blessings). **DECIDE**
- [ ] **Important conflicting older design:** V4 Ambrosia water-cauldron recipe is historical; later Diluted Ichor cauldron draft was superseded by the custom Cosmic Brewing Stand. Do not conflate Ambrosia, Diluted Ichor and Pure Ichor or revive an obsolete recipe. **DECIDE whether Ambrosia remains separate**
- [ ] Optional Create Potato Cannon golden-carrot exception to Origin projectile immunity. **DEFER / DECIDE**

## H. Release engineering and quality — DEFER until features stabilize, but KEEP on roadmap
- [ ] Run full single-player campaign end to end, including rewards and all endings. **KEEP**
- [ ] Test dedicated server, multiplayer ownership, party-size scaling and client/server GUI authority. **KEEP**
- [ ] Test save/rejoin/restart, dimension transitions, death interception, cleanup and reward persistence. **KEEP**
- [ ] Audit all drop/loot paths for duplicate items, loss, overflow, and block-entity inventory persistence. **KEEP**
- [ ] Test mod compatibility: Create optional integrations, enchantments, resurrection mods and fluid/pipes rules. **KEEP**
- [ ] Optimize generation performance, entity AI, rendering and sound; reduce debug noise. **DEFER**
- [ ] Clean up NeoForge deprecations and compiler warnings (~18 initially; later compilation reported 40). **DEFER**
- [ ] Finish attribution, localization, accessibility, creative inventory, recipes, tooltips and release packaging. **KEEP**

## Suggested first sorting pass

**Keep active:** Wrath boss + Sanctums; Origin Temple; dungeon loot/extraction; Ring progression; companion loops; Patchouli diary; correctness of existing workstations and UI.

**Verify before building anything new:** harvesting/drop duplication; workstation icons; brewing charge/bank/recycling mechanics; jukebox volume; loot rarity/provenance; Cluckles and Magnet behaviors; multiplayer persistence.

**Decide deliberately:** Second Fall timing, lore monuments, Ambrosia as a distinct fluid, optional compact HUD, Repository sorting/search, optional Create easter egg, cosmetic trim/flicker polish.

**Not active / superseded:** earlier cauldron-based Diluted Ichor recipe; 27 identical loot rarity rolls; original 0.5% trap-stone bonus; earlier Origin forgiveness cap of 10; old anvil model; 3×3 craftable title disc; old GUI shortcut placement; the incorrect Wrath overlay-on-entry behavior.

