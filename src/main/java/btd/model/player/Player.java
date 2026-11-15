package btd.model.player;

/**
 * [PURPOSE]       Encapsulates player account profile data: unique ID, handle name, and personal best high score.
 * [OOP / PATTERN] Domain Entity / Data Encapsulation / POJO Pattern.
 * [CALLED BY]     btd/dao/PlayerDAO.java, btd/ui/AppController.java,
 *                 btd/ui/screens/GamePanel.java, btd/ui/screens/LandingPanel.java.
 * [CONNECTS TO]   btd/dao/PlayerDAO.java, btd/ui/screens/ResultsPanel.java.
 * [DEFENSE NOTE]  Encapsulates score mutation and database representation in one domain object.
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
     * [DEFENSE NOTE]  Instantiated directly from database query results upon login or player selection.
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

    /** [PURPOSE] Unique gamer tag username. */
    public String getPlayerName() {
        return playerName;
    }

    /** [PURPOSE] Personal record of maximum waves survived in a single run. */
    public int getHighestScore() {
        return highestScore;
    }

    /**
     * [PURPOSE]       Updates personal best wave record when a new high score is achieved.
     * [OOP / PATTERN] Encapsulated Setter.
     * [CALLED BY]     btd/ui/screens/GamePanel.java (finishRun()).
     * [CONNECTS TO]   btd/dao/PlayerDAO.java (updateHighestScore()).
     */
    public void setHighestScore(int highestScore) {
        this.highestScore = highestScore;
    }

    /**
     * [PURPOSE]       Provides readable representation for landing panel player pickers and logs.
     * [OOP / PATTERN] Method Overriding (Object.toString()).
     * [CALLED BY]     btd/util/DbCheck.java (console output).
     */
    @Override
    public String toString() {
        return playerName + " (highest score: " + highestScore + ")";
    }
}
