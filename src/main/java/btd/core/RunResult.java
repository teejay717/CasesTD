package btd.core;

/**
 * [PURPOSE]       Immutable data carrier summarizing run statistics for victory/defeat display and high score tracking.
 * [OOP / PATTERN] Java Record / Data Transfer Object (DTO) / Immutability.
 * [CALLED BY]     btd/ui/screens/GamePanel.java (finishRun()), btd/ui/AppController.java (showResults()), btd/ui/screens/ResultsPanel.java (show()).
 * [CONNECTS TO]   btd/ui/screens/ResultsPanel.java (displays it); previousBest comes from btd/model/player/Player.java.
 * [DEFENSE NOTE]  Encapsulates waves cleared, kills, cases opened, previous best, and new record flag in one immutable record.
 */
public record RunResult(int wavesCleared, int kills, int casesOpened, int previousBest, boolean newRecord) {
}
