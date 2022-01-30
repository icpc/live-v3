package org.icpclive.events.WF;

import org.icpclive.events.*;
import org.icpclive.events.WF.json.WFProblemInfo;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by aksenov on 05.05.2015.
 */
public class WFContestInfo extends ContestInfo {
    protected WFRunInfo[] runs;
    public String[] languages;
    public WFTeamInfo[] teamInfos;
    protected long[] timeFirstSolved;
    protected int maxRunId;
    protected WFRunInfo[] firstSolvedRun;

    private WFTeamInfo[] standings = null;

    public WFContestInfo(int problemsNumber, int teamsNumber) {
        problemNumber = problemsNumber;
        teamNumber = teamsNumber;
        teamInfos = new WFTeamInfo[teamsNumber];
        timeFirstSolved = new long[problemsNumber];
        languages = new String[100];
        runs = new WFRunInfo[1000000];
        firstSolvedRun = new WFRunInfo[problemsNumber];
    }

    protected WFContestInfo() {
    }

    public void recalcStandings() {
        WFTeamInfo[] standings = new WFTeamInfo[teamNumber];
        int n = 0;
        Arrays.fill(timeFirstSolved, Integer.MAX_VALUE);
        Arrays.fill(firstSolvedRun, null);
        for (WFTeamInfo team : teamInfos) {
            if (team == null)
                continue;

            team.solved = 0;
            team.penalty = 0;
            team.lastAccepted = 0;
            for (int j = 0; j < problemNumber; j++) {
                List<? extends RunInfo> runs = team.getRuns().get(j);
                int wrong = 0;
                for (RunInfo run : runs) {
                    WFRunInfo wfrun = (WFRunInfo) run;
                    if ("AC".equals(run.getResult())) {
                        if (!run.isJudged()) {
                            System.err.println("!!!");
                        }
                        team.solved++;
                        int time = (int) (wfrun.getTime() / 60000);
                        team.penalty += wrong * 20 + time;
                        team.lastAccepted = Math.max(team.lastAccepted, wfrun.getTime());
                        if (wfrun.getTime() < timeFirstSolved[j]) {
                            timeFirstSolved[j] = wfrun.getTime();
                            firstSolvedRun[j] = wfrun;
                        }
                        break;
                    } else if (wfrun.getResult().length() > 0 && !"CE".equals(wfrun.getResult())) {
                        wrong++;
                    }
                }
            }
            standings[n++] = team;
        }

        Arrays.sort(standings, 0, n, TeamInfo.strictComparator);

        for (int i = 0; i < n; i++) {
            if (i > 0 && TeamInfo.comparator.compare(standings[i], standings[i - 1]) == 0) {
                standings[i].rank = standings[i - 1].rank;
            } else {
                standings[i].rank = i + 1;
            }
        }
        this.standings = standings;
    }

    public void recalcStandings(WFTeamInfo[] standings) {
        for (WFTeamInfo team : standings) {
            team.solved = 0;
            team.penalty = 0;
            team.lastAccepted = 0;
            for (int j = 0; j < problemNumber; j++) {
                List<? extends RunInfo> runs = team.getRuns().get(j);
                int wrong = 0;
                for (RunInfo run : runs) {
                    if ("AC".equals(run.getResult())) {
                        team.solved++;
                        int time = (int) (run.getTime() / 60000);
                        team.penalty += wrong * 20 + time;
                        team.lastAccepted = Math.max(team.lastAccepted, run.getTime());
                        break;
                    } else if (run.getResult().length() > 0 && !"CE".equals(run.getResult())) {
                        wrong++;
                    }
                }
            }
        }

        Arrays.sort(standings, 0, standings.length, TeamInfo.strictComparator);

        for (int i = 0; i < standings.length; i++) {
            if (i > 0 && TeamInfo.comparator.compare(standings[i], standings[i - 1]) == 0) {
                standings[i].rank = standings[i - 1].rank;
            } else {
                standings[i].rank = i + 1;
            }
        }
    }


    public void addTeam(WFTeamInfo team) {
        teamInfos[team.getId()] = team;
    }

    public boolean runExists(int id) {
        return runs[id] != null;
    }

    public WFRunInfo getRun(int id) {
        return runs[id];
    }

    public void addRun(WFRunInfo run) {
//		System.err.println("add runId: " + run.getId());
        if (!runExists(run.getId())) {
            runs[run.getId()] = run;
            teamInfos[run.getTeamId()].addRun(run, run.getProblemId());
            maxRunId = Math.max(maxRunId, run.getId());
        }
    }

    public int getLastRunId() {
        return maxRunId;
    }

    public void addTest(WFTestCaseInfo test) {
//		System.out.println("Adding test " + test.id + " to runId " + test.runId);
        if (runExists(test.runId)) {
            WFRunInfo run = runs[test.runId];
            run.add(test);
            if (!run.isJudged()) {
                run.setLastUpdateTime(Math.max(run.getLastUpdateTime(), test.time));
            }
//			System.out.println("Run " + runs[test.runId] + " passed " + runs[test.runId].getPassedTestsNumber() + " tests");
        }
    }

    @Override
    public TeamInfo getParticipant(String name) {
        for (int i = 0; i < teamNumber; i++) {
            if (teamInfos[i].getName().equals(name) || teamInfos[i].getShortName().equals(name)) {
                return teamInfos[i];
            }
        }
        return null;
    }

    @Override
    public TeamInfo getParticipant(int id) {
        return teamInfos[id];
    }

    public TeamInfo[] getStandings() {
        return standings;
    }

    @Override
    public long[] firstTimeSolved() {
        return timeFirstSolved;
    }

    @Override
    public RunInfo[] firstSolvedRun() {
        return firstSolvedRun;
    }

    @Override
    public RunInfo[] getRuns() {
        return runs;
    }

    public WFProblemInfo getProblemById(int id) {
        return (WFProblemInfo) problems.get(id);
    }

    public WFTeamInfo getParticipantByHashTag(String hashTag) {
        for (int i = 0; i < teamNumber; i++) {
            if (hashTag != null && hashTag.equalsIgnoreCase(teamInfos[i].getHashTag())) {
                return teamInfos[i];
            }
        }
        return null;
    }

    private TeamInfo[] getPossibleStandings(boolean isOptimistic) {
        WFTeamInfo[] possibleStandings = new WFTeamInfo[teamNumber];
        int teamIndex = 0;
        for (WFTeamInfo team : standings) {
            possibleStandings[teamIndex] = team.copy();
            for (int j = 0; j < problemNumber; j++) {
                List<? extends RunInfo> runs = team.getRuns().get(j);
                int runIndex = 0;
                for (RunInfo run : runs) {
                    WFRunInfo clonedRun = new WFRunInfo((WFRunInfo) run);

                    if (clonedRun.getResult().length() == 0) {
                        clonedRun.judged = true;
                        String expectedResult = isOptimistic ? "AC" : "WA";
                        clonedRun.result = (runIndex == runs.size() - 1) ? expectedResult : "WA";
                        clonedRun.reallyUnknown = true;
                    }
                    possibleStandings[teamIndex].addRun(clonedRun, j);
                    runIndex++;
                }
            }
            teamIndex++;
        }

        recalcStandings(possibleStandings);

        return possibleStandings;
    }

    public TeamInfo[] getStandings(OptimismLevel level) {
        switch (level) {
            case NORMAL:
                return getStandings();
            case OPTIMISTIC:
                return getPossibleStandings(true);
            case PESSIMISTIC:
                return getPossibleStandings(false);
        }

        return null;
    }
}
