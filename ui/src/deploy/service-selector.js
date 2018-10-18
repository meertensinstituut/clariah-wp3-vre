import React from "react";
import {Button, Panel, Table} from "react-bootstrap";
import DeployMsg from "./deploy-msg";
import DreamFactory from '../common/dreamfactory';
import PropTypes from 'prop-types';
import ErrorMsg from "../common/error-msg";
import DeployResource from "../common/deploy-resource";

export default class ServiceSelector extends React.Component {

    /**
     * Props are defined below class
     */
    constructor(props) {
        super(props);
        this.state = {
            object: null,
            services: null,
            selected: this.props.selected,
            error: null
        };
        this.getResources();
    }

    async getResources() {
        let object;

        object = await DreamFactory
            .getObject(this.props.file)
            .catch((e) => this.setState({error: e}));

        if (!object) {
            return;
        }
        const services = await DeployResource
            .getServices(object.id)
            .catch((e) => this.setState({error: e}));

        this.setState({services, object});

    }

    getFileName() {
        let file = this.state.object.filepath.split(/[//]+/).pop();
        return <code>{file}</code>;
    }

    handleSelect(service) {
        let newService;
        if (this.state.selected === service) {
            newService = null;
        } else {
            newService = service;
        }
        this.setState(
            {selected: newService},
            () => this.props.onSelect(this.state.selected)
        );
    }

    isSelected = (id) => {
        return this.state.selected !== null && this.state.selected === id;
    };

    render() {
        if (this.state.error)
            return <ErrorMsg title="Could not select service" error={this.state.error}/>;

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
                                        <i className={this.isSelected(service.id) ? "fa fa-check-square-o" : "fa fa fa-square-o"}/>
                                    </Button>
                                </td>
                            </tr>
                        );
                    }, this)}
                    </tbody>
                </Table>
            </div>
        );
    }
}

ServiceSelector.propTypes = {
    file: PropTypes.any.isRequired,
    onSelect: PropTypes.func.isRequired
};
