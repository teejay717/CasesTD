package btd.model.tower;

/** Something that has levels, like a tower. */
public interface Upgradable {

    int getLevel();

    /** Sets the level and applies its stat bonus. */
    void applyLevel(int newLevel);
}
