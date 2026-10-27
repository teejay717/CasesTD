package btd.factory;

import btd.model.tower.SingleTargetTower;
import btd.model.tower.SniperTower;
import btd.model.tower.SplashTower;
import btd.model.tower.SupportTower;
import btd.model.tower.Tower;
import btd.model.tower.TowerType;

/** Builds the right Tower subclass from the tower_class value stored in the database. */
public final class TowerFactory {

    private TowerFactory() {
    }

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
