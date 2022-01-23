package org.icpclive.events.codeforces;

import org.icpclive.events.RunInfo;
import org.icpclive.events.TeamInfo;
import org.icpclive.events.codeforces.api.data.CFProblemResult;
import org.icpclive.events.codeforces.api.data.CFRanklistRow;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author egor@egork.net
 */
public class CFTeamInfo implements TeamInfo {
    private final CFRanklistRow row;
    private int id;

    public CFTeamInfo(CFRanklistRow row) {
        this.row = row;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getRank() {
        return row.rank;
    }

    @Override
    public String getName() {
        if (row.party.teamName != null) {
            return row.party.teamName;
        }
        return row.party.members.get(0).handle;
    }

    @Override
    public String getShortName() {
        return getName();
    }

    @Override
    public String getAlias() {
        return getName();
    }

    @Override
    public Set<String> getGroups() {
        return Collections.emptySet();
    }

    @Override
    public int getPenalty() {
        return row.penalty == 0 ? (int) row.points : row.penalty;
    }

    public int getPoints() {
        return (int) row.points;
    }

    @Override
    public int getSolvedProblemsNumber() {
        int solved = 0;
        for (CFProblemResult result : row.problemResults) {
            solved += result.points > 0 ? 1 : 0;
        }
        return solved;
    }

    @Override
    public long getLastAccepted() {
        long last = 0;
        for (CFProblemResult result : row.problemResults) {
            if (result.points > 0) {
                last = Math.max(last, result.bestSubmissionTimeSeconds);
            }
        }
        return last;
    }

    @Override
    public List<CFRunInfo>[] getRuns() {
        return CFEventsLoader.getInstance().getContestData().getRuns(row.party);
    }

    @Override
    public void addRun(RunInfo run, int problem) {
        CFEventsLoader.getInstance().getContestData().addRun((CFRunInfo) run, problem);
    }

    @Override
    public String getHashTag() {
        return "";
    }

    @Override
    public TeamInfo copy() {
        return new CFTeamInfo(row);
    }

    public void setId(int id) {
        this.id = id;
    }
}
