// import { applyMiddleware, combineReducers, createStore } from "redux";
// import { composeWithDevTools } from "redux-devtools-extension";
// import { persistReducer, persistStore } from "redux-persist";
// import storage from "redux-persist/lib/storage";
// import thunkMiddleware from "redux-thunk";
import { contestInfoReducer } from "./contest/contestInfo";
import { queueReducer } from "./contest/queue";
import { scoreboardReducer } from "./contest/scoreboard";
import debugReducer from "./debug";
import statusReducer from "./status";
import { widgetsReducer } from "./widgets";
import { tickerReducer } from "./ticker";
import statisticsReducer from "./contest/statistics";
// import { getPersistConfig } from "redux-deep-persist";
import { configureStore } from "@reduxjs/toolkit";

// const persistConfig = getPersistConfig({
//     key: "root",
//     storage,
//     whitelist: ["debug.log"],
//     rootReducer: combinedReducer
// });

// const persistedReducer = persistReducer(persistConfig, combinedReducer);

export const store = configureStore({
    reducer: {
        widgets: widgetsReducer,
        status: statusReducer,
        debug: debugReducer,
        queue: queueReducer,
        scoreboard: scoreboardReducer,
        contestInfo: contestInfoReducer,
        ticker: tickerReducer,
        statistics: statisticsReducer
    },
    // middleware: getDefaultMiddleware => {
    //
    // }
    devTools: import.meta.env.DEV,
});
// export const persistor = persistStore(store);


// Infer the `RootState` and `AppDispatch` types from the store itself

export type RootState = ReturnType<typeof store.getState>

export type AppDispatch = typeof store.dispatch

