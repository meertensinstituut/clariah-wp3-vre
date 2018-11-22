import React from "react";
import {Redirect, withRouter} from 'react-router-dom';
import ServiceSelector from "./service-selector";
import Configurator from "./configurator";
import DeployResource from "../common/deploy-resource";
import DeployMsg from "./deploy-msg";
import queryString from 'query-string';
import ErrorMsg from "../common/error-msg";

import './deploy.css';
import {Wizard} from "../react-gui/wizard";

class Deploy extends React.Component {

    constructor(props) {
        super(props);

        this.state = {
            service: null,
            serviceName: null,
            config: null,
            steps: [
                {key: 'file', value: queryString.parse(this.props.location.search).file, label: '<< Select file'},
                {key: 'service', value: null, label: 'Select service'},
                {key: 'config', value: null, label: 'Configure service'},
                {key: 'deploy', value: null, label: 'Deploy >>'}
            ],
            active: 'service',
            completed: false,
            goToFiles: false
        };
    }

    handleChangedSteps = (activeIndex, newIndex) => {
        if(newIndex > activeIndex && !this.state.completed) {
            return;
        }

        const steps = this.state.steps;
        const activeKey = steps[newIndex].key;
        const completed = !!steps[newIndex].value;
        const goToFiles = steps[newIndex].key === 'file';

        this.setState({steps, active: activeKey, completed, goToFiles}, () => {
            if (activeKey === 'deploy') {
                this.handleDeploy();
            }
        });
    };

    handleSelectService = (selected) => {
        this.setStepValue('service', (selected ? selected : null));
        const completed = selected !== null;
        const steps = this.state.steps;
        this.setState({completed, steps});
    };

    handleValidConfig = (serviceName, config) => {
        const completed = true;
        const steps = this.state.steps;

        this.setStepValue('config', true);

        this.setState({
            serviceName,
            config,
            completed,
            steps
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

    renderSteps() {
        let activeIndex = this.state.steps.findIndex(s => s.key === this.state.active);

        const wizardSteps = this.state.steps.map((s, i) => {
            const stepState = (i === activeIndex ? 'current' : (i < activeIndex ? 'done' : ''));
            return {
                onClick: () => this.handleChangedSteps(activeIndex, i),
                stepState: stepState,
                stepNumber: i + 1,
                stepText: s.label
            };
        });
        return <Wizard wizardSteps={wizardSteps} />;
    }

    render() {
        if (this.state.error)
            return <ErrorMsg error={this.state.error}/>;

        if (this.state.goToFiles)
            return <Redirect to='/files'/>;

        const steps = this.renderSteps();

        const selectService = this.state.active === 'service'
            ?
            <div>
                <h2>1. Select service</h2>
                <ServiceSelector
                    file={this.getStepValue('file')}
                    selected={this.getStepValue('service')}
                    onSelect={this.handleSelectService}
                />
            </div>
            :
            null;

        const configureService = this.state.active === 'config'
            ?
            <div>
                <h2>2. Configure service</h2>
                <Configurator
                    service={Number(this.getStepValue('service'))}
                    file={Number(this.getStepValue('file'))}
                    onValid={this.handleValidConfig}
                    onInvalid={this.handleInvalidConfig}
                />
            </div>
            :
            null;

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
            </div>
        );
    }
}

export default withRouter(Deploy);