import React from "react";
import {Button, Modal, Panel, Table} from "react-bootstrap";
import Switchboard from "./switchboard";
import DeployMsg from "./deploy-msg";

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

    handleDeploy(service) {
        this.setState({deployed: true});
        requestDeployment.call(this, service);
    }

    getFileName() {
        let file = this.state.object.filepath.split(/[//]+/).pop();
        return <code>{file}</code>;
    }

    render() {
        if (this.state.object === null) {
            return "";
        }
        if (this.state.services === null || this.state.services === undefined) {
            getServices.call(this);
            return "";
        }

        return (
            <Modal.Dialog>
                <Modal.Header>
                    <Modal.Title>Deploy</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <DeployMsg deployment={this.state.deployment}/>
                    <Panel>
                        <Panel.Body>Process {this.getFileName()} with one of the services below.</Panel.Body>
                    </Panel>
                    <Table striped bordered condensed hover>
                        <thead>
                        <tr>
                            <th>#</th>
                            <th>Service</th>
                        </tr>
                        </thead>
                        <tbody>
                        {this.state.services.map(function (service) {
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
                                            Deploy <i className="fa fa-play-circle"/>
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

function getServices() {
    Switchboard
        .getServices(this.state.object.id)
        .done((data) => {
            this.setState({services: data});
            this.forceUpdate();
        });
}

function requestDeployment(service) {
    Switchboard
        .getDeployment(service.name, this.state.object.id)
        .done((data) => {
            this.setState({deployment: data});
            this.forceUpdate();
        });
}
