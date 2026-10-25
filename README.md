# CasesTD

OOP final project: a Bloons-style tower defense game with a case-opening layer. Players earn Coins from waves, open cases to unbox towers into an inventory, then place, upgrade, or sell them.

Built with Java (Swing), Maven, and MySQL.

## Requirements

- JDK 21 or newer
- Maven (or the Maven support built into IntelliJ, Eclipse, or NetBeans)
- MySQL 8.0.16 or newer (older versions silently ignore the `CHECK` constraint in `schema.sql`), or XAMPP with phpMyAdmin

## Setup

1. **Clone the repo** and open it as a Maven project in your IDE. Open the folder that contains `pom.xml`, because the app reads `data/db_config.properties` from the project root.
2. **Create the database.** Run `database/schema.sql` in MySQL Workbench (or phpMyAdmin), then run `database/seed_data.sql`. The schema creates `btd_case_db` with 4 tables: `towers`, `cases`, `case_towers`, `players`.
3. **Create a MySQL login for the project (optional).** You can use your own root login instead. To create a separate one that can only use this database, run this in MySQL Workbench:

   ```sql
   CREATE USER 'casestd'@'localhost' IDENTIFIED BY 'casestd';
   GRANT ALL PRIVILEGES ON btd_case_db.* TO 'casestd'@'localhost';
   ```

4. **Create your local config.** Copy `data/db_config.example.properties` to `data/db_config.properties` and fill it in. If you made the login above, it looks like this:

   ```
   db.url=jdbc:mysql://localhost:3306/btd_case_db
   db.user=casestd
   db.password=casestd
   ```

   This file is git-ignored, so never commit it. Use your own port in `db.url` if your MySQL is not on 3306 (run `SELECT @@port;` in Workbench to check).
5. **Check the data.** `SELECT * FROM towers;` should return 10 rows, `cases` 3 rows, and `case_towers` 21 rows (10 Standard + 7 Premium + 4 Legendary).
6. **Test the connection.** Run `btd.util.DbCheck` from the project root. It should print `Sam (highest score: 14)` for "Sam" and "sam", and "not found" for the third name. If it fails with "Access denied", the user or password in `db_config.properties` is wrong. If it fails with "No suitable driver", reload the Maven project so the MySQL driver downloads.
7. **Run** `btd.Main`.

### Running in VS Code

Open the folder containing `pom.xml` with the Extension Pack for Java installed, then let the Java project finish loading. Open `DbCheck.java` and click **Run** above `main`, or right-click in the editor and select **Run Java**. Use the same steps for `Main.java`.

Code Runner's **Run Code** command compiles only the current file. That causes `package btd.dao does not exist` when running `DbCheck.java`, because it leaves out the project source path and Maven dependencies. Use the Java extension's runner for this project.

## How to play

1. Type a name on the landing page, click the crate, then click Play.
2. You start with 75 Coins and 1 free Standard Case. Open the **SHOP**, open your free case, and press **COLLECT**.
3. Click a tower card in the inventory, then click the map to place it (it starts at Level 1). Right-click a card to sell it.
4. Press **START WAVE** (or Space). Zombies walk the path; if they reach the end you lose lives. Clearing a wave gives Coins.
5. Spend Coins on more cases at any time. Extra copies of a tower are used to **UPGRADE** it (click a placed tower). Only one tower per type can reach Level 5.
6. The run ends when lives hit zero. Your waves cleared is saved as your highest score if it beats your record.

Game numbers (starting Coins, rewards, upgrade costs, enemy scaling, the first waves) are in `data/maps/waves_config.json`. Tower and case stats are in the database (`database/seed_data.sql`). All of these are placeholders the team can tune. The game visuals are placeholders too until the UI/UX art is ready: tower sprites load automatically from the `sprite_path` in the `towers` table if the file exists.

If you pull a version that changes the catalog, re-run `database/schema.sql` and then `database/seed_data.sql`. The `players` table is kept, so saved scores are not lost.

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
    ├── config/          DatabaseConnection (Singleton)
    ├── dao/             TowerDAO, CaseDAO, PlayerDAO
    ├── factory/         TowerFactory, EnemyFactory
    ├── model/           tower/, enemy/, gacha/, player/
    ├── core/            GameEngine, GameSession, GameMap, GameConfig, WaveSpawner,
    │                    CaseRNGManager, UpgradeCostTable, Catalog, VisualEffect, RunResult
    ├── ui/              AppController, screens/, components/, theme/
    └── util/            FontLoader, ImageLoader, SpriteLoader, DbCheck
```

## Team rules

- Never commit `data/db_config.properties`.
- Share database changes by editing `database/schema.sql` and `database/seed_data.sql`, not by passing around raw SQL dumps.
- The UI layer never runs SQL. All database access goes through the DAO classes.
- Economy and stat numbers in `data/maps/waves_config.json` and `database/seed_data.sql` are placeholders until the team balances them.
