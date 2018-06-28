import React from "react";
import {Alert} from "react-bootstrap";

export default class DeployMsg extends React.Component {

    render() {
        let deployment = this.props.deployment;
        if (deployment === null || deployment === undefined) {
            return "";
        }

        let msgType = deployment.status === "DEPLOYED"
            ? "info"
            : "warning";

        return (
            <Alert bsStyle={msgType}>
                {deployment.msg}
                <br/>
                {deployment.workDir === undefined ? "" : "Working directory: " + deployment.workDir}
            </Alert>
        );
    }

}