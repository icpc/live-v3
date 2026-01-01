import React from "react";
import styled from "styled-components";
import c from "@/config";

const QueueHeaderWrap = styled.div`
    font-size: ${c.QUEUE_HEADER_FONT_SIZE};
    font-weight: ${c.GLOBAL_DEFAULT_FONT_WEIGHT_BOLD};
    font-family: ${c.GLOBAL_DEFAULT_FONT_FAMILY};
    line-height: ${c.QUEUE_HEADER_LINE_HEIGHT};
    color: white;
    width: fit-content;
    max-width: 100%;
    display: flex;
`;

const Title = styled.div`
    flex: 1 0 0;
`;

const Caption = styled.div``;

interface QueueHeaderProps {
    onRef?: (el: HTMLElement | null) => void;
}

export const QueueHeader = ({ onRef }: QueueHeaderProps) => {
    return (
        <QueueHeaderWrap ref={onRef}>
            <Title>{c.QUEUE_TITLE}</Title>
            <Caption>{c.QUEUE_CAPTION}</Caption>
        </QueueHeaderWrap>
    );
};
