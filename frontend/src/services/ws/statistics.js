import { setStatistics } from "../../redux/contest/statistics";

export const handleMessage = (dispatch, e) => {
    const message = JSON.parse(e.data);
    dispatch(setStatistics(message.stats));
};
