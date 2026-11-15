package btd.model.tower;

/**
 * [PURPOSE]       Defines upgrade contract for any tower or entity that gains levels and stat bonuses.
 * [OOP / PATTERN] Interface Abstraction / Open-Closed Principle.
 * [CALLED BY]     btd/core/GameSession.java (upgrade()).
 * [CONNECTS TO]   btd/model/tower/Tower.java (implements this contract).
 * [DEFENSE NOTE]  Standardizes how upgrade levels are applied across all tower archetypes uniformly.
 */
public interface Upgradable {

    /**
     * [PURPOSE]       Retrieves current upgrade tier level (1 to 5).
     * [OOP / PATTERN] State Query / Getter Abstraction.
     * [CALLED BY]     btd/core/GameSession.java, btd/ui/screens/SidePanel.java, btd/ui/screens/GamePanel.java.
     * [CONNECTS TO]   Tower internal state.
     * [DEFENSE NOTE]  Level 5 represents the maximum tier where the unique passive unlocks.
     */
    int getLevel();

    /**
     * [PURPOSE]       Applies new upgrade tier and updates associated scaling multipliers.
     * [OOP / PATTERN] State Mutation / Lifecycle Hook.
     * [CALLED BY]     btd/core/GameSession.java (upgrade()).
     * [CONNECTS TO]   btd/model/tower/Tower.java (applyLevel()).
     * [DEFENSE NOTE]  Triggers applyMaxPassive() when reaching Level 5.
     */
    void applyLevel(int newLevel);
}
