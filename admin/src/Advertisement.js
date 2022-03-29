import React from "react";

import "./App.css";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import VisibilityIcon from "@mui/icons-material/Visibility";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";
import { Table, TableBody, TableCell, TableHead, TableRow } from "@mui/material";
import Button from "@mui/material/Button";
import Box from "@mui/material/Box";

import ShowButton from "./ShowButton";
import { BACKEND_API_URL } from "./config";
import {PresetsPanel} from "./PresetsPanel";

class AdvertisementPanel extends PresetsPanel {
}

function Advertisement() {
    return (
        <div className="Advertisement">
            <AdvertisementPanel/>
        </div>
    );
}

export default Advertisement;
