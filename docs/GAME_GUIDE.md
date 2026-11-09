# CasesTD: How the game works (defense guide)

## 1. The big picture

The game is split into layers. Each layer only talks to the one below it.

| Layer | Package | Job |
|---|---|---|
| Screens | `btd.ui` | Draw things and pass clicks on. No game rules here. |
| Game logic | `btd.core` | The rules: waves, coins, upgrades, selling, case rolls. |
| Models | `btd.model` | What things are: towers, enemies, rarities, the player. |
| Data access | `btd.dao`, `btd.config` | The only code that runs SQL. |
| Data | MySQL, `waves_config.json`, `map1_path.csv` | Stats, costs and the path, so they can be tuned without code changes. |

Why this matters: the UI team can replace every screen and the game rules will not change.

## 2. Class map

- **AppController** switches between landing, game and results screens.
- **GamePanel** is the gameplay screen. A timer calls it about 60 times a second. It runs the engine and repaints.
- **GameEngine** runs the waves. It owns the enemy list and calls the towers.
- **GameSession** holds what the player owns: coins, lives, inventory, placed towers. All changes go through its methods (place, upgrade, sell, buy case), so the rules are enforced in one place.
- **WaveSpawner** turns a wave number into a timed list of enemies.
- **CaseRNGManager** rolls a case: pick a rarity from the weights, then a random tower of that rarity.
- **GameMap** loads the path, finds a position along it, and checks if a spot is buildable.
- **Catalog** holds all towers and cases loaded from the database once.
- **GameConfig** holds the numbers from `waves_config.json`.

## 3. Where each OOP concept lives

| Concept | Where |
|---|---|
| Abstraction | `Tower` and `Enemy` are abstract. You never create a plain Tower. |
| Inheritance | `SingleTargetTower`, `SplashTower`, `SniperTower`, `SupportTower` extend `Tower`. `BasicEnemy`, `FastEnemy`, `ArmoredEnemy`, `BossEnemy` extend `Enemy`. |
| Polymorphism | The engine calls `tower.update()`. The abstract `attack()` runs different code per subclass. `ArmoredEnemy` overrides `reduceDamage` (takes 30% less). `BossEnemy` overrides `stun` (half duration). After any stun ends, an enemy is immune to stuns for 1.5 s. `SupportTower` overrides `applyAura`. |
| Interfaces | `Upgradable` (implemented by `Tower`), `Damageable` (implemented by `Enemy`). |
| Encapsulation | Fields are private. `GameSession` is the only way to change coins, inventory and towers. |
| Composition | `GameEngine` has a `GameSession`, a `GameMap` and a `WaveSpawner`. |
| Singleton | `DatabaseConnection` (one shared connection). |
| Factory | `TowerFactory` and `EnemyFactory` build the right subclass from a name stored in data. |
| Builder | `GameButton.Builder` (from the landing page). |
| DAO | `PlayerDAO`, `TowerDAO`, `CaseDAO`. The UI never runs SQL. |
| Records and enums | `TowerType`, `CaseDef`, `RunResult` (records). `Rarity`, `Special` (enums). |

## 4. What happens in one game tick (1/60 second)

1. The timer calls `GamePanel.tick()`, which calls `engine.update(1/60)` (more times per frame on x2 or x3 speed).
2. `WaveSpawner` adds any enemies whose spawn time has come.
3. Every tower's buff is reset, then support towers (Radio Tower) apply their buff to nearby towers.
4. Every tower counts down its cooldown. When ready, its own `attack()` runs and may add a visual effect.
5. Every enemy updates: poison and burn damage, stun check, then moves along the path.
6. Dead enemies give their kill coins. Enemies that reach the end remove lives.
7. If lives reach 0 the state becomes GAME_OVER. If the wave is fully spawned and no enemies remain, the wave reward is paid and the state goes back to BETWEEN_WAVES.
8. `GamePanel` repaints everything.

## 5. The rules and where they are enforced

- **Upgrading** (`GameSession.upgrade`): costs spare copies of the same tower. The cost depends on rarity and target level (`UpgradeCostTable`, numbers in the JSON).
- **One Level 5 per type** (`GameSession.levelFiveTypes`): a set of tower types that already have a Level 5. Others stay capped at Level 4. Selling the Level 5 frees the slot.
- **Level bonus** (`Tower.levelMultiplier`): +10% damage and fire rate per level.
- **Level 5 passive** (`applyMaxPassive`, different in each subclass).
- **Selling**: refund depends on rarity (Common 30, Rare 50, Epic 75, Legendary 150).
- **Enemy scaling** (`GameConfig`): +35% of base health per wave, and an extra x1.25 each time a new block of 10 waves starts (wave 11, 21, 31...), so 10 to 11 is a much bigger step than 11 to 12. +2% speed per wave (capped at +50%). Every 10th wave adds a boss. The game is endless.
- **Case rolls**: the result is rolled the moment you pay. The spinner animation only reveals it.
- **Score**: when the run ends, waves cleared is saved with `PlayerDAO.updateHighestScore`, only if it beats the old record.

## 6. Likely questions

**Why abstract classes for Tower and Enemy?**
All towers share the same cooldown, level and range logic. Only the attack differs, so `attack()` is abstract and each subclass fills it in.

**Why a DAO?**
So SQL lives in one place. If a table changes, only the DAO changes, not the screens.

**Why is the data in the database and a JSON file?**
Tower and case stats are catalog data, so they are in MySQL. Economy and wave numbers are tuning values, so they are in JSON. Both can be changed without recompiling.

**Why a Factory?**
The database stores the tower class as text. The factory turns that text into the correct subclass, so the rest of the code never needs a big if/else.

**How is the case luck fair?**
`CaseRNGManager` uses the weights in the cases table. If a rarity has no towers in a case, it is skipped and the other weights are used.

**What happens if the database is down?**
The game shows an error message when you press Play, because the catalog cannot be loaded. The player profile and score also need the database.

**How would you add a new tower?**
Add a row in `seed_data.sql` (and the case pool). If it behaves like an existing class, no Java change is needed. For new behavior, add a subclass of `Tower` and one line in `TowerFactory`.

## 7. Known limits (be honest about these)

- All stats and the economy are placeholders for the team to balance.
- The visuals are placeholders until the UI/UX art is ready. Tower sprites load from `sprite_path` automatically if the files exist.
- The game is paused while the shop is open.
