package org.icpclive.events.codeforces;

import org.icpclive.Config;
import org.icpclive.DataBus;
import org.icpclive.events.EventsLoader;
import org.icpclive.events.codeforces.api.CFApiCentral;
import org.icpclive.events.codeforces.api.data.CFContest;
import org.icpclive.events.codeforces.api.data.CFSubmission;
import org.icpclive.events.codeforces.api.results.CFStandings;
import org.slf4j.*;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * @author egor@egork.net
 */
public class CFEventsLoader extends EventsLoader {
    private static final Logger log = LoggerFactory.getLogger(CFEventsLoader.class);
    private static final String CF_API_KEY_PROPERTY_NAME = "cf.api.key";
    private static final String CF_API_SECRET_PROPERTY_NAME = "cf.api.secret";
    private CFContestInfo contestInfo = new CFContestInfo();
    private CFApiCentral central;

    public CFEventsLoader() throws IOException {
        Properties properties = Config.loadProperties("events");
        emulationSpeed = 1;
        central = new CFApiCentral(Integer.parseInt(properties.getProperty("contest_id")));
        if (properties.containsKey(CF_API_KEY_PROPERTY_NAME) && properties.containsKey(CF_API_SECRET_PROPERTY_NAME)) {
            central.setApiKeyAndSecret(properties.getProperty(CF_API_KEY_PROPERTY_NAME), properties.getProperty(CF_API_SECRET_PROPERTY_NAME));
        }
    }

    public static CFEventsLoader getInstance() {
        EventsLoader eventsLoader = EventsLoader.getInstance();
        if (!(eventsLoader instanceof CFEventsLoader)) {
            throw new IllegalStateException();
        }
        return (CFEventsLoader) eventsLoader;
    }

    @Override
    public void run() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            CFStandings standings = central.getStandings();
            if (standings == null) {
                return;
            }
            List<CFSubmission> submissions = standings.contest.phase == CFContest.CFContestPhase.BEFORE ? null :
                    central.getStatus();
            log.info("Data received");
            contestInfo.update(standings, submissions);
            DataBus.INSTANCE.publishContestInfo(contestInfo);
        }, 0, 5, TimeUnit.SECONDS);
        try {
            if (!scheduler.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS)) {
                log.error("Scheduler in CFEventsLoader finished by timeout");
            }
        } catch (InterruptedException e) {
            // ignored
        }
    }

    @Override
    public CFContestInfo getContestData() {
        return contestInfo;
    }
}
