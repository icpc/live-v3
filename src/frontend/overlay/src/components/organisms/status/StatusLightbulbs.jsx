import React from "react";
import { useSelector } from "react-redux";
import styled from "styled-components";
import { WebsocketStatus } from "../../../redux/status";

const colors = {
    [WebsocketStatus.CONNECTED]: "green",
    [WebsocketStatus.CONNECTING]: "gray",
    [WebsocketStatus.DISCONNECTED]: "red",
    [undefined]: "black"
};

const compactcolors = {
    ...colors,
    [WebsocketStatus.CONNECTED]: "rgba(0,0,0,0)"
};

const Lightbulb = styled.div`
  width: ${props => props.compact ? "10px" : "30px"};
  height: ${props => props.compact ? "10px" : "30px"};
  display: inline-block;
  border-radius: 30px;
  background-color: ${props => props.color};
  vertical-align: top;
`;

const LightbulbWrap = styled.div`
    line-height: ${props => props.compact ? "10px" : "30px"};
`;

const StatusLightbulbsWrap = styled.div`
    display: ${props => props.compact ? "flex" : ""};
`;

export const StatusLightbulbs = ({ compact = false }) => {
    const status = useSelector(state => state.status.websockets);
    return <StatusLightbulbsWrap compact={compact}>
        {Object.entries(status).map(([key, value]) => {
            return <LightbulbWrap key={key} compact={compact}>
                {!compact ? key : null}
                <Lightbulb color={(compact ? compactcolors : colors)[value]} compact={compact}/>
            </LightbulbWrap>;
        })
        }
    </StatusLightbulbsWrap>;
};
