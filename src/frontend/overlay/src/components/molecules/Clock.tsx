import { DateTime, Settings, SystemZone } from "luxon";
import { useCallback, useEffect, useState } from "react";

import PropTypes from "prop-types";
import { useAppSelector } from "@/redux/hooks";
import { ClockType, ContestInfo, ContestStatus } from "@shared/api";

Settings.defaultZone = "utc";

export function getStartTime(status: ContestStatus) : number {
    switch (status.type) {
    case ContestStatus.Type.before:
        return status.scheduledStartAtUnixMs ?? 0;
    case ContestStatus.Type.over:
    case ContestStatus.Type.running:
    case ContestStatus.Type.finalized:
        return status.startedAtUnixMs;
    }
}

const formatTime = (time: number, showSeconds: boolean): string => {
    return DateTime.fromMillis(time).toFormat(showSeconds ? "H:mm:ss" : "H:mm");
};

const formatLongTime = (ms: number): string => {
    const hours = Math.floor(ms / (1000 * 60 * 60));
    const minutes = Math.floor((ms % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((ms % (1000 * 60)) / 1000);
    return `${hours}:${minutes.toString().padStart(2, "0")}:${seconds.toString().padStart(2, "0")}`;
};

const calculateContestTime = (contestInfo: ContestInfo, clockType: ClockType): number => {
    const emulationSpeed = contestInfo.emulationSpeed ?? 1;
    const startTime = getStartTime(contestInfo.status);
    
    if (clockType === ClockType.countdown) {
        const contestEndTime = DateTime.fromMillis(startTime + contestInfo.contestLengthMs / emulationSpeed, { zone: "utc" });
        const now = DateTime.now().setZone("utc");
        return contestEndTime.diff(now).milliseconds * emulationSpeed;
    } else {
        const contestStartTime = DateTime.fromMillis(startTime, { zone: "utc" });
        const now = DateTime.now().setZone("utc");
        return now.diff(contestStartTime).milliseconds * emulationSpeed;
    }
};

const handleBeforeStatus = (contestInfo: ContestInfo, clockType: ClockType, showStatus: boolean, showSeconds: boolean): string => {
    const beforeStatus = contestInfo.status;
    
    if (beforeStatus.type === ContestStatus.Type.before && beforeStatus.holdTimeMs !== undefined) {
        return "-" + formatTime(beforeStatus.holdTimeMs, showSeconds);
    }
    
    if (beforeStatus.type === ContestStatus.Type.before && beforeStatus.scheduledStartAtUnixMs !== undefined) {
        const realTimeDiff = DateTime.fromMillis(beforeStatus.scheduledStartAtUnixMs).diffNow().milliseconds;
        const milliseconds = Math.abs(realTimeDiff * (contestInfo.emulationSpeed ?? 1));
        
        if (clockType === ClockType.countdown) {
            return formatLongTime(Math.max(0, milliseconds));
        }
        
        if (milliseconds <= 0) {
            return "-" + formatTime(-milliseconds + 1000, showSeconds);
        } else if (milliseconds <= 60 * 1000) {
            return formatTime(milliseconds, showSeconds);
        }
    }
    
    return showStatus ? "BEFORE" : "";
};

const handleRunningStatus = (contestInfo: ContestInfo, clockType: ClockType, showStatus: boolean, showSeconds: boolean): string => {
    if (clockType === ClockType.countdown) {
        const emulationSpeed = contestInfo.emulationSpeed ?? 1;
        const contestEndTime = DateTime.fromMillis(
            getStartTime(contestInfo.status) + contestInfo.contestLengthMs / emulationSpeed,
            { zone: "utc" }
        );
        const now = DateTime.now().setZone("utc");
        const remainingMs = contestEndTime.diff(now).milliseconds * emulationSpeed;
        
        if (remainingMs <= 0) {
            return showStatus ? "OVER" : "";
        }
        return formatTime(Math.max(0, remainingMs), showSeconds);
    } else {
        const milliseconds = Math.min(calculateContestTime(contestInfo, clockType), contestInfo.contestLengthMs);
        return formatTime(milliseconds, showSeconds);
    }
};

export const ContestClock = ({
    noStatusText = "??",
    showStatus = true,
    clockType = ClockType.standard,
    showSeconds = true,
    timeZone = null
}) => {
    const contestInfo = useAppSelector((state) => state.contestInfo.info);

    const getDateTimeNowWithCustomTimeZone = (zone: SystemZone | string): string =>
        DateTime.now().setZone(zone).toFormat(showSeconds ? "HH:mm:ss" : "HH:mm");

    const getStatus = useCallback(() => {
        if (clockType === ClockType.global) {
            return getDateTimeNowWithCustomTimeZone(timeZone ?? new SystemZone());
        }

        if (contestInfo === undefined) {
            return noStatusText;
        }

        switch (contestInfo.status.type) {
        case ContestStatus.Type.before:
            return handleBeforeStatus(contestInfo, clockType, showStatus, showSeconds);
        case ContestStatus.Type.running:
            return handleRunningStatus(contestInfo, clockType, showStatus, showSeconds);
        case ContestStatus.Type.over:
        case ContestStatus.Type.finalized:
            return showStatus ? "OVER" : "";
        }
    }, [contestInfo, clockType, showStatus, showSeconds, noStatusText, timeZone]);
    const [status, setStatus] = useState(getStatus());
    useEffect(() => {
        const interval = setInterval(() => setStatus(getStatus()), 200);
        return () => clearInterval(interval);
    }, [getStatus]);
    return <>{status}</>;
};

ContestClock.propTypes = {
    noStatusText: PropTypes.string,
    showStatus: PropTypes.bool,
    clockType: PropTypes.oneOf([ClockType.standard, ClockType.countdown, ClockType.global]),
    showSeconds: PropTypes.bool,
    timeZone: PropTypes.string,
};

export default ContestClock;
