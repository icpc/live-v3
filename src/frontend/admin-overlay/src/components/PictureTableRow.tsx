import React, { useCallback, useState, useMemo } from "react";
import { TableCell, TableRow, TextField } from "@mui/material";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import SaveIcon from "@mui/icons-material/Save";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import CardMedia from "@mui/material/CardMedia";
import Typography from "@mui/material/Typography";

import ShowPresetButton from "./controls/ShowPresetButton.tsx";
import { activeRowColor } from "../styles.js";

// TODO: MOVE TO consts.ts?
const FULL_IMG_WIDTH = "25%";
const FULL_INFO_WIDTH = "75%";

type PictureSettings = {
    name: string;
    url: string;
} & Record<string, unknown>;

type PictureRowData = {
    id: number | string;
    shown: boolean;
    settings: PictureSettings;
};

type PictureTableRowProps = {
    data: PictureRowData;
    onShow: () => void;
    onEdit: (updated: PictureRowData) => unknown;
    onDelete: () => void;
    isImmutable?: boolean;
};

export function PictureTableRow({
    data,
    onShow,
    onEdit,
    onDelete,
    isImmutable,
}: PictureTableRowProps): React.ReactElement {
    const [isEditing, setIsEditing] = useState<boolean>(false);
    const initialFormState = useMemo(
        () => ({
            name: data.settings.name ?? "",
            url: data.settings.url ?? "",
        }),
        [data.settings.name, data.settings.url],
    );

    const [form, setForm] = useState<PictureSettings>(initialFormState);

    const handleChange = useCallback(
        (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
            setForm((prev) => ({ ...prev, [key]: e.target.value })),
        [],
    );

    const handleEditToggle = useCallback(async () => {
        if (isImmutable) {
            return;
        }

        if (!isEditing) {
            setForm(initialFormState);
            setIsEditing(true);
            return;
        }

        const updated: PictureRowData = {
            ...data,
            settings: {
                ...data.settings,
                name: form.name.trim(),
                url: form.url.trim(),
            },
        };

        await onEdit(updated);
        setIsEditing(false);
    }, [data, form.name, form.url, isEditing, isImmutable, onEdit]);

    const handleSubmit = useCallback(
        async (e: React.FormEvent<HTMLFormElement>) => {
            e.preventDefault();
            if (isEditing) {
                await handleEditToggle();
            }
        },
        [handleEditToggle, isEditing],
    );

    const imgAlt = useMemo(
        () => data.settings.name?.toString() || "picture",
        [data.settings.name],
    );

    return (
        <TableRow key={data.id}>
            <TableCell component="th" scope="row" sx={{ p: 1, border: 0 }}>
                <Card
                    sx={{
                        display: "flex",
                        alignItems: "center",
                        backgroundColor: data.shown
                            ? activeRowColor
                            : undefined,
                    }}
                >
                    <CardMedia
                        sx={{ display: "flex", width: FULL_IMG_WIDTH }}
                        component="img"
                        image={data.settings.url}
                        alt={imgAlt}
                    />
                    <Box
                        sx={{
                            display: "flex",
                            width: FULL_INFO_WIDTH,
                            flexDirection: "column",
                        }}
                    >
                        <Box component="form" onSubmit={handleSubmit}>
                            <CardContent sx={{ pl: 2, pr: 2, pt: 2, pb: 1 }}>
                                {!isEditing ? (
                                    <>
                                        <Typography
                                            gutterBottom
                                            variant="h6"
                                            sx={{ mb: 0 }}
                                        >
                                            {data.settings.name}
                                        </Typography>
                                        <Typography
                                            gutterBottom
                                            variant="caption"
                                        >
                                            {data.settings.url}
                                        </Typography>
                                    </>
                                ) : (
                                    <>
                                        <Box sx={{ my: 1 }}>
                                            <TextField
                                                autoFocus
                                                fullWidth
                                                value={form.name}
                                                type="text"
                                                size="small"
                                                label="name"
                                                onChange={handleChange("name")}
                                            />
                                        </Box>
                                        <Box sx={{ my: 1 }}>
                                            <TextField
                                                fullWidth
                                                value={form.url}
                                                type="text"
                                                size="small"
                                                label="url"
                                                onChange={handleChange("url")}
                                            />
                                        </Box>
                                    </>
                                )}
                            </CardContent>

                            <Box
                                sx={{
                                    display: "flex",
                                    pl: 1,
                                    pr: 1,
                                    pb: 1,
                                    justifyContent: "center",
                                }}
                            >
                                <ShowPresetButton
                                    onClick={onShow}
                                    checked={data.shown}
                                />
                                <IconButton
                                    color={!isEditing ? "inherit" : "primary"}
                                    onClick={handleEditToggle}
                                    disabled={isImmutable}
                                    aria-label={!isEditing ? "Edit" : "Save"}
                                >
                                    {!isEditing ? <EditIcon /> : <SaveIcon />}
                                </IconButton>
                                <IconButton
                                    color="error"
                                    onClick={onDelete}
                                    disabled={isImmutable}
                                    aria-label="Delete"
                                >
                                    <DeleteIcon />
                                </IconButton>
                            </Box>
                        </Box>
                    </Box>
                </Card>
            </TableCell>
        </TableRow>
    );
}
