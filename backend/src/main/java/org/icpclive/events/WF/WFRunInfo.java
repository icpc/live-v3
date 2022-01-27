package org.icpclive.events.WF;

import org.icpclive.events.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by aksenov on 16.04.2015.
 */
public class WFRunInfo implements RunInfo {
    public int id;
    public boolean judged;
    public boolean reallyUnknown;

    public String result = "";
    public int languageId;
    public int problemId;
    public int passed;
    public int total;
    public long time;
    public long lastUpdateTime;

    public int teamId;
    public TeamInfo team;

    private Set<Integer> passedTests = new HashSet<>();

    public WFRunInfo() {
    }

    public WFRunInfo(WFRunInfo another) {
        this.id = another.id;
        this.judged = another.judged;
        this.result = another.result;
        this.languageId = another.languageId;
        this.problemId = another.problemId;
        this.teamId = another.teamId;
        this.time = another.time;
        this.passed = another.getPassedTestsNumber();
        this.total = another.getTotalTestsNumber();
        this.lastUpdateTime = another.getLastUpdateTime();
    }

    public void add(WFTestCaseInfo test) {
        if (total == 0) {
            total = test.total;
        }
        passedTests.add(test.id);
        passed = passedTests.size();
        lastUpdateTime = Math.max(lastUpdateTime, test.time);
    }

    @Override
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = Math.max(this.lastUpdateTime, lastUpdateTime); // ?????
    }

    public int getPassedTestsNumber() {
        return passed;
    }

    public int getTotalTestsNumber() {
        return total;
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean isAccepted() {
        return "AC".equals(result);
    }

    @Override
    public boolean isAddingPenalty() {
        // TODO: this should be received from cds
        return isJudged() && !isAccepted() && !"CE".equals(result);
    }

    @Override
    public boolean isJudged() {
        return judged;
    }

    @Override
    public String getResult() {
        return result;
    }

    @Override
    public ProblemInfo getProblem() {
        return EventsLoader.getInstance().getContestData().problems.get(getProblemId());
    }

    @Override
    public int getProblemId() {
        return problemId;
    }

    @Override
    public long getTime() {
        return time;
    }

    public int getTeamId() {
        return teamId;
    }

    public boolean isReallyUnknown() {
        return reallyUnknown;
    }

    public double getPercentage() {
        return 1.0 * this.getPassedTestsNumber() / this.getTotalTestsNumber();
    }

    @Override
    public String toString() {
        String teamName = "" + teamId;
        if (team != null) teamName = team.getShortName();
        return teamName + " " + (char) ('A' + problemId) + " " + result;
    }
}
