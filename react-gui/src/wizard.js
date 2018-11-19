import React from 'react';
import './css/wizard.css';
//const Fragment = React.Fragment;


  export class WizardStep extends React.Component {
    render() {
      return (
        <a href={this.props.stepLink} className="wizard__step wizard__step--{this.props.stepState}" >
          <div className="wizard__line">&nbsp;</div>
          <div className="wizard__step-number-dot">{this.props.stepNumber}</div>
          <div className="wizard__text">{this.props.stepText}</div>
        </a>
      );
    }
  }

  export class Wizard extends React.Component {

    render() {
      return (
        <div className="wizard mt5 m5">
          {this.props.wizardSteps.map((wizardSteps) => <WizardStep
            stepLink={wizardSteps.stepLink}
            stepState={wizardSteps.stepState}
            stepNumber={wizardSteps.stepNumber}
            stepText={wizardSteps.stepText}
           />)}
        </div>
      );
    }
  }
