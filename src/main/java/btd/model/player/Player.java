package btd.model.player;

/**
 * [PURPOSE]       One row of the 'players' table: id, name, and highest score (most waves cleared in one run).
 * [OOP / PATTERN] Entity / Encapsulation (private fields + getters; only highestScore has a setter).
 * [CALLED BY]     Created in btd/dao/PlayerDAO.java (findByName()). Kept by btd/ui/screens/LandingPanel.java,
 *                 passed by btd/ui/AppController.java to btd/ui/screens/GamePanel.java (startRun(), finishRun()).
 * [CONNECTS TO]   btd/dao/PlayerDAO.java.
 * [DEFENSE NOTE]  playerId and playerName are final, so they can't change. highestScore changes when a record is beaten.
 */
public class Player {

    private final int playerId;
    private final String playerName;
    private int highestScore;

    /**
     * [PURPOSE]       Constructs a Player entity.
     * [OOP / PATTERN] Entity Constructor / Encapsulation.
     * [CALLED BY]     btd/dao/PlayerDAO.java (findByName()).
     * [CONNECTS TO]   Internal state fields.
     * [DEFENSE NOTE]  Built from one ResultSet row when the player types their name and opens the crate.
     */
    public Player(int playerId, String playerName, int highestScore) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.highestScore = highestScore;
    }

    /** [PURPOSE] Primary key ID from database 'players' table. */
    public int getPlayerId() {
        return playerId;
    }

    /** [PURPOSE] The player's name (UNIQUE in the DB, so no two players share it). */
    public String getPlayerName() {
        return playerName;
    }

    /** [PURPOSE] Personal record of maximum waves survived in a single run. */
    public int getHighestScore() {
        return highestScore;
    }

    /**
     * [PURPOSE]       Updates the in-memory best score after a new record.
     * [OOP / PATTERN] Setter.
     * [CALLED BY]     btd/ui/screens/GamePanel.java (finishRun()), right after PlayerDAO.updateHighestScore() succeeds.
     * [CONNECTS TO]   Keeps this object in sync with the database row, so the menu shows the new score without reloading.
     */
    public void setHighestScore(int highestScore) {
        this.highestScore = highestScore;
    }

    /**
     * [PURPOSE]       Text version of the player, e.g. "Sam (highest score: 14)".
     * [OOP / PATTERN] Method Overriding (Object.toString()).
     * [CALLED BY]     btd/util/DbCheck.java (prints players to the console). Not used by the game screens.
     */
    @Override
    public String toString() {
        return playerName + " (highest score: " + highestScore + ")";
    }
}
