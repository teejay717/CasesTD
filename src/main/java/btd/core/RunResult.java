package btd.core;

/**
 * [PURPOSE]       Immutable data carrier summarizing run statistics for victory/defeat display and high score tracking.
 * [OOP / PATTERN] Java Record / Data Transfer Object (DTO) / Immutability.
 * [CALLED BY]     btd/ui/AppController.java (onGameOver()), btd/ui/screens/ResultsPanel.java (showResult()).
 * [CONNECTS TO]   btd/model/player/Player.java, btd/dao/PlayerDAO.java.
 * [DEFENSE NOTE]  Encapsulates waves cleared, kills, cases opened, previous best, and new record flag in one immutable record.
 */
public record RunResult(int wavesCleared, int kills, int casesOpened, int previousBest, boolean newRecord) {
}
