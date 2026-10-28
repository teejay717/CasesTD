USE btd_case_db;

-- Stats are placeholders for the team to tune.
-- effect_value / effect_duration_ms by special:
--   STUN    effect_duration_ms = stun time in ms
--   POISON  effect_value = damage per second, effect_duration_ms = how long it lasts
--   BURN    effect_value = damage per second per stack (stacks up to 5), effect_duration_ms = how long it lingers
--   BUFF    effect_value = % fire rate boost given to other towers in range
--   TURRETS effect_value = number of mini-turrets (each shoots a different enemy)
--   PIERCE  effect_value = how many enemies the shot passes through
INSERT INTO towers
(tower_name, tower_class, rarity, base_damage, attack_range, fire_rate, splash_radius,
 special, effect_value, effect_duration_ms, description, sprite_path) VALUES
('Soldier',     'SingleTargetTower', 'COMMON',     10, 130, 1.5,   0, 'NONE',    0,    0, 'Basic single-target tower.',                         'assets/sprites/towers/soldier.png'),
('Grenadier',   'SplashTower',       'COMMON',      9, 115, 0.8,  45, 'NONE',    0,    0, 'Deals explosive area damage.',                        'assets/sprites/towers/grenadier.png'),
('Policeman',   'SupportTower',      'COMMON',      2, 115, 0.4,   0, 'STUN',    0,  900, 'Stuns every enemy in range for a short time.',          'assets/sprites/towers/policeman.png'),
('Scientist',   'SplashTower',       'RARE',        5, 135, 0.8,  60, 'POISON',  7, 3000, 'Throws toxic chemicals that burn enemies over time.',   'assets/sprites/towers/scientist.png'),
('Sniper',      'SniperTower',       'RARE',       55, 380, 0.5,   0, 'NONE',    0,    0, 'High range, hits the toughest enemy.',                 'assets/sprites/towers/sniper.png'),
('Radio Tower', 'SupportTower',      'RARE',        0, 150, 1.0,   0, 'BUFF',   30,    0, 'Boosts the fire rate of nearby towers.',               'assets/sprites/towers/radio_tower.png'),
('Flamethrower','SplashTower',       'EPIC',        3,  95, 6.0,  35, 'BURN',    4, 2000, 'Fast flames stack burn damage that lingers.',           'assets/sprites/towers/flamethrower.png'),
('Engineer',    'SupportTower',      'EPIC',        7, 150, 1.4,   0, 'TURRETS', 3,    0, 'Builds mini-turrets that shoot different enemies.',     'assets/sprites/towers/engineer.png'),
('Railgunner',  'SniperTower',       'LEGENDARY', 140, 600, 0.4,   0, 'PIERCE',  5,    0, 'Slow, huge shots that pierce through many enemies.',   'assets/sprites/towers/railgunner.png'),
('Mortar',      'SplashTower',       'LEGENDARY', 120, 450, 0.25, 90, 'NONE',    0,    0, 'Long-range, slow heavy explosive shells.',             'assets/sprites/towers/mortar.png');

INSERT INTO cases
(case_name, cost, common_weight, rare_weight, epic_weight, legendary_weight, sprite_path) VALUES
('Standard Case', 100, 65, 25,  8, 2, 'assets/sprites/cases/standard.png'),
('Premium Case',  250,  0, 60, 32, 8, 'assets/sprites/cases/premium.png'),
-- Legendary Case: placeholder stats, to be tuned by the game logic team
('Legendary Case', 500,  0,  0, 70, 30, 'assets/sprites/cases/legendary.png');

-- Standard Case: all towers
INSERT INTO case_towers (case_id, tower_id)
SELECT c.case_id, t.tower_id FROM cases c JOIN towers t
WHERE c.case_name = 'Standard Case';

-- Premium Case: exclude Commons
INSERT INTO case_towers (case_id, tower_id)
SELECT c.case_id, t.tower_id FROM cases c JOIN towers t
WHERE c.case_name = 'Premium Case' AND t.rarity <> 'COMMON';

-- Legendary Case: Epic and Legendary towers only (placeholder pool)
INSERT INTO case_towers (case_id, tower_id)
SELECT c.case_id, t.tower_id FROM cases c JOIN towers t
WHERE c.case_name = 'Legendary Case' AND t.rarity IN ('EPIC', 'LEGENDARY');

-- Sample players for the demo (delete before final submission)
INSERT IGNORE INTO players (player_name, highest_score) VALUES
('Sam', 14),
('Alex', 9);


