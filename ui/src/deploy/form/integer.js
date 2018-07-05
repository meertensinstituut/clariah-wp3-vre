import React from "react";
import {FormControl, FormGroup} from 'react-bootstrap';

export default class Integer extends React.Component {

    handleValidation() {
        const value = this.props.value;
        if (value.length === 0) return null;
        return RegExp(/^[0-9]*$/).test(value)
            ? "success"
            : "error";
    }

    handleChange = (e) => {
        this.props.onChange(e.target.value);
    };

    render() {
        return (
            <div>
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
            </div>
        );
    }
}