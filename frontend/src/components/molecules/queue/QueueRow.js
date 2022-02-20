import React from "react";
import styled from "styled-components";
import { QUEUE_ROW_HEIGHT } from "../../../config";


const QueueRowWrap = styled.div`
  background-color: ${props => props.fts ? "red" : "blue"};
  height: ${QUEUE_ROW_HEIGHT}px;
  width: 100%;
`;

export const QueueRow = ({ bottom, entryData, teamData, ...props }) => {
    return <QueueRowWrap {...props} fts={entryData.isFirstToSolve}>{entryData.id} - {entryData.teamId} - {entryData.problemId} - {entryData.result}</QueueRowWrap>;
};

