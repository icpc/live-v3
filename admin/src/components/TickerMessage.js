import React from "react";
import Container from "@mui/material/Container";
import PropTypes from "prop-types";
import { IconButton, ButtonGroup, Typography } from "@mui/material";
import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "../errors";
import AddIcon from "@mui/icons-material/Add";
import ClockIcon from "@mui/icons-material/AccessTime";
import ScoreboardIcon from "@mui/icons-material/EmojiEvents";
import TextIcon from "@mui/icons-material/Abc";
import { TickerTableRow } from "./TickerTableRow";
import Dashboard from "./Dashboard";
import { usePresetWidgetService } from "../services/presetWidget";
import { PresetsManager } from "./PresetsManager";
import { AbstractWidgetService } from "../services/abstractWidget";

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

const makeAddButtons = (part) => {
    function AddButtons({ onCreate }) {
        return (<ButtonGroup>
            {addPresetButtons.filter(p => p.part === undefined || p.part === part).map(p =>
                <IconButton color="primary" size="large" key={p.type}
                    onClick={() => onCreate({ ...p.settings, type: p.type, part: part })}>
                    <AddIcon/><p.component/>
                </IconButton>)}
        </ButtonGroup>);
    }
    AddButtons.propTypes = { onCreate: PropTypes.func.isRequired };
    return AddButtons;
};

const TickerPart = ({ service, part }) =>
    (<PresetsManager
        service={service}
        tableKeys={["type", "text", "periodMs"]}
        tableKeysHeaders={["Type", "Text", "Period (ms)"]}
        RowComponent={TickerTableRow}
        rowsFilter={row => row.settings.part === part}
        AddButtons={makeAddButtons(part)}
    />);
TickerPart.propTypes = {
    service: PropTypes.instanceOf(AbstractWidgetService).isRequired,
    part: PropTypes.string.isRequired,
};

function TickerMessage() {
    const { enqueueSnackbar, } = useSnackbar();
    const service = usePresetWidgetService("/tickerMessage", errorHandlerWithSnackbar(enqueueSnackbar));

    const elements = {
        "Short":
        <Container maxWidth="md" sx={{ pt: 2 }} className="TickerPanel">
            {/* <Typography variant="h5" gutterBottom>Short</Typography> */}
            <TickerPart service={service} part={"short"}/>
        </Container>,
        "Long":
        <Container maxWidth="md" sx={{ pt: 2 }} className="TickerPanel">
            {/* <Typography variant="h5" gutterBottom sx={{ mt: 3 }}>Long</Typography> */}
            <TickerPart service={service} part={"long"}/>
        </Container>
    };

    return (
        <Dashboard elements={elements} layout="oneColumn" maxWidth="md"/>
    );
}

export default TickerMessage;

