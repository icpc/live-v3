import { DateTime, Settings, SystemZone } from "luxon";
import { useCallback, useEffect, useState } from "react";
import { useSelector } from "react-redux";
import PropTypes from "prop-types";
// import SystemZone from "luxon/src/zones/systemZone";

Settings.defaultZone = "utc";


export const ContestClock = ({ noStatusText = "??", showStatus = true, globalTimeMode = false }) => {
    const contestInfo = useSelector((state) => state.contestInfo.info);
    const getStatus = useCallback(() => {
        if (globalTimeMode === true) {
            return DateTime.now().setZone(new SystemZone()).toFormat("HH:mm:ss");
        }

        if (contestInfo === undefined) {
            return noStatusText;
        }

        if (contestInfo.status === "RUNNING") {
            const milliseconds = DateTime.fromMillis(contestInfo.startTimeUnixMs).diffNow().negate().milliseconds *
                (contestInfo.emulationSpeed ?? 1);
            return DateTime.fromMillis(milliseconds).toFormat("H:mm:ss");
        } else if (contestInfo.status === "BEFORE") {
            if (contestInfo.holdBeforeStartTimeMs !== undefined) {
                return "-" + DateTime.fromMillis(contestInfo.holdBeforeStartTimeMs).toFormat("H:mm:ss");
            } else if (contestInfo.startTimeUnixMs !== undefined) {
                const milliseconds = DateTime.fromMillis(contestInfo.startTimeUnixMs).diffNow().negate().milliseconds *
                    (contestInfo.emulationSpeed ?? 1);
                if (milliseconds > 0) {
                    return DateTime.fromMillis(milliseconds).toFormat("H:mm:ss");
                }
                return "-" + DateTime.fromMillis(-milliseconds + 1000).toFormat("H:mm:ss");
            }
        }

        return showStatus ? contestInfo.status : "";
    }, [contestInfo, globalTimeMode]);
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
