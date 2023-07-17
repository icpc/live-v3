import { useEffect, useState } from "react";

export const useWithTimeoutAfterRender = (timeout) => {
    const [isNotShownYet, setIsNotShownYet] = useState(true);
    useEffect(() => {
        const timer = setTimeout(() => {
            setIsNotShownYet(false);
        }, timeout);
        return () => clearTimeout(timer);
    }, []);
    return isNotShownYet;
};
