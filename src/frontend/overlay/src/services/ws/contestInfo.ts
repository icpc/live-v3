import { setInfo } from "../../redux/contest/contestInfo";
import {ContestInfo} from "@shared/api";

export const handleMessage = (dispatch, e) => {
    const message = JSON.parse(e.data) as ContestInfo;
    dispatch(setInfo(message));
};
