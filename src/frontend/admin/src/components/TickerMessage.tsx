import React from "react";
import { Container, IconButton, ButtonGroup } from "@mui/material";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "shared-code/errors";
import AddIcon from "@mui/icons-material/Add";
import ClockIcon from "@mui/icons-material/AccessTime";
import ScoreboardIcon from "@mui/icons-material/EmojiEvents";
import TextIcon from "@mui/icons-material/Abc";
import ImageIcon from "@mui/icons-material/Image";
import { TickerTableRow } from "./TickerTableRow";
import Dashboard from "./Dashboard";
import { usePresetWidgetService } from "../services/presetWidget";
import { PresetsManager, AddButtonsProps, PresetService } from "./PresetsManager";
import { ClockType, TickerPart } from "../../../generated/api";

interface BaseTickerSettings extends Record<string, unknown> {
    periodMs: number;
    part: TickerPart;
    type: string;
}

interface ClockSettings extends BaseTickerSettings {
    type: "clock";
    clockType: ClockType;
    showSeconds: boolean;
    timeZone: string | null;
}

interface ScoreboardSettings extends BaseTickerSettings {
    type: "scoreboard";
    from: number;
    to: number;
}

interface TextSettings extends BaseTickerSettings {
    type: "text";
    text: string;
}

interface ImageSettings extends BaseTickerSettings {
    type: "image";
    path: string;
}

type TickerMessageSettings = ClockSettings | ScoreboardSettings | TextSettings | ImageSettings;

interface PresetButtonConfig {
    type: "clock" | "scoreboard" | "text" | "image";
    component: React.ComponentType;
    part?: TickerPart;
    settings: Omit<TickerMessageSettings, "part">;
}

interface TickerPartComponentProps {
    service: PresetService<TickerMessageSettings>;
    part: TickerPart;
}

interface DashboardElements {
    [key: string]: React.ReactElement;
}

const PRESET_BUTTON_CONFIGS: PresetButtonConfig[] = [
    {
        type: "clock",
        component: ClockIcon,
        settings: {
            type: "clock",
            periodMs: 30000,
            clockType: ClockType.standard,
            showSeconds: true,
            timeZone: null
        } as Omit<ClockSettings, "part">,
    },
    {
        type: "scoreboard",
        component: ScoreboardIcon,
        part: TickerPart.long,
        settings: {
            type: "scoreboard",
            periodMs: 30000,
            from: 1,
            to: 12
        } as Omit<ScoreboardSettings, "part">,
    },
    {
        type: "text",
        component: TextIcon,
        settings: {
            type: "text",
            periodMs: 30000,
            text: ""
        } as Omit<TextSettings, "part">,
    },
    {
        type: "image",
        component: ImageIcon,
        settings: {
            type: "image",
            periodMs: 30000,
            path: ""
        } as Omit<ImageSettings, "part">,
    },
];

function filterPresetsByPart(
    presets: PresetButtonConfig[],
    part: TickerPart
): PresetButtonConfig[] {
    return presets.filter(preset => preset.part === undefined || preset.part === part);
};

const createAddButtons = (part: TickerPart): React.FC<AddButtonsProps<TickerMessageSettings>> => {
    const AddButtons: React.FC<AddButtonsProps<TickerMessageSettings>> = ({ onCreate }) => {
        const filteredPresets = filterPresetsByPart(PRESET_BUTTON_CONFIGS, part);

        const handlePresetCreate = (preset: PresetButtonConfig) => {
            const settingsWithPart = {
                ...preset.settings,
                part: part,
            } as TickerMessageSettings;

            onCreate(settingsWithPart);
        };

        return (
            <ButtonGroup>
                {filteredPresets.map((preset) => {
                    const IconComponent = preset.component;
                    return (
                        <IconButton
                            key={preset.type}
                            color="primary"
                            size="large"
                            onClick={() => handlePresetCreate(preset)}
                            aria-label={`Add ${preset.type} preset`}
                        >
                            <AddIcon />
                            <IconComponent />
                        </IconButton>
                    );
                })}
            </ButtonGroup>
        );
    };

    AddButtons.displayName = `AddButtons-${part}`;
    return AddButtons;
};

function TickerPartComponent({
    service,
    part
}: TickerPartComponentProps): React.ReactElement {
    const AddButtonsComponent = createAddButtons(part);

    // TODO: THINK about types
    const rowsFilter = (row) => row.settings.part === part;

    return (
        <PresetsManager<TickerMessageSettings>
            service={service}
            tableKeys={["type", "text", "periodMs"]}
            tableKeysHeaders={["Type", "Text", "Period (ms)"]}
            RowComponent={TickerTableRow}
            rowsFilter={rowsFilter}
            AddButtons={AddButtonsComponent}
        />
    );
};

function TickerMessage(): React.ReactElement {
    const { enqueueSnackbar } = useSnackbar();
    const service = usePresetWidgetService<TickerMessageSettings>(
        "/tickerMessage",
        errorHandlerWithSnackbar(enqueueSnackbar)
    );

    const dashboardElements: DashboardElements = {
        Short: (
            <Container
                maxWidth="md"
                sx={{ pt: 2 }}
                className="TickerPanel"
            >
                <TickerPartComponent
                    service={service}
                    part={TickerPart.short}
                />
            </Container>
        ),
        Long: (
            <Container
                maxWidth="md"
                sx={{ pt: 2 }}
                className="TickerPanel"
            >
                <TickerPartComponent
                    service={service}
                    part={TickerPart.long}
                />
            </Container>
        ),
    };

    return (
        <Dashboard
            elements={dashboardElements}
            layout="oneColumn"
            maxWidth="md"
        />
    );
};

export default TickerMessage;
