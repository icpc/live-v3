import { EffectCallback, useEffect, useRef } from "react";

export const useMountEffect = (effect: EffectCallback) => {
    const hasMount = useRef(false);
    
    useEffect(() => {
        if (!hasMount.current) {
            hasMount.current = true;
            return effect();
        }
    }, [effect]);
};
