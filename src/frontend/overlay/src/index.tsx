import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { Provider } from "react-redux";
// import { PersistGate } from "redux-persist/integration/react";
import { createGlobalStyle } from "styled-components";
import App from "./App";
import "./assets/fonts/fonts.scss";

import { store } from "./redux/store";

const container = document.getElementById("root");
const root = createRoot(container);

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

root.render(
    <StrictMode>
        <Provider store={store}>
            {/*<PersistGate loading={null} persistor={persistor}>*/}
            <GlobalStyle/>
            <App/>
            {/*</PersistGate>*/}
        </Provider>
    </StrictMode>
);

