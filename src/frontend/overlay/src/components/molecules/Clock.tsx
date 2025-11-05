import { DateTime, Settings, SystemZone } from "luxon";
import React, { useCallback, useEffect, useState } from "react";

import { useAppSelector } from "@/redux/hooks";
import { ClockType, ContestInfo, ContestStatus } from "@shared/api";
import { ShrinkingBox } from "../atoms/ShrinkingBox";
import c from "@/config";

Settings.defaultZone = "utc";

const formatLongTime = (ms: number, showSeconds: boolean): string => {
    const s = Math.floor(ms / 1000);
    const minus = s < 0 ? "-" : "";
    const hours = Math.floor(Math.abs(s) / (60 * 60));
    const minutes = Math.floor((Math.abs(s) % (60 * 60)) / 60);
    const seconds = Math.floor(Math.abs(s) % 60);
    if (showSeconds) {
        return `${minus}${hours}:${minutes.toString().padStart(2, "0")}:${seconds.toString().padStart(2, "0")}`;
    } else {
        return `${minus}${hours}:${minutes.toString().padStart(2, "0")}`;
    }
};

export const getStartTime = (contestInfo: ContestInfo): number => {
    switch (contestInfo.status.type) {
    case ContestStatus.Type.before:
        return contestInfo.status.scheduledStartAtUnixMs ?? 0;
    case ContestStatus.Type.running:
        return contestInfo.status.startedAtUnixMs ?? 0;
    case ContestStatus.Type.finalized:
    case ContestStatus.Type.over:
        return contestInfo.status.startedAtUnixMs ?? 0;
    }
}

export const calculateContestTime = (contestInfo: ContestInfo): number => {
    if (contestInfo === undefined) {
        return undefined;
    }

    const emulationSpeed = contestInfo.emulationSpeed ?? 1;
    switch (contestInfo.status.type) {
        case ContestStatus.Type.before:
            if (contestInfo.status.holdTimeMs !== undefined) {
                return -contestInfo.status.holdTimeMs * emulationSpeed;
            }
            return contestInfo.status.scheduledStartAtUnixMs
                ? Math.min(
                      (DateTime.now().toMillis() -
                          contestInfo.status.scheduledStartAtUnixMs) *
                          emulationSpeed,
                      0,
                  )
                : undefined;
        case ContestStatus.Type.running:
            return Math.min(
                (DateTime.now().toMillis() -
                    contestInfo.status.startedAtUnixMs) *
                    emulationSpeed,
                contestInfo.contestLengthMs,
            );
        case ContestStatus.Type.finalized:
        case ContestStatus.Type.over:
            return contestInfo.contestLengthMs;
    }
};

interface ContestClockProps {
    noStatusText?: string;
    clockType?: ClockType;
    showSeconds?: boolean;
    timeZone?: string | null;
}

export const ContestClock: React.FC<ContestClockProps> = ({
    noStatusText = "??",
    clockType = ClockType.standard,
    showSeconds = true,
    timeZone = null,
}) => {
    const contestInfo = useAppSelector((state) => state.contestInfo.info);

    const getStatus = useCallback(() => {
        if (clockType === ClockType.global) {
            return DateTime.now()
                .setZone(timeZone ?? new SystemZone())
                .toFormat(showSeconds ? "HH:mm:ss" : "HH:mm");
        }

        if (contestInfo === undefined) {
            return noStatusText;
        }

        switch (contestInfo.status.type) {
            case ContestStatus.Type.over:
            case ContestStatus.Type.finalized:
                return "OVER";
            default:
                const contestTime = calculateContestTime(contestInfo);
                if (contestTime !== undefined) {
                    if (clockType === ClockType.countdown && contestTime > 0) {
                        return formatLongTime(
                            contestInfo.contestLengthMs - contestTime,
                            showSeconds,
                        );
                    }

                    return formatLongTime(contestTime, showSeconds);
                } else {
                    return (contestInfo.status.type as string).toUpperCase();
                }
        }
    }, [contestInfo, clockType, showSeconds, noStatusText, timeZone]);

    const [status, setStatus] = useState(getStatus());
    useEffect(() => {
        const interval = setInterval(() => setStatus(getStatus()), 200);
        return () => clearInterval(interval);
    }, [getStatus]);
    return (
        <ShrinkingBox
            text={status}
            fontSize={c.TICKER_TEXT_FONT_SIZE}
            fontFamily={c.TICKER_FONT_FAMILY}
            align="center"
        />
    );
};

export default ContestClock;
