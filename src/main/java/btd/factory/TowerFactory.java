package btd.factory;

import btd.model.tower.SingleTargetTower;
import btd.model.tower.SniperTower;
import btd.model.tower.SplashTower;
import btd.model.tower.SupportTower;
import btd.model.tower.Tower;
import btd.model.tower.TowerType;

/**
 * [PURPOSE]       Turns a TowerType (blueprint) into the right Tower subclass object, using the tower_class column.
 * [OOP / PATTERN] Factory Pattern (Simple / Static Factory) / Utility Class (final + private constructor).
 * [CALLED BY]     btd/core/GameSession.java (place()).
 * [CONNECTS TO]   btd/model/tower/Tower.java, btd/model/tower/SingleTargetTower.java,
 *                 btd/model/tower/SplashTower.java, btd/model/tower/SniperTower.java,
 *                 btd/model/tower/SupportTower.java.
 * [DEFENSE NOTE]  The rest of the code only says "give me a Tower" and never writes "new SniperTower(...)".
 *                 A brand-new tower class = write the subclass + add one case here. Nothing else changes.
 */
public final class TowerFactory {

    private TowerFactory() {
    }

    /**
     * [PURPOSE]       Matches tower_class string to construct the corresponding concrete Tower subclass.
     * [OOP / PATTERN] Switch expression on a String / returns the parent type Tower (polymorphism).
     * [CALLED BY]     btd/core/GameSession.java (place()).
     * [CONNECTS TO]   TowerType record, Concrete Tower subclasses.
     * [DEFENSE NOTE]  Throws IllegalArgumentException if an unrecognized tower class string is passed.
     */
    public static Tower create(TowerType type, double x, double y) {
        return switch (type.towerClass()) {
            case "SingleTargetTower" -> new SingleTargetTower(type, x, y);
            case "SplashTower" -> new SplashTower(type, x, y);
            case "SniperTower" -> new SniperTower(type, x, y);
            case "SupportTower" -> new SupportTower(type, x, y);
            default -> throw new IllegalArgumentException("Unknown tower class: " + type.towerClass());
        };
    }
}
