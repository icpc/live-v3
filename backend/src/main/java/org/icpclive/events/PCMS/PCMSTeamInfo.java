package org.icpclive.events.PCMS;

import org.icpclive.events.PCMS.ioi.IOIPCMSRunInfo;
import org.icpclive.events.RunInfo;
import org.icpclive.events.TeamInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class PCMSTeamInfo implements TeamInfo {
    public String alias;

    public PCMSTeamInfo(int problemsNumber) {
        problemRuns = new ArrayList[problemsNumber];
        Arrays.setAll(problemRuns, i -> new ArrayList<>());
        this.rank = 1;
    }

    public PCMSTeamInfo(int id, String alias, String hallId, String name, String shortName, String hashTag,
                        HashSet<String> groups, int problemsNumber, int delay) {
        this(problemsNumber);

        this.id = id;
        this.alias = alias;
        this.hallId = hallId;
        this.name = name;
        this.shortName = shortName;
        this.groups = groups == null ? null : new HashSet<>(groups);
        this.hashTag = hashTag;
        this.delay = delay;
    }

    public PCMSTeamInfo(String name, int problemsNumber) {
        this(-1, "", "1", name, null, null, null, problemsNumber, 0);
    }

    public PCMSTeamInfo(PCMSTeamInfo pcmsTeamInfo) {
        this(pcmsTeamInfo.id, pcmsTeamInfo.alias, pcmsTeamInfo.hallId, pcmsTeamInfo.name,
                pcmsTeamInfo.shortName, pcmsTeamInfo.hashTag, pcmsTeamInfo.groups, pcmsTeamInfo.problemRuns.length,
                pcmsTeamInfo.delay);

        for (int i = 0; i < pcmsTeamInfo.problemRuns.length; i++) {
            problemRuns[i].addAll(pcmsTeamInfo.problemRuns[i]);
        }
    }

    @Override
    public void addRun(RunInfo run, int problemId) {
        if (run != null) {
            problemRuns[problemId].add(run);
        }
    }

    public int mergeRuns(ArrayList<PCMSRunInfo> runs, int problemId, int lastRunId, long currentTime) {
        int previousSize = problemRuns[problemId].size();
        for (int i = 0; i < previousSize && i < runs.size(); i++) {
            PCMSRunInfo run = (PCMSRunInfo) problemRuns[problemId].get(i);
            if (run instanceof IOIPCMSRunInfo) {
                if ((((IOIPCMSRunInfo) run).getScore() != ((IOIPCMSRunInfo) runs.get(i)).getScore())
                        || (!run.isJudged() && runs.get(i).isJudged())
                        || (((IOIPCMSRunInfo) run).getTotalScore() < ((IOIPCMSRunInfo) runs.get(i)).getTotalScore())) {
                    run.setLastUpdateTimestamp(currentTime);
                    run.setResult(runs.get(i).result);
                    run.setIsJudged(true);
                    IOIPCMSRunInfo ioiRun = (IOIPCMSRunInfo) run;
                    ioiRun.setScore(((IOIPCMSRunInfo) runs.get(i)).getScore());
                    ioiRun.setTotalScore(((IOIPCMSRunInfo) runs.get(i)).getTotalScore());
                }

            } else {
                if (!run.isJudged() &&
                        runs.get(i).isJudged()) {
                    run.setLastUpdateTimestamp(currentTime);
                    run.setResult(runs.get(i).result);
                    run.setIsJudged(true);
                }
            }
        }
        for (int i = previousSize; i < runs.size(); i++) {
            runs.get(i).id = lastRunId++;
            problemRuns[problemId].add(runs.get(i));
        }
        return lastRunId;
    }

    public int getRunsNumber(int problemId) {
        return problemRuns[problemId].size();
    }

    public long getLastSubmitTime(int problemId) {
        int runsNumber = getRunsNumber(problemId);
        return runsNumber == 0 ? -1 : problemRuns[problemId].get(runsNumber).getTime();
    }

    public int getId() {
        return id;
    }

    @Override
    public int getRank() {
        return rank;
    }

    public String getAlias() {
        return alias;
    }

    public String getHallId() {
        return hallId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getShortName() {
        return shortName;
    }

    @Override
    public HashSet<String> getGroups() {
        return groups;
    }

    @Override
    public int getPenalty() {
        return penalty;
    }

    @Override
    public int getSolvedProblemsNumber() {
        return solved;
    }

    public ArrayList<RunInfo>[] getRuns() {
        return problemRuns;
    }

    public long getLastAccepted() {
        return lastAccepted;
    }

    public String getHashTag() {
        return hashTag;
    }

    public int getDelay() {
        return delay;
    }

    @Override
    public PCMSTeamInfo copy() {
        return new PCMSTeamInfo(this.id, this.alias, this.hallId, this.name, this.shortName, this.hashTag,
                this.groups, problemRuns.length, delay);
    }

    public String toString() {
        return hallId + ". " + shortName;
    }

    public int id;

    public String hallId;
    public String name;
    public String shortName;
    public HashSet<String> groups;
    public String hashTag;

    public int rank;
    public int solved;
    public int penalty;
    public long lastAccepted;

    public ArrayList<RunInfo>[] problemRuns;

    public int delay;
}
