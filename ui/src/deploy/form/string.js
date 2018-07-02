import React from "react";
import {ControlLabel, FormControl, FormGroup, HelpBlock} from 'react-bootstrap';

export default class String extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            value: ""
        };
        this.handleChange = this.handleChange.bind(this);
    }

    handleValidation() {
        const length = this.state.value.length;
        if(length === 0) return null;
        return 'success';
    }

    handleChange(e) {
        this.setState({ value: e.target.value });
    }

    render() {
        return (
            <FormGroup
                controlId="formBasicText"
                validationState={this.handleValidation()}
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