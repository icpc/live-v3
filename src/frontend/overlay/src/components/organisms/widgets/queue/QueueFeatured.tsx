import React from "react";
import { useTransition } from "react-transition-state";
import styled, { css, CSSObject } from "styled-components";
import c from "@/config";
import { TeamMediaHolder } from "@/components/organisms/holder/TeamMediaHolder";
import { QueueRow } from "./QueueRow";
import { QueueRowInfo } from "./utils/queueState";
import { slideInFromRight, slideOutToRight } from "./utils/animations";

const appearStatesFeatured = {
    entering: css`
        animation: ${slideInFromRight()}
            ${c.QUEUE_ROW_FEATURED_RUN_APPEAR_TIME}ms ease-out;
    `,
    exiting: css`
        animation: ${slideOutToRight()}
            ${c.QUEUE_ROW_FEATURED_RUN_APPEAR_TIME}ms ease-in;
    `,
    exited: css`
        opacity: 0;
    `,
};

const StyledFeatured = styled.div<{ additional: CSSObject }>`
    width: ${c.QUEUE_FEATURED_RUN_WIDTH};
    position: absolute;

    right: calc(100% - ${c.QUEUE_FEATURED_RUN_RIGHT_OFFSET});
    padding: ${c.QUEUE_FEATURED_RUN_PADDING_TOP}
        ${c.QUEUE_FEATURED_RUN_PADDING_RIGHT}
        ${c.QUEUE_FEATURED_RUN_PADDING_BOTTOM}
        ${c.QUEUE_FEATURED_RUN_PADDING_LEFT};

    background-color: ${c.QUEUE_BACKGROUND_COLOR};
    border-radius: ${c.QUEUE_FEATURED_RUN_BORDER_RADIUS_TOP_LEFT} 0 0
        ${c.QUEUE_FEATURED_RUN_BORDER_RADIUS_BOTTOM_LEFT};
    overflow: hidden;
    display: flex;
    flex-direction: column;
    gap: ${c.QUEUE_ROW_FEATURED_RUN_PADDING}px;

    ${({ additional }) => additional}
`;

const StyledHorizontalFeatured = styled.div<{ additional: CSSObject }>`
    width: ${c.QUEUE_HORIZONTAL_FEATURED_RUN_WIDTH};
    position: absolute;

    right: 0;
    bottom: 100%;
    padding: ${c.QUEUE_WRAP_PADDING}px;

    background-color: ${c.QUEUE_BACKGROUND_COLOR};
    border-radius: ${c.QUEUE_HORIZONTAL_FEATURED_RUN_BORDER_RADIUS_TOP_LEFT}
        ${c.QUEUE_HORIZONTAL_FEATURED_RUN_BORDER_RADIUS_TOP_RIGHT} 0 0;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    gap: ${c.QUEUE_ROW_FEATURED_RUN_PADDING}px;

    ${({ additional }) => additional}
`;

export const Featured = ({ runInfo }: { runInfo: QueueRowInfo | null }) => {
    const [transition, toggle] = useTransition({
        timeout: c.QUEUE_ROW_FEATURED_RUN_APPEAR_TIME,
        mountOnEnter: true,
        unmountOnExit: true,
    });

    React.useEffect(() => {
        toggle(!!runInfo);
    }, [runInfo, toggle]);

    if (!runInfo || !transition.isMounted) {
        return null;
    }

    const realState = runInfo.isFeaturedRunMediaLoaded
        ? transition.status
        : "exited";

    return (
        <StyledFeatured additional={appearStatesFeatured[realState]}>
            <TeamMediaHolder
                media={runInfo.featuredRunMedia[0]}
                onLoadStatus={runInfo.setIsFeaturedRunMediaLoaded}
            />
            <QueueRow runInfo={runInfo} />
        </StyledFeatured>
    );
};

export const HorizontalFeatured = ({
    runInfo,
}: {
    runInfo: QueueRowInfo | null;
}) => {
    const [transition, toggle] = useTransition({
        timeout: c.QUEUE_ROW_FEATURED_RUN_APPEAR_TIME,
        mountOnEnter: true,
        unmountOnExit: true,
    });

    React.useEffect(() => {
        toggle(!!runInfo);
    }, [runInfo, toggle]);

    if (!runInfo || !transition.isMounted) {
        return null;
    }

    const realState = runInfo.isFeaturedRunMediaLoaded
        ? transition.status
        : "exited";

    return (
        <StyledHorizontalFeatured additional={appearStatesFeatured[realState]}>
            <TeamMediaHolder
                media={runInfo.featuredRunMedia[0]}
                onLoadStatus={runInfo.setIsFeaturedRunMediaLoaded}
            />
            <QueueRow runInfo={runInfo} />
        </StyledHorizontalFeatured>
    );
};
