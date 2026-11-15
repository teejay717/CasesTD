package btd.model.tower;

/**
 * [PURPOSE]       Contract for anything that has a level and can be upgraded.
 * [OOP / PATTERN] Interface / Abstraction.
 * [CALLED BY]     Implemented by btd/model/tower/Tower.java, so all 4 tower subclasses get it.
 *                 Its methods are used by btd/core/GameSession.java (upgrade(), upgradeBlockReason()).
 * [CONNECTS TO]   btd/model/tower/Tower.java (implements this contract).
 * [DEFENSE NOTE]  Every tower is guaranteed to have getLevel() and applyLevel(), whatever its type.
 */
public interface Upgradable {

    /**
     * [PURPOSE]       Retrieves current upgrade tier level (1 to 5).
     * [OOP / PATTERN] State Query / Getter Abstraction.
     * [CALLED BY]     btd/core/GameSession.java, btd/ui/screens/SidePanel.java, btd/ui/screens/GamePanel.java (level pips).
     * [CONNECTS TO]   Tower internal state.
     * [DEFENSE NOTE]  Level 5 represents the maximum tier where the unique passive unlocks.
     */
    int getLevel();

    /**
     * [PURPOSE]       Applies new upgrade tier and updates associated scaling multipliers.
     * [OOP / PATTERN] State Mutation / Lifecycle Hook.
     * [CALLED BY]     btd/core/GameSession.java (upgrade()).
     * [CONNECTS TO]   btd/model/tower/Tower.java (applyLevel()).
     * [DEFENSE NOTE]  Tower's version calls applyMaxPassive() when the level reaches 5.
     */
    void applyLevel(int newLevel);
}
