import { useEffect, useState } from "react";

export const FPSCounter = () => {
    const [fps, setFps] = useState<string>();
    useEffect(() => {
        let startTime = null;
        let frame = 0;
        let handle = null;

        const tick: FrameRequestCallback = (time) => {
            frame++;
            if (time - startTime > 1000) {
                setFps((frame / ((time - startTime) / 1000)).toFixed(1));
                startTime = time;
                frame = 0;
            }
            handle = window.requestAnimationFrame(tick);
        };
        tick(null);
        return () => window.cancelAnimationFrame(handle);
    }, []);
    return (
        <div>Frames made last second: {fps}</div>
    );
};
