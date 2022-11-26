import React from "react";
import PropTypes from "prop-types";
import { Grid } from "@mui/material";
import Box from "@mui/material/Box";
import { grey } from "@mui/material/colors";

export const TEAM_FIELD_STRUCTURE = PropTypes.shape({
    id: PropTypes.oneOfType([PropTypes.number, PropTypes.string]).isRequired,
    shown: PropTypes.bool.isRequired,
    selected: PropTypes.bool.isRequired,
    name: PropTypes.string.isRequired,
    medias: PropTypes.shape({
        screen: PropTypes.string,
    }).isRequired,
});

export class Team extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return (<Grid sx={{ display: "flex", width: "100%", height: "100%" }}>
            <Box
                key={this.props.rowData.id}
                sx={{ backgroundColor:
                    (this.props.rowData.shown?
                        this.props.tStyle.activeColor :
                        (this.props.rowData.selected?
                            this.props.tStyle.selectedColor :
                            this.props.tStyle.inactiveColor)),
                display: "flex",
                alignItems: "center",
                width: "100%",
                height: "100%",
                cursor: "pointer",
                margin: "4px",
                borderBottom: "1px solid rgba(224, 224, 224, 1)",
                color: (this.props.rowData.selected || this.props.rowData.shown ? grey[900] : grey[700]) }}
                onClick={() => this.props.onClick(this.props.rowData.id)}>
                <Box key="name" sx={{ display: "flex",
                    flexDirection: "row",
                    margin: "4px" }}>
                    {this.props.rowData.contestSystemId}
                    {":"}
                    {this.props.rowData.name}{" "}
                    {/*{this.props.rowData.medias.screen}*/}
                </Box>
            </Box>
        </Grid>);
    }
}

Team.propTypes = {
    tStyle: PropTypes.shape({
        activeColor: PropTypes.string,
        inactiveColor: PropTypes.string,
        selectedColor: PropTypes.string,
    }).isRequired,
    rowData: TEAM_FIELD_STRUCTURE,
    createErrorHandler: PropTypes.func,
    isImmutable: PropTypes.bool,
    onClick: PropTypes.func.isRequired
};
