package btd.core;

import btd.model.gacha.CaseDef;
import btd.model.gacha.Rarity;
import btd.model.tower.TowerType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * [PURPOSE]       Manages two-phase gacha RNG rolling: determines rarity tier via weighted distribution,
 *                 then selects uniformly among towers belonging to that tier in the case pool.
 * [OOP / PATTERN] Weighted Roulette Selection / Cumulative Distribution Function (CDF) Sampling / Dependency Injection (Random).
 * [CALLED BY]     btd/core/GameSession.java (open(), used by buyCase() and openFreeCase()).
 * [CONNECTS TO]   btd/model/gacha/CaseDef.java, btd/model/tower/TowerType.java.
 * [DEFENSE NOTE]  Prevents dead drops: dynamically filters out rarities that have zero towers in the case pool.
 */
public class CaseRNGManager {

    private final Random random;

    /**
     * [PURPOSE]       Default constructor initializing internal standard pseudo-random number generator.
     * [OOP / PATTERN] Constructor Overloading.
     * [CALLED BY]     btd/ui/screens/GamePanel.java (startRun()).
     */
    public CaseRNGManager() {
        this(new Random());
    }

    /**
     * [PURPOSE]       Parameterized constructor allowing seeded Random injection for unit testing.
     * [OOP / PATTERN] Dependency Injection.
     * [CALLED BY]     The default constructor (this(new Random())). No unit tests exist yet, but a seeded Random could be passed in for them.
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
     * [PURPOSE]       Picks rarity tier using cumulative weight distribution, skipping rarities absent from pool.
     * [OOP / PATTERN] Cumulative Distribution Function (CDF) Roulette Wheel Selection.
     * [CALLED BY]     btd/core/CaseRNGManager.java (roll()).
     * [CONNECTS TO]   btd/model/gacha/CaseDef.java (weightOf()).
     * [DEFENSE NOTE]  Normalizes weights on-the-fly across populated rarities so total probabilities always equal 1.0.
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
