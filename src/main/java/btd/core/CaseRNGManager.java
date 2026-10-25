package btd.core;

import btd.model.gacha.CaseDef;
import btd.model.gacha.Rarity;
import btd.model.tower.TowerType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Rolls a case: first picks a rarity using the case weights, then a random tower of that rarity. */
public class CaseRNGManager {

    private final Random random;

    public CaseRNGManager() {
        this(new Random());
    }

    public CaseRNGManager(Random random) {
        this.random = random;
    }

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

    /** Only rarities that actually have a tower in the pool can be rolled; the others are skipped. */
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

    private boolean hasTower(CaseDef def, Rarity rarity) {
        for (TowerType t : def.pool()) {
            if (t.rarity() == rarity) {
                return true;
            }
        }
        return false;
    }
}
