# CasesTD

OOP final project: a Bloons-style tower defense game with a case-opening layer. Players earn Coins from waves, open cases to unbox towers into an inventory, then place, upgrade, or sell them.

Built with Java (Swing), Maven, and MySQL.

## Requirements

- JDK 21 or newer
- Maven (or the Maven support built into IntelliJ, Eclipse, or NetBeans)
- MySQL 8.0.16 or newer (older versions silently ignore the `CHECK` constraint in `schema.sql`), or XAMPP with phpMyAdmin

## Setup

1. **Clone the repo** and open it as a Maven project in your IDE.
2. **Create your local config.** Copy `data/db_config.example.properties` to `data/db_config.properties` and enter your own MySQL URL, user, and password. This file is git-ignored, so never commit it.
3. **Create the database.** Run `database/schema.sql` in MySQL Workbench (or phpMyAdmin), then run `database/seed_data.sql`. The schema creates `btd_case_db` with 4 tables: `towers`, `cases`, `case_towers`, `players`.
4. **Check the data.** `SELECT * FROM towers;` should return 10 rows, `cases` 3 rows, and `case_towers` 21 rows (10 Standard + 7 Premium + 4 Legendary).
5. **Run** `btd.Main`.

## Project structure

```
CasesTD/
├── pom.xml
├── database/            schema.sql, seed_data.sql, backups/
├── data/                db_config.example.properties, maps/ (map1_path.csv, waves_config.json)
├── assets/              fonts/, sprites/{towers,enemies,cases}, ui/, audio/
├── docs/                ERD, UML diagrams, wireframes, theme notes
└── src/main/java/btd/
    ├── Main.java
    ├── config/          DatabaseConnection (Singleton), GameConfig
    ├── dao/             TowerDAO, CaseDAO, PlayerDAO
    ├── factory/         TowerFactory
    ├── model/           tower/, enemy/, gacha/, player/
    ├── core/            GameLoop, WaveSpawner, GameSession, CaseRNGManager, UpgradeCostTable
    ├── ui/              screens/, components/, theme/
    └── util/            AssetLoader, CsvReader
```

## Team rules

- Never commit `data/db_config.properties`.
- Share database changes by editing `database/schema.sql` and `database/seed_data.sql`, not by passing around raw SQL dumps.
- The UI layer never runs SQL. All database access goes through the DAO classes.
- Economy numbers marked `null` in `data/maps/waves_config.json` are placeholders for the game logic team to settle.
