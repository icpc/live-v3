import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Container, Typography,
} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import ShowPresetButton from "@/components/controls/ShowPresetButton.tsx";
import { ReactNode, useState } from "react";
import { useServiceLoadStatus, useSingleWidgetService } from "@/services/abstractSingleWidget.ts";
import { FullScreenClockSettings, ObjectSettings } from "@shared/api.ts";
import { useScoreboardWidgetService } from "@/services/scoreboardService.ts";
import ScoreboardManager, { DEFAULT_SCOREBOARD_SETTINGS } from "@/components/managers/ScoreboardManager.tsx";
import FullScreenClockManager, { DEFAULT_FULL_SCREEN_CLOCK_SETTINGS } from "@/components/managers/FullScreenClockManager.tsx";

type WidgetGroupProp = {
    title: string;
    children?: ReactNode;
    isShown: boolean;
    onClickShow: (newState: boolean) => void;
};

const WidgetGroup = ({ title, children, isShown, onClickShow }: WidgetGroupProp) => {
    const [expanded, setExpanded] = useState(false);

    return (
        <Accordion
            slotProps={{ transition: { unmountOnExit: true } }}
            expanded={expanded && !!children}
            onChange={(_, e) => setExpanded(e)}
            className="WidgetGroupAccordion"
        >
            <AccordionSummary expandIcon={children && <ExpandMoreIcon />}>
                <ShowPresetButton
                    onClick={onClickShow}
                    checked={isShown}
                />
                <Typography className="aboba" variant="body2" gutterBottom>{title}</Typography>
            </AccordionSummary>
            <AccordionDetails sx={{ py: 1 }}>
                {children}
            </AccordionDetails>
        </Accordion>
    );
};

type NoSettingsWidgetProps = {
    title: string;
    apiPath: string;
};

const SimpleWidgetGroup = ({ title, apiPath }: NoSettingsWidgetProps) => {
    const service = useSingleWidgetService<ObjectSettings>(apiPath);
    const { isShown } = useServiceLoadStatus(service, undefined);

    return (
        <WidgetGroup title={title} isShown={isShown} onClickShow={s => s ? service.show() : service.hide()}/>
    );
};

const ScoreboardWidgetGroup = () => {
    const service = useScoreboardWidgetService();
    const { isShown, settings, setSettings } =
        useServiceLoadStatus(service, DEFAULT_SCOREBOARD_SETTINGS);

    return (
        <WidgetGroup title={"Scoreboard"} isShown={isShown} onClickShow={s => s ? service.show() : service.hide()}>
            <ScoreboardManager service={service} isShown={isShown} settings={settings} setSettings={setSettings} />
        </WidgetGroup>
    );
};

const FullScreenClockWidgetGroup = () => {
    const service = useSingleWidgetService<FullScreenClockSettings>("/fullScreenClock");
    const { isShown, settings, setSettings } =
        useServiceLoadStatus(service, DEFAULT_FULL_SCREEN_CLOCK_SETTINGS);

    return (
        <WidgetGroup title={"Full screen clock"} isShown={isShown} onClickShow={s => s ? service.show() : service.hide()}>
            <FullScreenClockManager service={service} isShown={isShown} settings={settings} setSettings={setSettings} />
        </WidgetGroup>
    );
};

const ControlsPage = () => {
    return (
        <Container maxWidth="md" sx={{ pt: 2 }} className="Controls">
            <ScoreboardWidgetGroup/>
            <SimpleWidgetGroup title={"Queue"} apiPath={"/queue"}/>
            <SimpleWidgetGroup title={"Statistic"} apiPath={"/statistics"}/>
            <SimpleWidgetGroup title={"Ticker"} apiPath={"/ticker"}/>
            <FullScreenClockWidgetGroup/>
        </Container>
    );
};

export default ControlsPage;
