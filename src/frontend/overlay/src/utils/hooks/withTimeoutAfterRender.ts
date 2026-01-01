import { useEffect, useState } from "react";

export const useDelayed = <T>(timeout: number, value: T, initial: T): T => {
    const [isNotShownYet, setIsNotShownYet] = useState<T>(initial);
    useEffect(() => {
        const timer = setTimeout(() => {
            setIsNotShownYet(value);
        }, timeout);
        return () => clearTimeout(timer);
    }, [timeout, value]);
    return isNotShownYet;
};

export const useDelayedBoolean = (timeout: number): boolean => {
    return useDelayed(timeout, true, false);
};
