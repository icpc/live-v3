import React, { useCallback, useState } from "react";
import {
    Box,
    Button,
    ButtonGroup,
    createTheme,
    Grid,
    Table,
    TableBody,
    TableCell,
    TableRow,
    TextField,
    ThemeProvider,
} from "@mui/material";
import PropTypes from "prop-types";
import { selectedRowColor } from "../styles";
import { useTeamSpotlightService } from "../services/teamSpotlight";

const rowTheme = createTheme({
    components: {
        MuiTableCell: {
            styleOverrides: {
                // Name of the slot
                root: {
                    // Some CSS
                    lineHeight: "100%",
                    padding: "4px 6px"
                },
            },
            variants: [{
                props: {
                    type: "icon"
                },
                style: {
                    fontSize: "0px"
                }
            }]
        },
    },
});

const formatScore = (score) =>
    score.toLocaleString(undefined, { maximumFractionDigits: 2 });

function TeamScoreTable({ teams, selectedTeams, onRowClick, scoreCalculator }) {
    const rowBackground = useCallback((e) => {
        if (selectedTeams.includes(e.teamId)) {
            return selectedRowColor;
        } else {
            return undefined;
        }
    }, [selectedTeams]);
    return (
        <ThemeProvider theme={rowTheme}>
            <Table sx={{ m: 2 }} size="small">
                <TableBody>
                    {teams.map((event, rowId) =>
                        <TableRow key={rowId} sx={{ backgroundColor: rowBackground(event), cursor: "pointer" }}
                            onClick={() => onRowClick(event)}>
                            <TableCell>{event.teamName}</TableCell>
                            <TableCell>{scoreCalculator(event.teamId) && "+" + formatScore(scoreCalculator(event.teamId))}
                            </TableCell>
                            <TableCell>{formatScore(event.score)}</TableCell>
                        </TableRow>)}
                </TableBody>
            </Table>
        </ThemeProvider>);
}

TeamScoreTable.propTypes = {
    teams: PropTypes.arrayOf(
        PropTypes.shape({
            teamId: PropTypes.any.isRequired,
            teamName: PropTypes.string.isRequired,
            score: PropTypes.number.isRequired,
        }).isRequired).isRequired,
    selectedTeams: PropTypes.arrayOf(PropTypes.number.isRequired).isRequired,
    onRowClick: PropTypes.func.isRequired,
    scoreCalculator: PropTypes.func.isRequired,
};

const ButtonGroupTextField = (props) =>
    <TextField
        sx={{ width: "120px" }}
        size="small"
        variant="outlined"
        InputProps={{ style: { height: "36.5px" } }}
        {...props}
    />;

function TeamSpotlight() {
    const { teamsList, addScore } = useTeamSpotlightService();
    const [selectedTeamIds, setSelectedTeamIds] = useState([]);
    const selectTeam = useCallback(({ teamId }) => {
        if (selectedTeamIds.includes(teamId)) {
            setSelectedTeamIds(selectedTeamIds.filter(t => t !== teamId));
        } else {
            setSelectedTeamIds([ ...selectedTeamIds, teamId ]);
        }
    }, [selectedTeamIds, setSelectedTeamIds]);

    const [scoreFrom, setScoreFrom] = useState(100);
    const [scoreTo, setScoreTo] = useState(90);
    const getCurrentScore = useCallback(teamId => {
        const ordinaryNumber = selectedTeamIds.indexOf(teamId);
        if (ordinaryNumber === -1) {
            return undefined;
        }
        return ordinaryNumber === 0 ? scoreFrom : scoreFrom - (scoreFrom - scoreTo) * ordinaryNumber / (selectedTeamIds.length - 1);
    }, [selectedTeamIds, scoreFrom, scoreTo]);

    const onResetClick = useCallback(() => setSelectedTeamIds([]),
        [setSelectedTeamIds]);
    const onAddScoreClick = useCallback(() => {
        addScore(selectedTeamIds.map(teamId => ({ teamId, score: getCurrentScore(teamId) })))
            .then(onResetClick);
    }, [addScore, setScoreFrom, getCurrentScore, setSelectedTeamIds]);

    return (<Grid sx={{
        display: "flex",
        alignContent: "center",
        justifyContent: "center",
        alignItems: "center",
        flexDirection: "column"
    }}>
        <Box sx={{ width: { "md": "75%", "sm": "100%", "xs": "100%" } }}>
            <Box sx={{ pt: 2 }}>
                <ButtonGroup disabled={selectedTeamIds.length === 0} sx={{ px: 2 }}>
                    <ButtonGroupTextField
                        onChange={e => setScoreFrom(e.target.value)}
                        value={scoreFrom}
                        label="Score from"
                    />
                    <ButtonGroupTextField
                        onChange={e => setScoreTo(e.target.value)}
                        value={scoreTo}
                        label="Score to"
                    />
                    <Button color="primary" variant={"outlined"} onClick={onAddScoreClick}>Add score</Button>
                    <Button color="error" onClick={onResetClick}>Reset</Button>
                </ButtonGroup>
            </Box>
            <TeamScoreTable teams={teamsList} selectedTeams={selectedTeamIds}
                onRowClick={selectTeam} scoreCalculator={getCurrentScore}/>
        </Box>
    </Grid>
    );
}

export default TeamSpotlight;
