import React from 'react';
import './css/wizard.css';

export class WizardStep extends React.Component {
    render() {
        return (
            <div
                onClick={() => this.props.onClick()}
                className={"wizard__step wizard__step--" + this.props.stepState}
            >
                <div className="wizard__line">&nbsp;</div>
                <div className="wizard__step-number-dot">{this.props.stepNumber}</div>
                <div className="wizard__text">{this.props.stepText}</div>
            </div>
        );
    }
}

export class Wizard extends React.Component {
    render() {
        return (
            <div className="wizard mt5 m5">
                {this.props.wizardSteps.map((wizardStep, i) => {
                    return <WizardStep
                        key={i}
                        onClick={wizardStep.onClick}
                        stepState={wizardStep.stepState}
                        stepNumber={wizardStep.stepNumber}
                        stepText={wizardStep.stepText}
                    />
                })}
            </div>
        );
    }
}
