import * as path from "node:path";

export const getPathParents = (p: string): string[] => {
    const res = [];
    while (p) {
        res.push(p);
        p = p.split(path.sep).slice(0, -1).join(path.sep);
    }
    return res;
};

