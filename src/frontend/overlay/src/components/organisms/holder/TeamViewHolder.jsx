import _ from "lodash";
import { DateTime } from "luxon";
import React, { useEffect, useLayoutEffect, useRef } from "react";
import { useDispatch, useSelector } from "react-redux";
import styled from "styled-components";
import {
    CELL_QUEUE_VERDICT_WIDTH,
    TEAMVIEW_SMALL_FACTOR,
    VERDICT_NOK,
    VERDICT_OK,
    VERDICT_UNKNOWN
} from "../../../config";
import { SCOREBOARD_TYPES } from "../../../consts";
import { pushLog } from "../../../redux/debug";
import { Cell } from "../../atoms/Cell";
import { ProblemCell, RankCell, TextShrinkingCell } from "../../atoms/ContestCells";
import { StarIcon } from "../../atoms/Star";
import { formatScore } from "../../atoms/ContestCells";
import { ScoreboardIOITaskCell } from "../widgets/Scoreboard";
import { GrabberPlayerClient } from "../../../utils/grabber/grabber_player";

const NUMWIDTH = 80;
const NAMEWIDTH = 300;
const STATWIDTH = 80;

const ScoreboardCell = styled(Cell)`
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0;
  position: relative;
`;

const ScoreboardStatCell = styled(ScoreboardCell)`
  width: ${STATWIDTH}px;
`;


const TeamInfoWrapper = styled.div`
  display: flex;
  position: relative;
  height: 100%;
`;

const ScoreboardTaskCellWrap = styled(ScoreboardCell)`
  flex-grow: 1;
  flex-shrink: 1;
  flex-basis: 100%;
  height: 100%;
  padding: 5px;
  min-width: 40px;
`;

const TeamTaskStatus = Object.freeze({
    solved: 1,
    failed: 2,
    untouched: 3,
    unknown: 4,
    first: 5
});

const TeamTaskColor = Object.freeze({
    [TeamTaskStatus.solved]: VERDICT_OK,
    [TeamTaskStatus.failed]: VERDICT_NOK,
    [TeamTaskStatus.untouched]: undefined,
    [TeamTaskStatus.unknown]: VERDICT_UNKNOWN,
    [TeamTaskStatus.first]: VERDICT_OK,
});

const TeamTaskSymbol = Object.freeze({
    [TeamTaskStatus.solved]: "+",
    [TeamTaskStatus.failed]: "-",
    [TeamTaskStatus.untouched]: "",
    [TeamTaskStatus.unknown]: "?",
    [TeamTaskStatus.first]: "+",
});

const ScoreboardTaskCell = ({ status, attempts }) => {
    return <ScoreboardTaskCellWrap background={TeamTaskColor[status]}>
        {status === TeamTaskStatus.first && <StarIcon/>}
        {TeamTaskSymbol[status]}
        {status !== TeamTaskStatus.untouched && attempts > 0 && attempts}
    </ScoreboardTaskCellWrap>;
};

const StatisticsProblemCell = styled(ProblemCell)`
  padding: 0 10px;
  width: 50px;
  box-sizing: border-box;
`;

const ScoreboardTimeCell = styled(ScoreboardCell)`
  flex-grow: 1;
  flex-shrink: 1;
  flex-basis: 100%;
  height: 100%;
  padding: 5px;
  padding-left: 10px;
  min-width: 40px;
`;

export function getStatus(isFirstToSolve, isSolved, pendingAttempts, wrongAttempts) {
    if (isFirstToSolve) {
        return TeamTaskStatus.first;
    } else if (isSolved) {
        return TeamTaskStatus.solved;
    } else if (pendingAttempts > 0) {
        return TeamTaskStatus.unknown;
    } else if (wrongAttempts > 0) {
        return TeamTaskStatus.failed;
    } else {
        return TeamTaskStatus.untouched;
    }
}

// opacity: ${TEAM_VIEW_OPACITY};
const ScoreboardColumnWrapper = styled.div`
  display: grid;
  grid-template-columns: repeat(2, auto);
  grid-auto-rows: 1fr;
  position: absolute;
  transform-origin: top right;
  transform: ${props => props.isSmall ? `scale(${TEAMVIEW_SMALL_FACTOR})` : ""};
  white-space: nowrap;
`;
const ScoreboardTeamInfoRow = styled.div`
  grid-column-start: 1;
  grid-column-end: 3;
`;
const TaskRow = styled.div`
  display: flex;
  width: 100%;
  grid-column-start: 2;
  grid-column-end: 3;
`;


const ScoreboardColumn = ({ teamId, isSmall }) => {
    let scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal]?.ids[teamId]);
    for (let i = 0; i < scoreboardData?.problemResults.length; i++) {
        scoreboardData.problemResults[i]["index"] = i;
    }
    const tasks = useSelector(state => state.contestInfo?.info?.problems);
    const contestData = useSelector((state) => state.contestInfo.info);
    if (contestData === undefined) {
        return null;
    }

    if (!contestData) {
        return null;
    } else if (contestData.resultType === "ICPC") {
        return <ScoreboardColumnWrapper isSmall={isSmall}>
            <ScoreboardTeamInfoRow>
                <TeamInfo teamId={teamId}/>
            </ScoreboardTeamInfoRow>
            {_.sortBy(scoreboardData?.problemResults, "lastSubmitTimeMs").flatMap(({
                wrongAttempts,
                pendingAttempts,
                isSolved,
                isFirstToSolve,
                lastSubmitTimeMs,
                index
            }, i) =>
                getStatus(isFirstToSolve, isSolved, pendingAttempts, wrongAttempts) === TeamTaskStatus.untouched ? null :
                    <TaskRow key={i}>
                        <ScoreboardTimeCell>{DateTime.fromMillis(lastSubmitTimeMs).toFormat("H:mm")}</ScoreboardTimeCell>
                        <StatisticsProblemCell probData={tasks[index]}/>
                        <ScoreboardTaskCell status={getStatus(isFirstToSolve, isSolved, pendingAttempts, wrongAttempts)}
                            attempts={wrongAttempts + pendingAttempts}/>
                    </TaskRow>
            )}
        </ScoreboardColumnWrapper>;
    } else {
        return <ScoreboardColumnWrapper isSmall={isSmall}>
            <ScoreboardTeamInfoRow>
                <TeamInfo teamId={teamId}/>
            </ScoreboardTeamInfoRow>
            {_.sortBy(scoreboardData?.problemResults, "lastSubmitTimeMs").flatMap(({
                score,
                lastSubmitTimeMs,
                index
            }, i) =>
                (score === undefined || lastSubmitTimeMs === undefined) ? null :
                    <TaskRow key={i}>
                        <ScoreboardTimeCell>{DateTime.fromMillis(lastSubmitTimeMs).toFormat("H:mm")}</ScoreboardTimeCell>
                        <StatisticsProblemCell probData={tasks[index]}/>
                        <ScoreboardIOITaskCell width={CELL_QUEUE_VERDICT_WIDTH} score={score}  minScore={contestData?.problems[index]?.minScore} maxScore={contestData?.problems[index]?.maxScore}/>
                    </TaskRow>
            )}
        </ScoreboardColumnWrapper>;
    }
};

export const TeamInfo = ({ teamId }) => {
    const contestInfo = useSelector((state) => state.contestInfo.info);
    const teamData = useSelector((state) => state.contestInfo.info?.teamsId[teamId]);
    const scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal]?.ids[teamId]);
    return <TeamInfoWrapper>
        <RankCell rank={scoreboardData?.rank} width={NUMWIDTH + "px"} medal={scoreboardData?.medalType}/>
        <TextShrinkingCell text={teamData?.shortName ?? ""} width={NAMEWIDTH + "px"} canGrow={false} canShrink={false}/>
        <ScoreboardStatCell>
            {scoreboardData === null ? null : formatScore(scoreboardData?.totalScore, 1)}
        </ScoreboardStatCell>
        {contestInfo?.resultType !== "IOI" &&
        <ScoreboardStatCell>
            {scoreboardData?.penalty}
        </ScoreboardStatCell>}

    </TeamInfoWrapper>;
};

export const TeamImageWrapper = styled.img`
  border-radius: ${({ borderRadius }) => borderRadius};
`;

export const TeamVideoWrapper = styled.video`
  position: absolute;
  width: 100%;
  height: 100%;
  bottom: 0;
  aspect-ratio: 16/9;
  object-fit: cover;
  object-position: bottom;
  border-radius: ${({ borderRadius }) => borderRadius};
`;


export const TeamWebRTCProxyVideoWrapper = ({ Wrapper = TeamVideoWrapper, url, setIsLoaded, ...props }) => {
    const dispatch = useDispatch();
    const videoRef = useRef();
    const rtcRef = useRef();
    useEffect(() => {
        setIsLoaded(false);
        rtcRef.current = new RTCPeerConnection();
        rtcRef.current.ontrack = function (event) {
            if (event.track.kind !== "video") {
                return;
            }
            videoRef.current.srcObject = event.streams[0];
            videoRef.current.play();
        };
        rtcRef.current.addTransceiver("video");
        rtcRef.current.addTransceiver("audio");
        rtcRef.current.createOffer()
            .then(offer => {
                rtcRef.current.setLocalDescription(offer);
                return fetch(url, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(offer),
                });
            })
            .then(res => res.json())
            .then(res => rtcRef.current.setRemoteDescription(res))
            .catch(e => console.trace("ERROR featching  webrtc peer connection info: " + e));

        return () => rtcRef.current?.close();
    }, [url]);
    return (<Wrapper
        ref={videoRef}
        onError={() => setIsLoaded(false) || dispatch(pushLog("ERROR on loading image in Picture widget"))}
        onLoadedData={() => {
            // console.log("Loaded");
            // console.log(videoRef.current.currentTime);
            return setIsLoaded(true);
        }}
        autoPlay
        muted
        {...props}
    />);
};


export const TeamWebRTCGrabberVideoWrapper = ({ Wrapper = TeamVideoWrapper, url, peerName, streamType, credential, onLoadStatus, ...props }) => {
    const dispatch = useDispatch();
    const videoRef = useRef();
    useEffect(() => {
        const client = new GrabberPlayerClient("play", url);
        client.authorize(credential);
        client.on("initialized", () => {
            console.log(`Connecting to grabber peer ${peerName} for stream ${streamType}`);
            client.connect({ peerName: peerName }, streamType, (track) => {
                videoRef.current.srcObject = null;
                videoRef.current.srcObject = track;
                videoRef.current.play();
                console.log(`WebRTCSocket pc2 received remote stream (${peerName}, ${streamType})`);
            });
        });
        client.on("auth:failed", () => {
            dispatch(pushLog(`Webrtc content failed from ${url} peerName ${peerName}. Incorrect credential`));
            console.warn(`Webrtc content failed from ${url} peerName ${peerName}. Incorrect credential`);
        });

        return () => {
            client.close();
            if (videoRef.current) {
                videoRef.current.srcObject = null;
            }
        };
    }, [url, peerName, streamType]);

    return (<Wrapper
        ref={videoRef}
        onLoadedData={() => onLoadStatus(true)}
        onError={() => onLoadStatus(false) || dispatch(pushLog("ERROR on loading image in WebRTC widget"))}
        muted
        {...props}/>);
};


export const FullWidthWrapper = styled.div`
  width: 100%;

  // this is how you make aspect ratio before aspect-ratio. 
  // Do not remove until the whole world starts using modern VMix
  // Sadly this hack will cut off the bottom of the picture
  // But since all we show here is 16/9 images - it's ok.
  // Have to deal with it.
  padding-bottom: 56.25%;
  height: 0;
  overflow: hidden;
  box-sizing: border-box;
`;

export const TeamVideoAnimationWrapperWithFixForOldBrowsers = styled.div`
  position: relative;
  width: 100%;
  display: flex;
  justify-content: start;
  align-items: center;
  padding-bottom: 56.25%;
  height: 0;
  overflow: hidden;
`;


const teamViewComponentRender = {
    TaskStatus: ({ onLoadStatus, teamId, isSmall, ...props }) => {
        useLayoutEffect(() => onLoadStatus(true),
            []);
        return <ScoreboardColumn teamId={teamId} isSmall={isSmall} {...props}/>;
    },
    Photo: ({ onLoadStatus, url, className }) => {
        return <FullWidthWrapper className={className}>
            <TeamImageWrapper src={url} onLoad={() => onLoadStatus(true)}/>
        </FullWidthWrapper>;
    },
    Object: ({ onLoadStatus, url }) => {
        onLoadStatus(true);
        return <FullWidthWrapper>
            <object data={url} type="image/svg+xml">
            </object>
        </FullWidthWrapper>;
    },
    Video: ({ onLoadStatus, url, ...props }) => {
        return <FullWidthWrapper>
            <TeamVideoWrapper
                src={url}
                onCanPlay={() => onLoadStatus(true)}
                onError={() => onLoadStatus(false)}
                autoPlay
                loop
                muted
                {...props}
            />
        </FullWidthWrapper>;
    },
    WebRTCProxyConnection: ({ onLoadStatus, url, audioUrl, ...props }) => {
        return <FullWidthWrapper>
            {audioUrl && <audio src={audioUrl} onLoadedData={() => onLoadStatus(true)} autoPlay/>}
            <TeamWebRTCProxyVideoWrapper url={url} setIsLoaded={onLoadStatus} {...props}/>
        </FullWidthWrapper>;
    },
    WebRTCGrabberConnection: (props) => {
        return <TeamVideoAnimationWrapperWithFixForOldBrowsers>
            <TeamWebRTCGrabberVideoWrapper {...props}/>
        </TeamVideoAnimationWrapperWithFixForOldBrowsers>;
    },
};

export const TeamViewHolder = ({ onLoadStatus, media, isSmall, className }) => {
    const Component = teamViewComponentRender[media.type];
    if (Component === undefined) {
        useEffect(() => onLoadStatus(true),
            [media.teamId]);
        return undefined;
    }
    return <Component onLoadStatus={onLoadStatus} isSmall={isSmall} className={className} {...media}/>;
};
