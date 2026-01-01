import React, {
    useRef,
    useMemo,
    useEffect,
    useState,
    startTransition,
} from "react";
import styled from "styled-components";
import c from "@/config";
import { useResizeObserver } from "usehooks-ts";
import { ScoreboardSettings, Widget } from "@shared/api";
import { OverlayWidgetC } from "@/components/organisms/widgets/types";
import { ScoreboardHeader, ScoreboardTableHeader } from "./ScoreboardHeader";
import { AnimatedRow } from "./ScoreboardRow";
import {
    useScoreboardRows,
    useScoreboardData,
} from "./hooks/useScoreboardData";
import { useScroller, useAnimatedScrollPos } from "./hooks/useScoreboardScroll";
import { useAnimatingTeams } from "./hooks/useScoreboardAnimation";

const ScoreboardWrap = styled.div`
    overflow: hidden;
    display: flex;
    flex-direction: column;
    gap: ${c.SCOREBOARD_GAP};

    box-sizing: border-box;
    width: 100%;
    height: 100%;
    padding: ${c.SCOREBOARD_PADDING_TOP} ${c.SCOREBOARD_PADDING_RIGHT} 0
        ${c.SCOREBOARD_PADDING_LEFT};

    color: ${c.SCOREBOARD_TEXT_COLOR};

    background-color: ${c.SCOREBOARD_BACKGROUND_COLOR};
    border-radius: ${c.SCOREBOARD_BORDER_RADIUS};
`;

const ScoreboardContent = styled.div`
    display: flex;
    flex: 1 0 0;
    flex-direction: column;
    gap: ${c.SCOREBOARD_BETWEEN_HEADER_PADDING}px;
`;

const ScoreboardRowsWrap = styled.div<{ maxHeight: number }>`
    position: relative;

    overflow: hidden;
    flex: 1 0 0;

    height: auto;
    max-height: ${({ maxHeight }) => `${maxHeight}px`};
`;

interface ScoreboardRowsProps {
    settings: ScoreboardSettings;
    onPage: number;
}

const ScoreboardRows = ({ settings, onPage }: ScoreboardRowsProps) => {
    const rows = useScoreboardRows(settings.optimismLevel, settings.group);
    const rowHeight = c.SCOREBOARD_ROW_HEIGHT + c.SCOREBOARD_ROW_PADDING;

    const targetScrollPos = useScroller(
        rows.length,
        onPage,
        c.SCOREBOARD_SCROLL_INTERVAL,
        settings.scrollDirection,
    );

    const { getScrollPos, subscribe } = useAnimatedScrollPos(targetScrollPos);
    const { scoreboardData, normalScoreboardData, contestData } =
        useScoreboardData(settings.optimismLevel);

    const animatingTeams = useAnimatingTeams(rows);

    const [prevWindow, setPrevWindow] = useState({
        scrollPos: targetScrollPos,
        onPage,
    });

    const visibleTeams = useMemo(() => {
        const effectiveOnPage = Math.max(1, onPage);
        const effectivePrevOnPage = Math.max(1, prevWindow.onPage);

        const currentMin = targetScrollPos;
        const currentMax = targetScrollPos + effectiveOnPage;

        const prevMin = prevWindow.scrollPos;
        const prevMax = prevWindow.scrollPos + effectivePrevOnPage;

        const visible = new Set<string>();

        for (const [teamId, position] of rows) {
            if (position >= currentMin && position <= currentMax) {
                visible.add(teamId);
            }
            if (position >= prevMin && position <= prevMax) {
                visible.add(teamId);
            }
        }

        for (const [teamId, info] of animatingTeams) {
            const trajMin = Math.min(info.fromPos, info.toPos);
            const trajMax = Math.max(info.fromPos, info.toPos);

            if (trajMax >= currentMin && trajMin <= currentMax) {
                visible.add(teamId);
            }
        }

        return visible;
    }, [rows, targetScrollPos, onPage, animatingTeams, prevWindow]);

    useEffect(() => {
        const timeout = setTimeout(() => {
            startTransition(() =>
                setPrevWindow({ scrollPos: targetScrollPos, onPage }),
            );
        }, c.SCOREBOARD_ROW_TRANSITION_TIME);
        return () => clearTimeout(timeout);
    }, [targetScrollPos, onPage]);

    const teamsToRender = useMemo(() => {
        return rows.filter(([teamId]) => visibleTeams.has(teamId));
    }, [rows, visibleTeams]);

    const effectiveOnPage = Math.max(1, onPage);

    return (
        <ScoreboardRowsWrap maxHeight={effectiveOnPage * rowHeight}>
            {teamsToRender.map(([teamId, position]) => (
                <AnimatedRow
                    key={teamId}
                    teamId={teamId}
                    targetPos={position}
                    animatingInfo={animatingTeams.get(teamId)}
                    rowHeight={rowHeight}
                    getScrollPos={getScrollPos}
                    subscribeScroll={subscribe}
                    zIndex={rows.length - position}
                    scoreboardRow={scoreboardData?.ids[teamId]}
                    rank={normalScoreboardData?.rankById[teamId]}
                    awards={scoreboardData?.idAwards[teamId]}
                    contestData={contestData}
                />
            ))}
        </ScoreboardRowsWrap>
    );
};

export const Scoreboard: OverlayWidgetC<Widget.ScoreboardWidget> = ({
    widgetData: { settings },
}) => {
    const ref = useRef<HTMLDivElement>(null);
    const { height = 0 } = useResizeObserver({ ref });
    const onPage = Math.floor(
        (height - c.SCOREBOARD_HEADER_HEIGHT) /
            (c.SCOREBOARD_ROW_HEIGHT + c.SCOREBOARD_ROW_PADDING),
    );

    return (
        <ScoreboardWrap>
            <ScoreboardHeader optimismLevel={settings.optimismLevel} />
            <ScoreboardContent ref={ref}>
                <ScoreboardTableHeader />
                <ScoreboardRows settings={settings} onPage={onPage} />
            </ScoreboardContent>
        </ScoreboardWrap>
    );
};

export default Scoreboard;
