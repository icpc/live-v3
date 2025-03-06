import React, { Dispatch, SetStateAction, useEffect, useLayoutEffect, useState } from "react";
import styled, { Keyframes, keyframes } from "styled-components";
import c from "../../../config";
import { OverlayTeamViewSettings, TeamViewPosition, Widget } from "@shared/api";
import { OverlayWidgetC, OverlayWidgetProps } from "@/components/organisms/widgets/types";
import { TeamMediaHolder } from "@/components/organisms/holder/TeamMediaHolder";
import TimeLine from "@/components/organisms/holder/TimeLine";
import { ContestantViewCorner } from "@/components/molecules/info/ContestantViewCorner";
import { ContestantViewLine } from "@/components/molecules/info/ContestantViewLine";
import { LocationRectangle } from "@/utils/location-rectangle";

const slideIn = keyframes`
    from {
        opacity: 0;
    }
    to {
        opacity: 1;
    }
`;

const slideOut = keyframes`
    from {
        opacity: 1;
    }
    to {
        opacity: 0;
    }
`;

const RoundedTeamMediaHolder = styled(TeamMediaHolder)`
    border-radius: ${c.GLOBAL_BORDER_RADIUS};
`;

const TeamViewContainer = styled.div.attrs({
    className: "TeamViewContainer",
})<{ show: boolean; animation?: Keyframes; animationStyle: string }>`
    width: 100%;
    height: 100%;
    display: ${props => props.show ? "flex" : "none"};
    flex-direction: column;
    justify-content: start;
    align-items: flex-end;
    position: absolute;
    animation: ${props => props.animation} ${c.TEAM_VIEW_APPEAR_TIME}ms ${props => props.animationStyle};
    animation-fill-mode: forwards;
`;

type CommonContentProps = {
    location: LocationRectangle;
    position?: TeamViewPosition;
    setPrimaryLoaded: Dispatch<SetStateAction<boolean>>;
    setSecondaryLoaded: Dispatch<SetStateAction<boolean>>;
    setAchievementLoaded: Dispatch<SetStateAction<boolean>>;
} & OverlayTeamViewSettings;

const PrimaryMediaWrapper = styled.div`
    position: absolute;
    width: 100%;
    height: 100%;
    z-index: 1;
    overflow: hidden;
`;

const AchievementWrapper = styled(PrimaryMediaWrapper)`
    z-index: 2;
`;

const TeamViewGrid = styled.div<{ $secondaryY: number; $achievementY: number }>`
    width: 100%;
    height: 100%;
    display: grid;
    grid-template-columns: 1fr ${props => props.$secondaryY * 2 / 9 * 16}px;
    grid-template-rows: 1fr ${props => props.$secondaryY}px ${props => props.$secondaryY}px ${props => props.$achievementY}px;
    z-index: 3;
    border-radius: ${c.GLOBAL_BORDER_RADIUS};
`;

const SecondaryMediaWrapper = styled.div<{ withAchievement: boolean }>`
    grid-column: 2 / 3;
    grid-row-start: ${props => props.withAchievement ? 3 : 2};
    grid-row-end: ${props => props.withAchievement ? 5 : 4};
    overflow: hidden;
`;

const TaskStatusWrapper = styled.div<{ withAchievement: boolean; withSecondary: boolean }>`
    grid-column: 2 / 3;
    grid-row-start: 1;
    grid-row-end: ${props => props.withSecondary ? (props.withAchievement ? 3 : 2) : 4};
    display: grid;
    align-items: end;
    overflow: hidden;
    border-radius: ${c.GLOBAL_BORDER_RADIUS};
`;

const TimelineWrapper = styled.div`
    grid-column: 1 / 2;
    grid-row: 3 / 4;
    display: grid;
    align-items: end;
`;

const teamViewVariant = (position: TeamViewPosition | undefined) => {
    if (position === TeamViewPosition.SINGLE || position === undefined) {
        return "single";
    } else if (position === TeamViewPosition.PVP_TOP || position === TeamViewPosition.PVP_BOTTOM) {
        return "pvp";
    }
    return "split";
};

const SingleContent = ({ teamId, primary, setPrimaryLoaded, secondary, setSecondaryLoaded, achievement, setAchievementLoaded, showTaskStatus, showTimeLine, location }: CommonContentProps) => {
    const achievementY = location.sizeY - location.sizeX / 16 * 9;
    const secondaryY = achievementY > 0 ? achievementY : (location.sizeY * c.TEAMVIEW_FULLSCREEN_SECONDARY_FACTOR / 2);
    return (
        <>
            {primary && (
                <PrimaryMediaWrapper>
                    <RoundedTeamMediaHolder media={primary} onLoadStatus={setPrimaryLoaded} />
                </PrimaryMediaWrapper>
            )}
            {achievement && (
                <AchievementWrapper>
                    <TeamMediaHolder media={achievement} onLoadStatus={setAchievementLoaded} />
                </AchievementWrapper>
            )}
            <TeamViewGrid $secondaryY={secondaryY} $achievementY={achievementY > 0 ? achievementY : 0}>
                {secondary && (
                    <SecondaryMediaWrapper withAchievement={!!achievement}>
                        <RoundedTeamMediaHolder media={secondary} onLoadStatus={setSecondaryLoaded} />
                    </SecondaryMediaWrapper>
                )}
                {showTaskStatus && (
                    <TaskStatusWrapper withAchievement={!!achievement} withSecondary={!!secondary}>
                        <ContestantViewCorner teamId={teamId} isSmall={false} />
                    </TaskStatusWrapper>
                )}
                {showTimeLine && (
                    <TimelineWrapper>
                        <div><TimeLine teamId={teamId} /></div>
                    </TimelineWrapper>
                )}
            </TeamViewGrid>
        </>
    );
};

type PVPWrapperProps = { $isTop: boolean };

const PVPGrid = styled.div<{ $primaryY: number; $secondaryY: number} & PVPWrapperProps>`
    width: 100%;
    height: 100%;
    display: grid;
    grid-template-columns: ${props => props.$primaryY / 9 * 16}px ${props => props.$secondaryY / 9 * 16}px;
    grid-template-rows: ${props => props.$isTop
        ? `${props.$secondaryY}px 1fr ${c.PVP_TABLE_ROW_HEIGHT * 2.5}px ${c.PVP_TABLE_ROW_HEIGHT * 0.5}px`
        : `${c.PVP_TABLE_ROW_HEIGHT * 0.5}px ${c.PVP_TABLE_ROW_HEIGHT * 2.5}px 1fr ${props.$secondaryY}px`};
    z-index: 1;
    border-radius: ${c.GLOBAL_BORDER_RADIUS};
    overflow: hidden;
`;

const PVPPrimaryMediaWrapper = styled.div<PVPWrapperProps>`
    grid-column: 1 / 2;
    grid-row: ${props => props.$isTop ? "1 / 4" : "2 / 5"};
`;

const PVPSecondaryMediaWrapper = styled.div<PVPWrapperProps>`
    grid-column: 2;
    grid-row: ${props => props.$isTop ? "1 / 2" : "4 / 5"};
`;

const PVPTaskStatusWrapper = styled.div<PVPWrapperProps>`
    position: absolute;
    width: 100%;
    height: ${c.PVP_TABLE_ROW_HEIGHT * 3 + "px"};
    top: ${props => props.$isTop ? "auto" : 0};
    bottom: ${props => !props.$isTop ? "auto" : 0};
    z-index: 2;
    overflow: hidden;
    display: grid;
    justify-items: end;
`;

const PVPAchievementWrapper = styled.div<PVPWrapperProps>`
    grid-column: 2;
    grid-row: ${props => props.$isTop ? "2 / 3" : "3 / 4"};
    display: grid;
    position: relative;
    background: ${c.PVP_BACKGROUND};
    overflow: hidden;
`;

const PVPTackStatusBackWrapper = styled.div<PVPWrapperProps>`
    grid-column: 2;
    grid-row: ${props => props.$isTop ? "3 / 4" : "2 / 3"};
    background: ${c.PVP_BACKGROUND};
`;

const PVPAchievementInnerWrapper = styled.div`
    position: absolute;
    width: 676px;
    bottom: -4px;
`;

const PVPContent = ({ teamId, primary, setPrimaryLoaded, secondary, setSecondaryLoaded, achievement, setAchievementLoaded, showTaskStatus, location, position }: CommonContentProps) => {
    const isTop = position === TeamViewPosition.PVP_TOP;
    const primaryY = location.sizeY - 0.5 * c.PVP_TABLE_ROW_HEIGHT;
    const secondaryY = (location.sizeX - (location.sizeY - 0.5 * c.PVP_TABLE_ROW_HEIGHT) / 9 * 16) / 16 * 9;
    return (
        <>
            <PVPGrid
                $primaryY={primaryY}
                $secondaryY={secondaryY}
                $isTop={isTop}
            >
                {primary && (
                    <PVPPrimaryMediaWrapper $isTop={isTop}>
                        <TeamMediaHolder media={primary} onLoadStatus={setPrimaryLoaded} />
                    </PVPPrimaryMediaWrapper>
                )}
                {secondary && (
                    <PVPSecondaryMediaWrapper $isTop={isTop}>
                        <TeamMediaHolder media={secondary} onLoadStatus={setSecondaryLoaded} />
                    </PVPSecondaryMediaWrapper>
                )}
                {achievement && (
                    <PVPAchievementWrapper $isTop={isTop}>
                        <PVPAchievementInnerWrapper>
                            <TeamMediaHolder media={achievement} onLoadStatus={setAchievementLoaded} />
                        </PVPAchievementInnerWrapper>
                    </PVPAchievementWrapper>
                )}
                <PVPTackStatusBackWrapper $isTop={isTop}/>
            </PVPGrid>
            {showTaskStatus && (
                <PVPTaskStatusWrapper $isTop={isTop}>
                    <ContestantViewLine teamId={teamId} isTop={isTop} tasksContainerY={secondaryY / 9 * 16}/>
                </PVPTaskStatusWrapper>
            )}
        </>
    );
};

const SplitScreenGrid = styled.div<{ $secondaryY: number }>`
    width: 100%;
    height: 100%;
    display: grid;
    grid-template-columns: 1fr ${props => props.$secondaryY / 9 * 16}px;
    grid-template-rows: 1fr ${props => props.$secondaryY / 2}px  ${props => props.$secondaryY / 2}px;
    z-index: 3;
    border-radius: ${c.GLOBAL_BORDER_RADIUS};
`;

const SplitContent = ({ teamId, primary, setPrimaryLoaded, secondary, setSecondaryLoaded, showTaskStatus, location }: CommonContentProps) => {
    return (
        <>
            {primary && (
                <PrimaryMediaWrapper>
                    <RoundedTeamMediaHolder media={primary} onLoadStatus={setPrimaryLoaded} />
                </PrimaryMediaWrapper>
            )}
            <SplitScreenGrid $secondaryY={location.sizeY * c.SPLITSCREEN_SECONDARY_FACTOR}>
                {secondary && (
                    <SecondaryMediaWrapper withAchievement={false}>
                        <RoundedTeamMediaHolder media={secondary} onLoadStatus={setSecondaryLoaded} />
                    </SecondaryMediaWrapper>
                )}
                {showTaskStatus && (
                    <TaskStatusWrapper withAchievement={false} withSecondary={!!secondary}>
                        <ContestantViewCorner teamId={teamId} isSmall={false} />
                    </TaskStatusWrapper>
                )}
            </SplitScreenGrid>
        </>
    );
};

export const TeamView: OverlayWidgetC<Widget.TeamViewWidget> = ({ widgetData: { settings, ...restProps }, transitionState }) => {
    const [curSettings, setCurSettings] = useState<OverlayTeamViewSettings>(settings);
    const [nextSettings, setNextSettings] = useState<OverlayTeamViewSettings>(null);

    useEffect(() => {
        if (settings.teamId != curSettings.teamId) {
            setNextSettings(settings);
        }
    }, [settings]);

    const onNextLoaded = () => {
        setTimeout(() => {
            setCurSettings(nextSettings);
            setNextSettings(null);
        }, c.TEAM_VIEW_APPEAR_TIME);
    };

    return <>
        {/** Current */}
        <TeamViewSingleContent key={curSettings?.teamId} widgetData={{ settings: curSettings, ...restProps }} transitionState={transitionState}/> 
        {/** Next */}
        {nextSettings && <TeamViewSingleContent key={nextSettings?.teamId} widgetData={{ settings: nextSettings, ...restProps }} transitionState={transitionState} onLoaded={onNextLoaded}/> }
    </>;
};

type TeamViewSingleContentProps = OverlayWidgetProps<Widget.TeamViewWidget> & {
    onLoaded?: () => void
}

export const TeamViewSingleContent = ({ widgetData: { settings, widgetLocationId }, transitionState, onLoaded }: TeamViewSingleContentProps) => {
    const { primary, secondary, achievement, position } = settings;
    const location = c.WIDGET_POSITIONS[widgetLocationId];
    const variant = teamViewVariant(position);

    const [primaryLoaded, setPrimaryLoaded] = useState(false);
    const [secondaryLoaded, setSecondaryLoaded] = useState(false);
    const [achievementLoaded, setAchievementLoaded] = useState(false);
    const isLoaded = (!primary || primaryLoaded) && (!secondary || secondaryLoaded || true)
        && (variant === "single" || !achievement || achievementLoaded);

    useLayoutEffect(() => {
        if (isLoaded && onLoaded) {
            onLoaded();
        }
    }, [isLoaded]);

    const props = { ...settings, setPrimaryLoaded, setSecondaryLoaded, setAchievementLoaded, location };

    return (
        <TeamViewContainer
            show={isLoaded}
            animation={isLoaded && (transitionState === "exiting" ? slideOut : slideIn)}
            animationStyle={transitionState === "exiting" ? "ease-in" : "ease-out"}
            data-teamid={settings?.teamId}
        >
            {variant === "single" && <SingleContent {...props} />}
            {variant === "pvp" && <PVPContent {...props} />}
            {variant === "split" && <SplitContent {...props} />}
        </TeamViewContainer>
    );
};
TeamView.ignoreAnimation = true;
TeamView.overrideTimeout = c.TEAM_VIEW_APPEAR_TIME;
