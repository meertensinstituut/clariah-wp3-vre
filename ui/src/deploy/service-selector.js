import React from "react";
import {Button, Panel, Table} from "react-bootstrap";
import DeployMsg from "./deploy-msg";
import DreamFactory from '../common/dreamfactory';
import PropTypes from 'prop-types';
import Switchboard from "../common/switchboard";

export default class ServiceSelector extends React.Component {

    /**
     * Props are defined below class
     */
    constructor(props) {
        super(props);
        this.state = {
            object: null,
            services: null,
            selected: null
        };
        this.getResources();
        this.isSelected = this.isSelected.bind(this);
    }

    getResources() {
        DreamFactory.getObject(this.props.file).done((data) => {
            this.setState({object: data});
            Switchboard.getServices(this.state.object.id).done((data) => {
                this.setState({services: data});
            });
        });
    }

    getFileName() {
        let file = this.state.object.filepath.split(/[//]+/).pop();
        return <code>{file}</code>;
    }

    handleSelect(service) {
        let newService = this.state.selected !== null && this.state.selected.id === service.id
            ? null
            : service;
        this.setState({selected: newService});
    }

    isSelected(id) {
        return this.state.selected !== null && this.state.selected.id === id;
    }

    canGoToNextStep() {
        return this.state.selected !== null;
    }

    handleNextStep() {
        this.props.onSelect(this.state.selected);
    }

    render() {
        if (this.state.services === null)
            return <div className="main">Loading service selector...</div>;

        return (
            <div>
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
                                        bsStyle={this.isSelected(service.id) ? "success" : "info"}
                                        className="pull-right"
                                        onClick={() => this.handleSelect(service)}
                                    >
                                        {this.isSelected(service.id) ? "Selected " : "Select "}
                                        <i className={this.isSelected(service.id) ? "fa fa-check-square-o" : "fa fa fa-square-o"} />
                                    </Button>
                                </td>
                            </tr>
                        );
                    }, this)}
                    </tbody>
                </Table>
                <Button
                    bsSize="xsmall"
                    bsStyle="success"
                    className="pull-right"
                    onClick={() => this.handleNextStep()}
                    disabled={!this.canGoToNextStep()}
                >Next step &gt;</Button>
            </div>
        );
    }
}

ServiceSelector.propTypes = {
    file: PropTypes.number.isRequired,
    onSelect: PropTypes.func.isRequired
};
