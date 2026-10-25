package btd.core;

/** What happened in one run, shown on the results screen. */
public record RunResult(int wavesCleared, int kills, int casesOpened, int previousBest, boolean newRecord) {
}
