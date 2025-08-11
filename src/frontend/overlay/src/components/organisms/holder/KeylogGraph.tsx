import { useEffect, useMemo, useRef, useState } from "react";
import styled from "styled-components";
import c from "@/config";
import { isShouldUseDarkColor  } from "@/utils/colors";

interface KeylogSvgProps {
    $z: number;
}

const KeylogSvg = styled.svg<KeylogSvgProps>`
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    z-index: ${({ $z }) => $z};
`;

type KeylogGraphProps = {
    keylog: number[];
    contestLengthMs: number;
    isPvp: boolean;
    teamColor?: string;
};

type Point = [number, number];

function stepPath(points: Array<Point>) {
    if (points.length === 0) return "";
    
    const [x0, y0] = points[0];

    return points.slice(1).reduce(
        (path, [currX, currY], _) => {
            return path + ` H ${currX} V ${currY}`;
        },
        `M ${x0} ${y0}`
    );
}

export function KeylogGraph({
    keylog,
    contestLengthMs,
    isPvp,
    teamColor,
}: KeylogGraphProps) {
    const svgRef = useRef<SVGSVGElement | null>(null);
    const [size, setSize] = useState<{ w: number, h: number }>({ w: 0, h: 0 });

    useEffect(() => {
        if (!svgRef.current) return;
        const ro = new ResizeObserver(([entry]) => {
            const cr = entry.contentRect;
            setSize({ w: cr.width, h: cr.height });
        });

        ro.observe(svgRef.current);
        return () => ro.disconnect();
    }, []);

    useEffect(() => {
        if (!svgRef.current) return;
        const path = svgRef.current.querySelector("path[data-line]") as SVGPathElement | null;
        if (!path) return;

        const L = path.getTotalLength();
        path.style.transition = "none";
        path.style.strokeDasharray = `${L}`;
        path.style.strokeDashoffset = `${L}`;

        path.getBoundingClientRect();

        path.style.transition = `stroke-dashoffset ${c.KEYLOG_ANIMATION_DURATION}ms ${c.KEYLOG_ANIMATION_EASING}`;
        path.style.strokeDashoffset = "0";
    }, [keylog.length]);

    const { pathD, fillD } = useMemo(() => {
        const w = size.w;
        const h = size.h;

        if (!w || !h || !contestLengthMs || !keylog?.length) return { pathD: "", fillD: "" };

        const leftPad = c.TIMELINE_LEFT_TIME_PADDING;
        const usableW = w * (isPvp ? c.TIMELINE_REAL_WIDTH_PVP : c.TIMELINE_REAL_WIDTH);
        const rightEdge = leftPad + usableW;

        const bucketMs = c.KEYLOG_BUCKET_SIZE_MS;
        const maxVal = c.KEYLOG_MAXIMUM_FOR_NORMALIZATION;
        const topPad = c.KEYLOG_TOP_PADDING;
        const bottomPad = c.KEYLOG_BOTTOM_PADDING;

        const pts: Array<Point> = [];

        for (let i = 0; i < keylog.length; i++) {
            const t = Math.min((i * bucketMs) / contestLengthMs, 1);
            const x = leftPad + t * usableW;
            const v = keylog[i];
            const yNorm = v / maxVal;
            const y = topPad + (1 - yNorm) * (h - topPad - bottomPad);
            pts.push([x, y]);
        }

        pts[pts.length - 1][0] = Math.min(pts[pts.length - 1][0], rightEdge);

        const d = stepPath(pts);
        const fill = d + ` V ${h - bottomPad} H ${pts[0][0]} Z`;


        return { pathD: d, fillD: fill };
    }, [size, keylog, contestLengthMs, isPvp]);

    const useDark = isShouldUseDarkColor(teamColor ?? c.CONTEST_COLOR);
    const stroke = useDark ? c.KEYLOG_STROKE_DARK : c.KEYLOG_STROKE_LIGHT;
    const fill = useDark ? c.KEYLOG_FILL_DARK : c.KEYLOG_FILL_LIGHT;

    return (
        <KeylogSvg ref={svgRef} $z={c.KEYLOG_Z_INDEX} preserveAspectRatio="none">
            <defs>
                <filter id="kglow" x="-10%" y="-10%" width="120%" height="120%">
                    <feGaussianBlur stdDeviation={c.KEYLOG_GLOW_BLUR} result="blur" />
                </filter>
            </defs>
            {fillD && <path d={fillD} fill={fill} />}
            {pathD && (
                <path
                    data-line
                    d={pathD}
                    fill="none"
                    stroke={stroke}
                    strokeWidth={c.KEYLOG_STROKE_WIDTH}
                    strokeLinejoin="miter"
                    strokeLinecap="butt"
                    vectorEffect="non-scaling-stroke"
                />
            )}
        </KeylogSvg>
    );
}

