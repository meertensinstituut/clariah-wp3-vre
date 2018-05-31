import React from "react";
import {Button, Modal, Panel, Table} from "react-bootstrap";
import $ from "jquery";

export default class DeployServiceModal extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            object: null,
            services: null,
        };
    }

    static getDerivedStateFromProps(nextProps, prevState) {
        if (nextProps.object === null) {
            return {
                object: null
            };
        }
        return {
            object: nextProps.object
        }
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

    getFileName(filepath) {
        return filepath.split(/[//]+/).pop();
    }

    render() {
        if (this.state.object === null) {
            return "";
        }

        if(this.state.services === null || this.state.services === undefined) {
            this.requestServices();
            return "";
        }

        return (
            <Modal.Dialog>
                <Modal.Header>
                    <Modal.Title>Deploy</Modal.Title>
                </Modal.Header>
                <Modal.Body>
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
                    <Button
                        onClick={() => this.handleClose()}
                    >
                        Close
                    </Button>
                </Modal.Footer>
            </Modal.Dialog>
        );
    }

}
