package org.icpclive.events.WF.json;

/**
 * Created by Meepo on  4/1/2018.
 */
public class WFTeamInfo extends org.icpclive.events.WF.WFTeamInfo {
    public String cdsId;

    // videos url
    public String photo;
    public String video;
    public String[] screens;
    public String[] cameras;

    public WFTeamInfo(int problems) {
        super(problems);
    }

    public String getAlias() {
        return cdsId;
    }

    public boolean isTemplated(String type) {
        switch (type) {
            case "screen":
                return false;
            case "camera":
                return false;
        }
        return true;
    }

    public String getUrlByType(String type) {
        switch (type) {
            case "screen":
                return screens[0];
            case "camera":
                return cameras[0];
            case "video":
                return cdsId;
            default:
                return "";
        }
    }

    public String getUrlByType(String type, int id) {
        switch (type) {
            case "screen":
                return screens[id % screens.length];
            case "camera":
                return cameras[id % cameras.length];
            case "video":
                return cdsId;
            default:
                return "";
        }
    }

    public String toString() {
        return cdsId + ". " + shortName;
    }
}
