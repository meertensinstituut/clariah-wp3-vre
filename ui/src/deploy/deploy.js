import React from "react";
import {Redirect, withRouter} from 'react-router-dom';
import queryString from 'query-string';
import ServiceSelector from "./service-selector";
import Configurator from "./configurator";
import Switchboard from "../common/switchboard";
import DeployMsg from "./deploy-msg";
import Steps from "./steps";

class Deploy extends React.Component {

    constructor(props) {
        super(props);

        let params = queryString.parse(this.props.location.search);
        this.state = {
            params: params,
            redirect: null,
            service: null,
            serviceName: null,
            config: null,
            steps: [
                {key: 'file', value: params.file, label: '<< Select file', callback: this.handleGoToFiles},
                {key: 'service', value: params.service, label: 'Select service', callback: this.handleGoToService},
                {key: 'config', value: false, label: 'Configure service', callback: this.handleGoToConfig},
                {key: 'deploy', value: false, label: 'Deploy >>', callback: this.handleDeploy}
            ],
            active: 'service',
            completed: false
        };
        if(params.file) this.state.active = 'service';
        if(params.service) {
            this.state.service = params.service;
            this.state.active = 'config';
        }
    }

    handleGoToService = () => {
        this.setState({active: 'service', completed: true});
    };

    handleSelectService = (selected) => {
        const service = (selected ? selected.id : null);
        const completed = service !== false;
        this.setState({completed, service});
    };

    handleGoToConfig = () => {
        if(isNaN(this.state.service)) {
            return;
        }
        this.state.steps
            .find(s => s.key === 'service')
            .value = this.state.service;
        this.setState({
            active: 'config',
            completed: false,
            steps: this.state.steps
        });
    };

    handleGoToFiles = () =>  {
        this.setState({redirect: "/files"});
    };

    handleValidConfig = (serviceName, config) => {
        this.setState({
            serviceName,
            config,
            completed: true,
            steps: this.state.steps
        });
    };

    handleInvalidConfig = () => {
        this.setState({config: null});
    };

    handleDeploy = () => {
        this.requestDeployment(
            this.state.serviceName,
            this.state.config
        ).done((data) => {
            this.state.steps
                .find(s => s.key === 'deploy')
                .value = true;
            this.setState(
                {active: 'deploy', steps: this.state.steps},
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

    getParamValue(step, steps = this.state.steps) {
        let find = steps.find(s => s.key === step);
        return find.value;
    }

    render() {
        if (this.state.redirect !== null) return <Redirect to='/files'/>;

        if (this.state.params.file === undefined) return <Redirect to="/files"/>;

        let steps =
            <Steps
                steps={this.state.steps}
                active={this.state.active}
                completed={this.state.completed}
            />;

        let selectService = this.state.active === 'service'
            ?
            <div>
                <h2>1. Select service</h2>
                <ServiceSelector
                    file={this.state.params.file}
                    selected={this.getParamValue('service')}
                    onSelect={this.handleSelectService}
                />
            </div>
            :
            null;

        let configureService = this.state.active === 'config'
            ?
            <div>
                <h2>2. Configure service</h2>
                <Configurator
                    service={Number(this.getParamValue('service'))}
                    file={Number(this.getParamValue('file'))}
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
                {steps}
                {deploymentMsg}
                {selectService}
                <div className="clearfix"/>
                {configureService}
                {steps}
            </div>
        );
    }
}

export default withRouter(Deploy);