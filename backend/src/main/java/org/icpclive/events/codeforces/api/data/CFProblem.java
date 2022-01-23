package org.icpclive.events.codeforces.api.data;

import java.util.List;

/**
 * @author egor@egork.net
 */
public class CFProblem {
    public Integer contestId;
    public String problemsetName;
    public String index;
    public String name;
    public CFProblemType type;
    public Double points;
    public Integer rating;
    public List<String> tags;

    public enum CFProblemType {
        PROGRAMMING,
        QUESTION
    }
}
