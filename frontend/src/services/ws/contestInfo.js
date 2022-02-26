import { setInfo } from "../../redux/contest/contestInfo";
import { pushLog } from "../../redux/debug";

export const handleMessage = (dispatch) => (e) => {
    const message = JSON.parse(e.data);
    dispatch(pushLog(JSON.stringify(message)));
    dispatch(setInfo(message));
};
