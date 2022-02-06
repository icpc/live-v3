import _ from "lodash";
import { handleMessage as mainScreenHandler } from "./mainScreen";
import { handleMessage as queueHandler } from "./queue";


let handler = {
    get: function(target, name) {
        if (Object.getOwnPropertyDescriptor(target, name)) {
            return target[name];
        } else {
            return () => (e) => {
                console.error(`NO HANDLER FOR WEBSOCKET ${name}. GOT EVENT ${_.truncate(e.data, { length: 100 })}`);
            };
        }
    }
};

export const WEBSOCKET_HANDLERS = new Proxy({
    mainScreen: mainScreenHandler,
    queue: queueHandler,
}, handler);
