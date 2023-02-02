import { ExpandLess, ExpandMore } from "@mui/icons-material";
import ArrowBackIosIcon from "@mui/icons-material/ArrowBackIos";
import ArrowForwardIosIcon from "@mui/icons-material/ArrowForwardIos";
import CircleCheckedIcon from "@mui/icons-material/CheckCircleOutline";
import CircleUncheckedIcon from "@mui/icons-material/RadioButtonUnchecked";
import {
    Box,
    Button,
    ButtonGroup,
    Checkbox,
    Container,
    IconButton,
    styled,
    Switch,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableRow,
    TextField,
    Typography
} from "@mui/material";
import { useSnackbar } from "notistack";
import PropTypes from "prop-types";
import React, { useCallback, useEffect, useRef, useState } from "react";
import { BASE_URL_BACKEND } from "../config";
import { errorHandlerWithSnackbar } from "../errors";
import { createApiGet, createApiPost } from "../utils";

function NumericField({ onChange : _onChange, value, minValue, arrowsDelta }) {
    arrowsDelta = arrowsDelta ?? 1;
    const ref = useRef(null);
    const blockWidth = ref.current?.offsetWidth ?? 0;
    const isPossibleArrows = blockWidth > 150;
    const onChange = useCallback(v => {
        const newV = Number.parseInt(v);
        _onChange(minValue === undefined || newV >= minValue ? newV : minValue);
    }, [_onChange, minValue]);
    return (<Box display="flex" justifyContent="space-between" alignItems="center" ref={ref}>
        {isPossibleArrows && <IconButton onClick={() => onChange(value - arrowsDelta)}><ArrowBackIosIcon/></IconButton>}
        <TextField type="number" size="small" onChange={e => onChange(e.target.value)} value={value}
            sx={{ maxWidth: isPossibleArrows ? blockWidth - 100 : 1 }}/>
        {isPossibleArrows && <IconButton onClick={() => onChange(value + arrowsDelta)}><ArrowForwardIosIcon/></IconButton>}
    </Box>);
}

NumericField.propTypes = {
    value: PropTypes.number.isRequired,
    minValue: PropTypes.number,
    arrowsDelta: PropTypes.number,
    onChange: PropTypes.func.isRequired,
};

const SlimTableCell = styled(TableCell)({
    padding: 4,
});

function ScoreboardSettings({ isShown, onClickShow, onClickHide, settings, setSettings }) {
    return (<Table align="center" sx={{ my: 2 }} size="small">
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
                    <NumericField value={settings.startFromRow} minValue={1} arrowsDelta={settings.teamsOnPage}
                        onChange={v => setSettings(s => ({ ...s, startFromRow: v }))}/>
                </SlimTableCell>
                <SlimTableCell align={"center"}>
                    <NumericField value={settings.numRows} minValue={0} arrowsDelta={settings.teamsOnPage}
                        onChange={v => setSettings(s => ({ ...s, numRows: v }))}/>
                </SlimTableCell>
                <SlimTableCell align={"center"}>
                    <Switch checked={settings.isInfinite}
                        onChange={t => setSettings(s => ({ ...s, isInfinite: t.target.checked }))}/>
                </SlimTableCell>
            </TableRow>
        </TableBody>
    </Table>);
}

ScoreboardSettings.propTypes = {
    isShown: PropTypes.bool.isRequired,
    onClickShow: PropTypes.func.isRequired,
    onClickHide: PropTypes.func.isRequired,
    settings: PropTypes.shape({
        startFromRow: PropTypes.number.isRequired,
        numRows: PropTypes.number.isRequired,
        teamsOnPage: PropTypes.number.isRequired,
        isInfinite: PropTypes.bool.isRequired,
    }).isRequired,
    setSettings: PropTypes.func.isRequired,
};

function ScoreboardOptLevelCells({ settings, setSettings, group }) {
    return ["normal", "optimistic", "pessimistic"].map(type =>
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
}

ScoreboardOptLevelCells.propTypes = {
    settings: PropTypes.shape({
        group: PropTypes.string,
        optimismLevel: PropTypes.string.isRequired,
    }).isRequired,
    setSettings: PropTypes.func.isRequired,
    group: PropTypes.string.isRequired,
};

function ScoreboardGroupSetting({ settings, setSettings, groupsList }) {
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
                <TableRow key={group}>
                    <TableCell>
                        {group}
                    </TableCell>
                    <ScoreboardOptLevelCells settings={settings} setSettings={setSettings} group={group}/>
                </TableRow>
            )}
        </TableBody>
    </Table>);
}

ScoreboardGroupSetting.propTypes = {
    settings: PropTypes.shape({
        group: PropTypes.string,
        optimismLevel: PropTypes.string.isRequired,
    }).isRequired,
    setSettings: PropTypes.func.isRequired,
    groupsList: PropTypes.arrayOf(PropTypes.string).isRequired,
};

const apiPost = createApiPost(BASE_URL_BACKEND + "/scoreboard");
const apiGet = createApiGet(BASE_URL_BACKEND + "/scoreboard");

function ScoreboardManager() {
    const { enqueueSnackbar, } = useSnackbar();
    const createErrorHandler = errorHandlerWithSnackbar(enqueueSnackbar);

    const [isShown, setIsShown] = useState(false);
    const [settings, setSettings] = useState({
        isInfinite: true,
        optimismLevel: "Normal",
        group: "all",
        startFromRow: 1,
        numRows: 0,
        teamsOnPage: 23,
    });
    const [groupsList, setGroupsList] = useState([]);

    const update = (isFirstLoad) => {
        apiGet("")
            .then(
                (result) => {
                    setIsShown(result.shown);
                    if (isFirstLoad) {
                        result.settings.group = result.settings.group ?? "all";
                        result.settings.numRows = result.settings.numRows ?? 100;
                        setSettings(result.settings);
                    }
                })
            .catch(createErrorHandler("Failed to load list of presets"));
        apiGet("/regions")
            .then((result) => setGroupsList(result))
            .catch(createErrorHandler("Failed to load info"));
    };

    useEffect(() => update(true), []);

    const onClickHide = () => {
        apiPost("/hide")
            .then(() => update())
            .catch(createErrorHandler("Failed to hide scoreboard"));
    };

    const onClickShow = () => {
        apiPost("/show_with_settings", settings)
            .then(() => setIsShown(true))
            .then(() => update())
            .catch(createErrorHandler("Failed to show scoreboard"));
    };

    return (
        <Container maxWidth="md" sx={{ display: "flex", width: "75%", flexDirection: "column", pt: 2 }}
            className="ScoreboardSettings">
            <ScoreboardSettings isShown={isShown} onClickShow={onClickShow} onClickHide={onClickHide}
                settings={settings} setSettings={setSettings}/>
            <ScoreboardGroupSetting groupsList={groupsList} settings={settings} setSettings={setSettings}/>
        </Container>
    );
}

export default ScoreboardManager;
