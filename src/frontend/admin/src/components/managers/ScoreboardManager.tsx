import { ExpandLess, ExpandMore } from "@mui/icons-material";
import CircleCheckedIcon from "@mui/icons-material/CheckCircleOutline";
import CircleUncheckedIcon from "@mui/icons-material/RadioButtonUnchecked";
import {
    Box,
    Button,
    ButtonGroup,
    Checkbox,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableRow,
    Typography,
} from "@mui/material";
import {
    Dispatch,
    SetStateAction,
    useCallback,
    useEffect,
    useState,
} from "react";
import { SlimTableCell } from "../atoms/Table.tsx";
import {
    GroupInfo,
    OptimismLevel,
    ScoreboardSettings,
    ScoreboardScrollDirection,
} from "@shared/api.ts";
import { ScoreboardWidgetService } from "@/services/scoreboardService.ts";
import ScrollDirectionSwitcher from "@/components/controls/ScrollDirectionSwitcher.tsx";

type ScoreboardSettingsTabProps = {
    isShown: boolean;
    showWithSettings: (settings: ScoreboardSettings) => void;
    hide: () => void;
    settings: ScoreboardSettings;
    setSettings: Dispatch<SetStateAction<ScoreboardSettings>>;
};

const ScoreboardSettingsTab = ({
    isShown,
    showWithSettings,
    hide,
    settings,
    setSettings,
}: ScoreboardSettingsTabProps) => {
    const setScrollDirection = useCallback(
        (d: ScoreboardScrollDirection) => {
            setSettings((s) => {
                const newSettings: ScoreboardSettings = {
                    ...s,
                    scrollDirection: d,
                };
                if (isShown) {
                    showWithSettings(newSettings);
                }
                return newSettings;
            });
        },
        [setSettings, isShown, showWithSettings],
    );

    return (
        <Stack spacing={4} direction="row" flexWrap="wrap" sx={{ mx: 2 }}>
            <ButtonGroup>
                <Button
                    color={isShown ? "success" : "primary"}
                    onClick={() => showWithSettings(settings)}
                    variant="contained"
                >
                    {isShown ? "Update" : "Show"}
                </Button>
                <Button
                    color="error"
                    disabled={!isShown}
                    onClick={hide}
                    variant="contained"
                >
                    Hide
                </Button>
            </ButtonGroup>
            <ScrollDirectionSwitcher
                direction={settings.scrollDirection}
                setDirection={setScrollDirection}
            />
        </Stack>
    );
};

type ScoreboardOptLevelCellsProps = {
    settings: ScoreboardSettings;
    setSettings: Dispatch<SetStateAction<ScoreboardSettings>>;
    group: string;
};

const ScoreboardOptLevelCells = ({
    settings,
    setSettings,
    group,
}: ScoreboardOptLevelCellsProps) => {
    return [
        OptimismLevel.normal,
        OptimismLevel.optimistic,
        OptimismLevel.pessimistic,
    ].map((type) => (
        <SlimTableCell key={type} align="center">
            <Checkbox
                icon={<CircleUncheckedIcon />}
                checkedIcon={<CircleCheckedIcon />}
                checked={
                    settings.group === group && settings.optimismLevel === type
                }
                sx={{ "& .MuiSvgIcon-root": { fontSize: 24 } }}
                onChange={() =>
                    setSettings((state) => ({
                        ...state,
                        optimismLevel: type,
                        group: group,
                    }))
                }
            />
        </SlimTableCell>
    ));
};

type ScoreboardGroupSettingProps = {
    settings: ScoreboardSettings;
    setSettings: Dispatch<SetStateAction<ScoreboardSettings>>;
    groupsList: GroupInfo[];
};

const ScoreboardGroupSetting = ({
    settings,
    setSettings,
    groupsList,
}: ScoreboardGroupSettingProps) => {
    const [isGroupsExpand, setIsGroupsExpand] = useState(false);
    useEffect(
        () => setIsGroupsExpand((s) => s || settings.group !== "all"),
        [settings.group],
    );

    return (
        <Table sx={{ m: 2 }} size="small">
            <TableHead>
                <TableRow>
                    <TableCell>
                        <Typography variant="h6">Groups</Typography>
                    </TableCell>
                    {["Normal", "Optimistic", "Pessimistic"].map((val) => (
                        <TableCell key={val} align="center">
                            <Typography variant="h6">{val}</Typography>
                        </TableCell>
                    ))}
                </TableRow>
            </TableHead>
            <TableBody>
                <TableRow key={"__all__"}>
                    <TableCell>
                        <Box
                            display="flex"
                            justifyContent="space-between"
                            alignItems="center"
                        >
                            <Box>All groups</Box>
                            <Button
                                onClick={() =>
                                    setIsGroupsExpand(!isGroupsExpand)
                                }
                            >
                                {isGroupsExpand ? (
                                    <ExpandLess />
                                ) : (
                                    <ExpandMore />
                                )}
                            </Button>
                        </Box>
                    </TableCell>
                    <ScoreboardOptLevelCells
                        settings={settings}
                        setSettings={setSettings}
                        group={"all"}
                    />
                </TableRow>
                {isGroupsExpand &&
                    groupsList.map((group) => (
                        <TableRow key={group.id}>
                            <TableCell>{group.displayName}</TableCell>
                            <ScoreboardOptLevelCells
                                settings={settings}
                                setSettings={setSettings}
                                group={group.id}
                            />
                        </TableRow>
                    ))}
            </TableBody>
        </Table>
    );
};

export const DEFAULT_SCOREBOARD_SETTINGS: ScoreboardSettings = {
    optimismLevel: OptimismLevel.normal,
    group: "all",
    scrollDirection: ScoreboardScrollDirection.Forward,
};

// TODO: create generic type for all managers that has service, settings and setSettings
export type ScoreboardManagerProps = {
    service: ScoreboardWidgetService;
    isShown: boolean;
    settings: ScoreboardSettings;
    setSettings: Dispatch<SetStateAction<ScoreboardSettings>>;
};

const ScoreboardManager = ({
    service,
    isShown,
    settings,
    setSettings,
}: ScoreboardManagerProps) => {
    const [groupsList, setGroupsList] = useState([]);

    useEffect(() => {
        service.groups().then((result) => setGroupsList(result));
    }, [service]);

    return (
        <>
            <ScoreboardSettingsTab
                isShown={isShown}
                showWithSettings={(s: ScoreboardSettings) =>
                    service.showWithSettings(s)
                }
                hide={() => service.hide()}
                settings={settings}
                setSettings={setSettings}
            />
            <ScoreboardGroupSetting
                groupsList={groupsList}
                settings={settings}
                setSettings={setSettings}
            />
        </>
    );
};

export default ScoreboardManager;
