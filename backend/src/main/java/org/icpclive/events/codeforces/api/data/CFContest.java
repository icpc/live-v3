package org.icpclive.events.codeforces.api.data;

/**
 * @author egor@egork.net
 */
public class CFContest {
    public int id;
    public String name;
    public CFContestType type;
    public CFContestPhase phase;
    public boolean frozen;
    public long durationSeconds;
    public Long startTimeSeconds;
    public Long relativeTimeSeconds;
    public String preparedBy;
    public String websiteUrl;
    public String description;
    public Integer difficulty;
    public String kind;
    public String icpcRegion;
    public String country;
    public String city;
    public String season;

    public enum CFContestType {
        CF, IOI, ICPC
    }

    public enum CFContestPhase {
        BEFORE, CODING, PENDING_SYSTEM_TEST, SYSTEM_TEST, FINISHED
    }
}
