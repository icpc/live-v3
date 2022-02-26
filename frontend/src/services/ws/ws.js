import _ from "lodash";
import { pushLog } from "../../redux/debug";
import { handleMessage as mainScreenHandler } from "./mainScreen";
import { handleMessage as queueHandler } from "./queue";


let handler = {
    get: function(target, name) {
        if (Object.getOwnPropertyDescriptor(target, name)) {
            return target[name];
        } else {
            return (dispatch) => (e) => {
                const m = `NO HANDLER FOR WEBSOCKET ${name}. GOT EVENT ${_.truncate(e.data, { length: 100 })}`;
                console.error(m);
                dispatch(pushLog(m));
            };
        }
    }
};

export const WEBSOCKET_HANDLERS = new Proxy({
    mainScreen: mainScreenHandler,
    queue: queueHandler,
}, handler);
