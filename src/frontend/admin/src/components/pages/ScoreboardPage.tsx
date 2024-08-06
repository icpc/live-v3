import ScoreboardManager, { DEFAULT_SCOREBOARD_SETTINGS } from "@/components/managers/ScoreboardManager.tsx";
import { useScoreboardWidgetService } from "@/services/scoreboardService.ts";
import { useServiceLoadStatus } from "@/services/abstractSingleWidget.ts";
import { Container } from "@mui/material";

const ScoreboardPage = () => {
    const service = useScoreboardWidgetService();
    const { isShown, settings, setSettings } =
        useServiceLoadStatus(service, DEFAULT_SCOREBOARD_SETTINGS);

    return (
        <Container maxWidth="md" sx={{ display: "flex", width: "75%", flexDirection: "column", pt: 2 }}
            className="ScoreboardSettings">
            <ScoreboardManager service={service} isShown={isShown} settings={settings} setSettings={setSettings} />
        </Container>
    );
};

export default ScoreboardPage;
