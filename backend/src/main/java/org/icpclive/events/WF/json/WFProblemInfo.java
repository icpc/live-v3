package org.icpclive.events.WF.json;

import org.icpclive.events.ProblemInfo;

/**
 * Created by Aksenov239 on 3/5/2018.
 */
public class WFProblemInfo extends ProblemInfo {
    public int id;
    public int testCount;
    public int[] submissions;

    public WFProblemInfo(int languages) {
        submissions = new int[languages];
    }
}
