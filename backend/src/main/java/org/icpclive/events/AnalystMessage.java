package org.icpclive.events;

import org.icpclive.events.WF.WFAnalystMessage;

/**
 * @author egor@egork.net
 */
public interface AnalystMessage {
    int getId();

    int getTeam();

    long getTime();

    int getPriority();

    int getProblem();

    int getRunId();

    WFAnalystMessage.WFAnalystMessageCategory getCategory();

    String getMessage();
}
