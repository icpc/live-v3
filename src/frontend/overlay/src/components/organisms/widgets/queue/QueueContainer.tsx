import React from "react";
import styled from "styled-components";
import c from "@/config";
import { Widget } from "@shared/api";
import { LocationRectangle } from "@/utils/location-rectangle";
import { useDelayedBoolean } from "@/utils/hooks/withTimeoutAfterRender";
import { QueueHeader } from "./QueueHeader";
import { Featured, HorizontalFeatured } from "./QueueFeatured";
import { QueueRowWithTransition } from "./QueueRow";
import { useVerticalQueueRowsData } from "./hooks/useVerticalQueueData";
import { useHorizontalQueueRowsData } from "./hooks/useHorizontalQueueData";
import { useQueueLayout } from "./hooks/useQueueLayout";

type QueueWidget = Widget.QueueWidget;

const QueueWrap = styled.div<{
    hasFeatured: boolean;
    variant: "vertical" | "horizontal";
}>`
    width: 100%;
    height: 100%;
    position: absolute;
    background-color: ${({ variant }) =>
        variant === "horizontal"
            ? c.QUEUE_HORIZONTAL_BACKGROUND_COLOR
            : c.QUEUE_BACKGROUND_COLOR};
    background-repeat: no-repeat;
    border-radius: ${c.GLOBAL_BORDER_RADIUS};
    border-top-right-radius: ${(props) =>
        props.hasFeatured ? "0" : c.GLOBAL_BORDER_RADIUS};
    padding: ${c.QUEUE_WRAP_PADDING}px;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    gap: ${c.QUEUE_GAP};
`;

const RowsContainer = styled.div`
    position: relative;
    width: 100%;
    height: 100%;
    overflow: hidden;
`;

const HorizontalRowsContainer = styled.div`
    position: absolute;
    width: calc(100% - ${c.QUEUE_WRAP_PADDING * 2}px);
    height: calc(100% - ${c.QUEUE_WRAP_PADDING * 2}px);
    overflow: hidden;
`;

type QueueComponentProps = {
    shouldShow: boolean;
    widget: QueueWidget;
};

const QueueComponent = ({ shouldShow, widget }: QueueComponentProps) => {
    const horizontal = widget.settings.horizontal;
    const location = c.WIDGET_POSITIONS[
        widget.widgetLocationId
    ] as LocationRectangle;

    const width = location.sizeX - c.QUEUE_WRAP_PADDING * 2;
    const { height, headerWidth, handleHeaderRef, handleRowsContainerRef } =
        useQueueLayout(horizontal ? undefined : location.sizeY - 200);

    const [verticalFeatured, verticalQueueRows] = useVerticalQueueRowsData({
        height: height ?? 0,
        width,
    });

    const [horizontalFeatured, horizontalQueueRows] =
        useHorizontalQueueRowsData({
            height: height ?? 0,
            ftsRowWidth: width - headerWidth,
        });

    const featured = horizontal ? horizontalFeatured : verticalFeatured;
    const queueRows = horizontal ? horizontalQueueRows : verticalQueueRows;

    const FeaturedComponent = horizontal ? HorizontalFeatured : Featured;
    const RowsContainerComponent = horizontal
        ? HorizontalRowsContainer
        : RowsContainer;

    return (
        <>
            <FeaturedComponent runInfo={featured} />
            <QueueWrap
                hasFeatured={!!featured}
                variant={horizontal ? "horizontal" : "vertical"}
            >
                <QueueHeader onRef={horizontal ? handleHeaderRef : undefined} />
                <RowsContainerComponent ref={handleRowsContainerRef}>
                    {shouldShow &&
                        queueRows.map((row) => (
                            <QueueRowWithTransition
                                key={row.id}
                                row={row}
                                horizontal={horizontal}
                            />
                        ))}
                </RowsContainerComponent>
            </QueueWrap>
        </>
    );
};

type QueueProps = {
    widgetData: Widget.QueueWidget;
};

export const Queue = ({ widgetData }: QueueProps) => {
    const shouldShow = useDelayedBoolean(300);
    return <QueueComponent shouldShow={shouldShow} widget={widgetData} />;
};

Queue.shouldCrop = false;
Queue.zIndex = 1;

export default Queue;
