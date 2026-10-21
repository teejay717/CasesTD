package btd.model.player;

/** A saved player: a unique name and the most waves they have cleared in one run. */
public class Player {

    private final int playerId;
    private final String playerName;
    private int highestScore;

    public Player(int playerId, String playerName, int highestScore) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.highestScore = highestScore;
    }

    public int getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getHighestScore() {
        return highestScore;
    }

    public void setHighestScore(int highestScore) {
        this.highestScore = highestScore;
    }

    @Override
    public String toString() {
        return playerName + " (highest score: " + highestScore + ")";
    }
}
