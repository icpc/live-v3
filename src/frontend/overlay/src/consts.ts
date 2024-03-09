export const DEBUG = import.meta.env.DEV;

export const WEBSOCKETS = {
    mainScreen: "mainScreen",
    contestInfo: "contestInfo",
    queue: "queue",
    statistics: "statistics",
    ticker: "ticker",
    scoreboardNormal: "scoreboard/normal",
    scoreboardOptimistic: "scoreboard/optimistic",
    scoreboardPessimistic: "scoreboard/pessimistic",
};

export const SCOREBOARD_TYPES = Object.freeze({
    normal: "normal",
    optimistic: "optimistic",
    pessimistic: "pessimistic"
});

export const faviconTemplate = `
<svg fill="none" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg">
<g clip-path="url(#a)">
<rect width="512" height="512" rx="256" fill="{CONTEST_COLOR}"/>
<circle cx="255.49" cy="256.49" r="42.75" fill="{TEXT_COLOR}"/>
<path d="m256 372.33v66.667m-66.667 0h133.33m-163.33-66.667h193.33c18.668 0 28.003 0 35.133-3.633 6.272-3.195 11.372-8.295 14.567-14.567 3.633-7.13 3.633-16.465 3.633-35.133v-126.67c0-18.668 0-28.002-3.633-35.133-3.195-6.272-8.295-11.371-14.567-14.567-7.13-3.633-16.465-3.633-35.133-3.633h-193.33c-18.668 0-28.002 0-35.133 3.633-6.272 3.196-11.371 8.295-14.567 14.567-3.633 7.131-3.633 16.465-3.633 35.133v126.67c0 18.668 0 28.003 3.633 35.133 3.196 6.272 8.295 11.372 14.567 14.567 7.131 3.633 16.465 3.633 35.133 3.633z" stroke="{TEXT_COLOR}" stroke-linecap="round" stroke-width="50"/>
</g>
<defs>
<clipPath id="a">
<rect width="512" height="512" rx="256" fill="{TEXT_COLOR}"/>
</clipPath>
</defs>
</svg>`;
