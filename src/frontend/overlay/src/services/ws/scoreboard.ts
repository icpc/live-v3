import { setScoreboard } from "../../redux/contest/scoreboard";
import { LegacyScoreboard } from "@shared/api";

export const handleMessage = (type) => (dispatch, e) => {
    const message = JSON.parse(e.data) as LegacyScoreboard;
    dispatch(setScoreboard(type, message.rows));
};
