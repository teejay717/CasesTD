CREATE DATABASE IF NOT EXISTS btd_case_db;
USE btd_case_db;

-- The catalog tables are rebuilt every time this script runs (safe to re-run).
-- The players table is kept, so saved highest scores are not lost.
DROP TABLE IF EXISTS case_towers;
DROP TABLE IF EXISTS towers;
DROP TABLE IF EXISTS cases;

-- 1. Master tower catalog (Read-Only)
CREATE TABLE towers (
    tower_id           INT AUTO_INCREMENT PRIMARY KEY,
    tower_name         VARCHAR(50) NOT NULL UNIQUE,
    tower_class        ENUM('SingleTargetTower','SplashTower','SniperTower','SupportTower') NOT NULL,
    rarity             ENUM('COMMON','RARE','EPIC','LEGENDARY') NOT NULL,
    base_damage        INT NOT NULL,
    attack_range       INT NOT NULL,              -- in pixels
    fire_rate          DOUBLE NOT NULL,           -- shots per second
    splash_radius      INT NOT NULL DEFAULT 0,    -- SplashTower only
    special            ENUM('NONE','STUN','POISON','BURN','BUFF','TURRETS','PIERCE') NOT NULL DEFAULT 'NONE',
    effect_value       DOUBLE NOT NULL DEFAULT 0, -- meaning depends on special (see description)
    effect_duration_ms INT NOT NULL DEFAULT 0,    -- how long STUN / POISON / BURN last
    description        VARCHAR(120),
    sprite_path        VARCHAR(255) NOT NULL
);

-- 2. Cases sold in the shop (Read-Only)
CREATE TABLE cases (
    case_id          INT AUTO_INCREMENT PRIMARY KEY,
    case_name        VARCHAR(50) NOT NULL UNIQUE,
    cost             INT NOT NULL,
    common_weight    DOUBLE NOT NULL,
    rare_weight      DOUBLE NOT NULL,
    epic_weight      DOUBLE NOT NULL,
    legendary_weight DOUBLE NOT NULL,
    sprite_path      VARCHAR(255),
    CONSTRAINT chk_weights_sum CHECK (
        ABS(common_weight + rare_weight + epic_weight + legendary_weight - 100) < 0.001)
);

-- 3. Case tower pools (Read-Only join table)
CREATE TABLE case_towers (
    case_id  INT NOT NULL,
    tower_id INT NOT NULL,
    PRIMARY KEY (case_id, tower_id),
    FOREIGN KEY (case_id)  REFERENCES cases(case_id)   ON DELETE CASCADE,
    FOREIGN KEY (tower_id) REFERENCES towers(tower_id) ON DELETE CASCADE
);

-- 4. Player profiles (Written on first Play and when a run beats the highest score)
CREATE TABLE IF NOT EXISTS players (
    player_id     INT AUTO_INCREMENT PRIMARY KEY,
    player_name   VARCHAR(50) NOT NULL UNIQUE,
    highest_score INT NOT NULL DEFAULT 0
);
