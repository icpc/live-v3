import _ from "lodash";
import { SCOREBOARD_TYPES } from "../../consts";
import { pushLog } from "../../redux/debug";
import { handleMessage as contestInfoHandler } from "./contestInfo";
import { handleMessage as mainScreenHandler } from "./mainScreen";
import { handleMessage as queueHandler } from "./queue";
import { handleMessage as scoreboardHandler } from "./scoreboard";
import { handleMessage as statisticsHandler } from "./statistics";
import { handleMessage as tickerHandler } from "./ticker";


const handler = {
    get: function (target, name) {
        if (Object.getOwnPropertyDescriptor(target, name)) {
            return (dispatch) => {
                return (e) => {
                    dispatch(pushLog(`${name} - ${e.data}`));
                    target[name](dispatch, e);
                };
            };
        } else {
            return (dispatch) =>
                (e) => {
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
    scoreboardOptimistic: scoreboardHandler(SCOREBOARD_TYPES.optimistic),
    scoreboardNormal: scoreboardHandler(SCOREBOARD_TYPES.normal),
    scoreboardPessimistic: scoreboardHandler(SCOREBOARD_TYPES.pessimistic),
    contestInfo: contestInfoHandler,
    ticker: tickerHandler,
    statistics: statisticsHandler,
}, handler);
