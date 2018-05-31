import React from "react";
import {Alert, Button, Modal, Panel, Table} from "react-bootstrap";
import $ from "jquery";

export default class DeployServiceModal extends React.Component {

    constructor(props) {
        super(props);
        this.state = DeployServiceModal.startState();
    }

    static getDerivedStateFromProps(nextProps, prevState) {
        if (nextProps.object === null) {
            return DeployServiceModal.startState();
        }
        return {object: nextProps.object};
    }

    static startState() {
        return {
            object: null,
            services: null,
            deployment: null,
            deployed: false
        };
    }

    handleClose() {
        this.props.deselectObject();
    }

    requestServices() {
        let url = `http://localhost:9010/switchboard/rest/object/${this.state.object.id}/services`;
        $.get({
            url: url
        }).done((data) => {
            this.setState({services: data});
            this.forceUpdate();
        });
    }

    handleDeploy(service) {
        this.setState({deployed: true});
        let url = `http://localhost:9010/switchboard/rest/exec/${service.name}`;
        let object = this.state.object;
        $.post({
            url: url,
            contentType: "application/json; charset=utf-8",
            data: JSON.stringify({
                params: [{
                    value: object.id,
                    type: "file",
                    name: "untokinput",
                    params: [{"language": "nld"}]
                }]
            })
        }).done((data) => {
            this.setState({deployment: data});
            this.forceUpdate();
        });
    }

    getFileName(filepath) {
        return filepath.split(/[//]+/).pop();
    }

    render() {
        if (this.state.object === null) {
            return "";
        }
        if (this.state.services === null || this.state.services === undefined) {
            this.requestServices();
            return "";
        }

        let deploymentMsg = "";
        let deployment = this.state.deployment;
        if (deployment !== null && deployment !== undefined) {
            let msgType = deployment.status === "DEPLOYED" ? "info" : "warning";
            deploymentMsg = <Alert bsStyle={msgType}>
                {deployment.msg}
                <br />
                {deployment.workDir === undefined ? "" : "Working directory: " + deployment.workDir}
            </Alert>;
        }

        return (
            <Modal.Dialog>
                <Modal.Header>
                    <Modal.Title>Deploy</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    {deploymentMsg}
                    <Panel>
                        <Panel.Body>Process <code>{this.getFileName(this.state.object.filepath)}</code> with one of the services below.</Panel.Body>
                    </Panel>
                    <Table striped bordered condensed hover>
                        <thead>
                        <tr>
                            <th>#</th>
                            <th>Service</th>
                        </tr>
                        </thead>
                        <tbody>
                        {this.state.services.map(function (service, i) {
                            return (
                                <tr className="clickable"
                                    key={service.id}
                                >
                                    <td>{service.id}</td>
                                    <td>
                                        {service.name}
                                        <Button
                                            bsSize="xsmall"
                                            bsStyle="success"
                                            className="pull-right"
                                            onClick={() => this.handleDeploy(service)}
                                            disabled={this.state.deployed}
                                        >
                                            Deploy &gt;
                                        </Button>
                                    </td>
                                </tr>
                            );
                        }, this)}
                        </tbody>
                    </Table>

                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={() => this.handleClose()}>Close</Button>
                </Modal.Footer>
            </Modal.Dialog>
        );
    }

}
