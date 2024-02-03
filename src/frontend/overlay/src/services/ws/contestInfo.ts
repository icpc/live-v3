import { setInfo } from "@/redux/contest/contestInfo";
import { ContestInfo } from "@shared/api";
import { AppDispatch } from "@/redux/store";

export const handleMessage = (dispatch: AppDispatch, e) => {
    const message = JSON.parse(e.data) as ContestInfo;
    dispatch(setInfo(message));
};
