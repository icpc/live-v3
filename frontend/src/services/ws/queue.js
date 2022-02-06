import { pushLog } from "../../redux/debug";
import { addRun, removeRun } from "../../redux/queue";

export const handleMessage = (dispatch) => (e) => {
    const message = JSON.parse(e.data);
    dispatch(pushLog(JSON.stringify(message)));
    switch (message.type) {
    case "AddRunToQueue":
        dispatch(addRun(message.info));
        break;
    case "RemoveRunFromQueue":
        dispatch(removeRun(message.info.id));
        break;
    }
};
