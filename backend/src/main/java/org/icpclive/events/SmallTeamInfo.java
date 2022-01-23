package org.icpclive.events;

import java.util.List;
import java.util.HashSet;

/**
 * Created by Aksenov239 on 19.03.2016.
 */
public class SmallTeamInfo implements TeamInfo {
    private int rank, solved, penalty;
    private String shortName;

    public SmallTeamInfo(TeamInfo team) {
        rank = team.getRank();
        solved = team.getSolvedProblemsNumber();
        penalty = team.getPenalty();
        shortName = team.getShortName();
    }

    public int getId() {
        return -1;
    }

    public int getRank() {
        return rank;
    }

    public String getName() {
        return null;
    }

    public String getShortName() {
        return shortName;
    }

    public String getAlias() {
        return null;
    }

    public HashSet<String> getGroups() {
        return null;
    }

    public int getSolvedProblemsNumber() {
        return solved;
    }

    public int getPenalty() {
        return penalty;
    }

    public long getLastAccepted() {
        return 0;
    }

    public List<RunInfo>[] getRuns() {
        return null;
    }

    public String getHashTag() { return null; }

    public void addRun(RunInfo run, int problem) {
    }
    public TeamInfo copy() {
        return new SmallTeamInfo(this);
    }
}
