import React from "react";
import {Redirect, withRouter} from 'react-router-dom';
import queryString from 'query-string';
import {Button, Pagination} from 'react-bootstrap';
import ServiceSelector from "./service-selector";
import Configurator from "./configurator";
import Switchboard from "../common/switchboard";
import DeployMsg from "./deploy-msg";

class Deploy extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            params: queryString.parse(this.props.location.search),
            redirect: null,
            serviceName: null,
            serviceParams: null,
            config: null,
        };

        this.handleSelect = this.handleSelect.bind(this);
    }

    handleBackClick() {
        this.setState({redirect: "/files"});
    }

    handleBackToServiceClick() {
        const params = this.state.params;
        delete params.service;
        delete params.deployed;
        delete params.params;
        this.props.history.push('/deploy?' + queryString.stringify(params));
        this.setState({params});
    }

    handleSelect(service) {
        let params = this.state.params;
        params.service = service.id;
        this.props.history.push('/deploy?' + queryString.stringify(params));
        this.setState({params});
    }

    isCurrentStep(page, nextPage) {
        return this.state.params[page] !== undefined
            && this.state.params[nextPage] === undefined;
    }

    isDisabledStep(previousPage) {
        return this.state.params[previousPage] === undefined;
    }

    handleValidConfig = (serviceName, config) => {
        this.setState({serviceName, config});
    };

    handleInvalidConfig = () => {
        this.setState({config: null});
    };

    handleDeploy = () => () => {
        let params = this.state.params;
        params.params = true;
        this.props.history.push('/deploy?' + queryString.stringify(params));

        this.requestDeployment(
            this.state.serviceName,
            this.state.config
        ).done((data) => {
            let params = this.state.params;
            params.deployed = true;
            this.props.history.push('/deploy?' + queryString.stringify(params));
            this.setState(
                {params},
                () => window.scrollTo(0, 0)
            );
        });
    };

    requestDeployment(service, config) {
        return Switchboard.getDeployment(service, config).done((data) => {
            this.setState({deployment: data});
            this.forceUpdate();
        });
    }

    render() {
        if (this.state.redirect !== null) return <Redirect to='/files'/>;

        if (this.state.params.file === undefined) return <Redirect to="/files"/>;

        const selectService = this.isCurrentStep('file', 'service')
            ?
            <div>
                <h2>1. Select service</h2>
                <ServiceSelector
                    file={this.state.params.file}
                    selected={this.state.params.service}
                    onSelect={this.handleSelect}
                />
            </div>
            :
            null;

        const configureService = this.isCurrentStep('service', true)
            ?
            <div>
                <h2>2. Configure service</h2>
                <Configurator
                    service={Number(this.state.params.service)}
                    file={Number(this.state.params.file)}
                    onValid={this.handleValidConfig}
                    onInvalid={this.handleInvalidConfig}
                />
            </div>
            :
            null;

        let deploymentMsg = this.state.deployment === null
            ?
            null
            :
            <DeployMsg deployment={this.state.deployment}/>;

        return (
            <div>
                <div>
                    <Pagination>
                        <Pagination.Item
                            onClick={() => this.handleBackClick()}
                        >
                            &lt; Select file
                        </Pagination.Item>
                        <Pagination.Item
                            active={this.isCurrentStep('file', 'service')}
                            disabled={this.isDisabledStep('file')}
                            onClick={() => this.handleBackToServiceClick()}
                        >
                            Select service
                        </Pagination.Item>
                        <Pagination.Item
                            active={this.isCurrentStep('service', 'deploy')}
                            disabled={this.isDisabledStep('service')}
                        >
                            Configure service
                        </Pagination.Item>
                        <Pagination.Item
                            active={this.isCurrentStep('deploy', 'deployed')}
                            disabled={!this.state.config || this.state.params.deployed}
                            onClick={this.handleDeploy()}
                        >
                            Deploy service
                        </Pagination.Item>
                    </Pagination>
                </div>
                {deploymentMsg}
                {selectService}
                <div className="clearfix"/>
                {configureService}
                <Button
                    bsSize="xsmall"
                    bsStyle="success"
                    className="pull-right deploy-service"
                    onClick={this.handleDeploy()}
                    disabled={!this.state.config || this.state.params.deployed}
                >
                    Next step <i className="fa fa-angle-double-right"/>
                </Button>
            </div>
        );
    }
}

export default withRouter(Deploy);