package org.icpclive.events.WF;

import org.icpclive.events.RunInfo;
import org.icpclive.events.TeamInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by Meepo on 3/5/2018.
 */
public class WFTeamInfo implements TeamInfo {

    protected ArrayList<RunInfo>[] problem_runs;

    public int id = -1;
    public int rank;
    public String name;

    public int solved;
    public int penalty;
    public long lastAccepted;
    public HashSet<String> groups;

    public String shortName;

    public String hashTag;

    public WFTeamInfo(int problems) {
        problem_runs = new ArrayList[problems];
        for (int i = 0; i < problems; i++) {
            problem_runs[i] = new ArrayList<>();
        }
        groups = new HashSet<>();
    }

    public WFTeamInfo(WFTeamInfo teamInfo) {
        this(teamInfo.getRuns().length);
        id = teamInfo.id;
        rank = teamInfo.rank;
        name = teamInfo.name;

        groups = new HashSet<>(teamInfo.groups);
        shortName = teamInfo.shortName;
    }

    public WFTeamInfo copy() {
        WFTeamInfo teamInfo = new WFTeamInfo(problem_runs.length);
        teamInfo.id = id;
        teamInfo.rank = rank;
        teamInfo.name = name;
        teamInfo.groups = new HashSet<>(groups);
        teamInfo.shortName = shortName;

        return teamInfo;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getRank() {
        return rank;
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
    public String getAlias() {
        return (id + 1) + "";
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

    public List<RunInfo>[] getRuns() {
        return problem_runs;
    }

    public long getLastAccepted() {
        return lastAccepted;
    }

    public String getHashTag() {
        return hashTag;
    }

    public void addRun(RunInfo run, int problemId) {
        ArrayList<RunInfo> runs = problem_runs[problemId];
        synchronized (runs) {
            runs.add(run);
        }
    }

    public String toString() {
        return String.format("%03d", id + 1) + ". " + shortName;
    }
}
