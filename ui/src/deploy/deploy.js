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
import IllegalActionError from "../common/illegal-action-error";

class Deploy extends React.Component {

    constructor(props) {
        super(props);

        const fileId = queryString.parse(this.props.location.search).file;

        this.state = {
            service: null,
            serviceName: null,
            config: null,
            steps: [
                {key: 'file', value: fileId, label: '<< Select file'},
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
        if (newIndex > activeIndex && !this.state.completed) {
            this.setState({error: new IllegalActionError('Please complete the current step first')});
            return;
        }

        const steps = this.state.steps;
        const activeKey = steps[newIndex].key;
        const value = steps[newIndex].value;
        const completed = !!value;
        const goToFiles = steps[newIndex].key === 'file';

        this.setState({steps, active: activeKey, completed, goToFiles}, () => {
            if (activeKey === 'deploy') {
                this.handleDeploy();
            }
        });
    };

    handleSelectService = (selected) => {
        this.getStep('service').value = selected ? selected : null;
        const completed = selected !== null;
        const steps = this.state.steps;
        this.setState({completed, steps});
    };

    handleConfigChange = (serviceName, config) => {
        console.log("config", config);
        if(config.valid) {
            this.handleValidConfig(serviceName, config);
        } else {
            this.handleInvalidConfig();
        }
    };

    handleValidConfig(serviceName, config) {
        const completed = true;
        const steps = this.state.steps;
        this.getStep('config').value = true;

        this.setState({
            serviceName,
            config,
            completed,
            steps
        });
    }

    handleInvalidConfig = () => {
        console.log("config is invalid");
        this.getStep('config').value = false;
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

    getStep(key) {
        return this.state.steps.find(s => s.key === key)
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
        return <Wizard wizardSteps={wizardSteps}/>;
    }

    gotoNextStep = () => {
        const currentIndex = this.state.steps.findIndex(
            s => s.key === this.state.active
        );
        const nextIndex = currentIndex + 1;
        if (nextIndex < this.state.steps.length) {
            this.handleChangedSteps(currentIndex, nextIndex);
        }
    };

    render() {
        const error = this.state.error
            ?
            <ErrorMsg error={this.state.error}/>
            :
            null;

        if (this.state.goToFiles)
            return <Redirect to='/files'/>;

        const steps = this.renderSteps();

        const selectService = this.state.active === 'service'
            ?
            <div>
                <h2>1. Select service</h2>
                <ServiceSelector
                    file={this.getStep('file').value}
                    selected={this.getStep('service').value}
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
                    service={Number(this.getStep('service').value)}
                    file={Number(this.getStep('file').value)}
                    onChange={this.handleConfigChange}
                />
            </div>
            :
            null;

        const deploymentMsg = this.state.deployment !== null
            ?
            <DeployMsg deployment={this.state.deployment}/>
            :
            null;

        const nextBtn = this.state.completed
            ?
            <button type="button" className="btn-success pull-right" onClick={() => this.gotoNextStep()}>Next>></button>
            :
            null;

        return (
            <div>
                {steps}
                {error}
                {deploymentMsg}
                {selectService}
                {configureService}
                {nextBtn}
            </div>
        );
    }
}

export default withRouter(Deploy);