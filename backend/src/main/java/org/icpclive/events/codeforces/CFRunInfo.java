package org.icpclive.events.codeforces;

import org.icpclive.events.RunInfo;
import org.icpclive.events.SmallTeamInfo;
import org.icpclive.events.codeforces.api.data.CFSubmission;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * @author egor@egork.net
 */
public class CFRunInfo implements RunInfo {
    private static final Map<CFSubmission.CFSubmissionVerdict, String> verdictToString;

    static {
        EnumMap<CFSubmission.CFSubmissionVerdict, String> vts = new EnumMap<CFSubmission.CFSubmissionVerdict, String>(CFSubmission.CFSubmissionVerdict.class);
        vts.put(CFSubmission.CFSubmissionVerdict.CHALLENGED, "CH");
        vts.put(CFSubmission.CFSubmissionVerdict.COMPILATION_ERROR, "CE");
        vts.put(CFSubmission.CFSubmissionVerdict.CRASHED, "CR");
        vts.put(CFSubmission.CFSubmissionVerdict.FAILED, "FL");
        vts.put(CFSubmission.CFSubmissionVerdict.IDLENESS_LIMIT_EXCEEDED, "IL");
        vts.put(CFSubmission.CFSubmissionVerdict.INPUT_PREPARATION_CRASHED, "IC");
        vts.put(CFSubmission.CFSubmissionVerdict.MEMORY_LIMIT_EXCEEDED, "ML");
        vts.put(CFSubmission.CFSubmissionVerdict.OK, "AC");
        vts.put(CFSubmission.CFSubmissionVerdict.PARTIAL, "PA");
        vts.put(CFSubmission.CFSubmissionVerdict.PRESENTATION_ERROR, "PE");
        vts.put(CFSubmission.CFSubmissionVerdict.REJECTED, "RJ");
        vts.put(CFSubmission.CFSubmissionVerdict.RUNTIME_ERROR, "RE");
        vts.put(CFSubmission.CFSubmissionVerdict.SECURITY_VIOLATED, "SV");
        vts.put(CFSubmission.CFSubmissionVerdict.SKIPPED, "SK");
        vts.put(CFSubmission.CFSubmissionVerdict.TESTING, "");
        vts.put(CFSubmission.CFSubmissionVerdict.TIME_LIMIT_EXCEEDED, "TL");
        vts.put(CFSubmission.CFSubmissionVerdict.WRONG_ANSWER, "WA");
        verdictToString = Collections.unmodifiableMap(vts);
    }

    private CFSubmission submission;
    private long lastUpdate;
    private int points = 0;

    public CFRunInfo(CFSubmission submission) {
        this.submission = submission;
        lastUpdate = this.submission.relativeTimeSeconds;
    }

    @Override
    public int getId() {
        return (int) submission.id;
    }

    @Override
    public boolean isAccepted() {
        return submission.verdict == CFSubmission.CFSubmissionVerdict.OK;
    }

    @Override
    public boolean isJudged() {
        return submission.verdict != CFSubmission.CFSubmissionVerdict.TESTING;
    }

    @Override
    public String getResult() {
        if (submission.verdict == null) {
            return "";
        }
        return verdictToString.get(submission.verdict);
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getPoints() {
        return points;
    }

    @Override
    public CFProblemInfo getProblem() {
        return CFEventsLoader.getInstance().getContestData().getProblem(submission.problem);
    }

    @Override
    public int getProblemId() {
        return getProblem().id;
    }

    @Override
    public int getTeamId() {
        CFTeamInfo participant = CFEventsLoader.getInstance().getContestData().getParticipant(CFContestInfo.getName(submission.author));
        return participant.getId();
    }

    @Override
    public SmallTeamInfo getTeamInfoBefore() {
        return null;
    }

    @Override
    public boolean isReallyUnknown() {
        return false;
    }

    @Override
    public double getPercentage() {
        return (double) submission.passedTestCount / getProblem().getTotalTests();
    }

    @Override
    public long getTime() {
        return submission.relativeTimeSeconds * 1000;
    }

    @Override
    public long getLastUpdateTime() {
        return lastUpdate * 1000;
    }

    public CFSubmission getSubmission() {
        return submission;
    }

    public void updateFrom(CFSubmission submission, long contestTime) {
        if (submission.verdict != this.submission.verdict || submission.passedTestCount != this.submission.passedTestCount) {
            this.submission = submission;
            lastUpdate = contestTime;
        }
    }
}
