import { DateTime, Settings, SystemZone } from "luxon";
import { useCallback, useEffect, useState } from "react";
import { useSelector } from "react-redux";
import PropTypes from "prop-types";
// import SystemZone from "luxon/src/zones/systemZone";

Settings.defaultZone = "utc";


export const ContestClock = ({
    noStatusText = "??",
    showStatus = true,
    globalTimeMode = false,
    contestCountdownMode = false,
    quietMode = false,
}) => {
    const formatTime = (time, fullFormat = false) => {
        if (!fullFormat && quietMode && time > 5 * 60 * 1000) {
            return DateTime.fromMillis(time).toFormat("H:mm");
        }
        return DateTime.fromMillis(time).toFormat("H:mm:ss");
    };
    const contestInfo = useSelector((state) => state.contestInfo.info);
    const getMilliseconds = useCallback(() => {
        if (contestCountdownMode) {
            return DateTime.fromMillis(contestInfo.startTimeUnixMs + contestInfo.contestLengthMs)
                .diffNow().milliseconds * (contestInfo.emulationSpeed ?? 1);
        } else {
            return DateTime.fromMillis(contestInfo.startTimeUnixMs)
                .diffNow().negate().milliseconds * (contestInfo.emulationSpeed ?? 1);
        }
    }, [contestInfo, contestCountdownMode]);

    const getStatus = useCallback(() => {
        if (globalTimeMode === true) {
            return DateTime.now().setZone(new SystemZone()).toFormat(quietMode ? "HH:mm" : "HH:mm:ss");
        }

        if (contestInfo === undefined) {
            return noStatusText;
        }

        if (contestInfo.status === "BEFORE") {
            const milliseconds = DateTime.fromMillis(contestInfo.startTimeUnixMs)
                .diffNow().negate().milliseconds * (contestInfo.emulationSpeed ?? 1);
            if (contestInfo.holdBeforeStartTimeMs !== undefined) {
                return "-" + formatTime(contestInfo.holdBeforeStartTimeMs, true);
            } else if (contestInfo.startTimeUnixMs !== undefined && milliseconds <= 0) {
                return "-" + formatTime(-milliseconds + 1000, true);
            }
        }
        const milliseconds = getMilliseconds();
        if (contestInfo.status === "RUNNING" || contestInfo.status === "BEFORE") {
            return formatTime(milliseconds);
        }

        return showStatus ? contestInfo.status : "";
    }, [contestInfo, globalTimeMode, getMilliseconds, formatTime]);
    const [status, setStatus] = useState(getStatus());
    useEffect(() => {
        const interval = setInterval(() => setStatus(getStatus()), 200);
        return () => clearInterval(interval);
    }, [getStatus]);
    return status;
};

ContestClock.propTypes = {
    noStatusText: PropTypes.string,
    showStatus: PropTypes.bool,
    globalTimeMode: PropTypes.bool,
};

export default ContestClock;
