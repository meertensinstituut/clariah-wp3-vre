import React from "react";
import {Alert} from "react-bootstrap";
import {NavLink} from "react-router-dom";

export default class DeployMsg extends React.Component {

    render() {
        let deployment = this.props.deployment;
        if (deployment === null || deployment === undefined) {
            return "";
        }

        let msgType = deployment.status === "DEPLOYED"
            ? "info"
            : "warning";

        let link = deployment.workDir
            ? <NavLink
                exact
                to={'/poll/' + deployment.workDir}
                className='nav-link'
                activeClassName='active'
            >
                Bekijk status
            </NavLink>
            : null;

        return (
            <Alert bsStyle={msgType}>
                {deployment.msg}
                <br/>
                {link}
            </Alert>
        );
    }

}