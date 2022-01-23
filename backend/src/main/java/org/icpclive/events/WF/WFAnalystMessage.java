package org.icpclive.events.WF;

import org.icpclive.events.AnalystMessage;
import org.slf4j.*;

/**
 * @author egor@egork.net
 */
public class WFAnalystMessage implements AnalystMessage {
    private static Logger log = LoggerFactory.getLogger(WFAnalystMessage.class);

    private int id = -1;
    private int team = -1;
    private long time = -1;
    private int priority = -1;
    private int problem = -1;
    private int runId = -1;
    private WFAnalystMessageCategory category;
    private String message;

    public enum WFAnalystMessageCategory {
        AUTO("auto"),
        HUMAN("human");

        public final String key;


        WFAnalystMessageCategory(String key) {
            this.key = key;
        }

        public static WFAnalystMessageCategory getCategory(String category) {
            for (WFAnalystMessageCategory aCategory: values()) {
                if (category.equals(aCategory.key)) {
                    return aCategory;
                }
            }
            log.warn("Unknown category: " + category);
            return HUMAN;
        }
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getTeam() {
        return team;
    }

    public void setTeam(int team) {
        this.team = team;
    }

    @Override
    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public int getProblem() {
        return problem;
    }

    public void setProblem(int problem) {
        this.problem = problem;
    }

    @Override
    public int getRunId() {
        return runId;
    }

    public void setRunId(int runId) {
        this.runId = runId;
    }

    @Override
    public WFAnalystMessageCategory getCategory() {
        return category;
    }

    public void setCategory(WFAnalystMessageCategory category) {
        this.category = category;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "WFAnalystMessage{" +
                "id=" + id +
                ", team=" + team +
                ", time=" + time +
                ", priority=" + priority +
                ", problem=" + problem +
                ", runId=" + runId +
                ", category=" + category +
                ", message='" + message + '\'' +
                '}';
    }
}
