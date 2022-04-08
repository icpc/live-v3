import React from "react";
import "../App.css";
import { PresetsTable } from "./PresetsTable";
import PropTypes from "prop-types";
import Typography from "@mui/material/Typography";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "../errors";
import AddIcon from "@mui/icons-material/Add";
import ClockIcon from "@mui/icons-material/AccessTime";
import ScoreboardIcon from "@mui/icons-material/EmojiEvents";
import TextIcon from "@mui/icons-material/Abc";
import IconButton from "@mui/material/IconButton";
import { ButtonGroup } from "@mui/material";
import { TickerTableRow } from "./TickerTableRow";

const addPresetButtons = [
    {
        part: "short",
        type: "clock",
        component: ClockIcon,
        settings: { periodMs: 30000 },
    }, {
        part: "short",
        type: "text",
        component: TextIcon,
        settings: { text: "", periodMs: 30000 },
    }, {
        part: "long",
        type: "scoreboard",
        component: ScoreboardIcon,
        settings: { periodMs: 30000, from: 1, to: 12 },
    }, {
        part: "long",
        type: "text",
        component: TextIcon,
        settings: { text: "", periodMs: 30000 },
    },
];

class TickerTable extends PresetsTable {
    rowsFilter(row) {
        return super.rowsFilter(row) && row.settings.part === this.props.partType;
    }

    renderAddButton() {
        return (<ButtonGroup>
            {addPresetButtons.filter(p => p.part === this.props.partType).map(p =>
                <IconButton color="primary" size="large" key={p.type}
                    onClick={() => {this.doAddPreset({ ...p.settings, type: p.type, part: p.part });}}>
                    <AddIcon/><p.component/>
                </IconButton>)}
        </ButtonGroup>);
    }
}

TickerTable.defaultProps = {
    ...PresetsTable.defaultProps,
    apiPath: "/tickermessage",
    apiTableKeys: ["type", "text", "periodMs"],
    tableKeysHeaders: ["Type", "Text", "Period (ms)"],
    rowComponent: TickerTableRow,
};

TickerTable.propTypes = {
    partType: PropTypes.string.isRequired,
};

function TickerMessage() {
    const { enqueueSnackbar, } = useSnackbar();
    return (
        <div className="TickerPanel">
            <Typography variant="h5" gutterBottom>Short</Typography>
            <TickerTable partType={"short"} createErrorHandler={errorHandlerWithSnackbar(enqueueSnackbar)}/>
            <Typography variant="h5" gutterBottom sx={{ mt: 2 }}>Long</Typography>
            <TickerTable partType={"long"} createErrorHandler={errorHandlerWithSnackbar(enqueueSnackbar)}/>
        </div>
    );
}

export default TickerMessage;

