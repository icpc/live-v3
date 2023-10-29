import { applyMiddleware, combineReducers, createStore } from "redux";
import { composeWithDevTools } from "redux-devtools-extension";
import { persistReducer, persistStore } from "redux-persist";
import storage from "redux-persist/lib/storage";
import thunkMiddleware from "redux-thunk";
import { contestInfoReducer } from "./contest/contestInfo";
import { queueReducer } from "./contest/queue";
import { scoreboardReducer } from "./contest/scoreboard";
import { debugReducer } from "./debug";
import { statusReducer } from "./status";
import { widgetsReducer } from "./widgets";
import { tickerReducer } from "./ticker";
import { statisticsReducer } from "./contest/statistics";
import { getPersistConfig } from "redux-deep-persist";

const combinedReducer = combineReducers({
    widgets: widgetsReducer,
    status: statusReducer,
    debug: debugReducer,
    queue: queueReducer,
    scoreboard: scoreboardReducer,
    contestInfo: contestInfoReducer,
    ticker: tickerReducer,
    statistics: statisticsReducer
});

const persistConfig = getPersistConfig({
    key: "root",
    storage,
    whitelist: ["debug.log"],
    rootReducer: combinedReducer
});

const persistedReducer = persistReducer(persistConfig, combinedReducer);

const bindMiddleware = (middleware) => {
    if (process.env.NODE_ENV !== "production") {
        return composeWithDevTools(applyMiddleware(...middleware));
    }
    return applyMiddleware(...middleware);
};

export const store = createStore(persistedReducer, bindMiddleware([thunkMiddleware]));
export let persistor = persistStore(store);
