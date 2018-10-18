import React from "react";
import {Redirect, withRouter} from 'react-router-dom';
import ServiceSelector from "./service-selector";
import Configurator from "./configurator";
import DeployResource from "../common/deploy-resource";
import DeployMsg from "./deploy-msg";
import Steps from "./steps";
import ErrorMsg from "../common/error-msg";

import './deploy.css';

class Deploy extends React.Component {

    constructor(props) {
        super(props);

        this.state = {
            redirect: null,
            service: null,
            serviceName: null,
            config: null,
            steps: [
                {key: 'file', value: null, label: '<< Select file'},
                {key: 'service', value: null, label: 'Select service'},
                {key: 'config', value: null, label: 'Configure service'},
                {key: 'deploy', value: null, label: 'Deploy >>'}
            ],
            active: 'file',
            completed: false
        };
    }

    handleChangedSteps = () => (steps, active, completed) => {
        let redirect = active === "file";
        this.setState({steps, active, completed, redirect}, () => {
            if (active === 'deploy') {
                this.handleDeploy();
            }
        });
    };

    handleSelectService = (selected) => {
        this.setStepValue('service', (selected ? selected : null));
        const completed = selected !== null;
        this.setState({completed, steps: this.state.steps});
    };

    handleValidConfig = (serviceName, config) => {
        const completed = true;
        this.setStepValue('config', true);
        this.setState({
            serviceName,
            config,
            completed,
            steps: this.state.steps
        });
    };

    handleInvalidConfig = () => {
        this.setStepValue('config', false);
        this.setState({
            config: null,
            steps: this.state.steps
        });
    };

    handleDeploy = async () => {
        const data = await this.requestDeployment(
            this.state.serviceName,
            this.state.config
        );

        this.setState({
                active: 'deploy',
                steps: this.state.steps,
                deployment: data
            },
            () => window.scrollTo(0, 0)
        );
    };

    async requestDeployment(service, config) {
        const data = await DeployResource
            .postDeployment(service, config)
            .catch((e) => this.setState({error: e}));
        await this.setState({deployment: data});
        return data;
    }

    setStepValue(key, value) {
        this.state.steps
            .find(s => s.key === key)
            .value = value;
    }

    getStepValue(key) {
        return this.state.steps
            .find(s => s.key === key)
            .value;
    }

    render() {
        if (this.state.error)
            return <ErrorMsg error={this.state.error}/>;

        if (this.state.redirect)
            return <Redirect to='/files'/>;

        const steps =
            <Steps
                steps={this.state.steps}
                active={this.state.active}
                completed={this.state.completed}
                onChangedSteps={this.handleChangedSteps()}
            />;

        const selectService = this.state.active === 'service'
            ? <div>
                <h2>1. Select service</h2>
                <ServiceSelector
                    file={this.getStepValue('file')}
                    selected={this.getStepValue('service')}
                    onSelect={this.handleSelectService}
                />
            </div>
            : null;

        const configureService = this.state.active === 'config'
            ? <div>
                <h2>2. Configure service</h2>
                <Configurator
                    service={Number(this.getStepValue('service'))}
                    file={Number(this.getStepValue('file'))}
                    onValid={this.handleValidConfig}
                    onInvalid={this.handleInvalidConfig}
                />
            </div>
            : null;

        const deploymentMsg = this.state.deployment !== null
            ? <DeployMsg deployment={this.state.deployment}/>
            : null;

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