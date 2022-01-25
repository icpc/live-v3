package org.icpclive.events.PCMS;

import org.icpclive.events.EventsLoader;
import org.icpclive.events.ProblemInfo;
import org.icpclive.events.RunInfo;

public class PCMSRunInfo implements RunInfo {
    public PCMSRunInfo() {
        this.judged = true;
    }

    public PCMSRunInfo(boolean judged, String result, int problem, long time, long timestamp, int teamId) {
        this.judged = judged;
        this.result = result;
        this.problem = problem;
        this.time = time;
        this.timestamp = timestamp;
        this.lastUpdateTimestamp = time;
        this.teamId = teamId;
    }

    public PCMSRunInfo(RunInfo run) {
        this.judged = run.isJudged();
        this.result = run.getResult();
        this.problem = run.getProblemId();
        this.time = run.getTime();
        this.lastUpdateTimestamp = run.getLastUpdateTime();
        this.teamId = run.getTeamId();
    }

    public int getId() {
        return id;
    }

    public boolean isAccepted() {
        return "AC".equals(result);
    }

    @Override
    public boolean isJudged() {
        return judged;
    }

    public void setIsJudged(boolean judged) {
        this.judged = judged;
    }

    @Override
    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    @Override
    public ProblemInfo getProblem() {
        return EventsLoader.getInstance().getContestData().problems.get(getProblemId());
    }

    @Override
    public int getProblemId() {
        return problem;
    }

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public long getLastUpdateTime() {
        return lastUpdateTimestamp;
    }

    public void setLastUpdateTimestamp(long lastUpdateTimestamp) {
        lastUpdateTimestamp = System.currentTimeMillis();
    }

    public int getTeamId() {
        return teamId;
    }

    public boolean isReallyUnknown() {
        return reallyUnknown;
    }

    public double getPercentage() {
        return 0;
    }

    protected boolean judged;
    protected String result = "";
    protected int id;
    protected int teamId;
    protected int problem;
    protected long time;
    protected long timestamp;
    protected long lastUpdateTimestamp;
    public boolean reallyUnknown;
}
