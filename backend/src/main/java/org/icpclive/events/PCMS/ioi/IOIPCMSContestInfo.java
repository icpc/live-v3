package org.icpclive.events.PCMS.ioi;

import org.icpclive.events.*;
import org.icpclive.events.PCMS.PCMSContestInfo;

public class IOIPCMSContestInfo extends PCMSContestInfo {
    @Override
    public TeamInfo[] getStandings(OptimismLevel optimismLevel) {
        return getStandings();
    }

    IOIPCMSContestInfo(int problemNumber) {
        super(problemNumber);
    }

    public void calculateRanks() {
        standings.get(0).rank = 1;
        for (int i = 1; i < standings.size(); i++) {
            if (IOIPCMSTeamInfo.comparator.compare((IOIPCMSTeamInfo) standings.get(i), (IOIPCMSTeamInfo) standings.get(i - 1)) == 0) {
                standings.get(i).rank = standings.get(i - 1).rank;
            } else {
                standings.get(i).rank = i + 1;
            }
        }
    }
}