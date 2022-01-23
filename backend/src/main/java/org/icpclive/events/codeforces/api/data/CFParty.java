package org.icpclive.events.codeforces.api.data;

import java.util.List;

/**
 * @author egor@egork.net
 */
public class CFParty {
    public Integer contestId;
    public List<CFMember> members;
    public CFPartyParticipantType participantType;
    public Integer teamId;
    public String teamName;
    public boolean ghost;
    public Integer room;
    public Long startTimeSeconds;

    public enum CFPartyParticipantType {
        CONTESTANT, PRACTICE, VIRTUAL, MANAGER, OUT_OF_COMPETITION
    }
}
