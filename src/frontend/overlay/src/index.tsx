import React from "react";
import ReactDOM from "react-dom";
import { Provider } from "react-redux";
// import { PersistGate } from "redux-persist/integration/react";
import { createGlobalStyle } from "styled-components";
import App from "./App";
import "./assets/fonts/fonts.scss";

import { store } from "./redux/store";


export const GlobalStyle = createGlobalStyle`
  body {
    margin: 0;
    padding: 0;

    font-family: Helvetica, sans-serif;

    /* height: 100vh;
    width: 100vw; */

    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
  }

  * {
    scrollbar-width: none;

    -ms-overflow-style: none;
  }

  *::-webkit-scrollbar {
    display: none;
  }
`;

ReactDOM.render(
    <React.StrictMode>
        <Provider store={store}>
            {/*<PersistGate loading={null} persistor={persistor}>*/}
            <GlobalStyle/>
            <App/>
            {/*</PersistGate>*/}
        </Provider>
    </React.StrictMode>,
    document.getElementById("root")
);

