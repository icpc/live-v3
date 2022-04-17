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

const Lightbulb = styled.div`
  width: 30px;
  height: 30px;
  display: inline-block;
  border-radius: 30px;
  background-color: ${props => props.color};
`;

const LightbulbWrap = styled.div`
    
`;

const StatusLightbulbsWrap = styled.div`
    
`;

export const StatusLightbulbs = () => {
    const status = useSelector(state => state.status.websockets);
    return <StatusLightbulbsWrap>
        {Object.entries(status).map(([key, value]) => {
            return <LightbulbWrap key={key}>
                {key}
                <Lightbulb color={colors[value]}/>
            </LightbulbWrap>;
        })
        }
    </StatusLightbulbsWrap>;
};
