USE btd_case_db;

INSERT IGNORE INTO towers
(tower_name, tower_class, rarity, base_damage, attack_range, fire_rate,
 splash_radius, slow_percent, slow_duration_ms, sprite_path) VALUES
('Dart Slinger',  'SingleTargetTower', 'COMMON',     10, 120, 1.5,   0,  0,    0, 'assets/sprites/towers/dart_slinger.png'),
('Frost Pebble',  'SlowSupportTower',  'COMMON',      2, 110, 1.0,   0, 20, 1500, 'assets/sprites/towers/frost_pebble.png'),
('Bomb Bucket',   'SplashTower',       'COMMON',      8, 100, 0.8,  40,  0,    0, 'assets/sprites/towers/bomb_bucket.png'),
('Twin Blaster',  'SingleTargetTower', 'RARE',       12, 130, 3.0,   0,  0,    0, 'assets/sprites/towers/twin_blaster.png'),
('Blast Barrel',  'SplashTower',       'RARE',       20, 120, 0.9,  60,  0,    0, 'assets/sprites/towers/blast_barrel.png'),
('Ice Lantern',   'SlowSupportTower',  'RARE',        4, 140, 1.2,   0, 35, 2000, 'assets/sprites/towers/ice_lantern.png'),
('Rail Scout',    'SniperTower',       'EPIC',       70, 400, 0.5,   0,  0,    0, 'assets/sprites/towers/rail_scout.png'),
('Cryo Cannon',   'SlowSupportTower',  'EPIC',       15, 160, 1.0,   0, 50, 2500, 'assets/sprites/towers/cryo_cannon.png'),
('Inferno Mortar','SplashTower',       'LEGENDARY',  60, 200, 0.7, 110,  0,    0, 'assets/sprites/towers/inferno_mortar.png'),
('Void Marksman', 'SniperTower',       'LEGENDARY', 220, 600, 0.4,   0,  0,    0, 'assets/sprites/towers/void_marksman.png');

INSERT IGNORE INTO cases
(case_name, cost, common_weight, rare_weight, epic_weight, legendary_weight, sprite_path) VALUES
('Standard Case', 100, 65, 25,  8, 2, 'assets/sprites/cases/standard.png'),
('Premium Case',  250,  0, 60, 32, 8, 'assets/sprites/cases/premium.png'),
-- Legendary Case: placeholder stats, to be tuned by the game logic team
('Legendary Case', 500,  0,  0, 70, 30, 'assets/sprites/cases/legendary.png');

-- Standard Case: all towers
INSERT IGNORE INTO case_towers (case_id, tower_id)
SELECT c.case_id, t.tower_id FROM cases c JOIN towers t
WHERE c.case_name = 'Standard Case';

-- Premium Case: exclude Commons
INSERT IGNORE INTO case_towers (case_id, tower_id)
SELECT c.case_id, t.tower_id FROM cases c JOIN towers t
WHERE c.case_name = 'Premium Case' AND t.rarity <> 'COMMON';

-- Legendary Case: Epic and Legendary towers only (placeholder pool)
INSERT IGNORE INTO case_towers (case_id, tower_id)
SELECT c.case_id, t.tower_id FROM cases c JOIN towers t
WHERE c.case_name = 'Legendary Case' AND t.rarity IN ('EPIC', 'LEGENDARY');

-- Sample players for the demo (delete before final submission)
INSERT IGNORE INTO players (player_name, highest_score) VALUES
('Sam', 14),
('Alex', 9);
