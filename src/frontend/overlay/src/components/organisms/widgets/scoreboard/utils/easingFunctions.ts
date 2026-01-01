

export function easeInOutQuad(progress: number): number {
    return progress < 0.5
        ? 2 * progress * progress
        : 1 - Math.pow(-2 * progress + 2, 2) / 2;
};

export function interpolate(from: number, to: number, progress: number): number {
    return from + (to - from) * easeInOutQuad(progress);
};

export function calculateProgress(startTime: number, duration: number): number {
    const elapsed = performance.now() - startTime;
    return Math.min(elapsed / duration, 1);
};
