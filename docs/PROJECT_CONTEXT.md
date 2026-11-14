# CasesTD: Project Context (handoff for future AI conversations)

Paste this whole file at the start of a new conversation. It describes the project, what has been built, how it works, and what is left.

Last updated: Sun Oct 25, 2026.

---

## 1. Who and what

- **Project:** CasesTD, an OOP final class project (Group 4). A Bloons-style tower defense with zombies, plus a case-opening (gacha) layer. Players earn Coins from waves, open cases to unbox towers into an inventory, then place, upgrade, or sell them.
- **Me (TJ):** Role A, Database and Backend. I also wrote most of the playable game with AI help on Oct 25.
- **Teammates:** SHROD and MICKO write code. Other members do UI/UX (the visuals, still in progress) and documentation.
- **Professor's requirement:** the game must be **functional** by Thursday, Oct 29, 2026. The landing-page demo (Oct 22) is done.
- **Stack:** Java 21, Swing (custom painting), Maven, MySQL 8 (`btd_case_db`), Gson for JSON config.
- **Repo:** GitHub `teejay717/CasesTD` (private). Local folder: `C:\Users\Admin\Documents\Projects\CasesTD\CasesTD` (the folder with `pom.xml`).
- **How I like help:** brief, guided explanations; work fast; economy values and stats are placeholders the team can tune; docs should be pasteable into Google Docs. I need to understand the code well enough to defend it to my professor.

## 2. Git state

- `main`: landing page work (merged PRs #6 `feature/landing-page` and #7 `feature/landing-assets`). Other remote branches from teammates: `GameButton`, `HelpPanel`, `Themes`.
- **`feature/playable-game`**: the full playable game, created Oct 25, files written into the working tree. **Commit and push it if not done yet:**
  ```
  git checkout feature/playable-game
  git add -A
  git status        # make sure data/db_config.properties is NOT listed
  git commit -m "Add playable game: engine, towers, enemies, shop, UI"
  git push -u origin feature/playable-game
  ```
  Then open a PR into `main`.
- Never commit `data/db_config.properties` (git-ignored; holds my local MySQL login `casestd` / `casestd`).
- Windows line endings: the repo stores LF. If an editor saves CRLF, the whole file shows as changed in git. Converting back to LF fixes it.
- Still to check: whether SHROD or MICKO have game code on their branches that conflicts with `feature/playable-game`.

## 3. Setup and running

1. Run `database/schema.sql`, then `database/seed_data.sql` in MySQL (Workbench or phpMyAdmin). The schema drops and rebuilds `towers`, `cases`, `case_towers`, but keeps `players` (scores are not lost). **Every teammate must re-run both after pulling the playable-game branch**, because the towers table changed.
2. Copy `data/db_config.example.properties` to `data/db_config.properties` and fill in the login.
3. Optional check: run `btd.util.DbCheck`.
4. Run `btd.Main` **from the project root** (assets, data and config are read with relative paths).
5. In VS Code use the Java extension's Run, not Code Runner (Code Runner compiles one file and breaks packages).

## 4. Game rules (from the submitted report; all numbers are placeholders)

- Currency: Coins only. Start: 75 Coins, 1 free Standard Case, 20 lives.
- Cases: Standard 100 coins (Common 65 / Rare 25 / Epic 8 / Legendary 2, all 10 towers). Premium 250 (0/60/32/8, the 7 non-common towers). Legendary 500 (0/0/80/20, the 4 epic and legendary towers; odds and price are placeholders, the report did not define them).
- Rolling a case: pick a rarity by weight (rarities with no tower in the pool are skipped), then a random tower of that rarity. The result is decided when you pay; the spinner only reveals it.
- Inventory copies can be: placed for free at Level 1, used as upgrade copies, or sold.
- Upgrade cost in spare copies, by rarity (to Lv2 / Lv3 / Lv4 / Lv5): Common 1/1/2/3, Rare 1/1/1/2, Epic 1/1/1/1, Legendary 1/1/1/1.
- +10% damage and fire rate per level. Only one tower per type may reach Level 5 (others cap at Lv4). Level 5 unlocks a passive.
- Sell refund (placed tower or inventory copy): Common 30, Rare 50, Epic 75, Legendary 150.
- Wave clear reward: 100 + 10 x wave. Kill coins: Basic 10, Fast 7, Armored 28, Boss 500.
- Enemy scaling: health +35% of base per wave, times 1.25 again at the start of every 10-wave block (wave 11, 21, 31...); speed +2% per wave, capped at +50%. Boss every 10th wave. Endless (no last wave).
- Lives reach 0: run ends. **Highest score = waves cleared**, saved only if it beats the record.
- Opening cases mid-wave is allowed; the current build **pauses the game while the shop is open**.

## 5. Tower catalog (team decision, Oct 25; stats are placeholders)

| Tower | Class | Rarity | Dmg | Range | Rate/s | Special |
|---|---|---|---|---|---|---|
| Soldier | SingleTargetTower | Common | 10 | 130 | 1.5 | none |
| Grenadier | SplashTower | Common | 9 | 115 | 0.8 | splash 45 |
| Policeman | SupportTower | Common | 2 | 90 | 0.4 | STUN 1500 ms (2250 ms at Lv5), all enemies in range; enemies are stun-immune for 1.5 s after a stun |
| Scientist | SplashTower | Rare | 5 | 135 | 0.8 | splash 60, POISON 7 dps for 3 s |
| Sniper | SniperTower | Rare | 55 | 380 | 0.5 | targets highest-health enemy |
| Radio Tower | SupportTower | Rare | 0 | 150 | 1.0 | BUFF: +30% fire rate to nearby towers |
| Flamethrower | SplashTower | Epic | 3 | 95 | 6.0 | splash 35, BURN 4 dps per stack (max 5), 2 s |
| Engineer | SupportTower | Epic | 7 | 150 | 1.4 | TURRETS: 3 mini-turrets, each shoots a different enemy |
| Railgunner | SniperTower | Legendary | 140 | 600 | 0.4 | PIERCE up to 5 enemies in a line |
| Mortar | SplashTower | Legendary | 120 | 450 | 0.25 | splash 90 |

Level 5 passives: SingleTarget +50% fire rate; Splash +50% splash radius; Sniper x2 damage (and pierce count x1.5); Support +50% effect strength (stun time, buff, turret count).

Enemies: Basic (hp 30, speed 55, leak 1), Fast (hp 18, speed 105, leak 1), Armored (hp 90, speed 40, leak 2, takes 30% less damage), Boss (hp 1200, speed 32, leak 10, stuns last half as long).

## 6. Database (`btd_case_db`)

- `towers(tower_id, tower_name UNIQUE, tower_class ENUM(SingleTargetTower, SplashTower, SniperTower, SupportTower), rarity ENUM, base_damage, attack_range, fire_rate, splash_radius, special ENUM(NONE, STUN, POISON, BURN, BUFF, TURRETS, PIERCE), effect_value, effect_duration_ms, description, sprite_path)`
- `cases(case_id, case_name UNIQUE, cost, common_weight, rare_weight, epic_weight, legendary_weight, sprite_path)` with a CHECK that weights sum to 100 (needs MySQL 8.0.16+).
- `case_towers(case_id, tower_id)` many-to-many join, FKs with ON DELETE CASCADE. Expected rows: 21 (10 + 7 + 4).
- `players(player_id, player_name UNIQUE, highest_score)`; seed has sample players Sam 14 and Alex 9.
- `effect_value` meaning depends on `special`: POISON = dps, BURN = dps per stack, BUFF = % boost, TURRETS = turret count, PIERCE = max enemies hit. `effect_duration_ms` = STUN / POISON / BURN time.
- `sprite_path` points to `assets/sprites/towers/*.png`; those files do not exist yet, so the game draws placeholder shapes.

Tuning file `data/maps/waves_config.json`: economy (start coins, free cases, lives, wave reward, kill coins, sell refunds, upgrade copy costs), scaling, and hand-written waves 1 to 3. Waves 4+ are generated in code. Path: `data/maps/map1_path.csv` (corner points drawn for 1280 px width, scaled to the 960 px map area).

## 7. Code architecture (`src/main/java/btd`)

Layers: `ui` (draws, forwards clicks, no rules) → `core` (rules) → `model` (data and behavior of things) → `dao`/`config` (only code that runs SQL) → MySQL / JSON / CSV.

**model**
- `gacha/Rarity` (enum with display name, color, gem count), `gacha/CaseDef` (record: id, name, cost, 4 weights, pool), `player/Player`.
- `tower/TowerType` (record = one towers row, the blueprint), `tower/Special` (enum), `tower/Upgradable` (interface: getLevel, applyLevel).
- `tower/Tower` (abstract, implements Upgradable): final `update(dt, enemies, effects)` runs the cooldown and calls abstract `attack()`; abstract `applyMaxPassive()` and `getPassiveDescription()`; `applyAura()` hook; stats = base x LEVEL_MULTIPLIERS[level - 1] ({1.00, 1.45, 1.80, 2.03, 2.30}) x passive x buff.
- Subclasses: `SingleTargetTower` (furthest enemy), `SplashTower` (area, plus poison/burn), `SniperTower` (highest health; PIERCE beam), `SupportTower` (STUN all stunnable enemies in range, TURRETS multi-target, BUFF via applyAura).
- `enemy/Damageable` (interface), `enemy/Enemy` (abstract: health, speed, distance along path, poison/burn/stun, `takeDamage` calls overridable `reduceDamage`), subclasses `BasicEnemy`, `FastEnemy`, `ArmoredEnemy` (overrides reduceDamage), `BossEnemy` (overrides stun).

**factory**: `TowerFactory` (tower_class text → subclass), `EnemyFactory` (type name → subclass).

**core**
- `GameMap`: path, `pointAt(distance)`, `distanceToPath`, `canBuildAt` (38 px clearance, below 60 px HUD). Map area 960x720.
- `GameConfig`: Gson POJO for the JSON; `healthScale(wave)`, `speedScale(wave)`, `killReward`, `waveClearReward`.
- `UpgradeCostTable`, `CaseRNGManager` (two-step roll), `Catalog` (loads towers and cases once).
- `GameSession`: private coins, lives, free cases, inventory (List<TowerType>), placed towers, `levelFiveTypes` set. Methods: buyCase, openFreeCase, place, upgradeBlockReason, upgradeCost, upgrade, sell, sellInventory, refundFor, canPlaceAt.
- `WaveSpawner`: builds a timed spawn queue per wave; waves 1-3 from JSON; later: Basic 6+2n, Fast n+1 from wave 3, Armored n-3 from wave 5; boss on every 10th.
- `GameEngine`: states BETWEEN_WAVES / WAVE_ACTIVE / GAME_OVER. `update(dt)`: spawn → reset buffs → auras → towers update → enemies update (kills pay coins, leaks cost lives) → game over or wave clear (pays reward).
- `VisualEffect` (short-lived line/circle), `RunResult` (record: wavesCleared, kills, casesOpened, previousBest, newRecord).

**dao / config**: `DatabaseConnection` (Singleton, reads `data/db_config.properties`), `PlayerDAO` (findByName, findOrCreate, updateHighestScore with `WHERE highest_score < ?`), `TowerDAO.findAll`, `CaseDAO.findAll`.

**ui** (all visuals are **placeholders** until the UI/UX team's art is ready; marked in comments)
- `MainFrame` (CardLayout, 1280x720), `AppController` (landing → game → results; loads Catalog, config, map once).
- `screens/LandingPanel` (team's landing page: name field, crate opens profile via PlayerDAO, Play button; I added `getPlayer()`), `HelpPanel` (two-page help book overlay).
- `screens/GamePanel`: Swing Timer ~60 fps, fixed 1/60 s steps, speed x1/x2/x3, mouse routing (shop → side panel → map), placing, selecting, right-click popup to sell inventory, Esc cancels, Space starts wave, QUIT button, 2 s game-over banner then saves score.
- `screens/SidePanel` (x 960-1280: inventory grouped by type with counts, tower info with UPGRADE/SELL, SHOP / START WAVE / SPEED buttons; talks back through the `SidePanel.Actions` interface).
- `screens/ShopOverlay` (3 case cards with odds, free case on the Standard card, 5 s ease-out spinner strip, click to skip, result card, COLLECT).
- `screens/ResultsPanel` (waves cleared, new high score, kills, cases opened, PLAY AGAIN / MENU).
- `components/GameButton` (Builder pattern, from the landing page), `TextButton`, `TowerArt` (sprite if file exists, else shape), `Gfx` (text helpers); `theme/Theme`, `theme/Palette`.
- `util/FontLoader` (Luckiest Guy), `ImageLoader` (assets/ui), `SpriteLoader`, `DbCheck`.

OOP concepts to cite: abstraction (Tower, Enemy), inheritance (4 + 4 subclasses), polymorphism (attack, reduceDamage, stun, applyAura), interfaces (Upgradable, Damageable, SidePanel.Actions), encapsulation (GameSession), composition (GameEngine), Singleton, Factory, Builder, DAO, records, enums.

## 8. Testing done

- Compiled with javac + Gson (not through Maven, not against real MySQL).
- Headless bot simulation: median about wave 19 cleared, so it is playable and gets hard; some unlucky starts (e.g. Policeman or Radio Tower as the only starter) die early.
- Offscreen screenshots and simulated clicks: shop, free case, spinner, collect, placing, upgrade panel, starting a wave all work.
- Not tested: right-click sell popup, a real run on MySQL by a human.

## 9. Docs produced

- `docs/GAME_GUIDE.md` in the repo: short defense guide (layers, class map, OOP table, one game tick, rules, likely questions).
- `CasesTD_Game_Walkthrough.pdf` (39 pages, kept locally, not in the repo): 7 batches (foundation data, enemies, towers and factories, map and rules, waves and engine, database loading, screens) with real code snippets in VS Code-style colors, plus professor Q&A for each batch.
- README updated with How to play, the new structure, and the re-run-the-schema note.
- Team docs (report, ERD, presentation) were done for the landing demo; they still mention the old catalog (e.g. "SlowSupportTower").

## 10. To-do / next steps

1. Commit and push `feature/playable-game`, open a PR, have teammates pull and re-run schema + seed.
2. Play it on real MySQL and fix any bugs (first priority before Thursday Oct 29).
3. Balance stats and economy with the team (JSON + seed only, no code change needed).
4. Update the ERD, tower tables and report to the new 10-tower catalog and class names.
5. When UI/UX art is ready: drop tower sprites at the `sprite_path` locations (loaded automatically); restyle `GamePanel`, `SidePanel`, `ShopOverlay`, `ResultsPanel` without touching `core`.
6. Optional ideas: keep the game running while the shop is open (report allows mid-wave opening), enemy sprites, sound, more maps.

## 11. How to ask a new AI for help

Start with: "Here is my project context file" and paste this file. Then attach the specific Java file(s) you want to change, because the AI cannot see the repo unless you give it the files.
