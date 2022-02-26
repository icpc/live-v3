import { pushLog } from "../../redux/debug";
import { addRun, modifyRun, removeRun, setFromSnapshot } from "../../redux/queue";

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
    case "ModifyRunInQueue":
        dispatch(modifyRun(message.info));
        break;
    case "QueueSnapshot":
        dispatch(setFromSnapshot(message.infos));
        break;
    default:
        dispatch(pushLog(`UNKNOWN MESSAGE TYPE: ${message.type}`));
        break;
    }
};
