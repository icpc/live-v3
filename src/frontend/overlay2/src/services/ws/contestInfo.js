import { setInfo } from "../../redux/contest/contestInfo";

export const handleMessage = (dispatch, e) => {
    const message = JSON.parse(e.data);
    dispatch(setInfo(message));
};
