import React from "react";
import {FormControl, FormGroup} from 'react-bootstrap';

export default class String extends React.Component {

    handleValidation() {
        const length = this.props.value.length;
        if (length === 0) return null;
        return 'success';
    }

    handleChange = (e) => {
        this.props.onChange(e.target.value);
        this.setState({value: e.target.value});
    };

    render() {
        return (
            <FormGroup
                controlId="formBasicText"
                validationState={this.handleValidation()}
            >
                <FormControl
                    type="text"
                    value={this.props.value}
                    onChange={this.handleChange}
                />
                <FormControl.Feedback/>
            </FormGroup>
        );
    }
}