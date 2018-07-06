import React from "react";
import {FormControl, FormGroup} from 'react-bootstrap';
import RemoveButton from "./remove-button";

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
                    <div className="input-group mb-3">
                        <FormControl
                            type="text"
                            value={this.props.value}
                            onChange={this.handleChange}
                        />
                        <FormControl.Feedback/>
                        <div className="input-group-append">
                            <RemoveButton
                                canRemove={this.props.canRemove}
                                onRemove={this.props.onRemove}
                                nextToInput={true}
                            />
                        </div>
                    </div>
                </FormGroup>
            </div>
        );
    }
}