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
