import ScoreboardManager, {
    DEFAULT_SCOREBOARD_SETTINGS,
} from "@admin/components/managers/ScoreboardManager.tsx";
import { useScoreboardWidgetService } from "@admin/services/scoreboardService.ts";
import { useServiceLoadStatus } from "@admin/services/abstractSingleWidget.ts";
import { Container } from "@mui/material";

const ScoreboardPage = () => {
    const service = useScoreboardWidgetService();
    const { isShown, settings, setSettings } = useServiceLoadStatus(
        service,
        DEFAULT_SCOREBOARD_SETTINGS,
    );

    return (
        <Container
            maxWidth="md"
            sx={{
                display: "flex",
                width: "75%",
                flexDirection: "column",
                pt: 2,
            }}
            className="ScoreboardSettings"
        >
            <ScoreboardManager
                service={service}
                isShown={isShown}
                settings={settings}
                setSettings={setSettings}
            />
        </Container>
    );
};

export default ScoreboardPage;
