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
import PropTypes from "prop-types";

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
FileItem.propTypes = {
    fileName: PropTypes.string.isRequired,
    highlight: PropTypes.bool,
};

const fileUrl = (fileName) => {
    return MEDIAS_LOCATION + "/" + fileName;
};

function MediaFiles() {
    const errorHandler = useErrorHandlerWithSnackbar();

    const apiGet = createApiGet(BASE_URL_BACKEND + "/media");
    const [mediaFiles, setMediaFiles] = useState([]);
    const [uploadedFileUrls, setUploadedFileUrls] = useState(null);

    const loadFiles = () => {
        apiGet("").then(f => setMediaFiles(f));
    };

    useEffect(() => {
        loadFiles();
    }, [uploadedFileUrls]);

    const uploadNewFile = (files) => {
        files = [...files];
        const formData = new FormData();
        files.forEach(file => formData.append("file", file));
        fetch(BASE_URL_BACKEND + "/media/upload", {
            method: "POST",
            body: formData,
        }).then(r => r.json()).then(r => {
            if (r.status === "error") {
                errorHandler("Failed to upload files " + files.map(f => f.name).join(","));
            } else if (r.status !== "ok" && !r.response) {
                errorHandler("Failed to upload files");
            }
            setUploadedFileUrls(r.status === "ok" && r.response ? r.response : null);
            loadFiles();
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
                {mediaFiles.map(fileName => <FileItem key={fileName} fileName={fileName} />)}
            </Box>
        </Container>
    );
}

export default MediaFiles;
