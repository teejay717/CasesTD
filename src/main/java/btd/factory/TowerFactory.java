package btd.factory;

import btd.model.tower.SingleTargetTower;
import btd.model.tower.SniperTower;
import btd.model.tower.SplashTower;
import btd.model.tower.SupportTower;
import btd.model.tower.Tower;
import btd.model.tower.TowerType;

/**
 * [PURPOSE]       Instantiates concrete Tower subclasses from the database tower_class string.
 * [OOP / PATTERN] Factory Method Pattern / Open-Closed Principle.
 * [CALLED BY]     btd/core/GameSession.java (place() method).
 * [CONNECTS TO]   btd/model/tower/Tower.java, btd/model/tower/SingleTargetTower.java,
 *                 btd/model/tower/SplashTower.java, btd/model/tower/SniperTower.java,
 *                 btd/model/tower/SupportTower.java.
 * [DEFENSE NOTE]  Decouples tower creation from placement logic; adding a new tower class requires
 *                 modifying only this factory rather than changing code across all UI screens.
 */
public final class TowerFactory {

    private TowerFactory() {
    }

    /**
     * [PURPOSE]       Matches tower_class string to construct the corresponding concrete Tower subclass.
     * [OOP / PATTERN] Pattern Matching Switch / Polymorphic Subclass Construction.
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
