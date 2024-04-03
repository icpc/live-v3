import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
    Container,
    Box,
    ToggleButtonGroup,
    ToggleButton,
    Paper,
    Stack,
    Button,
    ButtonGroup,
    TextField,
    InputAdornment,
    FormLabel,
    FormControl,
    ThemeProvider,
    createTheme,
} from "@mui/material";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "shared-code/errors";
import { TeamViewService, useLocatorService } from "../services/oracleLocatorWidget";
import ArrowDropDown from "@mui/icons-material/ArrowDropDown";
import { SelectTeamTable } from "./TeamTable";
import PropTypes from "prop-types";

const isTeamSatisfiesSearch = (team, searchValue) => {
    if (searchValue === "" || team.id === null) {
        return true;
    }
    return (team.contestSystemId + " : " + team.shortName + " : " + team.name).toLowerCase().includes(searchValue);
};

const useTeamsList = (rawTeams, status) => {
    const [selectedTeamId, setSelectedTeamId] = useState(undefined);
    const teamsWithStatus = useMemo(
        () => rawTeams.map(t => ({
            ...t,
            shown: Object.values(status).some(s => s.shown && s.settings.teamId === t.id),
            selected: t.id === selectedTeamId,
        })),
        [rawTeams, status, selectedTeamId]);

    const [searchValue, setSearchValue] = useState("");

    const filteredTeams = useMemo(() => {
        return teamsWithStatus.filter(t => isTeamSatisfiesSearch(t, searchValue));
    }, [teamsWithStatus, searchValue]);
    return { teams: filteredTeams, selectedTeamId, setSelectedTeamId, searchValue, setSearchValue };
};

const teamViewTheme = createTheme({
    components: {
        MuiPaper: {
            styleOverrides: {
                root: ({ theme }) => ({
                    padding: theme.spacing(2),
                    marginBottom: theme.spacing(1),
                }),
            },
        },
        MuiButton: {
            defaultProps: {
                size: "small",
            },
        },
        MuiButtonGroup: {
            defaultProps: {
                size: "small",
            },
        },
        MuiButtonBase: {
            styleOverrides: {
                root: {
                    margin: "0 !important",
                },
            },
        },
    },
});

const VariantSelect = ({ variants, variant, setVariant }) => {
    return (
        <ToggleButtonGroup
            value={variant}
            color="primary"
            size="small"
            exclusive
            onChange={(_, v) => v && setVariant(v)}
        >
            {variants.ids.map(v => <ToggleButton key={v} value={v}>Oracle {v}</ToggleButton>)}
        </ToggleButtonGroup>
    );
};

VariantSelect.propTypes = {
    variants: PropTypes.any,
    variant: PropTypes.oneOf(["1", ]).isRequired,
    setVariant: PropTypes.func.isRequired,
};

const InstanceStatus = ({ selectedInstance, onShow, onHide }) => {
    const isShowButtonDisabled = !(selectedInstance || selectedInstance === undefined);
    return (
        <Stack sx={{ mb: 1 }} spacing={1} direction="row" flexWrap="wrap" alignItems={"center"}>
            <ButtonGroup variant="contained" sx={{ m: 0 }}>
                <Button color="primary" disabled={isShowButtonDisabled} onClick={onShow(true)}>
                    {selectedInstance ? "Selected" : "Show here"}
                </Button>
                <Button color="error" onClick={onHide}>Hide</Button>
            </ButtonGroup>
        </Stack>
    );
};

InstanceStatus.propTypes = {
    selectedInstance: PropTypes.string,
    onShow: PropTypes.func.isRequired,
    onHide: PropTypes.func.isRequired,
};

const useOracleList = (oracleLocatorService) => {
    const [oracles, setOracles] = useState({ ids: [] });
    useEffect(() => {
        oracleLocatorService.oracles().then(s => setOracles(s));
    }, []);
    return oracles;
};

const OracleViewManager = ({ service }) => {
    const oracles = useOracleList(service);
    const [oracle, setOracle] = useState(1);

    const [rawTeams, setRawTeams] = useState([]);
    const { teams, selectedTeamId, setSelectedTeamId, searchValue, setSearchValue } = useTeamsList(rawTeams, {});
    useEffect(() => {
        service.teams().then((ts) => setRawTeams([...ts]));
    }, [service]);

    const [selectedInstance, setSelectedInstance] = useState(undefined);

    const onInstanceSelect = useCallback((instance) => () => {
        if (instance === selectedInstance) {
            setSelectedInstance(undefined);
        } else {
            setSelectedInstance(instance);
        }
    }, [selectedInstance]);

    const [selectedTeamName, selectedTeamCdsId] = useMemo(() => {
        if (selectedTeamId === undefined) {
            return ["", null];
        }
        const team = teams.find(team => team.id === selectedTeamId);
        return [team?.name ?? "", team?.contestSystemId];
    }, [teams, selectedTeamId]);

    const onMove = useCallback(() => {
        const settings = {
            oracleId: oracle,
            teamId: selectedTeamCdsId,
        };
        service.moveWithSettings(settings);
    }, [selectedInstance, selectedTeamId, service]);

    const onShow = useCallback(() => {
        const settings = {
            oracleId: oracle,
            teamId: selectedTeamCdsId,
        };
        service.showWithSettings(settings);
    }, [selectedInstance, selectedTeamId, service]);

    const onHide = useCallback(() => {
        service.hide();
        setSelectedInstance(undefined);
        setSelectedTeamId(undefined);
    }, [service]);

    return (
        <Box>
            <Box sx={{ mb: 1 }} display="flex" flexWrap="wrap" justifyContent="space-between" alignItems="center">
                <VariantSelect variants={oracles} variant={oracle} setVariant={setOracle} />
            </Box>
            <InstanceStatus onShow={onInstanceSelect} onHide={onHide} selectedInstance={selectedInstance} />

            {selectedInstance !== undefined &&
                <Paper>
                    {selectedTeamId === undefined && (
                        <Box>
                            <TextField
                                onChange={(e) => setSearchValue(e.target.value.toLowerCase())}
                                defaultValue={searchValue}
                                size="small"
                                margin="none"
                                label="Search"
                                variant="outlined"
                                fullWidth
                            />
                            <SelectTeamTable teams={teams} onClickHandler={setSelectedTeamId} />
                        </Box>
                    )}
                    {selectedTeamId !== undefined && (
                        <>
                            <FormControl fullWidth sx={{ mb: 1 }}>
                                <FormLabel component="legend">Team name</FormLabel>
                                <TextField
                                    defaultValue={selectedTeamName}
                                    variant="standard"
                                    fullWidth
                                    InputProps={{
                                        endAdornment: (
                                            <InputAdornment position="end">
                                                <ArrowDropDown />
                                            </InputAdornment>
                                        ),
                                        onClick: () => setSelectedTeamId(undefined),
                                    }}
                                />
                            </FormControl>
                            <ButtonGroup variant="contained">
                                <Button
                                    color="secondary"
                                    variant="contained"
                                    onClick={onMove}>Move sniper
                                </Button>
                                <Button
                                    color="primary"
                                    variant="contained"
                                    onClick={onShow}>Show locator
                                </Button>
                                <Button
                                    color="error"
                                    variant="contained"
                                    onClick={onHide}>Hide
                                </Button>
                            </ButtonGroup>
                        </>
                    )}
                </Paper>
            }
        </Box>
    );
};

OracleViewManager.propTypes = {
    service: PropTypes.instanceOf(TeamViewService).isRequired,
};


function OracleLocator() {
    const { enqueueSnackbar, } = useSnackbar();
    const service = useLocatorService(errorHandlerWithSnackbar(enqueueSnackbar));

    return (
        <Container sx={{ pt: 2 }}>
            <ThemeProvider theme={teamViewTheme}>
                <OracleViewManager service={service}/>
            </ThemeProvider>
        </Container>
    );
}

export default OracleLocator;
