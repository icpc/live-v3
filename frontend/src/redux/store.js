import { applyMiddleware, combineReducers, createStore } from "redux";
import { composeWithDevTools } from "redux-devtools-extension";
import { persistReducer, persistStore } from "redux-persist";
import storage from "redux-persist/lib/storage";
import thunkMiddleware from "redux-thunk";
import { debugReducer } from "./debug";
import { statusReducer } from "./status";
import { widgetsReducer } from "./widgets";

const combinedReducer = combineReducers({
    widgets: widgetsReducer,
    status: statusReducer,
    debug: debugReducer
});

const persistConfig = {
    key: "root",
    storage,
    whitelist: ["debug"],
    // blacklist: ["auth.isLoading"]
};

const persistedReducer = persistReducer(persistConfig, combinedReducer);

const bindMiddleware = (middleware) => {
    if (process.env.NODE_ENV !== "production") {
        return composeWithDevTools(applyMiddleware(...middleware));
    }
    return applyMiddleware(...middleware);
};

export const store = createStore(persistedReducer, bindMiddleware([thunkMiddleware]));
export let persistor = persistStore(store);
