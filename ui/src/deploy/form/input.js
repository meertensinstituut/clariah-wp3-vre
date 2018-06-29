import React from "react";
import {ControlLabel, FormControl, FormGroup, HelpBlock} from 'react-bootstrap';

export default class Input extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            value: ""
        }
    }

    handleValidationFormBasicText() {
        const length = this.state.value.length;
        if (length > 10) return 'success';
        if (length > 5) return 'warning';
        if (length > 0) return 'error';
        return null;
    }

    render() {
        return (
            <FormGroup
                controlId="formBasicText"
                validationState={this.handleValidationFormBasicText()}
            >
                <ControlLabel>{this.props.param.label}</ControlLabel>
                <HelpBlock>{this.props.param.description}</HelpBlock>
                <FormControl
                    type="text"
                    value={this.props.param.value}
                    placeholder=""
                    onChange={this.handleChange}
                />
                <FormControl.Feedback/>
            </FormGroup>
        );
    }
}