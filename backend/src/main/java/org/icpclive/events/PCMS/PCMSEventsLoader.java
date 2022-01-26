package org.icpclive.events.PCMS;

import org.icpclive.Config;
import org.icpclive.DataBus;
import org.icpclive.api.ContestStatus;
import static org.icpclive.api.ContestStatus.*;
import org.icpclive.events.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.slf4j.*;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class PCMSEventsLoader extends EventsLoader {
    private static final Logger log = LoggerFactory.getLogger(PCMSEventsLoader.class);

    public void loadProblemsInfo(String problemsFile) throws IOException {
        String xml = new String(Files.readAllBytes(Paths.get(problemsFile)), StandardCharsets.UTF_8);
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
        Element problems = doc.child(0);
        contestInfo.get().problems = new ArrayList<>();
        for (Element element : problems.children()) {
            ProblemInfo problem = new ProblemInfo();
            problem.letter = element.attr("alias");
            problem.name = element.attr("name");
            problem.color = element.attr("color") == null ? Color.BLACK : Color.decode(element.attr("color"));
            contestInfo.get().problems.add(problem);
        }
    }

    TeamInfo[] initialStandings;

    public PCMSEventsLoader() throws IOException {
        properties = Config.loadProperties("events");

        emulationSpeed = Double.parseDouble(properties.getProperty("emulation.speed", "1"));

        ContestInfo.CONTEST_LENGTH = Integer.parseInt(properties.getProperty("contest.length", "" + 5 * 60 * 60 * 1000));
        ContestInfo.FREEZE_TIME = Integer.parseInt(properties.getProperty("freeze.time", "" + 4 * 60 * 60 * 1000));

        int problemsNumber = Integer.parseInt(properties.getProperty("problems.number"));
        PCMSContestInfo initial = new PCMSContestInfo(problemsNumber);
        String fn = properties.getProperty("teams.url");
        String xml = new String(Files.readAllBytes(Paths.get(fn)), StandardCharsets.UTF_8);
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
        Element participants = doc.child(0);
        int id = 0;
        for (Element participant : participants.children()) {
            String participantName = participant.attr("name");
            String alias = participant.attr("id");
            String hallId = participant.attr("hall_id");
            if (hallId.length() == 0) {
                hallId = alias;
            }
            String shortName = participant.attr("shortname");
            if (shortName == null || shortName.length() == 0) {
                int index = participantName.indexOf("(");
                shortName = participantName.substring(0, index - 1);
                index = -1;//shortName.indexOf(",");
                shortName = shortName.substring(index == -1 ? 0 : index + 2);
                if (shortName.length() >= 30) {
                    shortName = shortName.substring(0, 27) + "...";
                }
            }
            String region = participant.attr("region");
            if (region == null || region.length() == 0) {
                int index = participantName.indexOf(",");
                if (index != -1) region = participantName.substring(0, index);
            }
            String hashTag = participant.attr("hashtag");

            if (region != null || region.length() != 0) {
                PCMSContestInfo.GROUPS.add(region);
            }
            HashSet<String> groups = new HashSet<>();
            groups.add(region);
            PCMSTeamInfo team = new PCMSTeamInfo(
                    id, alias, hallId, participantName, shortName,
                    hashTag, groups, initial.getProblemsNumber(), 0);
            if (team.shortName.length() != 0) {
                initial.addTeamStandings(team);
                id++;
            }
        }
        initialStandings = initial.getStandings();
        DataBus.INSTANCE.publishContestInfo(initial);
        contestInfo.set(initial);
        loadProblemsInfo(properties.getProperty("problems.url"));
    }

    private void updateStatements() throws IOException {
        try {
            String url = properties.getProperty("url");
            String login = properties.getProperty("login");
            String password = properties.getProperty("password");

            InputStream inputStream = NetworkUtils.openAuthorizedStream(url, login, password);

            String xml = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining());
            Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
            parseAndUpdateStandings(doc);
        } catch (IOException e) {
            log.error("error", e);
        }
    }

    @Override
    public void run() {
        //log.debug(check.getName() + " " + check.getShortName());
        while (true) {
            try {
                while (true) {
                    updateStatements();
                    Thread.sleep(5000);
                }
            } catch (IOException | InterruptedException e) {
                log.error("error", e);
            }
        }
    }

    private void parseAndUpdateStandings(Element element) {
        if ("contest".equals(element.tagName())) {
            PCMSContestInfo updatedContestInfo = parseContestInfo(element);
            DataBus.INSTANCE.publishContestInfo(updatedContestInfo);
            contestInfo.set(updatedContestInfo);
        } else {
            element.children().forEach(this::parseAndUpdateStandings);
        }
    }

    private int lastRunId = 0;

    private PCMSContestInfo parseContestInfo(Element element) {
        int problemsNumber = Integer.parseInt(properties.getProperty("problems.number"));
        PCMSContestInfo updatedContestInfo = new PCMSContestInfo(problemsNumber);

        long previousStartTime = contestInfo.get().getStartTime();
        long currentTime = Long.parseLong(element.attr("time"));
        ContestStatus previousStatus = contestInfo.get().status;

        updatedContestInfo.setStatus(ContestStatus.valueOf(element.attr("status").toUpperCase()));

        switch (updatedContestInfo.status) {
            case BEFORE:
                break;
            case RUNNING:
                if (previousStatus != RUNNING || previousStartTime == 0) {
                    updatedContestInfo.setStartTime(System.currentTimeMillis() - currentTime);
                } else {
                    updatedContestInfo.setStartTime(previousStartTime);
                }
                break;
            case PAUSED:
                if (previousStatus != PAUSED) {
                    updatedContestInfo.setStartTime(previousStartTime);
                    updatedContestInfo.setStatus(RUNNING);
                    updatedContestInfo.setStatus(PAUSED);
                } else {
                    updatedContestInfo.lastTime = contestInfo.get().lastTime;
                }
                break;
        }

        updatedContestInfo.frozen = "yes".equals(element.attr("frozen"));

        updatedContestInfo.CONTEST_LENGTH = contestInfo.get().CONTEST_LENGTH;
        updatedContestInfo.FREEZE_TIME = contestInfo.get().CONTEST_LENGTH;
        updatedContestInfo.problems = contestInfo.get().problems;

        TeamInfo[] standings = contestInfo.get().getStandings();
        boolean[] taken = new boolean[standings.length];
        element.children().forEach(session -> {
            if ("session".equals(session.tagName())) {
                PCMSTeamInfo teamInfo = parseTeamInfo(session);
                if (teamInfo != null) {
                    updatedContestInfo.addTeamStandings(teamInfo);
                    taken[teamInfo.getId()] = true;
                }
            }
        });

        for (int i = 0; i < taken.length; i++) {
            if (!taken[i]) {
                updatedContestInfo.addTeamStandings((PCMSTeamInfo) initialStandings[i]);
            }
        }

        updatedContestInfo.lastRunId = lastRunId - 1;
        updatedContestInfo.fillTimeFirstSolved();
        updatedContestInfo.calculateRanks();
        updatedContestInfo.makeRuns();

        return updatedContestInfo;
    }

    private PCMSTeamInfo parseTeamInfo(Element element) {
        String alias = element.attr("alias");
        PCMSTeamInfo teamInfo = contestInfo.get().getParticipant(alias);
        if (teamInfo == null || teamInfo.getShortName().length() == 0) {
            return null;
        }
        PCMSTeamInfo teamInfoCopy = new PCMSTeamInfo(teamInfo);

        teamInfoCopy.solved = Integer.parseInt(element.attr("solved"));
        teamInfoCopy.penalty = Integer.parseInt(element.attr("penalty"));

        for (int i = 0; i < element.children().size(); i++) {
            ArrayList<PCMSRunInfo> problemRuns = parseProblemRuns(element.child(i), i, teamInfoCopy.getId());
            lastRunId = teamInfoCopy.mergeRuns(problemRuns, i, lastRunId, contestInfo.get().getCurrentTime());
        }

        return teamInfoCopy;
    }

    private ArrayList<PCMSRunInfo> parseProblemRuns(Element element, int problemId, int teamId) {
        ArrayList<PCMSRunInfo> runs = new ArrayList<>();
        if (contestInfo.get().status == BEFORE) {
            return runs;
        }
        element.children().forEach(run -> {
            PCMSRunInfo runInfo = parseRunInfo(run, problemId, teamId);
            runs.add(runInfo);
        });

        return runs;
    }

    private static final HashMap<String, String> outcomeMap = new HashMap<String, String>() {{
        put("undefined", "UD");
        put("fail", "FL");
        put("unknown", "");
        put("accepted", "AC");
        put("compilation-error", "CE");
        put("wrong-answer", "WA");
        put("presentation-error", "PE");
        put("runtime-error", "RE");
        put("time-limit-exceeded", "TL");
        put("memory-limit-exceeded", "ML");
        put("output-limit-exceeded", "OL");
        put("idleness-limit-exceeded", "IL");
        put("security-violation", "SV");
    }};

    private PCMSRunInfo parseRunInfo(Element element, int problemId, int teamId) {
        long time = Long.parseLong(element.attr("time"));
        long timestamp = (contestInfo.get().getStartTime() + time) / 1000;
        boolean isFrozen = time >= ContestInfo.FREEZE_TIME;
        boolean isJudged = !isFrozen && !"undefined".equals(element.attr("outcome"));
        String result = "yes".equals(element.attr("accepted")) ? "AC" :
                !isJudged ? "" :
                        outcomeMap.getOrDefault(element.attr("outcome"), "WA");

        return new PCMSRunInfo(isJudged, result, problemId, time, timestamp, teamId);
    }

    public PCMSContestInfo getContestData() {
        return contestInfo.get();
    }

    AtomicReference<PCMSContestInfo> contestInfo = new AtomicReference<PCMSContestInfo>();
    private Properties properties;
}
