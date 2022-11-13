import _ from "lodash";
import { DateTime } from "luxon";
import React, { useEffect, useLayoutEffect, useRef } from "react";
import { useDispatch, useSelector } from "react-redux";
import { io } from "socket.io-client";
import styled from "styled-components";
import { TEAMVIEW_SMALL_FACTOR, VERDICT_NOK, VERDICT_OK, VERDICT_UNKNOWN } from "../../../config";
import { SCOREBOARD_TYPES } from "../../../consts";
import { pushLog } from "../../../redux/debug";
import { Cell } from "../../atoms/Cell";
import { ProblemCell, RankCell, TextShrinkingCell } from "../../atoms/ContestCells";
import { StarIcon } from "../../atoms/Star";
import { formatScore } from "../../atoms/ContestCells";

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

function getStatus(isFirstToSolve, isSolved, pendingAttempts, wrongAttempts) {
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
  position: relative;
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
};

export const TeamInfo = ({ teamId }) => {
    const teamData = useSelector((state) => state.contestInfo.info?.teamsId[teamId]);
    const scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal]?.ids[teamId]);
    return <TeamInfoWrapper>
        <RankCell rank={scoreboardData?.rank} width={NUMWIDTH + "px"} medal={scoreboardData?.medalType}/>
        <TextShrinkingCell text={teamData?.shortName ?? ""} width={NAMEWIDTH + "px"} canGrow={false} canShrink={false}/>
        <ScoreboardStatCell>
            {scoreboardData === null ? null : formatScore(scoreboardData?.totalScore, 1)}
        </ScoreboardStatCell>
        <ScoreboardStatCell>
            {scoreboardData?.penalty}
        </ScoreboardStatCell>

    </TeamInfoWrapper>;
};

const TeamImageWrapper = styled.img`
  position: absolute;
  width: 100%;
  top: 0;
`;

const TeamVideoWrapper = styled.video`
  position: absolute;
  width: 100%;
  top: 0;
`;


const TeamWebRTCVideoWrapper = ({ url, setIsLoaded }) => {
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
    return (<TeamVideoWrapper
        ref={videoRef}
        onError={() => setIsLoaded(false) || dispatch(pushLog("ERROR on loading image in Picture widget"))}
        onLoadedData={() => {
            // console.log("Loaded");
            // console.log(videoRef.current.currentTime);
            return setIsLoaded(true);
        }}
        autoPlay
        muted/>);
};


const TeamWebRTCSocketVideoWrapper = ({ url, peerName, credential, onLoadStatus }) => {
    const dispatch = useDispatch();
    const socketRef = useRef();
    const videoRef = useRef();
    const rtcRef = useRef();
    useEffect(() => {
        const socket = io(url, { auth: { token: credential ?? undefined } });
        socketRef.current = socket;
        socket.on("auth", (status) => {
            if (status === "forbidden") {
                dispatch(pushLog(`Webrtc content failed from ${url} peerName ${peerName}. Incorrect credential`));
                console.warn(`Webrtc content failed from ${url} peerName ${peerName}. Incorrect credential`);
            }
        });
        socket.on("init_peer", (pcConfig) => {
            rtcRef.current?.close();

            dispatch(pushLog(`Webrtc content from ${url} peerName ${peerName}`));

            const pc = new RTCPeerConnection(pcConfig);
            rtcRef.current = pc;
            pc.addTransceiver("video");

            pc.addEventListener("track", (e) => {
                console.log("WebRTCSocket got track");
                if (e.track.kind === "video" && e.streams.length > 0 && videoRef.current.srcObject !== e.streams[0]) {
                    videoRef.current.srcObject = null;
                    videoRef.current.srcObject = e.streams[0];
                    videoRef.current.play();
                    console.log("WebRTCSocket pc2 received remote stream");
                }
            });

            pc.createOffer().then(offer => {
                pc.setLocalDescription(offer);
                console.log(`WebRTCSocket send offer to [${peerName}]`);
                socketRef.current?.emit("offer_name", peerName, offer);
            });

            pc.addEventListener("icecandidate", (event) => {
                console.log(`WebRTCSocket sending ice to [${peerName}]`);
                socketRef.current?.emit("player_ice_name", peerName, event.candidate);
            });

            socketRef.current.on("offer_answer", (peerId, answer) => {
                console.log(`WebRTCSocket got offer_answer from ${peerId}`);
                pc.setRemoteDescription(answer).then(console.log);
            });

            socketRef.current.on("grabber_ice", async (_, candidate) => {
                console.log("WebRTCSocket got ice");
                await pc.addIceCandidate(candidate);
            });
        });
        return () => {
            rtcRef.current?.close();
            socketRef.current?.close();
        };
    }, [url, peerName]);

    return (<TeamVideoWrapper
        ref={videoRef}
        onLoadedData={() => onLoadStatus(true)}
        onError={() => onLoadStatus(false) || dispatch(pushLog("ERROR on loading image in WebRTC widget"))}
        muted/>);
};


const TeamVideoAnimationWrapper = styled.div`
  position: absolute;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: start;
  align-items: center;
`;


const teamViewComponentRender = {
    TaskStatus: ({ onLoadStatus, teamId, isSmall }) => {
        useLayoutEffect(() => onLoadStatus(true),
            []);
        return <ScoreboardColumn teamId={teamId} isSmall={isSmall}/>;
    },
    Photo: ({ onLoadStatus, url }) => {
        return <TeamVideoAnimationWrapper>
            <TeamImageWrapper src={url} onLoad={() => onLoadStatus(true)}/>
        </TeamVideoAnimationWrapper>;
    },
    Object: ({ onLoadStatus, url }) => {
        onLoadStatus(true);
        return <TeamVideoAnimationWrapper>
            <object data={url} type="image/svg+xml">
            </object>
        </TeamVideoAnimationWrapper>;
    },
    Video: ({ onLoadStatus, url }) => {
        return <TeamVideoAnimationWrapper>
            <TeamVideoWrapper
                src={url}
                onCanPlay={() => onLoadStatus(true)}
                onError={() => onLoadStatus(false)}
                autoPlay
                loop
                muted/>
        </TeamVideoAnimationWrapper>;
    },
    WebRTCFetchConnection: ({ onLoadStatus, url, audioUrl }) => {
        return <TeamVideoAnimationWrapper>
            {audioUrl && <audio src={audioUrl} onLoad={() => onLoadStatus(true)} autoPlay/>}
            <TeamWebRTCVideoWrapper url={url} setIsLoaded={onLoadStatus}/>
        </TeamVideoAnimationWrapper>;
    },
    WebRTCConnection: (props) => {
        return <TeamVideoAnimationWrapper>
            <TeamWebRTCSocketVideoWrapper {...props}/>
        </TeamVideoAnimationWrapper>;
    },
};

export const TeamViewHolder = ({ onLoadStatus, media, isSmall }) => {
    const Component = teamViewComponentRender[media.type];
    if (Component === undefined) {
        useEffect(() => onLoadStatus(true),
            [media.teamId]);
        return undefined;
    }
    return <Component onLoadStatus={onLoadStatus} isSmall={isSmall} {...media}/>;
};
