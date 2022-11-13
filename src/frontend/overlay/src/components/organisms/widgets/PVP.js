import React, { useEffect, useLayoutEffect, useRef } from "react";
import { useDispatch, useSelector } from "react-redux";
import { io } from "socket.io-client";
import styled from "styled-components";
import { PVP_APPEAR_TIME, STATISTICS_BG_COLOR, VERDICT_NOK, VERDICT_OK, VERDICT_UNKNOWN } from "../../../config";
import { SCOREBOARD_TYPES } from "../../../consts";
import { pushLog } from "../../../redux/debug";
import { Cell } from "../../atoms/Cell";
import { RankCell, TextShrinkingCell } from "../../atoms/ContestCells";
import { StarIcon } from "../../atoms/Star";
import { formatScore } from "../../atoms/ContestCells";

// Burn this.
// - Max

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
  grid-row: 1;
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
    [TeamTaskStatus.untouched]: STATISTICS_BG_COLOR,
    [TeamTaskStatus.unknown]: VERDICT_UNKNOWN,
    [TeamTaskStatus.first]: VERDICT_OK,
});

const StatisticsProblemCellWithColor = ({ probData, status }) => {
    return <ScoreboardTaskCellWrap background={TeamTaskColor[status]}>
        {status === TeamTaskStatus.first && <StarIcon/>}
        {probData?.letter ?? "??"}
    </ScoreboardTaskCellWrap>;
};


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

const ScoreboardRowAllWrapper = styled.div`
  display: grid;
  justify-content: start;
  grid-template-rows: 41.5px 41.5px;
  position: relative;
`;

const TaskRowWrapperFirst = styled.div`
  grid-row: 2 / 3;
  display: grid;
  justify-content: start;
  grid-template-rows: 1fr;

  grid-auto-flow: column;
  position: relative;
`;

const ScoreboardTeamInfoRowFirst = styled.div`
  grid-row: 1 / 2;
`;

const TaskRowWrapperSecond = styled.div`
  grid-row: 1 / 2;
  display: grid;
  justify-content: start;
  grid-template-rows: 1fr;

  grid-auto-flow: column;
  position: relative;
`;

const ScoreboardTeamInfoRowSecond = styled.div`
  grid-row: 2 / 3;
`;
const TaskRow = styled.div`
  display: flex;
  flex-direction: column;
`;

const ScoreboardRowAllTaskFirst = ({ teamId }) => {
    let scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal]?.ids[teamId]);
    console.log(scoreboardData);
    for (let i = 0; i < scoreboardData?.problemResults.length; i++) {
        scoreboardData.problemResults[i]["index"] = i;
    }
    const tasks = useSelector(state => state.contestInfo?.info?.problems);
    return <ScoreboardRowAllWrapper>
        <ScoreboardTeamInfoRowFirst>
            <TeamInfo teamId={teamId}/>
        </ScoreboardTeamInfoRowFirst>
        <TaskRowWrapperFirst>
            {scoreboardData?.problemResults.flatMap(({
                wrongAttempts,
                pendingAttempts,
                isSolved,
                isFirstToSolve,
                index
            }, i) =>
                <TaskRow key={i}>
                    <StatisticsProblemCellWithColor probData={tasks[index]}
                        status={getStatus(isFirstToSolve, isSolved, pendingAttempts, wrongAttempts)}/>
                </TaskRow>
            )}
        </TaskRowWrapperFirst>
    </ScoreboardRowAllWrapper>;
};

const ScoreboardRowAllTaskSecond = ({ teamId }) => {
    let scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal]?.ids[teamId]);
    //console.log(scoreboardData);
    for (let i = 0; i < scoreboardData?.problemResults.length; i++) {
        scoreboardData.problemResults[i]["index"] = i;
    }
    const tasks = useSelector(state => state.contestInfo?.info?.problems);
    return <ScoreboardRowAllWrapper>
        <TaskRowWrapperSecond>
            {scoreboardData?.problemResults.flatMap(({
                wrongAttempts,
                pendingAttempts,
                isSolved,
                isFirstToSolve,
                index
            }, i) =>
                <TaskRow key={i}>
                    <StatisticsProblemCellWithColor probData={tasks[index]}
                        status={getStatus(isFirstToSolve, isSolved, pendingAttempts, wrongAttempts)}
                        attempts={wrongAttempts + pendingAttempts}/>
                </TaskRow>
            )}
        </TaskRowWrapperSecond>
        <ScoreboardTeamInfoRowSecond>
            <TeamInfo teamId={teamId}/>
        </ScoreboardTeamInfoRowSecond>
    </ScoreboardRowAllWrapper>;
};

const TeamInfo = ({ teamId }) => {
    const teamData = useSelector((state) => state.contestInfo.info?.teamsId[teamId]);
    const scoreboardData = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal]?.ids[teamId]);
    console.log(teamData);
    return <TeamInfoWrapper>
        <RankCell rank={scoreboardData?.rank} width={NUMWIDTH + "px"} medal={scoreboardData?.medalType}/>
        <TextShrinkingCell text={teamData?.shortName} width={NAMEWIDTH + "px"} canGrow={false} canShrink={false}/>
        <ScoreboardStatCell>
            {scoreboardData === null ? "??" : formatScore(scoreboardData.totalScore, 1)}
        </ScoreboardStatCell>
        <ScoreboardStatCell>
            {scoreboardData?.penalty}
        </ScoreboardStatCell>

    </TeamInfoWrapper>;
};

const ScoreboardWrapper = styled.div.attrs(({ align }) => ({ style: { justifyContent: align } }))`
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: start;
  position: relative;
  flex-direction: column;
  animation: ${props => props.animation} ${PVP_APPEAR_TIME}ms ${props => props.animationStyle};
  animation-fill-mode: forwards;
`;

// opacity: ${PVP_OPACITY};

const PVPInfo = styled.div`
  position: absolute;
  top: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-items: start;
`;


const TeamImageWrapper = styled.img`
  height: 100%;
`;

const TeamVideoWrapper = styled.video`
  height: 100%;
`;

const TeamWebRTCVideoWrapper = ({ url, setIsLoaded }) => {
    const dispatch = useDispatch();
    const videoRef = useRef();
    const rtcRef = useRef();
    useEffect(() => {
        dispatch(pushLog(`Webrtc content from ${url}`));
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
            .catch(e => dispatch(pushLog("ERROR featching  webrtc peer connection info: " + e)));

        return () => rtcRef.current?.close();
    }, [url]);
    return (<TeamVideoWrapper
        ref={videoRef}
        onCanPlay={() => setIsLoaded(true)}
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
        autoPlay
        muted/>);
};


const TeamVideoAnimationWrapper = styled.div`
  position: absolute;
  right: 0;
  object-fit: contain;
  height: 100%;
  display: flex;
  justify-content: start;
  align-items: center;
`;


const teamViewComponentRender = {
    Photo: ({ onLoadStatus, url }) => {
        return <TeamVideoAnimationWrapper>
            <TeamImageWrapper src={url} onLoad={() => onLoadStatus(true)}/>
        </TeamVideoAnimationWrapper>;
    },
    Video: ({ onLoadStatus, url }) => {
        return <TeamVideoAnimationWrapper>
            <TeamVideoWrapper
                src={url}
                onCanPlay={() => onLoadStatus(true)}
                onError={() => onLoadStatus(false)}
                autoPlay
                muted/>
        </TeamVideoAnimationWrapper>;
    },
    WebRTCFetchConnection: ({ onLoadStatus, url }) => {
        return <TeamVideoAnimationWrapper>
            <TeamWebRTCVideoWrapper url={url} setIsLoaded={onLoadStatus}/>
        </TeamVideoAnimationWrapper>;
    },
    WebRTCConnection: (props) => {
        return <TeamVideoAnimationWrapper>
            <TeamWebRTCSocketVideoWrapper {...props}/>
        </TeamVideoAnimationWrapper>;
    },
};

const TeamViewHolder = ({ onLoadStatus, media }) => {
    const Component = teamViewComponentRender[media.type];
    if (Component === undefined) {
        useEffect(() => onLoadStatus(true),
            [media.teamId]);
        return undefined;
    }
    return <Component onLoadStatus={onLoadStatus} {...media}/>;
};

const TeamViewPInPWrapper = styled.div`
  position: absolute;

  width: ${({ sizeX }) => `${sizeX * 0.361}px`};
  height: ${({ sizeX }) => `${sizeX * 0.52 * 0.4}px`};
  left: 0;
  bottom: ${({ bottom }) => `${bottom}`};
  top: ${({ top }) => `${top}`};

`;

export const PVP = ({ mediaContent, settings, setLoadedComponents, location }) => {

    if (settings.position === "PVP_TOP") {
        return mediaContent.concat(settings.content.filter(e => !e.isMedia)).map((c, index) => {
            const onLoadStatus = (v) => setLoadedComponents(m => v ? (m | (1 << index)) : (m & ~(1 << index)));
            if (c.isMedia) {
                const component = <TeamViewHolder key={index} onLoadStatus={onLoadStatus} media={c}/>;
                if (c.pInP) {
                    return <TeamViewPInPWrapper bottom={"80px"} top={"auto"}
                        sizeX={location.sizeX}>{component}</TeamViewPInPWrapper>;
                } else {
                    return component;
                }
            } else {
                useLayoutEffect(() => onLoadStatus(true),
                    []);
                return <PVPInfo>
                    <ScoreboardWrapper align={"end"}>
                        <ScoreboardRowAllTaskFirst teamId={c.teamId}/>
                    </ScoreboardWrapper>
                </PVPInfo>;
            }
        });
    } else {
        return mediaContent.concat(settings.content.filter(e => !e.isMedia)).map((c, index) => {
            const onLoadStatus = (v) => setLoadedComponents(m => v ? (m | (1 << index)) : (m & ~(1 << index)));
            if (c.isMedia) {
                const component = <TeamViewHolder key={index} onLoadStatus={onLoadStatus} media={c}/>;
                if (c.pInP) {
                    return <TeamViewPInPWrapper top={"80px"} bottom={"auto"}
                        sizeX={location.sizeX}>{component}</TeamViewPInPWrapper>;
                } else {
                    return component;
                }
            } else {
                useLayoutEffect(() => onLoadStatus(true),
                    []);
                return <PVPInfo>
                    <ScoreboardWrapper align={"start"}>
                        <ScoreboardRowAllTaskSecond teamId={c.teamId}/>
                    </ScoreboardWrapper>
                </PVPInfo>;
            }
        });
    }
};
PVP.ignoreAnimation = true;
PVP.overrideTimeout = PVP_APPEAR_TIME;
export default PVP;
