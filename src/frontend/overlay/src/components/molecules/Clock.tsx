import { DateTime, Settings, SystemZone } from "luxon";
import { useCallback, useEffect, useState } from "react";

import PropTypes from "prop-types";
import { useAppSelector } from "@/redux/hooks";
import { ContestStatus } from "@shared/api";

Settings.defaultZone = "utc";


export const ContestClock = ({
    noStatusText = "??",
    showStatus = true,
    globalTimeMode = false,
    contestCountdownMode = false,
    quietMode = false,
    timeZone = null
}) => {
    const formatTime = (time, fullFormat = false) => {
        if (!fullFormat && quietMode && time > 5 * 60 * 1000) {
            return DateTime.fromMillis(time).toFormat("H:mm");
        }
        return DateTime.fromMillis(time).toFormat("H:mm:ss");
    };
    const contestInfo = useAppSelector((state) => state.contestInfo.info);
    const getMilliseconds = useCallback(() => {
        if (contestCountdownMode) {
            return DateTime.fromMillis(contestInfo.startTimeUnixMs + contestInfo.contestLengthMs)
                .diffNow().milliseconds * (contestInfo.emulationSpeed ?? 1);
        } else {
            return DateTime.fromMillis(contestInfo.startTimeUnixMs)
                .diffNow().negate().milliseconds * (contestInfo.emulationSpeed ?? 1);
        }
    }, [contestInfo, contestCountdownMode]);

    const getDateTimeNowWithCustomTimeZone = (zone) =>
        DateTime.now().setZone(zone).toFormat(quietMode ? "HH:mm" : "HH:mm:ss");

    const getStatus = useCallback(() => {
        if (globalTimeMode === true) {
            return getDateTimeNowWithCustomTimeZone(timeZone ?? new SystemZone());
        }

        if (timeZone !== null) {
            return getDateTimeNowWithCustomTimeZone(timeZone);
        }

        if (contestInfo === undefined) {
            return noStatusText;
        }
        switch (contestInfo.status) {
        case ContestStatus.BEFORE: {
            const milliseconds = DateTime.fromMillis(contestInfo.startTimeUnixMs)
                .diffNow().negate().milliseconds * (contestInfo.emulationSpeed ?? 1);
            if (contestInfo.holdBeforeStartTimeMs !== undefined) {
                return "-" + formatTime(contestInfo.holdBeforeStartTimeMs, true);
            } else if (contestInfo.startTimeUnixMs !== undefined && milliseconds <= 0) {
                return "-" + formatTime(-milliseconds + 1000, true);
            } else if (contestInfo.startTimeUnixMs !== undefined && milliseconds <= 60 * 1000) {
                // hack just in case backend is slow in sending contest state
                return formatTime(milliseconds , true);
            }
            return showStatus ? "BEFORE" : "";
        }
        case ContestStatus.RUNNING:
        case ContestStatus.FAKE_RUNNING: {
            const milliseconds = Math.min(getMilliseconds(), contestInfo.contestLengthMs);
            return formatTime(milliseconds);
        }
        case ContestStatus.OVER:
        case ContestStatus.FINALIZED:
            return showStatus ? "OVER" : "";
        }
    }, [contestInfo, globalTimeMode, getMilliseconds, formatTime]);
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
    globalTimeMode: PropTypes.bool,
};

export default ContestClock;
