package btd.core;

import btd.model.gacha.CaseDef;
import btd.model.gacha.Rarity;
import btd.model.tower.TowerType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * [PURPOSE]       Decides which tower a case gives, in two steps:
 *                 1) pick a rarity using the case's weights (e.g. Standard Case: 65 / 25 / 8 / 2),
 *                 2) pick one tower of that rarity from the case's pool, all equally likely.
 * [OOP / PATTERN] Weighted Random ("roulette wheel") Selection / Dependency Injection (the Random is passed in).
 * [CALLED BY]     btd/core/GameSession.java (open(), which buyCase() and openFreeCase() both use).
 * [CONNECTS TO]   btd/model/gacha/CaseDef.java, btd/model/tower/TowerType.java.
 * [DEFENSE NOTE]  Skips any rarity that has no tower in the pool, so a roll can never land on an empty rarity.
 */
public class CaseRNGManager {

    private final Random random;

    /**
     * [PURPOSE]       Default constructor: uses a normal new Random().
     * [OOP / PATTERN] Constructor Overloading / Constructor Chaining (this(...)).
     * [CALLED BY]     btd/ui/screens/GamePanel.java (startRun(): new GameSession(config, new CaseRNGManager())).
     */
    public CaseRNGManager() {
        this(new Random());
    }

    /**
     * [PURPOSE]       Constructor that takes the Random to use.
     * [OOP / PATTERN] Dependency Injection.
     * [CALLED BY]     The default constructor above. (The project has no unit tests yet, but passing
     *                 new Random(42) here would make every roll repeat the same way, which is useful for testing.)
     */
    public CaseRNGManager(Random random) {
        this.random = random;
    }

    /**
     * [PURPOSE]       Executes two-phase roll: first picks rarity tier, then picks random tower from pool within that tier.
     * [OOP / PATTERN] Two-Phase Discrete Sampling Algorithm.
     * [CALLED BY]     btd/core/GameSession.java (open()).
     * [CONNECTS TO]   rollRarity(), btd/model/gacha/CaseDef.java (pool()).
     * [DEFENSE NOTE]  Guarantees selected tower strictly belongs to chosen rarity and is present in case pool.
     */
    public TowerType roll(CaseDef def) {
        Rarity rarity = rollRarity(def);
        List<TowerType> sameRarity = new ArrayList<>();
        for (TowerType t : def.pool()) {
            if (t.rarity() == rarity) {
                sameRarity.add(t);
            }
        }
        return sameRarity.get(random.nextInt(sameRarity.size()));
    }

    /**
     * [PURPOSE]       Picks a rarity using the case's weights, skipping rarities with no tower in the pool.
     * [OOP / PATTERN] Roulette Wheel Selection.
     * [CALLED BY]     btd/core/CaseRNGManager.java (roll()).
     * [CONNECTS TO]   btd/model/gacha/CaseDef.java (weightOf()).
     * [DEFENSE NOTE]  How it works (Standard Case 65/25/8/2, total = 100):
     *                 pick = random 0..100, e.g. 72. Subtract 65 (Common) -> 7, still >= 0.
     *                 Subtract 25 (Rare) -> -18, below 0 -> result is RARE.
     *                 Because 'total' only adds up rarities that exist in the pool, the odds stay correct.
     */
    public Rarity rollRarity(CaseDef def) {
        double total = 0;
        for (Rarity r : Rarity.values()) {
            if (hasTower(def, r)) {
                total += def.weightOf(r);
            }
        }
        double pick = random.nextDouble() * total;
        Rarity last = Rarity.COMMON;
        for (Rarity r : Rarity.values()) {
            if (!hasTower(def, r) || def.weightOf(r) <= 0) {
                continue;
            }
            last = r;
            pick -= def.weightOf(r);
            if (pick < 0) {
                return r;
            }
        }
        return last;
    }

    /** [PURPOSE] Helper check verifying whether case pool contains at least one tower of given rarity. */
    private boolean hasTower(CaseDef def, Rarity rarity) {
        for (TowerType t : def.pool()) {
            if (t.rarity() == rarity) {
                return true;
            }
        }
        return false;
    }
}
