import * as React from "react";
import { Route } from "react-router-dom";
import { Controls } from "./Controls";

export default [
    <Route exact path="/controls" key="controls" component={Controls} />,
];
