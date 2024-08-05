import { ExpandLess, ExpandMore } from "@mui/icons-material";
import CircleCheckedIcon from "@mui/icons-material/CheckCircleOutline";
import CircleUncheckedIcon from "@mui/icons-material/RadioButtonUnchecked";
import {
    Box,
    Button,
    ButtonGroup,
    Checkbox,
    Switch,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableRow,
    Typography
} from "@mui/material";
import { Dispatch, SetStateAction, useEffect, useState } from "react";
import { SlimTableCell } from "../atoms/Table.tsx";
import NumericField from "../controls/NumericField.tsx";
import { GroupInfo, OptimismLevel, ScoreboardSettings } from "@shared/api.ts";
import { ScoreboardWidgetService } from "@/services/scoreboardService.ts";


type ScoreboardSettingsTabProps = {
    isShown: boolean;
    onClickShow: () => void;
    onClickHide: () => void;
    settings: ScoreboardSettings;
    setSettings: Dispatch<SetStateAction<ScoreboardSettings>>;
}

const ScoreboardSettingsTab = ({ isShown, onClickShow, onClickHide, settings, setSettings }: ScoreboardSettingsTabProps) => {
    return (<Table align="center" sx={{ my: 0 }} size="small">
        <TableHead>
            <TableRow>
                {["", "Start from row", "Amount of rows", "Infinity"].map(val =>
                    <SlimTableCell key={val} align={"center"}>
                        <Typography variant="h6">{val}</Typography>
                    </SlimTableCell>
                )}
            </TableRow>
        </TableHead>
        <TableBody>
            <TableRow>
                <SlimTableCell align={"center"}>
                    <ButtonGroup variant="contained" sx={{ m: 2 }}>
                        <Button color="primary" disabled={isShown} onClick={onClickShow}>Show</Button>
                        <Button color="error" disabled={!isShown} onClick={onClickHide}>Hide</Button>
                    </ButtonGroup>
                </SlimTableCell>
                <SlimTableCell align={"center"}>
                    <NumericField value={settings.startFromRow} minValue={1} arrowsDelta={settings.startFromRow}
                        onChange={v => setSettings(s => ({ ...s, startFromRow: v }))}/>
                </SlimTableCell>
                <SlimTableCell align={"center"}>
                    <NumericField value={settings.numRows} minValue={0} arrowsDelta={settings.numRows}
                        onChange={v => setSettings(s => ({ ...s, numRows: v }))}/>
                </SlimTableCell>
                <SlimTableCell align={"center"}>
                    <Switch checked={settings.isInfinite}
                        onChange={t => setSettings(s => ({ ...s, isInfinite: t.target.checked }))}/>
                </SlimTableCell>
            </TableRow>
        </TableBody>
    </Table>);
};

type ScoreboardOptLevelCellsProps = {
    settings: ScoreboardSettings;
    setSettings: Dispatch<SetStateAction<ScoreboardSettings>>;
    group: string;
}

const ScoreboardOptLevelCells = ({ settings, setSettings, group }: ScoreboardOptLevelCellsProps) => {
    return [OptimismLevel.normal, OptimismLevel.optimistic, OptimismLevel.pessimistic].map(type =>
        <SlimTableCell key={type} align="center">
            <Checkbox
                icon={<CircleUncheckedIcon/>}
                checkedIcon={<CircleCheckedIcon/>}
                checked={settings.group === group && settings.optimismLevel === type}
                sx={{ "& .MuiSvgIcon-root": { fontSize: 24 } }}
                onChange={() => setSettings(state => ({
                    ...state,
                    optimismLevel: type,
                    group: group
                }))}
            />
        </SlimTableCell>
    );
};

type ScoreboardGroupSettingProps = {
    settings: ScoreboardSettings;
    setSettings: Dispatch<SetStateAction<ScoreboardSettings>>;
    groupsList: GroupInfo[];
}

const ScoreboardGroupSetting = ({ settings, setSettings, groupsList }: ScoreboardGroupSettingProps) => {
    const [isGroupsExpand, setIsGroupsExpand] = useState(false);
    useEffect(() => setIsGroupsExpand(s => s || settings.group !== "all"), [settings.group]);

    return (<Table sx={{ m: 2 }} size="small">
        <TableHead>
            <TableRow>
                <TableCell>
                    <Typography variant="h6">Groups</Typography>
                </TableCell>
                {["Normal", "Optimistic", "Pessimistic"].map(val =>
                    <TableCell key={val} align="center">
                        <Typography variant="h6">{val}</Typography>
                    </TableCell>
                )}
            </TableRow>
        </TableHead>
        <TableBody>
            <TableRow key={"__all__"}>
                <TableCell>
                    <Box display="flex" justifyContent="space-between" alignItems="center">
                        <Box>All groups</Box>
                        <Button onClick={() => setIsGroupsExpand(!isGroupsExpand)}>
                            {isGroupsExpand ? <ExpandLess/> : <ExpandMore/>}</Button>
                    </Box>
                </TableCell>
                <ScoreboardOptLevelCells settings={settings} setSettings={setSettings} group={"all"}/>
            </TableRow>
            {isGroupsExpand && groupsList.map(group =>
                <TableRow key={group.id}>
                    <TableCell>
                        {group.displayName}
                    </TableCell>
                    <ScoreboardOptLevelCells settings={settings} setSettings={setSettings} group={group.id}/>
                </TableRow>
            )}
        </TableBody>
    </Table>);
};

export const DEFAULT_SCOREBOARD_SETTINGS: ScoreboardSettings = {
    isInfinite: true,
    optimismLevel: OptimismLevel.normal,
    group: "all",
    startFromRow: 1,
    numRows: 0,
};

// TODO: create generic type for all managers that has service, settings and setSettings
export type ScoreboardManagerProps = {
    service: ScoreboardWidgetService;
    isShown: boolean;
    settings: ScoreboardSettings;
    setSettings: Dispatch<SetStateAction<ScoreboardSettings>>;
}

const ScoreboardManager = ({ service, isShown, settings, setSettings }: ScoreboardManagerProps) => {
    const [groupsList, setGroupsList] = useState([]);

    useEffect(() => {
        service.groups().then((result) => setGroupsList(result));
    }, [service]);

    const onClickHide = () => {
        // TODO: when hide not reload settings from server, because I want set new settings and than hide + show
        service.hide();
    };

    const onClickShow = () => {
        service.showWithSettings(settings);
    };

    return (
        <>
            <ScoreboardSettingsTab isShown={isShown} onClickShow={onClickShow} onClickHide={onClickHide}
                settings={settings} setSettings={setSettings}/>
            <ScoreboardGroupSetting groupsList={groupsList} settings={settings} setSettings={setSettings}/>
        </>
    );
};

export default ScoreboardManager;
