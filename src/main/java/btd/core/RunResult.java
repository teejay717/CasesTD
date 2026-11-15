package btd.core;

/**
 * [PURPOSE]       Immutable summary of one finished run, shown on the results screen.
 * [OOP / PATTERN] Java Record / Data Transfer Object (DTO) / Immutability.
 * [CALLED BY]     Created in btd/ui/screens/GamePanel.java (finishRun()), passed through
 *                 btd/ui/AppController.java (showResults()) to btd/ui/screens/ResultsPanel.java (show(), paintComponent()).
 * [CONNECTS TO]   Nothing else; it only carries data from the game screen to the results screen.
 * [DEFENSE NOTE]  Holds waves cleared, kills, cases opened, previous best, and the new-record flag in one object.
 *                 A record gives the constructor, getters (wavesCleared(), kills()...), equals and toString for free.
 */
public record RunResult(int wavesCleared, int kills, int casesOpened, int previousBest, boolean newRecord) {
}
