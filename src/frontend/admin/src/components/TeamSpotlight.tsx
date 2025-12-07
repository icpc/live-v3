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
import { selectedRowColor } from "../styles";
import { useTeamSpotlightService } from "../services/teamSpotlight";
import { TeamId } from "@shared/api";
import { Team, ScoreEntry } from "@/services/teamSpotlight";

interface ButtonGroupTextFieldProps {
    value: number;
    label: string;
    onChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
}

interface TeamScoreTableProps {
    teams: Team[];
    selectedTeams: TeamId[];
    onRowClick: (team: Team) => void;
    scoreCalculator: (teamId: TeamId) => number | undefined;
}

const tableTheme = createTheme({
    components: {
        MuiTableCell: {
            styleOverrides: {
                root: {
                    lineHeight: "100%",
                    padding: "4px 6px",
                },
            },
        },
    },
});

function formatScore(score: number): string {
    return score.toLocaleString(undefined, { maximumFractionDigits: 2 });
}

function calculateDistributedScore(
    teamId: TeamId,
    selectedTeamIds: TeamId[],
    scoreFrom: number,
    scoreTo: number,
): number | undefined {
    const ordinaryNumber = selectedTeamIds.indexOf(teamId);
    if (ordinaryNumber === -1) {
        return undefined;
    }

    if (ordinaryNumber === 0) {
        return scoreFrom;
    }

    const scoreRange = scoreFrom - scoreTo;
    const stepSize = scoreRange / (selectedTeamIds.length - 1);
    return scoreFrom - stepSize * ordinaryNumber;
}

function ButtonGroupTextField({
    value,
    label,
    onChange,
}: ButtonGroupTextFieldProps): React.ReactElement {
    return (
        <TextField
            sx={{ width: "120px" }}
            size="small"
            variant="outlined"
            value={value}
            label={label}
            onChange={onChange}
            type="number"
            InputProps={{
                style: { height: "36.5px" },
                inputProps: { step: "any" },
            }}
        />
    );
}

function TeamScoreTable({
    teams,
    selectedTeams,
    onRowClick,
    scoreCalculator,
}: TeamScoreTableProps) {
    const getRowBackground = useCallback(
        (team: Team): string | undefined => {
            return selectedTeams.includes(team.id)
                ? selectedRowColor
                : undefined;
        },
        [selectedTeams],
    );

    return (
        <ThemeProvider theme={tableTheme}>
            <Table sx={{ m: 2 }} size="small">
                <TableBody>
                    {teams.map((team, rowId) => {
                        const calculatedScore = scoreCalculator(team.id);
                        return (
                            <TableRow
                                key={`${team.id}-${rowId}`}
                                sx={{
                                    backgroundColor: getRowBackground(team),
                                    cursor: "pointer",
                                    "&:hover": {
                                        opacity: 0.8,
                                    },
                                }}
                                onClick={() => onRowClick(team)}
                            >
                                <TableCell>{team.teamName}</TableCell>
                                <TableCell>
                                    {calculatedScore &&
                                        `+${formatScore(calculatedScore)}`}
                                </TableCell>
                                <TableCell>{formatScore(team.score)}</TableCell>
                            </TableRow>
                        );
                    })}
                </TableBody>
            </Table>
        </ThemeProvider>
    );
}

function TeamSpotlight(): React.ReactElement {
    const { teamsList, addScore } = useTeamSpotlightService();
    const [selectedTeamIds, setSelectedTeamIds] = useState<TeamId[]>([]);
    const [scoreFrom, setScoreFrom] = useState<number>(100);
    const [scoreTo, setScoreTo] = useState<number>(90);

    const selectTeam = useCallback(
        (team: Team) => {
            setSelectedTeamIds((prev) => {
                if (prev.includes(team.id)) {
                    return prev.filter((id) => id !== team.id);
                }
                return [...prev, team.id];
            });
        },
        [setSelectedTeamIds],
    );

    const getCurrentScore = useCallback(
        (teamId: TeamId): number | undefined => {
            return calculateDistributedScore(
                teamId,
                selectedTeamIds,
                scoreFrom,
                scoreTo,
            );
        },
        [selectedTeamIds, scoreFrom, scoreTo],
    );

    const handleResetClick = useCallback(
        () => setSelectedTeamIds([]),
        [setSelectedTeamIds],
    );

    const handleAddScoreClick = useCallback(async () => {
        const scoreEntires: ScoreEntry[] = selectedTeamIds.map((teamId) => ({
            teamId,
            score: getCurrentScore(teamId) || 0,
        }));

        try {
            await addScore(scoreEntires);
            handleResetClick();
        } catch (error) {
            console.error(`Failed to add scores: ${error}`);
        }
    }, [selectedTeamIds, getCurrentScore, addScore, handleResetClick]);

    const handleScoreFromChange = useCallback(
        (event: React.ChangeEvent<HTMLInputElement>) => {
            setScoreFrom(Number(event.target.value));
        },
        [],
    );

    const handleScoreToChange = useCallback(
        (event: React.ChangeEvent<HTMLInputElement>) => {
            setScoreTo(Number(event.target.value));
        },
        [],
    );

    const isActionDisabled = selectedTeamIds.length === 0;

    return (
        <Grid
            sx={{
                display: "flex",
                alignContent: "center",
                justifyContent: "center",
                alignItems: "center",
                flexDirection: "column",
            }}
        >
            <Box sx={{ width: { md: "75%", sm: "100%", xs: "100%" } }}>
                <Box sx={{ pt: 2 }}>
                    <ButtonGroup disabled={isActionDisabled} sx={{ px: 2 }}>
                        <ButtonGroupTextField
                            value={scoreFrom}
                            label="Score from"
                            onChange={handleScoreFromChange}
                        />
                        <ButtonGroupTextField
                            value={scoreTo}
                            label="Score to"
                            onChange={handleScoreToChange}
                        />
                        <Button
                            color="primary"
                            variant="outlined"
                            onClick={handleAddScoreClick}
                        >
                            Add score
                        </Button>
                        <Button color="error" onClick={handleResetClick}>
                            Reset
                        </Button>
                    </ButtonGroup>
                </Box>
                <TeamScoreTable
                    teams={teamsList}
                    selectedTeams={selectedTeamIds}
                    onRowClick={selectTeam}
                    scoreCalculator={getCurrentScore}
                />
            </Box>
        </Grid>
    );
}

export default TeamSpotlight;
