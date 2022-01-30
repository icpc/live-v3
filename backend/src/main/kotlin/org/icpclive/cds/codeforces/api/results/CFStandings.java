package org.icpclive.cds.codeforces.api.results;

import org.icpclive.cds.codeforces.api.data.CFContest;
import org.icpclive.cds.codeforces.api.data.CFProblem;
import org.icpclive.cds.codeforces.api.data.CFRanklistRow;

import java.util.List;

/**
 * @author egor@egork.net
 */
public class CFStandings {
    public CFContest contest;
    public List<CFProblem> problems;
    public List<CFRanklistRow> rows;
}
