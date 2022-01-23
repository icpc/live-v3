package org.icpclive.events.codeforces.api.data;

/**
 * @author egor@egork.net
 */
public class CFProblemResult {
    public double points;
    public int penalty;
    public int rejectedAttemptCount;
    public CFProblemResultType type;
    public long bestSubmissionTimeSeconds;

    public enum CFProblemResultType {
        PRELIMINARY, FINAL
    }
}
