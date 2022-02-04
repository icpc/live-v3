import React from "react";
import { useSelector } from "react-redux";
import styled from "styled-components";
import { WebsocketStatus } from "../../../redux/status";

const colors = {
    [WebsocketStatus.CONNECTED]: "green",
    [WebsocketStatus.CONNECTING]: "gray",
    [WebsocketStatus.DISCONNECTED]: "red"
};

const Lightbulb = styled.div`
  width: 30px;
  height: 30px;
  border-radius: 30px;
  background-color: ${props => props.color};
`;

export const StatusLightbulb = () => {
    const status = useSelector(state => state.status.websocketStatus);
    return <Lightbulb color={colors[status]}/>;
};
