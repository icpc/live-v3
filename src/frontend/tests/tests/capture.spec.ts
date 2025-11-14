import { test } from "@playwright/test";
import { generateTest } from "./test-generator.js";
import { TestConfig } from "./types.js";

const contestConfigs: TestConfig[] = [
    {
        path: "config/icpc-wf/2023/finals",
        layouts: [
            {
                displayDelay: 5000,
                widgets: [{ path: "queue" }, { path: "scoreboard" }],
                analytics: {
                    messageId: "run_1674",
                    makeFeaturedType: "record",
                },
            },
            {
                displayDelay: 10000,
                widgets: [
                    { path: "queue" },
                    {
                        path: "teamView/SINGLE",
                        settings: {
                            mediaTypes: ["camera", "screen"],
                            teamId: 47065,
                            showTaskStatus: true,
                            showAchievement: true,
                            showTimeLine: true,
                        },
                    },
                ],
            },
            {
                displayDelay: 10000,
                widgets: [
                    { path: "queue" },
                    {
                        path: "teamPVP/PVP_TOP",
                        settings: {
                            mediaTypes: ["screen", "photo"],
                            teamId: null,
                            showTaskStatus: true,
                            showAchievement: true,
                        },
                    },
                    {
                        path: "teamPVP/PVP_BOTTOM",
                        settings: {
                            mediaTypes: ["screen", "photo"],
                            teamId: 47060,
                            showTaskStatus: true,
                            showAchievement: true,
                        },
                    },
                ],
            },
        ],
    },
    { path: "config/__tests/ejudge_icpc_unfreeze/2023-voronezh" },
    //    "config/__tests/ejudge_ioi/regionalroi-lpk-2021-d1",
    { path: "config/__tests/ejudge_ioi_virtual/mosh-2023-keldysh" },
    { path: "config/__tests/pcms_icpc_freeze/icpc-nef-2022-2023" },
    { path: "config/__tests/pcms_icpc_overrides/icpc-nef-2021-2022" },
    // { path: "config/__tests/pcms_ioi/innopolis-open-2022-2023-final" },
    { path: "config/__tests/testsys_icpc/spbsu-2023-may" },
];

for (const [index, contestConfig] of contestConfigs.entries()) {
    test(`config ${contestConfig.path}`, generateTest(index, contestConfig));
}
