import React from "react";
import {Button, Panel, Table} from "react-bootstrap";
import DeployMsg from "./deploy-msg";
import DreamFactory from '../common/dreamfactory';
import PropTypes from 'prop-types';
import Switchboard from "../common/switchboard";
import StatePropsViewer from "../common/state-props-viewer";

export default class ServiceSelector extends React.Component {

    /**
     * Props are defined below class
     */
    constructor(props) {
        super(props);
        this.state = {
            object: null,
            services: null,
            selected: this.props.selected
        };
        this.getResources();
        this.isSelected = this.isSelected.bind(this);
    }

    getResources() {
        DreamFactory.getObject(this.props.file).done((data) => {
            this.setState({object: data});
            this.getServices();
        });
    }

    getServices() {
        Switchboard.getServices(this.state.object.id).done((data) => {
            this.setState({services: data});
        });
    }

    getFileName() {
        let file = this.state.object.filepath.split(/[//]+/).pop();
        return <code>{file}</code>;
    }

    handleSelect(service) {
        let newService;
        if(this.state.selected === service) {
            newService = null;
        } else {
            newService = service;
        }
        this.setState(
            {selected: newService},
            () => this.props.onSelect(this.state.selected)
        );
    }

    isSelected(id) {
        return this.state.selected !== null && this.state.selected === id;
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
                                        onClick={() => this.handleSelect(service.id)}
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
                <StatePropsViewer state={this.state} props={this.props} hide={false}/>
            </div>
        );
    }
}

ServiceSelector.propTypes = {
    file: PropTypes.any.isRequired,
    onSelect: PropTypes.func.isRequired
};
