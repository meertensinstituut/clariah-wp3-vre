import React from "react";
import {FormControl, FormGroup} from 'react-bootstrap';

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
        if (length === 0) return null;
        return 'success';
    }

    handleChange(e) {
        this.props.onChange(e.target.value);
        this.setState({value: e.target.value});
    }

    render() {
        return (
            <FormGroup
                controlId="formBasicText"
                validationState={this.handleValidation()}
            >
                <FormControl
                    type="text"
                    value={this.state.value}
                    onChange={this.handleChange}
                />
                <FormControl.Feedback/>
            </FormGroup>
        );
    }
}