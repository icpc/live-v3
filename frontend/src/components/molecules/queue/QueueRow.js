import React from "react";
import styled from "styled-components";
import { QUEUE_ROW_HEIGHT } from "../../../config";

export const QueueRowPlaceholder = styled.div`
    height: ${QUEUE_ROW_HEIGHT}px;
    top: ${props => props.top}
`;

const QueueRowWrap = styled(QueueRowPlaceholder)`
    
`;

export const QueueRow = ({ top, teamData }) => {
    return <QueueRowWrap>aboba</QueueRowWrap>;
};

