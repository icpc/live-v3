package org.icpclive.events.codeforces.api.data;

import java.util.List;

/**
 * @author egor@egork.net
 */
public class CFRanklistRow {
    public CFParty party;
    public int rank;
    public double points;
    public int penalty;
    public int successfulHackCount;
    public int unsuccessfulHackCount;
    public List<CFProblemResult> problemResults;
    public Long lastSubmissionTimeSeconds;
}
