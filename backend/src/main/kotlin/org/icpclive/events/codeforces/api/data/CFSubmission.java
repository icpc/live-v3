package org.icpclive.events.codeforces.api.data;

/**
 * @author egor@egork.net
 */
public class CFSubmission {
    public long id;
    public int contestId;
    public long creationTimeSeconds;
    public long relativeTimeSeconds;
    public CFProblem problem;
    public CFParty author;
    public String programmingLanguage;
    public CFSubmissionVerdict verdict;
    public CFSubmissionTestSet testset;
    public int passedTestCount;
    public int timeConsumedMillis;
    public long memoryConsumedBytes;

    public enum CFSubmissionVerdict {
        FAILED, OK, PARTIAL, COMPILATION_ERROR, RUNTIME_ERROR, WRONG_ANSWER, PRESENTATION_ERROR, TIME_LIMIT_EXCEEDED, MEMORY_LIMIT_EXCEEDED, IDLENESS_LIMIT_EXCEEDED, SECURITY_VIOLATED, CRASHED, INPUT_PREPARATION_CRASHED, CHALLENGED, SKIPPED, TESTING, REJECTED
    }

    public enum CFSubmissionTestSet {
        SAMPLES, PRETESTS, TESTS, CHALLENGES, TESTS1, TESTS2, TESTS3, TESTS4, TESTS5, TESTS6, TESTS7, TESTS8, TESTS9, TESTS10
    }
}
