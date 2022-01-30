package org.icpclive.events.WF.json;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.icpclive.events.NetworkUtils;
import org.icpclive.events.TeamInfo;
import org.icpclive.events.WF.WFRunInfo;
import org.icpclive.events.WF.WFTeamInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * Created by Aksenov239 on 3/5/2018.
 */
public class WFContestInfo extends org.icpclive.events.WF.WFContestInfo {
    public WFContestInfo() {
        problemById = new HashMap<>();
        teamById = new HashMap<>();
        languageById = new HashMap<>();
        runBySubmissionId = new HashMap<>();
        runByJudgementId = new HashMap<>();
    }

    public void initializationFinish() {
        problemNumber = problems.size();
        teamNumber = teamInfos.length;
        timeFirstSolved = new long[problemNumber];
        runs = new WFRunInfo[1000000];
        firstSolvedRun = new WFRunInfo[problemNumber];
    }

    // Groups
    HashMap<String, String> groupById;

    // Problems
    HashMap<String, WFProblemInfo> problemById;

    // Teams
    HashMap<String, WFTeamInfo> teamById;

    // Languages
    WFLanguageInfo[] languages;
    HashMap<String, WFLanguageInfo> languageById;

    // Submissions
    HashMap<String, WFRunInfo> runBySubmissionId;
    HashMap<String, WFRunInfo> runByJudgementId;

    public void addRun(WFRunInfo runInfo) {
        runInfo.id = maxRunId + 1;
        runs[runInfo.id] = runInfo;
        teamInfos[runInfo.teamId].addRun(runInfo, runInfo.problemId);
        getProblemById(runInfo.problemId).submissions[runInfo.languageId]++;
        maxRunId++;
    }

    public WFTeamInfo getTeamByCDSId(String cdsId) {
        return teamById.get(cdsId);
    }

    public void checkStandings(String url, String login, String password) {
        try {
            TeamInfo[] standings = getStandings();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    NetworkUtils.openAuthorizedStream(url + "/scoreboard", login, password)
            ));
            String json = "";
            String line;
            while ((line = br.readLine()) != null) {
                json += line.trim();
            }

            JsonArray jsonTeams = new Gson().fromJson(json, JsonArray.class);
            for (int i = 0; i < jsonTeams.size(); i++) {
                JsonObject je = jsonTeams.get(i).getAsJsonObject();
                String id = je.get("team_id").getAsString();
                JsonObject score = je.get("score").getAsJsonObject();
                int num_solved = score.get("num_solved").getAsInt();
                int total_time = score.get("total_time").getAsInt();
                TeamInfo team = getTeamByCDSId(id);
                if (team.getSolvedProblemsNumber() != num_solved ||
                        team.getPenalty() != total_time) {
                    System.err.println("Incorrect for team " + team);
                    return;
                }
            }
            System.err.println("Correct scoreboard");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
