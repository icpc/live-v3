import React from "react";
import Container from "@mui/material/Container";

import "../App.css";
import PropTypes from "prop-types";
import { IconButton, ButtonGroup, Typography } from "@mui/material";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "../errors";
import AddIcon from "@mui/icons-material/Add";
import ClockIcon from "@mui/icons-material/AccessTime";
import ScoreboardIcon from "@mui/icons-material/EmojiEvents";
import TextIcon from "@mui/icons-material/Abc";
import { TickerTableRow } from "./TickerTableRow";
import { PresetsManager } from "./PresetsManager";

const addPresetButtons = [
    {
        type: "clock",
        component: ClockIcon,
        settings: { periodMs: 30000 },
    }, {
        part: "long",
        type: "scoreboard",
        component: ScoreboardIcon,
        settings: { periodMs: 30000, from: 1, to: 12 },
    }, {
        type: "text",
        component: TextIcon,
        settings: { text: "", periodMs: 30000 },
    },
];

class TickerManager extends PresetsManager {
    rowsFilter(row) {
        return super.rowsFilter(row) && row.settings.part === this.props.partType;
    }

    renderAddButton() {
        return (<ButtonGroup>
            {addPresetButtons.filter(p => p.part === undefined || p.part === this.props.partType).map(p =>
                <IconButton color="primary" size="large" key={p.type}
                    onClick={() => {this.onCreate({ ...p.settings, type: p.type, part: this.props.partType });}}>
                    <AddIcon/><p.component/>
                </IconButton>)}
        </ButtonGroup>);
    }
}
TickerManager.propTypes = {
    ...TickerManager.propTypes,
    partType: PropTypes.string.isRequired,
};
TickerManager.defaultProps = {
    ...PresetsManager.defaultProps,
    apiPath: "/tickerMessage",
    tableKeys: ["type", "text", "periodMs"],
    tableKeysHeaders: ["Type", "Text", "Period (ms)"],
    rowComponent: TickerTableRow,
};

function TickerMessage() {
    const { enqueueSnackbar, } = useSnackbar();
    return (
        <Container maxWidth="md" sx={{ pt: 2 }} className="TickerPanel">
            <Typography variant="h5" gutterBottom>Short</Typography>
            <TickerManager partType={"short"} createErrorHandler={errorHandlerWithSnackbar(enqueueSnackbar)}/>
            <Typography variant="h5" gutterBottom sx={{ mt: 3 }}>Long</Typography>
            <TickerManager partType={"long"} createErrorHandler={errorHandlerWithSnackbar(enqueueSnackbar)}/>
        </Container>
    );
}

export default TickerMessage;

