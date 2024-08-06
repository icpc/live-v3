import { Container } from "@mui/material";
import { useServiceLoadStatus, useSingleWidgetService } from "@/services/abstractSingleWidget.ts";
import FullScreenClockManager, {
    DEFAULT_FULL_SCREEN_CLOCK_SETTINGS
} from "@/components/managers/FullScreenClockManager.tsx";
import { FullScreenClockSettings } from "@shared/api.ts";

const FullScreenClockPage = () => {
    const service = useSingleWidgetService<FullScreenClockSettings>("/fullScreenClock");
    const { isShown, settings, setSettings } =
        useServiceLoadStatus(service, DEFAULT_FULL_SCREEN_CLOCK_SETTINGS);

    return (
        <Container maxWidth="md" sx={{ display: "flex", flexDirection: "column", pt: 2 }}>
            <FullScreenClockManager service={service} isShown={isShown} settings={settings} setSettings={setSettings} />
        </Container>
    );
};

export default FullScreenClockPage;
