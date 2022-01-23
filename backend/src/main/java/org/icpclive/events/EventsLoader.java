package org.icpclive.events;

import org.icpclive.Config;
import org.icpclive.events.PCMS.PCMSEventsLoader;
import org.icpclive.events.PCMS.ioi.IOIPCMSEventsLoader;
import org.icpclive.events.WF.json.WFEventsLoader;
import org.icpclive.events.codeforces.CFEventsLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

public abstract class EventsLoader implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(EventsLoader.class);

    private static EventsLoader instance;

    protected double emulationSpeed;
    protected long emulationStartTime;

    public static synchronized EventsLoader getInstance() {
        if (instance == null) {
            try {
                Properties properties = Config.loadProperties("events");
                String standingsType = properties.getProperty("standings.type");
                if ("WF".equals(standingsType)) {
                    instance = new WFEventsLoader(false);
                } else if ("WFRegionals".equals(standingsType)) {
                    instance = new WFEventsLoader(true);
                } else if ("PCMS".equals(standingsType)) {
                    instance = new PCMSEventsLoader();
                } else if ("CF".equals(standingsType)) {
                    instance = new CFEventsLoader();
                } else if ("IOIPCMS".equals(standingsType)) {
                    instance = new IOIPCMSEventsLoader();
                }
            } catch (IOException e) {
                log.error("error", e);
            }
        }
        return instance;
    }


    public abstract ContestInfo getContestData();

    public double getEmulationSpeed() {
        return emulationSpeed;
    }
}
