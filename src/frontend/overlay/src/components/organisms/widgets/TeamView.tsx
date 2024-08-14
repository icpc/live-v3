import React, { Dispatch, SetStateAction, useState } from "react";
import styled, { Keyframes, keyframes } from "styled-components";
import c from "../../../config";
import { LocationRectangle, MediaType, OverlayTeamViewSettings, TeamId, TeamViewPosition, Widget } from "@shared/api";
import { OverlayWidgetC } from "@/components/organisms/widgets/types";
import { TeamMediaHolder } from "@/components/organisms/holder/TeamMediaHolder";
import TimeLine from "@/components/organisms/holder/TimeLine";
import { ContestantViewCorner } from "@/components/molecules/info/ContestantViewCorner";
import { ContestantViewLine } from "@/components/molecules/info/ContestantViewLine";

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

const TeamViewContainer = styled.div<{ show: boolean; animation?: Keyframes; animationStyle: string }>`
    width: 100%;
    height: 100%;
    display: ${props => props.show ? "flex" : "none"};
    flex-direction: column;
    justify-content: start;
    align-items: flex-end;
    position: relative;
    animation: ${props => props.animation} ${c.TEAM_VIEW_APPEAR_TIME}ms ${props => props.animationStyle};
    animation-fill-mode: forwards;
`;

// const TeamViewPInPWrapper = styled.div`
//     width: 100%;
//     height: 100%;
//     grid-column-start: 2;
//     grid-column-end: 3;
//     grid-row-start: 2;
//     grid-row-end: 4;
//     position: relative;
//     border-radius: ${c.GLOBAL_BORDER_RADIUS};
// `;
//
// const TeamViewWrapper = styled.div<{ sizeX: number; sizeY: number }>`
//     width: 100%;
//     height: 100%;
//     display: grid;
//     justify-content: end;
//     grid-template-columns: ${({ sizeX, sizeY }) => `${sizeX - 2 * (Math.max(sizeY - sizeX * 9 / 16, 100)) * 16 / 9}px`}
//                           ${({ sizeX, sizeY }) => `${2 * (Math.max(sizeY - sizeX * 9 / 16, 100)) * 16 / 9}px`};
//     grid-template-rows: ${({ sizeX, sizeY }) => `${sizeY - 2 * Math.max(sizeY - sizeX * 9 / 16, 100)}px`}
//                       ${({ sizeX, sizeY }) => `${Math.max(sizeY - sizeX * 9 / 16, 100)}px`}
//                       ${({ sizeX, sizeY }) => `${Math.max(sizeY - sizeX * 9 / 16, 100)}px`};
// `;


// const TeamViewContestantViewHolder = styled(ContestantViewHolder)`
//     top: 0; /* # FIXME: fuck this. */
// `;

// export function TeamViewContent({ mediaContent, settings, setLoadedComponents, location, isSmall }) {
//     const hasPInP = settings.content.filter(e => !e.isMedia).concat(mediaContent).filter((c) => c.pInP).length > 0;
//
//     return <TeamViewWrapper sizeX={location.sizeX} sizeY={location.sizeY}>
//         {settings.content.filter(e => !e.isMedia).concat(mediaContent).map((c, index) => {
//             const onLoadStatus = (v) => setLoadedComponents(m => v ? (m | (1 << index)) : (m & ~(1 << index)));
//             const component = <TeamViewContestantViewHolder key={c.type + index} onLoadStatus={onLoadStatus} media={c}
//                 isSmall={isSmall} hasPInP={hasPInP}/>;
//             if (c.pInP) {
//                 return <TeamViewPInPWrapper key={c.type + index} sizeX={location.sizeX}>{component}</TeamViewPInPWrapper>;
//             }
//             return component;
//         })}
//     </TeamViewWrapper>;
// }

// export const TeamViewOld = ({ widgetData: { settings, location }, transitionState }: OverlayWidgetProps<Widget.TeamViewWidget>) => {
//     const [loadedComponents, setLoadedComponents] = useState(0);
//     const isLoaded = loadedComponents === (1 << settings.content.length) - 1;
//     const mediaContent = settings.content.filter(e => e.isMedia).map((e, index) => ({ ...e, pInP: index > 0 }));
//     const isSmall = settings.position !== "SINGLE_TOP_RIGHT";
//     const passedProps = {
//         mediaContent,
//         settings,
//         setLoadedComponents,
//         location
//     };
//     return <TeamViewContainer
//         show={isLoaded}
//         animation={isLoaded && (transitionState === "exiting" ? slideOut : slideIn)}
//         animationStyle={transitionState === "exiting" ? "ease-in" : "ease-out"}
//     >
//         {settings.position === "PVP_TOP" || settings.position === "PVP_BOTTOM" ?
//             <PVP {...passedProps}/> :
//             <TeamViewContent isSmall={isSmall} {...passedProps}/>
//         }
//     </TeamViewContainer>;
// };

type SeparateContentType = {
    taskStatusId: TeamId | null;
    achievement: MediaType | null;
    timelineId: TeamId | null;
    primary: MediaType | null;
    secondary: MediaType | null;
}

const separateContent = ({ content }: OverlayTeamViewSettings): SeparateContentType => {
    const taskStatusId = content.find(c => c.type === MediaType.Type.TaskStatus)?.teamId;
    const achievement = content.find(c => (c.type === MediaType.Type.Object || c.type === MediaType.Type.Image) && !c.isMedia);
    const timelineId = content.find(c => c.type === MediaType.Type.TimeLine)?.teamId;

    const medias = content.filter(c => c.isMedia);
    const primary = medias.length > 0 && medias[0];
    const secondary = medias.length > 1 && medias[1];

    return { taskStatusId, achievement, timelineId, primary, secondary };
};

const PrimaryMediaWrapper = styled.div`
    position: absolute;
    width: 100%;
    height: 100%;
    z-index: 1;
`;

const AchievementWrapper = styled(PrimaryMediaWrapper)`
    z-index: 2;
`;

const TeamViewGrid = styled.div<{ $achievementY: number }>`
    width: 100%;
    height: 100%;
    display: grid;
    grid-template-columns: 1fr ${props => props.$achievementY * 2 / 9 * 16}px;
    grid-template-rows: 1fr ${props => props.$achievementY}px ${props => props.$achievementY}px ${props => props.$achievementY}px;
    z-index: 3;
    border-radius: ${c.GLOBAL_BORDER_RADIUS};
`;

const SecondaryMediaWrapper = styled.div<{ withAchievement: boolean }>`
    grid-column: 2 / 3;
    grid-row-start: ${props => props.withAchievement ? 3 : 2};
    grid-row-end: ${props => props.withAchievement ? 5 : 4};
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
    if (position === TeamViewPosition.SINGLE_TOP_RIGHT || position === undefined) {
        return "single";
    } else if (position === TeamViewPosition.PVP_TOP || position === TeamViewPosition.PVP_BOTTOM) {
        return "pvp";
    }
    return "split";
};

type VariantProps = {
    location: LocationRectangle;
    position?: TeamViewPosition;
    setPrimaryLoaded: Dispatch<SetStateAction<boolean>>;
    setSecondaryLoaded: Dispatch<SetStateAction<boolean>>;
    setAchievementLoaded: Dispatch<SetStateAction<boolean>>;
} & SeparateContentType;

const SingleVariant = ({ primary, setPrimaryLoaded, secondary, setSecondaryLoaded, achievement, setAchievementLoaded, taskStatusId, timelineId, location }: VariantProps) => {
    const achievementY = location.sizeY - location.sizeX / 16 * 9;
    return (
        <>
            {primary && (
                <PrimaryMediaWrapper>
                    <TeamMediaHolder media={primary} onLoadStatus={setPrimaryLoaded} />
                </PrimaryMediaWrapper>
            )}
            {achievement && (
                <AchievementWrapper>
                    <TeamMediaHolder media={achievement} onLoadStatus={setAchievementLoaded} />
                </AchievementWrapper>
            )}
            <TeamViewGrid $achievementY={achievementY}>
                {secondary && (
                    <SecondaryMediaWrapper withAchievement={!!achievement}>
                        <TeamMediaHolder media={secondary} onLoadStatus={setSecondaryLoaded} />
                    </SecondaryMediaWrapper>
                )}
                {timelineId && (
                    <TimelineWrapper>
                        <div>
                            <TimeLine teamId={timelineId} />
                        </div>
                    </TimelineWrapper>
                )}
                {taskStatusId && (
                    <TaskStatusWrapper withAchievement={!!achievement} withSecondary={!!secondary}>
                        <ContestantViewCorner teamId={taskStatusId} isSmall={false} />
                    </TaskStatusWrapper>
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
    border-radius: ${c.GLOBAL_BORDER_RADIUS};
    overflow: hidden;
`;

const PVPAchievementInnerWrapper = styled.div`
    position: absolute;
    width: 676px;
    bottom: -4px;
`;

const PVPVariant = ({ primary, setPrimaryLoaded, secondary, setSecondaryLoaded, achievement, setAchievementLoaded, taskStatusId, location, position }: VariantProps) => {
    const isTop = position === TeamViewPosition.PVP_TOP;
    return (
        <>
            <PVPGrid
                $primaryY={location.sizeY - 0.5 * c.PVP_TABLE_ROW_HEIGHT}
                $secondaryY={(location.sizeX - (location.sizeY - 0.5 * c.PVP_TABLE_ROW_HEIGHT) / 9 * 16) / 16 * 9}
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
            </PVPGrid>
            {taskStatusId && (
                <PVPTaskStatusWrapper $isTop={isTop}>
                    <ContestantViewLine teamId={taskStatusId} isTop={isTop}/>
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

const SplitVariant = ({ primary, setPrimaryLoaded, secondary, setSecondaryLoaded, taskStatusId, location }: VariantProps) => {
    return (
        <>
            {primary && (
                <PrimaryMediaWrapper>
                    <TeamMediaHolder media={primary} onLoadStatus={setPrimaryLoaded} />
                </PrimaryMediaWrapper>
            )}
            <SplitScreenGrid $secondaryY={location.sizeY * 0.39}>
                {secondary && (
                    <SecondaryMediaWrapper withAchievement={false}>
                        <TeamMediaHolder media={secondary} onLoadStatus={setSecondaryLoaded} />
                    </SecondaryMediaWrapper>
                )}
                {taskStatusId && (
                    <TaskStatusWrapper withAchievement={false} withSecondary={!!secondary}>
                        <ContestantViewCorner teamId={taskStatusId} isSmall={false} />
                    </TaskStatusWrapper>
                )}
            </SplitScreenGrid>
        </>
    );
};

export const TeamView: OverlayWidgetC<Widget.TeamViewWidget> = ({ widgetData: { settings, location }, transitionState }) => {
    const position = settings.position;
    const variant = teamViewVariant(position);
    const { primary, secondary, achievement, taskStatusId, timelineId } = separateContent(settings);

    const [primaryLoaded, setPrimaryLoaded] = useState(false);
    const [secondaryLoaded, setSecondaryLoaded] = useState(false);
    const [achievementLoaded, setAchievementLoaded] = useState(false);
    const isLoaded = (!primary || primaryLoaded) && (!secondary || secondaryLoaded || true)
        && (variant === "single" || !achievement || achievementLoaded);

    const props = { primary, secondary, achievement, taskStatusId, timelineId, setPrimaryLoaded, setSecondaryLoaded, setAchievementLoaded, location, position };

    return (
        <TeamViewContainer
            show={isLoaded}
            animation={isLoaded && (transitionState === "exiting" ? slideOut : slideIn)}
            animationStyle={transitionState === "exiting" ? "ease-in" : "ease-out"}
        >
            {variant === "single" && <SingleVariant {...props} />}
            {variant === "pvp" && <PVPVariant {...props} />}
            {variant === "split" && <SplitVariant {...props} />}
        </TeamViewContainer>
    );
};
TeamView.ignoreAnimation = true;
TeamView.overrideTimeout = c.TEAM_VIEW_APPEAR_TIME;
