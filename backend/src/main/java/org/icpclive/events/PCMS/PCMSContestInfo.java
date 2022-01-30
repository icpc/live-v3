package org.icpclive.events.PCMS;

import org.icpclive.events.*;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class PCMSContestInfo extends ContestInfo {
    @Override
    public TeamInfo[] getStandings() {
        return standings.stream().toArray(TeamInfo[]::new);
    }

    public TeamInfo[] getPossibleStandings(boolean optimistic) {
        TeamInfo[] original = getStandings();

        PCMSTeamInfo[] standings = new PCMSTeamInfo[original.length];
        for (int i = 0; i < original.length; i++) {
            standings[i] = (PCMSTeamInfo)original[i].copy();
            List<? extends List<? extends RunInfo>> runs = original[i].getRuns();
            for (int j = 0; j < problemNumber; j++) {
                int runIndex = 0;
                for (RunInfo run : runs.get(j)) {
                    PCMSRunInfo clonedRun = new PCMSRunInfo(run);

                    if (clonedRun.getResult().length() == 0) {
                        clonedRun.judged = true;
                        String expectedResult = optimistic ? "AC" : "WA";
                        clonedRun.result = runIndex == runs.get(j).size() - 1 ? expectedResult : "WA";
                        clonedRun.reallyUnknown = true;
                    }
                    standings[i].addRun(clonedRun, j);
                    runIndex++;
                }
            }
        }
        for (PCMSTeamInfo team : standings) {
            team.solved = 0;
            team.penalty = 0;
            team.lastAccepted = 0;
            List<? extends List<? extends RunInfo>> runs = team.getRuns();
            for (int j = 0; j < problemNumber; j++) {
                int wrong = 0;
                for (RunInfo run : runs.get(j)) {
                    if ("AC".equals(run.getResult())) {
                        team.solved++;
                        int time = (int)(run.getTime() / 60 / 1000);
                        team.penalty += wrong * 20 + time;
                        team.lastAccepted = Math.max(team.lastAccepted, run.getTime());
                        break;
                    } else if (run.getResult().length() > 0 && !"CE".equals(run.getResult())) {
                        wrong++;
                    }
                }
            }
        }

        Arrays.sort(standings, TeamInfo.comparator);

        for (int i = 0; i < standings.length; i++) {
            if (i > 0 && TeamInfo.comparator.compare(standings[i - 1], standings[i]) == 0) {
                standings[i].rank = standings[i - 1].rank;
            } else {
                standings[i].rank = i + 1;
            }
        }
        return standings;
    }

    @Override
    public TeamInfo[] getStandings(OptimismLevel optimismLevel) {
        switch (optimismLevel) {
            case NORMAL:
                return getStandings();
            case OPTIMISTIC:
                return getPossibleStandings(true);
            case PESSIMISTIC:
                return getPossibleStandings(false);
        }
        return null;
    }

    public PCMSContestInfo(int problemNumber) {
        super(problemNumber);
        standings = new ArrayList<>();
        positions = new HashMap<>();
        timeFirstSolved = new long[problemNumber];

        FREEZE_TIME = 4 * 60 * 60 * 1000;
    }

    public void fillTimeFirstSolved() {
        standings.forEach(teamInfo -> {
            ArrayList<ArrayList<RunInfo>> runs = teamInfo.getRuns();
            for (int i = 0; i < runs.size(); i++) {
                for (RunInfo run : runs.get(i)) {
                    if (run.isAccepted()) {
                        timeFirstSolved[i] = Math.min(timeFirstSolved[i], run.getTime());
                    }
                }
            }
        });
    }

    public void calculateRanks() {
        standings.get(0).rank = 1;
        for (int i = 1; i < standings.size(); i++) {
            if (TeamInfo.comparator.compare(standings.get(i), standings.get(i - 1)) == 0) {
                standings.get(i).rank = standings.get(i - 1).rank;
            } else {
                standings.get(i).rank = i + 1;
            }
        }
    }

    public void makeRuns() {
        ArrayList<RunInfo> runs = new ArrayList<>();
        for (TeamInfo team : standings) {
            for (List<? extends RunInfo> innerRuns : team.getRuns()) {
                runs.addAll(innerRuns);
            }
        }
        this.runs = runs.toArray(new PCMSRunInfo[0]);
        Arrays.sort(this.runs);

        firstSolvedRuns = new PCMSRunInfo[problemNumber];
        for (RunInfo run : this.runs) {
            if (firstSolvedRuns[run.getProblemId()] == null && run.isAccepted() &&
                    run.getTime() <= FREEZE_TIME) {
                firstSolvedRuns[run.getProblemId()] = run;
            }
        }
    }

    public void addTeamStandings(PCMSTeamInfo teamInfo) {
        standings.add(teamInfo);
        positions.put(teamInfo.getAlias(), standings.size() - 1);
        teamNumber = standings.size();
    }

    PCMSTeamInfo getParticipant(Integer teamRank) {
        return teamRank == null ? new PCMSTeamInfo(problemNumber) : standings.get(teamRank);
    }

    @Override
    public PCMSTeamInfo getParticipant(String name) {
        Integer teamRank = getParticipantRankByName(name);
        return getParticipant(teamRank);
    }

    @Override
    public PCMSTeamInfo getParticipant(int id) {
        for (PCMSTeamInfo team: standings) {
            if (team.getId() == id) {
                return team;
            }
        }
        return null;
    }

    Integer getParticipantRankByName(String participantName) {
        return positions.get(participantName);
    }

    public long[] firstTimeSolved() {
        return timeFirstSolved;
    }

    @Override
    public RunInfo[] firstSolvedRun() {
        return firstSolvedRuns;
    }

    @Override
    public PCMSTeamInfo getParticipantByHashTag(String hashTag) {
        for (PCMSTeamInfo teamInfo : standings) {
            if (hashTag != null && hashTag.equalsIgnoreCase(teamInfo.getHashTag())) {
                return teamInfo;
            }
        }
        return null;
    }

    protected ArrayList<PCMSTeamInfo> standings;
    protected long[] timeFirstSolved;

    public Map<String, Integer> positions;
    public boolean frozen;

    private RunInfo[] runs;
    private RunInfo[] firstSolvedRuns;

    public int lastRunId = 0;
    
	@Override
	public RunInfo[] getRuns() {
		return runs;
	}

    @Override
    public RunInfo getRun(int id) {
        return null;
    }

    @Override
    public int getLastRunId() {
        return lastRunId;
    }

}