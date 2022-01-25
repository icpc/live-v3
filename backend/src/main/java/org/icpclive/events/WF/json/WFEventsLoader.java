package org.icpclive.events.WF.json;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.icpclive.Config;
import org.icpclive.events.ContestInfo;
import org.icpclive.events.EventsLoader;
import org.icpclive.events.NetworkUtils;
import org.icpclive.events.WF.WFOrganizationInfo;
import org.icpclive.events.WF.WFRunInfo;
import org.icpclive.events.WF.WFTestCaseInfo;
import org.slf4j.*;

import java.awt.*;
import java.io.*;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Created by aksenov on 16.04.2015.
 */
public class WFEventsLoader extends EventsLoader {
    private static final Logger log = LoggerFactory.getLogger(WFEventsLoader.class);
    public static final Object GLOBAL_LOCK = new Object();

    private static volatile WFContestInfo contestInfo;

    private String url;
    private String login;
    private String password;
    private boolean regionals;

    private boolean emulation;

    public WFEventsLoader(boolean regionals) {
        try {
            Properties properties = Config.loadProperties("events");

            login = properties.getProperty("login");
            password = properties.getProperty("password");

            NetworkUtils.prepareNetwork(login, password);

            // in format https://example.com/api/contests/wf14/
            url = properties.getProperty("url");
            emulationSpeed = Double.parseDouble(properties.getProperty("emulation.speed", "1"));
            emulationStartTime = Long.parseLong(properties.getProperty("emulation.startTime", "0"));

            if (!(url.startsWith("http") || url.startsWith("https"))) {
                emulation = true;
            } else {
                emulationSpeed = 1;
            }

            this.regionals = regionals;
            contestInfo = initialize();
        } catch (IOException e) {
            log.error("error", e);
        }
    }

    public ContestInfo getContestData() {
        return contestInfo;
    }

    public String readJsonArray(String url) throws IOException {
        BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        NetworkUtils.openAuthorizedStream(url, login, password)));
        String json = "";
        String line;
        while ((line = br.readLine()) != null) {
            json += line.trim();
        }
        return json;
    }

    private void readGroupsInfo(WFContestInfo contest) throws IOException {
        JsonArray jsonGroups = new Gson().fromJson(
                readJsonArray(url + "/groups"), JsonArray.class);
        contest.groupById = new HashMap<>();
        for (int i = 0; i < jsonGroups.size(); i++) {
            JsonObject je = jsonGroups.get(i).getAsJsonObject();
            String id = je.get("id").getAsString();
            String name = je.get("name").getAsString();
            contest.groupById.put(id, name);
            ContestInfo.GROUPS.add(name);
        }
    }

    private void readProblemInfos(WFContestInfo contest) throws IOException {
        JsonArray jsonProblems = new Gson().fromJson(
                readJsonArray(url + "/problems"), JsonArray.class);
        contest.problems = new ArrayList<>();
        contest.problemById = new HashMap<>();
        contest.problemById = new HashMap<>();
        WFProblemInfo[] problems = new WFProblemInfo[jsonProblems.size()];
        for (int i = 0; i < jsonProblems.size(); i++) {
            JsonObject je = jsonProblems.get(i).getAsJsonObject();

            WFProblemInfo problemInfo = new WFProblemInfo(contest.languages.length);
            String cdsId = je.get("id").getAsString();
            problemInfo.name = je.get("name").getAsString();
            problemInfo.id = je.get("ordinal").getAsInt();
            problemInfo.color = Color.decode(je.get("rgb").getAsString());
            if (je.get("test_data_count") == null) {
                // TODO
                problemInfo.testCount = 100;
            } else {
                problemInfo.testCount = je.get("test_data_count").getAsInt();
            }
            problemInfo.letter = je.get("label").getAsString();
            problems[i] = problemInfo;
            contest.problemById.put(cdsId, problemInfo);
        }

        Arrays.sort(problems, (WFProblemInfo a, WFProblemInfo b) -> a.id - b.id);
        for (int i = 0; i < problems.length; i++) {
            problems[i].id = i;
            contest.problems.add(problems[i]);
        }
    }

    private static int compareAsNumbers(String a, String b) {
        for (int i = 0; i < Math.min(a.length(), b.length()); i++) {
            if (a.charAt(i) != b.charAt(i)) {
                boolean aDigit = Character.isDigit(a.charAt(i));
                boolean bDigit = Character.isDigit(b.charAt(i));
                if (!aDigit) {
                    if (!bDigit) {
                        return Character.compare(a.charAt(i), b.charAt(i));
                    } else {
                        if (i > 0 && Character.isDigit(a.charAt(i - 1))) {
                            return -1;
                        }
                        return Character.compare(a.charAt(i), b.charAt(i));
                    }
                } else {
                    if (!bDigit) {
                        if (i > 0 && Character.isDigit(a.charAt(i - 1))) {
                            return 1;
                        }
                        return Character.compare(a.charAt(i), b.charAt(i));
                    } else {
                        int aTo = i + 1;
                        while (aTo < a.length() && Character.isDigit(a.charAt(aTo))) {
                            aTo++;
                        }
                        int bTo = i + 1;
                        while (bTo < b.length() && Character.isDigit(b.charAt(bTo))) {
                            bTo++;
                        }
                        if (aTo != bTo) {
                            return Integer.compare(aTo, bTo);
                        }
                        return new BigInteger(a.substring(i, aTo)).compareTo(new BigInteger(b.substring(i, bTo)));
                    }
                }
            }
        }
        return Integer.compare(a.length(), b.length());
    }

    static class Organization {
        private String formalName;
        private String shortName;
        private String hashTag;
        private String id;

        public Organization(String formalName, String shortName, String hashTag, String id) {
            this.formalName = formalName;
            this.shortName = shortName;
            this.hashTag = hashTag;
            this.id = id;
        }
    }

    private void readTeamInfosWF(WFContestInfo contest) throws IOException {
        JsonArray jsonOrganizations = new Gson().fromJson(
                readJsonArray(url + "/organizations"), JsonArray.class);
        HashMap<String, Organization> organizations = new HashMap<>();
        for (int i = 0; i < jsonOrganizations.size(); i++) {
            JsonObject je = jsonOrganizations.get(i).getAsJsonObject();
            // TODO
            String name = je.get("formal_name").getAsString();
            String shortName = je.get("name").getAsString();
            String hashTag = je.get("twitter_hashtag") == null ?
                    null : je.get("twitter_hashtag").getAsString();
            String id = je.get("id").getAsString();
            organizations.put(id, new Organization(name, shortName, hashTag, id));
        }

        JsonArray jsonTeams = new Gson().fromJson(
                readJsonArray(url + "/teams"), JsonArray.class);
        contest.teamById = new HashMap<>();
        contest.teamInfos = new org.icpclive.events.WF.WFTeamInfo[jsonTeams.size()];
        for (int i = 0; i < jsonTeams.size(); i++) {
            JsonObject je = jsonTeams.get(i).getAsJsonObject();
            if (je.get("organization_id").isJsonNull()) {
                continue;
            }
            Organization teamOrg = organizations.get(je.get("organization_id").getAsString());
            WFTeamInfo teamInfo = new WFTeamInfo(contest.problems.size());
            teamInfo.shortName = teamOrg.shortName;
            teamInfo.name = teamOrg.formalName;
            teamInfo.hashTag = teamOrg.hashTag;

            JsonArray groups = je.get("group_ids").getAsJsonArray();
            for (int j = 0; j < groups.size(); j++) {
                String groupId = groups.get(j).getAsString();
                String group = contest.groupById.get(groupId);
                teamInfo.groups.add(group);
            }

            if (je.get("desktop") != null) {
                JsonArray hrefs = je.get("desktop").getAsJsonArray();
                teamInfo.screens = new String[hrefs.size()];
                for (int j = 0; j < hrefs.size(); j++) {
                    teamInfo.screens[j] = hrefs.get(j).getAsJsonObject().get("href").getAsString();
                }
            }

            if (je.get("webcam") != null) {
                JsonArray hrefs = je.get("webcam").getAsJsonArray();
                teamInfo.cameras = new String[hrefs.size()];
                for (int j = 0; j < hrefs.size(); j++) {
                    teamInfo.cameras[j] = hrefs.get(j).getAsJsonObject().get("href").getAsString();
                }
            }

            teamInfo.cdsId = je.get("id").getAsString();
            contest.teamById.put(teamInfo.cdsId, teamInfo);
            contest.teamInfos[i] = teamInfo;
        }
        Arrays.sort(contest.teamInfos, (a, b) -> compareAsNumbers(((WFTeamInfo) a).cdsId, ((WFTeamInfo) b).cdsId));

        for (int i = 0; i < contest.teamInfos.length; i++) {
            contest.teamInfos[i].id = i;
        }
    }

    private void readTeamInfosRegionals(WFContestInfo contest) throws IOException {
        JsonArray jsonOrganizations = new Gson().fromJson(
                readJsonArray(url + "/organizations"), JsonArray.class);
        HashMap<String, WFOrganizationInfo> organizations = new HashMap<>();
        for (int i = 0; i < jsonOrganizations.size(); i++) {
            JsonObject je = jsonOrganizations.get(i).getAsJsonObject();
            WFOrganizationInfo organizationInfo = new WFOrganizationInfo();
            // TODO
            organizationInfo.formalName = je.get("formal_name").getAsString();
            organizationInfo.name = je.get("name").getAsString();
            organizations.put(je.get("id").getAsString(), organizationInfo);
        }

        JsonArray jsonTeams = new Gson().fromJson(
                readJsonArray(url + "/teams"), JsonArray.class);
        contest.teamInfos = new org.icpclive.events.WF.WFTeamInfo[jsonTeams.size()];
        contest.teamById = new HashMap<>();
        for (int i = 0; i < jsonTeams.size(); i++) {
            JsonObject je = jsonTeams.get(i).getAsJsonObject();
            if (je.get("organization_id").isJsonNull()) {
                continue;
            }
            WFTeamInfo teamInfo = new WFTeamInfo(contest.problems.size());

            WFOrganizationInfo organizationInfo = organizations.get(je.get("organization_id").getAsString());

            teamInfo.name = organizationInfo.name + ": " + je.get("name").getAsString();
            teamInfo.shortName = shortName(teamInfo.name);

            JsonArray groups = je.get("group_ids").getAsJsonArray();
            for (int j = 0; j < groups.size(); j++) {
                String groupId = groups.get(j).getAsString();
                String group = contest.groupById.get(groupId);
                teamInfo.groups.add(group);
            }

            if (je.get("desktop") != null) {
                JsonArray hrefs = je.get("desktop").getAsJsonArray();
                teamInfo.screens = new String[hrefs.size()];
                for (int j = 0; j < hrefs.size(); j++) {
                    teamInfo.screens[j] = hrefs.get(j).getAsJsonObject().get("href").getAsString();
                }
            }

            if (je.get("webcam") != null) {
                JsonArray hrefs = je.get("webcam").getAsJsonArray();
                teamInfo.cameras = new String[hrefs.size()];
                for (int j = 0; j < hrefs.size(); j++) {
                    teamInfo.cameras[j] = hrefs.get(j).getAsJsonObject().get("href").getAsString();
                }
            }

            teamInfo.cdsId = je.get("id").getAsString();
            contest.teamById.put(teamInfo.cdsId, teamInfo);
            contest.teamInfos[i] = teamInfo;
        }
        Arrays.sort(contest.teamInfos, (a, b) -> compareAsNumbers(((WFTeamInfo) a).cdsId, ((WFTeamInfo) b).cdsId));

        for (int i = 0; i < contest.teamInfos.length; i++) {
            contest.teamInfos[i].id = i;
        }
    }

    public void readLanguagesInfos(WFContestInfo contestInfo) throws IOException {
        JsonArray jsonLanguages = new Gson().fromJson(
                readJsonArray(url + "/languages"), JsonArray.class);
        contestInfo.languages = new WFLanguageInfo[jsonLanguages.size()];
        contestInfo.languageById = new HashMap<>();
        for (int i = 0; i < jsonLanguages.size(); i++) {
            JsonObject je = jsonLanguages.get(i).getAsJsonObject();
            WFLanguageInfo languageInfo = new WFLanguageInfo();
            String cdsId = je.get("id").getAsString();
            languageInfo.name = je.get("name").getAsString();
            contestInfo.languages[i] = languageInfo;
            contestInfo.languageById.put(cdsId, languageInfo);
        }
    }

    private WFContestInfo initialize() throws IOException {
        WFContestInfo contestInfo = new WFContestInfo();
        readGroupsInfo(contestInfo);
        System.err.println("Groups");
        readLanguagesInfos(contestInfo);
        System.err.println("lanugage");
        readProblemInfos(contestInfo);
        System.err.println("problem");
        if (regionals) {
            readTeamInfosRegionals(contestInfo);
        } else {
            readTeamInfosWF(contestInfo);
        }
        contestInfo.initializationFinish();
        log.info("Problems " + contestInfo.problems.size() + ", teamInfos " + contestInfo.teamInfos.length);

        contestInfo.recalcStandings();
        return contestInfo;
    }

    public void reinitialize() throws IOException {
        WFContestInfo contestInfo = new WFContestInfo();
        readGroupsInfo(contestInfo);
        readLanguagesInfos(contestInfo);
        readProblemInfos(contestInfo);
        readTeamInfosRegionals(contestInfo);
        contestInfo.initializationFinish();

        contestInfo.setStatus(ContestInfo.Status.RUNNING);
        contestInfo.setStartTime(this.contestInfo.getStartTime());

        contestInfo.recalcStandings();
        this.contestInfo = contestInfo;
    }

    public long parseTime(String time) {
        ZonedDateTime zdt = ZonedDateTime.parse(time + ":00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
//        ZonedDateTime zdt = ZonedDateTime.parse(time, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return zdt.toInstant().toEpochMilli();
//        LocalDateTime ldt = LocalDateTime.parse(time + ":00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
//        return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public long parseRelativeTime(String time) {
        String[] z = time.split("\\.");
        String[] t = z[0].split(":");
        int h = Integer.parseInt(t[0]);
        int m = Integer.parseInt(t[1]);
        int s = Integer.parseInt(t[2]);
        int ms = z.length == 1 ? 0 : Integer.parseInt(z[1]);
        return ((h * 60 + m) * 60 + s) * 1000 + ms;
    }

    public void readContest(WFContestInfo contestInfo, JsonObject je) {
        JsonElement startTimeElement = je.get("start_time");
        if (!startTimeElement.isJsonNull()) {
            contestInfo.setStartTime(parseTime(startTimeElement.getAsString()));
            contestInfo.setStatus(ContestInfo.Status.RUNNING);
        } else {
            contestInfo.setStatus(ContestInfo.Status.BEFORE);
        }
        if (emulation) {
            contestInfo.setStartTime(System.currentTimeMillis());
        }
        WFContestInfo.CONTEST_LENGTH =
                (int) parseRelativeTime(je.get("duration").getAsString());
        WFContestInfo.FREEZE_TIME = WFContestInfo.CONTEST_LENGTH -
                (int) parseRelativeTime(je.get("scoreboard_freeze_duration").getAsString());
    }

    public void readState(WFContestInfo contestInfo, JsonObject je) {
        if (je.get("started").isJsonNull()) {
            return;
        }
        String startTime = je.get("started").getAsString();
        contestInfo.setStartTime(parseTime(startTime));
        if (emulation) {
            contestInfo.setStartTime(System.currentTimeMillis());
        }
        if (je.get("ended").isJsonNull()) {
            contestInfo.setStatus(ContestInfo.Status.RUNNING);
        } else {
            contestInfo.setStatus(ContestInfo.Status.OVER);
        }
    }

    boolean firstRun = true;

    public void waitForEmulation(long time) {
        if (emulation) {
            try {
//                if (firstRun) {
//                    contestInfo.setStartTime((long) (contestInfo.getStartTime() - emulationStartTime * 60000 / emulationSpeed));
//                    firstRun = false;
//                }
                long dt = (long) ((time - contestInfo.getCurrentTime()) / emulationSpeed);
                //System.err.println("wait for " + dt + " ms");
                if (dt > 0) Thread.sleep(dt);
            } catch (InterruptedException e) {
                log.error("error", e);
            }
        }
    }

    public void readSubmission(WFContestInfo contestInfo, JsonObject je, boolean update) {
        waitForEmulation(parseRelativeTime(je.get("contest_time").getAsString()));
        if (update) {
            return;
        }
        WFRunInfo run = new WFRunInfo();

        String cdsId = je.get("id").getAsString();

        WFLanguageInfo languageInfo = contestInfo.languageById.get(je.get("language_id").getAsString());
        run.languageId = languageInfo.id;

        WFProblemInfo problemInfo = contestInfo.problemById.get(je.get("problem_id").getAsString());
        run.problemId = problemInfo.id;

        WFTeamInfo teamInfo = (WFTeamInfo) contestInfo.teamById.get(je.get("team_id").getAsString());
        run.teamId = teamInfo.id;
        run.team = teamInfo;

        run.time = parseRelativeTime(je.get("contest_time").getAsString());

        run.setLastUpdateTime(run.time);

        contestInfo.addRun(run);

        contestInfo.runBySubmissionId.put(cdsId, run);
    }

    public void readJudgement(WFContestInfo contestInfo, JsonObject je) {
        String cdsId = je.get("id").getAsString();

        WFRunInfo runInfo = contestInfo.runBySubmissionId.get(je.get("submission_id").getAsString());

        if (runInfo == null) {
            System.err.println("FAIL! " + je);
            return;
        }

        contestInfo.runByJudgementId.put(cdsId, runInfo);


        JsonElement verdictElement = je.get("judgement_type_id");
        String verdict = verdictElement.isJsonNull() ? "" : verdictElement.getAsString();

        log.info("Judging " + contestInfo.getParticipant(runInfo.getTeamId()) + " " + verdict);

        if (verdictElement.isJsonNull()) {
            runInfo.judged = false;
            runInfo.result = "";
            waitForEmulation(parseRelativeTime(je.get("start_contest_time").getAsString()));
            return;
        }

        long time = je.get("end_contest_time").isJsonNull() ? 0 :
                parseRelativeTime(je.get("end_contest_time").getAsString());
        waitForEmulation(time);

        if (runInfo.time <= ContestInfo.FREEZE_TIME) {
            runInfo.result = verdict;
            runInfo.judged = true;

//            long start = System.currentTimeMillis();
            contestInfo.recalcStandings();
//            contestInfo.checkStandings(url, login, password);
//            log.info("Standing are recalculated in " + (System.currentTimeMillis() - start) + " ms");
        } else {
            runInfo.judged = false;
        }

        runInfo.setLastUpdateTime(time);
    }

    public void readRun(WFContestInfo contestInfo, JsonObject je, boolean update) {
        if (je.get("judgement_id").isJsonNull()) {
            System.err.println(je);
            return;
        }

        WFRunInfo runInfo = contestInfo.runByJudgementId.get(je.get("judgement_id").getAsString());

        long time = parseRelativeTime(je.get("contest_time").getAsString());

        waitForEmulation(time);

        if (runInfo == null || runInfo.time > ContestInfo.FREEZE_TIME || update) {
            return;
        }

        WFTestCaseInfo testCaseInfo = new WFTestCaseInfo();
        testCaseInfo.id = je.get("ordinal").getAsInt();
        testCaseInfo.result = je.get("judgement_type_id").getAsString();
        testCaseInfo.time = time;
        testCaseInfo.timestamp = parseTime(je.get("time").getAsString());
        testCaseInfo.runId = runInfo.id;
        testCaseInfo.total = contestInfo.getProblemById(runInfo.problemId).testCount;

//        System.err.println(runInfo);
        contestInfo.addTest(testCaseInfo);
    }

    public void run() {
        String lastEvent = null;
        boolean initialized = false;
        while (true) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(NetworkUtils.openAuthorizedStream(this.url + "/event-feed", login, password),
                            "utf-8"))) {
                String abortedEvent = lastEvent;
                lastEvent = null;

                WFContestInfo contestInfo = initialize();
                if (abortedEvent == null) {
                    WFEventsLoader.contestInfo = contestInfo;
                }
                System.err.println("Aborted event " + abortedEvent);

                while (true) {
                    String line = br.readLine();
                    if (line == null) {
                        break;
                    }

//                    System.err.println(line);
                    JsonObject je = new Gson().fromJson(line, JsonObject.class);
                    if (je == null) {
                        log.info("Non-json line");
                        System.err.println("Non-json line: " + Arrays.toString(line.toCharArray()));
                        continue;
                    }
                    String id = je.get("id").getAsString().substring(3);

                    if (id.equals(abortedEvent)) {
                        WFEventsLoader.contestInfo = contestInfo;
                    }
                    lastEvent = id;
                    boolean update = !je.get("op").getAsString().equals("create");
                    String type = je.get("type").getAsString();
                    JsonObject json = je.get("data").getAsJsonObject();

                    synchronized (GLOBAL_LOCK) {
                        switch (type) {
                            case "contests":
                                readContest(contestInfo, json);
                                break;
                            case "state":
                                readState(contestInfo, json);
                                break;
                            case "submissions":
                                readSubmission(contestInfo, json, update);
                                break;
                            case "judgements":
                                readJudgement(contestInfo, json);
                                break;
                            case "runs":
                                readRun(contestInfo, json, update);
                                break;
                            case "problems":
                                if (!update && !initialized) {
                                    initialized = true;
                                    throw new Exception("Problems weren't loaded, exception to restart feed");
                                }
                            default:
                        }
                    }
                }
                return;
            } catch (Throwable e) {
                log.error("error", e);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e1) {
                    log.error("error", e1);
                }
                log.info("Restart event feed");
                System.err.println("Restarting feed");
            }
        }
    }

    // public static ArrayBlockingQueue<RunInfo> getAllRuns() {

    static Map<String, String> shortNames = new HashMap<>();

    static {
        try {
            Properties properties = new Properties();
            properties.load(WFEventsLoader.class.getClassLoader().getResourceAsStream("events.properties"));

            File override = new File(properties.getProperty("teamInfos.shortnames.override", "override.txt"));
            if (override.exists()) {
                BufferedReader in = new BufferedReader(new FileReader("override.txt"));
                String line;
                while ((line = in.readLine()) != null) {
                    String[] ss = line.split("\t");
                    shortNames.put(ss[0], ss[1]);
                }
            }
        } catch (Exception e) {
            log.error("error", e);
        }
    }

    static String shortName(String name) {
        assert shortNames.get(name) == null;
        if (shortNames.containsKey(name)) {
            return shortNames.get(name);
        } else if (name.length() > 22) {
            return name.substring(0, 19) + "...";
        } else {
            return name;
        }
    }
}
