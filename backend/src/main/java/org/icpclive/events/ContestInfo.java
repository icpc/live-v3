package org.icpclive.events;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Stream;

public abstract class ContestInfo {
    public int teamNumber;
    public int problemNumber = 0;
    private long startTime = 0;
    public List<ProblemInfo> problems = new ArrayList<>();
    public long lastTime;
    private static String[] hashtags;

    public enum Status {
        BEFORE,
        RUNNING,
        PAUSED,
        OVER
    }

    public Status status = Status.BEFORE;

    public static int CONTEST_LENGTH = 5 * 60 * 60 * 1000;
    public static int FREEZE_TIME = 4 * 60 * 60 * 1000;
    public static final TreeSet<String> GROUPS = new TreeSet<>();

    protected ContestInfo() {
    }

    protected ContestInfo(int problemNumber) {
        this.problemNumber = problemNumber;
    }

    public int getTeamsNumber() {
        return teamNumber;
    }

    public int getProblemsNumber() {
        return problemNumber;
    }

    public void setStatus(Status status) {
        System.err.println("New status: " + status);
        lastTime = getCurrentTime();
        this.status = status;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        System.err.println("Set start time " + new Date(startTime));
        this.startTime = startTime;
    }

    public long getTimeFromStart() {
//        if (status == Status.BEFORE) {
//            return 0;
//        }
        return (long) ((System.currentTimeMillis() - startTime) * EventsLoader.getInstance().getEmulationSpeed());
    }

    public long getCurrentTime() {
        return getCurrentTime(0);
    }

    public long getCurrentTime(int delay) {
        switch (status) {
            case BEFORE:
                return 0;
            case PAUSED:
                return lastTime;
            case RUNNING:
                return startTime == 0 ? 0 :
                        Math.min(getTimeFromStart(), ContestInfo.CONTEST_LENGTH);
            case OVER:
                if (delay != 0) {
                    return Math.min(getTimeFromStart(), ContestInfo.CONTEST_LENGTH + delay);
                }
                return ContestInfo.CONTEST_LENGTH;
            default:
                return 0;
        }
    }

    public boolean isFrozen() {
        return getCurrentTime() >= ContestInfo.FREEZE_TIME;
    }

    public abstract TeamInfo getParticipant(String name);

    public abstract TeamInfo getParticipant(int id);

    public abstract TeamInfo getParticipantByHashTag(String hashTag);

    public abstract TeamInfo[] getStandings();

    public abstract TeamInfo[] getStandings(OptimismLevel optimismLevel);

    public static final String ALL_REGIONS = "all";

    public TeamInfo[] getStandings(String group, OptimismLevel optimismLevel) {
        if (ALL_REGIONS.equals(group)) {
            return getStandings(optimismLevel);
        }
        TeamInfo[] infos = getStandings(optimismLevel);
        return Stream.of(infos).filter(x -> x.getGroups().contains(group)).toArray(TeamInfo[]::new);
    }

    public String[] getHashTags() {
        if (hashtags != null) {
            return hashtags;
        }
        ArrayList<String> hashtags = new ArrayList<>();
        TeamInfo[] infos = getStandings();
        for (TeamInfo teamInfo : infos) {
            if (teamInfo.getHashTag() != null) {
                hashtags.add(teamInfo.getHashTag());
            }
        }
        return hashtags.toArray(new String[0]);
    }

    public abstract long[] firstTimeSolved();

    public abstract RunInfo[] firstSolvedRun();

    public abstract RunInfo[] getRuns();

    public abstract RunInfo getRun(int id);

    public abstract int getLastRunId();
}
