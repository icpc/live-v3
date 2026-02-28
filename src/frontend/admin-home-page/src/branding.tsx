import { createTheme } from "@mui/material";
import { useEffect, useState } from "react";
import { createApiGet } from "shared-code/utils";
import faviconTemplate from "./assets/admin-favicon.svg?raw";

const BACKEND_PROTO =
    window.location.protocol === "https:" ? "https://" : "http://";

export const ADMIN_BACKEND_ROOT =
    import.meta.env.VITE_BACKEND_ROOT ??
    BACKEND_PROTO + window.location.hostname + ":" + window.location.port;

const hexToRgb = (hex: string) => {
    const result =
        /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})([a-f\d]{2}?)$/i.exec(hex);

    return result
        ? {
              r: parseInt(result[1], 16),
              g: parseInt(result[2], 16),
              b: parseInt(result[3], 16),
          }
        : null;
};

const isShouldUseDarkColor = (backgroundColor: string) => {
    const rgb = hexToRgb(backgroundColor);
    if (!rgb) {
        return false;
    }

    const { r, g, b } = rgb;
    const brightness = Math.round((r * 299 + g * 587 + b * 114) / 1000);

    return brightness > 125;
};

const setFavicon = (svg: string) => {
    const existingLink = document.querySelector(
        "link[rel='shortcut icon']",
    ) as HTMLLinkElement | null;
    const link = existingLink ?? document.createElement("link");
    link.type = "image/x-icon";
    link.rel = "shortcut icon";
    link.href = "data:image/svg+xml;base64," + btoa(svg);
    if (!existingLink) {
        document.head.appendChild(link);
    }
};

export const createAdminTheme = (contestColor: string | null) => {
    if (!contestColor) {
        return createTheme({
            palette: {
                text: {
                    primary: "#FFFFFF",
                },
            },
        });
    }

    const textColor = isShouldUseDarkColor(contestColor)
        ? "#000000"
        : "#FFFFFF";

    return createTheme({
        palette: {
            mode: "light",
            primary: {
                main: contestColor,
                contrastText: textColor,
            },
            text: {
                primary: textColor,
            },
        },
    });
};

export const useAdminBranding = (apiRoot: string, titleSuffix: string) => {
    const [contestColor, setContestColor] = useState<string | null>(null);
    const [hiddenPageNames, setHiddenPageNames] = useState<string[]>([]);

    useEffect(() => {
        createApiGet(apiRoot)("/visualConfig").then((config) => {
            setHiddenPageNames(config["ADMIN_HIDE_MENU"] ?? []);

            if (config["CONTEST_COLOR"]) {
                const nextContestColor = config["CONTEST_COLOR"];
                setContestColor(nextContestColor);
                setFavicon(
                    faviconTemplate
                        .replaceAll("{CONTEST_COLOR}", nextContestColor)
                        .replaceAll(
                            "{TEXT_COLOR}",
                            isShouldUseDarkColor(nextContestColor)
                                ? "#000000"
                                : "#FFFFFF",
                        ),
                );
            }

            if (config["CONTEST_CAPTION"]) {
                document.title = `${config["CONTEST_CAPTION"]} — ${titleSuffix}`;
            } else {
                document.title = titleSuffix;
            }
        });
    }, [apiRoot, titleSuffix]);

    return { contestColor, hiddenPageNames };
};
