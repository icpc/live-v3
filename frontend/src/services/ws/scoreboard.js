import { setScoreboard } from "../../redux/contest/scoreboard";
import { pushLog } from "../../redux/debug";

export const handleMessage = (type) => (dispatch) => (e) => {
    const message = JSON.parse(e.data);
    dispatch(pushLog(JSON.stringify(message)));
    dispatch(setScoreboard(type, message.rows));
};
