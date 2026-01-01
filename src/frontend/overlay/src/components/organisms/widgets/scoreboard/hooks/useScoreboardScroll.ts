import {
    useEffect,
    useState,
    useRef,
    useLayoutEffect,
    useCallback,
    startTransition,
} from "react";
import c from "@/config";
import { ScoreboardScrollDirection } from "@shared/api";
import { easeInOutQuad } from "@/components/organisms/widgets/scoreboard/utils/easingFunctions";

export function useScroller(
    totalRows: number,
    singleScreenRowCount: number,
    interval: number,
    direction: ScoreboardScrollDirection | undefined,
) {
    const effectiveRowCount = Math.max(1, singleScreenRowCount);
    const showRows = totalRows;
    const numPages = Math.max(1, Math.ceil(showRows / effectiveRowCount));
    const singlePageRowCount = Math.ceil(showRows / numPages);

    const curPageRef = useRef(0);
    const [scrollPos, setScrollPos] = useState(0);

    const calcScrollPos = useCallback(
        (page: number) => {
            const pageEndRow = Math.min(
                (page + 1) * singlePageRowCount,
                totalRows,
            );
            return Math.max(0, pageEndRow - effectiveRowCount);
        },
        [singlePageRowCount, totalRows, effectiveRowCount],
    );

    useEffect(() => {
        if (direction === ScoreboardScrollDirection.FirstPage) {
            curPageRef.current = 0;
            startTransition(() => setScrollPos(calcScrollPos(0)));
        } else if (direction === ScoreboardScrollDirection.LastPage) {
            curPageRef.current = numPages - 1;
            startTransition(() => setScrollPos(calcScrollPos(numPages - 1)));
        }
    }, [direction, numPages, calcScrollPos]);

    useEffect(() => {
        if (
            direction !== ScoreboardScrollDirection.Pause &&
            direction !== ScoreboardScrollDirection.FirstPage &&
            direction !== ScoreboardScrollDirection.LastPage
        ) {
            const intervalId = setInterval(() => {
                const delta =
                    direction === ScoreboardScrollDirection.Back ? -1 : 1;
                let nextPage = curPageRef.current + delta;
                if (nextPage < 0) {
                    nextPage = numPages - 1;
                }
                if (nextPage >= numPages) {
                    nextPage = 0;
                }
                curPageRef.current = nextPage;
                startTransition(() => setScrollPos(calcScrollPos(nextPage)));
            }, interval);
            return () => {
                clearInterval(intervalId);
            };
        }
    }, [interval, numPages, direction, calcScrollPos]);

    return scrollPos;
};

export function useAnimatedScrollPos(targetScrollPos: number) {
    const scrollPosRef = useRef(targetScrollPos);
    const animationRef = useRef<number | null>(null);
    const startTimeRef = useRef<number>(0);
    const startPosRef = useRef<number>(targetScrollPos);
    const targetPosRef = useRef<number>(targetScrollPos);
    const subscribersRef = useRef<Set<() => void>>(new Set());

    const subscribe = useCallback((callback: () => void) => {
        subscribersRef.current.add(callback);
        return () => subscribersRef.current.delete(callback);
    }, []);

    const getScrollPos = useCallback(() => scrollPosRef.current, []);

    useLayoutEffect(() => {
        if (targetScrollPos === targetPosRef.current) return;

        startPosRef.current = scrollPosRef.current;
        targetPosRef.current = targetScrollPos;
        startTimeRef.current = performance.now();

        const animate = (now: number) => {
            const elapsed = now - startTimeRef.current;
            const duration = c.SCOREBOARD_ROW_TRANSITION_TIME;
            const progress = Math.min(elapsed / duration, 1);

            scrollPosRef.current =
                startPosRef.current +
                (targetPosRef.current - startPosRef.current) * easeInOutQuad(progress);

            subscribersRef.current.forEach((cb: () => void) => cb());

            if (progress < 1) {
                animationRef.current = requestAnimationFrame(animate);
            }
        };

        if (animationRef.current) {
            cancelAnimationFrame(animationRef.current);
        }
        animationRef.current = requestAnimationFrame(animate);

        return () => {
            if (animationRef.current) {
                cancelAnimationFrame(animationRef.current);
            }
        };
    }, [targetScrollPos]);

    return { getScrollPos, subscribe };
};
