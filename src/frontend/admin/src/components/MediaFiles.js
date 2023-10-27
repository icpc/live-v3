import React, { useState, useEffect } from "react";
import { styled } from "@mui/material/styles";
import { FileUploader } from "react-drag-drop-files";
import { Button, Container, Link, Paper, Stack } from "@mui/material";
import { errorHandlerWithSnackbar, useErrorHandlerWithSnackbar } from "../errors";
import { createApiGet } from "../utils";
import CloudUploadIcon from "@mui/icons-material/CloudUpload";
import AttachFileIcon from "@mui/icons-material/AttachFile";
import Box from "@mui/material/Box";
import { BASE_URL_BACKEND, MEDIAS_LOCATION } from "../config";
import { useSnackbar } from "notistack";
import "../App.css";

const VisuallyHiddenInput = styled("input")({
    clip: "rect(0 0 0 0)",
    clipPath: "inset(50%)",
    height: 1,
    overflow: "hidden",
    position: "absolute",
    bottom: 0,
    left: 0,
    whiteSpace: "nowrap",
    width: 1,
});

const FileLink = styled(Link)(({ theme, highlight }) => ({
    ...theme.typography.body2,
    padding: "8px",
    display: "flex",
    alignItems: "center",
    flexWrap: "wrap",
    color: highlight ? theme.palette.primary.main : null,
}));

const FileItem = ({ fileName, highlight }) => {
    return (
        <Paper>
            <FileLink href={fileUrl(fileName)} highlight={highlight} target="_blank">
                <AttachFileIcon fontSize="small"/>
                <span>{highlight ? fileUrl(fileName) : fileName}</span>
            </FileLink>
        </Paper>
    );
};

const fileUrl = (fileName) => {
    return MEDIAS_LOCATION + "/" + fileName;
};

function AdvancedJson() {
    const errorHandler = useErrorHandlerWithSnackbar();

    const apiGet = createApiGet(BASE_URL_BACKEND + "/media");
    const [mediaFiles, setMediaFiles] = useState([]);
    const [uploadedFileUrl, setUploadedFileUrl] = useState(null);

    const loadFiles = () => {
        apiGet("").then(f => setMediaFiles(f));
    };

    useEffect(() => {
        loadFiles();
    }, []);

    const uploadNewFile = (file) => {
        // const files = e.target.files;
        // if (files.length < 1) {
        //     errorHandler("No media file selected");
        //     return;
        // }
        const formData = new FormData();
        formData.append("file", file);
        fetch(BASE_URL_BACKEND + "/media/upload", {
            method: "POST",
            body: formData,
        }).then(r => r.json()).then(r => {
            if (r.status === "error") {
                errorHandler("Failed to uplaod file: " + file);
            } else if (r.status !== "ok" && !r.response) {
                errorHandler("Failed to uplaod file");
            }
            setUploadedFileUrl(r.status === "ok" && r.response ? r.response : null);
            loadFiles();
        });
    };

    return (
        <Container maxWidth="lg" sx={{ pt: 2 }}>
            <FileUploader handleChange={uploadNewFile} name="file" classes="media-files-uploader" />

            {uploadedFileUrl && (
                <Box sx={{ pt: 1 }}>
                    <FileItem fileName={uploadedFileUrl} highlight />
                </Box>
            )}

            <Box sx={{
                pt: 1,
                display: "grid",
                gridTemplateColumns: { md: "1fr 1fr 1fr 1fr", sm: "1fr 1fr" },
                gap: 1,
            }}>
                {mediaFiles.map(fileName => <FileItem key={fileName} fileName={fileName} />)}
            </Box>
        </Container>
    );
}

export default AdvancedJson;
