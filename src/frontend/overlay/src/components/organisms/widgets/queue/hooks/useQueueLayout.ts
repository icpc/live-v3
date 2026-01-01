import { useState, useCallback } from "react";

export function useQueueLayout(initialHeight?: number) {
    const [height, setHeight] = useState<number | undefined>(initialHeight);
    const [headerWidth, setHeaderWidth] = useState<number>(0);

    const handleHeaderRef = useCallback((el: HTMLElement | null) => {
        if (el != null) {
            setHeaderWidth(el.getBoundingClientRect().width);
        }
    }, []);

    const handleRowsContainerRef = useCallback((el: HTMLElement | null) => {
        if (el != null) {
            const bounding = el.getBoundingClientRect();
            setHeight(bounding.height);
        }
    }, []);

    return {
        height,
        headerWidth,
        handleHeaderRef,
        handleRowsContainerRef,
    };
};
