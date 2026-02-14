import { Award } from "@shared/api";
import c from "@/config";

export enum AwardEffect {
    GOLD = "gold",
    SILVER = "silver",
    BRONZE = "bronze",
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
