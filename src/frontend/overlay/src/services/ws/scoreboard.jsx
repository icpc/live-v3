import { setScoreboard } from "../../redux/contest/scoreboard";

export const handleMessage = (type) => (dispatch, e) => {
    const message = JSON.parse(e.data);
    dispatch(setScoreboard(type, message.rows));
};
