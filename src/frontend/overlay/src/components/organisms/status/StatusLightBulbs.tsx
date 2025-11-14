import React from "react";
import styled from "styled-components";
import { useAppSelector } from "@/redux/hooks";
import { WebsocketStatus } from "@/redux/status";

const getColor = (
    status: WebsocketStatus | undefined,
    compact: boolean,
): string => {
    if (status === undefined) {
        return "black";
    } else {
        return {
            [WebsocketStatus.CONNECTED]: compact ? "rgba(0,0,0,0)" : "green",
            [WebsocketStatus.CONNECTING]: "gray",
            [WebsocketStatus.DISCONNECTED]: "red",
        }[status];
    }
};

const Lightbulb = styled.div<{ compact: boolean }>`
    display: inline-block;

    width: ${(props) => (props.compact ? "10px" : "30px")};
    height: ${(props) => (props.compact ? "10px" : "30px")};

    vertical-align: top;

    background-color: ${(props) => props.color};
    border-radius: 30px;
`;

const LightbulbWrap = styled.div<{ compact: boolean }>`
    line-height: ${(props) => (props.compact ? "10px" : "30px")};
`;

const StatusLightBulbsWrap = styled.div<{ compact: boolean }>`
    display: ${(props) => (props.compact ? "flex" : "")};
`;

export const StatusLightBulbs = ({ compact = false }) => {
    const status = useAppSelector((state) => state.status.websockets);
    return (
        <StatusLightBulbsWrap compact={compact}>
            {Object.entries(status).map(([key, value]) => {
                return (
                    <LightbulbWrap key={key} compact={compact}>
                        {!compact ? key : null}
                        <Lightbulb
                            color={getColor(value, compact)}
                            compact={compact}
                        />
                    </LightbulbWrap>
                );
            })}
        </StatusLightBulbsWrap>
    );
};
