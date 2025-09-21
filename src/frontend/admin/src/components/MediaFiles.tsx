import React, { useState, useEffect } from "react";
import { styled } from "@mui/material/styles";
import { FileUploader } from "react-drag-drop-files";
import {  Container, Link, Paper } from "@mui/material";
import { useErrorHandlerWithSnackbar } from "../errors";
import { createApiGet } from "shared-code/utils";
import AttachFileIcon from "@mui/icons-material/AttachFile";
import Box from "@mui/material/Box";
import { BASE_URL_BACKEND, MEDIAS_LOCATION } from "../config";
import "../App.css";

const fileUrl = (fileName: string): string => `${MEDIAS_LOCATION}/${fileName}`;

const FileLink = styled(Link, {
    shouldForwardProp: prop => prop !== "highlight",
})<{ highlight?: boolean }>(({ theme, highlight }) => ({
    ...theme.typography.body2,
    padding: "8px",
    display: "flex",
    alignItems: "center",
    flexWrap: "wrap",
    color: highlight ? theme.palette.primary.main : null,
}));

interface FileItemProps {
    fileName: string;
    highlight?: boolean;
}

function FileItem({
    fileName,
    highlight
}: FileItemProps): React.ReactElement {
    return (
        <Paper>
            <FileLink href={fileUrl(fileName)} highlight={highlight} target="_blank" rel="noreferrer">
                <AttachFileIcon fontSize="small"/>
                <span>{highlight ? fileUrl(fileName) : fileName}</span>
            </FileLink>
        </Paper>
    );
};

function MediaFiles(): React.ReactElement {
    const errorHandler = useErrorHandlerWithSnackbar();

    const apiGet = createApiGet(BASE_URL_BACKEND + "/media");
    const [mediaFiles, setMediaFiles] = useState<string[]>([]);
    const [uploadedFileUrls, setUploadedFileUrls] = useState<string[] | null>(null);

    const loadFiles = () => {
        apiGet("")
            .then((f: unknown) => setMediaFiles((f as string[]) ?? []))
            .catch(() => {
                errorHandler("Failed to load media files");
            });
    };

    useEffect(() => {
        loadFiles();
    }, [uploadedFileUrls]);

    const uploadNewFile = (files) => {
        const list: File[] = Array.isArray(files) ? files : [files];
        const formData = new FormData();
        list.forEach(file => formData.append("file", file));

        fetch(`${BASE_URL_BACKEND}/media/upload`, {
            method: "POST",
            body: formData,
        })
            .then(r => r.json())
            .then(r => {
                if (r.status === "error") {
                    errorHandler(`Failed to upload files ${list.map(f => f.name).join(",")}`);
                } else if (r.status !== "ok" && !r.response) {
                    errorHandler("Failed to upload files");
                }
                setUploadedFileUrls(r.status === "ok" && r.response ? r.response : null);
                loadFiles();
            })
            .catch(() => {
                errorHandler("Failed to upload files");
            });
    };

    return (
        <Container maxWidth="lg" sx={{ pt: 2 }}>
            <FileUploader
                handleChange={uploadNewFile}
                name="file"
                classes="media-files-uploader"
                multiple
            />

            {uploadedFileUrls && uploadedFileUrls.map(file => (
                <Box sx={{ pt: 1 }} key={file}>
                    <FileItem fileName={file} highlight />
                </Box>
            ))}

            <Box sx={{
                pt: 1,
                display: "grid",
                gridTemplateColumns: { md: "1fr 1fr 1fr 1fr", sm: "1fr 1fr" },
                gap: 1,
            }}>
                {mediaFiles.map(fileName =>
                    <FileItem fileName={fileName} />
                )}
            </Box>
        </Container>
    );
}

export default MediaFiles;
