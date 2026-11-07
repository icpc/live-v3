import { Award } from "@shared/api";
import c from "@/config";

export const AwardEffect = {
    GOLD: "gold",
    SILVER: "silver",
    BRONZE: "bronze",
} as const;

export type KnownAwardEffect = (typeof AwardEffect)[keyof typeof AwardEffect];

/**
 * Visual effect applied to a team holding a matching award.
 * Any string works; EFFECT_RANK_BACKGROUND_COLOR defines the color used for it.
 * @TJS-type string
 * @TJS-examples ["gold", "silver", "bronze"]
 */
export type AwardEffect = KnownAwardEffect | (string & {});

export interface AwardEffectColors
    extends Partial<Record<KnownAwardEffect, string>> {
    [effect: string]: string | undefined;
}

const getEffectById = (id: string) => {
    if (c.AWARD_EFFECTS[id]) {
        return c.AWARD_EFFECTS[id];
    }

    for (const key in c.AWARD_EFFECTS) {
        if (key.endsWith("*")) {
            const prefix = key.slice(0, -1);
            if (id.startsWith(prefix)) {
                return c.AWARD_EFFECTS[key];
            }
        }
    }

    return null;
};

export const getAwardsEffects = (awards: Award[]) => {
    const effects = (awards || [])
        .map((award) => getEffectById(award.id))
        .filter((effect) => effect !== null);
    return Array.from(new Set(effects));
};
