package org.icpclive.events.codeforces;

import org.icpclive.events.ProblemInfo;
import org.icpclive.events.codeforces.api.data.CFProblem;

/**
 * @author egor@egork.net
 */
public class CFProblemInfo extends ProblemInfo {
    public final CFProblem problem;
    public final int id;
    private int totalTests = 1;

    public CFProblemInfo(CFProblem problem, int id) {
        this.problem = problem;
        this.id = id;
        letter = problem.index;
        name = problem.name;
    }

    public void update(CFRunInfo run) {
        totalTests = Math.max(totalTests, run.getSubmission().passedTestCount + (run.isAccepted() ? 0 : 1));
    }

    public int getTotalTests() {
        return totalTests;
    }
}
